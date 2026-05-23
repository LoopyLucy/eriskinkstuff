package co.uk.loopylucy.kinkstuff.registration;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.common.block.entity.PetBedBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Central registry for all modded Block Entities.
 * Block Entities are used to store data and perform logic for specific blocks (like the Pet Bed).
 */
public class ModBlockEntities {
    /** The registry for block entity types. */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ErisKinkStuff.MODID);

    /** The block entity type for the Pet Bed. */
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PetBedBlockEntity>> PET_BED_BE =
            BLOCK_ENTITIES.register("pet_bed_be", () -> BlockEntityType.Builder.of(PetBedBlockEntity::new, ModBlocks.PET_BED.get()).build(null));

    /**
     * Entry point for registering block entities to the mod event bus.
     * 
     * @param eventBus The mod event bus.
     */
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}