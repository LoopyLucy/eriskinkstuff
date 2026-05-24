package co.uk.loopylucy.tameableplayers.common;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.network.LeashSyncPacket;
import co.uk.loopylucy.tameableplayers.registration.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeashManager {
    /** Maps a leashed player's UUID to their holder's UUID. */
    public static final Map<UUID, UUID> LEASHED_PLAYERS = new HashMap<>();

    /** Tracks how many ticks a player has been horizontally distant to trigger fallback teleportation. */
    public static final Map<UUID, Integer> STUCK_TICKS = new HashMap<>();

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

        TameablePlayers.LOGGER.info("Leash logic triggered: holder={}, target={}, hand={}", holder.getName().getString(), targetPlayer.getName().getString(), hand);

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
                    TameablePlayers.LOGGER.info("Leash successfully attached to {}", targetPlayer.getName().getString());
                } else if (LEASHED_PLAYERS.get(targetPlayer.getUUID()).equals(holder.getUUID())) {
                    // If already leashed by the same holder, right-clicking with a lead releases them (vanilla behaviour)
                    releaseLeash(targetPlayer, true);
                    TameablePlayers.LOGGER.info("Leash successfully released from {} using lead.", targetPlayer.getName().getString());
                }
            }
            // Logic for releasing a leash with an empty hand
            else if (heldItem.isEmpty() && alreadyLeashed) {
                if (LEASHED_PLAYERS.get(targetPlayer.getUUID()).equals(holder.getUUID())) {
                    releaseLeash(targetPlayer, true);
                    TameablePlayers.LOGGER.info("Leash successfully released from {}", targetPlayer.getName().getString());
                }
            }
        } else {
            TameablePlayers.LOGGER.info("Target {} is not wearing a collar, ignoring leash interaction.", targetPlayer.getName().getString());
        }
    }

    /** Resolves the UUID of a holder into a LivingEntity instance. */
    public static LivingEntity getLeashHolder(Player player) {
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

        //Bypass "flying" kick while leashed
        if (target instanceof ServerPlayer serverTarget) {
            AttributeInstance flightAttribute = serverTarget.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            if (flightAttribute != null) {
                flightAttribute.removeModifier(ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "leash_flight_bypass"));
                flightAttribute.addTransientModifier(new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "leash_flight_bypass"),
                        1.0,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }
        }

        LeashSyncPacket packet = new LeashSyncPacket(targetUUID, holderUUID);

        // Sync to the target and everyone tracking them
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, packet);

        // Explicitly sync to the holder
        if (holder instanceof ServerPlayer serverHolder) {
            PacketDistributor.sendToPlayer(serverHolder, packet);
        }
        TameablePlayers.LOGGER.info("Leash map synchronized to tracking matrix!");
    }

    /**
     * Removes a leash connection and notifies relevant clients.
     */
    public static void releaseLeash(Player player, boolean dropItem) {
        java.util.UUID removed = LEASHED_PLAYERS.remove(player.getUUID());
        if (removed == null) return;

        // Re-enable flying kick restrictions (because I guess people might be hacking in a modded server?)
        if (player instanceof ServerPlayer serverPlayer) {
            AttributeInstance flightAttribute = serverPlayer.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            if (flightAttribute != null) {
                flightAttribute.removeModifier(ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "leash_flight_bypass"));
            }

            //Ground player if not creative or spectator
            if (!serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }

        LeashSyncPacket emptyPacket = new LeashSyncPacket(player.getUUID(), null);

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, emptyPacket);

        if (dropItem && !player.level().isClientSide()) {
            player.drop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEAD), false);
        }
    }

    /** Simple check if a player is currently in the leashed map. */
    public static boolean isLeashed(Player player) {
        if (player == null) return false;
        return LEASHED_PLAYERS.containsKey(player.getUUID());
    }

    /** Checks if a player is wearing a Collar in their Curios slots. */
    public static boolean isWearingCollar(Player player) {
        var invOpt = CuriosApi.getCuriosInventory(player);
        return invOpt.isPresent() && invOpt.get().findFirstCurio(ModItems.COLLAR.get()).isPresent();
    }
}
