package net.scruffy.dermicraft.interfaces;


import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import javax.swing.plaf.basic.BasicComboBoxUI;

public interface IFleshBlockEntity {

    void drops();

    default boolean isServerSide(Level level) {
        return !level.isClientSide;
    }

    default void drops(Level level, IItemHandler inventory, BlockPos worldPosition) {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }

}
