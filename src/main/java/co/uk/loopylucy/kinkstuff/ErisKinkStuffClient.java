package co.uk.loopylucy.kinkstuff;

import co.uk.loopylucy.kinkstuff.client.*;
import co.uk.loopylucy.kinkstuff.client.models.MittensModel;
import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.item.items.ClickerItem;
import co.uk.loopylucy.kinkstuff.item.items.CollarItem;
import co.uk.loopylucy.kinkstuff.item.items.MittensItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * The main client-side mod class.
 * Handles the registration of renderers, models, and color handlers that are only 
 * relevant on the logical client.
 */
@Mod(value = ErisKinkStuff.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ErisKinkStuff.MODID, value = Dist.CLIENT)
public class ErisKinkStuffClient {

    /**
     * Handles client-side initialization, such as registering Curios renderers.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork( () -> {
            // Register renderers for items that appear on the player model
            CuriosRendererRegistry.register(ModItems.COLLAR.get(), () -> new CollarRenderer(ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "item/models/collar_model")));
            CuriosRendererRegistry.register(ModItems.COLLAR_TEST.get(), () -> new CollarRenderer(ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "item/models/collar_model")));
            CuriosRendererRegistry.register(ModItems.MITTENS.get(), () -> new MittensRenderer(
                    new MittensModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.MITTENS))
            ));
        });
        ErisKinkStuff.LOGGER.info("Client Loaded and Renderer Registered!");
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
     * Registers color handlers for dyeable items on the client.
     */
    @SubscribeEvent
    public static void onItemColorHandler(RegisterColorHandlersEvent.Item event) {
        ErisKinkStuff.LOGGER.info("Colour Handler Started!");
        
        // Collar color handler
        event.register((itemStack, tintIndex) -> {
            if (itemStack.getItem() instanceof CollarItem collar && tintIndex == 0) {
                int colour = collar.getColor(itemStack);
                return (colour == 0xFFFFFF) ? 0xFFFFFFFF : (0xFF000000 | colour);
            }
            return -1;
        },
                ModItems.COLLAR.get(),
                ModItems.COLLAR_TEST.get()
        );

        event.register((itemStack, tintIndex) -> {
            if (itemStack.getItem() instanceof CollarItem collar && tintIndex == 0) {
                int colour = collar.getColor(itemStack);
                return (colour == 0xFFFFFF) ? 0xFFFFFFFF : (0xFF000000 | colour);
            }
            return -1;
        }, ModItems.COLLAR_TEST.get());

        // Mittens color handler
        event.register((itemStack, tintIndex) -> {
            if (itemStack.getItem() instanceof MittensItem mittens && tintIndex == 0) {
                int colour = mittens.getColor(itemStack);
                return (colour == 0xFFFFFF) ? 0xFFFFFFFF : (0xFF000000 | colour);
            }
            return -1;
        }, ModItems.MITTENS.get());

        // Clicker color handler
        event.register((itemStack, tintIndex) -> {
            if (itemStack.getItem() instanceof ClickerItem collar && tintIndex == 0) {
                int colour = collar.getColor(itemStack);
                return (colour == 0xFFFFFF) ? 0xFFFFFFFF : (0xFF000000 | colour);
            }
            return -1;
        }, ModItems.CLICKER.get());

        ErisKinkStuff.LOGGER.info("Colour Handler Registered!");
    }

    /**
     * Registers custom model layer definitions.
     */
    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.MITTENS, MittensModel::createLayer);
        ErisKinkStuff.LOGGER.info("Collar Layer Registered!");
    }
}