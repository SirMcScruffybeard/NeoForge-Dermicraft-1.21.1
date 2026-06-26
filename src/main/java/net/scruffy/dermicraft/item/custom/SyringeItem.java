package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.interfaces.IInject;
import net.scruffy.dermicraft.main.Dermicraft;

public class SyringeItem extends Item implements IInject, IHaveFluidData {

    public static final int CAPACITY = 100;

    public SyringeItem() {
        super(new Item.Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            //Return "Syringe with + ingredientFluid name
            return Component.translatable("item." + Dermicraft.MOD_ID + ".syringe.filled", data.getFluidString());
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!isServerSide(level)) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack stack = context.getItemInHand();

        if (isValidFluidHandler(getTargetFluidHandler(level, pos, face))) {
            draw(level, pos, face, stack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void draw(Level level, BlockPos pos, Direction face, ItemStack stack) {
        IFluidHandler handler = getTargetFluidHandler(level, pos, face);

        if (isValidFluidHandler(handler) && targetHasEnough(CAPACITY, handler)) {
            FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
            if (data.isFluidEmpty()) {
                stack.set(getFluidDataType(), FluidData.createData(handler.drain(CAPACITY, IFluidHandler.FluidAction.EXECUTE)));
            }
        }
    }
}
