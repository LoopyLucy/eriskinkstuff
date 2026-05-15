package co.uk.loopylucy.kinkstuff.init;

import co.uk.loopylucy.kinkstuff.block.ModBlocks;
import co.uk.loopylucy.kinkstuff.block.entity.PetBedBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "eriskinkstuff");

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PetBedBlockEntity>> PET_BED_BE =
            BLOCK_ENTITIES.register("pet_bed", () -> BlockEntityType.Builder.of(
                    PetBedBlockEntity::new, ModBlocks.PET_BED.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
