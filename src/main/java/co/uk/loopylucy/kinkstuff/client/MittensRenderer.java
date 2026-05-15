package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
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

public class MittensRenderer implements ICurioRenderer {
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "textures/entity/mittens_dyeable.png");
    private final MittensModel model;

    public MittensRenderer(MittensModel model) {
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
            this.model.attackTime = humanoidModel.attackTime;
            this.model.riding = humanoidModel.riding;
            this.model.young = humanoidModel.young;
            this.model.leftArmPose = humanoidModel.leftArmPose;
            this.model.rightArmPose = humanoidModel.rightArmPose;
            this.model.crouching = humanoidModel.crouching;
            this.model.body.visible = true;


            int colour = ((MittensItem)stack.getItem()).getColor(stack);
            int finalARGB = 0xFF000000 | (colour & 0xFFFFFF);

            this.model.prepareMobModel(slotContext.entity(), limbSwing, limbSwingAmount, partialTicks);
            this.model.setupAnim(slotContext.entity(), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutout(BASE_TEXTURE));

            matrixStack.pushPose();
            this.model.leftArm.translateAndRotate(matrixStack);
            this.model.leftMitten.render(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, finalARGB);
            matrixStack.popPose();

            matrixStack.pushPose();
            this.model.rightArm.translateAndRotate(matrixStack);
            this.model.rightMitten.render(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, finalARGB);
            matrixStack.popPose();
        }
    }
}