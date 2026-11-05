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
    EFFLUENTCER("effluentcer");

    private final String SECTION_NAME;

    MachineStatKey(String sectionName) {
        SECTION_NAME = sectionName;
    }

    /** The machine's section name in the config file. */
    public String getSectionName() {
        return SECTION_NAME;
    }
}
