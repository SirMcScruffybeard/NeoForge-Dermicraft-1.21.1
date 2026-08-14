package net.scruffy.dermicraft.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.network.GadgetBeamTargetPayload;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side broadcast half of the gadget-beam sync pair (see {@link GadgetBeamTargetPayload} for
 * the wire format and the reasoning for the self-predicts/others-synced split). Deliberately
 * generic -- any held-trigger beam gadget calls {@link #tick} once per server tick with its current
 * target, keyed by its own entity id, and gets change-detection + broadcast for free without
 * hand-rolling its own throttle logic.
 *
 * <p>Change-detection, not a raw per-tick broadcast: a held beam's target is often stationary for
 * many consecutive ticks (a player holding still on one block), and resending the same point every
 * tick would be pure waste. Only an actual change in active-state or a target that's moved past
 * {@link #POSITION_EPSILON} triggers a new packet; unchanged state costs nothing beyond the map
 * lookup.
 */
public final class GadgetBeamSync {

    private GadgetBeamSync() {}

    /** Below this, a target "move" is floating-point/raycast jitter, not a real aim change --
     * cosmetic beam, so sub-tenth-block precision isn't worth a packet. */
    private static final double POSITION_EPSILON = 0.05;

    private record LastSent(boolean active, Vec3 target) {}

    private static final Map<Integer, LastSent> LAST_SENT = new HashMap<>();

    /**
     * Call once per server tick per beam-capable entity. {@code target} is ignored when
     * {@code active} is false. Broadcasts only on a meaningful change, to every player currently
     * tracking {@code holder} (never to {@code holder} itself -- see the payload's javadoc for why).
     */
    public static void tick(ServerPlayer holder, boolean active, @Nullable Vec3 target) {
        int entityId = holder.getId();
        LastSent previous = LAST_SENT.get(entityId);

        boolean changed = previous == null
                || previous.active() != active
                || (active && (previous.target() == null || previous.target().distanceToSqr(target) > POSITION_EPSILON * POSITION_EPSILON));
        if (!changed) return;

        LAST_SENT.put(entityId, new LastSent(active, active ? target : null));

        GadgetBeamTargetPayload payload = active && target != null
                ? new GadgetBeamTargetPayload(entityId, true, (float) target.x, (float) target.y, (float) target.z)
                : GadgetBeamTargetPayload.inactive(entityId);

        PacketDistributor.sendToPlayersTrackingEntity(holder, payload);
    }

    /** Drops the cached last-sent state for an entity -- call on logout/gadget removal so a stale
     * entry doesn't suppress the next real activation's broadcast for a player who reconnects with a
     * fresh id reused by chance. Not strictly required (a wrong suppression only costs one missed
     * packet, immediately corrected by the next change), but cheap to keep tidy. */
    public static void clear(int entityId) {
        LAST_SENT.remove(entityId);
    }
}
