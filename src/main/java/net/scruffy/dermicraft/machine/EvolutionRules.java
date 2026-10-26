package net.scruffy.dermicraft.machine;

import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.hazard.HazardFamily;

/**
 * The one place that decides whether an Evolution Module applies to a machine and what it would do. Each
 * Evolution Module item calls the rule for its kind from its {@code evaluate} method (the level ladders through
 * {@link EvolutionLadder}) with the machine's current {@link EvolutionStateData} and the level or hazard on its
 * stack; the machine asks the module, then owns the rest itself: counting the time, deciding when it advances,
 * and applying the new state. The Flesh Lab's available-upgrades list asks the same question, so both always
 * agree.
 *
 * <p>Rules (machine upgrade redesign notes):
 * <ul>
 *   <li><b>Tier:</b> a module only works as the machine's exact next tier (no skipping). Below it asks to
 *   raise the tier first; at or past it there is nothing further. Not available once Overgrowth has begun.</li>
 *   <li><b>Hazard:</b> a family holds at most one level, mild or severe, and a severe module covers mild too
 *   and may be applied without mild first. Adding a new family costs one of the tier's tolerance slots (the
 *   cap equals the tier); raising a family already held from mild to severe costs none and is faster. A level
 *   already held is not applied again.</li>
 *   <li><b>Overgrowth:</b> only at the top tier, only as the exact next level, up to the configured maximum.</li>
 * </ul>
 */
public final class EvolutionRules {

    private EvolutionRules() {}

    public static EvolutionResult evaluateTier(EvolutionStateData state, int targetTier) {
        // Once Overgrowth has begun the machine is at the top tier already.
        if (state.overgrowth() > 0 || targetTier <= state.tier()) {
            return new EvolutionResult.Blocked(EvolutionResult.NO_FURTHER_LEVELS);
        }
        if (targetTier > EvolutionStateData.MAX_TIER) {
            return new EvolutionResult.Blocked(EvolutionResult.NO_FURTHER_LEVELS);
        }
        if (targetTier != state.tier() + 1) {
            return new EvolutionResult.Blocked(EvolutionResult.RAISE_TIER_FIRST);
        }
        return new EvolutionResult.Upgrade(state.withTier(targetTier), EvolutionStats.getTierTicks(targetTier));
    }

    public static EvolutionResult evaluateHazard(EvolutionStateData state, HazardFamily family, int level) {
        if (family == null || level < 1 || level > family.getMaxLevel()) {
            return new EvolutionResult.Blocked(EvolutionResult.NO_FURTHER_LEVELS);
        }
        int held = state.level(family);
        if (held >= level) {
            return new EvolutionResult.Blocked(EvolutionResult.HAZARD_ALREADY_HELD);
        }
        if (held > 0) {
            // Mild to Severe in a family already held: no extra tolerance slot, and faster.
            return new EvolutionResult.Upgrade(state.withLevel(family, level), EvolutionStats.getHazardUpgradeTicks());
        }
        // A new family (including Severe without Mild first) needs a free tolerance slot: the cap equals the tier.
        if (state.getHeldFamilyCount() >= state.tier()) {
            return new EvolutionResult.Blocked(EvolutionResult.HAZARD_CAP_REACHED);
        }
        return new EvolutionResult.Upgrade(state.withLevel(family, level), EvolutionStats.getHazardNewTicks());
    }

    public static EvolutionResult evaluateOvergrowth(EvolutionStateData state, int targetLevel) {
        if (state.tier() < EvolutionStateData.MAX_TIER) {
            return new EvolutionResult.Blocked(EvolutionResult.RAISE_TIER_FIRST);
        }
        if (targetLevel <= state.overgrowth() || targetLevel > EvolutionStats.getOvergrowthMaxLevel()) {
            return new EvolutionResult.Blocked(EvolutionResult.NO_FURTHER_LEVELS);
        }
        if (targetLevel != state.overgrowth() + 1) {
            // Skipping levels is not allowed; the module for the next level must be used first.
            return new EvolutionResult.Blocked(EvolutionResult.PREVIOUS_LEVEL_FIRST);
        }
        return new EvolutionResult.Upgrade(state.withOvergrowth(targetLevel), EvolutionStats.getOvergrowthTicks(targetLevel));
    }
}
