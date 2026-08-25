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
import net.scruffy.dermicraft.block.entity.custom.CharredMetastasizerBlockEntity;
import net.scruffy.dermicraft.machine.MachineTier;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Metastasizer's block half -- see {@link CharredMetastasizerBlockEntity} for the actual
 * capability leap (hazard-tolerant reagent tank). Thin re-skin of {@link MetastasizerBlock}, same
 * pattern as {@code CharredMasticatorBlock}: same FACING/STATE properties (inherited), same
 * interaction/rotation behavior, just its own CODEC, block entity type, and ticker registration.
 */
public class CharredMetastasizerBlock extends MetastasizerBlock {

    public static final MapCodec<CharredMetastasizerBlock> CODEC = simpleCodec(CharredMetastasizerBlock::new);

    public CharredMetastasizerBlock(Properties properties) {
        super(properties, MachineTier.CHARRED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CharredMetastasizerBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CHARRED_METASTASIZER_BE.get(),
                (lvl, blockPos, blockState, be) -> be.tick(lvl));
    }
}
