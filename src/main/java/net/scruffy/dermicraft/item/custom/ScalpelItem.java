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
import net.scruffy.dermicraft.interfaces.IBloodLet;
import net.scruffy.dermicraft.interfaces.IHarvestParts;
import net.scruffy.dermicraft.item.custom.base.ToolItem;
import org.jetbrains.annotations.NotNull;

public class ScalpelItem extends ToolItem implements IHarvestParts, IBloodLet {

    public static final int IRON_DURABILITY = 10;
    public static final int PRIMITIVE_DURABILITY = 5;
    private static final int USE_WEAR = 1;

    /**
     * @param durability Shared by both the iron Scalpel and the Primitive (Flint + Stick) alternate
     *                    -- same behavior, just a different durability stat. Mirrors the
     *                    BladderItem/MachineTier convention of parameterizing one class instead of
     *                    subclassing for stat-only variants.
     */
    public ScalpelItem(int durability) {
        super(new Item.Properties()
                .durability(durability)
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
            if (hasPoison(player)) {
                removePoison(player);
            }

            applyBloodLet(player);

            damageTool(stack, player, USE_WEAR);
        }

        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();

        if (isHarvestable(context.getLevel(), context.getClickedPos())) {
            damageTool(player, context.getItemInHand(), USE_WEAR, context.getHand());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }


}
