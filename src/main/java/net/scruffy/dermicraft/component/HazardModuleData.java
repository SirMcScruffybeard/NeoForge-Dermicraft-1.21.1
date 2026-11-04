package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.scruffy.dermicraft.hazard.HazardFamily;

/**
 * What a hazard module grants: the family and the level (1 mild, 2 severe), attached to each module item through
 * the {@code hazard_module} item data map. The same data means
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

    public static final Codec<HazardModuleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    StringRepresentable.fromEnum(HazardFamily::values).fieldOf("family").forGetter(HazardModuleData::family),
                    Codec.INT.fieldOf("level").forGetter(HazardModuleData::level))
            .apply(instance, HazardModuleData::new));
}
