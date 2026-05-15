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

/**
 * Custom renderer for the Collar curio item.
 * This class handles the actual drawing of the collar onto the player's model,
 * including applying the custom dye color and rendering the pendant.
 */
public class CollarRenderer implements ICurioRenderer {
    /** Texture for the dyeable base of the collar. */
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "textures/entity/dyeable_collar_base.png");
    /** Texture for the static gold pendant. */
    private static final ResourceLocation PENDANT_TEXTURE = ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "textures/entity/gold_collar.png");
    
    private final CollarModel model;

    public CollarRenderer(CollarModel model) {
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
            // Synchronize our collar model's animation state with the player's model
            this.model.attackTime = humanoidModel.attackTime;
            this.model.riding = humanoidModel.riding;
            this.model.young = humanoidModel.young;
            this.model.leftArmPose = humanoidModel.leftArmPose;
            this.model.rightArmPose = humanoidModel.rightArmPose;
            this.model.crouching = humanoidModel.crouching;
            this.model.body.visible = true;

            // Fetch the dye color from the item stack
            int colour = ((CollarItem)stack.getItem()).getColor(stack);
            int finalARGB = 0xFF000000 | (colour & 0xFFFFFF);

            // Prepare and set up animations
            this.model.prepareMobModel(slotContext.entity(), limbSwing, limbSwingAmount, partialTicks);
            this.model.setupAnim(slotContext.entity(), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            matrixStack.pushPose();
            // Move our model to align with the player's body
            this.model.body.translateAndRotate(matrixStack);

            // 1. RENDER DYEABLE BASE
            VertexConsumer baseVertexConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutout(BASE_TEXTURE));
            this.model.dyeableParts.render(matrixStack, baseVertexConsumer, light, OverlayTexture.NO_OVERLAY, finalARGB);

            // 2. RENDER STATIC PENDANT
            // Slightly offset and scale the pendant for better visual alignment
            matrixStack.translate(0.0F, -0.02F, 0.0F);
            matrixStack.scale(0.6F, 0.6F, 1.0F);
            VertexConsumer pendantVertexConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutout(PENDANT_TEXTURE));
            this.model.staticParts.render(matrixStack, pendantVertexConsumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

            matrixStack.popPose();
        }
    }
}