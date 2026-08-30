package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.KnowledgeVatBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Knowledge Vat's block half -- see {@link KnowledgeVatBlockEntity} for the actual deposit/
 * withdraw/tank logic. {@code noLootTable()} matches every other machine's own "destroyed on
 * normal break, Forceps-recoverable" rule (see the COLLECTIBLE tag entry).
 */
public class KnowledgeVatBlock extends ModBaseEntityBlock {

    public static final MapCodec<KnowledgeVatBlock> CODEC = simpleCodec(KnowledgeVatBlock::new);

    /** Drives actual world light emission, kept in sync with the tank's contents -- XP is "special",
     * so a Vat actually holding some glows, same pattern DroolingMachineBlock/BeakerBlock already
     * use for their own fluid-holding blocks. See {@code KnowledgeVatBlockEntity#onTankContentsChanged}
     * for where this actually gets set. */
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

    public KnowledgeVatBlock(Properties properties) {
        super(properties
                .noLootTable()
                .noOcclusion()
                .lightLevel(state -> state.getValue(LIGHT_LEVEL))
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(LIGHT_LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT_LEVEL);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new KnowledgeVatBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof KnowledgeVatBlockEntity vat) {
                    vat.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    // Standard fluid-container interaction (bucket, Syringe, etc.) takes priority; an empty hand
    // then deposits (or, crouching, withdraws) a level. Deliberately handled here rather than in
    // useWithoutItem -- mirrors MasticatorBlock/MutatorBlock's own "crouch + empty hand" shape,
    // since a crouching player's empty-hand click doesn't reliably reach useWithoutItem at all.
    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {

        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof KnowledgeVatBlockEntity vat)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (FluidUtil.interactWithFluidHandler(player, hand, vat.getTank(null))) {
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            boolean acted = player.isShiftKeyDown()
                    ? vat.withdrawLevel(serverPlayer)
                    : vat.depositLevel(serverPlayer);
            if (acted) return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Nothing left to do on a fallen-through click -- no GUI to open, unlike every other machine's
    // own useWithoutItem. Still needs the override so a fallen-through empty-hand click doesn't
    // fall to whatever vanilla's own default block interaction would otherwise do.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide()) {
            return null;
        }

        return createTickerHelper(pBlockEntityType, ModBlockEntities.KNOWLEDGE_VAT_BE.get(),
                ((level, blockPos, blockState, vat) -> vat.tick(level)));
    }
}
