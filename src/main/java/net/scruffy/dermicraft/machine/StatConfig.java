package net.scruffy.dermicraft.machine;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.Arrays;
import java.util.List;

/**
 * The SERVER config for the L-level stats. The spec's defaults come from StatDefaults. The snapshot is
 * built lazily on first use after a config load and dropped on load, reload and unload; when the config
 * is not loaded yet (main menu, client startup) the hardcoded defaults are returned.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public final class StatConfig {

    private static final int TIERS = 4;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends Number>> ITEM_TRANSFER;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> FLUID_TRANSFER;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> MODULE_SLOTS;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> SIMPLE_MACHINE_SPEED;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> SIMPLE_MACHINE_FUEL_USE;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> SIMPLE_MACHINE_MAX_HEALTH;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> SIMPLE_MACHINE_TANK_CAPACITY_MB;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> SKIN_TANK_CAPACITY_MB;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> CRAW_STACKS;
    private static final ModConfigSpec.DoubleValue OVERGROWTH_PERCENT;
    private static final ModConfigSpec.IntValue OVERGROWTH_MAX_LEVEL;
    private static final ModConfigSpec.IntValue HUNGER_DAMAGE_PER_CYCLE;
    private static final ModConfigSpec.DoubleValue UNFUELED_SPEED;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("shared");
        ITEM_TRANSFER = tierList("itemTransferPerSecond", "Items per second a machine or Craw may auto-push.", StatDefaults.ITEM_TRANSFER, 0.1, 100000);
        FLUID_TRANSFER = tierList("fluidTransferPerSecond", "Millibuckets per second a machine or tank may auto-push.", StatDefaults.FLUID_TRANSFER, 1, 10000000);
        MODULE_SLOTS = tierList("generalModuleSlots", "General Module slots (the Evolution slot is separate). 0 to 4.", toDoubles(StatDefaults.MODULE_SLOTS), 0, 4);
        BUILDER.pop();

        BUILDER.push("simple_machine");
        SIMPLE_MACHINE_SPEED = tierList("speedMultiplier", "Processing speed multiplier per tier.", StatDefaults.SIMPLE_MACHINE_SPEED, 0.1, 1000);
        SIMPLE_MACHINE_FUEL_USE = tierList("fuelUseMultiplier", "Fuel used per cycle, relative to L1, per tier.", StatDefaults.SIMPLE_MACHINE_FUEL_USE, 0.1, 1000);
        SIMPLE_MACHINE_MAX_HEALTH = tierList("maxHealth", "Maximum machine health per tier.", StatDefaults.SIMPLE_MACHINE_MAX_HEALTH, 1, 1000000);
        SIMPLE_MACHINE_TANK_CAPACITY_MB = tierList("tankCapacityMb", "Capacity of each tank, in millibuckets (1000 = 1 bucket), per tier.", StatDefaults.SIMPLE_MACHINE_TANK_CAPACITY_MB, 1, 100000000);
        HUNGER_DAMAGE_PER_CYCLE = BUILDER.comment("HP a processing machine loses per 10-tick cycle with no fuel.")
                .defineInRange("hungerDamagePerCycle", StatDefaults.HUNGER_DAMAGE_PER_CYCLE, 0, 1000);
        UNFUELED_SPEED = BUILDER.comment("Processing speed with no fuel (flat, independent of tier).")
                .defineInRange("unfueledSpeed", (double) StatDefaults.UNFUELED_SPEED, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("skin_tank");
        SKIN_TANK_CAPACITY_MB = tierList("tankCapacityMb", "Skin Tank capacity, in millibuckets (1000 = 1 bucket), per tier.", StatDefaults.SKIN_TANK_CAPACITY_MB, 1, 100000000);
        BUILDER.pop();

        BUILDER.push("craw");
        CRAW_STACKS = tierList("stacks", "Craw capacity in stacks per tier; the item limit is this times the stored item's max stack size.", StatDefaults.CRAW_STACKS, 1, 100000);
        BUILDER.pop();

        BUILDER.push("overgrowth");
        OVERGROWTH_PERCENT = BUILDER.comment("Percent of the L4 value each Overgrowth level adds to every numeric stat.")
                .defineInRange("percentPerLevel", StatDefaults.OVERGROWTH_PERCENT, 0.0, 100.0);
        OVERGROWTH_MAX_LEVEL = BUILDER.comment("Highest Overgrowth level a machine can reach (the game's own hard limit is " + StatDefaults.OVERGROWTH_MAX_LEVEL + ").")
                .defineInRange("maxLevel", StatDefaults.OVERGROWTH_MAX_LEVEL, 1, StatDefaults.OVERGROWTH_MAX_LEVEL);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private StatConfig() {}

    private static ModConfigSpec.ConfigValue<List<? extends Number>> tierList(
            String name, String comment, double[] defaults, double min, double max) {
        List<Double> defaultList = Arrays.stream(defaults).boxed().toList();
        return BUILDER.comment(comment + " Exactly " + TIERS + " values, L1 to L4.")
                .defineList(List.of(name), () -> defaultList, () -> defaultList.get(defaultList.size() - 1),
                        o -> o instanceof Number n && n.doubleValue() >= min && n.doubleValue() <= max,
                        ModConfigSpec.Range.of(TIERS, TIERS));
    }

    private static double[] toDoubles(int[] values) {
        return Arrays.stream(values).asDoubleStream().toArray();
    }

    // ---- The lazy snapshot ----

    private static volatile StatValues snapshot;

    /** The current stat numbers: the config's, once loaded, otherwise the hardcoded defaults. */
    static StatValues getCurrent() {
        StatValues s = snapshot;
        if (s != null) return s;
        if (!SPEC.isLoaded()) return StatDefaults.VALUES;   // not cached: the config may load later
        s = buildFromConfig();
        snapshot = s;
        return s;
    }


    private static StatValues buildFromConfig() {
        return new StatValues(
                table(ITEM_TRANSFER, StatDefaults.ITEM_TRANSFER),
                table(FLUID_TRANSFER, StatDefaults.FLUID_TRANSFER),
                slots(MODULE_SLOTS, StatDefaults.MODULE_SLOTS),
                table(SIMPLE_MACHINE_SPEED, StatDefaults.SIMPLE_MACHINE_SPEED),
                table(SIMPLE_MACHINE_FUEL_USE, StatDefaults.SIMPLE_MACHINE_FUEL_USE),
                table(SIMPLE_MACHINE_MAX_HEALTH, StatDefaults.SIMPLE_MACHINE_MAX_HEALTH),
                table(SIMPLE_MACHINE_TANK_CAPACITY_MB, StatDefaults.SIMPLE_MACHINE_TANK_CAPACITY_MB),
                table(SKIN_TANK_CAPACITY_MB, StatDefaults.SKIN_TANK_CAPACITY_MB),
                table(CRAW_STACKS, StatDefaults.CRAW_STACKS),
                OVERGROWTH_PERCENT.get(), OVERGROWTH_MAX_LEVEL.get(),
                HUNGER_DAMAGE_PER_CYCLE.get(), UNFUELED_SPEED.get().floatValue());
    }

    /** The config list as a table, or the default if it is somehow not exactly one value per tier. */
    private static double[] table(ModConfigSpec.ConfigValue<List<? extends Number>> value, double[] fallback) {
        List<? extends Number> list = value.get();
        if (list.size() != fallback.length) {
            Dermicraft.LOGGER.warn("Stat config list {} has {} values, expected {}; using defaults.",
                    value.getPath(), list.size(), fallback.length);
            return fallback;
        }
        double[] out = new double[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i).doubleValue();
        return out;
    }

    private static int[] slots(ModConfigSpec.ConfigValue<List<? extends Number>> value, int[] fallback) {
        double[] asDoubles = table(value, toDoubles(fallback));
        int[] out = new int[asDoubles.length];
        for (int i = 0; i < out.length; i++) out[i] = (int) Math.round(asDoubles[i]);
        return out;
    }

    // ---- Dropping the snapshot when the file changes ----

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) { dropIfOurs(event); }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) { dropIfOurs(event); }

    @SubscribeEvent
    static void onUnload(ModConfigEvent.Unloading event) { dropIfOurs(event); }

    private static void dropIfOurs(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) snapshot = null;
    }
}