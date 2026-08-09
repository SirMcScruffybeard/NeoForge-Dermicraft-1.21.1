package net.scruffy.dermicraft.screen.custom.workbench;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.item.custom.SunderItem;

import java.util.function.BooleanSupplier;

/**
 * Hand-fill path for Sunder's onboard tank on the Mod page -- drop a filled container in, transfer
 * happens immediately on contact, same "immediate, like everything else" fluid-transfer rule
 * {@code ScrenchMenu.FuelFillSlot} already follows. This is the manual/carried-fuel path; the
 * "Fill from pool" button is the other, station-only path -- see WorkbenchScreen.
 *
 * <p>Backed by its own tiny 1-slot container (the held container passes through here, it isn't
 * fuel itself), same shape as {@code ScrenchMenu.FuelFillSlot}. Reads the *current* work-item
 * stack on every transfer attempt rather than one captured at construction, so it stays correct if
 * the working item changes while the menu is open.
 */
public class SunderFuelFillSlot extends Slot {

    private final IItemHandler workItem;
    private final BooleanSupplier active;

    public SunderFuelFillSlot(IItemHandler workItem, SimpleContainer fillContainer, int x, int y, BooleanSupplier active) {
        super(fillContainer, 0, x, y);
        this.workItem = workItem;
        this.active = active;
    }

    @Override
    public boolean isActive() {
        // Page-selected AND a Sunder is actually the one being serviced -- same rule as
        // SunderChainSlot; otherwise this stayed live (and highlightable) with an empty/non-Sunder
        // work item, matching the same "home state has no sub-panel" rule the background art
        // already follows.
        return active.getAsBoolean() && workItem.getStackInSlot(0).getItem() instanceof SunderItem;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getCapability(Capabilities.FluidHandler.ITEM, null) != null;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        ItemStack held = getItem();
        if (held.isEmpty()) return;

        ItemStack workStack = workItem.getStackInSlot(0);
        if (!(workStack.getItem() instanceof SunderItem)) return;

        IFluidHandlerItem sunderTank = workStack.getCapability(Capabilities.FluidHandler.ITEM, null);
        IFluidHandlerItem containerHandler = held.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (sunderTank == null || containerHandler == null) return;

        if (FluidUtil.tryFluidTransfer(sunderTank, containerHandler, Integer.MAX_VALUE, true).isEmpty()) return;
        set(containerHandler.getContainer());
    }
}
