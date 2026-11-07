package net.scruffy.dermicraft.screen.custom.render_furnace;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.RenderFurnaceBlockEntity;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class RenderFurnaceMenu extends AbstractEvolvingMachineMenu<RenderFurnaceBlockEntity> {

    public RenderFurnaceMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public RenderFurnaceMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.RENDER_FURNACE_MENU.get(), containerId, inv, blockEntity,
                RenderFurnaceBlockEntity.INVENTORY_SIZE, ModBlocks.RENDER_FURNACE);

        addMainSlot(BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60);
        addMainSlot(BE.getItemHandler(null), RenderFurnaceBlockEntity.INPUT_SLOT, 61, 35);
        addMainSlot(BE.getItemHandler(null), RenderFurnaceBlockEntity.OUTPUT_SLOT, 121, 35);
        addModuleTabSlots();

        setQuickMoveInputSlots(1, 1); // INPUT_SLOT only -- skip the fuel tank slot, OUTPUT_SLOT and Module slots

        restoreActiveTab();
    }
}
