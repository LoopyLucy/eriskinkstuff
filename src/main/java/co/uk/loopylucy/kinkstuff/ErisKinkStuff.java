package co.uk.loopylucy.kinkstuff;

import co.uk.loopylucy.kinkstuff.block.ModBlocks;
import co.uk.loopylucy.kinkstuff.block.blocks.PetBedBlock;
import co.uk.loopylucy.kinkstuff.block.entity.PetBedBlockEntity;
import co.uk.loopylucy.kinkstuff.init.ModBlockEntities;
import co.uk.loopylucy.kinkstuff.init.ModCreativeTabs;
import co.uk.loopylucy.kinkstuff.init.ModDataComponents;
import co.uk.loopylucy.kinkstuff.item.ModItems;
import co.uk.loopylucy.kinkstuff.network.LeashServerPacket;
import co.uk.loopylucy.kinkstuff.network.LeashSyncPacket;
import co.uk.loopylucy.kinkstuff.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;

@Mod(ErisKinkStuff.MODID)
public class ErisKinkStuff {
    public static final String MODID = "eriskinkstuff";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ErisKinkStuff(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPackets);
        modEventBus.addListener(this::registerBlockColors);
        modEventBus.addListener(this::registerItemColors);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSounds.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);

        ModCreativeTabs.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Common Loaded!");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(CuriosCapability.ITEM, (stack, context) -> CuriosApi.getCurio(stack).orElse(null), ModItems.COLLAR.get());
        event.registerItem(CuriosCapability.ITEM, (stack, context) -> CuriosApi.getCurio(stack).orElse(null), ModItems.MITTENS.get());
        LOGGER.info("Capabilities Registered!");
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID);

        registrar.playToClient(LeashSyncPacket.TYPE, LeashSyncPacket.CODEC, LeashSyncPacket::handle);
        registrar.playToServer(LeashServerPacket.TYPE, LeashServerPacket.CODEC, LeashServerPacket::handle);
        LOGGER.info("Packets Registered!");
    }

    private void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                // Read local properties for the specific piece Minecraft is trying to draw right now
                net.minecraft.core.Direction facing = state.getValue(PetBedBlock.FACING);
                int x = state.getValue(PetBedBlock.X_PART);
                int z = state.getValue(PetBedBlock.Z_PART);

                // Inline coordinate translator: Safely mimics your Java class matrix math
                net.minecraft.core.BlockPos gridShift = switch (facing) {
                    case NORTH -> net.minecraft.core.BlockPos.ZERO.east(x).south(z);
                    case SOUTH -> net.minecraft.core.BlockPos.ZERO.west(x).north(z);
                    case WEST  -> net.minecraft.core.BlockPos.ZERO.north(x).east(z);
                    case EAST  -> net.minecraft.core.BlockPos.ZERO.south(x).west(z);
                    default    -> net.minecraft.core.BlockPos.ZERO;
                };

                // Pinpoint exactly where the master data entity is located in the world
                net.minecraft.core.BlockPos originPos = pos.subtract(gridShift);

                // Pull the custom color from the master entity and apply it to this dummy piece
                if (level.getBlockEntity(originPos) instanceof PetBedBlockEntity bedBE) {
                    return bedBE.getCustomColour();
                }
            }
            return -1; // Default fallback tint if no data is found
        }, ModBlocks.PET_BED.get());
    }


    // 2. INTEGRATED ITEM COLOR HANDLER: No extra classes needed!
    private void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
                return dyedColor != null ? dyedColor.rgb() : 0xFFFFFF;
            }
            return -1;
        }, ModItems.PET_BED.get()); // Make sure this matches your exact key name in ModItems
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server Loaded!");
    }

}