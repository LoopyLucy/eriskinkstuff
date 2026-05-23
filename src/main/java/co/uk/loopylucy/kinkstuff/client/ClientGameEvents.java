package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.client.render.FirstPersonMittenRenderer;
import co.uk.loopylucy.kinkstuff.network.LeashServerPacket;
import co.uk.loopylucy.kinkstuff.registration.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ErisKinkStuff.MODID, value = Dist.CLIENT)
public class ClientGameEvents {
    /**
     * Renders the first-person mitten overlay onto the player's arms.
     * Fires every frame to align the custom model with the player's movement.
     */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || ClientAccess.MITTENS_MODEL == null) return;

        // Check if mittens are equipped via Curios before rendering
        CuriosApi.getCuriosInventory(player).flatMap(inventory -> inventory.findFirstCurio(ModItems.MITTENS.get())).ifPresent(slotResult -> FirstPersonMittenRenderer.renderFromArmEvent(
                event,
                slotResult.stack(),
                ClientAccess.MITTENS_MODEL
        ));
    }

    /**
     * Intercepts right-clicks on entities to handle custom leashing/unleashing logic.
     * This ensures the client notifies the server of the intent to interact with a player.
     */
    @SubscribeEvent
    public static void onClientRightClick(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide()) return;

        if (event.getTarget() instanceof Player targetPlayer) {
            ItemStack heldItem = event.getItemStack();

            // Trigger leashing if holding a lead, or unleashing if the hand is empty
            if (heldItem.is(Items.LEAD) || (heldItem.isEmpty() && event.getHand() == InteractionHand.MAIN_HAND)) {
                // Send authoritative packet to server for processing
                PacketDistributor.sendToServer(new LeashServerPacket(targetPlayer.getUUID(), event.getHand()));

                // Cancel vanilla interaction to prevent accidental double-handling
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    /**
     * Renders the leash rope in first-person view.
     * This handles drawing the rope when the local player is either leashed OR the holder.
     */
    @SubscribeEvent
    public static void onRenderFirstPersonLeash(RenderHandEvent event) {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        // Only process during the main hand rendering pass
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        float partialTicks = event.getPartialTick();

        // Get lighting at the player's position
        int blockLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(localPlayer.level(), localPlayer.blockPosition());

        // Calculate smooth camera position
        double cameraX = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.xo, localPlayer.getX());
        double cameraY = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.yo, localPlayer.getY()) + localPlayer.getEyeHeight();
        double cameraZ = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.zo, localPlayer.getZ());

        // ROLE A: The local player IS the one leashed
        java.util.UUID holderUUID = ClientLeashTracker.getHolderFor(localPlayer.getUUID());
        if (holderUUID != null) {
            Entity holder = null;
            assert Minecraft.getInstance().level != null;
            for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
                if (entity.getUUID().equals(holderUUID)) { holder = entity; break; }
            }

            if (holder instanceof LivingEntity livingHolder) {
                // Calculate smooth holder position
                double holderX = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.xo, livingHolder.getX());
                double holderY = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.yo, livingHolder.getY()) + (livingHolder.getEyeHeight() * 0.7D);
                double holderZ = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.zo, livingHolder.getZ());

                float dx = (float) (holderX - cameraX);
                float dy = (float) (holderY - cameraY);
                float dz = (float) (holderZ - cameraZ);

                poseStack.pushPose();
                // Compensate for camera rotation
                float headYaw = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.yRotO, localPlayer.getYRot());
                float headPitch = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.xRotO, localPlayer.getXRot());
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(headPitch));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(headYaw + 180.0F));

                // Draw rope from "neck" area to holder
                drawRopeSegments(bufferSource.getBuffer(RenderType.leash()), poseStack.last(), 0.0F, -0.3F, -0.2F, dx, dy, dz, blockLight);
                poseStack.popPose();
                return;
            }
        }

        // ROLE B: The local player IS the holder
        assert Minecraft.getInstance().level != null;
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (entity instanceof Player targetPlayer) {
                java.util.UUID targetsHolderUUID = ClientLeashTracker.getHolderFor(targetPlayer.getUUID());

                if (targetsHolderUUID != null && targetsHolderUUID.equals(localPlayer.getUUID())) {
                    // Calculate smooth target position
                    double targetX = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.xo, targetPlayer.getX());
                    double targetY = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.yo, targetPlayer.getY()) + (targetPlayer.getEyeHeight() * 0.85D);
                    double targetZ = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.zo, targetPlayer.getZ());

                    float dx = (float) (targetX - cameraX);
                    float dy = (float) (targetY - cameraY);
                    float dz = (float) (targetZ - cameraZ);

                    poseStack.pushPose();
                    float headYaw = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.yRotO, localPlayer.getYRot());
                    float headPitch = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.xRotO, localPlayer.getXRot());
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(headPitch));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(headYaw + 180.0F));

                    // Draw rope from "hand" area to target's neck
                    drawRopeSegments(bufferSource.getBuffer(RenderType.leash()), poseStack.last(), 0.3F, -0.4F, -0.1F, dx, dy, dz, blockLight);
                    poseStack.popPose();
                    break;
                }
            }
        }
    }

    /**
     * Draws a series of line segments to simulate a rope with gravitational sag.
     */
    private static void drawRopeSegments(VertexConsumer vertexConsumer, com.mojang.blaze3d.vertex.PoseStack.Pose entry,
                                         float startX, float startY, float startZ, float endX, float endY, float endZ, int blockLight) {
        for (int i = 0; i <= 24; ++i) {
            float segmentPct = (float) i / 24.0F;

            // Linear interpolation for X and Z
            float segX = net.minecraft.util.Mth.lerp(segmentPct, startX, endX);
            float segZ = net.minecraft.util.Mth.lerp(segmentPct, startZ, endZ);

            // Linear Y interpolation plus a parabolic sag factor
            float baseLinearY = net.minecraft.util.Mth.lerp(segmentPct, startY, endY);
            float sagFactor = segmentPct * (1.0F - segmentPct);
            float segY = baseLinearY - (sagFactor * 1.4F);

            // Add vertices to create a thin rope-like geometry
            vertexConsumer.addVertex(entry.pose(), segX, segY, segZ).setColor(44, 28, 12, 255).setLight(blockLight);
            vertexConsumer.addVertex(entry.pose(), segX + 0.015F, segY + 0.015F, segZ).setColor(44, 28, 12, 255).setLight(blockLight);
        }
    }
}