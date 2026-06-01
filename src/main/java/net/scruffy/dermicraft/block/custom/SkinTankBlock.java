package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkinTankBlock extends ModBaseEntityBlock {

    public static final MapCodec<SkinTankBlock> CODEC = simpleCodec(SkinTankBlock::new);

    public SkinTankBlock(Properties properties) {
        super(properties
                .noLootTable()
                .noOcclusion()
                .ignitedByLava()
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SkinTankBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof SkinTankBlockEntity skinTankBlockEntity) {
                    skinTankBlockEntity.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @NotNull
    @Override
    protected  ItemInteractionResult useItemOn (
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {

        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SkinTankBlockEntity tankBlockEntity) {
            FluidUtil.interactWithFluidHandler(player, hand, tankBlockEntity.getTank(null));
           // player.openMenu(new SimpleMenuProvider(tankBlockEntity, Component.literal("Tank")), pos);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if(pLevel.isClientSide()) {
            return null;
        }

        return createTickerHelper(pBlockEntityType, ModBlockEntities.SKIN_TANK_BE.get(),
                ((level, blockPos, blockState, skinTankBlockEntity) -> skinTankBlockEntity.tick(level)));
    }

}
