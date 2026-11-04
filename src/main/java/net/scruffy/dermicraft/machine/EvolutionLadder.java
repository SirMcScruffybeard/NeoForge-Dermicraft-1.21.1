package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;
import org.jetbrains.annotations.Nullable;

/**
 * The two level ladders a Level Evolution Module can climb: the tier (L1 to L4) and Overgrowth (above L4). Both
 * are "the exact next level" rules; what differs is the state field raised, how long a step takes and the
 * limits, all inside {@link EvolutionRules}.
 */
public enum EvolutionLadder {
    TIER(EvolutionRules::evaluateTier),
    OVERGROWTH(EvolutionRules::evaluateOvergrowth);

    /** A ladder's rule: the machine asking (for its own times), the machine's state and the level on the module. */
    @FunctionalInterface
    private interface Rule {
        EvolutionResult apply(@Nullable MachineStatKey machine, EvolutionStateData state, int level);
    }

    private final Rule RULE;

    EvolutionLadder(Rule rule) {
        RULE = rule;
    }

    public EvolutionResult evaluate(@Nullable MachineStatKey machine, EvolutionStateData state, int level) {
        return RULE.apply(machine, state, level);
    }
}
