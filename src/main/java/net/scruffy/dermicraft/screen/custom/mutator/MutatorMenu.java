package net.scruffy.dermicraft.screen.custom.mutator;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MutatorBlockEntity;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class MutatorMenu extends AbstractEvolvingMachineMenu<MutatorBlockEntity> {

    public MutatorMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MutatorMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MUTATOR_MENU.get(), containerId, inv, blockEntity,
                MutatorBlockEntity.INVENTORY_SIZE, ModBlocks.MUTATOR);

        addMainSlot(BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60);
        addMainSlot(BE.getItemHandler(null), BE.getReagentTank().SLOT, 31, 60);
        addMainSlot(BE.getItemHandler(null), MutatorBlockEntity.INPUT_SLOT, 61, 35);
        addMainSlot(BE.getItemHandler(null), MutatorBlockEntity.OUTPUT_SLOT, 121, 35);
        addModuleTabSlots();

        setQuickMoveInputSlots(2, 1); // INPUT_SLOT only -- skip the tank-container slots, OUTPUT_SLOT and Module slots

        restoreActiveTab();
    }

    /** In Fill mode there is no craft to measure, so the progress arrow never fills (see MutatorBlockEntity#tickFill). */
    @Override
    public boolean isCrafting() {
        return BE.getMode() == MutatorBlockEntity.Mode.MUTATE && super.isCrafting();
    }

    public MutatorBlockEntity.Mode getMode() {
        return BE.getMode();
    }
}
