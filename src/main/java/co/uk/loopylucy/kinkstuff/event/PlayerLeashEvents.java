package co.uk.loopylucy.kinkstuff.event;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.network.LeashSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
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

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player leashedPlayer = event.getEntity();
        if (leashedPlayer.level().isClientSide()) return;

        if (LEASHED_PLAYERS.containsKey(leashedPlayer.getUUID())) {
            // Safety: Un-equipping collar snaps leash
            if (!isWearingCollar(leashedPlayer)) {
                releaseLeash(leashedPlayer, true);
                return;
            }

            LivingEntity holder = getLeashHolder(leashedPlayer);
            if (holder != null) {
                // Break leash conditions (Dimensional shifts, death, or extreme distance thresholds)
                if (holder.level() != leashedPlayer.level() || !holder.isAlive() || leashedPlayer.distanceTo(holder) > 16.0F) {
                    releaseLeash(leashedPlayer, true);
                    return;
                }

                double distance = leashedPlayer.distanceTo(holder);
                if (distance > 4.0D) {
                    Vec3 direction = holder.position().subtract(leashedPlayer.position()).normalize();

                    // Force a server-controlled knockback packet directly into the target client's packet loop
                    if (leashedPlayer instanceof ServerPlayer serverPlayer) {
                        // knockback(strength, ratioX, ratioZ)
                        serverPlayer.knockback(0.4F, -direction.x, -direction.z);
                        // Force an engine update packet to override the velocity array instantly
                        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));
                    }
                }

                if (distance > 15.0D) {
                    Vec3 direction = holder.position().subtract(leashedPlayer.position()).normalize();
                    double pullIntensity = distance - 4.0D; // Tweak this decimal to increase/decrease speed
                    Vec3 pullVelocity = direction.scale(pullIntensity);

                    if (leashedPlayer instanceof ServerPlayer serverPlayer) {
                        // Calculate the absolute destination coordinates
                        double targetX = serverPlayer.getX() + pullVelocity.x;
                        double targetY = serverPlayer.getY();
                        double targetZ = serverPlayer.getZ() + pullVelocity.z;

                        // Force a server-authoritative teleport tracking sync
                        // Parameters: (ServerLevel, X, Y, Z, Set<RelativeMovement>, Yaw, Pitch)
                        serverPlayer.teleportTo(
                                (ServerLevel) serverPlayer.level(),
                                targetX,
                                targetY,
                                targetZ,
                                java.util.Set.of(), // Forces absolute position coordinate changes
                                serverPlayer.getYRot(),
                                serverPlayer.getXRot()
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStartTrackingPlayer(PlayerEvent.StartTracking event) {
        // When a player client enters rendering range of the leashed player, send them the rope data
        ErisKinkStuff.LOGGER.info("onStartPlayerTracking() Test");
        if (event.getTarget() instanceof Player targetPlayer) {
            if (LEASHED_PLAYERS.containsKey(targetPlayer.getUUID())) {
                java.util.UUID holderUUID = LEASHED_PLAYERS.get(targetPlayer.getUUID());

                if (event.getEntity() instanceof ServerPlayer looker) {
                    // Synchronise the visual data to the specific looking player client
                    looker.connection.send(new LeashSyncPacket(targetPlayer.getUUID(), holderUUID));
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

        // 1. Send data to all nearby observers looking at them
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(target, packet);

        // 2. Explicitly send the packet to the leashed target player client
        if (target instanceof ServerPlayer serverTarget) {
            serverTarget.connection.send(packet);
        }

        // 3. Explicitly send the packet to the holder player client
        if (holder instanceof ServerPlayer serverHolder) {
            serverHolder.connection.send(packet);
        }
    }

    public static void releaseLeash(Player player, boolean dropItem) {
        java.util.UUID removed = LEASHED_PLAYERS.remove(player.getUUID());
        LeashSyncPacket emptyPacket = new LeashSyncPacket(player.getUUID(), null);

        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(player, emptyPacket);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(emptyPacket);
        }

        if (removed != null && dropItem && !player.level().isClientSide()) {
            player.drop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEAD), false);
        }
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
