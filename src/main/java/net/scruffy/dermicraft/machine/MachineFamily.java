package net.scruffy.dermicraft.machine;

/**
 * A group of machines that share a config table of their own, between the global values and each machine's own
 * overrides: a stat resolves to the machine's own value if it has one, otherwise the family's, otherwise the global
 * one. A family's table is all optional (empty means "use the global value"), so adding a family changes nothing
 * until someone writes a value in it. The flags say which keys the family's section (and its machines' sections) has.
 */
public enum MachineFamily {
    /** Cauldron, Crucible, Geode: they make fluid with no input, burn no fuel, have no HP and handle no hazards. */
    DROOLING("drooling", false, false, true);

    private final String SECTION_NAME;
    private final boolean HANDLES_HAZARDS;
    private final boolean USES_FUEL_AND_HEALTH;
    private final boolean USES_PASSIVE_YIELD;

    MachineFamily(String sectionName, boolean handlesHazards, boolean usesFuelAndHealth, boolean usesPassiveYield) {
        SECTION_NAME = sectionName;
        HANDLES_HAZARDS = handlesHazards;
        USES_FUEL_AND_HEALTH = usesFuelAndHealth;
        USES_PASSIVE_YIELD = usesPassiveYield;
    }

    /** The family's section name in the config file. */
    public String getSectionName() {
        return SECTION_NAME;
    }

    public boolean handlesHazards() {
        return HANDLES_HAZARDS;
    }

    public boolean usesFuelAndHealth() {
        return USES_FUEL_AND_HEALTH;
    }

    public boolean usesPassiveYield() {
        return USES_PASSIVE_YIELD;
    }
}
