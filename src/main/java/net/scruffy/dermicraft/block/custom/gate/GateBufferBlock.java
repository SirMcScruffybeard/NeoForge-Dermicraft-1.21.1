package net.scruffy.dermicraft.block.custom.gate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.block.custom.ModBaseEntityBlock;
import net.scruffy.dermicraft.block.entity.custom.GateBufferBlockEntity;
import net.scruffy.dermicraft.interfaces.IGateBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Gate Buffer -- the universal relay slot of the Gate multiblock. A single block that becomes an
 * item-slot buffer or a fluid-tank buffer depending on the channel the Controller assigns it (kind
 * can flip freely while empty). Holds the actual relayed contents; the Controller's tick moves those
 * to/from the target machine's channel, and a touching Port exposes them outward. On break it drops
 * its buffered items but never spills fluid -- same no-punishment rule as the Node.
 */
public class GateBufferBlock extends ModBaseEntityBlock implements IGateBlock {

    public static final MapCodec<GateBufferBlock> CODEC = simpleCodec(GateBufferBlock::new);

    public GateBufferBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new GateBufferBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof GateBufferBlockEntity buffer) {
                buffer.drops();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Temporary diagnostic (see GateControllerBlock's matching one) -- empty-hand right-click prints
    // this Buffer's assigned channel/kind and current contents. Commented out (not deleted) -- kept
    // for future debugging; a real diagnostic tool is planned to replace this ad hoc block-level hook.
//    @Override
//    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
//        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GateBufferBlockEntity buffer) {
//            player.sendSystemMessage(Component.literal(
//                    "Assigned: " + buffer.getAssignedChannelId() + " [" + buffer.getAssignedKind() + "], empty=" + buffer.isEmpty()));
//            player.sendSystemMessage(Component.literal(
//                    "Item: " + buffer.getItemStore().getStackInSlot(0) + ", Fluid: " + buffer.getFluidStore().getFluid()));
//        }
//        return InteractionResult.SUCCESS;
//    }
}
