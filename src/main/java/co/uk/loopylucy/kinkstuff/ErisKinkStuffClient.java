package co.uk.loopylucy.kinkstuff;

import co.uk.loopylucy.kinkstuff.client.CollarModel;
import co.uk.loopylucy.kinkstuff.client.CollarRenderer;
import co.uk.loopylucy.kinkstuff.client.ModModelLayers;
import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.item.items.CollarItem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
@Mod(value = ErisKinkStuff.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ErisKinkStuff.MODID, value = Dist.CLIENT)
public class ErisKinkStuffClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork( () -> {
            CuriosRendererRegistry.register(ModItems.COLLAR.get(), () -> new CollarRenderer(
                    new CollarModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.COLLAR))
            ));
        });
        ErisKinkStuff.LOGGER.info("Client Loaded and Renderer Registered!");
    }

    @SubscribeEvent
    public static void onItemColorHandler(RegisterColorHandlersEvent.Item event) {
        ErisKinkStuff.LOGGER.info("Colour Handler Started!");
        event.register((itemStack, tintIndex) -> {
            if (itemStack.getItem() instanceof CollarItem collar && tintIndex == 0) {
                int colour = collar.getColor(itemStack);
                return (colour == 0xFFFFFF) ? 0xFFFFFFFF : (0xFF000000 | colour);
            }
            return -1;
        }, ModItems.COLLAR.get());
        ErisKinkStuff.LOGGER.info("Colour Handler Registered!");
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.COLLAR, CollarModel::createLayer);
        ErisKinkStuff.LOGGER.info("Collar Layer Registered!");
    }
}
