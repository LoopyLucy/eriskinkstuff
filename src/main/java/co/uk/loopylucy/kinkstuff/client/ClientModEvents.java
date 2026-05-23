package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.client.model.MittensModel;
import co.uk.loopylucy.kinkstuff.client.render.BodyItemRenderer;
import co.uk.loopylucy.kinkstuff.client.render.HeadItemRenderer;
import co.uk.loopylucy.kinkstuff.client.render.MittensRenderer;
import co.uk.loopylucy.kinkstuff.client.render.SkinOverlayRenderer;
import co.uk.loopylucy.kinkstuff.common.block.PetBedBlock;
import co.uk.loopylucy.kinkstuff.common.block.entity.PetBedBlockEntity;
import co.uk.loopylucy.kinkstuff.common.item.*;
import co.uk.loopylucy.kinkstuff.registration.ModBlocks;
import co.uk.loopylucy.kinkstuff.registration.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.util.function.Function;

@EventBusSubscriber(modid = ErisKinkStuff.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    private static final ResourceLocation BLINDFOLD_OVERLAY = ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "textures/misc/blindfold_overlay.png");

    /**
     * Handles client-side initialization, such as registering Curios renderers.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork( () -> {
            // Register renderers for items that appear on the player model
            CuriosRendererRegistry.register(ModItems.COLLAR.get(), () -> new BodyItemRenderer(ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "item/models/collar_model")));
            CuriosRendererRegistry.register(ModItems.BLINDFOLD.get(), () -> new HeadItemRenderer(ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "item/models/blindfold_model"), 0.0F, 1.0F));
            CuriosRendererRegistry.register(ModItems.MITTENS.get(), () -> new MittensRenderer( new MittensModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.MITTENS))));
            CuriosRendererRegistry.register(ModItems.LATEX_BODYSUIT.get(), () -> new SkinOverlayRenderer(ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "textures/entity/latex_bodysuit.png")));
            CuriosRendererRegistry.register(ModItems.CAT_EARS.get(), () -> new HeadItemRenderer(null, 1.45F, 0.4F));
        });
        ErisKinkStuff.LOGGER.info("Client Loaded and Renderers Registered!");
    }

    /**
     * Registers custom model layer definitions.
     */
    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.MITTENS, MittensModel::createLayer);
        ErisKinkStuff.LOGGER.info("Mod Layers Registered!");
    }

    /**
     * Registers GUI Layers
     */
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                    VanillaGuiLayers.CAMERA_OVERLAYS,
                    ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "blindfold_overlay"),
                    ((guiGraphics, deltaTracker) -> renderBlindfold(guiGraphics))
                );
    }

    /**
     * Blindfold layer drawing helper method
     */
    private static void renderBlindfold(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (mc.player.isSpectator()) return;

        CuriosApi.getCuriosInventory(mc.player).ifPresent(handler -> {
            if (handler.findFirstCurio(ModItems.BLINDFOLD.get()).isPresent()) {
                int width = guiGraphics.guiWidth();
                int height = guiGraphics.guiHeight();

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                guiGraphics.blit(
                        BLINDFOLD_OVERLAY,
                        0, 0,
                        -90,
                        0.0F, 0.0F,
                        width, height,
                        width, height
                );

                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
            }
        });
    }

    /**
     * Initializes static model references for use in custom rendering logic.
     */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        ClientAccess.MITTENS_MODEL = new MittensModel(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.MITTENS)
        );
    }

    /**
     * Registers colour handlers for dyeable items on the client.
     */
    @SubscribeEvent
    public static void onItemColorHandler(RegisterColorHandlersEvent.Item event) {
        ErisKinkStuff.LOGGER.info("Colour Handler Started!");

        registerColour(event, ModItems.COLLAR.get(), stack -> ((CollarItem) stack.getItem()).getColour(stack));
        registerColour(event, ModItems.MITTENS.get(), stack -> ((MittensItem) stack.getItem()).getColour(stack));
        registerColour(event, ModItems.CLICKER.get(), stack -> ((ClickerItem) stack.getItem()).getColour(stack));
        registerColour(event, ModItems.LATEX_BODYSUIT.get(), stack -> ((LatexBodysuitItem) stack.getItem()).getColour(stack));
        registerColour(event, ModItems.BLINDFOLD.get(), stack -> ((BlindfoldItem) stack.getItem()).getColour(stack));
        registerColour(event, ModItems.CAT_EARS.get(), stack -> ((CatEarsItem) stack.getItem()).getColour(stack));

        ErisKinkStuff.LOGGER.info("Colour Handler Registered!");
    }

    private static void registerColour(RegisterColorHandlersEvent.Item event, Item item, Function<ItemStack, Integer> colourGetter) {
        event.register((itemStack, tintIndex) -> {
            if (tintIndex != 0) return -1;

            int colour = colourGetter.apply(itemStack);
            return (colour == 0xFFFFFF) ? 0xFFFFFFFF : (0xFF000000 | colour);
        }, item);
    }

    /**
     * Handles block colour registration for dyeable blocks like the Pet Bed.
     */
    @SubscribeEvent
    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                // Logic to resolve the 'origin' position of a multi-block Pet Bed
                net.minecraft.core.Direction facing = state.getValue(PetBedBlock.FACING);
                int x = state.getValue(PetBedBlock.X_PART);
                int z = state.getValue(PetBedBlock.Z_PART);

                net.minecraft.core.BlockPos gridShift = switch (facing) {
                    case NORTH -> net.minecraft.core.BlockPos.ZERO.east(x).south(z);
                    case SOUTH -> net.minecraft.core.BlockPos.ZERO.west(x).north(z);
                    case WEST  -> net.minecraft.core.BlockPos.ZERO.north(x).east(z);
                    case EAST  -> net.minecraft.core.BlockPos.ZERO.south(x).west(z);
                    default    -> net.minecraft.core.BlockPos.ZERO;
                };

                net.minecraft.core.BlockPos originPos = pos.subtract(gridShift);

                // Fetch the custom color from the BlockEntity
                if (level.getBlockEntity(originPos) instanceof PetBedBlockEntity bedBE) {
                    return bedBE.getCustomColour();
                }
            }
            return -1;
        }, ModBlocks.PET_BED.get());
    }

    /**
     * Handles item colour registration for dyeable items.
     */
    @SubscribeEvent
    private static void registerBlockItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
                return dyedColor != null ? dyedColor.rgb() : 0xFFFFFF;
            }
            return -1;
        }, ModItems.PET_BED.get());
    }
}