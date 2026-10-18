package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;

/**
 * Hardcoded default stats, indexed by tier (0 = L1 ... 3 = L4). Package-private: only the stat classes
 * ({@link SimpleMachineStats}, {@link SkinTankStats}, {@link CrawStats}) read them (the arrays are
 * mutable). These stay as the safety net when a server config exists: an unloaded or invalid config
 * falls back to them. Values are the decided table in the machine notes; capacities are in millibuckets
 * (tanks) or stacks (Craw), transfer rates per second.
 */
final class StatDefaults {

    private StatDefaults() {}

    // ---- Shared by simple machines and the storage that auto-pushes ----
    static final double[] ITEM_TRANSFER = {4, 8, 16, 32};       // items per second
    static final double[] FLUID_TRANSFER = {1000, 2000, 4000, 8000}; // mB per second
    static final int[] MODULE_SLOTS = {1, 2, 3, 4};              // general slots, no Overgrowth growth

    // ---- Simple machines ----
    static final double[] SIMPLE_MACHINE_SPEED = {1, 2, 4, 8};
    static final double[] SIMPLE_MACHINE_FUEL_USE = {1, 1.5, 2.25, 3.375};
    static final double[] SIMPLE_MACHINE_MAX_HEALTH = {200, 400, 800, 1600};
    static final double[] SIMPLE_MACHINE_TANK_CAPACITY_MB = {5000, 10000, 20000, 40000};

    // ---- Storage ----
    static final double[] SKIN_TANK_CAPACITY_MB = {10000, 20000, 40000, 80000};
    static final double[] CRAW_STACKS = {10, 20, 40, 80};

    // ---- Single values ----
    static final double OVERGROWTH_PERCENT = 5;                  // of the L4 value, per level, for every numeric stat
    static final int HUNGER_DAMAGE_PER_CYCLE = 1;
    static final float UNFUELED_SPEED = 0.1f;

    static final int OVERGROWTH_MAX_LEVEL = EvolutionStateData.MAX_OVERGROWTH;

    static final StatValues VALUES = new StatValues(
            ITEM_TRANSFER, FLUID_TRANSFER, MODULE_SLOTS,
            SIMPLE_MACHINE_SPEED, SIMPLE_MACHINE_FUEL_USE,
            SIMPLE_MACHINE_MAX_HEALTH, SIMPLE_MACHINE_TANK_CAPACITY_MB,
            SKIN_TANK_CAPACITY_MB, CRAW_STACKS,
            OVERGROWTH_PERCENT, OVERGROWTH_MAX_LEVEL,
            HUNGER_DAMAGE_PER_CYCLE, UNFUELED_SPEED);


}
