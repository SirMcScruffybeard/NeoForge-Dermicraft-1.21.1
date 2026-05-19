package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.interfaces.IInject;
import net.scruffy.dermicraft.item.custom.base.ToolItem;
import net.scruffy.dermicraft.main.Dermicraft;

public class SyringeItem extends ToolItem implements IInject {

    public static final int CAPACITY = 100;

    public SyringeItem() {
        super(new Item.Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            //Return "Syringe with + fluid name
            return Component.translatable("item." + Dermicraft.MOD_ID + ".syringe.filled", data.getFluidString());
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Player player = context.getPlayer();

        if (isServerSide(level)) {
            if (isValidFluidHandler(getTargetFluidHandler(level, pos, face))) {
                draw(level, pos, face, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private void draw(Level level, BlockPos pos, Direction face, Player player) {
        IFluidHandler handler = getTargetFluidHandler(level, pos, face);

        if (isServerSide(level)) {
            if (isValidFluidHandler(handler)) {
                if (targetHasEnough(CAPACITY, handler)) {
                    ItemStack stack = player.getItemInHand(player.getUsedItemHand());
                    FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
                    if (data.isFluidEmpty()) {
                        stack.set(getDataType(), FluidData.createData(handler.drain(CAPACITY, IFluidHandler.FluidAction.EXECUTE)));
                    }
                }
            }
        }
    }

    private void inject(UseOnContext context) {
        Level level = context.getLevel();

        if (!level.isClientSide) {

        }
    }
    /*

   Inject
    get target block
    check valid block
    inject
    drain syringe
     */

    //Currently not in use. Keeping for example and in case I decide to use again
    public static class SyringeFluidHandler implements IFluidHandlerItem {

        protected ItemStack container;

        public SyringeFluidHandler(ItemStack stack) {
            container = stack;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidData data = container.getOrDefault(getDataType(), FluidData.EMPTY);
            return data.getFluidStack();
        }

        @Override
        public int getTankCapacity(int i) {
            return SyringeItem.CAPACITY;
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluidStack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!getFluidInTank(0).isEmpty() || resource.isEmpty() || resource.getAmount() < getTankCapacity(0)) {
                return 0;
            }

            if (action.execute()) {
                container.set(getDataType(), FluidData.createData(resource.copy()));
            }
            return SyringeItem.CAPACITY;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.is(getFluidInTank(0).getFluid())) {
                return drain(resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack contained = getFluidInTank(0);
            if (contained.isEmpty() || maxDrain < 250) return FluidStack.EMPTY;

            if (action.execute()) {
                container.set(getDataType(), FluidData.EMPTY);
            }
            return contained;
        }

        private DataComponentType<FluidData> getDataType() {
            return ModDataComponentTypes.FLUID_DATA.get();
        }
    }
}
