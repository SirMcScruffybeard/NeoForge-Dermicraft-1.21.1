package net.scruffy.dermicraft.block.custom.duct;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.block.custom.ModBaseEntityBlock;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;

/**
 * Shared base for the Node — the multi-way "pump"/router of the Innards Duct system (the only
 * place branching/distribution happens; ducts themselves are dumb two-ended conduits).
 *
 * <p>Non-directional block (fixed orientation, no {@code FACING}, like Skin Tank) whose top face
 * carries a north-pointing arrow so the orientation is readable. Empty-hand right-click opens the
 * routing/buffer GUI. On break the block entity drops its buffered items but never spills its fluid
 * (see design notes). Routing/flow logic itself is not built yet. Kept abstract so upgrade tiers slot in.
 */
public abstract class AbstractNodeBlock extends ModBaseEntityBlock {

    protected AbstractNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof NodeBlockEntity nodeBlockEntity) {
                nodeBlockEntity.drops();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
