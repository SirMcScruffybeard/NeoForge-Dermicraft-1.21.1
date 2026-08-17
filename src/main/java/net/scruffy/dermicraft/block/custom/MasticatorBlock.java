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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.machine.MachineTier;
import net.scruffy.dermicraft.machine.TieredMachine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MasticatorBlock extends ModBaseEntityBlock implements TieredMachine {

    public static final MapCodec<MasticatorBlock> CODEC = simpleCodec(MasticatorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Drives which face texture renders (idle / running / recovering-HP "error"); see MasticatorVisualState.
    public static final EnumProperty<MasticatorVisualState> STATE = EnumProperty.create("state", MasticatorVisualState.class);

    private final MachineTier tier;

    // simpleCodec needs a Properties-only constructor; it produces the progenitor (BASIC) tier.
    // A future upgrade tier registers its own block via the (Properties, MachineTier) constructor.
    public MasticatorBlock(Properties properties) {
        this(properties, MachineTier.BASIC);
    }

    public MasticatorBlock(Properties properties, MachineTier tier) {
        super(properties
                .noLootTable()
                .ignitedByLava()
        );
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE, MasticatorVisualState.IDLE));
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MasticatorBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof MasticatorBlockEntity masticatorBlockEntity) {
                    masticatorBlockEntity.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    // Empty-hand quick-extraction is a CROUCH action -- a plain empty-hand click always falls
    // through to useWithoutItem (the GUI); crouch + empty hand pulls the ingredient item instead.
    // Mirrors MutatorBlock's interaction shape.
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof MasticatorBlockEntity masticator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction face = hitResult.getDirection();

        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, face)) {
            masticator.setChanged();
            masticator.updateBlock();
            return ItemInteractionResult.SUCCESS;
        }

        boolean crouchExtract = player.getItemInHand(hand).isEmpty() && player.isShiftKeyDown();
        if (crouchExtract) {
            ItemStack extracted = masticator.extractIngredients();
            if (!extracted.isEmpty()) {
                player.setItemInHand(hand, extracted);
                return ItemInteractionResult.SUCCESS;
            }
        } else if (!player.getItemInHand(hand).isEmpty()
                && stack.getCapability(Capabilities.FluidHandler.ITEM) == null) {
            int before = stack.getCount();
            ItemStack leftover = masticator.insertItemStack(stack);
            if (leftover.getCount() != before) {
                player.setItemInHand(hand, leftover);
                return ItemInteractionResult.SUCCESS;
            }
            // Nothing was actually inserted (ingredient slot already occupied by something else) --
            // don't eat the click, fall through so vanilla opens the GUI instead.
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Opens the GUI directly on any empty-hand click that useItemOn didn't claim (i.e. not crouching,
    // or crouching at a face with nothing to pull) -- no Outerface required. The block still carries
    // the HAS_SCREEN tag, so the Outerface continues to work too.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider menuProvider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState state, BlockEntityType<T> blockEntityType) {
        if (pLevel.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.MASTICATOR_BE.get(),
                ((level, blockPos, blockState, DcBlockEntity) -> DcBlockEntity.tick(level)));
    }
}
