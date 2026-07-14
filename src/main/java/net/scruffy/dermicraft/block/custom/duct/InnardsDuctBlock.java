package net.scruffy.dermicraft.block.custom.duct;

import net.scruffy.dermicraft.hazard.HazardProfile;

/**
 * Tier 1 Innards Duct - the dumb transport segment. All behaviour currently lives in
 * {@link AbstractInnardsDuctBlock}; this concrete class exists so upgrade tiers can be added as
 * sibling subclasses of the shared base.
 */
public class InnardsDuctBlock extends AbstractInnardsDuctBlock {

    public InnardsDuctBlock(Properties properties) {
        super(properties);
    }

    @Override
    public HazardProfile hazardProfile() {
        return HazardProfile.TIER_1;
    }
}
