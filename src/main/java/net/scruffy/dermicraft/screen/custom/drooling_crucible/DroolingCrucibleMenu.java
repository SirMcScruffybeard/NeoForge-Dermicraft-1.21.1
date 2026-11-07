package net.scruffy.dermicraft.screen.custom.drooling_crucible;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.DroolingCrucibleBlockEntity;
import net.scruffy.dermicraft.screen.DroolingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class DroolingCrucibleMenu extends DroolingMachineMenu<DroolingCrucibleBlockEntity> {

    public DroolingCrucibleMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public DroolingCrucibleMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.DROOLING_CRUCIBLE_MENU.get(), containerId, inv, blockEntity, ModBlocks.DROOLING_CRUCIBLE);
    }
}
