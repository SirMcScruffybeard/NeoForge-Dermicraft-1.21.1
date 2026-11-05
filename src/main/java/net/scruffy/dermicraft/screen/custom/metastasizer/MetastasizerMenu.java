package net.scruffy.dermicraft.screen.custom.metastasizer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MetastasizerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class MetastasizerMenu extends AbstractEvolvingMachineMenu<MetastasizerBlockEntity> {

    public MetastasizerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MetastasizerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.METASTASIZER_MENU.get(), containerId, inv, blockEntity,
                MetastasizerBlockEntity.INVENTORY_SIZE, ModBlocks.METASTASIZER);

        addMainSlot(BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60);
        addMainSlot(BE.getItemHandler(null), BE.getReagentTank().SLOT, 31, 60);
        addMainSlot(BE.getItemHandler(null), MetastasizerBlockEntity.PATTERN_SLOT, 61, 35);
        addMainSlot(BE.getItemHandler(null), MetastasizerBlockEntity.OUTPUT_SLOT, 121, 35);
        addModuleTabSlots();

        setQuickMoveInputSlots(2, 1); // PATTERN_SLOT only -- skip the tank-container slots, OUTPUT_SLOT and Module slots

        restoreActiveTab();
    }
}
