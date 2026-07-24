package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * S.I.P.P.I.N.G.'s mode state: Storage vs Disposal, plus the arm/confirm-timer state for the
 * Storage-to-Disposal switch (only relevant while the buffer is non-empty -- see project notes).
 */
public record SippingModeData(boolean disposalMode, boolean armed, long armedAtGameTime) {

    public static final Codec<SippingModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("disposalMode").forGetter(SippingModeData::disposalMode),
                    Codec.BOOL.fieldOf("armed").forGetter(SippingModeData::armed),
                    Codec.LONG.fieldOf("armedAtGameTime").forGetter(SippingModeData::armedAtGameTime))
            .apply(instance, SippingModeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SippingModeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SippingModeData::disposalMode,
            ByteBufCodecs.BOOL, SippingModeData::armed,
            ByteBufCodecs.VAR_LONG, SippingModeData::armedAtGameTime,
            SippingModeData::new);

    public static final SippingModeData DEFAULT = new SippingModeData(false, false, 0L);
}
