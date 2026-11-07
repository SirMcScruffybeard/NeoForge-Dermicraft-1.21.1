package net.scruffy.dermicraft.screen.custom.drooling_crucible;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.screen.DroolingMachineScreen;

public class DroolingCrucibleScreen extends DroolingMachineScreen<DroolingCrucibleMenu> {

    public DroolingCrucibleScreen(DroolingCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "drooling_crucible");
    }
}
