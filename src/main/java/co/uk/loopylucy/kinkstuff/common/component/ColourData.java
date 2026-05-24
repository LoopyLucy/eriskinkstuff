package co.uk.loopylucy.kinkstuff.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ColourData(int colour0, int colour1) {
    public static final Codec<ColourData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("colour0").forGetter(ColourData::colour0),
            Codec.INT.fieldOf("colour1").forGetter(ColourData::colour1)
    ).apply(instance, ColourData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ColourData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ColourData::colour0,
            ByteBufCodecs.INT, ColourData::colour1,
            ColourData::new
    );
}
