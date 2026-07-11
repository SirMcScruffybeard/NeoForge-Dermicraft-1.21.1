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
 * Tier 1 Node - the "pump" that routes fluid/items between duct runs. Shared behaviour lives in
 * {@link AbstractNodeBlock}; this concrete class supplies the codec and block entity so upgrade
 * tiers can be added as sibling subclasses of the shared base.
 */
public class NodeBlock extends AbstractNodeBlock {

    public static final MapCodec<NodeBlock> CODEC = simpleCodec(NodeBlock::new);

    public NodeBlock(Properties properties) {
        super(properties);
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
