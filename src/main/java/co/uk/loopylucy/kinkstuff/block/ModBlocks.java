package co.uk.loopylucy.kinkstuff.block;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.block.blocks.PetBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ErisKinkStuff.MODID);

    public static final DeferredBlock<Block> PET_BED = BLOCKS.registerBlock("pet_bed",
            PetBedBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(0.4F)
                    .sound(SoundType.WOOL)
                    .ignitedByLava()
            );

    public static void register(IEventBus eventBus) { BLOCKS.register(eventBus); }
}
