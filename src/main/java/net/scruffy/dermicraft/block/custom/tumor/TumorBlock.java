package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.interfaces.IHarvestableBlock;
import net.scruffy.dermicraft.util.ModItemUtil;

import java.util.List;

public class TumorBlock extends Block implements IHarvestableBlock {

    public TumorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (isHarvester(stack)) {
            harvest(level, player, stack, pos);
            changeState(level, pos, state);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void changeState(Level level, BlockPos pos, BlockState state) {
        changeToMarredTumor(level, pos, state);
    }

    @Override
    public List<ItemStack> harvest(Level level, Player player, ItemStack stack, BlockPos pos) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return List.of();

        BlockState state = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, level.getBlockEntity(pos), player, stack);
        ModItemUtil.giveItems(player, drops);
        return drops;
    }
}
