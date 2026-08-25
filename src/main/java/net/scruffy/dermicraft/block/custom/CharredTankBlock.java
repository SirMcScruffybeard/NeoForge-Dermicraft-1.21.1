package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.CharredTankBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Tank's block half -- see {@link CharredTankBlockEntity} for the actual capability leap
 * (double capacity, thermal-hazard-tolerant tank). Otherwise a thin re-skin of {@link SkinTankBlock}:
 * same translucent-window properties, same interaction behavior, just its own {@code CODEC}, block
 * entity type, and ticker registration so the two blocks stay fully independent registry entries.
 */
public class CharredTankBlock extends SkinTankBlock {

    public static final MapCodec<CharredTankBlock> CODEC = simpleCodec(CharredTankBlock::new);

    public CharredTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CharredTankBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CHARRED_TANK_BE.get(),
                (lvl, blockPos, blockState, be) -> be.tick(lvl));
    }
}
