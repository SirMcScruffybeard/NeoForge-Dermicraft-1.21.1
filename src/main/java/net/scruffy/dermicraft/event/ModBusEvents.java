package net.scruffy.dermicraft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.SyringeItem;
import net.scruffy.dermicraft.main.Dermicraft;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class ModBusEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

       // event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new SyringeItem.SyringeFluidHandler(stack), ModItems.SYRINGE);


    }

}
