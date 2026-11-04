package net.scruffy.dermicraft.hazard;

import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.machine.FluidHazards;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable description of how much of each hazard family a container (tank, duct, machine) tolerates: a level per
 * family, 0 for none, 1 for Mild (the only level of Thermal and Biohazard), 2 for Severe. A level covers every level
 * below it, so holding Severe also covers Mild.
 *
 * <p>Acceptance rule: a fluid is allowed iff, in every family, the band it falls in (see {@link FluidHazards}) is at or
 * below the level this profile holds. Tier presets below follow the design ladder, but a profile can hold any
 * combination, so off-ladder grants (a Metaphysical tolerance on a Tier 1 machine) work without disturbing the rest.
 *
 * <p>Weakest-link across a run of containers is {@link #intersect(HazardProfile)}: the lower level in each family.
 */
public final class HazardProfile {

    private final EnumMap<HazardFamily, Integer> levels;

    private HazardProfile(Map<HazardFamily, Integer> levels) {
        this.levels = new EnumMap<>(HazardFamily.class);
        for (HazardFamily family : HazardFamily.values()) {
            int level = Math.clamp(levels.getOrDefault(family, 0), 0, family.getMaxLevel());
            this.levels.put(family, level);
        }
    }

    /** A profile holding exactly the given levels (families left out are 0). */
    public static HazardProfile of(Map<HazardFamily, Integer> levels) {
        return new HazardProfile(levels);
    }

    /** Tolerates every family up to its top level, an unrestricted container that gates nothing. */
    public static final HazardProfile ALL = allAtMax();

    // --- Ladder presets (cumulative, matching the design notes) ---
    public static final HazardProfile TIER_1 = new HazardProfile(Map.of());
    public static final HazardProfile TIER_2 = TIER_1.plusFamily(HazardFamily.THERMAL, 1);
    public static final HazardProfile TIER_3 = TIER_2.plusFamily(HazardFamily.RADIATION, 1);
    public static final HazardProfile TIER_4 = TIER_3
            .plusFamily(HazardFamily.RADIATION, 2)
            .plusFamily(HazardFamily.BIOHAZARD, 1);

    private static HazardProfile allAtMax() {
        Map<HazardFamily, Integer> max = new EnumMap<>(HazardFamily.class);
        for (HazardFamily family : HazardFamily.values()) max.put(family, family.getMaxLevel());
        return new HazardProfile(max);
    }

    /** The level this profile holds in {@code family}, 0 for none. */
    public int getLevel(HazardFamily family) {
        return levels.get(family);
    }

    /** True if this profile tolerates {@code family} at all (a machine's penalty for it is at least reduced). */
    public boolean tolerates(HazardFamily family) {
        return getLevel(family) >= 1;
    }

    /** True if every family's band for the fluid is within the level this profile holds. */
    public boolean accepts(FluidStack fluid) {
        for (HazardFamily family : HazardFamily.values()) {
            if (FluidHazards.getLevel(fluid, family) > getLevel(family)) {
                return false;
            }
        }
        return true;
    }

    /** New profile that additionally tolerates {@code family} up to {@code level} (never lowers an existing level). */
    public HazardProfile plusFamily(HazardFamily family, int level) {
        EnumMap<HazardFamily, Integer> next = new EnumMap<>(levels);
        next.merge(family, level, Math::max);
        return new HazardProfile(next);
    }

    /** Weakest-link combine: the lower level in each family. */
    public HazardProfile intersect(HazardProfile other) {
        EnumMap<HazardFamily, Integer> next = new EnumMap<>(HazardFamily.class);
        for (HazardFamily family : HazardFamily.values()) {
            next.put(family, Math.min(getLevel(family), other.getLevel(family)));
        }
        return new HazardProfile(next);
    }
}
