package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.block.entity.custom.BeakerBlockEntity;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.List;

public class BeakerItem extends BlockItem implements IHaveFluidData {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME;

    public BeakerItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return !stack.has(getDataType()) ? 16 : 1;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            return Component.translatable("item." + Dermicraft.MOD_ID + ".beaker.filled", data.getFluidString());
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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();

        if (player != null && player.isCrouching()) {
            return super.useOn(context);
        }

        return InteractionResult.PASS;
    }

    /**
     * Fluid source blocks have no collision, so the normal block-targeted interaction (useOn) never sees them.
     * Mirrors BucketItem's own use() override: raytrace manually, including fluids, to pick up / pour like a bucket.
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

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        // Snapshot the fluid before super.place(), which consumes (shrinks) the held
        // stack and would leave a count-1 filled beaker empty by the time we read it.
        FluidData data = context.getItemInHand().getOrDefault(getDataType(), FluidData.EMPTY);

        InteractionResult result = super.place(context);

        if (result.consumesAction() && isServerSide(context.getLevel()) && !data.isFluidEmpty()) {
            BlockEntity entity = context.getLevel().getBlockEntity(context.getClickedPos());

            if (entity instanceof BeakerBlockEntity beakerBlockEntity) {
                beakerBlockEntity.getTank(null).fill(data.fluidStack().copy(), IFluidHandler.FluidAction.EXECUTE);
            }
        }

        return result;
    }
}
