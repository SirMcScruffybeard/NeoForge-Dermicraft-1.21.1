package net.scruffy.dermicraft.screen.custom.drooling_cauldron;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.DroolingCauldronBlockEntity;
import net.scruffy.dermicraft.screen.DroolingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class DroolingCauldronMenu extends DroolingMachineMenu<DroolingCauldronBlockEntity> {

    public DroolingCauldronMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public DroolingCauldronMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.DROOLING_CAULDRON_MENU.get(), containerId, inv, blockEntity, ModBlocks.DROOLING_CAULDRON);
    }
}
