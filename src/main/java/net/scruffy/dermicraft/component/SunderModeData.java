package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * Sunder's rev/dig-in state machine. Stores the current {@link State}, the game time it was
 * entered (so {@code inventoryTick} can detect when a timed state has elapsed), and -- only
 * meaningful during {@code SAWING} -- the locked target's UUID.
 *
 * <p>The target is a UUID, not a live {@code Entity} reference, because this is persistent
 * component data (survives a save/reload) and entities aren't safely storable that way. This isn't
 * extra complexity to handle reload as a special case: "can this UUID still be resolved to a live,
 * valid entity this tick" is already the exact check needed for the normal mid-play case too (the
 * target dies, despawns, or unloads while sawing) -- reload just becomes one more reason that check
 * can fail, not a separate code path.
 */
public record SunderModeData(int state, long since, Optional<UUID> target) {

    /**
     * IDLE -- not revved, chain shows the idle bone.
     * ARM_DELAY -- trigger just pressed, waiting out the initial 5-tick delay before anything
     *   visibly changes (a release here cancels straight back to IDLE, nothing to wind down).
     * ACTIVE -- chain swapped to the running bone, rev_up_down plays once into the running loop.
     *   Revved and idle -- not locked onto anything yet.
     * SAWING -- a revved hit landed on a valid target: locked in place, pulsed damage/fuel-then-
     *   hunger drain/chain wear running automatically (see SunderItem's inventoryTick), until the
     *   target dies, the time cap hits, resources run dry, or the player is knocked back. Named
     *   deliberately not "digging" -- see the design notes -- to avoid reading as "digs into the
     *   ground." Exits back to ACTIVE (not through the release sequence), since none of its exit
     *   conditions are "the player let go of the trigger."
     * RELEASE_DELAY -- trigger released (or forced by interruption), waiting out the second
     *   10-tick delay before winding down. Chain/animation stay as ACTIVE/SAWING left them.
     * UNREVVING -- animation target switched back to idle; GeckoLib's transition blend does the
     *   "reverse" visually (see the Sunder design notes -- this GeckoLib version has no true reverse
     *   playback, transitionLength blending is the proven substitute, same technique EaterItem's
     *   Body controller already relies on). Chain stays on the running bone until this window ends.
     */
    public enum State {IDLE, ARM_DELAY, ACTIVE, SAWING, RELEASE_DELAY, UNREVVING}

    public static final Codec<SunderModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("state").forGetter(SunderModeData::state),
                    Codec.LONG.fieldOf("since").forGetter(SunderModeData::since),
                    UUIDUtil.CODEC.optionalFieldOf("target").forGetter(SunderModeData::target))
            .apply(instance, SunderModeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SunderModeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SunderModeData::state,
            ByteBufCodecs.VAR_LONG, SunderModeData::since,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), SunderModeData::target,
            SunderModeData::new);

    public static final SunderModeData DEFAULT = new SunderModeData(State.IDLE.ordinal(), 0L, Optional.empty());

    public State stateEnum() {
        return State.values()[state];
    }

    public static SunderModeData of(State state, long gameTime) {
        return new SunderModeData(state.ordinal(), gameTime, Optional.empty());
    }

    public static SunderModeData sawing(UUID targetId, long gameTime) {
        return new SunderModeData(State.SAWING.ordinal(), gameTime, Optional.of(targetId));
    }
}
