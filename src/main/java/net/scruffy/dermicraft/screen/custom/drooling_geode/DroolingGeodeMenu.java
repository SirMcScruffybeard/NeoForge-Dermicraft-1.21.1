package net.scruffy.dermicraft.screen.custom.drooling_geode;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.DroolingGeodeBlockEntity;
import net.scruffy.dermicraft.screen.DroolingMachineMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class DroolingGeodeMenu extends DroolingMachineMenu<DroolingGeodeBlockEntity> {

    public DroolingGeodeMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public DroolingGeodeMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.DROOLING_GEODE_MENU.get(), containerId, inv, blockEntity, ModBlocks.DROOLING_GEODE);
    }
}
