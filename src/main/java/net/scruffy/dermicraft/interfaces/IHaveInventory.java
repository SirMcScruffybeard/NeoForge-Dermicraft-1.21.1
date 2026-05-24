package net.scruffy.dermicraft.interfaces;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public interface IHaveInventory {

    default ItemStack insertItem(IItemHandler inventory, int slot, ItemStack stack) {
      return inventory.insertItem(slot, stack, false);
    }

    default ItemStack extractItem(IItemHandler inventory, int slot, int amount) {
        return inventory.extractItem(slot, amount, false);
    }
}
