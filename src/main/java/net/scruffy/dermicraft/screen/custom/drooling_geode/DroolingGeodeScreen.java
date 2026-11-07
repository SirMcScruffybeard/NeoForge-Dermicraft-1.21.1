package net.scruffy.dermicraft.screen.custom.drooling_geode;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.screen.DroolingMachineScreen;

public class DroolingGeodeScreen extends DroolingMachineScreen<DroolingGeodeMenu> {

    public DroolingGeodeScreen(DroolingGeodeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "drooling_geode");
    }
}
