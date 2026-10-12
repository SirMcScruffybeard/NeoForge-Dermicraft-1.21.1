package net.scruffy.dermicraft.block.custom.tumor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.ModBaseEntityBlock;
import net.scruffy.dermicraft.block.entity.custom.MarredTumorBlockEntity;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.interfaces.*;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.util.ModItemUtil;
import net.scruffy.dermicraft.util.ToolUtil;
import org.jetbrains.annotations.Nullable;

public class MarredTumorBlock extends ModBaseEntityBlock implements ISutableBlock, ICutStitches, IInjectableBlock {

    public static final MapCodec<MarredTumorBlock> CODEC = simpleCodec(MarredTumorBlock::new);

    public static final BooleanProperty STITCHED = BooleanProperty.create("stitched");

    public MarredTumorBlock(Properties properties) {
        super(properties
                .strength(.02f)
                .explosionResistance(5f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.8f)
                .ignitedByLava()
                .noLootTable());

        registerDefaultState(getStateDefinition().any().setValue(STITCHED, false));

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STITCHED);
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
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return state.getValue(STITCHED)
                ? new ItemStack(ModBlocks.STITCHED_TUMOR.get())
                : super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {

            // Stitching and cutting stitches are property changes on this same block, so they never
            // reach here. A stitched tumor turning into a machine has consumed its contents; any
            // other removal (a break, Forceps pickup) spills them.
            boolean evolving = state.getValue(STITCHED) && !newState.isAir();

            if (!evolving) {
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

        if(state.getValue(STITCHED)) {
            if(ToolUtil.isExtractionTool(stack)) {
                cutStitches(level, pos,player,stack, tumorEntity);
                return ItemInteractionResult.SUCCESS;
            }

            if (ToolUtil.isInjectionTool(stack)) {
                inject(level, player, stack, tumorEntity);
                return ItemInteractionResult.SUCCESS;
            }

            return ItemInteractionResult.FAIL;
        }

        if (ToolUtil.isSutureTool(stack)) {
            suture(level, pos, player, stack, tumorEntity, hand);
            return ItemInteractionResult.SUCCESS;
        }

        if (ToolUtil.isCollectionTool(stack)) {
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
    public void suture(Level level, BlockPos pos, Player player, ItemStack sutureStack, MarredTumorBlockEntity tumorEntity, InteractionHand hand) {
        if (level.isClientSide) return;

        if (level.getBlockState(pos).getValue(STITCHED)) return;

        tumorEntity.updateRecipeCache();

        boolean decorative = tumorEntity.isEmpty();
        var recipeHolder = tumorEntity.getCachedRecipeHolder();

        if (!decorative && (recipeHolder == null || !recipeHolder.value().testSuture(sutureStack))) {
            level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1.0F, 0.5F);
            return;
        }

        if (sutureStack.getItem() instanceof ISuture suture) {
            suture.useMaterials(player, hand);
        }

       level.setBlock(pos, level.getBlockState(pos).setValue(STITCHED, true), 3);
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
        blockEntity.drops();
    }

    @Override
    public void cutStitches(Level level, BlockPos pos, Player player, ItemStack scalpelStack, MarredTumorBlockEntity tumorEntity) {
        BlockState state = level.getBlockState(pos);

        if (!state.getValue(STITCHED)) return;

        if (!player.isCreative()) {
            scalpelStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }

        level.setBlock(pos, state.setValue(STITCHED, false), 3);
        tumorEntity.updateRecipeCache();

        playStitchCutSound(level, pos);
    }

    @Override
    public boolean inject(Level level, Player player, ItemStack stack, BlockEntity tumorEntity) {
        if(!level.getBlockState(tumorEntity.getBlockPos()).getValue(STITCHED)) return false;
        if (!(tumorEntity instanceof  MarredTumorBlockEntity te)) return false;
        if (!(stack.getItem() instanceof IInject syringe)) return false;

        FluidData data = stack.getOrDefault(syringe.getFluidDataType(), FluidData.EMPTY);
        if (data.isFluidEmpty()) return false;

        FluidStack fluidStack = data.getFluidStack();
        EarlyImplantRecipe recipe = te.getCachedRecipe();

        if (recipe == null || !recipe.testFluid(fluidStack)) return false;

        syringe.emptyDataFluidIfSurvival(stack, player);
        evolveImplant(level, player, te.getBlockPos(), recipe);
        return true;
    }

    private void evolveImplant(Level level, Player player, BlockPos pos, EarlyImplantRecipe recipe) {
        ItemStack resultStack = recipe.getResultItem(level.registryAccess());
        Block finalBlock = Block.byItem(resultStack.getItem());

        playEvolveSound(level, pos);

        BlockState resultState = finalBlock.defaultBlockState();
        if (resultState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            resultState = resultState.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    player.getDirection().getOpposite());
        }

        level.setBlock(pos, resultState, 3);

        BlockState placedState = level.getBlockState(pos);
        placedState.getBlock().setPlacedBy(level, pos, placedState, player, ItemStack.EMPTY);
    }

    @Override
    public boolean canInject(BlockState state) {
        return state.getValue(STITCHED);
    }
}
