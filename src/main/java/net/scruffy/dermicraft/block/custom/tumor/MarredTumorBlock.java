package net.scruffy.dermicraft.block.custom.tumor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MarredTumorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.StitchedTumorBlockEntity;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.interfaces.ISutableBlock;
import net.scruffy.dermicraft.util.ModItemUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MarredTumorBlock extends EarlySurgeryTumorBlock implements ISutableBlock {

    public static final MapCodec<MarredTumorBlock> CODEC = simpleCodec(MarredTumorBlock::new);

    public MarredTumorBlock(Properties properties) {
        super(properties
                .strength(.02f)
                .explosionResistance(5f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.8f)
                .ignitedByLava());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MarredTumorBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {

            // CHECK: Is this a legitimate break, or are we just stitching it closed?
            boolean isStitching = newState.is(ModBlocks.STITCHED_TUMOR.get());

            // Only drop items if we are NOT stitching (e.g., breaking with a pickaxe)
            if (!isStitching) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MarredTumorBlockEntity tumorEntity) {
                    ItemStackHandler inv = tumorEntity.getInventory();
                    for (int i = 0; i < inv.getSlots(); i++) {
                        ItemStack stack = inv.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Block.popResource(level, pos, stack);
                            inv.setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }
                }
            }

            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MarredTumorBlockEntity tumorEntity)) {
            playDefaultSutureSound(level, player);
            return ItemInteractionResult.FAIL;
        }

        if (isSutureTool(stack)) {
            suture(level, pos, player, stack, tumorEntity);
            return ItemInteractionResult.SUCCESS;
        }

        if (isCollectionTool(stack)) {
            emptyTumorToWorld(level, pos, tumorEntity);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            retrieveLastItem(level, player, tumorEntity);
        }

        if (!stack.isEmpty()) {
            insertItem(tumorEntity, player, stack);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void suture(Level level, BlockPos pos, Player player, ItemStack sutureStack, MarredTumorBlockEntity tumorEntity) {
        if (level.isClientSide) return;

        tumorEntity.updateRecipeCache();
        var recipe = tumorEntity.getCachedRecipe();
/*
        if (recipe == null) {
            System.out.println("Suture Failed: Server sees " + tumorEntity.getInventory().getSlots() + " slots checked.");
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }

 */

        if (recipe == null || !recipe.testSuture(sutureStack)) {
            level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1.0F, 0.5F);
            return;
        }

        // Take a safe deep snapshot copy of the items inside before we destroy the block entity data
        ItemStackHandler oldInventory = tumorEntity.getInventory();
        List<ItemStack> inventorySnapshot = new ArrayList<>();
        for (int i = 0; i < oldInventory.getSlots(); i++) {
            if (!oldInventory.getStackInSlot(i).isEmpty()) {
                inventorySnapshot.add(oldInventory.getStackInSlot(i).copy());
            }
        }

        changeState(level, pos, ModBlocks.STITCHED_TUMOR.get());

        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe instanceof StitchedTumorBlockEntity stitchedEntity) {
            ItemStackHandler newInv = stitchedEntity.getInventory();
            for (int i = 0; i < inventorySnapshot.size(); i++) {
                newInv.setStackInSlot(i, inventorySnapshot.get(i));
            }

            stitchedEntity.setEvolvingRecipe(recipe);
        }


    }

    private void insertItem(MarredTumorBlockEntity blockEntity, Player player, ItemStack stack) {
        boolean absorbed = blockEntity.insertItem(stack);
        if (absorbed) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
    }


    private void retrieveLastItem(Level level, Player player, MarredTumorBlockEntity blockEntity) {
        ItemStackHandler inv = blockEntity.getInventory();
        int targetSlot = -1;
        for (int i = inv.getSlots() - 1; i >= 0; i--) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                targetSlot = i;
                break;
            }
        }

        if (targetSlot != -1) {
            ItemStack extracted = inv.extractItem(targetSlot, 1, false);
            if (!extracted.isEmpty()) {
                ModItemUtil.giveItem(player, extracted);
                level.playSound(null, blockEntity.getBlockPos(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 1.0F, 0.7F);
            }
        }
    }


    protected void emptyTumorToWorld(Level level, BlockPos pos, MarredTumorBlockEntity blockEntity) {
        if (blockEntity instanceof MarredTumorBlockEntity be) {
            be.drops();
        }
    }

    @Override
    public void changeState(Level level, BlockPos pos, Block targetBlock) {
        setState(level, pos, targetBlock.defaultBlockState());
    }


}
