package net.scruffy.dermicraft.hazard;

import net.minecraft.util.StringRepresentable;

/**
 * The four hazard families a machine can hold a tolerance to (see the Machine Upgrade Redesign notes). A family counts
 * once against a machine's tolerance cap (the cap equals its tier) regardless of the level held. Each family has a
 * display initial and a top level: Thermal and Biohazard are on/off (max 1), Radiation and Metaphysical go
 * none/mild/severe (max 2). Levels are plain ints (0 = none) so they fit
 * {@link net.scruffy.dermicraft.component.EvolutionStateData}.
 *
 * <p>A fluid's level in a family comes from its numbers ({@link net.scruffy.dermicraft.machine.FluidHazards}); a
 * tolerance at level N covers fluids at level N and below.
 */
public enum HazardFamily implements StringRepresentable {
    THERMAL('T', 1),
    RADIATION('R', 2),
    BIOHAZARD('B', 1),
    METAPHYSICAL('M', 2);

    private final char INITIAL;
    private final int MAX_LEVEL;

    HazardFamily(char initial, int maxLevel) {
        INITIAL = initial;
        MAX_LEVEL = maxLevel;
    }

    public char getInitial() {
        return INITIAL;
    }

    public int getMaxLevel() { return MAX_LEVEL; }

    /** The name used when a family is written into data (an Evolution Module's component). */
    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
