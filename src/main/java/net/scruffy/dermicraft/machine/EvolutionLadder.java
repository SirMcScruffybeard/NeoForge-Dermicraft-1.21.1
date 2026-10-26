package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;

import java.util.function.BiFunction;

/**
 * The two level ladders a Level Evolution Module can climb: the tier (L1 to L4) and Overgrowth (above L4). Both
 * are "the exact next level" rules; what differs is the state field raised, how long a step takes and the
 * limits, all inside {@link EvolutionRules}.
 */
public enum EvolutionLadder {
    TIER(EvolutionRules::evaluateTier),
    OVERGROWTH(EvolutionRules::evaluateOvergrowth);

    private final BiFunction<EvolutionStateData, Integer, EvolutionResult> RULE;

    EvolutionLadder(BiFunction<EvolutionStateData, Integer, EvolutionResult> rule) {
        RULE = rule;
    }

    public EvolutionResult evaluate(EvolutionStateData state, int level) {
        return RULE.apply(state, level);
    }
}