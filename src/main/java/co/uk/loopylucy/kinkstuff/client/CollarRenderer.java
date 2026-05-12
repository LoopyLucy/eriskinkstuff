package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.item.items.CollarItem;
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

public class CollarRenderer implements ICurioRenderer {
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "textures/entity/dyeable_collar_base.png");
    private static final ResourceLocation PENDANT_TEXTURE = ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "textures/entity/gold_collar.png");
    private final CollarModel model;

    public CollarRenderer(CollarModel model) {
        this.model = model;
    }

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

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            // Copy state from the entity model so animations match
            this.model.attackTime = humanoidModel.attackTime;
            this.model.riding = humanoidModel.riding;
            this.model.young = humanoidModel.young;
            this.model.leftArmPose = humanoidModel.leftArmPose;
            this.model.rightArmPose = humanoidModel.rightArmPose;
            this.model.crouching = humanoidModel.crouching;
            this.model.body.visible = true;

            //ErisKinkStuff.LOGGER.debug("CollarRenderer.render called for entity {}", slotContext.entity());

            // 1. Setup the color
            int colour = ((CollarItem)stack.getItem()).getColor(stack);
            int finalARGB = 0xFF000000 | (colour & 0xFFFFFF);

            // 2. Sync animations
            this.model.prepareMobModel(slotContext.entity(), limbSwing, limbSwingAmount, partialTicks);
            this.model.setupAnim(slotContext.entity(), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            // 3. Parent to body
            matrixStack.pushPose();
            this.model.body.translateAndRotate(matrixStack);

            VertexConsumer baseVertexConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutout(BASE_TEXTURE));
            this.model.dyeableParts.render(matrixStack, baseVertexConsumer, light, OverlayTexture.NO_OVERLAY, finalARGB);

            matrixStack.translate(0.0F, -0.02F, 0.0F);
            matrixStack.scale(0.6F, 0.6F, 1.0F);
            VertexConsumer pendantVertexConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutout(PENDANT_TEXTURE));
            this.model.staticParts.render(matrixStack, pendantVertexConsumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

            matrixStack.popPose();
        }
    }
}
