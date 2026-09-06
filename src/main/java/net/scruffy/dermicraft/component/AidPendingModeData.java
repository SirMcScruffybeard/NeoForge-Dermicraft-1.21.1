package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A.I.D.'s in-flight mode transition. Present only while the outgoing mode has a retract
 * animation playing (currently just Forceps -- see AidItem's class javadoc): the real
 * {@link AidModeData} isn't updated (so the outgoing bone stays visible and the retract clip
 * stays on screen) until {@code commitAtGameTime}, when AidItem's inventoryTick applies
 * {@code targetMode} and clears this component. Absent entirely for a mode with no retract clip
 * yet, which still commits instantly.
 */
public record AidPendingModeData(int targetMode, long commitAtGameTime) {

    public static final Codec<AidPendingModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("target_mode").forGetter(AidPendingModeData::targetMode),
                    Codec.LONG.fieldOf("commit_at_game_time").forGetter(AidPendingModeData::commitAtGameTime))
            .apply(instance, AidPendingModeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AidPendingModeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AidPendingModeData::targetMode,
            ByteBufCodecs.VAR_LONG, AidPendingModeData::commitAtGameTime,
            AidPendingModeData::new);
}
