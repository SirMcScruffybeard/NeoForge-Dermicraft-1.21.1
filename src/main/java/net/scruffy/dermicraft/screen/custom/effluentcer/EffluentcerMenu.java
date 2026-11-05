package net.scruffy.dermicraft.screen.custom.effluentcer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.EffluentcerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class EffluentcerMenu extends AbstractEvolvingMachineMenu<EffluentcerBlockEntity> {

    public EffluentcerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public EffluentcerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.EFFLUENTCER_MENU.get(), containerId, inv, blockEntity,
                EffluentcerBlockEntity.INVENTORY_SIZE, ModBlocks.EFFLUENTCER);

        this.BE.setInteractingPlayer(inv.player);

        // Coordinates follow the composited "inline pair" screen layout (see the machine
        // notes): HP far-left, fuel far-right, input A+B together left of center, result
        // beyond the arrow -- slot x tracks each tank column's blit x (+1), slot y fixed at 60.
        addMainSlot(BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60);
        addMainSlot(BE.getItemHandler(null), BE.getInputATank().SLOT, 41, 60);
        addMainSlot(BE.getItemHandler(null), BE.getInputBTank().SLOT, 61, 60);
        addMainSlot(BE.getItemHandler(null), BE.getResultTank().SLOT, 113, 60);
        addModuleTabSlots();

        // Every Main-tab slot here is a fluid-container passthrough (fuel/inputA/inputB/result) --
        // there's no real solid item input, so shift-click from the player's inventory has nothing
        // to target and just no-ops rather than dumping into a tank slot.
        setQuickMoveInputSlots(0, 0);

        restoreActiveTab();
    }
}
