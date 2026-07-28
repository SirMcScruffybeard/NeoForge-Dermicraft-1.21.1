package net.scruffy.dermicraft.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/**
 * A view over a contiguous slot range of a backing handler, with fixed insert/extract permissions.
 *
 * <p>Exists for {@link net.scruffy.dermicraft.interfaces.IHasChannels} channels: a machine keeps one
 * flat {@code ItemStackHandler} for all its slots, but a Gate channel needs to address just the
 * slots belonging to that channel, with that channel's own direction baked in (a feed channel
 * accepts pushes but must never be drained; an output buffer is the reverse). Masticator solves the
 * single-slot case with an inline anonymous handler; this generalises it to ranges so the farming
 * machines' multi-slot feed/buffer rows don't each need their own copy.
 *
 * <p>Slot indices are channel-local: slot 0 here is {@code firstSlot} on the backing handler.
 */
public class SlotRangeItemHandler implements IItemHandlerModifiable {

    private final IItemHandlerModifiable backing;
    private final int firstSlot;
    private final int slotCount;
    private final boolean allowInsert;
    private final boolean allowExtract;

    public SlotRangeItemHandler(IItemHandlerModifiable backing, int firstSlot, int slotCount,
                                boolean allowInsert, boolean allowExtract) {
        this.backing = backing;
        this.firstSlot = firstSlot;
        this.slotCount = slotCount;
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
    }

    private boolean inRange(int slot) {
        return slot >= 0 && slot < slotCount;
    }

    @Override
    public int getSlots() {
        return slotCount;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return inRange(slot) ? backing.getStackInSlot(firstSlot + slot) : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (inRange(slot)) backing.setStackInSlot(firstSlot + slot, stack);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!allowInsert || !inRange(slot)) return stack;
        return backing.insertItem(firstSlot + slot, stack, simulate);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!allowExtract || !inRange(slot)) return ItemStack.EMPTY;
        return backing.extractItem(firstSlot + slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inRange(slot) ? backing.getSlotLimit(firstSlot + slot) : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return allowInsert && inRange(slot) && backing.isItemValid(firstSlot + slot, stack);
    }
}
