package net.scruffy.dermicraft.property;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.scruffy.dermicraft.util.ModMath;

public class ModFoodProperties {

    public static final FoodProperties DENSE_MUSCLE = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(.6f)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ModMath.Time.getSecondsToTicks(10)), 0.1f)
            .build();

    public static final FoodProperties EYE = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1f)
            .alwaysEdible()
            .fast()
            .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, ModMath.Time.getSecondsToTicks(15)), .75f)
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, ModMath.Time.getSecondsToTicks(10)), .25f)
            .build();

    public static final FoodProperties NERVE_CLUSTER = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.2f)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ModMath.Time.getSecondsToTicks(10)), .75f)
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, ModMath.Time.getSecondsToTicks(10)), .25f)
            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, ModMath.Time.getSecondsToTicks(10)), .20f)
            .build();


    public static final FoodProperties MRE = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.6f) //Final Saturation is calculated nutrition * saturationModifier * 2 -- on par with cooked chicken
            .build();

    public static final FoodProperties MEAT_FLAVORED_MEAT = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.5f) //Final Saturation is calculated nutrition * saturationModifier * 2
            .build();

}
