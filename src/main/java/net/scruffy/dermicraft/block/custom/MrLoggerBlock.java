package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.MrLoggerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import org.jetbrains.annotations.Nullable;

/** Mr. Logger's block. Facing, ACTIVE, ticking, breaking and the interactions are all in {@link AbstractFarmBlock}. */
public class MrLoggerBlock extends AbstractFarmBlock {

    public static final MapCodec<MrLoggerBlock> CODEC = simpleCodec(MrLoggerBlock::new);

    public MrLoggerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends AbstractFarmBlockEntity> getFarmBlockEntityType() {
        return ModBlockEntities.MR_LOGGER_BE.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MrLoggerBlockEntity(blockPos, blockState);
    }
}
