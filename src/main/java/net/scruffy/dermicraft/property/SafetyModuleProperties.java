package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.scruffy.dermicraft.hazard.HazardProfile;

import java.util.List;

/**
 * Per-item hazard grant for a Safety Module, looked up via {@code ModDataMaps.SAFETY_MODULE_PROPERTIES}
 * (keyed on the Module item) -- see dermicraft-gadget-notes.md -> Gadget upgrade points -> Modules
 * direction note. Tag membership in {@code ModTags.Items.MODULE_SAFETY} is what marks an item as a
 * Safety Module at all (generic dispatch); this data map is what says WHICH hazard(s) a specific one
 * grants -- the same kind-vs-data split {@code ModDataMaps.BIOFUELS} already uses for fluids.
 *
 * <p>A list rather than a single tag, so a future Safety Module could plausibly grant more than one
 * hazard kind at once, though every module in this first pass grants exactly one.
 */
public record SafetyModuleProperties(List<TagKey<Fluid>> hazards) {

    public static final Codec<SafetyModuleProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    TagKey.hashedCodec(Registries.FLUID).listOf().fieldOf("hazards").forGetter(SafetyModuleProperties::hazards))
            .apply(instance, SafetyModuleProperties::new));

    /** The {@link HazardProfile} this module grants while equipped -- built fresh from the data
     * each call rather than cached here, since caching the derived gadget-wide profile is the
     * loadout's own job (see the Modules direction note's "cache on slot change" decision), not
     * this per-item data record's. */
    public HazardProfile hazardProfile() {
        return HazardProfile.of(hazards);
    }
}
