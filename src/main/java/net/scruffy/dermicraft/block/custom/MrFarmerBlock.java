package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.MrFarmerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import org.jetbrains.annotations.Nullable;

/** Mr. Farmer's block. Facing, ACTIVE (the mr_farmer -> mr_farmer_on texture swap), ticking, breaking and the
 * interactions are all in {@link AbstractFarmBlock}. */
public class MrFarmerBlock extends AbstractFarmBlock {

    public static final MapCodec<MrFarmerBlock> CODEC = simpleCodec(MrFarmerBlock::new);

    public MrFarmerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends AbstractFarmBlockEntity> getFarmBlockEntityType() {
        return ModBlockEntities.MR_FARMER_BE.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MrFarmerBlockEntity(blockPos, blockState);
    }
}
