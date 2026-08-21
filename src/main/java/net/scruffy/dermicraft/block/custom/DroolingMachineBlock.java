package net.scruffy.dermicraft.block.custom;

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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.block.entity.custom.DroolingMachineBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the Drooling machine family's blocks (Cauldron, Crucible). Everything here was
 * already 100% identical between {@code DroolingCauldronBlock} and what Crucible needed --
 * interaction, rotation, and placement logic, none of it referencing the machine's specific fluid.
 * Only {@code newBlockEntity}/{@code getTicker} (which concrete block entity type to build) and
 * each block's own {@code CODEC} (tied to its exact constructor reference, not generically
 * shareable) stay per-subclass.
 */
public abstract class DroolingMachineBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected DroolingMachineBlock(Properties properties) {
        super(properties
                .noLootTable()
                .ignitedByLava()
        );
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

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

    @NotNull
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    // Empty-hand quick-extraction is a CROUCH action -- a plain empty-hand click always falls
    // through to useWithoutItem (the GUI); crouch + empty hand pulls the ingredient item instead.
    // A held item that doesn't actually insert anywhere (ingredient slot already occupied) falls
    // through too, rather than eating the click. Mirrors MasticatorBlock's interaction shape.
    @Override @NotNull
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof DroolingMachineBlockEntity<?> be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (FluidUtil.interactWithFluidHandler(player, hand, be.getTank(null))) {
            be.setChanged();
            be.updateBlock();
            return ItemInteractionResult.SUCCESS;
        }

        boolean crouchExtract = player.getItemInHand(hand).isEmpty() && player.isShiftKeyDown();
        if (crouchExtract) {
            ItemStack extracted = be.extractItemStack();
            if (!extracted.isEmpty()) {
                player.setItemInHand(hand, extracted);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (!player.getItemInHand(hand).isEmpty()
                && stack.getCapability(Capabilities.FluidHandler.ITEM) == null) {
            int before = stack.getCount();
            ItemStack leftover = be.insertItemStack(stack);
            if (leftover.getCount() != before) {
                player.setItemInHand(hand, leftover);
                return ItemInteractionResult.SUCCESS;
            }
            // Nothing was actually inserted (ingredient slot already occupied) -- don't eat the
            // click, fall through so vanilla opens the GUI instead.
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Opens the GUI directly on any empty-hand click that useItemOn didn't claim (i.e. not
    // crouching, or crouching at a face with nothing to pull) -- no Outerface required. Mirrors
    // MasticatorBlock's interaction shape.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider menuProvider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof DroolingMachineBlockEntity<?> be) {
                    be.drops();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
