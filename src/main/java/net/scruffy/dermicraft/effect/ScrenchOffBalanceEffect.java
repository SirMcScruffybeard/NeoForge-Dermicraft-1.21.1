package net.scruffy.dermicraft.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Scrench's completed-chain-swap penalty -- "off-balance from wrenching while walking," per the
 * design notes, deliberately built as its own raw attribute modifier rather than reused vanilla
 * Slowness. Applied with {@code showParticles}/{@code showIcon} both false (see
 * {@code ScrenchMenu#applyCompletedSwapCosts}) so it never shows a status icon or potion swirl --
 * a magic-debuff look is exactly what the design notes rule out.
 *
 * <p>Movement-speed-only, unlike {@link MovementDebuffEffect} (which also nerfs jump strength) --
 * the design notes only call for a speed penalty, so this extends {@code MobEffect} directly
 * rather than inheriting a jump penalty nobody asked for.
 *
 * <p>Magnitude and duration are both open design questions (see the Scrench design notes) --
 * placeholders, not tuned.
 */
public class ScrenchOffBalanceEffect extends MobEffect {

    private static final double SPEED_MODIFIER = -0.2;

    public ScrenchOffBalanceEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B7355);

        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "scrench_off_balance"),
                SPEED_MODIFIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
