package net.scruffy.dermicraft.screen.custom.craw;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.CrawBlockEntity;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * The Craw's menu on the shared evolution-machine base: the staging input and the storage slot on the Main tab, then the
 * shared Module tab slots. The Craw has no HP, so the base's health readouts report 0 and the screen hides the bar.
 */
public class CrawMenu extends AbstractEvolvingMachineMenu<CrawBlockEntity> {

    public CrawMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CrawMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenuTypes.CRAW_MENU.get(), containerId, inventory, blockEntity, 2, ModBlocks.CRAW);

        addMainSlot(BE.INPUT, CrawBlockEntity.INPUT_SLOT, 51, 35);
        addMainSlot(BE.INVENTORY, CrawBlockEntity.STORAGE_SLOT, 110, 35);
        addModuleTabSlots();

        setQuickMoveInputSlots(0, 1); // the staging input only -- storage drains from it on the next tick

        restoreActiveTab();
    }

    public int getStoredCount() {
        return BE.getStoredCount();
    }
}
