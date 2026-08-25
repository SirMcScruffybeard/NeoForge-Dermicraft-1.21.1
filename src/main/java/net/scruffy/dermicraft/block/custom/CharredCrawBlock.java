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
import net.scruffy.dermicraft.block.entity.custom.CharredCrawBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Craw's block half -- see {@link CharredCrawBlockEntity} for the actual capability leaps
 * (double capacity, double auto-push throughput). Otherwise a thin re-skin of {@link CrawBlock}:
 * same interaction behavior, just its own {@code CODEC}, block entity type, and ticker
 * registration so the two blocks stay fully independent registry entries.
 */
public class CharredCrawBlock extends CrawBlock {

    public static final MapCodec<CharredCrawBlock> CODEC = simpleCodec(CharredCrawBlock::new);

    public CharredCrawBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CharredCrawBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CHARRED_CRAW_BE.get(),
                (lvl, blockPos, blockState, be) -> be.tick(lvl));
    }
}
