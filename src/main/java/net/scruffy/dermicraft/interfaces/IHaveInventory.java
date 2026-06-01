package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

public interface IHaveInventory {

    default ItemStack insertItem(IItemHandler inventory, int slot, ItemStack stack) {
      return inventory.insertItem(slot, stack, false);
    }

    default ItemStack extractItem(IItemHandler inventory, int slot, int amount) {
        return inventory.extractItem(slot, amount, false);
    }

    void drops();

    default void dropItems(Level level, IItemHandler inventory, BlockPos worldPosition) {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

}
