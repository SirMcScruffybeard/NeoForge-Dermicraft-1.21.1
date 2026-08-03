package net.scruffy.dermicraft.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.scruffy.dermicraft.main.ModDamageTypes;

/**
 * Sunder's on-hit combat Bleed -- damage-over-time (poison-adjacent rate, not {@link BloodLet}'s
 * much slower surgical-context one) combined with {@link MovementDebuffEffect}'s inherited
 * slowness/jump penalty, per the design decision to mix a straightforward DoT with "the effect
 * from our blood-let effect" rather than building something unrelated. Unlike {@link BloodLet}
 * (player-only), this applies to any {@link LivingEntity} -- it's inflicted on mobs, not self-harm.
 *
 * <p>Numbers are a first pass, not tuned -- see the Sunder design notes' open questions.
 */
public class Bleed extends MovementDebuffEffect {

    private static final int DAMAGE_INTERVAL_TICKS = 25;
    private static final float DAMAGE_PER_TICK = 1.0F;

    protected Bleed() {
        super(MobEffectCategory.HARMFUL, 0xB22222, "bleed");
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration != 0 && duration % DAMAGE_INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        livingEntity.hurt(ModDamageTypes.getSource(livingEntity.level(), ModDamageTypes.BLEED), DAMAGE_PER_TICK);
        return true;
    }
}
