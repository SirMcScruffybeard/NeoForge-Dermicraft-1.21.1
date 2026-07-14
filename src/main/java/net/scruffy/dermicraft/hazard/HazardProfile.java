package net.scruffy.dermicraft.hazard;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.datagen.tag.ModTags;

import java.util.HashSet;
import java.util.Set;

/**
 * Immutable description of which hazard KINDS a container (tank, duct, machine) tolerates.
 *
 * <p>Acceptance rule is subset-based: a fluid is allowed iff every hazard kind it carries is in
 * this profile's tolerated set (fluid's hazards ⊆ tolerated). Tier presets below follow the design
 * ladder, but the set model also permits off-ladder combinations (e.g. the metaphysical hazards added
 * to any tier without disturbing the heat/radiation ordering).
 *
 * <p>Weakest-link across a run of containers is {@link #intersect(HazardProfile)} — the set analog
 * of taking the minimum tier.
 */
public final class HazardProfile {

    private final Set<TagKey<Fluid>> tolerated;

    private HazardProfile(Set<TagKey<Fluid>> tolerated) {
        this.tolerated = Set.copyOf(tolerated);
    }

    @SafeVarargs
    public static HazardProfile of(TagKey<Fluid>... hazards) {
        return new HazardProfile(Set.of(hazards));
    }

    /** Tolerates every known hazard — an unrestricted container that gates nothing. */
    public static final HazardProfile ALL = new HazardProfile(new HashSet<>(ModTags.Fluids.ALL_HAZARDS));

    // --- Ladder presets (cumulative, matching the design notes) ---
    public static final HazardProfile TIER_1 = new HazardProfile(Set.of());
    public static final HazardProfile TIER_2 = of(ModTags.Fluids.EXTREME_HEAT);
    public static final HazardProfile TIER_3 = of(ModTags.Fluids.EXTREME_HEAT, ModTags.Fluids.RADIATION_MILD);
    public static final HazardProfile TIER_4 = of(
            ModTags.Fluids.EXTREME_HEAT,
            ModTags.Fluids.RADIATION_MILD,
            ModTags.Fluids.RADIATION_SEVERE,
            ModTags.Fluids.BIOHAZARD);

    /** True if every hazard kind the fluid carries is tolerated by this profile. */
    public boolean accepts(FluidStack fluid) {
        for (TagKey<Fluid> hazard : ModTags.Fluids.ALL_HAZARDS) {
            if (fluid.is(hazard) && !tolerated.contains(hazard)) {
                return false;
            }
        }
        return true;
    }

    /** New profile that additionally tolerates {@code hazard} (off-ladder add-on, e.g. a metaphysical kind). */
    public HazardProfile plus(TagKey<Fluid> hazard) {
        Set<TagKey<Fluid>> next = new HashSet<>(tolerated);
        next.add(hazard);
        return new HazardProfile(next);
    }

    /** New profile that no longer tolerates {@code hazard}. */
    public HazardProfile minus(TagKey<Fluid> hazard) {
        Set<TagKey<Fluid>> next = new HashSet<>(tolerated);
        next.remove(hazard);
        return new HazardProfile(next);
    }

    /** Weakest-link combine: tolerates only hazards tolerated by BOTH profiles. */
    public HazardProfile intersect(HazardProfile other) {
        Set<TagKey<Fluid>> next = new HashSet<>(tolerated);
        next.retainAll(other.tolerated);
        return new HazardProfile(next);
    }

    public Set<TagKey<Fluid>> tolerated() {
        return tolerated;
    }
}
