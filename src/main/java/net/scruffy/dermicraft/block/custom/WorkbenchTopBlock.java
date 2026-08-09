package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchTopBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Workbench's top half -- purely visual/animated this pass (see WorkbenchTopBlockEntity's javadoc),
 * no GUI/menu of its own -- right-clicking it opens the *bottom's* menu (its own useWithoutItem
 * just delegates down to the paired bottom). Not meant to be placed by hand (see
 * ModCreativeModeTabs -- only the bottom is offered there); WorkbenchBlock#setPlacedBy is what
 * actually places this. Mirrors the bottom's own paired-removal logic so destroying either half
 * destroys both -- see WorkbenchBlock's class javadoc for the full pairing rule.
 */
public class WorkbenchTopBlock extends ModBaseEntityBlock {

    public static final MapCodec<WorkbenchTopBlock> CODEC = simpleCodec(WorkbenchTopBlock::new);

    // Same real-light treatment as WorkbenchBlock's own LIT -- see that field's javadoc. Toggled by
    // WorkbenchTopBlockEntity#setOpen.
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final int LIT_LEVEL = 8;

    // Set from WorkbenchBlock#setPlacedBy so the two halves always face the same way -- see that
    // field's javadoc for why GeoBlockRenderer needs no extra code to honor it.
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public WorkbenchTopBlock(Properties properties) {
        super(properties
                .noLootTable()
                .lightLevel(state -> state.getValue(LIT) ? LIT_LEVEL : 0)
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
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

    // GeckoLib blocks have no baked mesh of their own -- ENTITYBLOCK_ANIMATED tells vanilla the
    // block entity's own renderer draws everything, every frame, rather than using a static model.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WorkbenchTopBlockEntity(blockPos, blockState);
    }

    // Lets light pass through fully -- see WorkbenchBlock's identical override for why noOcclusion()
    // alone isn't enough.
    @Override
    protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            // Take the paired bottom with it -- guarded the same way WorkbenchBlock's own onRemove
            // is, against re-entrant clearing from that same call.
            BlockPos belowPos = pos.below();
            if (level.getBlockState(belowPos).is(ModBlocks.WORKBENCH.get())) {
                level.setBlock(belowPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos belowPos = pos.below();
        if (!level.isClientSide && level.getBlockEntity(belowPos) instanceof MenuProvider menuProvider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(belowPos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
