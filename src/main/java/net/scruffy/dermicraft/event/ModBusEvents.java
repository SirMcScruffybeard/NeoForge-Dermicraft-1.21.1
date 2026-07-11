package net.scruffy.dermicraft.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.BeakerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.CrawBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.DroolingCauldronBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.EffluentcerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MetastasizerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
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

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.METASTASIZER_BE.get(), MetastasizerBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.METASTASIZER_BE.get(), MetastasizerBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CRAW_BE.get(), CrawBlockEntity::getItemHandler);

        // Node: item automation is exposed via getItemHandler(Direction), which restricts any
        // direction-based (capability) query to the transport buffer slot only -- the GUI-only
        // fluid-handler slot stays unreachable by pipes/hoppers/other Nodes regardless.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.INNARDS_NODE_BE.get(), NodeBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.INNARDS_NODE_BE.get(), NodeBlockEntity::getTank);



        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.RigidFluidDataFluidHandler(stack, GlassFlaskItem.CAPACITY), ModItems.GLASS_FLASK.get());

        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.FlexibleFluidDataFluidHandler(stack, BeakerItem.CAPACITY), ModBlocks.BEAKER_ITEM.get());
    }

}
