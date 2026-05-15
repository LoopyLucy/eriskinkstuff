package co.uk.loopylucy.kinkstuff.init;

import co.uk.loopylucy.kinkstuff.block.ModBlocks;
import co.uk.loopylucy.kinkstuff.block.entity.PetBedBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    // 1. Instantiates the authoritative NeoForge registry for Block Entities
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "eriskinkstuff");

    // 2. Registers your custom PetBed entity type and binds it directly to your Pet Bed Block
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PetBedBlockEntity>> PET_BED_BE =
            BLOCK_ENTITIES.register("pet_bed", () -> BlockEntityType.Builder.of(
                    PetBedBlockEntity::new, ModBlocks.PET_BED.get()).build(null));

    // 3. Simple registration initializer method
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
