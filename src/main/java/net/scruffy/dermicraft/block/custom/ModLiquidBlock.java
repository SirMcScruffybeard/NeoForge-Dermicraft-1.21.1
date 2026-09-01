package net.scruffy.dermicraft.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Plain {@link LiquidBlock} with two differences from vanilla:
 *
 * <p>{@link #getName()} delegates to the fluid's own {@code FluidType} description (the
 * {@code fluid_type.dermicraft.*} lang key) instead of vanilla's default
 * {@code block.dermicraft.<id>_block} key. No fluid block in the mod has ever had a real
 * {@code block.*} lang entry -- every one silently fell back to its untranslated registry id
 * whenever something displayed the block's name directly (looking at the world block, F3, a HUD mod
 * like Jade), even though every other place a fluid's identity shows up (buckets, tank/screen
 * tooltips) already reads the correct, translated {@code fluid_type} name. Reusing that same name
 * here fixes every fluid block at once rather than needing a duplicate lang entry per fluid.
 *
 * <p>{@link #randomTick} gives every fluid's already-authored {@code FluidType#temperature} value a
 * real world effect for the first time (see the {@code project_fluid_negative_density_inert} memory
 * for the general pattern -- NeoForge's own FluidType properties are otherwise inert unless a mod
 * hooks them up itself). Piggybacks entirely on vanilla's own random-tick scheduler rather than a
 * bespoke tick-cycle/registry -- the same "small, automatically-staggered fraction of loaded blocks
 * per tick" mechanism vanilla already uses for crop growth, leaf decay, and biome-driven ice/snow, so
 * this is cheap by construction with zero extra bookkeeping. EVERY fluid block in the mod goes
 * through this class (see {@code ModFluids}), so this is automatically scoped to only our own
 * fluids -- vanilla water/lava and every other mod's fluids use their own block classes entirely and
 * are completely untouched.
 */
public class ModLiquidBlock extends LiquidBlock {

    private final FlowingFluid fluid;

    /** Vanilla water's own baseline (see every vanilla {@code FluidType.Properties} default) --
     * neutral point the hot/cold thresholds below are measured against, not an invented number. */
    private static final int NEUTRAL_TEMPERATURE = 300;
    /** Above this, a fluid melts nearby ice/snow -- only the Molten family (500-1600) currently
     * qualifies; every non-Molten fluid sits at 280-310, comfortably below this. Public --
     * {@code FluidHazardEvents} reuses this exact number for entity-ignite purposes, so the two
     * "is this fluid hot" checks can never drift apart. */
    public static final int HOT_THRESHOLD = 400;
    /** Below this, a fluid freezes nearby water and grows snow on nearby ground -- only Ender
     * Essence (200) currently qualifies; nothing else in the mod is authored colder than
     * {@link #NEUTRAL_TEMPERATURE}. */
    private static final int COLD_THRESHOLD = 250;

    /** Whether {@code type} is one of THIS mod's own fluids AND hot enough to melt/scorch/ignite --
     * the {@code instanceof BaseFluidType} check is what keeps this from also matching vanilla Lava
     * (a real temperature of 1300, well past {@link #HOT_THRESHOLD}) or another mod's hot fluid,
     * either of which would otherwise double up with vanilla's own existing lava-contact handling
     * (or reach outside this mod entirely) -- every fluid registered in {@code ModFluidTypes} uses
     * {@link net.scruffy.dermicraft.fluid.BaseFluidType} specifically, so this is exclusive to us. */
    public static boolean isHotDermicraftFluid(net.neoforged.neoforge.fluids.FluidType type) {
        return type instanceof net.scruffy.dermicraft.fluid.BaseFluidType && type.getTemperature() >= HOT_THRESHOLD;
    }

    public ModLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
        this.fluid = fluid;
    }

    @Override
    public MutableComponent getName() {
        return fluid.getFluidType().getDescription().copy();
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int temperature = fluid.getFluidType().getTemperature(state.getFluidState(), level, pos);
        if (temperature >= HOT_THRESHOLD) {
            meltNearby(level, pos);
            scorchSpread(level, pos, random);
        } else if (temperature <= COLD_THRESHOLD) {
            freezeNearby(level, pos);
            frostSpread(level, pos, random);
        }
    }

    /** One face-neighbor's worth of ice family melted to water, or one snow layer removed --
     * whichever this particular random tick's neighbor happens to be, same "a little at a time,
     * spread across many ticks" pacing the random-tick scheduler already gives every other block
     * that uses it. */
    private static void meltNearby(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);

            if (neighborState.is(Blocks.ICE) || neighborState.is(Blocks.PACKED_ICE) || neighborState.is(Blocks.BLUE_ICE)) {
                level.setBlockAndUpdate(neighbor, Fluids.WATER.defaultFluidState().createLegacyBlock());
            } else if (neighborState.is(Blocks.SNOW)) {
                level.setBlockAndUpdate(neighbor, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** Freezes an adjacent water source to Ice -- immediate face-neighbors only, every cold tick,
     * same as {@link #meltNearby}'s own melt side. */
    private static void freezeNearby(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);

            if (neighborState.is(Blocks.WATER) && neighborState.getFluidState().isSource()) {
                level.setBlockAndUpdate(neighbor, Blocks.ICE.defaultBlockState());
            }
        }
    }

    /** Horizontal reach of the frost/scorch-spread roll below -- shared by both, same reasoning
     * either direction. */
    private static final int SPREAD_HORIZONTAL_RADIUS = 4;
    /** Vertical reach of the frost/scorch-spread roll below -- tighter than horizontal since
     * spreading floor-to-floor through a building reads as a bigger overreach than spreading
     * sideways across open ground. */
    private static final int SPREAD_VERTICAL_RADIUS = 2;

    /** One random offset within a small area around the fluid, same "one attempt, no area scan"
     * shape vanilla's own spreading blocks (Mycelium, Fire) use to grow organically over many
     * random ticks rather than all at once -- shared by {@link #frostSpread} and
     * {@link #scorchSpread}, which only differ in what they do once they land somewhere. */
    private static BlockPos rollSpreadTarget(BlockPos pos, RandomSource random) {
        return pos.offset(
                random.nextInt(SPREAD_HORIZONTAL_RADIUS * 2 + 1) - SPREAD_HORIZONTAL_RADIUS,
                random.nextInt(SPREAD_VERTICAL_RADIUS * 2 + 1) - SPREAD_VERTICAL_RADIUS,
                random.nextInt(SPREAD_HORIZONTAL_RADIUS * 2 + 1) - SPREAD_HORIZONTAL_RADIUS);
    }

    /**
     * Whatever the roll lands on decides the effect -- a crop-like block (anything with an
     * {@code age} property -- covers {@code CropBlock} and its subclasses, Nether Wart, stem crops,
     * generically, not a per-block-type list) gets frosted back to its zero-growth state; a sapling
     * is killed outright (dropped, same as breaking it by hand); otherwise, an empty spot that
     * vanilla's own {@link SnowLayerBlock#canSurvive} would allow real snowfall to rest on gets a
     * Snow layer -- never forced onto a spot vanilla itself would refuse.
     */
    private static void frostSpread(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos target = rollSpreadTarget(pos, random);
        BlockState targetState = level.getBlockState(target);

        for (Property<?> property : targetState.getProperties()) {
            if (property instanceof IntegerProperty ageProperty && "age".equals(ageProperty.getName())) {
                level.setBlockAndUpdate(target, targetState.setValue(ageProperty, 0));
                return;
            }
        }

        if (targetState.is(BlockTags.SAPLINGS)) {
            level.destroyBlock(target, true);
            return;
        }

        if (level.isEmptyBlock(target) && Blocks.SNOW.defaultBlockState().canSurvive(level, target)) {
            level.setBlockAndUpdate(target, Blocks.SNOW.defaultBlockState());
        }
    }

    /**
     * Scorch/dry counterpart to {@link #frostSpread} -- same roll, opposite effects: Grass Block
     * scorches down to plain Dirt; Farmland's moisture resets to 0 (dries out) rather than being
     * destroyed outright, same "damage the state, not the block" restraint {@link #frostSpread}'s
     * own crop-age reset already uses; a sapling breaks (dropped, same standard break as frost's
     * own -- destroying it outright with no drop read as too harsh); Snow (layer or block) melts to
     * nothing, the same effect {@link #meltNearby} already gives immediate neighbors, just reaching
     * further out.
     */
    private static void scorchSpread(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos target = rollSpreadTarget(pos, random);
        BlockState targetState = level.getBlockState(target);

        if (targetState.is(Blocks.GRASS_BLOCK)) {
            level.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
            return;
        }

        if (targetState.is(Blocks.FARMLAND)) {
            int moisture = targetState.getValue(FarmBlock.MOISTURE);
            if (moisture > 0) {
                level.setBlockAndUpdate(target, targetState.setValue(FarmBlock.MOISTURE, 0));
            }
            return;
        }

        if (targetState.is(BlockTags.SAPLINGS)) {
            level.destroyBlock(target, true);
            return;
        }

        if (targetState.is(Blocks.SNOW) || targetState.is(Blocks.SNOW_BLOCK)) {
            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        }
    }
}
