package net.scruffy.dermicraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Server -> client: "entity {@code entityId} is currently aiming a gadget beam at {@code (x, y,
 * z)}", or {@code active = false} to clear it. Deliberately generic (entity id + point, nothing
 * gadget-specific) so any held-trigger, beam-style gadget can reuse the same wire format and
 * client-side cache ({@code GadgetBeamTracker}) instead of growing its own payload -- the first use
 * is Eater's planned ranged-mining outfit, but the shape doesn't assume Eater at all.
 *
 * <p>Only ever sent to players tracking {@code entityId}, NOT to that entity's own client -- the
 * holder renders their own beam from a local, zero-latency raycast instead (same reasoning vanilla
 * uses for arm swings/sprint particles: the person acting predicts their own feedback, everyone
 * else sees the server-confirmed version a tick or two behind). See the beam-sync design
 * discussion for the full reasoning.
 *
 * <p>Target is encoded as floats, not doubles -- this is a cosmetic beam endpoint, not authoritative
 * position data, so sub-millimeter precision buys nothing and only costs bandwidth on a payload
 * meant to be sent every tick a beam is active.
 */
public record GadgetBeamTargetPayload(int entityId, boolean active, float x, float y, float z) implements CustomPacketPayload {

    public static final Type<GadgetBeamTargetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "gadget_beam_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GadgetBeamTargetPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GadgetBeamTargetPayload::entityId,
            ByteBufCodecs.BOOL, GadgetBeamTargetPayload::active,
            ByteBufCodecs.FLOAT, GadgetBeamTargetPayload::x,
            ByteBufCodecs.FLOAT, GadgetBeamTargetPayload::y,
            ByteBufCodecs.FLOAT, GadgetBeamTargetPayload::z,
            GadgetBeamTargetPayload::new);

    /** Convenience for the common "beam turned off" case, so callers don't hand-roll zeroed coords. */
    public static GadgetBeamTargetPayload inactive(int entityId) {
        return new GadgetBeamTargetPayload(entityId, false, 0, 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
