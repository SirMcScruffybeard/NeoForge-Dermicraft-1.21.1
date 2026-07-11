package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/** Client -> server: player clicked a Node's distribution-mode toggle (Round-Robin <-> Equal Spread). */
public record NodeDistributionClickPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<NodeDistributionClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "node_distribution_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeDistributionClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NodeDistributionClickPayload::pos,
            NodeDistributionClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
