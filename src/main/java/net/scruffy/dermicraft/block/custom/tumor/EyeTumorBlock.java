package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.interfaces.IHarvestableBlock;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.util.ModItemUtil;

import java.util.ArrayList;
import java.util.List;

public class EyeTumorBlock extends TumorBlock implements IHarvestableBlock {

    public EyeTumorBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(.05f)
                .explosionResistance(15f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.6f)
                .ignitedByLava()
        );
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

            drops.add(getSingleTypeHarvest(level, ModItems.EYE.get()));

            ModItemUtil.giveItems(player, drops);
        }
        return drops;
    }
}

