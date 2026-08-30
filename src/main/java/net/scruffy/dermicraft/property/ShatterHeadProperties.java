package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.TextColor;

/**
 * Per-material stats for a Shatter head, looked up via {@code ModDataMaps.SHATTER_HEAD_PROPERTIES}
 * (keyed on the head Item) -- same "material stats live in a data map, not hardcoded per material
 * class" shape as Sunder's {@link ChainProperties}.
 *
 * <p>{@code miningTier} mirrors vanilla's own {@code Tiers#getLevel()} numbering (0 = Wood/Gold,
 * 1 = Stone, 2 = Iron, 3 = Diamond, 4 = Netherite) -- see {@code ShatterItem#meetsMiningTier} for how
 * it's actually checked against a block's {@code NEEDS_*_TOOL} tags. Real vanilla-style drops/speed
 * gate, not a hard block on the swing (see that method's own javadoc).
 *
 * <p>{@code damageShift} is a flat {@code ADD_VALUE} attack-damage modifier (2026-08-12, decided),
 * NOT a proportional multiplier the way Sunder's own {@code ChainProperties#damageMultiplier} works
 * -- deliberate, since heads are "distinct non-linear specials, not a power curve" (see the design
 * notes), and a flat per-material number is what actually fits that framing; a % multiplier would
 * have implied a linear curve regardless of the per-head special layered on top. Iron is the
 * baseline (shift = 0, per the decision that Iron is "the base" everything else is measured against)
 * -- every other material's shift is picked so Shatter's total attack damage with that head mounted
 * beats its real vanilla pickaxe equivalent's own total damage (see {@code ShatterItem}'s
 * {@code BASE_ATTACK_DAMAGE} javadoc for the actual target numbers and the margin used to derive
 * them). The per-head *special* (the non-linear combat identity distinct from either of these
 * numbers) is still an open design question -- {@code lootBonusChance} below is the first entry
 * in it, for Gold.
 *
 * <p>{@code lootBonusChance} is Gold's own signature trait (reversing an earlier "no dedicated
 * special" call for Gold) -- a mining-side counterpart to Sunder's identical Gold chain trait (see
 * {@code ChainProperties#lootBonusChance}'s own javadoc), but deliberately restricted to
 * {@code Tags.Blocks.ORES} rather than every block drop the way Sunder's combat version applies to
 * every mob drop -- "every block" would also catch things like Stone -> Cobblestone, which isn't
 * the intended "ore luck" identity. See {@code ShatterEvents#onBlockDropsLootBonus}. 0 for every
 * material without the trait.
 *
 * <p>{@code xpBonusChance}/{@code xpBonusAmount} are Knowledge's own signature trait -- a per-block
 * chance to spawn a bonus XP orb on top of whatever the block already awards, universal (not
 * ore-restricted the way Gold's loot bonus is, matching auto-smelt's own "every block" framing
 * instead) since it's rewarding the act of mining itself, not any one drop category. See
 * {@code ShatterEvents#onBlockDropsXpBonus}. 0/0 for every material without the trait.
 *
 * <p>{@code igniteChance}/{@code igniteFireSeconds} are Blaze Essence's own signature trait -- a
 * per-hit chance to set the target on fire, mirroring {@code ChainProperties}' identical fields
 * exactly (see that javadoc); Shatter has no sustained-attack mode the way Sunder's SAWING does, so
 * this always rolls the plain chance, no guaranteed-pulse override needed. See
 * {@code ShatterEvents#onIgniteOnHit}. {@code autoSmelt} is Blaze Essence's other trait -- every
 * block it mines drops its smelted result (via a real {@code SmeltingRecipe} lookup, granting XP
 * too) instead of the raw drop, universal (not ore-restricted the way Gold's loot bonus is) -- a
 * block with no smelting recipe just drops normally. See
 * {@code ShatterEvents#onBlockDropsAutoSmelt}/{@code AutoSmeltUtil}. False for every material
 * without the trait.
 *
 * <p>{@code miningSpeed} (2026-08-27, added) mirrors the mounted head's real vanilla pickaxe
 * equivalent's own destroy speed exactly (Stone 4.0, Iron/Copper 6.0, Diamond 8.0, Netherite 9.0,
 * Gold 12.0 -- fastest of all despite the lowest mining tier, the same "fast digging, no
 * effectiveness" flavor {@code miningTier} already captures for Gold). Materials with no real
 * vanilla pickaxe (Emerald, Blaze Essence) borrow the nearest equivalent already established
 * elsewhere in this data map: Emerald sits at the same Iron/Diamond midpoint (7.0) its
 * {@code damageShift} uses, Blaze Essence matches Iron (6.0) since its baseline stats are Iron's
 * throughout, the value being entirely in its ignite/auto-smelt traits. See
 * {@code ShatterItem#getDestroySpeed} for how this is actually consulted.
 */
public record ShatterHeadProperties(TextColor tint, int miningTier, float damageShift, float lootBonusChance,
                                     float igniteChance, int igniteFireSeconds, boolean autoSmelt, float miningSpeed,
                                     float xpBonusChance, float xpBonusAmount) {

    public static final Codec<ShatterHeadProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    TextColor.CODEC.fieldOf("tint").forGetter(ShatterHeadProperties::tint),
                    Codec.INT.fieldOf("mining_tier").forGetter(ShatterHeadProperties::miningTier),
                    Codec.FLOAT.optionalFieldOf("damage_shift", 0.0f).forGetter(ShatterHeadProperties::damageShift),
                    Codec.FLOAT.optionalFieldOf("loot_bonus_chance", 0.0f).forGetter(ShatterHeadProperties::lootBonusChance),
                    Codec.FLOAT.optionalFieldOf("ignite_chance", 0.0f).forGetter(ShatterHeadProperties::igniteChance),
                    Codec.INT.optionalFieldOf("ignite_fire_seconds", 0).forGetter(ShatterHeadProperties::igniteFireSeconds),
                    Codec.BOOL.optionalFieldOf("auto_smelt", false).forGetter(ShatterHeadProperties::autoSmelt),
                    Codec.FLOAT.optionalFieldOf("mining_speed", 1.0f).forGetter(ShatterHeadProperties::miningSpeed),
                    Codec.FLOAT.optionalFieldOf("xp_bonus_chance", 0.0f).forGetter(ShatterHeadProperties::xpBonusChance),
                    Codec.FLOAT.optionalFieldOf("xp_bonus_amount", 0.0f).forGetter(ShatterHeadProperties::xpBonusAmount))
            .apply(instance, ShatterHeadProperties::new));
}
