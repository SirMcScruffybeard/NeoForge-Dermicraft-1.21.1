package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.scruffy.dermicraft.hazard.HazardFamily;

import java.util.List;

/**
 * What a hazard module carries: the family and the level (1 mild, 2 severe) it grants. The same data means
 * different things on different items: a Safety Module grants the tolerance while installed, an Evolution hazard
 * module grants it permanently once the evolution completes. A Severe level covers Mild too.
 *
 * @param family the hazard family
 * @param level  clamped to 1 up to the family's top level
 */
public record HazardModuleData(HazardFamily family, int level) {

    public HazardModuleData {
        level = Math.clamp(level, 1, family.getMaxLevel());
    }

    /** The fluid hazard tags this grant tolerates (every level up to the one held). */
    public List<TagKey<Fluid>> getTags() {
        return family.getTags(level);
    }

    public static final Codec<HazardModuleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    StringRepresentable.fromEnum(HazardFamily::values).fieldOf("family").forGetter(HazardModuleData::family),
                    Codec.INT.fieldOf("level").forGetter(HazardModuleData::level))
            .apply(instance, HazardModuleData::new));

    public static final StreamCodec<FriendlyByteBuf, HazardModuleData> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(HazardFamily.class), HazardModuleData::family,
            ByteBufCodecs.VAR_INT, HazardModuleData::level,
            HazardModuleData::new);
}
