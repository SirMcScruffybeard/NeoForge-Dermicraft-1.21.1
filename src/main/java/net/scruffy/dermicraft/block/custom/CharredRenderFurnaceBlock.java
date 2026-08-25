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
import net.scruffy.dermicraft.block.entity.custom.CharredRenderFurnaceBlockEntity;
import net.scruffy.dermicraft.machine.MachineTier;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Render Furnace's block half -- see {@link CharredRenderFurnaceBlockEntity} for the
 * actual (purely stat-based) capability leap. Thin re-skin of {@link RenderFurnaceBlock}, same
 * pattern as {@code CharredMetastasizerBlock}: same {@code FACING}/{@code ACTIVE} properties
 * (inherited), same interaction/rotation behavior, just its own {@code CODEC}, block entity type,
 * and ticker registration.
 */
public class CharredRenderFurnaceBlock extends RenderFurnaceBlock {

    public static final MapCodec<CharredRenderFurnaceBlock> CODEC = simpleCodec(CharredRenderFurnaceBlock::new);

    public CharredRenderFurnaceBlock(Properties properties) {
        super(properties, MachineTier.CHARRED_NO_HEALTH);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CharredRenderFurnaceBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CHARRED_RENDER_FURNACE_BE.get(),
                (lvl, blockPos, blockState, be) -> be.tick(lvl));
    }
}
