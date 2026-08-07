package net.scruffy.dermicraft.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.interfaces.ISuture;
import net.scruffy.dermicraft.item.custom.base.ToolItem;
import net.scruffy.dermicraft.util.ModMath;

public class SutureKitItem extends ToolItem implements ISuture {

    public static final int IRON_DURABILITY = 10;
    public static final int PRIMITIVE_DURABILITY = 5;
    private static final int USE_WEAR = 1;

    /**
     * @param durability Shared by both the iron Suture Kit and the Primitive (String + Bone Meal)
     *                    alternate -- same behavior, just a different durability stat. Mirrors
     *                    ScalpelItem/ForcepsItem's parameterized-class convention.
     */
    public SutureKitItem(int durability) {
        super(new Item.Properties()
                .durability(durability)
        );
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
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
            useMaterials(context.getPlayer(), context.getHand());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
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
        useMaterials(player, player.getUsedItemHand());
        applySutureEffect(player, ModMath.Time.getSecondsToTicks(15), 0);
        playDefaultSutureSound(level, player);
    }

    @Override
    public void useMaterials(Player player, InteractionHand hand) {
        if (player.isCreative()) return;

        ItemStack stack = player.getItemInHand(hand);
        //Try to consume string in player inventory. If not damage Suture Kit
        if (!consumeString(player)) {
            stack.hurtAndBreak(USE_WEAR, player, LivingEntity.getSlotForHand(hand));
        }
    }


}
