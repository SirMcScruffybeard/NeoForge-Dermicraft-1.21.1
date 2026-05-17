package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.scruffy.dermicraft.interfaces.IHarvestableBlock;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.util.ModItemUtil;

import java.util.ArrayList;
import java.util.List;

public class InertTumorBlock extends TumorBlock implements IHarvestableBlock {
    public InertTumorBlock() {
        super(BlockBehaviour.Properties.of().ignitedByLava()
                .strength(.05f)
                .explosionResistance(15f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.6f)
                .ignitedByLava()
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (isHarvester(stack)) {
           harvest(level, player, stack,pos);

           changeState(level, pos, state);

           return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void changeState(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, ModBlocks.MARRED_TUMOR.get().defaultBlockState(), 3);

        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.levelEvent(2001, pos, Block.getId(state)); // Spawns the breaking particles of the Inert Tumor
    }

    @Override
    public List<ItemStack> harvest(Level level, Player player, ItemStack stack,  BlockPos pos) {

        List<ItemStack> drops = new ArrayList<>();
        RandomSource random = level.getRandom();

        int count = random.nextInt(2, 5);

        for (int i = 0; i < count; i++) {
            int chance = random.nextInt(3);

            ItemStack drop = switch (chance) {
                case 0 -> new ItemStack(ModItems.DENSE_MUSCLE.get());
                case 1 -> new ItemStack(ModItems.NERVE_CLUSTER.get());
                default -> new ItemStack(ModItems.EYE.get());
            };
            drops.add(drop);
        }

        ModItemUtil.giveItems(player, drops);

        return drops;
    }
}
