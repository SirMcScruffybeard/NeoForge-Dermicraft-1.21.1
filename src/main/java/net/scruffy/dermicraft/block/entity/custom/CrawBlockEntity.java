package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IPreserveContentsOnPickup;
import net.scruffy.dermicraft.screen.custom.craw.CrawMenu;
import org.jetbrains.annotations.Nullable;

public class CrawBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory, IPreserveContentsOnPickup {

    public static final int STACKS = 10;
    public static final int CAPACITY = 64 * STACKS; // 640 items of one type

    public static final int STORAGE_SLOT = 0;
    public static final int INPUT_SLOT = 0; // index within the separate INPUT handler

    // Single locked storage slot. Locks to whichever item is first inserted and
    // unlocks once it empties -- the item-side twin of SkinTank's fluid-type lock.
    public final ItemStackHandler INVENTORY = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return CAPACITY;
        }

        // ItemStackHandler.getStackLimit() normally clamps to the item's max stack size (64),
        // which would cap bulk storage at one stack -- override it so the slot honours CAPACITY.
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            ItemStack stored = getStackInSlot(slot);
            return stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide) {
                setChanged();
                updateBlock();
            }
        }
    };

    // GUI-only staging slot: whatever's dropped in here drains into storage on the next tick,
    // so players can shift-click/drag a whole stack in without the vanilla 64-per-click cap
    // applying to the storage slot itself. Doesn't affect the block's direct drawer interaction.
    //
    // The drain happens on tick rather than synchronously in onContentsChanged: vanilla's
    // SlotItemHandler.getMaxStackSize(ItemStack) probes a slot's capacity by calling
    // setStackInSlot(EMPTY) then setStackInSlot(currentStack) to simulate-and-restore -- both
    // calls fire onContentsChanged. Reacting to those with a real (non-simulated) transfer
    // corrupted state whenever storage already held a compatible stack, since the "restore"
    // call re-triggered a genuine insert as a side effect of what should be a read-only probe.
    public final ItemStackHandler INPUT = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide) {
                setChanged();
                updateBlock();
            }
        }
    };

    public void tick(Level level) {
        if (!level.isClientSide) {
            transferInputToStorage();
        }
    }

    private void transferInputToStorage() {
        ItemStack input = INPUT.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        ItemStack remainder = INVENTORY.insertItem(STORAGE_SLOT, input, false);
        // Only write back if something actually moved -- prevents re-triggering this same
        // listener when storage is full/type-mismatched and the remainder is unchanged.
        if (remainder.getCount() != input.getCount()) {
            INPUT.setStackInSlot(INPUT_SLOT, remainder);
        }
    }

    public CrawBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CRAW_BE.get(), pos, blockState);
    }

    public IItemHandler getItemHandler(@Nullable Direction face) {
        return INVENTORY; // any-face access -- ignore the face
    }

    public ItemStack getStoredStack() {
        return INVENTORY.getStackInSlot(STORAGE_SLOT);
    }

    public int getStoredCount() {
        return getStoredStack().getCount();
    }

    /**
     * Deposit from a held stack. Regular use inserts one item; crouch use inserts the
     * whole held stack (as much as fits). Shrinks and returns the held stack.
     */
    public ItemStack deposit(ItemStack held, boolean wholeStack) {
        if (held.isEmpty()) {
            return held;
        }
        int amount = wholeStack ? held.getCount() : 1;
        ItemStack toInsert = held.copyWithCount(amount);
        ItemStack remainder = INVENTORY.insertItem(STORAGE_SLOT, toInsert, false);
        int inserted = amount - remainder.getCount();
        held.shrink(inserted);
        return held;
    }

    /**
     * Withdraw from storage. Regular use pulls one item; crouch use pulls a full stack
     * (up to the item's max stack size, or the remainder if less).
     */
    public ItemStack withdraw(boolean fullStack) {
        ItemStack stored = getStoredStack();
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int amount = fullStack ? stored.getMaxStackSize() : 1;
        return INVENTORY.extractItem(STORAGE_SLOT, amount, false);
    }

    @Override
    public void drops() {
        if (level == null) {
            return;
        }
        // Storage can hold more than a vanilla stack; split into <=maxStackSize drops so no single
        // ItemEntity carries an over-99 count (which vanilla's item NBT codec rejects on save).
        ItemStack stored = getStoredStack();
        while (!stored.isEmpty()) {
            ItemStack chunk = stored.split(Math.min(stored.getCount(), stored.getMaxStackSize()));
            Containers.dropItemStack(level,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), chunk);
        }
        INVENTORY.setStackInSlot(STORAGE_SLOT, ItemStack.EMPTY);

        dropItems(level, INPUT, worldPosition); // input never exceeds one stack, safe as-is
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CRAW);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CrawMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // The storage slot can hold up to 640 of one item, but ItemStack's data codec caps a
        // stack's saved count at 99. So persist the item type from a single-count copy (which the
        // codec accepts) and store the real bulk amount separately as a plain int.
        ItemStack stored = getStoredStack();
        if (!stored.isEmpty()) {
            CompoundTag storageTag = new CompoundTag();
            storageTag.put("item", stored.copyWithCount(1).save(registries));
            storageTag.putInt("count", stored.getCount());
            tag.put("craw_storage", storageTag);
        }
        tag.put("craw_input_inv", INPUT.serializeNBT(registries));
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ItemStack stored = ItemStack.EMPTY;
        if (tag.contains("craw_storage")) {
            CompoundTag storageTag = tag.getCompound("craw_storage");
            stored = ItemStack.parse(registries, storageTag.getCompound("item")).orElse(ItemStack.EMPTY);
            if (!stored.isEmpty()) {
                stored.setCount(storageTag.getInt("count"));
            }
        }
        INVENTORY.setStackInSlot(STORAGE_SLOT, stored);
        INPUT.deserializeNBT(registries, tag.getCompound("craw_input_inv"));
    }
}
