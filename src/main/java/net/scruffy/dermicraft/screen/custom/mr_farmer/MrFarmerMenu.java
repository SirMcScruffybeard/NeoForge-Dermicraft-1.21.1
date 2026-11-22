package net.scruffy.dermicraft.screen.custom.mr_farmer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MrFarmerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractFarmMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Mr. Farmer's menu: the shared farm menu's Home slots (the fuel container and the harvest buffer) and Field tab. */
public class MrFarmerMenu extends AbstractFarmMenu<MrFarmerBlockEntity> {

    public MrFarmerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MrFarmerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MR_FARMER_MENU.get(), containerId, inv, blockEntity,
                ((MrFarmerBlockEntity) blockEntity).getInventorySize());

        addFuelSlot();
        addBufferSlots();
        addInputSlots();
        addModuleTabSlots();
    }
}
