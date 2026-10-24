package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;

/** The answer {@link EvolutionRules} gives for a module on a machine: an upgrade that will apply, or why it will not. */
public sealed interface EvolutionResult {

    /**
     * The module is live. When its time has been served the machine's state becomes {@code newState}.
     *
     * @param durationTicks how long the upgrade takes, from the config
     */
    record Upgrade(EvolutionStateData newState, int durationTicks) implements EvolutionResult {
    }

    /**
     * The module does nothing on this machine and is not consumed. {@code reasonKey} is a stable translation
     * key suffix (see the constants below) that a screen or the Flesh Lab can turn into its own wording.
     */
    record Blocked(String reasonKey) implements EvolutionResult {
    }

    String HAZARD_CAP_REACHED = "hazard_cap_reached";
    String HAZARD_ALREADY_HELD = "hazard_already_held";
    String RAISE_TIER_FIRST = "raise_tier_first";
    String NO_FURTHER_LEVELS = "no_further_levels";
    /** An Overgrowth module for a level above the next one; the module for the next level must be used first. */
    String PREVIOUS_LEVEL_FIRST = "previous_level_first";
}
