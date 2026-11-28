package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.MrHunterBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import org.jetbrains.annotations.Nullable;

/** Mr. Hunter's block. Facing, ACTIVE, ticking, breaking and the interactions are all in {@link AbstractFarmBlock}. */
public class MrHunterBlock extends AbstractFarmBlock {

    public static final MapCodec<MrHunterBlock> CODEC = simpleCodec(MrHunterBlock::new);

    public MrHunterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends AbstractFarmBlockEntity> getFarmBlockEntityType() {
        return ModBlockEntities.MR_HUNTER_BE.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MrHunterBlockEntity(blockPos, blockState);
    }
}
