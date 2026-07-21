package net.scruffy.dermicraft.network;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.scruffy.dermicraft.block.entity.custom.MutatorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(NodeDirectionClickPayload.TYPE, NodeDirectionClickPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    BlockEntity be = context.player().level().getBlockEntity(payload.pos());
                    if (be instanceof NodeBlockEntity node) {
                        node.cycleDirection(payload.direction());
                    }
                }));

        registrar.playToServer(NodeDistributionClickPayload.TYPE, NodeDistributionClickPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    BlockEntity be = context.player().level().getBlockEntity(payload.pos());
                    if (be instanceof NodeBlockEntity node) {
                        node.cycleDistribution();
                    }
                }));

        registrar.playToServer(NodeTransferToggleClickPayload.TYPE, NodeTransferToggleClickPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    BlockEntity be = context.player().level().getBlockEntity(payload.pos());
                    if (be instanceof NodeBlockEntity node) {
                        if (payload.fluid()) node.toggleFluids(payload.direction());
                        else node.toggleItems(payload.direction());
                    }
                }));

        registrar.playToServer(MutatorModeClickPayload.TYPE, MutatorModeClickPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    BlockEntity be = context.player().level().getBlockEntity(payload.pos());
                    if (be instanceof MutatorBlockEntity mutator) {
                        mutator.toggleMode();
                    }
                }));
    }
}
