package co.uk.loopylucy.kinkstuff.network;

import co.uk.loopylucy.kinkstuff.event.PlayerLeashEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LeashServerPacket(UUID targetUUID) implements CustomPacketPayload {
    public static final Type<LeashServerPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("eriskinkstuff", "leash_server"));

    public static final StreamCodec<FriendlyByteBuf, LeashServerPacket> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUUID(packet.targetUUID()),
            buf -> new LeashServerPacket(buf.readUUID())
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(final LeashServerPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer holder) {
                // Find the targeted player entity on the server world
                net.minecraft.world.entity.Entity targetEntity = holder.serverLevel().getEntities().get(payload.targetUUID());
                if (targetEntity instanceof net.minecraft.world.entity.player.Player targetPlayer) {
                    // Trigger the leash calculation mechanics
                    PlayerLeashEvents.handleServerLeashLogic(holder, targetPlayer);
                }
            }
        });
    }
}