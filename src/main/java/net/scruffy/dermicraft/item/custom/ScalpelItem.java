package net.scruffy.dermicraft.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.interfaces.IBloodLet;
import net.scruffy.dermicraft.interfaces.IHarvestParts;
import org.jetbrains.annotations.NotNull;

public class ScalpelItem extends Item implements IHarvestParts, IBloodLet {

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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {

        player.startUsingItem(usedHand);

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {

        if (level.isClientSide) return stack;

        if (livingEntity instanceof Player player) {
            if (hasPosion(player)) {
                removePoison(player);
            }
            applyBloodLetDamage(player, 2f);

            applyBloodLetEffect(player, 100, 0);

            playDefaultBloodLetSound(level, player);

            damageTool(stack, player, getTotalWear());
        }

        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (isHarvestable(context.getLevel(), context.getClickedPos())) {
            damageTool(player, context.getItemInHand(), getTotalWear());
        }

        return InteractionResult.SUCCESS;
    }

    private int getTotalWear() {
        return (int) (this.getMaxDamage(new ItemStack(this)) * USE_DAMAGE_MULTIPLIER);

    }
}
