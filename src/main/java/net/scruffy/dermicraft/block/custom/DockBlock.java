package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scruffy.dermicraft.block.entity.custom.DockBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dock -- placeholder pass, see DockBlockEntity's javadoc. Registered as a single facing-aware
 * block purely to place/preview the in-progress GeckoLib model; the eventual real 3x3x3 multiblock
 * (core + delegating floor tiles + non-interactive frame shell, per project_dock_model_plan) isn't
 * built yet.
 */
public class DockBlock extends ModBaseEntityBlock {

    public static final MapCodec<DockBlock> CODEC = simpleCodec(DockBlock::new);

    // GeoBlockRenderer auto-detects this exact property (HorizontalDirectionalBlock.FACING, same
    // DirectionProperty instance as BlockStateProperties.HORIZONTAL_FACING) and rotates the whole
    // GeckoLib model around the block's center accordingly -- same idiom as WorkbenchBlock's FACING.
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Derived directly from the current placeholder dock.geo.json ("base"/"roof" cubes, converted
    // px->block fraction by /16), then shifted up by +1 block on Y to match
    // DockBlockEntityRenderer's own +1 render offset (see its class javadoc), AND shifted +0.5 on
    // X/Z: the geo model's own coordinate space is centered on the block (matching GeckoLib's
    // convention of centering block models like entity models), but VoxelShape/collision space uses
    // the corner convention (block cell is [0,1]x[0,1]) -- GeckoLib's block renderer applies that
    // same +0.5 horizontally internally, so the shape has to match it explicitly since collision
    // isn't driven by the renderer. Only the floor and roof plates get a real shape, everything else
    // (frame/walls) is deliberately pass-through with no shape at all, per the model plan. These
    // numbers WILL change once the real model geometry is finalized.
    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(-1.0, 0.0, -1.0, 2.0, 0.5, 2.0),   // floor plate
            Shapes.box(-1.0, 2.75, -1.0, 2.0, 3.0, 2.0)   // roof plate
    );

    public DockBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @NotNull
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @NotNull
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // GeckoLib-rendered (no baked mesh of its own) -- same as WorkbenchBlock/WorkbenchTopBlock.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DockBlockEntity(blockPos, blockState);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
