package net.scruffy.dermicraft.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.scruffy.dermicraft.main.Dermicraft;

public abstract class MovementDebuffEffect extends MobEffect {

    protected MovementDebuffEffect(MobEffectCategory category, int color, String namePrefix) {
        super(category, color);

        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, namePrefix + "_slowness"),
                -.03, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        addAttributeModifier(Attributes.JUMP_STRENGTH,
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, namePrefix + "_jump_penalty"),
                -.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
