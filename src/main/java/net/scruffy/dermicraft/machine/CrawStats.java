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
        return StatTable.at(table, state.tier(), state.overgrowth(), StatConfig.getCurrent().overgrowthPercent());
    }

    /** How many stacks the Craw holds; multiply by the stored item's max stack size for the item limit. */
    public static int getStacks(EvolutionStateData state) {
        return StatTable.toInt(value(StatConfig.getCurrent().crawStacks(), state));
    }

    /** Items per second the Craw may auto-push; fractional, so callers keep a running budget. */
    public static double getItemTransferPerSecond(EvolutionStateData state) {
        return value(StatConfig.getCurrent().itemTransfer(), state);
    }

    /** General Module slots (the dedicated Evolution slot is separate); Overgrowth adds none. */
    public static int getModuleSlots(EvolutionStateData state) {
        return StatConfig.getCurrent().moduleSlots()[state.tier() - 1];
    }
}
