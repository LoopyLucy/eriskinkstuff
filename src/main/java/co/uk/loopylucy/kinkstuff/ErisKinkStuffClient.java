package co.uk.loopylucy.kinkstuff;

import co.uk.loopylucy.kinkstuff.client.*;
import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.item.items.CollarItem;
import co.uk.loopylucy.kinkstuff.item.items.MittensItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import top.theillusivec4.curios.api.CuriosApi;
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
            CuriosRendererRegistry.register(ModItems.MITTENS.get(), () -> new MittensRenderer(
                    new MittensModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.MITTENS))
            ));
        });
        ErisKinkStuff.LOGGER.info("Client Loaded and Renderer Registered!");
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        ClientAccess.MITTENS_MODEL = new MittensModel(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.MITTENS)
        );
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || ClientAccess.MITTENS_MODEL == null) return;

        // Fetch your inventory handler using the new API
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            inventory.findFirstCurio(ModItems.MITTENS.get()).ifPresent(slotResult -> {

                // Call our new First-Person Mitten handler
                FirstPersonMittenRenderer.renderFromArmEvent(
                        event,
                        slotResult.stack(),
                        ClientAccess.MITTENS_MODEL
                );
            });
        });
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

        event.register((itemStack, tintIndex) -> {
            if (itemStack.getItem() instanceof MittensItem mittens && tintIndex == 0) {
                int colour = mittens.getColor(itemStack);
                return (colour == 0xFFFFFF) ? 0xFFFFFFFF : (0xFF000000 | colour);
            }
            return -1;
        }, ModItems.MITTENS.get());

        ErisKinkStuff.LOGGER.info("Colour Handler Registered!");
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.COLLAR, CollarModel::createLayer);
        event.registerLayerDefinition(ModModelLayers.MITTENS, MittensModel::createLayer);
        ErisKinkStuff.LOGGER.info("Collar Layer Registered!");
    }
}