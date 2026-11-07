package net.scruffy.dermicraft.machine;

import org.jetbrains.annotations.Nullable;

/**
 * One machine's config overrides. A null field means the machine uses the shared value for it, so a machine
 * whose section is empty behaves exactly like the shared tables.
 */
record MachineOverrides(
        @Nullable double[] speed, @Nullable double[] fuelUse, @Nullable double[] maxHealth,
        @Nullable double[] tankCapacityMb, @Nullable double[] passiveYield, @Nullable int[] moduleSlots,
        @Nullable double[] evolutionTierSeconds, @Nullable Double evolutionHazardNewSeconds,
        @Nullable Double evolutionHazardUpgradeSeconds, @Nullable Double evolutionOvergrowthSeconds,
        @Nullable Double evolutionOvergrowthGrowthPercent) {

    /** No overrides: everything comes from the shared values. */
    static final MachineOverrides NONE = new MachineOverrides(null, null, null, null, null, null, null, null, null, null, null);
}
