package net.scruffy.dermicraft.screen.custom.workbench;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.item.custom.SunderChainItem;
import net.scruffy.dermicraft.item.custom.SunderItem;

import java.util.function.BooleanSupplier;

/**
 * Sunder's chain slot on the Mod page -- unlike {@code ScrenchMenu}'s equivalent (which
 * materializes the mounted chain out on menu open and reinstalls it on close, since that menu's
 * lifecycle is scoped to a single Sunder), this is a <b>live view</b> straight into whichever
 * item currently sits in the Mod page's working-item slot: reads/writes
 * {@code SunderItem.mountedChain} directly on every access, no open/close synchronization needed.
 * Avoids the edge case ScrenchMenu doesn't have to handle -- the working item can change (or be
 * removed) while this same persistent menu stays open.
 */
public class SunderChainSlot extends Slot {

    private static final SimpleContainer DUMMY = new SimpleContainer(0);

    private final IItemHandler workItem;
    private final BooleanSupplier active;

    public SunderChainSlot(IItemHandler workItem, int x, int y, BooleanSupplier active) {
        super(DUMMY, 0, x, y);
        this.workItem = workItem;
        this.active = active;
    }

    @Override
    public boolean isActive() {
        // Page-selected AND a Sunder is actually the one being serviced -- the earlier fix only
        // covered the first half, so this slot (and its hover-highlight) stayed live even with an
        // empty/non-Sunder work item, matching the same "home state has no sub-panel" rule the
        // background art already follows.
        return active.getAsBoolean() && workItemIsSunder();
    }

    private ItemStack workStack() {
        return workItem.getStackInSlot(0);
    }

    private boolean workItemIsSunder() {
        return workStack().getItem() instanceof SunderItem;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return workItemIsSunder() && stack.getItem() instanceof SunderChainItem;
    }

    @Override
    public ItemStack getItem() {
        if (!workItemIsSunder()) return ItemStack.EMPTY;
        return SunderItem.mountedChain(workStack()).itemStack();
    }

    @Override
    public void set(ItemStack stack) {
        if (!workItemIsSunder()) return;
        if (stack.isEmpty()) {
            SunderItem.clearMountedChain(workStack());
        } else {
            SunderItem.setMountedChain(workStack(), stack);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        // No-op target container (DUMMY) -- the real mutation already happened on the work item's
        // own ItemStack instance, held live by WorkbenchBlockEntity#WORK_ITEM.
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPickup(Player player) {
        return workItemIsSunder() && SunderItem.hasMountedChain(workStack());
    }

    @Override
    public ItemStack remove(int amount) {
        if (!workItemIsSunder()) return ItemStack.EMPTY;
        ItemStack chain = SunderItem.mountedChain(workStack()).itemStack();
        if (!chain.isEmpty()) {
            SunderItem.clearMountedChain(workStack());
        }
        return chain;
    }
}
