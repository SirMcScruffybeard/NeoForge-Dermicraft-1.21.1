package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/** Client -> server: player clicked a machine's fluid auto-drain toggle button. Generic across every
 * {@code MachineBaseBlockEntity} with an output tank (Masticator/Effluentcer/their Charred variants,
 * Skin Tank/Charred Tank, Drooling Cauldron/Crucible) -- see
 * {@code CrawAutoPushToggleClickPayload} for the item-side equivalent this mirrors. */
public record AutoDrainToggleClickPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<AutoDrainToggleClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "auto_drain_toggle_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AutoDrainToggleClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AutoDrainToggleClickPayload::pos,
            AutoDrainToggleClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
