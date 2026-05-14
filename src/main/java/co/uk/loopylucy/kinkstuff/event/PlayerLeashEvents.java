package co.uk.loopylucy.kinkstuff.event;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.network.LeashSyncPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

@EventBusSubscriber(modid = ErisKinkStuff.MODID)
public class PlayerLeashEvents {

    // Maps: Leashed Player -> Leash Holder
    private static final Map<UUID, UUID> LEASHED_PLAYERS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        Player holder = event.getEntity();

        // 1. CRITICAL: Strictly process mechanics on the logical Server to update maps
        if (holder.level().isClientSide()) return;

        // 2. Validate that the targeted object is a Player entity
        if (event.getTarget() instanceof Player targetPlayer) {
            ItemStack heldItem = event.getItemStack();

            // 3. Verify they have your collar equipped in their Curios grid slot
            if (isWearingCollar(targetPlayer)) {
                boolean alreadyLeashed = LEASHED_PLAYERS.containsKey(targetPlayer.getUUID());

                // SCENARIO A: Right-clicking with a Lead to ATTACH the player
                if (heldItem.is(Items.LEAD) && !alreadyLeashed) {
                    attachLeash(holder, targetPlayer);

                    if (!holder.getAbilities().instabuild) {
                        heldItem.shrink(1); // Consume the item on the server registry
                    }

                    // Force Minecraft to cancel further vanilla screen processing loops
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                // SCENARIO B: Right-clicking with an Empty Hand to RELEASE the player
                else if (heldItem.isEmpty() && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && alreadyLeashed) {
                    // Verify that only the tracking coordinator holding the rope can snap it
                    if (LEASHED_PLAYERS.get(targetPlayer.getUUID()).equals(holder.getUUID())) {
                        releaseLeash(targetPlayer, true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    // Inside PlayerLeashEvents.java:
    // Helper map to track how many consecutive ticks a player has been stuck far away
    private static final Map<UUID, Integer> STUCK_TICKS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player leashedPlayer = event.getEntity();
        if (leashedPlayer.level().isClientSide()) return; // Server thread physics only

        if (isLeashed(leashedPlayer)) {
            if (!isWearingCollar(leashedPlayer)) {
                releaseLeash(leashedPlayer, true);
                STUCK_TICKS.remove(leashedPlayer.getUUID());
                return;
            }

            LivingEntity holder = getLeashHolder(leashedPlayer);
            if (holder != null) {
                if (holder.level() != leashedPlayer.level() || !holder.isAlive() || leashedPlayer.distanceTo(holder) > 24.0F) {
                    releaseLeash(leashedPlayer, true);
                    STUCK_TICKS.remove(leashedPlayer.getUUID());
                    return;
                }

                double totalDistance = leashedPlayer.distanceTo(holder);

                // 1. ISOLATE VECTOR DISTANCES
                double dx = holder.getX() - leashedPlayer.getX();
                double dy = (holder.getY() + holder.getEyeHeight() * 0.5D) - (leashedPlayer.getY() + leashedPlayer.getEyeHeight());
                double dz = holder.getZ() - leashedPlayer.getZ();
                double horizontalDistance = Mth.sqrt((float) (dx * dx + dz * dz));

                // Track horizontal stuck loops for the fallback teleport
                if (horizontalDistance > 8.0D) {
                    int ticksStuck = STUCK_TICKS.getOrDefault(leashedPlayer.getUUID(), 0) + 1;
                    STUCK_TICKS.put(leashedPlayer.getUUID(), ticksStuck);

                    if (ticksStuck > 30) {
                        if (leashedPlayer instanceof ServerPlayer serverPlayer) {
                            net.minecraft.world.phys.Vec3 fallbackPos = holder.position().add(holder.getLookAngle().scale(-1.0D));
                            serverPlayer.teleportTo((ServerLevel) serverPlayer.level(), fallbackPos.x, holder.getY(), fallbackPos.z, Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot());
                            STUCK_TICKS.put(leashedPlayer.getUUID(), 0);
                            return;
                        }
                    }
                } else {
                    STUCK_TICKS.put(leashedPlayer.getUUID(), 0);
                }

                // 2. CALCULATE ROTATIONAL LOOK ANGLES
                float targetYaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90F;
                float targetPitch = (float) -(Mth.atan2(dy, horizontalDistance) * (180D / Math.PI));
                targetYaw = Mth.wrapDegrees(targetYaw);
                targetPitch = Mth.clamp(targetPitch, -90F, 90F);

                // 3. ENFORCE PHYSICS VIA KNOCKBACK AND LOOK-LOCK PACKETS
                if (leashedPlayer instanceof ServerPlayer serverPlayer) {

                    // If they wander outside the 4-block slack threshold, apply knockback momentum
                    if (totalDistance > 4.0D) {
                        net.minecraft.world.phys.Vec3 direction = holder.position().subtract(leashedPlayer.position()).normalize();

                        // Scale the knockback strength based on how far away they are being pulled
                        float pullStrength = (float) (totalDistance - 4.0D) * 0.15F;
                        pullStrength = Mth.clamp(pullStrength, 0.2F, 0.8F); // Clamp to prevent extreme launching

                        // Apply server-side knockback vector force
                        // Arguments: strength, xRatio, zRatio
                        // We invert the direction vectors so they are pulled TOWARD you rather than pushed away
                        serverPlayer.knockback(pullStrength, -direction.x, -direction.z);

                        // Vertical assist: If you are significantly higher up, add an extra lift boost
                        if (holder.getY() > leashedPlayer.getY() + 0.5D) {
                            serverPlayer.setDeltaMovement(serverPlayer.getDeltaMovement().add(0.0D, 0.2D, 0.0D));
                        }

                        // Immediately force an explicit velocity packet update to keep physics synchronized
                        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));
                    }

                    // 4. FORCE THE LOOK-LOCK (Absolute Rotation)
                    // We pass 0.0 for X, Y, Z because position movement is handled by the knockback physics loop.
                    // Leaving X, Y, Z as relative means their position stays untouched by this packet, completely stopping glitches.
                    serverPlayer.connection.send(new ClientboundPlayerPositionPacket(
                            0.0D,
                            0.0D,
                            0.0D,
                            targetYaw,
                            targetPitch,
                            Set.of(RelativeMovement.X, RelativeMovement.Y, RelativeMovement.Z), // Keep position relative (unmodified)
                            0
                    ));

                    // Instantly align internal orientation variables
                    serverPlayer.setYRot(targetYaw);
                    serverPlayer.setXRot(targetPitch);
                    serverPlayer.setYHeadRot(targetYaw);
                    serverPlayer.setYBodyRot(targetYaw);
                }
                leashedPlayer.hurtMarked = true;
            }
        } else {
            STUCK_TICKS.remove(leashedPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onStartTrackingPlayer(PlayerEvent.StartTracking event) {
        // When a third-party observer begins tracking/rendering the leashed player
        if (event.getTarget() instanceof Player targetPlayer) {
            if (LEASHED_PLAYERS.containsKey(targetPlayer.getUUID())) {
                java.util.UUID holderUUID = LEASHED_PLAYERS.get(targetPlayer.getUUID());

                // Extract the player who is doing the looking
                if (event.getEntity() instanceof ServerPlayer observerPlayer) {
                    // Directly force-feed the leash coordinates to the observer's client network pipeline
                    observerPlayer.connection.send(new LeashSyncPacket(targetPlayer.getUUID(), holderUUID));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        releaseLeash(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) releaseLeash(player, true);
    }

    private static LivingEntity getLeashHolder(Player player) {
        UUID holderUUID = LEASHED_PLAYERS.get(player.getUUID());
        if (holderUUID == null) return null;
        if (player.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getEntity(holderUUID) instanceof LivingEntity living) return living;
        }
        return null;
    }

    public static void attachLeash(LivingEntity holder, Player target) {
        java.util.UUID targetUUID = target.getUUID();
        java.util.UUID holderUUID = holder.getUUID();
        LEASHED_PLAYERS.put(targetUUID, holderUUID);

        LeashSyncPacket packet = new LeashSyncPacket(targetUUID, holderUUID);

        // MODERN 1.21.1 NETWORKING: Broadcasts to the target player AND every single client
        // that is currently within visual chunk tracking range of them.
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, packet);

        // Also explicitly notify the holder just in case they are outside standard tracking boundaries
        if (holder instanceof net.minecraft.server.level.ServerPlayer serverHolder) {
            serverHolder.connection.send(packet);
        }
        ErisKinkStuff.LOGGER.info("Leash map synchronized to tracking matrix!");
    }

    public static void releaseLeash(Player player, boolean dropItem) {
        java.util.UUID removed = LEASHED_PLAYERS.remove(player.getUUID());
        LeashSyncPacket emptyPacket = new LeashSyncPacket(player.getUUID(), null);

        // Wipe visual maps for everyone tracking the entity
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, emptyPacket);

        if (removed != null && dropItem && !player.level().isClientSide()) {
            player.drop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEAD), false);
        }
    }

    private static boolean isLeashed(Player player) {
        if (player == null) return false;
        return LEASHED_PLAYERS.containsKey(player.getUUID());
    }

    private static boolean isWearingCollar(Player player) {
        var invOpt = CuriosApi.getCuriosInventory(player);
        return invOpt.isPresent() && invOpt.get().findFirstCurio(ModItems.COLLAR.get()).isPresent();
    }

    public static void handleServerLeashLogic(Player holder, Player targetPlayer) {
        if (isWearingCollar(targetPlayer)) {
            boolean alreadyLeashed = LEASHED_PLAYERS.containsKey(targetPlayer.getUUID());
            ItemStack heldItem = holder.getMainHandItem();

            // ATTACHMENT
            if (heldItem.is(Items.LEAD) && !alreadyLeashed) {
                attachLeash(holder, targetPlayer);
                if (!holder.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                ErisKinkStuff.LOGGER.info("Leash successfully attached to " + targetPlayer.getName().getString());
            }
            // RELEASE
            else if (heldItem.isEmpty() && alreadyLeashed) {
                if (LEASHED_PLAYERS.get(targetPlayer.getUUID()).equals(holder.getUUID())) {
                    releaseLeash(targetPlayer, true);
                    ErisKinkStuff.LOGGER.info("Leash successfully released from " + targetPlayer.getName().getString());
                }
            }
        }
    }
}
