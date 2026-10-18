package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;

/**
 * Stats for the simple (fueled) machines: Masticator, Effluentcer, Metastasizer, Mutator, Render Furnace,
 * Render Kiln, Grafting Table and the Drooling family. Every method takes the machine's
 * {@link EvolutionStateData} and returns the stat for its tier; past L4 a stat is its L4 value plus the
 * Overgrowth percentage of that value per Overgrowth level (see {@link StatTable}); Module slots do not
 * grow. Values come from {@link StatConfig}'s current snapshot (the server config once it is loaded,
 * the {@link StatDefaults} otherwise), so a config change reaches every caller without changing any of
 * them. See "Stat table for the L levels" in
 * {@code dermicraft-machine-notes.md}.
 */
public final class SimpleMachineStats {

    private SimpleMachineStats() {}

    private static double value(double[] table, EvolutionStateData state) {
        return StatTable.at(table, state.tier(), state.overgrowth(), StatConfig.getCurrent().overgrowthPercent());
    }

    /** Tier processing speed multiplier; multiplies with the fuel's speed and the Work Speed Modules. */
    public static float getSpeedMultiplier(EvolutionStateData state) {
        return (float) value(StatConfig.getCurrent().simpleMachineSpeed(), state);
    }

    /** Tier multiplier on fuel spent per cycle (grows at half the rate of speed, so higher tiers are mildly more efficient per item). */
    public static float getFuelUseMultiplier(EvolutionStateData state) {
        return (float) value(StatConfig.getCurrent().simpleMachineFuelUse(), state);
    }

    /** Items per second the machine may auto-push; fractional, so callers keep a running budget. */
    public static double getItemTransferPerSecond(EvolutionStateData state) {
        return value(StatConfig.getCurrent().itemTransfer(), state);
    }

    /** Millibuckets per second the machine may auto-push. */
    public static int getFluidTransferPerSecond(EvolutionStateData state) {
        return StatTable.toInt(value(StatConfig.getCurrent().fluidTransfer(), state));
    }

    /** Maximum machine health; doubles per tier. */
    public static int getMaxHealth(EvolutionStateData state) {
        return StatTable.toInt(value(StatConfig.getCurrent().simpleMachineMaxHealth(), state));
    }

    /** Per-tank fluid capacity in mB. */
    public static int getTankCapacityMb(EvolutionStateData state) {
        return StatTable.toInt(value(StatConfig.getCurrent().simpleMachineTankCapacityMb(), state));
    }

    /** General Module slots (the dedicated Evolution slot is separate and always present); Overgrowth adds none. */
    public static int getModuleSlots(EvolutionStateData state) {
        return StatConfig.getCurrent().moduleSlots()[state.tier() - 1];
    }

    /** HP a processing machine loses per 10-tick cycle with no fuel; flat, does not scale with tier. */
    public static int getHungerDamagePerCycle() {
        return StatConfig.getCurrent().hungerDamagePerCycle();
    }

    /** Processing speed with no fuel; flat, independent of tier speed (about one progress tick per cycle). */
    public static float getUnfueledSpeed() {
        return StatConfig.getCurrent().unfueledSpeed();
    }
}
