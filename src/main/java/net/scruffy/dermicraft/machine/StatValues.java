package net.scruffy.dermicraft.machine;

/** An immutable bundle of every stat number: either the hardcoded defaults or a snapshot of the server config. */
record StatValues(
        double[] itemTransfer, double[] fluidTransfer, int[] moduleSlots,
        double[] simpleMachineSpeed, double[] simpleMachineFuelUse,
        double[] simpleMachineMaxHealth, double[] simpleMachineTankCapacityMb,
        double[] skinTankCapacityMb, double[] crawStacks,
        double overgrowthPercent, int overgrowthMaxLevel,
        int hungerDamagePerCycle, float unfueledSpeed,
        double[] evolutionTierSeconds, double evolutionHazardNewSeconds, double evolutionHazardUpgradeSeconds,
        double evolutionOvergrowthSeconds, double evolutionOvergrowthGrowthPercent, double evolutionCountdownSeconds) {
}