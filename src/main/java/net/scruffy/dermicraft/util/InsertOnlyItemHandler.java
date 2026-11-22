package net.scruffy.dermicraft.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A view of an item handler that can only be filled: reads and inserts go through to the real handler, but nothing can be
 * taken out. For a machine's input row as pipes and the Gate see it (they push seeds or feed in, never pull them back).
 * Unlike {@link SlotRangeItemHandler} it follows the backing handler's slot count live, so it stays right when the
 * handler is resized (a tier changes the number of slots).
 */
public final class InsertOnlyItemHandler implements IItemHandler {

    private final IItemHandler delegate;

    public InsertOnlyItemHandler(IItemHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return delegate.insertItem(slot, stack, simulate);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return delegate.isItemValid(slot, stack);
    }
}
