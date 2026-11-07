package net.scruffy.dermicraft.screen.custom.render_kiln;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.RenderKilnBlockEntity;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class RenderKilnMenu extends AbstractEvolvingMachineMenu<RenderKilnBlockEntity> {

    public RenderKilnMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public RenderKilnMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.RENDER_KILN_MENU.get(), containerId, inv, blockEntity,
                RenderKilnBlockEntity.INVENTORY_SIZE, ModBlocks.RENDER_KILN);

        addMainSlot(BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60);
        addMainSlot(BE.getItemHandler(null), BE.getInputTank().SLOT, 55, 60);
        addMainSlot(BE.getItemHandler(null), RenderKilnBlockEntity.OUTPUT_SLOT, 101, 35);
        addModuleTabSlots();

        // Input here is fluid-only (BE.getInputTank(), a bucket passthrough slot) -- no real solid
        // item input exists, so shift-click from the player's inventory has nothing to target.
        setQuickMoveInputSlots(0, 0);

        restoreActiveTab();
    }
}
