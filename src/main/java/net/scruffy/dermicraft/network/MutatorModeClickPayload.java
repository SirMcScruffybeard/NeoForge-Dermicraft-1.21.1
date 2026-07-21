package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/** Client -> server: player clicked the Mutator's Mutate/Fill mode toggle button. */
public record MutatorModeClickPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<MutatorModeClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "mutator_mode_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MutatorModeClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MutatorModeClickPayload::pos,
            MutatorModeClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
