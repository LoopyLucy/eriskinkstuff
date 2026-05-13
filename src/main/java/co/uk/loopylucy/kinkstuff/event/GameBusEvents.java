package co.uk.loopylucy.kinkstuff.event;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.client.ClientAccess;
import co.uk.loopylucy.kinkstuff.client.ClientLeashTracker;
import co.uk.loopylucy.kinkstuff.client.FirstPersonMittenRenderer;
import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.network.LeashServerPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ErisKinkStuff.MODID, value = Dist.CLIENT)
public class GameBusEvents {
    /**
     * LOOP 1: FIRST-PERSON MITTEN OVERLAY
     * Fires per-frame right before Minecraft draws the player's actual first-person hand skin.
     * It allows us to inject custom mitten geometry into the moving animation matrices.
     */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || ClientAccess.MITTENS_MODEL == null) return;

        // Query the Curios capability data array to see if mittens are equipped
        CuriosApi.getCuriosInventory(player).flatMap(inventory -> inventory.findFirstCurio(ModItems.MITTENS.get())).ifPresent(slotResult -> {
            // Forwards matrices to our specialized drawing engine
            FirstPersonMittenRenderer.renderFromArmEvent(
                    event,
                    slotResult.stack(),
                    ClientAccess.MITTENS_MODEL
            );
        });
    }

    /**
     * LOOP 2: CLIENT INTERACTION BLOCKER
     * Intercepts the raw click action BEFORE Minecraft registers that a player entity is under the crosshair.
     * This handles packaging the interaction data and sending it up to the server thread securely.
     */
    @SubscribeEvent
    public static void onClientRightClick(PlayerInteractEvent.RightClickItem event) {
        // Stop execution instantly if this fires on a server thread; crosshairs only exist on clients
        if (!event.getLevel().isClientSide()) return;

        ItemStack heldItem = event.getItemStack();
        // Check input parameters: item must be a Lead or hand must be completely empty
        if (heldItem.is(Items.LEAD) || (heldItem.isEmpty() && event.getHand() == InteractionHand.MAIN_HAND)) {

            // Scan the client's internal camera collision raytrace node
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hitResult;

                if (entityHit.getEntity() instanceof Player targetPlayer) {
                    // DISPATCH: Push a Serverbound execution packet up the network pipeline
                    PacketDistributor.sendToServer(new LeashServerPacket(targetPlayer.getUUID()));

                    // CANCEL: Tell the local client engine to swallow the input click immediately
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * LOOP 3: VISUAL LEAD ROPE RENDERING (THIRD-PERSON / MULTIPLAYER)
     * Fires every frame right after a LivingEntity (a player) has been compiled by the graphics engine.
     * It tracks client-side leash mappings and manually renders the loose rope geometry.
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player targetPlayer) {
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;

            java.util.UUID holderUUID = ClientLeashTracker.getHolderFor(targetPlayer.getUUID());
            if (holderUUID == null) return;

            net.minecraft.world.entity.Entity holder = null;

            // Iterate through the client's raw GPU rendering array list directly
            assert Minecraft.getInstance().level != null;
            for (net.minecraft.world.entity.Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
                if (entity.getUUID().equals(holderUUID)) {
                    holder = entity;
                    break;
                }
            }

            if (holder instanceof LivingEntity livingHolder) {
                PoseStack poseStack = event.getPoseStack();
                MultiBufferSource bufferSource = event.getMultiBufferSource();
                float partialTicks = event.getPartialTick();

                // 1. Calculate the precise position of the holder's hand/chest area
                double holderX = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.xo, livingHolder.getX());
                // Adjust height based on whether it is a player or an object (e.g. fence post)
                double holderHeight = livingHolder instanceof Player ? livingHolder.getEyeHeight() * 0.7D : livingHolder.getEyeHeight() * 0.5D;
                double holderY = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.yo, livingHolder.getY()) + holderHeight;
                double holderZ = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.zo, livingHolder.getZ());

                // 2. Calculate the target player's neck/collar position
                double targetX = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.xo, targetPlayer.getX());
                // This is the target position height (around the neck collar area)
                double targetHeight = targetPlayer.getEyeHeight() * 0.85D;
                double targetY = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.yo, targetPlayer.getY()) + targetHeight;
                double targetZ = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.zo, targetPlayer.getZ());

                // 3. THE CRITICAL REPOSITION: Math needs to be relative to the local matrix (0,0,0 at feet)
                // Start the line at the neck height instead of the ground origin
                float startX = 0.0F;
                float startY = (float) targetHeight;
                float startZ = 0.0F;

                // End the line relative to where the holder is standing compared to the target
                float endX = (float) (holderX - targetX);
                float endY = (float) (holderY - targetY) + startY; // Compensate for starting neck offset
                float endZ = (float) (holderZ - targetZ);

                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.leash());
                poseStack.pushPose();

                PoseStack.Pose entry = poseStack.last();
                for (int i = 0; i <= 24; ++i) {
                    float segmentPct = (float) i / 24.0F;

                    // Linear interpolation between the neck starting point and the holder's hand
                    float segX = net.minecraft.util.Mth.lerp(segmentPct, startX, endX);
                    float segZ = net.minecraft.util.Mth.lerp(segmentPct, startZ, endZ);

                    // Calculate the straight line height, then apply the gravity drop arc
                    float baseLinearY = net.minecraft.util.Mth.lerp(segmentPct, startY, endY);
                    // Gravity sag logic: forces a downward dip curve in the center of the rope segments
                    float sagFactor = segmentPct * (1.0F - segmentPct);
                    float segY = baseLinearY - (sagFactor * 1.8F); // Increase/decrease 1.8F to adjust looseness

                    int blockLight = event.getPackedLight();

                    // Render double-sided wire geometry
                    vertexConsumer.addVertex(entry.pose(), segX, segY, segZ)
                            .setColor(44, 28, 12, 255) // Classic lead leash brown
                            .setLight(blockLight);

                    vertexConsumer.addVertex(entry.pose(), segX + 0.025F, segY + 0.025F, segZ)
                            .setColor(44, 28, 12, 255)
                            .setLight(blockLight);
                }
                poseStack.popPose();
            }
        }
    }
}
