package net.scruffy.dermicraft.tank;

import net.scruffy.dermicraft.hazard.HazardProfile;

/** Tier-1 tank: accepts only non-hazardous fluids. */
public class VulnerableTank extends ModFluidTank {

    public VulnerableTank(int capacity, int slot) {
        super(capacity, slot, HazardProfile.TIER_1);
    }
}
