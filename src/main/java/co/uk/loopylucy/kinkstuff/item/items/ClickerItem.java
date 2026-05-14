package co.uk.loopylucy.kinkstuff.item.items;

import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.sound.ModSounds;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

public class ClickerItem extends Item {

    public ClickerItem() {
        super(new Properties()
                .stacksTo(1)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(0xFFFFFFFF, false))
        );
    }

    public int getColor(ItemStack stack) {
        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        return dyedItemColor != null ? dyedItemColor.rgb() : 0xFFFFFFFF;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player user, @NotNull InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        // Execute logic strictly on the logical server thread
        if (!level.isClientSide()) {
            BlockPos pos = user.blockPosition();

            // 1. PLAY GLOBAL AUDIO PACKET
            // Forcing (Player) null casts the packet to every loaded client in range, including the user
            level.playSound(
                    null,
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    ModSounds.CLICKER.get(), // Extracted from your fixed Mono 44.1kHz supplier registry
                    SoundSource.PLAYERS,
                    1.0F, // Volume
                    1.0F  // Pitch
            );

            // 2. SCAN FOR COLLARED TARGETS WITHIN 30 BLOCKS
            // Creates a 30x30x30 cuboid box matrix centered around the user's feet
            AABB searchBox = new AABB(pos).inflate(30.0D);
            List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, searchBox);

            for (Player target : nearbyPlayers) {
                // Ensure the user doesn't accidentally trigger the look-lock on themselves
                if (target != user && isWearingCollar(target)) {

                    if (target instanceof ServerPlayer serverTarget) {
                        // 3. FORCE CLIENT CAMERA LOOK LOCK
                        // Overrides the client mouse input buffer safely by commanding
                        // the target's eyes to lock directly onto the clicker's chest box node.
                        serverTarget.connection.send(new ClientboundPlayerLookAtPacket(
                                EntityAnchorArgument.Anchor.EYES,
                                user,
                                EntityAnchorArgument.Anchor.EYES
                        ));

                        // 4. SERVER-SIDE ROTATION SYNCHRONISATION
                        // Calculates background vectors to update server tracking fields instantly.
                        // This prevents third-party observers from seeing glitched or delayed head turns.
                        double dx = user.getX() - target.getX();
                        double dy = (user.getY() + user.getEyeHeight() * 0.5D) - (target.getY() + target.getEyeHeight());
                        double dz = user.getZ() - target.getZ();
                        double horizontalDistance = Mth.sqrt((float) (dx * dx + dz * dz));

                        float targetYaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90F;
                        float targetPitch = (float) -(Mth.atan2(dy, horizontalDistance) * (180D / Math.PI));

                        targetYaw = Mth.wrapDegrees(targetYaw);
                        targetPitch = Mth.clamp(targetPitch, -90F, 90F);

                        // Enforce immediate runtime field alignment
                        serverTarget.setYRot(targetYaw);
                        serverTarget.setXRot(targetPitch);
                        serverTarget.setYHeadRot(targetYaw);
                        serverTarget.setYBodyRot(targetYaw);
                        serverTarget.hurtMarked = true; // Signals structural update pack release
                    }
                }
            }
        }

        // Trigger the right-click hand swinging animation frame
        user.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    // Curios API bridge method to filter target entities safely
    private static boolean isWearingCollar(Player player) {
        if (player == null) return false;
        var invOpt = CuriosApi.getCuriosInventory(player);
        return invOpt.isPresent() && invOpt.get().findFirstCurio(ModItems.COLLAR.get()).isPresent();
    }
}