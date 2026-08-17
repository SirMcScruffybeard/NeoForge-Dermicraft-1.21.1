package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.MrShepardBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Mirrors MrFarmerBlock's shape/interaction pattern exactly -- same 6-way facing, same on/off
// texture swap, same fluid-container-fills-fuel-tank interaction. See dermicraft-machine-notes.md
// -> Farming automation concepts -> Mr. Shepard.
public class MrShepardBlock extends ModBaseEntityBlock {

    public static final MapCodec<MrShepardBlock> CODEC = simpleCodec(MrShepardBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    // Collision extends a full 24px up -- deliberately matching a vanilla fence rather than the
    // model's own ~22px hat. A Shepard is set INTO the pen's fence line, so a plain full-cube block
    // would be a 1-block-tall gap in an otherwise 1.5-tall wall: an escape hatch animals could jump.
    // Blocks can still be placed on top; placement only checks the target position for replaceability
    // and entities, never a neighbour's collision shape (same as placing a block atop a fence).
    // The block above will visually clip the hat -- accepted, pending a new model.
    //
    // X AND Z both extend .25 block (4px) past each side (-4 to 20 instead of 0 to 16) to close the
    // sliver gap animals could otherwise squeeze through at the seam with the neighbouring fence
    // blocks. Both axes rather than just the fence-line axis -- this shape isn't rotated by FACING,
    // so it can't tell which way a given pen's wall actually runs.
    private static final VoxelShape COLLISION_SHAPE = Block.box(-4, 0, -4, 20, 24, 20);

    public MrShepardBlock(Properties properties) {
        super(properties.noLootTable());
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    /**
     * Only the COLLISION shape is raised, not {@link #getShape} -- the selection outline stays a
     * normal cube so the outline doesn't overlap and fight with whatever block sits above.
     */
    @NotNull
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
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
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MrShepardBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.MR_SHEPARD_BE.get(),
                (lvl, pos, st, be) -> be.tick(lvl));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof MrShepardBlockEntity be) {
                    be.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MrShepardBlockEntity be
                    && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection())) {
                be.setChanged();
                be.updateBlock();
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider menuProvider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
