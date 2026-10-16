package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;

/**
 * Stats for the Craw. It follows the tier track only (no hazard tolerances) and has no health, fuel or
 * tank, so only capacity, item transfer and Module slots exist. Capacity is counted in stacks; the real
 * item limit is that many stacks of the stored item's max stack size. Same lookup rule and defaults as
 * {@link SimpleMachineStats}; the transfer rate and slot tables are shared with the machines.
 */
public final class CrawStats {

    private CrawStats() {}

    private static double value(double[] table, EvolutionStateData state) {
        return StatTable.at(table, state.tier(), state.overgrowth(), StatDefaults.OVERGROWTH_PERCENT);
    }

    /** How many stacks the Craw holds; multiply by the stored item's max stack size for the item limit. */
    public static int stacks(EvolutionStateData state) {
        return (int) Math.round(value(StatDefaults.CRAW_STACKS, state));
    }

    /** Items per second the Craw may auto-push; fractional, so callers keep a running budget. */
    public static double itemTransferPerSecond(EvolutionStateData state) {
        return value(StatDefaults.ITEM_TRANSFER, state);
    }

    /** General Module slots (the dedicated Evolution slot is separate); Overgrowth adds none. */
    public static int moduleSlots(EvolutionStateData state) {
        return StatDefaults.MODULE_SLOTS[state.tier() - 1];
    }
}
