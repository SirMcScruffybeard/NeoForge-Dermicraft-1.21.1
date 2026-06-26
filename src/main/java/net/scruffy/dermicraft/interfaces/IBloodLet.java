package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.effect.ModEffects;
import net.scruffy.dermicraft.main.ModDamageTypes;

public interface IBloodLet {
    default boolean hasEffect(Player player, Holder<MobEffect> effect) {
        return player.hasEffect(effect);
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

    default void applyBloodLetDamage(Player player, float damage) {
        player.hurt(ModDamageTypes.getSource(player.level(), ModDamageTypes.BLOOD_LET), damage);
    }

    default void applyBloodLetEffect(Player player, int ticks, int amplifier) {
        player.addEffect(new MobEffectInstance(ModEffects.BLOOD_LET, ticks, amplifier));
    }

    default void damageTool(ItemStack stack, LivingEntity livingEntity, int damage) {
        EquipmentSlot slot = livingEntity.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(damage, livingEntity, slot);
    }

    default void playBloodLetSound(Level level, Player player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }

    default void playDefaultBloodLetSound(Level level, Player player) {
        playBloodLetSound(level, player, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 1.5f, 0.5f);
    }
}
