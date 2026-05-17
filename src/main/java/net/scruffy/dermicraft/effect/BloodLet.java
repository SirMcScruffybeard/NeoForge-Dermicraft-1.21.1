package net.scruffy.dermicraft.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.scruffy.dermicraft.main.Dermicraft;

public class BloodLet extends MobEffect {

    protected BloodLet() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);

        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "blood_let_slowness"),
                -.03, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        addAttributeModifier(Attributes.JUMP_STRENGTH,
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "blood_let_jump_penalty"),
                -.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
