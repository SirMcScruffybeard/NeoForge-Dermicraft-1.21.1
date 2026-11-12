package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.hazard.HazardFamily;

/**
 * The selection rules of the Upgrade Stick, kept in one place so the screen (which greys out what is not allowed)
 * and the menu (which refuses it) always agree. They are the machine rules, minus the time and the modules: a
 * machine holds at most as many hazard families as its tier, each family at one level (Mild or Severe), and
 * Overgrowth exists only at the top tier. Each method returns the new selection, or the same one when the change is
 * not allowed.
 */
public final class UpgradeStickRules {

    private UpgradeStickRules() {}

    /** Whether the tier can be set to {@code tier} without leaving more hazard families than it allows. */
    public static boolean canSetTier(EvolutionStateData state, int tier) {
        return tier >= state.getHeldFamilyCount();
    }

    /** Selects a plain tier (L1 to L4); Overgrowth is cleared. */
    public static EvolutionStateData setTier(EvolutionStateData state, int tier) {
        if (!canSetTier(state, tier)) return state;
        return state.withTier(tier).withOvergrowth(0);
    }

    /** Selects Overgrowth: the top tier, at O1 unless an Overgrowth level is already chosen. */
    public static EvolutionStateData selectOvergrowth(EvolutionStateData state) {
        if (state.overgrowth() > 0) return state;
        return state.withTier(EvolutionStateData.MAX_TIER).withOvergrowth(1);
    }

    /** Moves the Overgrowth level by {@code delta}, within O1 and the configured maximum. */
    public static EvolutionStateData adjustOvergrowth(EvolutionStateData state, int delta) {
        if (state.overgrowth() == 0) return state;
        int max = Math.max(1, EvolutionStats.getOvergrowthMaxLevel());
        return state.withOvergrowth(Math.max(1, Math.min(max, state.overgrowth() + delta)));
    }

    /** Whether a hazard family not yet held could be added: the tolerance cap equals the tier. */
    public static boolean canAddFamily(EvolutionStateData state) {
        return state.getHeldFamilyCount() < state.tier();
    }

    /**
     * Clicking a hazard steps it: off, then each level up to the family's top, then off again. A family with one level
     * (Thermal, Biohazard) is simply on or off. Adding one that is off needs a free tolerance slot.
     */
    public static EvolutionStateData cycleHazard(EvolutionStateData state, HazardFamily family) {
        int held = state.level(family);
        if (held == 0) {
            return canAddFamily(state) ? state.withLevel(family, 1) : state;
        }
        if (held < family.getMaxLevel()) return state.withLevel(family, held + 1);
        return state.withLevel(family, 0);
    }

    /** The same state with every hazard tolerance removed, for a machine that does not handle hazards. */
    public static EvolutionStateData withoutHazards(EvolutionStateData state) {
        EvolutionStateData result = state;
        for (HazardFamily family : HazardFamily.values()) {
            result = result.withLevel(family, 0);
        }
        return result;
    }
}
