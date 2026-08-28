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
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof CrawBlockEntity craw)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Double-click vacuum -- checked here, unconditionally, BEFORE the empty-hand branch below,
        // because useItemOn always runs first for every click (including an empty hand -- see that
        // branch's own comment). Registering/checking this only once per physical click is why
        // useWithoutItem does NOT also call isDoubleClick: this method's own PASS_TO_DEFAULT_BLOCK_
        // INTERACTION fallthroughs (empty hand, or a held item that doesn't deposit) reach
        // useWithoutItem in the SAME click, so a second check there would register twice per click
        // and false-trigger the vacuum on the very first real click.
        if (craw.isDoubleClick(player)) {
            craw.vacuumFromInventory(player);
            return ItemInteractionResult.SUCCESS;
        }

        // Empty hand -> fall through to useWithoutItem for withdrawal/GUI.
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Injection tools are checked first -- an Early Incubating recipe (see
        // EarlyIncubatingRecipe) triggers on a matching fluid injection rather than being
        // deposited into storage like every other held item.
        if (ToolUtil.isInjectionTool(stack)) {
            inject(level, player, stack, craw);
            return ItemInteractionResult.SUCCESS;
        }

        // Deposits the whole held stack -- see CrawBlockEntity.deposit for why this isn't
        // crouch-gated (vanilla skips useItemOn entirely when crouching with a held item).
        int before = stack.getCount();
        ItemStack leftover = craw.deposit(stack);
        if (leftover.getCount() != before) {
            player.setItemInHand(hand, leftover);
            return ItemInteractionResult.SUCCESS;
        }

        // Nothing was actually deposited (wrong item type, or Craw locked to a different item) --
        // don't eat the click, fall through so vanilla opens the GUI instead.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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

    // Crouch withdraws a full stack, unchanged. A plain (non-crouch) click used to withdraw a
    // single item; it now opens the GUI instead -- matches every other machine's own "empty hand
    // (or a held item useItemOn didn't claim) opens the GUI" convention. Also reached whenever
    // useItemOn falls through with a non-empty invalid item (wrong type, or Craw locked to a
    // different item), same as Masticator/Effluentcer/Mutator's identical fall-through shape.
    // Double-click vacuum is NOT checked here -- useItemOn already checked it once for this same
    // click before falling through (see that method's own comment); checking it again here would
    // register the same physical click twice and false-trigger on the very first click.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrawBlockEntity craw) {
            if (player.isShiftKeyDown()) {
                // No GUI involved, so no double-click-reachability problem -- withdraws immediately,
                // same as always.
                ItemStack withdrawn = craw.withdraw(true);
                if (!withdrawn.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(withdrawn);
                }
            } else {
                // Delayed, not immediate -- see CrawBlockEntity#schedulePendingMenuOpen's own
                // javadoc for why opening right here would make the empty-hand double-click
                // unreachable (an open container screen swallows the second click).
                craw.schedulePendingMenuOpen(player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
