package net.scruffy.dermicraft.machine;

import org.jetbrains.annotations.Nullable;

/**
 * The machines that have their own section in the server config, where a stat or an evolution time can be set
 * for that machine alone. Anything a section leaves empty falls back to the shared tables, so a server owner
 * only writes what they want to change. A machine is added here when it starts reading the stat tables (moves
 * onto the evolution state), not before, so the config never offers settings that do nothing. A machine in a
 * {@link MachineFamily} also falls back to that family's table before the global values.
 */
public enum MachineStatKey {
    MASTICATOR("masticator"),
    METASTASIZER("metastasizer"),
    MUTATOR("mutator"),
    EFFLUENTCER("effluentcer"),
    RENDER_KILN("render_kiln"),
    RENDER_FURNACE("render_furnace"),
    GRAFTING_TABLE("grafting_table"),
    DROOLING_CAULDRON("drooling_cauldron", MachineFamily.DROOLING),
    DROOLING_CRUCIBLE("drooling_crucible", MachineFamily.DROOLING),
    DROOLING_GEODE("drooling_geode", MachineFamily.DROOLING),
    MR_FARMER("mr_farmer", MachineFamily.FARM),
    MR_SHEPHERD("mr_shepherd", MachineFamily.FARM),
    MR_LOGGER("mr_logger", MachineFamily.FARM);

    private final String SECTION_NAME;
    @Nullable
    private final MachineFamily FAMILY;
    private final boolean HANDLES_HAZARDS;
    private final boolean USES_FUEL;
    private final boolean USES_HEALTH;
    private final boolean USES_PASSIVE_YIELD;
    private final boolean USES_FIELD;

    /** An ordinary fueled machine: no family, fuel and HP, hazards, no passive yield. */
    MachineStatKey(String sectionName) {
        this(sectionName, null, true, true, true, false, false);
    }

    /** A machine in a family: its keys follow the family's flags. */
    MachineStatKey(String sectionName, MachineFamily family) {
        this(sectionName, family, family.handlesHazards(), family.usesFuel(), family.usesHealth(), family.usesPassiveYield(),
                family.usesField());
    }

    /** The full form, for a machine with no family but unusual keys (for example a future non-Drooling machine that
     * makes fluid with no input: {@code (name, null, true, true, true)}). */
    MachineStatKey(String sectionName, @Nullable MachineFamily family, boolean handlesHazards, boolean usesFuel,
                   boolean usesHealth, boolean usesPassiveYield, boolean usesField) {
        SECTION_NAME = sectionName;
        FAMILY = family;
        HANDLES_HAZARDS = handlesHazards;
        USES_FUEL = usesFuel;
        USES_HEALTH = usesHealth;
        USES_PASSIVE_YIELD = usesPassiveYield;
        USES_FIELD = usesField;
    }

    /** The machine's section name in the config file. */
    public String getSectionName() {
        return SECTION_NAME;
    }

    /** The family whose shared table sits between the global values and this machine's own, or null. */
    @Nullable
    public MachineFamily getFamily() {
        return FAMILY;
    }

    /** Whether hazard tolerances mean anything to this machine. False for the Drooling family, which has no fluid
     * input and no fuel: its Module tab hides the tolerances, hazard Evolution Modules are refused, and its config
     * section leaves out the hazard evolution times. */
    public boolean handlesHazards() {
        return HANDLES_HAZARDS;
    }

    /** Whether the machine burns fuel. False for the Drooling family, whose config section leaves out
     * {@code fuelUseMultiplier}. */
    public boolean usesFuel() {
        return USES_FUEL;
    }

    /** Whether the machine has HP. False for the Drooling family and the farm machines, whose config sections leave out
     * {@code maxHealth}. */
    public boolean usesHealth() {
        return USES_HEALTH;
    }

    /** Whether the machine works a player-set field. Only those machines' config sections have the field size and
     * distance keys. */
    public boolean usesField() {
        return USES_FIELD;
    }

    /** Whether the machine makes fluid with no input at a rate set in the config. Only those machines' config
     * sections have a {@code passiveYield} key, and the global passive yield affects only them. */
    public boolean usesPassiveYield() {
        return USES_PASSIVE_YIELD;
    }
}
