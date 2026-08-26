package net.scruffy.dermicraft.block.custom.duct;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.block.custom.ModBaseEntityBlock;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Shared base for the Node — the multi-way "pump"/router of the Innards Duct system (the only
 * place branching/distribution happens; ducts themselves are dumb two-ended conduits).
 *
 * <p>Non-directional block (fixed orientation, no {@code FACING}, like Skin Tank) whose top face
 * carries a north-pointing arrow so the orientation is readable. Empty-hand right-click opens the
 * routing/buffer GUI. On break the block entity drops its buffered items but never spills its fluid
 * (see design notes). Kept abstract so upgrade tiers slot in as sibling subclasses.
 *
 * <p>Carries a {@link NodeTier} (capacity / throughput / hazard tolerance) that the block entity
 * reads off its block state, so a stat-only tier is just a new subclass with a different NodeTier.
 */
public abstract class AbstractNodeBlock extends ModBaseEntityBlock implements TieredNode {

    private final NodeTier tier;

    protected AbstractNodeBlock(Properties properties, NodeTier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public NodeTier getTier() {
        return tier;
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

    /**
     * Tier swap: right-clicking a placed Node with a DIFFERENT-tier Node item in hand swaps the
     * block in place, carrying over the live block entity's full state (see
     * {@code NodeBlockEntity#exportStateForSwap}/{@code #importState}) -- no cost beyond the swap
     * itself, the player gets the old tier's block back. Both directions work identically (upgrade
     * or downgrade), since this is a plain state-preserving block swap either way.
     */
    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof AbstractNodeBlock newBlock)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Block oldBlock = state.getBlock();
        if (newBlock == oldBlock) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof NodeBlockEntity oldBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        CompoundTag snapshot = oldBE.exportStateForSwap(level.registryAccess());
        level.setBlock(pos, newBlock.defaultBlockState(), Block.UPDATE_ALL);

        if (level.getBlockEntity(pos) instanceof NodeBlockEntity newBE) {
            newBE.importState(snapshot, level.registryAccess());
            newBE.setChanged();
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ItemStack oldItem = new ItemStack(oldBlock.asItem());
        if (!player.getInventory().add(oldItem)) {
            player.drop(oldItem, false);
        }

        return ItemInteractionResult.SUCCESS;
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
