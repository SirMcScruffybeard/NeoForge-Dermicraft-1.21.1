package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.scruffy.dermicraft.component.BulkItemData;
import net.scruffy.dermicraft.component.BulkSlot;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Item-side twin of {@link IHaveFluidData}: shared plumbing for gadgets that carry a multi-slot
 * bulk item buffer inside their own {@link ItemStack} rather than in a placed block. Backed by
 * {@link BulkItemData} rather than vanilla's {@code DataComponents.CONTAINER}, since a per-slot
 * stack multiplier (Portable Mass Storage) can exceed the 99-count cap vanilla's container
 * component enforces -- see {@link BulkSlot} for why. Eater's slots just happen to never approach
 * that cap; sharing this store rather than a separate vanilla-backed one means a future Eater tier
 * that does scale past it needs no migration.
 */
public interface IHaveItemData {

    default DataComponentType<BulkItemData> getItemDataType() {
        return ModDataComponentTypes.BULK_ITEM_DATA.get();
    }

    /**
     * A data component belongs to the WHOLE {@link ItemStack} -- see
     * {@link IHaveFluidData#isSingleContainer} for the identical fluid-side reasoning. Any code
     * writing {@link BulkItemData} directly (rather than through a handler's insert/extract, which
     * already guards this at the call site) must check this first.
     */
    static boolean isSingleContainer(ItemStack stack) {
        return stack.getCount() <= 1;
    }

    /** Hands an item back to the player, dropping it at their feet if there's no room. See
     * {@link IHaveFluidData#giveOrDrop} for why this exact overload is used. */
    static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty() || player.getInventory().add(stack)) return;
        Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
    }

    /**
     * General-use item handler over a {@link BulkItemData} component: a fixed slot count, each
     * slot capped at {@code slotCapacity} (which may exceed a normal stack's 64/99 -- that's the
     * whole point of the backing store), and an optional filter narrowing what a slot accepts.
     *
     * <p>Disposal is not a separate handler class the way {@link IHaveFluidData.DisposalFluidHandler}
     * is for fluids: items have no mod-wide hazard-profile concept to gate against, so a "disposal
     * slot" is just a slot whose accepted items are voided by the caller after insertion succeeds,
     * or a filter that always returns false paired with the gadget discarding on insert -- left to
     * whatever gadget needs it (Portable Mass Storage's disposal slot) rather than baked in here.
     */
    class BulkItemHandler implements IItemHandlerModifiable {

        protected final ItemStack container;
        protected final DataComponentType<BulkItemData> dataType;
        protected final int slots;
        protected final int slotCapacity;
        protected final Predicate<ItemStack> filter;

        /** Defaults to {@link ModDataComponentTypes#BULK_ITEM_DATA} -- the original single-consumer
         * shape, kept for existing callers (Eater's own item buffer). A second bulk-data store on
         * the same stack (e.g. a Module loadout) must use the other constructor with its own
         * distinct {@link DataComponentType}, or the two would silently overwrite each other. */
        public BulkItemHandler(ItemStack container, int slots, int slotCapacity) {
            this(container, ModDataComponentTypes.BULK_ITEM_DATA.get(), slots, slotCapacity, stack -> true);
        }

        public BulkItemHandler(ItemStack container, int slots, int slotCapacity, Predicate<ItemStack> filter) {
            this(container, ModDataComponentTypes.BULK_ITEM_DATA.get(), slots, slotCapacity, filter);
        }

        public BulkItemHandler(ItemStack container, DataComponentType<BulkItemData> dataType,
                                int slots, int slotCapacity, Predicate<ItemStack> filter) {
            this.container = container;
            this.dataType = dataType;
            this.slots = slots;
            this.slotCapacity = slotCapacity;
            this.filter = filter;
        }

        private BulkItemData data() {
            return container.getOrDefault(dataType, BulkItemData.empty(slots)).withSize(slots);
        }

        private void writeData(BulkItemData data) {
            // See IHaveItemData#isSingleContainer -- writing into a stack of the gadget itself would
            // write the whole buffer into every copy in the stack.
            if (!isSingleContainer(container)) return;
            container.set(dataType, data);
        }

        @Override
        public int getSlots() {
            return slots;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return data().slot(slot).asDisplayStack();
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            writeData(data().withSlot(slot,
                    stack.isEmpty() ? BulkSlot.EMPTY : new BulkSlot(stack.copyWithCount(1), stack.getCount())));
        }

        @Override
        public int getSlotLimit(int slot) {
            return slotCapacity;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return filter.test(stack);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !filter.test(stack)) return stack;

            BulkSlot existing = data().slot(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing.template(), stack)) {
                return stack;
            }

            int room = slotCapacity - existing.count();
            if (room <= 0) return stack;

            int accepted = Math.min(room, stack.getCount());
            if (!simulate) {
                BulkSlot updated = existing.isEmpty()
                        ? new BulkSlot(stack.copyWithCount(1), accepted)
                        : existing.withCount(existing.count() + accepted);
                writeData(data().withSlot(slot, updated));
            }

            return accepted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) return ItemStack.EMPTY;

            BulkSlot existing = data().slot(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;

            int extracted = Math.min(amount, existing.count());
            ItemStack result = existing.template().copyWithCount(extracted);

            if (!simulate) {
                writeData(data().withSlot(slot, existing.withCount(existing.count() - extracted)));
            }

            return result;
        }
    }
}
