package net.scruffy.dermicraft.machine;

/**
 * The shared lookup rule behind every tiered stat table: a stat is its entry for the current tier, and
 * past the last tier (Overgrowth) the last entry plus a percentage of it per Overgrowth level. Kept
 * separate from any one family's tables so machines, storage and later suits, gadgets and the like
 * can each own their own tables and still use the same rule.
 */
final class StatTable {

    private StatTable() {}

    /**
     * @param table              the stat's values, one per tier (index 0 = tier 1)
     * @param tier               the current tier, 1 to {@code table.length}
     * @param overgrowth         the current Overgrowth level, 0 for none
     * @param overgrowthPercent  percent of the last entry added per Overgrowth level
     */
    static double at(double[] table, int tier, int overgrowth, double overgrowthPercent) {
        double last = table[table.length - 1];
        if (overgrowth > 0) {
            return last + last * (overgrowthPercent / 100.0) * overgrowth;
        }
        return table[tier - 1];
    }
}
