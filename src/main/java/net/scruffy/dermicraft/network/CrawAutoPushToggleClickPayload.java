package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/** Client -> server: player clicked a Craw's auto-push (pass-down) toggle button. */
public record CrawAutoPushToggleClickPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<CrawAutoPushToggleClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "craw_auto_push_toggle_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrawAutoPushToggleClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CrawAutoPushToggleClickPayload::pos,
            CrawAutoPushToggleClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
