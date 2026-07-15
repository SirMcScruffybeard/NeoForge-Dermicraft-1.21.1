package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
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

import java.util.List;

/**
 * Mobile fluid storage -- the standalone equipment origin later modified into the
 * suit-integrated Fuel Bladder / Feeder Bladder add-ons (see dermicraft-suit-notes.md).
 */
public class BladderItem extends ToolItem implements IHaveFluidData {

    public static final int CAPACITY = 2000;

    public BladderItem() {
        super(new Item.Properties());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return !stack.has(getDataType()) ? 16 : 1;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            return Component.translatable("item." + Dermicraft.MOD_ID + ".bladder.filled", data.getFluidString());
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
}
