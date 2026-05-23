package co.uk.loopylucy.kinkstuff.network;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.common.LeashManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A packet sent from the client to the server to request a leash interaction.
 * This is used because entity interactions on the client need to be authoritatively 
 * validated and executed on the server to maintain synchronization.
 *
 * @param targetUUID The UUID of the player being leashed or unleashed.
 * @param hand The hand the interacting player used (main or offhand).
 */
public record LeashServerPacket(UUID targetUUID, InteractionHand hand) implements CustomPacketPayload {
    /** The unique identifier for this packet type. */
    public static final Type<LeashServerPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "leash_server"));

    /** The codec used to serialize and deserialize this packet over the network. */
    public static final StreamCodec<FriendlyByteBuf, LeashServerPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.targetUUID());
                buf.writeEnum(packet.hand());
            },
            buf -> new LeashServerPacket(buf.readUUID(), buf.readEnum(InteractionHand.class))
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    /**
     * Handles the packet on the server thread.
     * 
     * @param payload The packet data.
     * @param context The network context, providing access to the player who sent the packet.
     */
    public static void handle(final LeashServerPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer holder) {
                // Resolve the target UUID into an actual entity instance on the server
                net.minecraft.world.entity.Entity targetEntity = holder.serverLevel().getEntity(payload.targetUUID());
                if (targetEntity instanceof net.minecraft.world.entity.player.Player targetPlayer) {
                    // Delegate to the central leash logic handler
                    LeashManager.handleServerLeashLogic(holder, targetPlayer, payload.hand());
                }
            }
        });
    }
}