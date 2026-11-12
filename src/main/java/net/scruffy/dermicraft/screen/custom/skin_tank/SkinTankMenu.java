package net.scruffy.dermicraft.screen.custom.skin_tank;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * The Skin Tank's menu on the shared evolution-machine base: its two container slots on the Main tab either side of the
 * gauge, then the shared Module tab slots. The tank has no HP, so the base's health readouts report 0 and the screen
 * hides the bar.
 */
public class SkinTankMenu extends AbstractEvolvingMachineMenu<SkinTankBlockEntity> {

    public SkinTankMenu(int containerId, Inventory inventory, FriendlyByteBuf extendData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extendData.readBlockPos()));
    }

    public SkinTankMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenuTypes.SKIN_TANK_MENU.get(), containerId, inventory, blockEntity,
                SkinTankBlockEntity.INVENTORY_SIZE, ModBlocks.SKIN_TANK);

        addMainSlot(BE.INVENTORY, SkinTankBlockEntity.INPUT, 43, 34);
        addMainSlot(BE.INVENTORY, SkinTankBlockEntity.OUTPUT, 117, 34);
        addModuleTabSlots();

        setQuickMoveInputSlots(0, 1); // INPUT only -- skip OUTPUT and the Module slot(s)

        restoreActiveTab();
    }
}
