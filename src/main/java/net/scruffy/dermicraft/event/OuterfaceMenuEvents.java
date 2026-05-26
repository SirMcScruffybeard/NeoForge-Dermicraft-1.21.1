package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.scruffy.dermicraft.block.custom.OuterfaceBlock;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class OuterfaceMenuEvents {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    @SubscribeEvent
    public static void onCloseMenu(PlayerContainerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Level level = player.level();

            double reach = 8;
            HitResult hit = player.pick(reach, 1f, false);

            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
                BlockPos targetPos = blockHit.getBlockPos();
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.getBlock() instanceof OuterfaceBlock outerface
                        && targetState.getValue(OuterfaceBlock.STATUS) == outerface.ON){

                    SCHEDULER.schedule(() -> {
                       player.server.execute(() -> {
                           if (level.getBlockState(targetPos).getBlock() instanceof OuterfaceBlock
                                   && targetState.getValue(OuterfaceBlock.STATUS) == outerface.ON) {
                               outerface.turnOff(level, targetPos, targetState);
                           }
                       });
                    }, 1, TimeUnit.SECONDS);
                }
            }
        }
    }
}
