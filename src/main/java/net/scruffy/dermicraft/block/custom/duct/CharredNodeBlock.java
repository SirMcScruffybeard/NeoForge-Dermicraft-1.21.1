package net.scruffy.dermicraft.block.custom.duct;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Tier 2 Node -- same {@link NodeBlockEntity}, higher throughput and thermal-hazard tolerance (see
 * {@link NodeTier#TIER_2}). The block entity reads its tier off the block via {@link TieredNode}, so
 * this is registered as an additional valid block on the shared {@code INNARDS_NODE_BE} type rather
 * than needing its own block-entity class/capabilities -- see {@link NodeTier}'s own javadoc.
 */
public class CharredNodeBlock extends AbstractNodeBlock {

    public static final MapCodec<CharredNodeBlock> CODEC = simpleCodec(CharredNodeBlock::new);

    public CharredNodeBlock(Properties properties) {
        super(properties, NodeTier.TIER_2);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new NodeBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;

        return createTickerHelper(blockEntityType, ModBlockEntities.INNARDS_NODE_BE.get(),
                (lvl, blockPos, blockState, nodeBlockEntity) -> nodeBlockEntity.tick(lvl));
    }
}
