package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.interfaces.ISutableBlock;

public class MarredTumorBlock extends TumorBlock implements ISutableBlock {

    public MarredTumorBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(.02f)
                .explosionResistance(5f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.8f)
                .ignitedByLava());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (isSutureTool(stack)) {
            suture(level, player, pos);
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void suture(Level level, Player player, BlockPos pos) {
        changeState(level, pos, ModBlocks.STITCHED_TUMOR.get());
        playSutureSound(level, player);
    }

    @Override
    public void changeState(Level level, BlockPos pos, Block targetBlock) {
        setState(level, pos, targetBlock.defaultBlockState());
    }
}
