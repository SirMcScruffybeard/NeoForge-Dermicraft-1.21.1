package net.scruffy.dermicraft.effect;

import net.minecraft.world.effect.MobEffectCategory;

public class BloodLet extends MovementDebuffEffect {

    protected BloodLet() {
        super(MobEffectCategory.HARMFUL, 0xFF4500, "blood_let");
    }
}
