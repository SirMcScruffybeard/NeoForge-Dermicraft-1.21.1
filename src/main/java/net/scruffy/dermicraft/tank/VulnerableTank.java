package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.hazard.HazardProfile;

import java.util.function.Supplier;

/** Tier-1 tank: accepts only non-hazardous fluids. */
public class VulnerableTank extends ModFluidTank {

    public VulnerableTank(int capacity, int slot) {
        super(capacity, slot, HazardProfile.TIER_1);
    }

    /**
     * Tier-1 base, plus whatever a currently-installed Safety Module grants -- read fresh on every
     * fill/drain rather than fixed at construction, so swapping the Module in or out of the owning
     * machine's slot takes effect immediately. A {@link Supplier} rather than a plain
     * {@link HazardProfile}, since the tank object is built once at block-entity construction while
     * the installed Module can change for the tank's whole lifetime -- see
     * {@code IHaveModules#installedHazardProfile(HazardProfile, net.minecraft.world.item.ItemStack)}
     * for the union rule the supplier is expected to apply.
     */
    public VulnerableTank(int capacity, int slot, Supplier<HazardProfile> profileSupplier) {
        super(capacity, slot, (FluidStack stack) -> profileSupplier.get().accepts(stack));
    }
}
