package net.scruffy.dermicraft.interfaces;

/**
 * Implemented by any machine block entity that can accumulate progress toward transforming into a
 * hazard-gated evolution of itself (Drooling Cauldron -> Crucible, Masticator -> Charred Masticator,
 * Metastasizer -> Charred Metastasizer) via an installed Evolution Module. Exists purely so a single
 * renderer can draw the creeping evolution-progress overlay for all of them -- see
 * {@code EvolutionOverlayBlockEntityRenderer}.
 */
public interface IEvolvingMachine {

    /** 0 when not currently evolving (no Module, one with no real Evolution properties, or already
     * fully evolved); otherwise how far accumulated progress is toward the evolution threshold, 0-1. */
    float getEvolutionProgressFraction();
}
