package co.uk.loopylucy.kinkstuff.network;

import co.uk.loopylucy.kinkstuff.client.ClientLeashTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LeashSyncPacket(UUID target, UUID holder) implements CustomPacketPayload {
    public static final Type<LeashSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("eriskinkstuff", "leash_sync"));

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

    public static void handle(final LeashSyncPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> ClientLeashTracker.update(payload.target(), payload.holder()));
    }
}