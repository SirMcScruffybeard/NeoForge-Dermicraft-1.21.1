package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchTopBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Workbench's top half -- purely visual/animated this pass (see WorkbenchTopBlockEntity's javadoc),
 * no GUI/menu of its own -- right-clicking it opens the *bottom's* menu (its own useWithoutItem
 * just delegates down to the paired bottom). Not meant to be placed by hand (see
 * ModCreativeModeTabs -- only the bottom is offered there); WorkbenchBlock#setPlacedBy is what
 * actually places this. Mirrors the bottom's own paired-removal logic so destroying either half
 * destroys both -- see WorkbenchBlock's class javadoc for the full pairing rule.
 */
public class WorkbenchTopBlock extends ModBaseEntityBlock {

    public static final MapCodec<WorkbenchTopBlock> CODEC = simpleCodec(WorkbenchTopBlock::new);

    public WorkbenchTopBlock(Properties properties) {
        super(properties.noLootTable());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // GeckoLib blocks have no baked mesh of their own -- ENTITYBLOCK_ANIMATED tells vanilla the
    // block entity's own renderer draws everything, every frame, rather than using a static model.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WorkbenchTopBlockEntity(blockPos, blockState);
    }

    // Lets light pass through fully -- see WorkbenchBlock's identical override for why noOcclusion()
    // alone isn't enough.
    @Override
    protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            // Take the paired bottom with it -- guarded the same way WorkbenchBlock's own onRemove
            // is, against re-entrant clearing from that same call.
            BlockPos belowPos = pos.below();
            if (level.getBlockState(belowPos).is(ModBlocks.WORKBENCH.get())) {
                level.setBlock(belowPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos belowPos = pos.below();
        if (!level.isClientSide && level.getBlockEntity(belowPos) instanceof MenuProvider menuProvider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(belowPos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
