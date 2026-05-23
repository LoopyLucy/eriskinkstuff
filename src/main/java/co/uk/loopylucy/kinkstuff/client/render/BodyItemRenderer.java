package co.uk.loopylucy.kinkstuff.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Custom renderer for items worn on the player's body.
 */
public class BodyItemRenderer implements ICurioRenderer {

    private final ResourceLocation modelLocation;
    public BodyItemRenderer(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
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
            BakedModel baseModel = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(this.modelLocation));

            BakedModel finalModel = baseModel.getOverrides().resolve(
                    baseModel,
                    stack,
                    (ClientLevel) slotContext.entity().level(),
                    slotContext.entity(),
                    slotContext.entity().getId()
            );
            if (finalModel == null) finalModel = baseModel;

            matrixStack.pushPose();
            humanoidModel.body.translateAndRotate(matrixStack);
            matrixStack.translate(0.0F, 0.54F, 0.0F);
            matrixStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            float scale = 1.0F;
            matrixStack.scale(scale, scale, scale);

            Minecraft.getInstance().getItemRenderer().render(
                    stack,
                    ItemDisplayContext.NONE,
                    false,
                    matrixStack,
                    renderTypeBuffer,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    finalModel
            );

            matrixStack.popPose();
        }
    }
}