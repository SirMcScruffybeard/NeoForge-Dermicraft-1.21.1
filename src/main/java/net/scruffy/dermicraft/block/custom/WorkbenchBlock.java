package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Workbench's bottom half -- hosts the real Storage/Mod/Fabrication GUI (see WorkbenchBlockEntity).
 * Paired with WorkbenchTopBlock, vanilla-door-style: placing the bottom auto-places the top directly
 * above (and refuses to place at all if there's no room for it, same as a door), and destroying
 * either half destroys both -- there's only ever one recoverable object, the bottom (today that
 * just means neither half drops anything on plain breaking, matching this mod's existing
 * "destroyed unless Forceps" convention; real Forceps recovery for Workbench isn't built yet and
 * should only ever target the bottom when it is). No true multiblock validation beyond this pass --
 * see dermicraft-gear-stations-notes.md -> Construction for the eventual full two-block structure.
 */
public class WorkbenchBlock extends ModBaseEntityBlock {

    public static final MapCodec<WorkbenchBlock> CODEC = simpleCodec(WorkbenchBlock::new);

    public WorkbenchBlock(Properties properties) {
        super(properties.noLootTable());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // GeckoLib-rendered (no baked mesh of its own) -- see WorkbenchTopBlock's identical override.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WorkbenchBlockEntity(blockPos, blockState);
    }

    // Same "can't place without room for the other half" rule a vanilla door uses. Must also accept
    // the space already being the paired top -- checking replaceable() alone would flip false the
    // instant setPlacedBy places the top there, since a real placed block isn't "replaceable"
    // anymore, and that check re-fires on the neighbor-changed event placing the top itself causes.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.canBeReplaced() || above.is(ModBlocks.WORKBENCH_TOP.get());
    }

    // Lets light pass through fully -- .noOcclusion() alone isn't enough for lighting: it only
    // switches the light engine to consult getShape() instead of assuming a full cube, but we never
    // overrode getShape (still a full 16x16x16 box), so it still computed as light-blocking either
    // way. This is the direct fix, independent of collision shape.
    @Override
    protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(pos.above(), ModBlocks.WORKBENCH_TOP.get().defaultBlockState(), 3);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (state.getBlock() != newState.getBlock()) {
                if (level.getBlockEntity(pos) instanceof WorkbenchBlockEntity workbench) {
                    workbench.drops();
                }
                // Take the paired top with it -- guarded against re-entrant clearing (this setBlock
                // call fires WorkbenchTopBlock's own onRemove synchronously, which checks this same
                // position before trying to clear it back).
                BlockPos abovePos = pos.above();
                if (level.getBlockState(abovePos).is(ModBlocks.WORKBENCH_TOP.get())) {
                    level.setBlock(abovePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider menuProvider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
