package co.uk.loopylucy.kinkstuff.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "eriskinkstuff");

    // A clean record to store both pieces of data together
    public record OverlayInfo(String texture, int color, boolean hideLayers) {
        public static final Codec<OverlayInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("texture").forGetter(OverlayInfo::texture),
                Codec.INT.fieldOf("color").forGetter(OverlayInfo::color),
                Codec.BOOL.optionalFieldOf("hide_layers", false).forGetter(OverlayInfo::hideLayers)
        ).apply(instance, OverlayInfo::new));
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OverlayInfo>> OVERLAY_INFO =
            COMPONENTS.register("overlay_info", () -> DataComponentType.<OverlayInfo>builder()
                    .persistent(OverlayInfo.CODEC) // Use .persistent() for saved components
                    .build());
}