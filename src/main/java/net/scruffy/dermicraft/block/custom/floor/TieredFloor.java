package net.scruffy.dermicraft.block.custom.floor;

import net.scruffy.dermicraft.hazard.HazardProfile;

/**
 * Implemented by a Lab Floor block to declare its tier (see dermicraft-machine-notes.md -> Lab
 * Floor -> Floor Tiers). Mirrors {@code machine.TieredMachine}'s "tier lives on the block itself"
 * convention -- {@link FloorNetwork} reads it straight off the blockstate during its walk, no block
 * entity needed.
 *
 * <p>Bundles the tier's two currently-built consequences: {@link #hazardProfile()} (fluid-type
 * capability, weakest-link across the whole connected network) and {@link #reach()} (local
 * floor-to-floor / floor-to-container connection span, per-floor rather than weakest-link).
 * Throughput, the design's third tier consequence, was dropped rather than built (2026-08-09).
 */
public interface TieredFloor {

    int floorTier();

    /** Tier 1 = safe fluids only, ... Tier 4 = the full hazard ladder -- see HazardProfile's own
     * ladder presets, reused verbatim rather than a bespoke floor-side ranking. */
    default HazardProfile hazardProfile() {
        return switch (floorTier()) {
            case 1 -> HazardProfile.TIER_1;
            case 2 -> HazardProfile.TIER_2;
            case 3 -> HazardProfile.TIER_3;
            default -> HazardProfile.TIER_4;
        };
    }

    /** Extra blocks a connection may skip past this floor tile to reach the next floor tile or a
     * container -- 0 (adjacent only) for Tier 1-2, 1 (one-block gap) for Tier 3-4. */
    default int reach() {
        return floorTier() >= 3 ? 1 : 0;
    }
}
