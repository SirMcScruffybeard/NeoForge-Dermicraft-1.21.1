package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;

/**
 * Stats for the Skin Tank (the Charred Tank has been folded into its L2 row). A tank has no health, fuel or processing
 * speed, so only capacity, fluid transfer and Module slots exist. Same lookup rule and defaults as
 * {@link SimpleMachineStats}; the transfer rate and slot tables are shared with the machines.
 */
public final class SkinTankStats {

    private SkinTankStats() {}

    private static double value(double[] table, EvolutionStateData state) {
        return StatTable.at(table, state.tier(), state.overgrowth(), StatConfig.getCurrent().overgrowthPercent());
    }

    /** Fluid capacity in mB. */
    public static int getTankCapacityMb(EvolutionStateData state) {
        return StatTable.toInt(value(StatConfig.getCurrent().skinTankCapacityMb(), state));
    }

    /** Millibuckets per second the tank may auto-push. */
    public static int getFluidTransferPerSecond(EvolutionStateData state) {
        return StatTable.toInt(value(StatConfig.getCurrent().fluidTransfer(), state));
    }

    /** General Module slots (the dedicated Evolution slot is separate); Overgrowth adds none. */
    public static int getModuleSlots(EvolutionStateData state) {
        return StatConfig.getCurrent().moduleSlots()[state.tier() - 1];
    }
}
