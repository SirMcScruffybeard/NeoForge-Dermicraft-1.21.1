package net.scruffy.dermicraft.screen.custom.mr_farmer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.screen.AbstractFarmScreen;

/** Mr. Farmer's screen: everything is the shared farm screen's Home and Field tabs. */
public class MrFarmerScreen extends AbstractFarmScreen<MrFarmerMenu> {

    public MrFarmerScreen(MrFarmerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
