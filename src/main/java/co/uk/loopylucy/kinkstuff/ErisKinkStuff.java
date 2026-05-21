package co.uk.loopylucy.kinkstuff;

import co.uk.loopylucy.kinkstuff.block.ModBlocks;
import co.uk.loopylucy.kinkstuff.block.blocks.PetBedBlock;
import co.uk.loopylucy.kinkstuff.block.entity.PetBedBlockEntity;
import co.uk.loopylucy.kinkstuff.init.ModBlockEntities;
import co.uk.loopylucy.kinkstuff.init.ModComponents;
import co.uk.loopylucy.kinkstuff.init.ModCreativeTabs;
import co.uk.loopylucy.kinkstuff.init.ModRecipes;
import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.network.LeashServerPacket;
import co.uk.loopylucy.kinkstuff.network.LeashSyncPacket;
import co.uk.loopylucy.kinkstuff.sound.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
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
        modEventBus.addListener(this::registerBlockColors);
        modEventBus.addListener(this::registerItemColors);

        // Register all modded content
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSounds.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);

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

    /**
     * Handles block color registration for dyeable blocks like the Pet Bed.
     */
    private void registerBlockColors(RegisterColorHandlersEvent.Block event) {
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
     * Handles item color registration for dyeable items.
     */
    private void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
                return dyedColor != null ? dyedColor.rgb() : 0xFFFFFF;
            }
            return -1;
        }, ModItems.PET_BED.get());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server Loaded!");
    }
}