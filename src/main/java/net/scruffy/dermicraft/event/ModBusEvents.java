package net.scruffy.dermicraft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.BeakerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.DroolingCauldronBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.EffluentcerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.BeakerItem;
import net.scruffy.dermicraft.item.custom.GlassFlaskItem;
import net.scruffy.dermicraft.main.Dermicraft;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class ModBusEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.DROOLING_CAULDRON_BE.get(), DroolingCauldronBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.DROOLING_CAULDRON_BE.get(), DroolingCauldronBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MASTICATOR_BE.get(), MasticatorBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MASTICATOR_BE.get(), MasticatorBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.SKIN_TANK_BE.get(), SkinTankBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.EFFLUENTCER_BE.get(), EffluentcerBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.EFFLUENTCER_BE.get(), EffluentcerBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.BEAKER_BE.get(), BeakerBlockEntity::getTank);



        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.RigidFluidDataFluidHandler(stack, GlassFlaskItem.CAPACITY), ModItems.GLASS_FLASK.get());

        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.FlexibleFluidDataFluidHandler(stack, BeakerItem.CAPACITY), ModBlocks.BEAKER_ITEM.get());
    }

}
