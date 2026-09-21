package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/** Client -> server: player clicked a Node leg's whitelist/blacklist toggle (item or fluid,
 * independently). */
public record NodeFilterModeClickPayload(BlockPos pos, Direction direction, boolean fluid) implements CustomPacketPayload {

    public static final Type<NodeFilterModeClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "node_filter_mode_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeFilterModeClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NodeFilterModeClickPayload::pos,
            Direction.STREAM_CODEC, NodeFilterModeClickPayload::direction,
            ByteBufCodecs.BOOL, NodeFilterModeClickPayload::fluid,
            NodeFilterModeClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
