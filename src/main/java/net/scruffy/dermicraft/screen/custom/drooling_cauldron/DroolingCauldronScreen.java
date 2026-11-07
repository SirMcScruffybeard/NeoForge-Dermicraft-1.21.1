package net.scruffy.dermicraft.screen.custom.drooling_cauldron;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.screen.DroolingMachineScreen;

public class DroolingCauldronScreen extends DroolingMachineScreen<DroolingCauldronMenu> {

    public DroolingCauldronScreen(DroolingCauldronMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "drooling_cauldron");
    }
}
