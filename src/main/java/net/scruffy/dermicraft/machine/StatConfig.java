package net.scruffy.dermicraft.machine;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> SIMPLE_MACHINE_PASSIVE_YIELD;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> SKIN_TANK_CAPACITY_MB;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> CRAW_STACKS;
    private static final ModConfigSpec.DoubleValue OVERGROWTH_PERCENT;
    private static final ModConfigSpec.IntValue OVERGROWTH_MAX_LEVEL;
    private static final ModConfigSpec.IntValue HUNGER_DAMAGE_PER_CYCLE;
    private static final ModConfigSpec.DoubleValue UNFUELED_SPEED;
    private static final ModConfigSpec.ConfigValue<List<? extends Number>> EVOLUTION_TIER_SECONDS;
    private static final ModConfigSpec.DoubleValue EVOLUTION_HAZARD_NEW_SECONDS;
    private static final ModConfigSpec.DoubleValue EVOLUTION_HAZARD_UPGRADE_SECONDS;
    private static final ModConfigSpec.DoubleValue EVOLUTION_OVERGROWTH_SECONDS;
    private static final ModConfigSpec.DoubleValue EVOLUTION_OVERGROWTH_GROWTH_PERCENT;
    private static final ModConfigSpec.DoubleValue EVOLUTION_COUNTDOWN_SECONDS;
    private static final ModConfigSpec.DoubleValue THERMAL_DAMAGE_MODIFIER;
    private static final ModConfigSpec.IntValue THERMAL_REFERENCE_TEMPERATURE;
    private static final ModConfigSpec.IntValue THERMAL_SAFE_RANGE;
    private static final ModConfigSpec.IntValue RADIATION_MILD_START, RADIATION_SEVERE_START, RADIATION_MAX;
    private static final ModConfigSpec.DoubleValue RADIATION_EFFECT_MODIFIER, BIOHAZARD_EFFECT_MODIFIER, METAPHYSICAL_EFFECT_MODIFIER;
    private static final ModConfigSpec.IntValue BIOHAZARD_START, BIOHAZARD_MAX;
    private static final ModConfigSpec.IntValue METAPHYSICAL_MILD_START, METAPHYSICAL_SEVERE_START, METAPHYSICAL_MAX;

    /** One machine's optional overrides in the config. An empty list or a negative number means "use the shared value". */
    private record MachineSection(
            ModConfigSpec.ConfigValue<List<? extends Number>> speed,
            ModConfigSpec.ConfigValue<List<? extends Number>> fuelUse,
            ModConfigSpec.ConfigValue<List<? extends Number>> maxHealth,
            ModConfigSpec.ConfigValue<List<? extends Number>> tankCapacityMb,
            ModConfigSpec.ConfigValue<List<? extends Number>> passiveYield,
            ModConfigSpec.ConfigValue<List<? extends Number>> moduleSlots,
            ModConfigSpec.ConfigValue<List<? extends Number>> evolutionTierSeconds,
            ModConfigSpec.DoubleValue evolutionHazardNewSeconds,
            ModConfigSpec.DoubleValue evolutionHazardUpgradeSeconds,
            ModConfigSpec.DoubleValue evolutionOvergrowthSeconds,
            ModConfigSpec.DoubleValue evolutionOvergrowthGrowthPercent) {}

    private static final Map<MachineStatKey, MachineSection> MACHINE_SECTIONS = new EnumMap<>(MachineStatKey.class);
    private static final Map<MachineFamily, MachineSection> FAMILY_SECTIONS = new EnumMap<>(MachineFamily.class);

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("shared");
        ITEM_TRANSFER = tierList("itemTransferPerSecond", "Items per second a machine or Craw may auto-push.", StatDefaults.ITEM_TRANSFER, 0.1, 100000);
        FLUID_TRANSFER = tierList("fluidTransferPerSecond", "Millibuckets per second a machine or tank may auto-push.", StatDefaults.FLUID_TRANSFER, 1, 10000000);
        MODULE_SLOTS = tierList("generalModuleSlots", "General Module slots (the Evolution slot is separate). 0 to 4.", toDoubles(StatDefaults.MODULE_SLOTS), 0, 4);
        BUILDER.pop();

        // "Simple machine" means any single-block machine (as opposed to the Skin Tank, Craw and the like): the fueled
        // machines and the Drooling family both read these as their global values.
        BUILDER.push("simple_machine");
        SIMPLE_MACHINE_SPEED = tierList("speedMultiplier", "Processing speed multiplier per tier.", StatDefaults.SIMPLE_MACHINE_SPEED, 0.1, 1000);
        SIMPLE_MACHINE_FUEL_USE = tierList("fuelUseMultiplier", "Fuel used per cycle, relative to L1, per tier.", StatDefaults.SIMPLE_MACHINE_FUEL_USE, 0.1, 1000);
        SIMPLE_MACHINE_MAX_HEALTH = tierList("maxHealth", "Maximum machine health per tier.", StatDefaults.SIMPLE_MACHINE_MAX_HEALTH, 1, 1000000);
        SIMPLE_MACHINE_TANK_CAPACITY_MB = tierList("tankCapacityMb", "Capacity of each tank, in millibuckets (1000 = 1 bucket), per tier.", StatDefaults.SIMPLE_MACHINE_TANK_CAPACITY_MB, 1, 100000000);
        SIMPLE_MACHINE_PASSIVE_YIELD = tierList("passiveYield", "Millibuckets per second a machine makes with no input, per tier. Only affects machines that have a passive yield (the Drooling family today); every other machine ignores it. A family table or a machine's own section can override it.", StatDefaults.SIMPLE_MACHINE_PASSIVE_YIELD, 0.1, 1000000);
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

        BUILDER.push("evolution");
        EVOLUTION_TIER_SECONDS = sizedList("tierSeconds", "Seconds of real time an Evolution Module takes to raise the tier to L2, L3 and L4.",
                StatDefaults.EVOLUTION_TIER_SECONDS, 1, 10000000, 3, "L2, L3, L4");
        EVOLUTION_HAZARD_NEW_SECONDS = BUILDER.comment("Seconds to add a hazard family the machine does not hold yet.")
                .defineInRange("hazardNewSeconds", StatDefaults.EVOLUTION_HAZARD_NEW_SECONDS, 1.0, 10000000.0);
        EVOLUTION_HAZARD_UPGRADE_SECONDS = BUILDER.comment("Seconds to raise a hazard family already held from Mild to Severe.")
                .defineInRange("hazardUpgradeSeconds", StatDefaults.EVOLUTION_HAZARD_UPGRADE_SECONDS, 1.0, 10000000.0);
        EVOLUTION_OVERGROWTH_SECONDS = BUILDER.comment("Seconds to reach Overgrowth level 1.")
                .defineInRange("overgrowthSeconds", StatDefaults.EVOLUTION_OVERGROWTH_SECONDS, 1.0, 10000000.0);
        EVOLUTION_OVERGROWTH_GROWTH_PERCENT = BUILDER.comment("Each further Overgrowth level takes this many percent of the level 1 time longer than the one before.")
                .defineInRange("overgrowthGrowthPercent", StatDefaults.EVOLUTION_OVERGROWTH_GROWTH_PERCENT, 0.0, 1000.0);
        EVOLUTION_COUNTDOWN_SECONDS = BUILDER.comment("Seconds of the cancel window for blocks that do not process (tanks, the Craw) before an Evolution Module commits.")
                .defineInRange("countdownSeconds", StatDefaults.EVOLUTION_COUNTDOWN_SECONDS, 0.0, 600.0);
        BUILDER.pop();


        BUILDER.push("hazards");
        THERMAL_DAMAGE_MODIFIER = BUILDER.comment("Thermal hazard: a machine processing a Thermal fluid without a Thermal tolerance loses this x (|fluid temperature - reference temperature| - safe range) / 100 HP per cycle (at least 1 once outside the safe range). 0 turns the effect off.")
                .defineInRange("thermalDamageModifier", StatDefaults.THERMAL_DAMAGE_MODIFIER, 0.0, 1000.0);
        THERMAL_REFERENCE_TEMPERATURE = BUILDER.comment("Temperature in kelvin of an ordinary fluid; the Thermal damage grows with the distance from this, hot or cold.")
                .defineInRange("thermalReferenceTemperature", StatDefaults.THERMAL_REFERENCE_TEMPERATURE, 0, 100000);
        THERMAL_SAFE_RANGE = BUILDER.comment("Kelvin either side of the reference temperature that does no Thermal damage (water is 300, so the default 50 means 250 to 350 is harmless). Past the range, damage grows with the distance beyond it.")
                .defineInRange("thermalSafeRange", StatDefaults.THERMAL_SAFE_RANGE, 0, 100000);
        // Intensity thresholds for the numeric hazards. A fluid's intensity (from the fluid_hazard data map) below the
        // mild start is harmless, from there to the severe start is Mild, from the severe start up is Severe. Tanks,
        // ducts and nodes refuse a fluid whose band is above their tolerance, and effects grow across their band.
        RADIATION_MILD_START = BUILDER.comment("Radiation: the lowest intensity that counts as Mild.")
                .defineInRange("radiationMildStart", StatDefaults.RADIATION_BAND.mildStart(), 1, 100000);
        RADIATION_SEVERE_START = BUILDER.comment("Radiation: the lowest intensity that counts as Severe.")
                .defineInRange("radiationSevereStart", StatDefaults.RADIATION_BAND.severeStart(), 1, 100000);
        RADIATION_MAX = BUILDER.comment("Radiation: the intensity where the Severe effect stops growing.")
                .defineInRange("radiationMax", StatDefaults.RADIATION_BAND.max(), 1, 100000);
        RADIATION_EFFECT_MODIFIER = BUILDER.comment("Radiation effect strength. A machine processing a radioactive fluid without enough tolerance spends more fuel per cycle (up to x2 across the Mild band, up to x3 across the Severe band) and, in the Severe band, loses up to 2 HP per cycle; this scales the extra fuel and the HP loss. 0 turns the effect off.")
                .defineInRange("radiationEffectModifier", StatDefaults.RADIATION_EFFECT_MODIFIER, 0.0, 1000.0);
        BIOHAZARD_START = BUILDER.comment("Biohazard: the lowest intensity that counts as hazardous (Biohazard has a single band).")
                .defineInRange("biohazardStart", StatDefaults.BIOHAZARD_BAND.mildStart(), 1, 100000);
        BIOHAZARD_MAX = BUILDER.comment("Biohazard: the intensity where the effect stops growing.")
                .defineInRange("biohazardMax", StatDefaults.BIOHAZARD_BAND.max(), 1, 100000);
        BIOHAZARD_EFFECT_MODIFIER = BUILDER.comment("Biohazard effect strength. A machine processing a Biohazard fluid without the tolerance works and heals slower, down to x0.5 of both across the band (x1 at the start of it); this scales how far down it goes. 0 turns the effect off.")
                .defineInRange("biohazardEffectModifier", StatDefaults.BIOHAZARD_EFFECT_MODIFIER, 0.0, 1.9);
        METAPHYSICAL_EFFECT_MODIFIER = BUILDER.comment("Metaphysical effect strength. A machine processing a Metaphysical fluid without enough tolerance gets an erratic work speed each cycle (up to +-50% across the Mild band, up to +-100% across the Severe band) and, in the Severe band, also random fuel use (up to +-50%) and random HP swings (up to 2 HP up or down); this scales how wide the swings are. 0 turns the effect off.")
                .defineInRange("metaphysicalEffectModifier", StatDefaults.METAPHYSICAL_EFFECT_MODIFIER, 0.0, 1.9);
        METAPHYSICAL_MILD_START = BUILDER.comment("Metaphysical: the lowest intensity that counts as Mild.")
                .defineInRange("metaphysicalMildStart", StatDefaults.METAPHYSICAL_BAND.mildStart(), 1, 100000);
        METAPHYSICAL_SEVERE_START = BUILDER.comment("Metaphysical: the lowest intensity that counts as Severe.")
                .defineInRange("metaphysicalSevereStart", StatDefaults.METAPHYSICAL_BAND.severeStart(), 1, 100000);
        METAPHYSICAL_MAX = BUILDER.comment("Metaphysical: the intensity where the Severe effect stops growing.")
                .defineInRange("metaphysicalMax", StatDefaults.METAPHYSICAL_BAND.max(), 1, 100000);
        BUILDER.pop();

        // Family tables: one section per family, between the global values and each machine's own. Every value starts
        // empty (lists) or -1 (numbers), which means "use the global value", so a server owner writes only what they
        // want to change for the whole family. A section only has the keys that mean something to the family.
        for (MachineFamily family : MachineFamily.values()) {
            BUILDER.comment("Shared overrides for the " + family.getSectionName() + " family. They replace the global values for these machines, and a machine's own section replaces them in turn. Leave a list empty (or a number at -1) to use the global value.")
                    .push(family.getSectionName());
            FAMILY_SECTIONS.put(family, buildSection(family.usesFuelAndHealth(), family.handlesHazards(), family.usesPassiveYield()));
            BUILDER.pop();
        }

        // Per-machine overrides. Every value starts empty (lists) or -1 (numbers), which means "use the family or
        // global value", so a server owner writes only what they want to change for that machine.
        // A machine's section only has the keys that mean something to it (see MachineStatKey), so the Drooling family's
        // sections leave out fuel use, max health and the hazard times.
        BUILDER.comment("Per-machine overrides. They replace the family table (if the machine is in a family) and the global values. Leave a list empty (or a number at -1) to use those.")
                .push("machines");
        for (MachineStatKey machine : MachineStatKey.values()) {
            BUILDER.push(machine.getSectionName());
            MACHINE_SECTIONS.put(machine, buildSection(machine.usesFuelAndHealth(), machine.handlesHazards(), machine.usesPassiveYield()));
            BUILDER.pop();
        }
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** A list that is either empty (use the shared values) or exactly {@code size} numbers in {@code [min, max]}. */
    private static ModConfigSpec.ConfigValue<List<? extends Number>> overrideList(
            String name, String comment, double min, double max, int size, String meaning) {
        return BUILDER.comment(comment + " Empty uses the shared values; otherwise exactly " + size + " values, " + meaning + ".")
                .defineList(List.of(name), () -> List.<Double>of(), () -> max,
                        o -> o instanceof Number n && n.doubleValue() >= min && n.doubleValue() <= max,
                        ModConfigSpec.Range.of(0, size));
    }

    /** A number that is -1 (use the shared value) or between 0 and {@code max}. */
    private static ModConfigSpec.DoubleValue overrideNumber(String name, String comment, double max) {
        return BUILDER.comment(comment + " -1 uses the shared value.").defineInRange(name, -1.0, -1.0, max);
    }

    private StatConfig() {}

    private static ModConfigSpec.ConfigValue<List<? extends Number>> tierList(
            String name, String comment, double[] defaults, double min, double max) {
        return sizedList(name, comment, defaults, min, max, TIERS, "L1 to L4");
    }

    /** A list that must have exactly {@code size} numbers in {@code [min, max]}, defaulting to {@code defaults}. */
    private static ModConfigSpec.ConfigValue<List<? extends Number>> sizedList(
            String name, String comment, double[] defaults, double min, double max, int size, String meaning) {
        List<Double> defaultList = Arrays.stream(defaults).boxed().toList();
        return BUILDER.comment(comment + " Exactly " + size + " values, " + meaning + ".")
                .defineList(List.of(name), () -> defaultList, () -> defaultList.get(defaultList.size() - 1),
                        o -> o instanceof Number n && n.doubleValue() >= min && n.doubleValue() <= max,
                        ModConfigSpec.Range.of(size, size));
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
                table(SIMPLE_MACHINE_PASSIVE_YIELD, StatDefaults.SIMPLE_MACHINE_PASSIVE_YIELD),
                table(SKIN_TANK_CAPACITY_MB, StatDefaults.SKIN_TANK_CAPACITY_MB),
                table(CRAW_STACKS, StatDefaults.CRAW_STACKS),
                OVERGROWTH_PERCENT.get(), OVERGROWTH_MAX_LEVEL.get(),
                HUNGER_DAMAGE_PER_CYCLE.get(), UNFUELED_SPEED.get().floatValue(),
                table(EVOLUTION_TIER_SECONDS, StatDefaults.EVOLUTION_TIER_SECONDS),
                EVOLUTION_HAZARD_NEW_SECONDS.get(), EVOLUTION_HAZARD_UPGRADE_SECONDS.get(),
                EVOLUTION_OVERGROWTH_SECONDS.get(), EVOLUTION_OVERGROWTH_GROWTH_PERCENT.get(),
                EVOLUTION_COUNTDOWN_SECONDS.get(),
                THERMAL_DAMAGE_MODIFIER.get(), THERMAL_REFERENCE_TEMPERATURE.get(), THERMAL_SAFE_RANGE.get(),
                new HazardBand(RADIATION_MILD_START.get(), RADIATION_SEVERE_START.get(), RADIATION_MAX.get()),
                new HazardBand(BIOHAZARD_START.get(), BIOHAZARD_MAX.get(), BIOHAZARD_MAX.get()),
                new HazardBand(METAPHYSICAL_MILD_START.get(), METAPHYSICAL_SEVERE_START.get(), METAPHYSICAL_MAX.get()),
                RADIATION_EFFECT_MODIFIER.get(), BIOHAZARD_EFFECT_MODIFIER.get(), METAPHYSICAL_EFFECT_MODIFIER.get(),
                buildFamilyOverrides(), buildMachineOverrides());
    }


    /** One section of optional overrides (a family's or a machine's), with only the keys the flags say apply. */
    private static MachineSection buildSection(boolean usesFuelAndHealth, boolean handlesHazards, boolean usesPassiveYield) {
        return new MachineSection(
                overrideList("speedMultiplier", "Processing speed multiplier per tier.", 0.1, 1000, TIERS, "L1 to L4"),
                usesFuelAndHealth ? overrideList("fuelUseMultiplier", "Fuel used per cycle, relative to L1, per tier.", 0.1, 1000, TIERS, "L1 to L4") : null,
                usesFuelAndHealth ? overrideList("maxHealth", "Maximum machine health per tier.", 1, 1000000, TIERS, "L1 to L4") : null,
                overrideList("tankCapacityMb", "Capacity of each tank, in millibuckets, per tier.", 1, 100000000, TIERS, "L1 to L4"),
                usesPassiveYield ? overrideList("passiveYield", "Millibuckets per second made with no input, per tier.", 0.1, 1000000, TIERS, "L1 to L4") : null,
                overrideList("generalModuleSlots", "General Module slots per tier (0 to 4).", 0, 4, TIERS, "L1 to L4"),
                overrideList("tierSeconds", "Seconds of production an Evolution Module takes to raise the tier.", 1, 10000000, 3, "L2, L3, L4"),
                handlesHazards ? overrideNumber("hazardNewSeconds", "Seconds to add a hazard family the machine does not hold yet.", 10000000) : null,
                handlesHazards ? overrideNumber("hazardUpgradeSeconds", "Seconds to raise a held hazard family from Mild to Severe.", 10000000) : null,
                overrideNumber("overgrowthSeconds", "Seconds to reach Overgrowth level 1.", 10000000),
                overrideNumber("overgrowthGrowthPercent", "Percent of the level 1 time each further Overgrowth level adds.", 1000));
    }

    private static MachineOverrides toOverrides(MachineSection section) {
        return new MachineOverrides(
                optionalTable(section.speed(), TIERS),
                optionalTable(section.fuelUse(), TIERS),
                optionalTable(section.maxHealth(), TIERS),
                optionalTable(section.tankCapacityMb(), TIERS),
                optionalTable(section.passiveYield(), TIERS),
                optionalSlots(section.moduleSlots()),
                optionalTable(section.evolutionTierSeconds(), 3),
                optionalNumber(section.evolutionHazardNewSeconds()),
                optionalNumber(section.evolutionHazardUpgradeSeconds()),
                optionalNumber(section.evolutionOvergrowthSeconds()),
                optionalNumber(section.evolutionOvergrowthGrowthPercent()));
    }

    private static Map<MachineFamily, MachineOverrides> buildFamilyOverrides() {
        Map<MachineFamily, MachineOverrides> overrides = new EnumMap<>(MachineFamily.class);
        for (Map.Entry<MachineFamily, MachineSection> entry : FAMILY_SECTIONS.entrySet()) {
            overrides.put(entry.getKey(), toOverrides(entry.getValue()));
        }
        return overrides;
    }

    private static Map<MachineStatKey, MachineOverrides> buildMachineOverrides() {
        Map<MachineStatKey, MachineOverrides> overrides = new EnumMap<>(MachineStatKey.class);
        for (Map.Entry<MachineStatKey, MachineSection> entry : MACHINE_SECTIONS.entrySet()) {
            overrides.put(entry.getKey(), toOverrides(entry.getValue()));
        }
        return overrides;
    }

    /** The list as a table, or null (use the shared table) if it is empty or not exactly {@code size} values. */
    private static double[] optionalTable(ModConfigSpec.ConfigValue<List<? extends Number>> value, int size) {
        if (value == null) return null; // a key this machine's section does not have
        List<? extends Number> list = value.get();
        if (list.isEmpty()) return null;
        if (list.size() != size) {
            Dermicraft.LOGGER.warn("Stat config list {} has {} values, expected {} or none; using the shared values.",
                    value.getPath(), list.size(), size);
            return null;
        }
        double[] out = new double[size];
        for (int i = 0; i < size; i++) out[i] = list.get(i).doubleValue();
        return out;
    }

    private static int[] optionalSlots(ModConfigSpec.ConfigValue<List<? extends Number>> value) {
        double[] asDoubles = optionalTable(value, TIERS);
        if (asDoubles == null) return null;
        int[] out = new int[asDoubles.length];
        for (int i = 0; i < out.length; i++) out[i] = (int) Math.round(asDoubles[i]);
        return out;
    }

    /** The number, or null (use the shared value) when it is negative. */
    private static Double optionalNumber(ModConfigSpec.DoubleValue value) {
        if (value == null) return null; // a key this machine's section does not have
        double number = value.get();
        return number < 0 ? null : number;
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