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
import net.scruffy.dermicraft.block.entity.custom.CharredRenderKilnBlockEntity;
import net.scruffy.dermicraft.machine.MachineTier;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Render Kiln's block half -- see {@link CharredRenderKilnBlockEntity} for the actual
 * capability leap (hazard-tolerant input tank). Otherwise a thin re-skin of {@link RenderKilnBlock}:
 * same {@code FACING}/{@code STATE} properties (inherited), same interaction/rotation behavior, just
 * its own {@code CODEC}, block entity type, and ticker registration so the two blocks stay fully
 * independent registry entries.
 */
public class CharredRenderKilnBlock extends RenderKilnBlock {

    public static final MapCodec<CharredRenderKilnBlock> CODEC = simpleCodec(CharredRenderKilnBlock::new);

    public CharredRenderKilnBlock(Properties properties) {
        super(properties, MachineTier.CHARRED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CharredRenderKilnBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CHARRED_RENDER_KILN_BE.get(),
                (lvl, blockPos, blockState, be) -> be.tick(lvl));
    }
}
