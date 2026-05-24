package co.uk.loopylucy.tameableplayers.network;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.client.ClientLeashTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A packet sent from the server to clients to synchronize leashed states.
 * This ensures that all clients know which players are leashed to whom, 
 * which is necessary for rendering the leash rope correctly.
 *
 * @param target The UUID of the player who is leashed.
 * @param holder The UUID of the entity (player) holding the leash, or null if unleashed.
 */
public record LeashSyncPacket(UUID target, UUID holder) implements CustomPacketPayload {
    /** The unique identifier for this packet type. */
    public static final Type<LeashSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "leash_sync"));

    /** The codec used to serialize and deserialize this packet over the network. */
    public static final StreamCodec<FriendlyByteBuf, LeashSyncPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.target());
                buf.writeBoolean(packet.holder() != null);
                if (packet.holder() != null) buf.writeUUID(packet.holder());
            },
            buf -> {
                UUID target = buf.readUUID();
                UUID holder = buf.readBoolean() ? buf.readUUID() : null;
                return new LeashSyncPacket(target, holder);
            }
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    /**
     * Handles the packet on the client thread.
     * Updates the client-side leash tracker.
     */
    public static void handle(final LeashSyncPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> ClientLeashTracker.update(payload.target(), payload.holder()));
    }
}