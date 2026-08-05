package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.CrawBlockEntity;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.interfaces.IInject;
import net.scruffy.dermicraft.recipe.early_incubating.EarlyIncubatingRecipe;
import net.scruffy.dermicraft.util.ToolUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrawBlock extends ModBaseEntityBlock {

    public static final MapCodec<CrawBlock> CODEC = simpleCodec(CrawBlock::new);

    public CrawBlock(Properties properties) {
        super(properties
                .noLootTable()
                .noOcclusion()
                .ignitedByLava()
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CrawBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof CrawBlockEntity crawBlockEntity) {
                    crawBlockEntity.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {

        // Empty hand -> fall through to useWithoutItem for withdrawal.
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof CrawBlockEntity craw) {
                // Injection tools are checked first -- an Early Incubating recipe (see
                // EarlyIncubatingRecipe) triggers on a matching fluid injection rather than
                // being deposited into storage like every other held item.
                if (ToolUtil.isInjectionTool(stack)) {
                    inject(level, player, stack, craw);
                    return ItemInteractionResult.SUCCESS;
                }

                // Deposits the whole held stack -- see CrawBlockEntity.deposit for why this isn't
                // crouch-gated (vanilla skips useItemOn entirely when crouching with a held item).
                ItemStack leftover = craw.deposit(stack);
                player.setItemInHand(hand, leftover);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Mirrors {@code StitchedTumorBlock.inject} -- any {@link IInject} tool (e.g. a loaded
     * Syringe) can trigger a matching cached recipe. Unlike the Tumor version, Craw is never
     * consumed or transformed; see {@link CrawBlockEntity#completeIncubation}.
     */
    private void inject(Level level, Player player, ItemStack stack, CrawBlockEntity craw) {
        if (stack.getItem() instanceof IInject syringe) {
            FluidData data = stack.getOrDefault(syringe.getFluidDataType(), FluidData.EMPTY);
            if (data.isFluidEmpty()) {
                return;
            }

            FluidStack fluidStack = data.getFluidStack();
            EarlyIncubatingRecipe recipe = craw.getCachedRecipe();

            if (recipe != null && recipe.testFluid(fluidStack)) {
                syringe.emptyDataFluidIfSurvival(stack, player);
                craw.completeIncubation(recipe);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof CrawBlockEntity craw) {
                // Regular click withdraws one item; crouch withdraws a full stack.
                ItemStack withdrawn = craw.withdraw(player.isShiftKeyDown());
                if (!withdrawn.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(withdrawn);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide()) {
            return null;
        }

        return createTickerHelper(pBlockEntityType, ModBlockEntities.CRAW_BE.get(),
                ((level, blockPos, blockState, crawBlockEntity) -> crawBlockEntity.tick(level)));
    }
}
