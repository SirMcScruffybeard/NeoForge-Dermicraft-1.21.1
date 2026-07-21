package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.GraftingTableBlockEntity;
import net.scruffy.dermicraft.machine.MachineTier;
import net.scruffy.dermicraft.machine.TieredMachine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Grafting Table: an automated, fuel-powered vanilla crafting table. Same "no HP, hard fuel
 * gate" philosophy as the Render Furnace (see {@link MachineTier#NO_HEALTH}) -- these two are a
 * deliberate matched pair. No FACING property: unlike every other machine, a 3x3 crafting grid has
 * no directional "front" to orient (vanilla's own crafting table doesn't rotate either), so
 * placement is unconditional and automation zoning is UP=fuel, DOWN=output, everything else=grid.
 */
public class GraftingTableBlock extends ModBaseEntityBlock implements TieredMachine {

    public static final MapCodec<GraftingTableBlock> CODEC = simpleCodec(GraftingTableBlock::new);

    private final MachineTier tier;

    public GraftingTableBlock(Properties properties) {
        this(properties, MachineTier.NO_HEALTH);
    }

    public GraftingTableBlock(Properties properties, MachineTier tier) {
        super(properties.noLootTable().ignitedByLava());
        this.tier = tier;
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new GraftingTableBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof GraftingTableBlockEntity graftingTableBlockEntity) {
                    graftingTableBlockEntity.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    // Right-click routing: top = fuel tank, bottom = output slot (extract only), every other face =
    // the 9-slot grid. Grid interaction (imprinting/depositing/extracting) only happens through the
    // GUI's click model now -- the ghost/pattern gesture (first click imprints for free, second
    // deposits) has no sensible one-off equivalent for a direct block right-click, so side faces
    // just fall through to the fuel/GUI paths below. Bottom (output) keeps direct crouch-extract,
    // same convention as the Mutator/Render Furnace.
    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof GraftingTableBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction face = hitResult.getDirection();
        boolean crouchExtract = player.getItemInHand(hand).isEmpty() && player.isShiftKeyDown();

        if (face == Direction.DOWN) {
            if (crouchExtract) {
                ItemStack extracted = be.extractResult();
                if (!extracted.isEmpty()) {
                    player.setItemInHand(hand, extracted);
                    return ItemInteractionResult.SUCCESS;
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, face)) {
            be.setChanged();
            be.updateBlock();
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Opens the GUI directly on any empty-hand click that useItemOn didn't claim -- no Outerface
    // required. The block still carries HAS_SCREEN, so the Outerface continues to work too.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider menuProvider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState state, BlockEntityType<T> blockEntityType) {
        if (pLevel.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.GRAFTING_TABLE_BE.get(),
                ((level, blockPos, blockState, be) -> be.tick(level)));
    }
}
