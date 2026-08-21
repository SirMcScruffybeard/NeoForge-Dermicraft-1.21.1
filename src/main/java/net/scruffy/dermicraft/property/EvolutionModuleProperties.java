package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.scruffy.dermicraft.hazard.HazardProfile;

import java.util.List;
import java.util.Optional;

/**
 * Per-item data for an Evolution Module, looked up via {@code ModDataMaps.EVOLUTION_MODULE_PROPERTIES}
 * (keyed on the Module item) -- see dermicraft-machine-notes.md's Drooling Cauldron entry, "Evolution
 * Module family". One record covers both consumer shapes the family serves, since which fields a
 * given item populates is what decides how it behaves, not a Java subtype:
 *
 * <ul>
 *   <li>{@code targetFluid} -- for a SELECTOR consumer (Drooling Cauldron): which fluid installing
 *   this Module switches production to.</li>
 *   <li>{@code hazards} -- for a HAZARD-GATED consumer (Masticator, eventually): same shape as
 *   {@link SafetyModuleProperties#hazards()}, additive to whatever that consumer's own Safety
 *   Module already grants.</li>
 * </ul>
 *
 * <p>A given item is free to populate one field, the other, or (in principle) both -- Heat Evolution
 * Module currently sets both {@code targetFluid} (lava, for Cauldron) and {@code hazards}
 * (EXTREME_HEAT, for whichever hazard-gated machine eventually reads it), since the same physical
 * item is meant to serve either consumer shape depending on which machine's Module slot it's in.
 *
 * <p>{@code evolutionThreshold} is ticks of genuinely active production (not wall-clock/idle time --
 * see the design doc) needed before the consumer's permanent Tier evolution completes.
 */
public record EvolutionModuleProperties(Optional<Fluid> targetFluid, List<TagKey<Fluid>> hazards, int evolutionThreshold) {

    public static final Codec<EvolutionModuleProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("target_fluid").forGetter(EvolutionModuleProperties::targetFluid),
                    TagKey.hashedCodec(Registries.FLUID).listOf().optionalFieldOf("hazards", List.of()).forGetter(EvolutionModuleProperties::hazards),
                    Codec.INT.fieldOf("evolution_threshold").forGetter(EvolutionModuleProperties::evolutionThreshold))
            .apply(instance, EvolutionModuleProperties::new));

    /** Same shape as {@link SafetyModuleProperties#hazardProfile()} -- built fresh each call, not
     * cached here. */
    public HazardProfile hazardProfile() {
        return HazardProfile.of(hazards);
    }
}
