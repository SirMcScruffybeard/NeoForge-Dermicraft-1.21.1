package net.scruffy.dermicraft.machine;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * An immutable bundle of every stat number: either the hardcoded defaults or a snapshot of the server config. The
 * shared tables apply to every machine; {@code machines} holds each machine's own overrides, and the {@code ...For}
 * methods resolve a machine's value or fall back to the shared one. A null machine key means the shared value.
 */
record StatValues(
        double[] itemTransfer, double[] fluidTransfer, int[] moduleSlots,
        double[] simpleMachineSpeed, double[] simpleMachineFuelUse,
        double[] simpleMachineMaxHealth, double[] simpleMachineTankCapacityMb, double[] simpleMachinePassiveYield,
        double[] skinTankCapacityMb, double[] crawStacks,
        double overgrowthPercent, int overgrowthMaxLevel,
        int hungerDamagePerCycle, float unfueledSpeed,
        double[] evolutionTierSeconds, double evolutionHazardNewSeconds, double evolutionHazardUpgradeSeconds,
        double evolutionOvergrowthSeconds, double evolutionOvergrowthGrowthPercent, double evolutionCountdownSeconds,
        double thermalDamageModifier, int thermalReferenceTemperature, int thermalSafeRange,
        HazardBand radiationBand, HazardBand biohazardBand, HazardBand metaphysicalBand,
        double radiationEffectModifier, double biohazardEffectModifier, double metaphysicalEffectModifier,
        Map<MachineFamily, MachineOverrides> families, Map<MachineStatKey, MachineOverrides> machines) {

    /** The family table of the machine's family, between the global values and the machine's own overrides. */
    private MachineOverrides family(@Nullable MachineStatKey machine) {
        if (machine == null || machine.getFamily() == null) return MachineOverrides.NONE;
        return families.getOrDefault(machine.getFamily(), MachineOverrides.NONE);
    }

    private MachineOverrides overrides(@Nullable MachineStatKey machine) {
        return machine == null ? MachineOverrides.NONE : machines.getOrDefault(machine, MachineOverrides.NONE);
    }

    double[] speedFor(@Nullable MachineStatKey machine) {
        double[] own = overrides(machine).speed();
        if (own == null) own = family(machine).speed();
        return own != null ? own : simpleMachineSpeed;
    }

    double[] fuelUseFor(@Nullable MachineStatKey machine) {
        double[] own = overrides(machine).fuelUse();
        if (own == null) own = family(machine).fuelUse();
        return own != null ? own : simpleMachineFuelUse;
    }

    double[] maxHealthFor(@Nullable MachineStatKey machine) {
        double[] own = overrides(machine).maxHealth();
        if (own == null) own = family(machine).maxHealth();
        return own != null ? own : simpleMachineMaxHealth;
    }

    double[] passiveYieldFor(@Nullable MachineStatKey machine) {
        double[] own = overrides(machine).passiveYield();
        if (own == null) own = family(machine).passiveYield();
        return own != null ? own : simpleMachinePassiveYield;
    }

    double[] tankCapacityMbFor(@Nullable MachineStatKey machine) {
        double[] own = overrides(machine).tankCapacityMb();
        if (own == null) own = family(machine).tankCapacityMb();
        return own != null ? own : simpleMachineTankCapacityMb;
    }

    int[] moduleSlotsFor(@Nullable MachineStatKey machine) {
        int[] own = overrides(machine).moduleSlots();
        if (own == null) own = family(machine).moduleSlots();
        return own != null ? own : moduleSlots;
    }

    double[] evolutionTierSecondsFor(@Nullable MachineStatKey machine) {
        double[] own = overrides(machine).evolutionTierSeconds();
        if (own == null) own = family(machine).evolutionTierSeconds();
        return own != null ? own : evolutionTierSeconds;
    }

    double evolutionHazardNewSecondsFor(@Nullable MachineStatKey machine) {
        Double own = overrides(machine).evolutionHazardNewSeconds();
        if (own == null) own = family(machine).evolutionHazardNewSeconds();
        return own != null ? own : evolutionHazardNewSeconds;
    }

    double evolutionHazardUpgradeSecondsFor(@Nullable MachineStatKey machine) {
        Double own = overrides(machine).evolutionHazardUpgradeSeconds();
        if (own == null) own = family(machine).evolutionHazardUpgradeSeconds();
        return own != null ? own : evolutionHazardUpgradeSeconds;
    }

    double evolutionOvergrowthSecondsFor(@Nullable MachineStatKey machine) {
        Double own = overrides(machine).evolutionOvergrowthSeconds();
        if (own == null) own = family(machine).evolutionOvergrowthSeconds();
        return own != null ? own : evolutionOvergrowthSeconds;
    }

    double evolutionOvergrowthGrowthPercentFor(@Nullable MachineStatKey machine) {
        Double own = overrides(machine).evolutionOvergrowthGrowthPercent();
        if (own == null) own = family(machine).evolutionOvergrowthGrowthPercent();
        return own != null ? own : evolutionOvergrowthGrowthPercent;
    }
}
