package net.scruffy.dermicraft.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.main.ModDamageTypes;

public class SpicyRegretEffect extends MobEffect {

    public static final int DURATION_IN_SECONDS = 20;

    private static final double SPEED_MODIFIER = 2.0;
    private static final int SOUND_INTERVAL_TICKS = 20;

    public SpicyRegretEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);

        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "spicy_regret"),
                SPEED_MODIFIER,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.level().isClientSide()) {
            livingEntity.setSharedFlagOnFire(true);

            livingEntity.hurt(ModDamageTypes.getSource(livingEntity.level(), ModDamageTypes.SPICY_REGRET), 1f + amplifier);

            if (livingEntity.tickCount % SOUND_INTERVAL_TICKS == 0) {
                livingEntity.level().playSound(
                        null,
                        livingEntity.blockPosition(),
                        SoundEvents.LAVA_AMBIENT,
                        SoundSource.PLAYERS,
                        1.0f,
                        1.0f
                );
            }
        }

        return super.applyEffectTick(livingEntity, amplifier);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onMobRemoved(LivingEntity livingEntity, int amplifier, Entity.RemovalReason reason) {
        super.onMobRemoved(livingEntity, amplifier, reason);

        livingEntity.setSharedFlagOnFire(false);
    }
}
