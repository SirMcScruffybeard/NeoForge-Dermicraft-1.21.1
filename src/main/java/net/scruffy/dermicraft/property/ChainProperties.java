package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffect;

import java.util.Optional;

/**
 * Per-material stats for a Sunder chain, looked up via {@code ModDataMaps.SUNDER_CHAIN_PROPERTIES}
 * (keyed on the chain Item) rather than hardcoded per material class -- see the Chain durability
 * design notes in {@code dermicraft-gadget-notes.md} for the full reasoning behind each field.
 *
 * <p>{@code damageMultiplier} is unified across standard-hit and dig-in for now (split later only
 * if needed). {@code bleedChance}/{@code decapChance} are additive and apply to standard hits
 * only -- dig-in's Bleed/decapitation are a boolean gate on chain presence, not chance-based, so
 * this material data never touches that path. {@code statusEffect} is deliberately optional, not a
 * slot every material fills -- reserved for a small number of special-case materials as a
 * signature trait.
 */
public record ChainProperties(float damageMultiplier, float bleedChance, float decapChance,
                               int durability, TextColor tint, Optional<MobEffect> statusEffect) {

    public static final Codec<ChainProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf("damage_multiplier").forGetter(ChainProperties::damageMultiplier),
                    Codec.FLOAT.fieldOf("bleed_chance").forGetter(ChainProperties::bleedChance),
                    Codec.FLOAT.fieldOf("decap_chance").forGetter(ChainProperties::decapChance),
                    Codec.INT.fieldOf("durability").forGetter(ChainProperties::durability),
                    TextColor.CODEC.fieldOf("tint").forGetter(ChainProperties::tint),
                    BuiltInRegistries.MOB_EFFECT.byNameCodec().optionalFieldOf("status_effect").forGetter(ChainProperties::statusEffect))
            .apply(instance, ChainProperties::new));
}
