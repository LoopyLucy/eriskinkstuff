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
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.client.event.RenderHandEvent;
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
    public static void onRenderFirstPersonLeash(RenderHandEvent event) {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        // 1. Only process during the main hand rendering sequence to prevent duplicate line drawing passes
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        float partialTicks = event.getPartialTick();

        // Establish the universal environmental lighting value at the player's exact block boundaries
        int blockLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(localPlayer.level(), localPlayer.blockPosition());

        // Compute basic camera world vector origins
        double cameraX = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.xo, localPlayer.getX());
        double cameraY = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.yo, localPlayer.getY()) + localPlayer.getEyeHeight();
        double cameraZ = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.zo, localPlayer.getZ());

        // ─── ROLE A: IF THE LOCAL PLAYER IS BEING LEASHED ───
        java.util.UUID holderUUID = ClientLeashTracker.getHolderFor(localPlayer.getUUID());
        if (holderUUID != null) {
            Entity holder = null;
            assert Minecraft.getInstance().level != null;
            for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
                if (entity.getUUID().equals(holderUUID)) { holder = entity; break; }
            }

            if (holder instanceof LivingEntity livingHolder) {
                double holderX = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.xo, livingHolder.getX());
                double holderY = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.yo, livingHolder.getY()) + (livingHolder.getEyeHeight() * 0.7D);
                double holderZ = net.minecraft.util.Mth.lerp(partialTicks, livingHolder.zo, livingHolder.getZ());

                float dx = (float) (holderX - cameraX);
                float dy = (float) (holderY - cameraY);
                float dz = (float) (holderZ - cameraZ);

                poseStack.pushPose();
                // Counter-rotate the screen matrices to cancel out head-bobbing and view shifts
                float headYaw = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.yRotO, localPlayer.getYRot());
                float headPitch = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.xRotO, localPlayer.getXRot());
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(headPitch));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(headYaw + 180.0F));

                // Start line lower on the screen (the collar area)
                drawRopeSegments(bufferSource.getBuffer(RenderType.leash()), poseStack.last(), 0.0F, -0.3F, -0.2F, dx, dy, dz, blockLight);
                poseStack.popPose();
                return; // Exit execution - leash processing for this frame is finished
            }
        }

        // ─── ROLE B: IF THE LOCAL PLAYER IS THE ONE HOLDING THE LEASH ───
        // Scan all loaded rendering entities to find if anyone is actively tied to your UUID
        assert Minecraft.getInstance().level != null;
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (entity instanceof Player targetPlayer) {
                java.util.UUID targetsHolderUUID = ClientLeashTracker.getHolderFor(targetPlayer.getUUID());

                if (targetsHolderUUID != null && targetsHolderUUID.equals(localPlayer.getUUID())) {
                    double targetX = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.xo, targetPlayer.getX());
                    double targetY = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.yo, targetPlayer.getY()) + (targetPlayer.getEyeHeight() * 0.85D);
                    double targetZ = net.minecraft.util.Mth.lerp(partialTicks, targetPlayer.zo, targetPlayer.getZ());

                    // Calculate the path heading out from your eyes to their neck collar box
                    float dx = (float) (targetX - cameraX);
                    float dy = (float) (targetY - cameraY);
                    float dz = (float) (targetZ - cameraZ);

                    poseStack.pushPose();
                    // Counter-rotate view matrix to lock the vector path to the targeted character model
                    float headYaw = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.yRotO, localPlayer.getYRot());
                    float headPitch = net.minecraft.util.Mth.lerp(partialTicks, localPlayer.xRotO, localPlayer.getXRot());
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(headPitch));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(headYaw + 180.0F));

                    // Start line on the right side of the screen (simulating your character's dominant hand holding the lead)
                    drawRopeSegments(bufferSource.getBuffer(RenderType.leash()), poseStack.last(), 0.3F, -0.4F, -0.1F, dx, dy, dz, blockLight);
                    poseStack.popPose();
                    break;
                }
            }
        }
    }

    // SHARED SEGMENT GENERATOR: Compiles the gravitational quadratic line arc mesh
    private static void drawRopeSegments(VertexConsumer vertexConsumer, com.mojang.blaze3d.vertex.PoseStack.Pose entry,
                                         float startX, float startY, float startZ, float endX, float endY, float endZ, int blockLight) {
        for (int i = 0; i <= 24; ++i) {
            float segmentPct = (float) i / 24.0F;

            float segX = net.minecraft.util.Mth.lerp(segmentPct, startX, endX);
            float segZ = net.minecraft.util.Mth.lerp(segmentPct, startZ, endZ);

            float baseLinearY = net.minecraft.util.Mth.lerp(segmentPct, startY, endY);
            float sagFactor = segmentPct * (1.0F - segmentPct);
            float segY = baseLinearY - (sagFactor * 1.4F); // Loose gravity rope sag

            vertexConsumer.addVertex(entry.pose(), segX, segY, segZ).setColor(44, 28, 12, 255).setLight(blockLight);
            vertexConsumer.addVertex(entry.pose(), segX + 0.015F, segY + 0.015F, segZ).setColor(44, 28, 12, 255).setLight(blockLight);
        }
    }
}