package net.scruffy.dermicraft.machine;

import net.neoforged.neoforge.fluids.FluidType;
import net.scruffy.dermicraft.component.EvolutionStateData;

/**
 * Stats for the Skin Tank (and the Charred Tank folded into it). A tank has no health, fuel or processing
 * speed, so only capacity, fluid transfer and Module slots exist. Same lookup rule and defaults as
 * {@link SimpleMachineStats}; the transfer rate and slot tables are shared with the machines.
 */
public final class SkinTankStats {

    private SkinTankStats() {}

    private static double value(double[] table, EvolutionStateData state) {
        return StatTable.at(table, state.tier(), state.overgrowth(), StatDefaults.OVERGROWTH_PERCENT);
    }

    /** Fluid capacity in mB. */
    public static int tankCapacityMb(EvolutionStateData state) {
        return (int) Math.round(value(StatDefaults.SKIN_TANK_BUCKETS, state) * FluidType.BUCKET_VOLUME);
    }

    /** Millibuckets per second the tank may auto-push. */
    public static int fluidTransferPerSecond(EvolutionStateData state) {
        return (int) Math.round(value(StatDefaults.FLUID_TRANSFER, state));
    }

    /** General Module slots (the dedicated Evolution slot is separate); Overgrowth adds none. */
    public static int moduleSlots(EvolutionStateData state) {
        return StatDefaults.MODULE_SLOTS[state.tier() - 1];
    }
}
