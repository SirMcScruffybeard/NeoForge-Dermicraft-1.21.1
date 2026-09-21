package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/** Client -> server: player clicked a Node leg's NBT/component exact-match toggle (item or fluid,
 * independently). Fluid-side toggle is stored but not yet consulted anywhere -- symmetry only, see
 * project_node_filter_system_design memory. */
public record NodeFilterNbtClickPayload(BlockPos pos, Direction direction, boolean fluid) implements CustomPacketPayload {

    public static final Type<NodeFilterNbtClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "node_filter_nbt_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeFilterNbtClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NodeFilterNbtClickPayload::pos,
            Direction.STREAM_CODEC, NodeFilterNbtClickPayload::direction,
            ByteBufCodecs.BOOL, NodeFilterNbtClickPayload::fluid,
            NodeFilterNbtClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
