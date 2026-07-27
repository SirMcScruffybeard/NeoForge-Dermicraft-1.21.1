package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * D.R.I.N.K.E.R.'s mode state, plus the arm/confirm timer guarding a Disposal void.
 *
 * <p>Stores the CYCLE STEP rather than the mode itself. The cycle is deliberately four steps over
 * three modes -- {@code Disposal -> Storage -> Transfer -> Storage -> } back to Disposal -- with
 * Storage sitting between the other two as a neutral rest stop, so you never traverse directly
 * from Transfer to Disposal. Storage therefore appears twice, and "which Storage am I on" decides
 * where the next press lands; a bare mode enum could not express that.
 */
public record DrinkerModeData(int step, boolean armed, long armedAtGameTime) {

    public static final int STEPS = 4;

    public enum Mode {DISPOSAL, STORAGE, TRANSFER}

    public static final Codec<DrinkerModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("step").forGetter(DrinkerModeData::step),
                    Codec.BOOL.fieldOf("armed").forGetter(DrinkerModeData::armed),
                    Codec.LONG.fieldOf("armedAtGameTime").forGetter(DrinkerModeData::armedAtGameTime))
            .apply(instance, DrinkerModeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DrinkerModeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DrinkerModeData::step,
            ByteBufCodecs.BOOL, DrinkerModeData::armed,
            ByteBufCodecs.VAR_LONG, DrinkerModeData::armedAtGameTime,
            DrinkerModeData::new);

    /** Starts on Storage rather than Disposal -- a fresh gadget should not default to destroying. */
    public static final DrinkerModeData DEFAULT = new DrinkerModeData(1, false, 0L);

    public Mode mode() {
        return switch (Math.floorMod(step, STEPS)) {
            case 0 -> Mode.DISPOSAL;
            case 2 -> Mode.TRANSFER;
            default -> Mode.STORAGE; // steps 1 and 3
        };
    }

    /** Advances one step, always forward. Any mode change clears a pending arm -- a confirm window
     * must never survive the player navigating away from the mode that opened it. */
    public DrinkerModeData next() {
        return new DrinkerModeData(Math.floorMod(step + 1, STEPS), false, 0L);
    }

    public DrinkerModeData armedAt(long gameTime) {
        return new DrinkerModeData(step, true, gameTime);
    }

    public DrinkerModeData disarmed() {
        return new DrinkerModeData(step, false, 0L);
    }
}
