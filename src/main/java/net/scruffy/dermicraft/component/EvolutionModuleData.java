package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.scruffy.dermicraft.hazard.HazardFamily;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * What an Evolution Module does, carried on the module's stack as the {@code EVOLUTION_MODULE_DATA}
 * component. A module stack without this component is the bare Evolution Module: it fits the Evolution
 * slot and does nothing. The component holds no numbers; {@code EvolutionRules} decides whether the upgrade
 * applies to a machine and {@code EvolutionStats} supply the time from the server config.
 *
 * @param type   which kind of upgrade
 * @param level  the level the module grants: the tier it raises to (TIER), the hazard level 1 mild or
 *               2 severe (HAZARD), or the Overgrowth level it raises to (OVERGROWTH); at least 1
 * @param family the hazard family, for HAZARD modules only (null otherwise)
 */
public record EvolutionModuleData(Type type, int level, @Nullable HazardFamily family) {

    public enum Type implements StringRepresentable {
        TIER, HAZARD, OVERGROWTH;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public EvolutionModuleData {
        level = Math.max(1, level);
        if (type != Type.HAZARD) family = null;
    }

    public static final Codec<EvolutionModuleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    StringRepresentable.fromEnum(Type::values).fieldOf("type").forGetter(EvolutionModuleData::type),
                    Codec.INT.fieldOf("level").forGetter(EvolutionModuleData::level),
                    StringRepresentable.fromEnum(HazardFamily::values).optionalFieldOf("family")
                            .forGetter(data -> Optional.ofNullable(data.family())))
            .apply(instance, (type, level, family) -> new EvolutionModuleData(type, level, family.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, EvolutionModuleData> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(Type.class), EvolutionModuleData::type,
            ByteBufCodecs.VAR_INT, EvolutionModuleData::level,
            ByteBufCodecs.optional(NeoForgeStreamCodecs.enumCodec(HazardFamily.class)),
            data -> Optional.ofNullable(data.family()),
            (type, level, family) -> new EvolutionModuleData(type, level, family.orElse(null)));

    /** A Tier module that raises a machine to {@code targetTier}. */
    public static EvolutionModuleData tier(int targetTier) {
        return new EvolutionModuleData(Type.TIER, targetTier, null);
    }

    /** A hazard module granting {@code family} at {@code level} (1 mild, 2 severe; a Severe module also covers Mild). */
    public static EvolutionModuleData hazard(HazardFamily family, int level) {
        return new EvolutionModuleData(Type.HAZARD, level, family);
    }

    /** An Overgrowth module that raises a machine to Overgrowth {@code targetLevel}. */
    public static EvolutionModuleData overgrowth(int targetLevel) {
        return new EvolutionModuleData(Type.OVERGROWTH, targetLevel, null);
    }
}
