package net.scruffy.dermicraft.block.custom.duct;

import net.scruffy.dermicraft.hazard.HazardProfile;

/**
 * Tier 2 Innards Duct -- same transport, just tolerates thermal-hazard fluids. See
 * {@link InnardsDuctBlock}'s own identical shape; the only difference is the profile returned here.
 */
public class CharredInnardsDuctBlock extends AbstractInnardsDuctBlock {

    public CharredInnardsDuctBlock(Properties properties) {
        super(properties);
    }

    @Override
    public HazardProfile hazardProfile() {
        return HazardProfile.TIER_2;
    }
}
