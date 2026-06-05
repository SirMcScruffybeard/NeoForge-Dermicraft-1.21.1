package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MasticatorBlock extends ModBaseEntityBlock {

    public static final MapCodec<MasticatorBlock> CODEC = simpleCodec(MasticatorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MasticatorBlock(Properties properties) {
        super(properties
                .noLootTable()
                .ignitedByLava()
        );
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

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MasticatorBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof MasticatorBlockEntity masticatorBlockEntity) {
                    masticatorBlockEntity.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {

        Direction face = hitResult.getDirection();

        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof MasticatorBlockEntity masticator) {

                if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, face)){
                    masticator.setChanged();
                    masticator.updateBlock();
                    return ItemInteractionResult.SUCCESS;
                }

                if (player.getItemInHand(hand).isEmpty()) {
                    player.setItemInHand(hand ,masticator.extractIngredients());
                    return ItemInteractionResult.SUCCESS;

                }else if (stack.getCapability(Capabilities.FluidHandler.ITEM) == null) {
                    player.setItemInHand(hand, masticator.insetItemStack(stack));
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState state, BlockEntityType<T> blockEntityType) {
        if (pLevel.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.MASTICATOR_BE.get(),
                ((level, blockPos, blockState, DcBlockEntity) -> DcBlockEntity.tick(level)));
    }
}
