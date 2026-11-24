package net.scruffy.dermicraft.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import net.scruffy.dermicraft.screen.AbstractFarmMenu;
import net.scruffy.dermicraft.block.custom.EvolutionBlockProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The block shared by the farm machines (Mr. Farmer, Mr. Shepherd and the ones that follow): a facing, an ACTIVE
 * (lit) property that swaps the block to its "on" texture while the machine is fueled and running, the ticker, the
 * drop-on-break, and the two interactions (a fluid container fills or drains the fuel tank through the block's fluid
 * handler, anything else opens the GUI). A machine's block supplies its codec, its block entity and its block
 * entity type.
 */
public abstract class AbstractFarmBlock extends ModBaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    protected AbstractFarmBlock(Properties properties) {
        super(properties.noLootTable());
        this.registerDefaultState(EvolutionBlockProperties.withDefaults(this.stateDefinition.any().setValue(ACTIVE, false)));
    }

    /** The block entity type this block's ticker is bound to. */
    protected abstract BlockEntityType<? extends AbstractFarmBlockEntity> getFarmBlockEntityType();

    @NotNull
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @NotNull
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
        EvolutionBlockProperties.add(builder); // the tier and Overgrowth stage, for the model's plate and accents
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != getFarmBlockEntityType()) return null;
        return (lvl, pos, st, be) -> ((AbstractFarmBlockEntity) be).tick(lvl);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof AbstractFarmBlockEntity be) {
                    be.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    // A fluid-container item (bucket, flask, syringe, ...) fills/drains the fuel tank through the block's registered
    // FluidHandler capability; the FuelTank's biofuel filter + fluid-type matching mean only a biofuel -- and, once the tank
    // holds one, only the matching biofuel -- is accepted. When the container can do nothing here (a fluid the tank refuses, an
    // empty container with nothing to take, anything the tank cannot use) the click falls through and opens the GUI like a
    // bare hand. A Module in hand opens the GUI on the Module tab instead. The client always says "handled" for a container,
    // so the click reaches the server, whose answer decides.
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (isModule(stack)) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            openMenu(level, pos, player, AbstractFarmMenu.MODULE_TAB);
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (level.getBlockEntity(pos) instanceof AbstractFarmBlockEntity be
                    && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection())) {
                be.setChanged();
                be.updateBlock();
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // not usable on the fuel tank: open the GUI
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Whether the stack is a Module (a general Module or an Evolution Module): right-clicking with one opens the Module tab. */
    private static boolean isModule(ItemStack stack) {
        return stack.is(net.scruffy.dermicraft.datagen.tag.ModTags.Items.MODULES)
                || net.scruffy.dermicraft.block.entity.custom.base.EvolvingMachineBlockEntity.isEvolutionModule(stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) openMenu(level, pos, player, AbstractFarmMenu.HOME_TAB);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Opens the machine's screen on the given tab: the server's menu reads the request off the block entity and the client's
     * from the extra data, so both start on the same tab. */
    private static void openMenu(Level level, BlockPos pos, Player player, int tab) {
        if (level.getBlockEntity(pos) instanceof AbstractFarmBlockEntity be && player instanceof ServerPlayer serverPlayer) {
            be.requestOpenTab(tab);
            serverPlayer.openMenu(be, buf -> {
                buf.writeBlockPos(pos);
                buf.writeVarInt(tab);
            });
        }
    }
}
