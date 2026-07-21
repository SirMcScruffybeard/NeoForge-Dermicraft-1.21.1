package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.item.custom.base.ToolItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.util.FluidFoodUtil;

import java.util.List;

/**
 * Mobile fluid storage -- the standalone equipment origin later modified into the
 * suit-integrated Fuel Bladder / Feeder Bladder add-ons (see dermicraft-suit-notes.md).
 */
public class BladderItem extends ToolItem implements IHaveFluidData {

    public static final int CAPACITY = 2000;

    private final boolean drinkable;

    public BladderItem() {
        this(false);
    }

    /**
     * @param drinkable Feeder Bladder passes true -- lets it drink an edible fluid (see
     *                  FluidFoodUtil / dermicraft-liquid-foods-notes.md) instead of only bucket-style
     *                  pickup/place. Bladder/Fuel Bladder stay fuel-logistics-only.
     */
    public BladderItem(boolean drinkable) {
        super(new Item.Properties());
        this.drinkable = drinkable;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return drinkable ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return !stack.has(getDataType()) ? 16 : 1;
    }

    /**
     * Keyed off this item's own registry path (not hardcoded "bladder") so Fuel Bladder / Feeder
     * Bladder -- same class, separate registrations -- each get their own ".filled" translation.
     */
    @Override
    public Component getName(ItemStack stack) {
        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            String path = BuiltInRegistries.ITEM.getKey(this).getPath();
            return Component.translatable("item." + Dermicraft.MOD_ID + "." + path + ".filled", data.getFluidString());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (data.isFluidEmpty()) return;

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.dermicraft.liquid.amount", data.getFluidAmount()));
        } else {
            tooltip.add(Component.translatable("tooltip.dermicraft.hold_shift_for_amount"));
        }
    }

    /**
     * No block form (unlike Beaker) -- raytrace manually, same as BucketItem/BeakerItem, so this
     * also picks up from / pours into fluid-handler blocks (tanks, machines), not just world fluid.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);

        if (drinkable && FluidFoodUtil.canDrink(player, data.getFluidStack())) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        boolean pickingUp = data.isFluidEmpty();

        BlockHitResult hitResult = getPlayerPOVHitResult(level, player,
                pickingUp ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos pos = hitResult.getBlockPos();
        Direction face = hitResult.getDirection();

        if (!level.mayInteract(player, pos)) {
            return InteractionResultHolder.fail(stack);
        }

        FluidActionResult actionResult;
        if (pickingUp) {
            actionResult = FluidUtil.tryPickUpFluid(stack, player, level, pos, face);
        } else {
            BlockState state = level.getBlockState(pos);
            BlockPos placePos = state.canBeReplaced() ? pos : pos.relative(face);
            actionResult = FluidUtil.tryPlaceFluid(player, level, hand, placePos, stack, data.fluidStack());
        }

        if (!actionResult.isSuccess()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!player.isCreative()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, actionResult.getResult()));
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || level.isClientSide) return stack;

        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        FluidData result = FluidFoodUtil.drink(player, data);

        // Same creative-mode exemption as the bucket-style pickup/pour path above -- eating still
        // applies (harmless), but the bladder's own fluid isn't actually spent.
        if (!player.isCreative()) {
            stack.set(getDataType(), result);
        }

        return stack;
    }
}
