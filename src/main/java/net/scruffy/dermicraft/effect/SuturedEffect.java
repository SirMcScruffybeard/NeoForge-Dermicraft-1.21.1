package net.scruffy.dermicraft.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.scruffy.dermicraft.main.ModDamageTypes;
import net.scruffy.dermicraft.util.ModPlayerUtil;

public class SuturedEffect extends MovementDebuffEffect {

    public SuturedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000, "sutured");
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof Player player) {

            if (player.isSprinting() || player.hurtTime > 0) {
                ruptureStitches(player);
                player.removeEffect(ModEffects.SUTURED);
                return false;
            }

            if (ModPlayerUtil.isFullHealth(player)) {
                player.removeEffect(ModEffects.SUTURED);
                return false;
            }

            if (player.tickCount % 10 == 0) {
                player.heal(.4f);
            }
        }
        return true;
    }

    private void ruptureStitches(Player player) {
        player.hurt(ModDamageTypes.getSource(player.level(), ModDamageTypes.RIP_STITCHES), 8f);

        playRipSound(player);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY(0.5), player.getZ(),
                    10, 0.1, 0.1, 0.1, 0.2);
        }
    }

    private void playRipSound(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1.5F, 0.5F);
    }
}
