package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a level-ladder Evolution Module carries: the level it raises a machine to, a tier (Tier modules) or an
 * Overgrowth level (Overgrowth modules). Which ladder it climbs is decided by the item, not the data.
 *
 * @param level the target level; at least 1
 */
public record LevelModuleData(int level) {

    public LevelModuleData {
        level = Math.max(1, level);
    }

    public static final Codec<LevelModuleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("level").forGetter(LevelModuleData::level))
            .apply(instance, LevelModuleData::new));

    public static final StreamCodec<ByteBuf, LevelModuleData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LevelModuleData::level,
            LevelModuleData::new);
}

