package net.scruffy.dermicraft.screen.custom.masticator;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class MasticatorMenu extends AbstractEvolvingMachineMenu<MasticatorBlockEntity> {

    public MasticatorMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MasticatorMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MASTICATOR_MENU.get(), containerId, inv, blockEntity,
                MasticatorBlockEntity.INVENTORY_SIZE, ModBlocks.MASTICATOR);

        addMainSlot(BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60);
        addMainSlot(BE.getItemHandler(null), BE.getIngredientTank().SLOT, 67, 60);
        addMainSlot(BE.getItemHandler(null), BE.getResultTank().SLOT, 123, 60);
        addMainSlot(BE.getItemHandler(null), MasticatorBlockEntity.INGREDIENT_ITEM_SLOT, 38, 35);
        addModuleTabSlots();

        setQuickMoveInputSlots(3, 1); // INGREDIENT_ITEM_SLOT only -- skip the tank-container slots and Module slots

        restoreActiveTab();
    }
}
