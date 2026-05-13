package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
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

public class FirstPersonMittenRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ErisKinkStuff.MODID, "textures/entity/mittens_dyeable.png"
    );

    public static void renderFromArmEvent(RenderArmEvent event, ItemStack stack, MittensModel model) {
        // 1. CRITICAL SAFETY CHECK: Prevent AirItem casting crashes
        if (stack.isEmpty() || !(stack.getItem() instanceof MittensItem)) {
            return;
        }

        PoseStack matrixStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int light = event.getPackedLight();

        // 1. Setup the color
        int colour = ((MittensItem)stack.getItem()).getColor(stack);
        int finalARGB = 0xFF000000 | (colour & 0xFFFFFF);

        // 2. Identify the active arm being drawn by the game engine
        boolean isRightArm = (event.getArm() == HumanoidArm.RIGHT);

        // Select the correct model parts
        ModelPart armPart = isRightArm ? model.rightArm : model.leftArm;
        ModelPart mittenPart = isRightArm ? model.rightMitten : model.leftMitten;

        // 3. Render the Mitten
        matrixStack.pushPose();

        // This copies the current first-person swing/sway rotations onto your model's shoulder node
        armPart.translateAndRotate(matrixStack);

        matrixStack.translate(-0.3F, 0.1F, 0.0F);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        mittenPart.render(matrixStack, buffer, light, OverlayTexture.NO_OVERLAY, finalARGB);

        matrixStack.popPose();
    }
}