package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/** Client -> server: player clicked a Node's per-direction button (item or fluid, independently),
 * cycle that leg's mode for the given type. */
public record NodeDirectionClickPayload(BlockPos pos, Direction direction, boolean fluid) implements CustomPacketPayload {

    public static final Type<NodeDirectionClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "node_direction_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeDirectionClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NodeDirectionClickPayload::pos,
            Direction.STREAM_CODEC, NodeDirectionClickPayload::direction,
            ByteBufCodecs.BOOL, NodeDirectionClickPayload::fluid,
            NodeDirectionClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
