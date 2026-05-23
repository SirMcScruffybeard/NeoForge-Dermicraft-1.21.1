package net.scruffy.dermicraft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.DroolingCauldronBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class ModBusEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

       // event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new SyringeItem.SyringeFluidHandler(stack), ModItems.SYRINGE);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.SKIN_TANK_BE.get(), SkinTankBlockEntity::getTank);

       event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.DROOLING_CAULDRON_BE.get(), DroolingCauldronBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.DROOLING_CAULDRON_BE.get(), DroolingCauldronBlockEntity::getTank);

    }

}
