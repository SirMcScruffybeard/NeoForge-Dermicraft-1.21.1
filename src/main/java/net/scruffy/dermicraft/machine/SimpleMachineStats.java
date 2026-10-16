package net.scruffy.dermicraft.machine;

import net.neoforged.neoforge.fluids.FluidType;
import net.scruffy.dermicraft.component.EvolutionStateData;

/**
 * Stats for the simple (fueled) machines: Masticator, Effluentcer, Metastasizer, Mutator, Render Furnace,
 * Render Kiln, Grafting Table and the Drooling family. Every method takes the machine's
 * {@link EvolutionStateData} and returns the stat for its tier; past L4 a stat is its L4 value plus the
 * Overgrowth percentage of that value per Overgrowth level (see {@link StatTable}); Module slots do not
 * grow. Values come from {@link StatDefaults} for now, so a later server config can sit behind these
 * same methods without changing any caller. See "Stat table for the L levels" in
 * {@code dermicraft-machine-notes.md}.
 */
public final class SimpleMachineStats {

    private SimpleMachineStats() {}

    private static double value(double[] table, EvolutionStateData state) {
        return StatTable.at(table, state.tier(), state.overgrowth(), StatDefaults.OVERGROWTH_PERCENT);
    }

    /** Tier processing speed multiplier; multiplies with the fuel's speed and the Work Speed Modules. */
    public static float speedMultiplier(EvolutionStateData state) {
        return (float) value(StatDefaults.SIMPLE_MACHINE_SPEED, state);
    }

    /** Tier multiplier on fuel spent per cycle (grows at half the rate of speed, so higher tiers are mildly more efficient per item). */
    public static float fuelUseMultiplier(EvolutionStateData state) {
        return (float) value(StatDefaults.SIMPLE_MACHINE_FUEL_USE, state);
    }

    /** Items per second the machine may auto-push; fractional, so callers keep a running budget. */
    public static double itemTransferPerSecond(EvolutionStateData state) {
        return value(StatDefaults.ITEM_TRANSFER, state);
    }

    /** Millibuckets per second the machine may auto-push. */
    public static int fluidTransferPerSecond(EvolutionStateData state) {
        return (int) Math.round(value(StatDefaults.FLUID_TRANSFER, state));
    }

    /** Maximum machine health; doubles per tier. */
    public static int maxHealth(EvolutionStateData state) {
        return (int) Math.round(value(StatDefaults.SIMPLE_MACHINE_MAX_HEALTH, state));
    }

    /** Per-tank fluid capacity in mB. */
    public static int tankCapacityMb(EvolutionStateData state) {
        return (int) Math.round(value(StatDefaults.SIMPLE_MACHINE_TANK_BUCKETS, state) * FluidType.BUCKET_VOLUME);
    }

    /** General Module slots (the dedicated Evolution slot is separate and always present); Overgrowth adds none. */
    public static int moduleSlots(EvolutionStateData state) {
        return StatDefaults.MODULE_SLOTS[state.tier() - 1];
    }

    /** HP a processing machine loses per 10-tick cycle with no fuel; flat, does not scale with tier. */
    public static int hungerDamagePerCycle() {
        return StatDefaults.HUNGER_DAMAGE_PER_CYCLE;
    }

    /** Processing speed with no fuel; flat, independent of tier speed (about one progress tick per cycle). */
    public static float unfueledSpeed() {
        return StatDefaults.UNFUELED_SPEED;
    }
}
