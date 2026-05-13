package net.scruffy.dermicraft.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.effect.ModEffects;
import net.scruffy.dermicraft.interfaces.IHarvestParts;
import net.scruffy.dermicraft.main.ModDamageTypes;
import org.jetbrains.annotations.NotNull;

public class ScalpelItem extends Item implements IHarvestParts {

    private static final int DEFAULT_DURABILITY = 100;
    private static final float USE_DAMAGE_MULTIPLIER = .1f;


    public ScalpelItem(Properties properties) {
        super(properties
                .durability(DEFAULT_DURABILITY)
        );
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    @Override
    @NotNull
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (isHarvestable(context.getLevel(), context.getClickedPos())) {
            context.getItemInHand().hurtAndBreak(getUseDurabilityDamage(), player, EquipmentSlot.MAINHAND);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if(!level.isClientSide) return InteractionResultHolder.success(stack);

        player.startUsingItem(usedHand);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if(level.isClientSide) return stack;

        if (livingEntity instanceof Player player) {
            if (player.isCrouching()) {
                if (player.hasEffect(MobEffects.POISON)) {
                    player.removeEffect(MobEffects.POISON);
                }
                player.hurt(ModDamageTypes.getSource(player.level(), ModDamageTypes.BLOOD_LET), 2f);

                player.addEffect(new MobEffectInstance(ModEffects.BLOOD_LET,100, 0));

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1.5F, 0.5F);
            }

            stack.hurtAndBreak(getUseDurabilityDamage(), player, EquipmentSlot.MAINHAND);
        }

        return stack;
    }

    private int getUseDurabilityDamage() {
        return (int) (this.getMaxDamage(new ItemStack(this)) * USE_DAMAGE_MULTIPLIER);

    }
}
