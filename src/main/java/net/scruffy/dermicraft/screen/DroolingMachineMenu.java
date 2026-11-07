package net.scruffy.dermicraft.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.entity.custom.DroolingMachineBlockEntity;

/**
 * The menu shared by the Drooling family (Cauldron, Crucible, Geode): on the Main tab the ingredient slot on the left
 * and the fluid-container slot on the right of the tank gauge, then the shared Module tab. A machine's own menu class
 * only names its menu type and block.
 *
 * @param <B> the machine's block entity
 */
public abstract class DroolingMachineMenu<B extends DroolingMachineBlockEntity<?>> extends AbstractEvolvingMachineMenu<B> {

    protected DroolingMachineMenu(MenuType<?> type, int containerId, Inventory inv, BlockEntity blockEntity,
                                  DeferredBlock<Block> block) {
        super(type, containerId, inv, blockEntity, DroolingMachineBlockEntity.INVENTORY_SIZE, block);

        addMainSlot(BE.INVENTORY, DroolingMachineBlockEntity.INPUT, 43, 34);
        addMainSlot(BE.INVENTORY, DroolingMachineBlockEntity.OUTPUT, 117, 34);
        addModuleTabSlots();

        setQuickMoveInputSlots(0, 1); // INPUT only -- skip the fluid-container slot and the Module slots

        restoreActiveTab();
    }
}
