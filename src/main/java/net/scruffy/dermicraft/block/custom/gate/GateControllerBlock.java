package net.scruffy.dermicraft.block.custom.gate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scruffy.dermicraft.block.custom.ModBaseEntityBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.GateControllerBlockEntity;
import net.scruffy.dermicraft.interfaces.IGateBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Gate Controller -- the brain of the Gate multiblock. Bonds (adjacency chain) to a target machine
 * that implements {@link net.scruffy.dermicraft.interfaces.IHasChannels} and to a sprawl of Buffers
 * and Ports, then, with NO GUI, greedily assigns bonded Buffers to the machine's unserviced channels
 * in the priority order the machine's channel list declares. Ticks the relay each poll cycle.
 */
public class GateControllerBlock extends ModBaseEntityBlock implements IGateBlock {

    public static final MapCodec<GateControllerBlock> CODEC = simpleCodec(GateControllerBlock::new);

    public GateControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new GateControllerBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return createTickerHelper(blockEntityType, ModBlockEntities.INNARDS_GATE_CONTROLLER_BE.get(),
                (lvl, blockPos, blockState, controller) -> controller.tick(lvl));
    }

    // Temporary diagnostic (see GateControllerBlockEntity#debugSummaryLines) -- empty-hand right-click
    // prints what this Controller currently sees. Commented out (not deleted) -- kept for future
    // debugging; a real diagnostic tool is planned to replace this ad hoc block-level hook (see
    // design notes on the diagnostic-tool + jam-clearing discussion).
//    @Override
//    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
//        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GateControllerBlockEntity controller) {
//            for (String line : controller.debugSummaryLines()) {
//                player.sendSystemMessage(Component.literal(line));
//            }
//        }
//        return InteractionResult.SUCCESS;
//    }
}
