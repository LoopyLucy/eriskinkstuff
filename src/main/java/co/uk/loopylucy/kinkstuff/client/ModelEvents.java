package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = ErisKinkStuff.MODID, value = Dist.CLIENT)
public class ModelEvents {

    public static ResourceLocation COLLAR_MODEL =
            ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "item/models/collar_model");

    public static ResourceLocation BLINDFOLD_MODEL =
            ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "item/models/blindfold_model");

    /**
     * Initializes additional model JSON files for rendering
     */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(COLLAR_MODEL));
        event.register(ModelResourceLocation.standalone(BLINDFOLD_MODEL));
    }
}
