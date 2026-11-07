package net.scruffy.dermicraft.machine;

/**
 * The machines that have their own section in the server config, where a stat or an evolution time can be set
 * for that machine alone. Anything a section leaves empty falls back to the shared tables, so a server owner
 * only writes what they want to change. A machine is added here when it starts reading the stat tables (moves
 * onto the evolution state), not before, so the config never offers settings that do nothing.
 */
public enum MachineStatKey {
    MASTICATOR("masticator"),
    METASTASIZER("metastasizer"),
    MUTATOR("mutator"),
    EFFLUENTCER("effluentcer"),
    RENDER_KILN("render_kiln"),
    RENDER_FURNACE("render_furnace"),
    GRAFTING_TABLE("grafting_table"),
    DROOLING_CAULDRON("drooling_cauldron", false, false),
    DROOLING_CRUCIBLE("drooling_crucible", false, false),
    DROOLING_GEODE("drooling_geode", false, false);

    private final String SECTION_NAME;
    private final boolean HANDLES_HAZARDS;
    private final boolean USES_FUEL_AND_HEALTH;

    MachineStatKey(String sectionName) {
        this(sectionName, true, true);
    }

    MachineStatKey(String sectionName, boolean handlesHazards, boolean usesFuelAndHealth) {
        SECTION_NAME = sectionName;
        HANDLES_HAZARDS = handlesHazards;
        USES_FUEL_AND_HEALTH = usesFuelAndHealth;
    }

    /** The machine's section name in the config file. */
    public String getSectionName() {
        return SECTION_NAME;
    }

    /** Whether hazard tolerances mean anything to this machine. False for the Drooling family, which has no fluid
     * input and no fuel: its Module tab hides the tolerances, hazard Evolution Modules are refused, and its config
     * section leaves out the hazard evolution times. */
    public boolean handlesHazards() {
        return HANDLES_HAZARDS;
    }

    /** Whether the machine burns fuel and has HP. False for the Drooling family, whose config section leaves out
     * {@code fuelUseMultiplier} and {@code maxHealth}. */
    public boolean usesFuelAndHealth() {
        return USES_FUEL_AND_HEALTH;
    }
}
