package net.scruffy.dermicraft.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.scruffy.dermicraft.main.ModDamageTypes;

public class BloodLet extends MovementDebuffEffect {

    private static final int DAMAGE_INTERVAL_TICKS = 50;

    protected BloodLet() {
        super(MobEffectCategory.HARMFUL, 0xFF4500, "blood_let");
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration != 0 && duration % DAMAGE_INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof Player player) {
            player.hurt(ModDamageTypes.getSource(player.level(), ModDamageTypes.BLOOD_LET), 1f);
        }
        return true;
    }
}
