package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/** Client -> server: player clicked a Node's per-leg item or fluid toggle button. */
public record NodeTransferToggleClickPayload(BlockPos pos, Direction direction, boolean fluid) implements CustomPacketPayload {

    public static final Type<NodeTransferToggleClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "node_transfer_toggle_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeTransferToggleClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NodeTransferToggleClickPayload::pos,
            Direction.STREAM_CODEC, NodeTransferToggleClickPayload::direction,
            ByteBufCodecs.BOOL, NodeTransferToggleClickPayload::fluid,
            NodeTransferToggleClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
