package net.scruffy.dermicraft.block.custom;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Plain {@link LiquidBlock} with one difference: {@link #getName()} delegates to the fluid's own
 * {@code FluidType} description (the {@code fluid_type.dermicraft.*} lang key) instead of vanilla's
 * default {@code block.dermicraft.<id>_block} key. No fluid block in the mod has ever had a real
 * {@code block.*} lang entry -- every one silently fell back to its untranslated registry id
 * whenever something displayed the block's name directly (looking at the world block, F3, a HUD mod
 * like Jade), even though every other place a fluid's identity shows up (buckets, tank/screen
 * tooltips) already reads the correct, translated {@code fluid_type} name. Reusing that same name
 * here fixes every fluid block at once rather than needing a duplicate lang entry per fluid.
 */
public class ModLiquidBlock extends LiquidBlock {

    private final FlowingFluid fluid;

    public ModLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
        this.fluid = fluid;
    }

    @Override
    public MutableComponent getName() {
        return fluid.getFluidType().getDescription().copy();
    }
}
