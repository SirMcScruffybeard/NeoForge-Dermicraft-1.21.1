package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.interfaces.IHarvestableBlock;
import net.scruffy.dermicraft.util.ModItemUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class TumorBlock extends Block implements IHarvestableBlock {

    public TumorBlock(Properties properties) {
        super(properties
                .noLootTable());
    }

    protected Item getHarvestItem() {
        return null;
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
        List<ItemStack> drops = new ArrayList<>();

        if (!level.isClientSide) {
            drops.add(getSingleTypeHarvest(level, getHarvestItem()));
            ModItemUtil.giveItems(player, drops);
        }
        return drops;
    }
}
