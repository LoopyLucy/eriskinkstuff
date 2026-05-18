package co.uk.loopylucy.kinkstuff.client;

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
 * Custom renderer for the Collar curio item.
 * This refactored version pulls the JSON model directly from the ItemStack,
 * matching whatever variant texture/model is assigned during registration.
 */
public class CollarRenderer implements ICurioRenderer {

    private final ResourceLocation modelLocation;
    public CollarRenderer(ResourceLocation modelLocation) {
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

        // Ensure the parent model is a HumanoidModel (standard player model) so we can attach to the chest/body
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

            // 1. PIN TO THE PLAYER'S BODY BONE
            // This synchronizes sneaking, animations, and rotations perfectly automatically.
            humanoidModel.body.translateAndRotate(matrixStack);

            // 2. CORRECTION MATRIX FOR JSON ITEM MODELS
            // JSON models are engineered assuming a 16x16x16 block grid.
            // When rendered directly as an item onto an entity, we have to scale and center it manually
            // so it sits beautifully around the neck.

            // Centering step: Slide the 16x16 grid so its origin is at the player's neck center
            // (You may need to tweak this Y-offset slightly depending on your exact JSON model placement)
            matrixStack.translate(0.0F, 0.54F, 0.0F);

            // Re-orient the JSON model to face forward
            matrixStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            // Scale the JSON block units down to match player entity scales
            // (Standard JSON blocks are roughly 1.0F wide, we need it scaled to the neck)
            float scale = 1.0F;
            matrixStack.scale(scale, scale, scale);

            // 3. THE MAGIC: RENDER THE JSON ITEM MODEL
            // Uses NONE display context to render the pure model geometry with all its defined layers.
            // Note: Color tinting is handled automatically here if you registered an ItemColor for your CollarItem.
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