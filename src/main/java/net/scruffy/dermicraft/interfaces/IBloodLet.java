package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.effect.ModEffects;

public interface IBloodLet {

    int[] BLOOD_LET_DURATIONS = {50, 100, 150};

    default boolean hasEffect(Player player, Holder<MobEffect> effect) {
        return player.hasEffect(effect);
    }

    default void playBloodLetSound(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1.5f, 0.5f);
    }

    default void applyBloodLet(Player player) {
        int duration = BLOOD_LET_DURATIONS[player.getRandom().nextInt(BLOOD_LET_DURATIONS.length)];
        player.addEffect(new MobEffectInstance(ModEffects.BLOOD_LET, duration, 0));
        playBloodLetSound(player);
    }

    default boolean hasPoison(Player player) {
        return hasEffect(player, MobEffects.POISON);
    }

    default void removeEffect(Player player, Holder<MobEffect> effect) {
        player.removeEffect(effect);
    }

    default void removePoison(Player player) {
        removeEffect(player, MobEffects.POISON);
    }

    default void damageTool(ItemStack stack, LivingEntity livingEntity, int damage) {
        EquipmentSlot slot = livingEntity.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(damage, livingEntity, slot);
    }
}
