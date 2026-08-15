package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Shatter's charge/release state -- much simpler than {@link SunderModeData}'s rev/dig-in machine,
 * since this is just "hold right-click to charge, release to fire or reset," with no persisted
 * target/lock-on state to carry (see {@code ShatterItem#inventoryTick}). {@code since} is the game
 * time the current state was entered, the same way Sunder's own timing field works, so
 * {@code inventoryTick} can tell how long the charge has been building.
 */
public record ShatterModeData(int state, long since) {

    /**
     * IDLE -- not charging, piston shows the idle animation.
     * CHARGING -- trigger just pressed, {@code build_charge} playing toward full charge.
     * CHARGED -- full charge reached, {@code hold_charge} looping, waiting on release.
     */
    public enum State {IDLE, CHARGING, CHARGED}

    public static final Codec<ShatterModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("state").forGetter(ShatterModeData::state),
                    Codec.LONG.fieldOf("since").forGetter(ShatterModeData::since))
            .apply(instance, ShatterModeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShatterModeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShatterModeData::state,
            ByteBufCodecs.VAR_LONG, ShatterModeData::since,
            ShatterModeData::new);

    public static final ShatterModeData DEFAULT = new ShatterModeData(State.IDLE.ordinal(), 0L);

    public State stateEnum() {
        return State.values()[state];
    }

    public static ShatterModeData of(State state, long gameTime) {
        return new ShatterModeData(state.ordinal(), gameTime);
    }
}
