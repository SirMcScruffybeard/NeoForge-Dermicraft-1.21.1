package net.scruffy.dermicraft.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.interfaces.ISuture;
import net.scruffy.dermicraft.item.custom.base.ToolItem;
import net.scruffy.dermicraft.util.ModMath;

public class SutureKitItem extends ToolItem implements ISuture {
    public SutureKitItem(Properties properties) {
        super(properties
                .durability(100));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 45;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (isSutable(level.getBlockState(context.getClickedPos()))) {

            useMaterials(context.getPlayer());
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!isFullHealth(player)) {
            player.startUsingItem(usedHand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if(level.isClientSide) return stack;

        if (livingEntity instanceof Player player) {
            suturePlayer(level, player, stack);
        }
        return stack;
    }

    @Override
    public void suturePlayer(Level level, Player player, ItemStack stack) {
        useMaterials(player);
        applySutureEffect(player, ModMath.time.getSecondsToTicks(15), 0);
        playDefaultSutureSound(level, player);
    }

    @Override
    public void useMaterials(Player player) {
        ItemStack stack = player.getItemInHand(player.getUsedItemHand());
        //Try to consume string in player inventory. If not damage Suture Kit
        if (!consumeString(player)) {
            stack.hurtAndBreak(20, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
    }


}
