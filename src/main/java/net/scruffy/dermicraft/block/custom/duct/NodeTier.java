package net.scruffy.dermicraft.block.custom.duct;

import net.scruffy.dermicraft.hazard.HazardProfile;

/**
 * The tunable knobs that distinguish one Node tier from the next. Node tiers vary purely by
 * throughput and fluid-hazard tolerance -- any genuinely new ability is expected to be a separate
 * block, not a tier here -- so this record deliberately carries no behaviour hooks.
 *
 * <p>The Node's tank is a transport buffer, not storage -- its capacity ({@link NodeBlockEntity#TANK_CAPACITY})
 * is fixed across every tier and is deliberately NOT a field here.
 *
 * <p>A Node block carries a NodeTier (see {@link TieredNode}) and its block entity reads its numbers
 * from it, so a higher-throughput / more-hazard-tolerant tier is just a new block registration plus
 * a new NodeTier constant -- no new block-entity code.
 *
 * @param itemThroughput  max items moved per leg per transfer cycle
 * @param fluidThroughput max fluid moved per leg per transfer cycle, in mB
 * @param hazardProfile   which fluids the buffer tank will hold (the whole run's fluid gate, since
 *                        the Node's tank is the only place fluid ever sits in the duct system)
 */
public record NodeTier(int itemThroughput, int fluidThroughput, HazardProfile hazardProfile) {

    /** The baseline the progenitor Node uses. Matches the old hard-coded values. */
    public static final NodeTier TIER_1 = new NodeTier(4, 100, HazardProfile.TIER_1);

    // To add a stat-only upgrade tier later, declare it here and hand it to a new node block, e.g.:
    // public static final NodeTier TIER_2 = new NodeTier(16, 400, HazardProfile.TIER_2);
}
