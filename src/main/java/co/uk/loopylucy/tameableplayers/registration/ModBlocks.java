package co.uk.loopylucy.tameableplayers.registration;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.common.block.PetBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for all modded blocks.
 * Uses DeferredRegister to ensure blocks are registered correctly.
 */
public class ModBlocks {
    /** The registry for blocks. */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TameablePlayers.MODID);

    /** 
     * The Pet Bed block. 
     * It is a soft, wool-like block with low strength and custom behaviour.
     */
    public static final DeferredBlock<Block> PET_BED = BLOCKS.registerBlock("pet_bed",
            PetBedBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(0.4F)
                    .sound(SoundType.WOOL)
                    .ignitedByLava()
            );

    /**
     * Entry point for registering blocks to the mod event bus.
     * 
     * @param eventBus The mod event bus.
     */
    public static void register(IEventBus eventBus) { BLOCKS.register(eventBus); }
}