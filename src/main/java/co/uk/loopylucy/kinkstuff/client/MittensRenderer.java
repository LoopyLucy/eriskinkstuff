package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.client.models.MittensModel;
import co.uk.loopylucy.kinkstuff.item.items.MittensItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Custom renderer for the Mittens curio item.
 * This class handles the actual drawing of the mittens onto the player's arms,
 * including applying the custom dye color.
 */
public class MittensRenderer implements ICurioRenderer {
    /** Texture for the dyeable mittens. */
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "textures/entity/mittens_dyeable.png");
    
    private final MittensModel model;

    public MittensRenderer(MittensModel model) {
        this.model = model;
    }

    /**
     * The main rendering method called by the Curios API.
     */
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack matrixStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource renderTypeBuffer,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        // Ensure the parent model is a HumanoidModel (standard player model)
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            // Synchronize our mittens model's animation state with the player's model
            this.model.attackTime = humanoidModel.attackTime;
            this.model.riding = humanoidModel.riding;
            this.model.young = humanoidModel.young;
            this.model.leftArmPose = humanoidModel.leftArmPose;
            this.model.rightArmPose = humanoidModel.rightArmPose;
            this.model.crouching = humanoidModel.crouching;
            this.model.body.visible = true;

            // Fetch the dye color from the item stack
            int colour = ((MittensItem)stack.getItem()).getColor(stack);
            int finalARGB = 0xFF000000 | (colour & 0xFFFFFF);

            // Prepare and set up animations
            this.model.prepareMobModel(slotContext.entity(), limbSwing, limbSwingAmount, partialTicks);
            this.model.setupAnim(slotContext.entity(), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutout(BASE_TEXTURE));

            // 1. RENDER ON LEFT ARM
            matrixStack.pushPose();
            this.model.leftArm.translateAndRotate(matrixStack);
            this.model.leftMitten.render(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, finalARGB);
            matrixStack.popPose();

            // 2. RENDER ON RIGHT ARM
            matrixStack.pushPose();
            this.model.rightArm.translateAndRotate(matrixStack);
            this.model.rightMitten.render(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, finalARGB);
            matrixStack.popPose();
        }
    }
}