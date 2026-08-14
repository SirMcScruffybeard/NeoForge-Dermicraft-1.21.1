package net.scruffy.dermicraft.client;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache of "where is entity X's gadget beam currently pointing", fed by
 * {@link net.scruffy.dermicraft.network.GadgetBeamTargetPayload}. Render layers read this for
 * every beam-holder EXCEPT the local player, who predicts their own beam from a local raycast
 * instead (see the payload's javadoc). Generic across gadgets by design -- keyed purely by entity
 * id, no gadget-specific state.
 *
 * <p>No expiry timer: the server sends an explicit {@code active = false} the moment a beam turns
 * off (trigger released, target lost, holder logs out server-side, etc.), so there's no "went
 * quiet" case that needs a timeout to catch -- an entry only exists here while the server considers
 * that beam live.
 */
public final class GadgetBeamTracker {

    private GadgetBeamTracker() {}

    private static final Map<Integer, Vec3> TARGETS = new HashMap<>();

    public static void update(int entityId, boolean active, Vec3 target) {
        if (!active) {
            TARGETS.remove(entityId);
            return;
        }
        TARGETS.put(entityId, target);
    }

    /** Null means "no active synced beam for this entity" -- either it never had one, or the server
     * just cleared it. Callers render nothing in that case. */
    @Nullable
    public static Vec3 targetFor(int entityId) {
        return TARGETS.get(entityId);
    }
}
