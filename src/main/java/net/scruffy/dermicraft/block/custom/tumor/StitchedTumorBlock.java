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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MarredTumorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.StitchedTumorBlockEntity;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.interfaces.IInject;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.util.ModItemUtil;
import net.scruffy.dermicraft.util.ToolUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StitchedTumorBlock extends EarlySurgeryTumorBlock {

    public static final MapCodec<StitchedTumorBlock> CODEC = simpleCodec(StitchedTumorBlock::new);

    public StitchedTumorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StitchedTumorBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {

            // CHECK: Are we doing a surgical reversion or an evolution?
            boolean isReverting = newState.is(ModBlocks.MARRED_TUMOR.get());
            boolean isEvolving = newState.getBlock() != Blocks.AIR && !isReverting;

            // Only drop items if it's a brute-force break (not reverting and not evolving)
            if (!isReverting && !isEvolving) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StitchedTumorBlockEntity stitchedEntity) {
                    ItemStackHandler inv = stitchedEntity.getInventory();
                    for (int i = 0; i < inv.getSlots(); i++) {
                        if (!inv.getStackInSlot(i).isEmpty()) {
                            Block.popResource(level, pos, inv.getStackInSlot(i));
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

        if (!(level.getBlockEntity(pos) instanceof StitchedTumorBlockEntity stitchedEntity)) {
            return ItemInteractionResult.FAIL;
        }

        if (ToolUtil.isExtractionTool(stack)) {
            cutStitches(level, pos, player, stack, stitchedEntity);
            return ItemInteractionResult.SUCCESS;
        }

        if (ToolUtil.isInjectionTool(stack)) {

            inject(level, player, stack, stitchedEntity);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.FAIL;
    }

    private void cutStitches(Level level, BlockPos pos, Player player, ItemStack scalpelStack, StitchedTumorBlockEntity stitchedEntity) {
        // Take an exact snapshot of the incubating contents
        List<ItemStack> savedItems = ModItemUtil.snapshotInventory(stitchedEntity.getInventory());

        // Damage the scalpel tool
        if (!player.isCreative()) {
            scalpelStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }

        // Revert world state back to an open Marred Tumor
        BlockState openState = ModBlocks.MARRED_TUMOR.get().defaultBlockState();
        level.setBlock(pos, openState, 3);

        // Feed the captured snapshot right back into the open block entity
        BlockEntity openBE = level.getBlockEntity(pos);
        if (openBE instanceof MarredTumorBlockEntity tumorEntity) {
            ModItemUtil.restoreInventory(tumorEntity.getInventory(), savedItems);
            tumorEntity.updateRecipeCache();
        }

        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    private void inject(Level level, Player player, ItemStack stack, StitchedTumorBlockEntity blockEntity) {
        if (stack.getItem() instanceof IInject syringe) {

            FluidData data = stack.getOrDefault(syringe.getFluidDataType(), FluidData.EMPTY);
            if (data.isFluidEmpty()) return;

            FluidStack fluidStack = data.getFluidStack();
            EarlyImplantRecipe recipe = blockEntity.getCachedRecipe();

            if (recipe != null && recipe.testFluid(fluidStack)) {
                syringe.emptyDataFluidIfSurvival(stack, player);

                this.evolveImplant(level, player, blockEntity.getBlockPos(), recipe);
            }
        }
    }

    /**
     * Consumes the stored biological layers, wipes the inventory metadata, and births
     * the new processing machine block directly in place.
     */
    private void evolveImplant(Level level, Player player, BlockPos pos, EarlyImplantRecipe recipe) {
        // Fetch the output item block associated with this recipe resultFluid
        ItemStack resultStack = recipe.getResultItem(level.registryAccess());
        Block finalMachineBlock = Block.byItem(resultStack.getItem());

        // Audiovisual breakthrough effect
        level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 1.2F, 0.5F);

        // defaultBlockState() alone always comes out facing NORTH regardless of which way the
        // player was actually standing during the injection -- orient it the same "face toward
        // whoever placed it" way every FACING machine's own getStateForPlacement already does
        // (MasticatorBlock, DroolingCauldronBlock, EffluentcerBlock, WorkbenchBlock, ...), rather
        // than leaving every implant-born machine stuck facing a fixed direction.
        BlockState resultState = finalMachineBlock.defaultBlockState();
        if (resultState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            resultState = resultState.setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection().getOpposite());
        }

        // Transform the block directly into the new machine (e.g., Drooling Cauldron)
        level.setBlock(pos, resultState, 3);

        // setBlock alone skips setPlacedBy -- fine for every ordinary single-block machine (none of
        // them override it), but a two-block Gear Station like the Workbench relies on setPlacedBy
        // to spawn its paired top half (see WorkbenchBlock#setPlacedBy). Calling it explicitly here,
        // generically, covers that case (and any future two-block station following the same
        // pairing convention) without special-casing the Workbench in this otherwise generic method.
        BlockState placedState = level.getBlockState(pos);
        placedState.getBlock().setPlacedBy(level, pos, placedState, player, ItemStack.EMPTY);
    }
}
