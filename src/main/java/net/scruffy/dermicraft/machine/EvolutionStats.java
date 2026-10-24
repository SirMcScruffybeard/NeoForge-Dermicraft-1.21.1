package net.scruffy.dermicraft.machine;

/**
 * The evolution times, read from the stat config's current snapshot (the hardcoded defaults while the
 * config is not loaded). All times are real seconds in the config and are returned here as ticks.
 * Which time applies to which upgrade is decided by {@link EvolutionRules}; this class only holds the numbers.
 */
public final class EvolutionStats {

    private static final int TICKS_PER_SECOND = 20;

    private EvolutionStats() {}

    private static int toTicks(double seconds) {
        return Math.max(1, StatTable.toInt(seconds * TICKS_PER_SECOND));
    }

    /** Ticks to raise the tier to {@code targetTier} (2, 3 or 4); each step takes longer than the one before by default. */
    public static int getTierTicks(int targetTier) {
        double[] seconds = StatConfig.getCurrent().evolutionTierSeconds();
        int index = Math.max(0, Math.min(seconds.length - 1, targetTier - 2));
        return toTicks(seconds[index]);
    }

    /** Ticks to add a hazard family the machine does not hold yet (including a Severe-first module). */
    public static int getHazardNewTicks() {
        return toTicks(StatConfig.getCurrent().evolutionHazardNewSeconds());
    }

    /** Ticks to raise a hazard family already held from Mild to Severe. */
    public static int getHazardUpgradeTicks() {
        return toTicks(StatConfig.getCurrent().evolutionHazardUpgradeSeconds());
    }

    /** Ticks to reach Overgrowth {@code targetLevel}: the level 1 time plus the growth percent of it for every level after the first. */
    public static int getOvergrowthTicks(int targetLevel) {
        StatValues values = StatConfig.getCurrent();
        double base = values.evolutionOvergrowthSeconds();
        double seconds = base * (1.0 + (values.evolutionOvergrowthGrowthPercent() / 100.0) * Math.max(0, targetLevel - 1));
        return toTicks(seconds);
    }

    /** Ticks of the cancel window for blocks that do not process (tanks, the Craw). */
    public static int getCountdownTicks() {
        return Math.max(0, StatTable.toInt(StatConfig.getCurrent().evolutionCountdownSeconds() * TICKS_PER_SECOND));
    }

    /** Highest Overgrowth level the config allows (never above the game's own hard limit). */
    public static int getOvergrowthMaxLevel() {
        return StatConfig.getCurrent().overgrowthMaxLevel();
    }
}
