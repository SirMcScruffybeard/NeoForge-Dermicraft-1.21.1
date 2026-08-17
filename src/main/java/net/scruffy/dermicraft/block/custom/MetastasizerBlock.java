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
import net.scruffy.dermicraft.block.entity.custom.MetastasizerBlockEntity;
import net.scruffy.dermicraft.machine.MachineTier;
import net.scruffy.dermicraft.machine.TieredMachine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetastasizerBlock extends ModBaseEntityBlock implements TieredMachine {

    public static final MapCodec<MetastasizerBlock> CODEC = simpleCodec(MetastasizerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Drives which face texture renders (idle / running / recovering-HP "error"); see MetastasizerVisualState.
    public static final EnumProperty<MetastasizerVisualState> STATE = EnumProperty.create("state", MetastasizerVisualState.class);

    private final MachineTier tier;

    // simpleCodec needs a Properties-only constructor; it produces the progenitor (BASIC) tier.
    // A future upgrade tier registers its own block via the (Properties, MachineTier) constructor.
    public MetastasizerBlock(Properties properties) {
        this(properties, MachineTier.BASIC);
    }

    public MetastasizerBlock(Properties properties, MachineTier tier) {
        super(properties
                .noLootTable()
                .ignitedByLava()
        );
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE, MetastasizerVisualState.IDLE));
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
        return new MetastasizerBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof MetastasizerBlockEntity metastasizerBlockEntity) {
                    metastasizerBlockEntity.drops();
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    // Right-click routing mirrors the automation face pattern: top = fuel tank, sides = both
    // the reagent tank (bucket) and the pattern item, bottom = result slot (extract only).
    //
    // Empty-hand clicks always fall through to useWithoutItem (the GUI) -- no quick-extract
    // shortcut. A held item always resolves to SUCCESS here and never falls through -- vanilla
    // opens the GUI on ANY PASS_TO_DEFAULT_BLOCK_INTERACTION result regardless of held item (it
    // doesn't check for an empty hand), so e.g. holding cobblestone at the fuel face (no fluid
    // handler, no pattern slot there) would otherwise fall through and pop the GUI open. Mirrors
    // MasticatorBlock/EffluentcerBlock's interaction shape.
    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof MetastasizerBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction face = hitResult.getDirection();

        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, face)) {
            be.setChanged();
            be.updateBlock();
            return ItemInteractionResult.SUCCESS;
        }

        if (face != Direction.UP && face != Direction.DOWN
                && stack.getCapability(Capabilities.FluidHandler.ITEM) == null) {
            int before = stack.getCount();
            ItemStack leftover = be.insertPattern(stack);
            if (leftover.getCount() == before) {
                // Nothing was actually inserted (pattern slot already occupied by something else) --
                // don't eat the click, fall through so vanilla opens the GUI instead.
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            player.setItemInHand(hand, leftover);
        }

        return ItemInteractionResult.SUCCESS;
    }

    // Opens the GUI directly on any click useItemOn didn't claim -- no Outerface required. The
    // block still carries the HAS_SCREEN tag, so the Outerface continues to work too.
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
        return createTickerHelper(blockEntityType, ModBlockEntities.METASTASIZER_BE.get(),
                ((level, blockPos, blockState, be) -> be.tick(level)));
    }
}
