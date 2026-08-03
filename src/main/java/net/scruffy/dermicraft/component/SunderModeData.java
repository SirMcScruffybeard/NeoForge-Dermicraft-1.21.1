package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Sunder's rev/dig-in state machine (basic pass -- no fuel gating or dig-in mechanic yet, see
 * SunderItem's class javadoc). Stores the current {@link State} plus the game time it was entered,
 * so {@code inventoryTick} can detect when a timed state (the two 10-tick delays, the wind-down
 * window) has elapsed.
 */
public record SunderModeData(int state, long since) {

    /**
     * IDLE -- not revved, chain shows the idle bone.
     * ARM_DELAY -- trigger just pressed, waiting out the initial 10-tick delay before anything
     *   visibly changes (a release here cancels straight back to IDLE, nothing to wind down).
     * ACTIVE -- chain swapped to the running bone, rev_up_down plays once into the running loop.
     * RELEASE_DELAY -- trigger released (or forced by interruption, once that exists), waiting out
     *   the second 10-tick delay before winding down. Chain/animation stay as ACTIVE left them.
     * UNREVVING -- animation target switched back to idle; GeckoLib's transition blend does the
     *   "reverse" visually (see the Sunder design notes -- this GeckoLib version has no true reverse
     *   playback, transitionLength blending is the proven substitute, same technique EaterItem's
     *   Body controller already relies on). Chain stays on the running bone until this window ends.
     */
    public enum State {IDLE, ARM_DELAY, ACTIVE, RELEASE_DELAY, UNREVVING}

    public static final Codec<SunderModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("state").forGetter(SunderModeData::state),
                    Codec.LONG.fieldOf("since").forGetter(SunderModeData::since))
            .apply(instance, SunderModeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SunderModeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SunderModeData::state,
            ByteBufCodecs.VAR_LONG, SunderModeData::since,
            SunderModeData::new);

    public static final SunderModeData DEFAULT = new SunderModeData(State.IDLE.ordinal(), 0L);

    public State stateEnum() {
        return State.values()[state];
    }

    public static SunderModeData of(State state, long gameTime) {
        return new SunderModeData(state.ordinal(), gameTime);
    }
}
