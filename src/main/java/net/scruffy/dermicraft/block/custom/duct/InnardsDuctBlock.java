package net.scruffy.dermicraft.block.custom.duct;

import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.datagen.tag.ModTags;

import java.util.function.Predicate;

/**
 * Tier 1 Innards Duct - the dumb transport segment. All behaviour currently lives in
 * {@link AbstractInnardsDuctBlock}; this concrete class exists so upgrade tiers can be added as
 * sibling subclasses of the shared base.
 */
public class InnardsDuctBlock extends AbstractInnardsDuctBlock {

    // Same rule as VulnerableTank -- the mod-wide Tier 1 restriction.
    private static final Predicate<FluidStack> FLUID_FILTER = fluidStack -> !fluidStack.is(ModTags.Fluids.HAZARDOUS);

    public InnardsDuctBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<FluidStack> fluidFilter() {
        return FLUID_FILTER;
    }
}
