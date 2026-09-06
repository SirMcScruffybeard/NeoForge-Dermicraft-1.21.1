package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A.I.D.'s active mode -- which of the four base tools (Forceps, Scalpel, Suture, Syringe) the
 * device currently acts as. Single-direction cycle only, in that fixed order (see AidItem's class
 * javadoc), so a plain ordinal is enough -- no rest-stop steps the way D.R.I.N.K.E.R.'s mode needs.
 */
public record AidModeData(int mode) {

    public enum Mode {FORCEPS, SCALPEL, SUTURE, SYRINGE}

    public static final Codec<AidModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("mode").forGetter(AidModeData::mode))
            .apply(instance, AidModeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AidModeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AidModeData::mode,
            AidModeData::new);

    public static final AidModeData DEFAULT = new AidModeData(Mode.FORCEPS.ordinal());

    public Mode modeEnum() {
        return Mode.values()[mode];
    }

    public AidModeData next() {
        return new AidModeData((mode + 1) % Mode.values().length);
    }
}
