package co.uk.loopylucy.kinkstuff.init;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, "eriskinkstuff");

    // COMPILER FIX: Combines standard MapCodec for disk storage with StreamCodec for network transmission
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> BED_COLOR =
            DATA_COMPONENT_TYPES.register("bed_color", () -> DataComponentType.<DyeColor>builder()
                    .persistent(DyeColor.CODEC) // Handles local disk saves (JSON/NBT mapping)
                    .networkSynchronized(ByteBufCodecs.idMapper(DyeColor::byId, DyeColor::getId)) // Handles server-to-client packets
                    .build());
}
