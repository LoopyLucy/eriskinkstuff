package co.uk.loopylucy.tameableplayers.event;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.network.LeashSyncPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

import static co.uk.loopylucy.tameableplayers.common.LeashManager.*;

/**
 * Handles all server-side logic and physics for the player leashing system.
 * This includes tracking leashed states, enforcing leash distance physics,
 * and synchronizing data between clients.
 */
@EventBusSubscriber(modid = TameablePlayers.MODID)
public class PlayerLeashEvents {

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

                //Makes sure that the player cannot fly whilst leashed despite technically being allowed.
                if(leashedPlayer instanceof ServerPlayer) {
                    if (!leashedPlayer.isCreative() && !leashedPlayer.isSpectator()) {
                        if (leashedPlayer.getAbilities().flying) {
                            leashedPlayer.getAbilities().flying = false;
                            leashedPlayer.onUpdateAbilities();
                        }
                    }
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
        releaseLeash(event.getEntity(), true);
    }

    /** Automatically release leash when a player dies. */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) releaseLeash(player, true);
    }
}
