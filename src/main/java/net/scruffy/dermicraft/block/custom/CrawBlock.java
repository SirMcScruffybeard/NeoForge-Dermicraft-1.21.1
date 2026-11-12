package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.CrawBlockEntity;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.interfaces.IInject;
import net.scruffy.dermicraft.interfaces.IInjectableBlock;
import net.scruffy.dermicraft.machine.StatConfig;
import net.scruffy.dermicraft.recipe.early_incubating.EarlyIncubatingRecipe;
import net.scruffy.dermicraft.util.ToolUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrawBlock extends ModBaseEntityBlock implements IInjectableBlock {

    public static final MapCodec<CrawBlock> CODEC = simpleCodec(CrawBlock::new);

    public CrawBlock(Properties properties) {
        super(properties
                .noLootTable()
                .noOcclusion()
                .ignitedByLava()
        );
        this.registerDefaultState(EvolutionBlockProperties.withDefaults(this.stateDefinition.any()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        EvolutionBlockProperties.add(builder);
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

    /**
     * A standing click with an item. Module items open the Module tab; injection tools trigger a matching Early
     * Incubating recipe (see {@link EarlyIncubatingRecipe}) instead of being deposited; anything else is deposited, one
     * item per click or the held stack on a double click, or, for an item with its own item handler (the Eater), its
     * contents are deposited. An empty hand, or a click that deposits nothing (a wrong item), falls through to
     * {@link #useWithoutItem}, which opens the GUI. Crouching clicks never get here: see {@code CrawClickEvents}.
     */
    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (tryOpenModuleTab(stack, level, pos, player)) return ItemInteractionResult.SUCCESS;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof CrawBlockEntity craw)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Empty hand -> fall through to useWithoutItem for the GUI, unless this is the second click of a double click
        // whose first click deposited the last item in hand (see consumeEmptyHandDoubleClick).
        if (stack.isEmpty()) {
            return craw.consumeEmptyHandDoubleClick(player)
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Injection tools are checked first -- an Early Incubating recipe triggers on a matching fluid injection rather
        // than being deposited into storage like every other held item.
        if (ToolUtil.isInjectionTool(stack)) {
            inject(level, player, stack, craw);
            return ItemInteractionResult.SUCCESS;
        }

        if (craw.handleStandingClick(player, stack)) return ItemInteractionResult.SUCCESS;

        // Nothing was actually deposited (wrong item type, or Craw locked to a different item) -- don't eat the click,
        // fall through so vanilla opens the GUI instead.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    /**
     * Mirrors {@code MarredTumorBlock.inject} -- any {@link IInject} tool (e.g. a loaded
     * Syringe, or A.I.D. in Syringe mode via {@link IInjectableBlock}) can trigger a matching
     * cached recipe. Unlike the Tumor version, Craw is never consumed or transformed; see
     * {@link CrawBlockEntity#completeIncubation}.
     */
    @Override
    public boolean inject(Level level, Player player, ItemStack stack, BlockEntity blockEntityRaw) {
        if (!(blockEntityRaw instanceof CrawBlockEntity craw)) return false;
        if (!(stack.getItem() instanceof IInject syringe)) return false;

        FluidData data = stack.getOrDefault(syringe.getFluidDataType(), FluidData.EMPTY);
        if (data.isFluidEmpty()) return false;

        FluidStack fluidStack = data.getFluidStack();
        EarlyIncubatingRecipe recipe = craw.getCachedRecipe();

        if (recipe == null || !recipe.testFluid(fluidStack)) return false;

        syringe.emptyDataFluidIfSurvival(stack, player);
        craw.completeIncubation(recipe);
        return true;
    }

    // Opens the GUI on a standing click that useItemOn didn't claim (an empty hand, or an item that deposited nothing).
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrawBlockEntity craw
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(craw, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    /**
     * A left click withdraws items, so breaking a Craw takes a held click: survival mining progresses at a fixed rate
     * that finishes in {@code StatConfig#getCrawBreakHoldTicks}, whatever the tool. Creative skips this (vanilla breaks
     * instantly there) and has its own hold, see {@code CrawBlockEntity#onCreativeLeftClick}.
     */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 1.0F / StatConfig.getCrawBreakHoldTicks();
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
