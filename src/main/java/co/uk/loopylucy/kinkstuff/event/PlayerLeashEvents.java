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
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

/**
 * Handles all server-side logic and physics for the player leashing system.
 * This includes tracking leashed states, enforcing leash distance physics,
 * and synchronizing data between clients.
 */
@EventBusSubscriber(modid = ErisKinkStuff.MODID)
public class PlayerLeashEvents {

    /** Maps a leashed player's UUID to their holder's UUID. */
    private static final Map<UUID, UUID> LEASHED_PLAYERS = new HashMap<>();
    
    /** Tracks how many ticks a player has been horizontally distant to trigger fallback teleportation. */
    private static final Map<UUID, Integer> STUCK_TICKS = new HashMap<>();

    /**
     * Primary entry point for leashing/unleashing logic.
     * Called via network packets from the client to ensure authoritative server handling.
     * 
     * @param holder The player attempting to leash/unleash another player.
     * @param targetPlayer The player being interacted with.
     * @param hand The hand the holder used for the interaction.
     */
    public static void handleServerLeashLogic(Player holder, Player targetPlayer, net.minecraft.world.InteractionHand hand) {
        if (holder == null || targetPlayer == null || holder == targetPlayer) return;
        if (holder.level().isClientSide()) return;

        ErisKinkStuff.LOGGER.info("Leash logic triggered: holder={}, target={}, hand={}", holder.getName().getString(), targetPlayer.getName().getString(), hand);

        if (isWearingCollar(targetPlayer)) {
            boolean alreadyLeashed = LEASHED_PLAYERS.containsKey(targetPlayer.getUUID());
            ItemStack heldItem = holder.getItemInHand(hand);

            // Logic for attaching a new leash or releasing using a Lead item
            if (heldItem.is(Items.LEAD)) {
                if (!alreadyLeashed) {
                    attachLeash(holder, targetPlayer);
                    if (!holder.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                    ErisKinkStuff.LOGGER.info("Leash successfully attached to " + targetPlayer.getName().getString());
                } else if (LEASHED_PLAYERS.get(targetPlayer.getUUID()).equals(holder.getUUID())) {
                    // If already leashed by the same holder, right-clicking with a lead releases them (vanilla behavior)
                    releaseLeash(targetPlayer, true);
                    ErisKinkStuff.LOGGER.info("Leash successfully released from " + targetPlayer.getName().getString() + " using lead.");
                }
            }
            // Logic for releasing a leash with an empty hand
            else if (heldItem.isEmpty() && alreadyLeashed) {
                if (LEASHED_PLAYERS.get(targetPlayer.getUUID()).equals(holder.getUUID())) {
                    releaseLeash(targetPlayer, true);
                    ErisKinkStuff.LOGGER.info("Leash successfully released from " + targetPlayer.getName().getString());
                }
            }
        } else {
            ErisKinkStuff.LOGGER.info("Target {} is not wearing a collar, ignoring leash interaction.", targetPlayer.getName().getString());
        }
    }

    /**
     * Periodic tick event to enforce leash physics and distance constraints.
     * Runs on the server for all players.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player leashedPlayer = event.getEntity();
        if (leashedPlayer.level().isClientSide()) return;

        if (isLeashed(leashedPlayer)) {
            // Safety check: If they lose their collar, the leash snaps
            if (!isWearingCollar(leashedPlayer)) {
                releaseLeash(leashedPlayer, true);
                STUCK_TICKS.remove(leashedPlayer.getUUID());
                return;
            }

            LivingEntity holder = getLeashHolder(leashedPlayer);
            if (holder != null) {
                // Check for dimensional shifts, death, or extreme distance
                if (holder.level() != leashedPlayer.level() || !holder.isAlive() || leashedPlayer.distanceTo(holder) > 24.0F) {
                    releaseLeash(leashedPlayer, true);
                    STUCK_TICKS.remove(leashedPlayer.getUUID());
                    return;
                }

                double totalDistance = leashedPlayer.distanceTo(holder);

                // Calculate vectors for look-at and pull physics
                double dx = holder.getX() - leashedPlayer.getX();
                double dy = (holder.getY() + holder.getEyeHeight() * 0.5D) - (leashedPlayer.getY() + leashedPlayer.getEyeHeight());
                double dz = holder.getZ() - leashedPlayer.getZ();
                double horizontalDistance = Mth.sqrt((float) (dx * dx + dz * dz));

                // Fallback Teleport Logic: If the player is stuck (e.g., behind a wall) for 30 ticks, teleport them to the holder
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

                // Look-Lock Physics: Force the leashed player to face their holder
                float targetYaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90F;
                float targetPitch = (float) -(Mth.atan2(dy, horizontalDistance) * (180D / Math.PI));
                targetYaw = Mth.wrapDegrees(targetYaw);
                targetPitch = Mth.clamp(targetPitch, -90F, 90F);

                if (leashedPlayer instanceof ServerPlayer serverPlayer) {

                    // Movement Physics: Apply knockback if outside the 4-block slack range
                    if (totalDistance > 4.0D) {
                        net.minecraft.world.phys.Vec3 direction = holder.position().subtract(leashedPlayer.position()).normalize();

                        float pullStrength = (float) (totalDistance - 4.0D) * 0.15F;
                        pullStrength = Mth.clamp(pullStrength, 0.2F, 0.8F);

                        serverPlayer.knockback(pullStrength, -direction.x, -direction.z);

                        // Give a small vertical boost if being pulled upwards
                        if (holder.getY() > leashedPlayer.getY() + 0.5D) {
                            serverPlayer.setDeltaMovement(serverPlayer.getDeltaMovement().add(0.0D, 0.2D, 0.0D));
                        }

                        // Notify client of velocity changes
                        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));
                    }

                    // Force client-side rotation sync
                    serverPlayer.connection.send(new ClientboundPlayerPositionPacket(
                            0.0D, 0.0D, 0.0D, targetYaw, targetPitch,
                            Set.of(RelativeMovement.X, RelativeMovement.Y, RelativeMovement.Z),
                            0
                    ));

                    // Align server-side rotation fields
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

    /**
     * Synchronizes leash data when a new player starts tracking an existing player.
     */
    @SubscribeEvent
    public static void onStartTrackingPlayer(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player targetPlayer) {
            if (LEASHED_PLAYERS.containsKey(targetPlayer.getUUID())) {
                java.util.UUID holderUUID = LEASHED_PLAYERS.get(targetPlayer.getUUID());

                if (event.getEntity() instanceof ServerPlayer observerPlayer) {
                    observerPlayer.connection.send(new LeashSyncPacket(targetPlayer.getUUID(), holderUUID));
                }
            }
        }
    }

    /** Automatically release leash when a player logs out. */
    @SubscribeEvent
    public static void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        releaseLeash(event.getEntity(), false);
    }

    /** Automatically release leash when a player dies. */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) releaseLeash(player, true);
    }

    /** Resolves the UUID of a holder into a LivingEntity instance. */
    private static LivingEntity getLeashHolder(Player player) {
        UUID holderUUID = LEASHED_PLAYERS.get(player.getUUID());
        if (holderUUID == null) return null;
        if (player.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getEntity(holderUUID) instanceof LivingEntity living) return living;
        }
        return null;
    }

    /**
     * Registers a new leash connection and notifies relevant clients.
     */
    public static void attachLeash(LivingEntity holder, Player target) {
        java.util.UUID targetUUID = target.getUUID();
        java.util.UUID holderUUID = holder.getUUID();
        LEASHED_PLAYERS.put(targetUUID, holderUUID);

        LeashSyncPacket packet = new LeashSyncPacket(targetUUID, holderUUID);

        // Sync to the target and everyone tracking them
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, packet);

        // Explicitly sync to the holder
        if (holder instanceof ServerPlayer serverHolder) {
            PacketDistributor.sendToPlayer(serverHolder, packet);
        }
        ErisKinkStuff.LOGGER.info("Leash map synchronized to tracking matrix!");
    }

    /**
     * Removes a leash connection and notifies relevant clients.
     */
    public static void releaseLeash(Player player, boolean dropItem) {
        java.util.UUID removed = LEASHED_PLAYERS.remove(player.getUUID());
        if (removed == null) return;
        
        LeashSyncPacket emptyPacket = new LeashSyncPacket(player.getUUID(), null);

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, emptyPacket);

        if (dropItem && !player.level().isClientSide()) {
            player.drop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEAD), false);
        }
    }

    /** Simple check if a player is currently in the leashed map. */
    private static boolean isLeashed(Player player) {
        if (player == null) return false;
        return LEASHED_PLAYERS.containsKey(player.getUUID());
    }

    /** Checks if a player is wearing a Collar in their Curios slots. */
    private static boolean isWearingCollar(Player player) {
        var invOpt = CuriosApi.getCuriosInventory(player);
        return invOpt.isPresent() && invOpt.get().findFirstCurio(ModItems.COLLAR.get()).isPresent();
    }
}
