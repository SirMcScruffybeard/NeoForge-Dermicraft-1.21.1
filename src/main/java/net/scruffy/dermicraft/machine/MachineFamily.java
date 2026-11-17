package net.scruffy.dermicraft.machine;

/**
 * A group of machines that share a config table of their own, between the global values and each machine's own
 * overrides: a stat resolves to the machine's own value if it has one, otherwise the family's, otherwise the global
 * one. A family's table is all optional (empty means "use the global value"), so adding a family changes nothing
 * until someone writes a value in it. The flags say which keys the family's section (and its machines' sections) has.
 */
public enum MachineFamily {
    /** Cauldron, Crucible, Geode: they make fluid with no input, burn no fuel, have no HP and handle no hazards. */
    DROOLING("drooling", false, false, false, true, false),
    /** Mr. Farmer, Mr. Shepherd and the ones that follow: they burn fuel but have no HP, handle no hazards, make no passive
     * fluid, and work a field whose largest size and distance are per-tier stats. */
    FARM("farm", false, true, false, false, true);

    private final String SECTION_NAME;
    private final boolean HANDLES_HAZARDS;
    private final boolean USES_FUEL;
    private final boolean USES_HEALTH;
    private final boolean USES_PASSIVE_YIELD;
    private final boolean USES_FIELD;

    MachineFamily(String sectionName, boolean handlesHazards, boolean usesFuel, boolean usesHealth,
                  boolean usesPassiveYield, boolean usesField) {
        SECTION_NAME = sectionName;
        HANDLES_HAZARDS = handlesHazards;
        USES_FUEL = usesFuel;
        USES_HEALTH = usesHealth;
        USES_PASSIVE_YIELD = usesPassiveYield;
        USES_FIELD = usesField;
    }

    /** The family's section name in the config file. */
    public String getSectionName() {
        return SECTION_NAME;
    }

    public boolean handlesHazards() {
        return HANDLES_HAZARDS;
    }

    public boolean usesFuel() {
        return USES_FUEL;
    }

    public boolean usesHealth() {
        return USES_HEALTH;
    }

    public boolean usesField() {
        return USES_FIELD;
    }

    public boolean usesPassiveYield() {
        return USES_PASSIVE_YIELD;
    }
}
