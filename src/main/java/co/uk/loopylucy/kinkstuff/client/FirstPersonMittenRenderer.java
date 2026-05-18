package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.client.models.MittensModel;
import co.uk.loopylucy.kinkstuff.item.items.MittensItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * Custom renderer for the Mittens in first-person view.
 * This class handles drawing the mittens over the player's arms when they are visible 
 * in the first-person camera.
 */
public class FirstPersonMittenRenderer {
    /** Texture for the dyeable mittens. */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ErisKinkStuff.MODID, "textures/entity/mittens_dyeable.png"
    );

    /**
     * Renders the mitten overlay during a RenderArmEvent.
     * This is called for both the left and right arms in first-person.
     * 
     * @param event The arm rendering event.
     * @param stack The mittens ItemStack.
     * @param model The baked MittensModel.
     */
    public static void renderFromArmEvent(RenderArmEvent event, ItemStack stack, MittensModel model) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MittensItem)) {
            return;
        }

        PoseStack matrixStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int light = event.getPackedLight();

        // Fetch and process dye color
        int colour = ((MittensItem)stack.getItem()).getColor(stack);
        int finalARGB = 0xFF000000 | (colour & 0xFFFFFF);

        boolean isRightArm = (event.getArm() == HumanoidArm.RIGHT);

        // Resolve the specific arm part and mitten sub-part to render
        ModelPart armPart = isRightArm ? model.rightArm : model.leftArm;
        ModelPart mittenPart = isRightArm ? model.rightMitten : model.leftMitten;

        matrixStack.pushPose();

        // 1. POSITIONING: Align with the arm's current animation frame
        armPart.translateAndRotate(matrixStack);

        // 2. FINE-TUNING: Slightly offset to better fit the first-person perspective
        matrixStack.translate(-0.3F, 0.1F, 0.0F);

        // 3. RENDERING: Draw the mitten geometry
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        mittenPart.render(matrixStack, buffer, light, OverlayTexture.NO_OVERLAY, finalARGB);

        matrixStack.popPose();
    }
}