package net.scruffy.dermicraft.screen.custom.workbench;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.function.IntSupplier;

/**
 * A single visible column of the Storage strip's scrolling window. Unlike {@code SlotItemHandler}
 * (whose backing index is fixed at construction), this slot's real index into the handler is
 * recomputed on every access from {@code rowSupplier.getAsInt() * COLUMNS + column} -- so the same
 * 9 on-screen Slot objects can be re-pointed at a different row just by the row changing, with no
 * slots added/removed and no extra client-server sync beyond what already keeps that row value
 * consistent (see {@code WorkbenchBlockEntity#scrollRow}, kept in sync the same way
 * {@code MrShepardBlockEntity}'s population cap is -- BE data sync, not a menu DataSlot).
 */
public class StorageStripSlot extends Slot {

    public static final int COLUMNS = 9;

    private static final SimpleContainer DUMMY = new SimpleContainer(0);

    private final IItemHandler handler;
    private final IntSupplier rowSupplier;
    private final int column;

    public StorageStripSlot(IItemHandler handler, IntSupplier rowSupplier, int column, int x, int y) {
        super(DUMMY, column, x, y);
        this.handler = handler;
        this.rowSupplier = rowSupplier;
        this.column = column;
    }

    private int index() {
        return rowSupplier.getAsInt() * COLUMNS + column;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return handler.isItemValid(index(), stack);
    }

    @Override
    public ItemStack getItem() {
        return handler.getStackInSlot(index());
    }

    @Override
    public void set(ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(index(), stack);
            setChanged();
        }
    }

    @Override
    public void setChanged() {
        // No-op target container (DUMMY) -- the real handler already notifies its own owner
        // (WorkbenchBlockEntity#onContentsChanged) whenever set()/remove() actually touch it.
    }

    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(index());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public boolean mayPickup(Player player) {
        return !handler.extractItem(index(), 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return handler.extractItem(index(), amount, false);
    }
}
