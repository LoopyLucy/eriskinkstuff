package co.uk.loopylucy.kinkstuff;

import co.uk.loopylucy.kinkstuff.registration.ModBlocks;
import co.uk.loopylucy.kinkstuff.registration.ModBlockEntities;
import co.uk.loopylucy.kinkstuff.registration.ModDataComponents;
import co.uk.loopylucy.kinkstuff.registration.ModCreativeTabs;
import co.uk.loopylucy.kinkstuff.registration.ModRecipes;
import co.uk.loopylucy.kinkstuff.registration.ModItems;
import co.uk.loopylucy.kinkstuff.network.LeashServerPacket;
import co.uk.loopylucy.kinkstuff.network.LeashSyncPacket;
import co.uk.loopylucy.kinkstuff.registration.ModSounds;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * The main mod class for Eri's Kink Stuff.
 * This class handles the initialization of the mod, registration of all modded objects
 * (items, blocks, sounds, etc.), and sets up capabilities and networking.
 */
@Mod(ErisKinkStuff.MODID)
public class ErisKinkStuff {
    public static final String MODID = "eriskinkstuff";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ErisKinkStuff(IEventBus modEventBus, ModContainer modContainer) {
        // Register lifecycle and setup listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPackets);


        // Register all modded content
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSounds.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);

        // Register the mod instance to the main NeoForge event bus
        NeoForge.EVENT_BUS.register(this);

        // Setup configuration
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Client-only setup for config screens
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Common Loaded!");
    }

    /**
     * Registers network packets for client-server communication.
     */
    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(LeashSyncPacket.TYPE, LeashSyncPacket.CODEC, LeashSyncPacket::handle);
        registrar.playToServer(LeashServerPacket.TYPE, LeashServerPacket.CODEC, LeashServerPacket::handle);
        LOGGER.info("Packets Registered!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server Loaded!");
    }
}