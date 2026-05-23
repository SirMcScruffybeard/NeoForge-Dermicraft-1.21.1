package net.scruffy.dermicraft.interfaces;


import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

public interface IFleshBlockEntity {

    void drops();

    default boolean isServerSide(Level level) {
        return !level.isClientSide;
    }

    default void dropItems(Level level, IItemHandler inventory, BlockPos worldPosition) {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inv);
    }



    default int getScaledProgress(int progress, int maxProgress, int scale) {
        if (maxProgress <= 0 || progress <= 0) {
            return 0;
        }
        return (progress * scale) / maxProgress;
    }

    default boolean isStillCrafting(int progress) {
        return  progress > 0;
    }

}
