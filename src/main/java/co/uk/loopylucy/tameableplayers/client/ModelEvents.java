package co.uk.loopylucy.tameableplayers.client;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = TameablePlayers.MODID, value = Dist.CLIENT)
public class ModelEvents {

    public static ResourceLocation COLLAR_MODEL =
            ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "item/models/collar_model");

    public static ResourceLocation BLINDFOLD_MODEL =
            ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "item/models/blindfold_model");

    public static ResourceLocation CAT_EARS_MODEL =
            ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "item/models/cat_ears_model");

    /**
     * Initializes additional model JSON files for rendering
     */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(COLLAR_MODEL));
        event.register(ModelResourceLocation.standalone(BLINDFOLD_MODEL));
        event.register(ModelResourceLocation.standalone(CAT_EARS_MODEL));
    }
}
