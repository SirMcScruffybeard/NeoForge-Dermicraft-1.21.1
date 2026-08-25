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
import net.scruffy.dermicraft.block.entity.custom.CharredMutatorBlockEntity;
import net.scruffy.dermicraft.machine.MachineTier;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Mutator's block half -- see {@link CharredMutatorBlockEntity} for the actual capability
 * leap (hazard-tolerant tank). Otherwise a thin re-skin of {@link MutatorBlock}: same
 * {@code FACING}/{@code STATE} properties (inherited), same interaction/rotation behavior, just its
 * own {@code CODEC}, block entity type, and ticker registration so the two blocks stay fully
 * independent registry entries.
 */
public class CharredMutatorBlock extends MutatorBlock {

    public static final MapCodec<CharredMutatorBlock> CODEC = simpleCodec(CharredMutatorBlock::new);

    public CharredMutatorBlock(Properties properties) {
        super(properties, MachineTier.CHARRED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CharredMutatorBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CHARRED_MUTATOR_BE.get(),
                (lvl, blockPos, blockState, be) -> be.tick(lvl));
    }
}
