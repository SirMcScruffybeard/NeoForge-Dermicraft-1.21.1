package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
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
import net.scruffy.dermicraft.block.entity.custom.GateBufferBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MrFarmerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MetastasizerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.BeakerItem;
import net.scruffy.dermicraft.item.custom.BladderItem;
import net.scruffy.dermicraft.item.custom.GlassFlaskItem;
import net.scruffy.dermicraft.item.custom.IdepItem;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

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

        // Mr. Farmer: both capabilities on all six faces. Fluid = the fuel tank (biofuel-filtered fill,
        // drain). Items = the automation wrapper (buffer extract-only, fuel slot accepts containers).
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MR_FARMER_BE.get(), MrFarmerBlockEntity::getTank);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MR_FARMER_BE.get(), MrFarmerBlockEntity::getAutomationItemHandler);

        // Node: item automation is exposed via getItemHandler(Direction), which restricts any
        // direction-based (capability) query to the transport buffer slot only -- the GUI-only
        // fluid-handler slot stays unreachable by pipes/hoppers/other Nodes regardless.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.INNARDS_NODE_BE.get(), NodeBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.INNARDS_NODE_BE.get(), NodeBlockEntity::getTank);

        // Innards Gate Port: has no block entity of its own -- it's a dumb pass-through whose
        // capability is whichever adjacent Gate Buffer it's physically touching (same direct-
        // adjacency idea a duct uses to reach a machine's END face). Scans all 6 neighbours (not
        // just the query direction) since a Port can be sandwiched between multiple Buffers; if more
        // than one matches, the first found (Direction.values() order) wins -- a Buffer deliberately
        // has NO external capability of its own, so a duct must go through a Port, never touch a
        // Buffer directly.
        event.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, direction) -> findTouchingBuffer(level, pos, GateBufferBlockEntity::getExposedItemHandler),
                ModBlocks.INNARDS_GATE_PORT.get());
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level, pos, state, blockEntity, direction) -> findTouchingBuffer(level, pos, GateBufferBlockEntity::getExposedFluidHandler),
                ModBlocks.INNARDS_GATE_PORT.get());

        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.RigidFluidDataFluidHandler(stack, GlassFlaskItem.CAPACITY), ModItems.GLASS_FLASK.get());

        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.FlexibleFluidDataFluidHandler(stack, BeakerItem.CAPACITY), ModBlocks.BEAKER_ITEM.get());

        // I.D.E.P.'s fluid storage is flexible (partial fills), same shape as the Beaker -- it's a
        // maintenance tool draining a Buffer's fluid, not a fixed-dose container like a Flask/Syringe.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.FlexibleFluidDataFluidHandler(stack, IdepItem.FLUID_CAPACITY), ModItems.IDEP.get());

        // Bladder: bulk mobile storage, same flexible (partial-fill) shape as Beaker/I.D.E.P.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.FlexibleFluidDataFluidHandler(stack, BladderItem.CAPACITY), ModItems.BLADDER.get());
    }

    @Nullable
    private static <T> T findTouchingBuffer(Level level, BlockPos portPos, Function<GateBufferBlockEntity, T> extractor) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(portPos.relative(dir)) instanceof GateBufferBlockEntity buffer) {
                T handler = extractor.apply(buffer);
                if (handler != null) return handler;
            }
        }
        return null;
    }

}
