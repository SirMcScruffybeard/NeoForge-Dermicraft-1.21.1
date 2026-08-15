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
 * numbers) is still an open design question.
 */
public record ShatterHeadProperties(TextColor tint, int miningTier, float damageShift) {

    public static final Codec<ShatterHeadProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    TextColor.CODEC.fieldOf("tint").forGetter(ShatterHeadProperties::tint),
                    Codec.INT.fieldOf("mining_tier").forGetter(ShatterHeadProperties::miningTier),
                    Codec.FLOAT.optionalFieldOf("damage_shift", 0.0f).forGetter(ShatterHeadProperties::damageShift))
            .apply(instance, ShatterHeadProperties::new));
}
