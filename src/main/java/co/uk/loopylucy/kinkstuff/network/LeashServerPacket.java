package co.uk.loopylucy.kinkstuff.network;

import co.uk.loopylucy.kinkstuff.event.PlayerLeashEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LeashServerPacket(UUID targetUUID, InteractionHand hand) implements CustomPacketPayload {
    public static final Type<LeashServerPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("eriskinkstuff", "leash_server"));

    public static final StreamCodec<FriendlyByteBuf, LeashServerPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.targetUUID());
                buf.writeEnum(packet.hand());
            },
            buf -> new LeashServerPacket(buf.readUUID(), buf.readEnum(InteractionHand.class))
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(final LeashServerPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer holder) {
                net.minecraft.world.entity.Entity targetEntity = holder.serverLevel().getEntity(payload.targetUUID());
                if (targetEntity instanceof net.minecraft.world.entity.player.Player targetPlayer) {
                    PlayerLeashEvents.handleServerLeashLogic(holder, targetPlayer, payload.hand());
                }
            }
        });
    }
}