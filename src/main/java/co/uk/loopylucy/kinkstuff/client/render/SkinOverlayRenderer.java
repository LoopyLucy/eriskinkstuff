package co.uk.loopylucy.kinkstuff.client.render;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.registration.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = ErisKinkStuff.MODID, value = Dist.CLIENT)
public class SkinOverlayRenderer implements ICurioRenderer {

    private final ResourceLocation texture;

    // Reflection cache
    private static boolean reflectionInitialized = false;
    private static Field layersField = null;
    private static Class<?> discoveredGenderLayerClass = null;
    private static Method layerRenderMethod = null;
    private static Field genderModelField = null;
    private static Field modelPartChildrenField = null;
    private static EntityDataAccessor<Byte> CUSTOMISATION_ACCESSOR = null;
    private static boolean discoveryFailed = false;

    // State tracking for restoring player skin settings
    private static final Map<UUID, Byte> originalSkinParts = new HashMap<>();
    
    // Safety set to prevent infinite recursion
    private static final Set<Object> visitedObjects = Collections.synchronizedSet(new HashSet<>());

    public SkinOverlayRenderer(ResourceLocation texture) {
        this.texture = texture;
    }

    /**
     * Hides the player's outer layers (jacket, sleeves, pants, hat) if an item with the hideLayers flag is equipped.
     * This is registered to the NeoForge event bus.
     */
    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            try {
                // Check if any equipped curio has the OVERLAY_INFO component and wants to hide layers
                if (handler.findFirstCurio(stack -> {
                    ModDataComponents.OverlayInfo info = stack.get(ModDataComponents.OVERLAY_INFO);
                    return info != null && info.hideLayers();
                }).isPresent()) {
                    // 1. DataTracker Hiding (Tells mods/vanilla that overlays are disabled)
                    try {
                        if (CUSTOMISATION_ACCESSOR == null) {
                            for (Field f : Player.class.getDeclaredFields()) {
                                if (Modifier.isStatic(f.getModifiers()) && f.getType() == EntityDataAccessor.class) {
                                    f.setAccessible(true);
                                    EntityDataAccessor<?> acc = (EntityDataAccessor<?>) f.get(null);
                                    // Customisation is a Byte accessor. We look for the common names.
                                    if (f.getName().equals("DATA_PLAYER_MODE_CUSTOMISATION") || f.getName().equals("f_36220_")) {
                                        CUSTOMISATION_ACCESSOR = (EntityDataAccessor<Byte>) acc;
                                        break;
                                    }
                                }
                            }
                        }
                        if (CUSTOMISATION_ACCESSOR != null) {
                            originalSkinParts.put(player.getUUID(), player.getEntityData().get(CUSTOMISATION_ACCESSOR));
                            player.getEntityData().set(CUSTOMISATION_ACCESSOR, (byte)0);
                        }
                    } catch (Exception ignored) {}

                    PlayerModel<?> model = event.getRenderer().getModel();
                    model.jacket.visible = false;
                    model.rightSleeve.visible = false;
                    model.leftSleeve.visible = false;
                    model.rightPants.visible = false;
                    model.leftPants.visible = false;
                    model.hat.visible = false;

                    // Aggressively scan for modded overlay parts
                    visitedObjects.clear();
                    scanAndHide(model.head);
                    scanAndHide(model.body);
                    scanAndHide(model.rightArm);
                    scanAndHide(model.leftArm);
                    scanAndHide(model.rightLeg);
                    scanAndHide(model.leftLeg);

                    // Scan Gender Mod layers specifically
                    if (layersField != null) {
                        List<RenderLayer<?, ?>> layers = (List<RenderLayer<?, ?>>) layersField.get(event.getRenderer());
                        if (layers != null) {
                            for (RenderLayer<?, ?> layer : layers) {
                                if (discoveredGenderLayerClass != null && discoveredGenderLayerClass.isInstance(layer)) {
                                    if (genderModelField != null) {
                                        Object genderModel = genderModelField.get(layer);
                                        scanAndHide(genderModel);
                                    }
                                    scanAndHide(layer);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            finally {
                visitedObjects.clear();
            }
        });
    }

    /**
     * Restores visibility states.
     */
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        Byte original = originalSkinParts.remove(player.getUUID());
        if (original != null && CUSTOMISATION_ACCESSOR != null) {
            try {
                player.getEntityData().set(CUSTOMISATION_ACCESSOR, original);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Entry point for recursive scanning.
     */
    private static void scanAndHide(Object obj) {
        if (obj == null || !visitedObjects.add(obj)) return;
        
        if (obj instanceof ModelPart part) {
            hideChildrenRecursive(part);
        } else {
            Class<?> current = obj.getClass();
            while (current != null && current != Object.class && !current.getName().startsWith("net.minecraft")) {
                try {
                    for (Field f : current.getDeclaredFields()) {
                        try {
                            if (ModelPart.class.isAssignableFrom(f.getType())) {
                                f.setAccessible(true);
                                ModelPart part = (ModelPart) f.get(obj);
                                if (part != null) {
                                    String name = f.getName().toLowerCase();
                                    if (isOverlayName(name)) {
                                        part.visible = false;
                                    }
                                    hideChildrenRecursive(part);
                                }
                            } else if (EntityModel.class.isAssignableFrom(f.getType()) || (f.getType().getName().contains("Model") && !f.getType().getName().contains("Resource"))) {
                                f.setAccessible(true);
                                Object next = f.get(obj);
                                if (next != null) scanAndHide(next);
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                current = current.getSuperclass();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void hideChildrenRecursive(ModelPart part) {
        if (part == null || !visitedObjects.add(part)) return;
        try {
            if (modelPartChildrenField == null) {
                try { modelPartChildrenField = ModelPart.class.getDeclaredField("children");
                } catch (NoSuchFieldException e) { ErisKinkStuff.LOGGER.warn("Unable to get children: ", e); }
                modelPartChildrenField.setAccessible(true);
            }
            
            Object childrenObj = modelPartChildrenField.get(part);
            if (childrenObj instanceof Map<?, ?> map) {
                // Copy map to avoid CME
                Map<String, ModelPart> children = new HashMap<>((Map<String, ModelPart>) map);
                for (Map.Entry<?, ?> entry : children.entrySet()) {
                    if (entry.getValue() instanceof ModelPart child) {
                        if (entry.getKey() instanceof String name && isOverlayName(name)) {
                            child.visible = false;
                        }
                        hideChildrenRecursive(child);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean isOverlayName(String name) {
        String lower = name.toLowerCase();
        return lower.contains("jacket") || lower.contains("overlay") || lower.contains("outer") || 
               lower.contains("top") || lower.contains("clothing") || lower.contains("shirt") ||
               lower.contains("cloak") || lower.contains("cape") || lower.contains("layer");
    }

    private static void initializeReflectionHooks(Object layerInstance) {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            try {
                layersField = net.minecraft.client.renderer.entity.LivingEntityRenderer.class.getDeclaredField("layers");
            } catch (NoSuchFieldException e) {
                ErisKinkStuff.LOGGER.warn("Unable to get layers: ", e);
            }
            layersField.setAccessible(true);

            Class<?> layerClass = layerInstance.getClass();
            discoveredGenderLayerClass = layerClass;

            // Try to find the model field inside the layer
            for (Field f : layerClass.getDeclaredFields()) {
                if (EntityModel.class.isAssignableFrom(f.getType()) || f.getType().getName().contains("Model")) {
                    f.setAccessible(true);
                    genderModelField = f;
                    break;
                }
            }

            Class<?> current = layerClass;
            while (current != null && current != Object.class) {
                for (Method method : current.getDeclaredMethods()) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length >= 5 && params[0] == PoseStack.class && params[1] == MultiBufferSource.class) {
                        method.setAccessible(true);
                        layerRenderMethod = method;
                        ErisKinkStuff.LOGGER.info("[KinkStuff] Bound to Gender Mod layer: {}", method.getName());
                        break;
                    }
                }
                if (layerRenderMethod != null) break;
                current = current.getSuperclass();
            }
        } catch (Exception e) {
            ErisKinkStuff.LOGGER.error("[KinkStuff] Hook init failed:", e);
            discoveryFailed = true;
        }
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

        // 1. Fetch Dynamic Component Info
        ModDataComponents.OverlayInfo info = stack.get(ModDataComponents.OVERLAY_INFO);
        ResourceLocation renderTexture = this.texture;
        int color = 0xFFFFFFFF;

        if (info != null) {
            if (info.texture() != null && !info.texture().isEmpty()) {
                renderTexture = ResourceLocation.parse(info.texture());
            }
            color = info.color();
        }

        // 2. Generic color logic: Prioritize standard DYED_COLOR component
        DyedItemColor dyedColor = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        if (dyedColor != null) {
            color = 0xFF000000 | dyedColor.rgb();
        }

        // Use the ARGB colour
        VertexConsumer tintedConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutoutNoCull(renderTexture));

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            // Now pass the tintedConsumer and colour to your render function
            renderInflatedPart(matrixStack, tintedConsumer, light, humanoidModel.head, 1.02F, color);
            renderInflatedPart(matrixStack, tintedConsumer, light, humanoidModel.hat, 1.025F, color);
            renderInflatedPart(matrixStack, tintedConsumer, light, humanoidModel.body, 1.02F, color);
            renderInflatedPart(matrixStack, tintedConsumer, light, humanoidModel.rightArm, 1.02F, color);
            renderInflatedPart(matrixStack, tintedConsumer, light, humanoidModel.leftArm, 1.02F, color);
            renderInflatedPart(matrixStack, tintedConsumer, light, humanoidModel.rightLeg, 1.02F, color);
            renderInflatedPart(matrixStack, tintedConsumer, light, humanoidModel.leftLeg, 1.02F, color);

            if (humanoidModel instanceof PlayerModel<?> playerModel) {
                renderInflatedPart(matrixStack, tintedConsumer, light, playerModel.jacket, 1.025F, color);
                renderInflatedPart(matrixStack, tintedConsumer, light, playerModel.rightSleeve, 1.025F, color);
                renderInflatedPart(matrixStack, tintedConsumer, light, playerModel.leftSleeve, 1.025F, color);
                renderInflatedPart(matrixStack, tintedConsumer, light, playerModel.rightPants, 1.025F, color);
                renderInflatedPart(matrixStack, tintedConsumer, light, playerModel.leftPants, 1.025F, color);
            }
        }

        if (slotContext.entity() instanceof Player player) {
            if (!discoveryFailed) {
                renderGenderModCompatibility(player, matrixStack, renderTypeBuffer, light,
                        limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, renderTexture, color);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void renderGenderModCompatibility(Player player, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int light,
                                              float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, ResourceLocation texture, int color) {
        try {
            Object abstractRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);

            if (abstractRenderer instanceof PlayerRenderer playerRenderer) {
                if (!reflectionInitialized) {
                    if (layersField == null) {
                        try { layersField = PlayerRenderer.class.getSuperclass().getDeclaredField("layers");
                        } catch (NoSuchFieldException e) { layersField = PlayerRenderer.class.getSuperclass().getDeclaredField("f_115312_"); }
                        layersField.setAccessible(true);
                    }
                    List<RenderLayer<?, ?>> layers = (List<RenderLayer<?, ?>>) layersField.get(playerRenderer);
                    for (RenderLayer<?, ?> layer : layers) {
                        String className = layer.getClass().getName();
                        if (className.contains("Gender") || className.contains("wildfire")) {
                            initializeReflectionHooks(layer);
                            break;
                        }
                    }
                }

                if (reflectionInitialized && !discoveryFailed && layerRenderMethod != null) {
                    List<RenderLayer<?, ?>> layers = (List<RenderLayer<?, ?>>) layersField.get(playerRenderer);
                    for (RenderLayer<?, ?> layer : layers) {
                        if (discoveredGenderLayerClass.isInstance(layer)) {

                            // Extract ARGB components
                            MultiBufferSource wrappedBuffer = getBufferSource(renderTypeBuffer, texture, color);

                            matrixStack.pushPose();
                            matrixStack.scale(1.022F, 1.022F, 1.022F);

                            Class<?>[] paramTypes = layerRenderMethod.getParameterTypes();
                            Object[] args = new Object[paramTypes.length];
                            args[0] = matrixStack;
                            args[1] = wrappedBuffer;
                            args[2] = light;
                            args[3] = player;
                            args[4] = limbSwing;
                            args[5] = limbSwingAmount;
                            args[6] = partialTicks;
                            args[7] = ageInTicks;
                            args[8] = netHeadYaw;
                            args[9] = headPitch;
                            for (int i = 10; i < paramTypes.length; i++) {
                                if (paramTypes[i] == int.class || paramTypes[i] == Integer.TYPE) args[i] = OverlayTexture.NO_OVERLAY;
                            }
                            layerRenderMethod.invoke(layer, args);
                            matrixStack.popPose();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            ErisKinkStuff.LOGGER.error("[KinkStuff] Error executing parent layer rendering override: ", e);
        }
    }

    private static @NotNull MultiBufferSource getBufferSource(MultiBufferSource renderTypeBuffer, ResourceLocation texture, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Use the dynamic texture location from the component and apply colour
        return renderType -> {
            VertexConsumer vc = renderTypeBuffer.getBuffer(RenderType.entityCutoutNoCull(texture));
            return new TintedVertexConsumer(vc, r, g, b, a);
        };
    }

    /**
     * Inner class to manually handle vertex tinting since DefaultedVertexConsumer is missing.
     */
    private record TintedVertexConsumer(VertexConsumer delegate, int r, int g, int b, int a) implements VertexConsumer {
        @Override
        public @NotNull VertexConsumer addVertex(float x, float y, float z) {
            return delegate.addVertex(x, y, z).setColor(r, g, b, a);
        }

        @Override
        public @NotNull VertexConsumer setColor(int r, int g, int b, int a) {
            // Apply our tint to the requested colour
            return delegate.setColor((r * this.r) / 255, (g * this.g) / 255, (b * this.b) / 255, (a * this.a) / 255);
        }

        @Override
        public @NotNull VertexConsumer setUv(float u, float v) {
            return delegate.setUv(u, v);
        }

        @Override
        public @NotNull VertexConsumer setUv1(int u, int v) {
            return delegate.setUv1(u, v);
        }

        @Override
        public @NotNull VertexConsumer setUv2(int u, int v) {
            return delegate.setUv2(u, v);
        }

        @Override
        public @NotNull VertexConsumer setNormal(float x, float y, float z) {
            return delegate.setNormal(x, y, z);
        }
    }

    private void renderInflatedPart(PoseStack matrixStack, VertexConsumer consumer, int light, ModelPart part, float scale, int color) {
        // We temporarily force the part to be visible so our overlay renders even if the base layer is hidden
        part.visible = true;

        matrixStack.pushPose();
        float origX = part.x;
        float origY = part.y;
        float origZ = part.z;

        part.x = origX / scale;
        part.y = origY / scale;
        part.z = origZ / scale;

        matrixStack.scale(scale, scale, scale);

        part.render(matrixStack, consumer, light, OverlayTexture.NO_OVERLAY, color);

        part.x = origX;
        part.y = origY;
        part.z = origZ;
        matrixStack.popPose();
    }
}
