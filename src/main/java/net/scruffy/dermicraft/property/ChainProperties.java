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
 * this material data never touches that path. {@code lootBonusChance} is Gold's own signature
 * trait (a "weak Fortune/Looting" bonus, see {@code SunderEvents#onLivingDrops}) -- a per-drop
 * chance to duplicate each item a kill drops, 0 for every material without the trait; applies
 * uniformly to every drop already present when it rolls (including a decapitated head, if any),
 * not restricted to ore-like drops the way real Fortune is, since this isn't trying to reuse
 * vanilla's actual enchantment mechanic. {@code statusEffect} is deliberately optional, not a slot
 * every material fills -- reserved for a small number of special-case materials as a signature
 * trait.
 *
 * <p>{@code igniteChance}/{@code igniteFireSeconds} are Blaze Essence's own signature trait -- a
 * per-hit chance to set the target on fire, 0/0 for every material without it. Standard hits roll
 * against {@code igniteChance} directly (see {@code SunderItem#hurtEnemy}); SAWING's continuous
 * pulses instead treat any nonzero {@code igniteChance} as a guaranteed ignite every pulse (see
 * {@code SunderItem#tickSawing}) -- {@code Entity#igniteForSeconds} only ever RAISES the remaining
 * fire duration, never lowers it, so re-applying the same {@code igniteFireSeconds} on every ~15-tick
 * pulse (well under a 4s/80-tick duration) keeps the target continuously refreshed through the whole
 * attack and lets it naturally burn out {@code igniteFireSeconds} after the last pulse, with no
 * separate "attack length" tracking needed. {@code smeltsLogs} is Blaze Essence's other trait --
 * SAWING's tree-felling drops Charcoal (via a real {@code SmeltingRecipe} lookup, granting its XP
 * too) instead of the raw Log, see {@code SunderItem#tickFelling}/{@code AutoSmeltUtil}. False for
 * every material without it.
 *
 * <p>{@code miningSpeed} (2026-08-27, added) mirrors the mounted chain's real vanilla Axe
 * equivalent's own destroy speed exactly -- vanilla tool speed is a per-Tier constant shared across
 * every tool type (Stone 4.0, Iron/Copper 6.0, Diamond 8.0, Netherite 9.0, Gold 12.0 -- fastest of
 * all despite the weakest combat/durability stats), same table {@code ShatterHeadProperties} uses
 * for its own Pickaxe equivalents. Materials with no real vanilla axe (Emerald, Blaze Essence) use
 * the same values their Shatter counterparts already settled on: Emerald at the Iron/Diamond
 * midpoint (7.0), Blaze Essence matching Iron (6.0). Unlike Shatter, axe-mineable blocks have no
 * real tier gate to mirror (logs/planks/pumpkins all drop regardless of tool), so this is speed-only
 * -- see {@code SunderItem#getDestroySpeed}. Swing (attack) speed is untouched by this -- a
 * completely separate {@code Attributes.ATTACK_SPEED} axis, not this field.
 */
public record ChainProperties(float damageMultiplier, float bleedChance, float decapChance,
                               float lootBonusChance, int durability, TextColor tint,
                               Optional<MobEffect> statusEffect, float igniteChance,
                               int igniteFireSeconds, boolean smeltsLogs, float miningSpeed) {

    public static final Codec<ChainProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf("damage_multiplier").forGetter(ChainProperties::damageMultiplier),
                    Codec.FLOAT.fieldOf("bleed_chance").forGetter(ChainProperties::bleedChance),
                    Codec.FLOAT.fieldOf("decap_chance").forGetter(ChainProperties::decapChance),
                    Codec.FLOAT.optionalFieldOf("loot_bonus_chance", 0.0f).forGetter(ChainProperties::lootBonusChance),
                    Codec.INT.fieldOf("durability").forGetter(ChainProperties::durability),
                    TextColor.CODEC.fieldOf("tint").forGetter(ChainProperties::tint),
                    BuiltInRegistries.MOB_EFFECT.byNameCodec().optionalFieldOf("status_effect").forGetter(ChainProperties::statusEffect),
                    Codec.FLOAT.optionalFieldOf("ignite_chance", 0.0f).forGetter(ChainProperties::igniteChance),
                    Codec.INT.optionalFieldOf("ignite_fire_seconds", 0).forGetter(ChainProperties::igniteFireSeconds),
                    Codec.BOOL.optionalFieldOf("smelts_logs", false).forGetter(ChainProperties::smeltsLogs),
                    Codec.FLOAT.optionalFieldOf("mining_speed", 1.0f).forGetter(ChainProperties::miningSpeed))
            .apply(instance, ChainProperties::new));
}
