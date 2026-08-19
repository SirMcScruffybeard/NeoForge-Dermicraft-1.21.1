package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.scruffy.dermicraft.util.CombinedFluidHandler;
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
import net.scruffy.dermicraft.block.entity.custom.MutatorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.GraftingTableBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.RenderFurnaceBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.RenderKilnBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.BeakerItem;
import net.scruffy.dermicraft.item.custom.BladderItem;
import net.scruffy.dermicraft.item.custom.GlassFlaskItem;
import net.scruffy.dermicraft.item.custom.DrinkerItem;
import net.scruffy.dermicraft.item.custom.IdepItem;
import net.scruffy.dermicraft.item.custom.SippingItem;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SippingModeData;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MUTATOR_BE.get(), MutatorBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MUTATOR_BE.get(), MutatorBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RENDER_FURNACE_BE.get(), RenderFurnaceBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.RENDER_FURNACE_BE.get(), RenderFurnaceBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.GRAFTING_TABLE_BE.get(), GraftingTableBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.GRAFTING_TABLE_BE.get(), GraftingTableBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RENDER_KILN_BE.get(), RenderKilnBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.RENDER_KILN_BE.get(), RenderKilnBlockEntity::getTank);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CRAW_BE.get(), CrawBlockEntity::getItemHandler);

        // Mr. Farmer: both capabilities on all six faces. Fluid = the fuel tank (biofuel-filtered fill,
        // drain). Items = the automation wrapper (buffer extract-only, fuel slot accepts containers).
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MR_FARMER_BE.get(), MrFarmerBlockEntity::getTank);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MR_FARMER_BE.get(), MrFarmerBlockEntity::getAutomationItemHandler);

        // Mr. Shepard: same shape as Mr. Farmer -- fuel tank fluid handler, automation-restricted item handler.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MR_SHEPARD_BE.get(),
                net.scruffy.dermicraft.block.entity.custom.MrShepardBlockEntity::getTank);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MR_SHEPARD_BE.get(),
                net.scruffy.dermicraft.block.entity.custom.MrShepardBlockEntity::getAutomationItemHandler);

        // Node: item automation is exposed via getItemHandler(Direction), which restricts any
        // direction-based (capability) query to the transport buffer slot only -- the GUI-only
        // fluid-handler slot stays unreachable by pipes/hoppers/other Nodes regardless.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.INNARDS_NODE_BE.get(), NodeBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.INNARDS_NODE_BE.get(), NodeBlockEntity::getTank);

        // Innards Gate Port: has no block entity of its own -- it's a dumb pass-through whose
        // capability is the Gate Buffer(s) it physically touches (same direct-adjacency idea a duct
        // uses to reach a machine's END face). Scans all 6 neighbours and exposes ALL touching
        // Buffers of the matching kind as one combined handler (fill spills over into the next once
        // one's full; drain pulls first-available) -- so one duct line through a Port can serve
        // several Buffers, e.g. feed fuel to two stacked machines at once. A Buffer deliberately has
        // NO external capability of its own, so a duct must go through a Port, never touch a Buffer
        // directly. NOTE: a Port mixing an INPUT Buffer and an OUTPUT Buffer is a player-side
        // plumbing error (a pull could short-circuit an input) -- deliberately not guarded, since the
        // Port is a dumb block with no channel-IO awareness; documented in the player guide instead.
        event.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, direction) -> combinedTouchingItemHandler(level, pos),
                ModBlocks.INNARDS_GATE_PORT.get());
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level, pos, state, blockEntity, direction) -> combinedTouchingFluidHandler(level, pos),
                ModBlocks.INNARDS_GATE_PORT.get());

        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.RigidFluidDataFluidHandler(stack, GlassFlaskItem.CAPACITY), ModItems.GLASS_FLASK.get());

        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.FlexibleFluidDataFluidHandler(stack, BeakerItem.CAPACITY), ModBlocks.BEAKER_ITEM.get());

        // I.D.E.P.'s fluid storage is flexible (partial fills), same shape as the Beaker -- it's a
        // maintenance tool draining a Buffer's fluid, not a fixed-dose container like a Flask/Syringe.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.FlexibleFluidDataFluidHandler(stack, IdepItem.FLUID_CAPACITY), ModItems.IDEP.get());

        // Bladder family: bulk mobile storage, same flexible (partial-fill) shape as Beaker/I.D.E.P.,
        // gated to Tier 1's hazard restrictions (no hazardous fluids at all -- same as Drinker).
        // Fuel Bladder/Feeder Bladder share the base Bladder's capacity/tier ladder (same upgrade
        // recipes across all three) -- hazard gating included defensively even though neither is
        // expected to ever need to reject a fluid in practice.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.HazardGatedFluidDataFluidHandler(stack, BladderItem.CAPACITY, HazardProfile.TIER_1), ModItems.BLADDER.get());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.HazardGatedFluidDataFluidHandler(stack, BladderItem.CAPACITY, HazardProfile.TIER_1), ModItems.FEEDER_BLADDER.get());

        // Fuel Bladder: same tier/capacity as the rest of the family, plus a biofuel-only restriction
        // on top of the hazard gate -- it's meant to feed equipment fuel, not hold arbitrary fluid.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.HazardGatedFluidDataFluidHandler(
                stack, BladderItem.CAPACITY, HazardProfile.TIER_1, fluidStack -> fluidStack.is(ModTags.Fluids.BIOFUELS)),
                ModItems.FUEL_BLADDER.get());

        // D.R.I.N.K.E.R.: one source block's worth, hazard-gated. The gate does double duty -- the
        // siphon checks "can this buffer take a full 1000mB?" against this handler, so a fluid the
        // profile refuses simply never accumulates, no separate hazard check needed in the item.
        // Reads DrinkerItem.installedHazardProfile(stack) rather than a fixed TIER_1 -- this factory
        // lambda is re-invoked fresh on every getCapability() lookup, so a currently-installed Safety
        // Module's grant is picked up live. A hardcoded TIER_1 here silently overrode whatever a
        // Module granted: the item's own use()-side check (accumulateSource/drainTank) already
        // unions the Module in and would let a hazardous fluid through, but this handler's fill()
        // still refused it underneath, so the siphon read that refusal back as "buffer too full."
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) ->
                        new IHaveFluidData.HazardGatedFluidDataFluidHandler(stack, DrinkerItem.CAPACITY,
                                DrinkerItem.installedHazardProfile(stack)),
                ModItems.DRINKER.get());

        // S.I.P.P.I.N.G.: Storage mode is a flexible hazard-gated buffer (same shape as Bladder);
        // Disposal mode bypasses the buffer entirely and voids anything the hazard profile accepts.
        // Which handler is returned depends on the stack's current mode, read fresh each lookup.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> {
            SippingModeData mode = stack.getOrDefault(ModDataComponentTypes.SIPPING_MODE_DATA.get(), SippingModeData.DEFAULT);
            return mode.disposalMode()
                    ? new IHaveFluidData.DisposalFluidHandler(stack, HazardProfile.TIER_1)
                    : new IHaveFluidData.HazardGatedFluidDataFluidHandler(stack, SippingItem.CAPACITY, HazardProfile.TIER_1);
        }, ModItems.SIPPING.get());

        // E.A.T.E.R.: 4-slot bulk item buffer, no filter -- base tier accepts anything it vacuums.
        event.registerItem(Capabilities.ItemHandler.ITEM,
                (stack, context) -> new net.scruffy.dermicraft.interfaces.IHaveItemData.BulkItemHandler(
                        stack, net.scruffy.dermicraft.item.custom.EaterItem.SLOT_COUNT,
                        net.scruffy.dermicraft.item.custom.EaterItem.SLOT_CAPACITY),
                ModItems.EATER.get());

        // Sunder: biofuel-only fuel tank, same shape as Fuel Bladder -- nothing reads/gates on it
        // yet (see SunderItem's class javadoc), but the capability exists so the Scrench maintenance
        // GUI's fuel gauge/fill slot has a real tank to display and fill.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.HazardGatedFluidDataFluidHandler(
                stack, net.scruffy.dermicraft.item.custom.SunderItem.FUEL_CAPACITY, HazardProfile.TIER_1,
                fluidStack -> fluidStack.is(ModTags.Fluids.BIOFUELS)),
                ModItems.SUNDER.get());

        // Shatter: same biofuel-only fuel tank shape as Sunder's own -- nothing reads/gates on it
        // yet (see ShatterItem's class javadoc), but the capability exists so the Scrench maintenance
        // GUI's fuel gauge/fill slot has a real tank to display and fill.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new IHaveFluidData.HazardGatedFluidDataFluidHandler(
                stack, net.scruffy.dermicraft.item.custom.ShatterItem.FUEL_CAPACITY, HazardProfile.TIER_1,
                fluidStack -> fluidStack.is(ModTags.Fluids.BIOFUELS)),
                ModItems.SHATTER.get());
    }

    /** All Gate Buffers directly touching {@code portPos}, in {@link Direction#values()} order. */
    private static List<GateBufferBlockEntity> touchingBuffers(Level level, BlockPos portPos) {
        List<GateBufferBlockEntity> buffers = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(portPos.relative(dir)) instanceof GateBufferBlockEntity buffer) {
                buffers.add(buffer);
            }
        }
        return buffers;
    }

    @Nullable
    private static IItemHandler combinedTouchingItemHandler(Level level, BlockPos portPos) {
        List<IItemHandlerModifiable> handlers = new ArrayList<>();
        for (GateBufferBlockEntity buffer : touchingBuffers(level, portPos)) {
            // Only the item-kind Buffers expose a (modifiable) item handler; the rest return null.
            if (buffer.getExposedItemHandler() instanceof IItemHandlerModifiable modifiable) {
                handlers.add(modifiable);
            }
        }
        if (handlers.isEmpty()) return null;
        if (handlers.size() == 1) return handlers.get(0);
        return new CombinedInvWrapper(handlers.toArray(new IItemHandlerModifiable[0]));
    }

    @Nullable
    private static IFluidHandler combinedTouchingFluidHandler(Level level, BlockPos portPos) {
        List<IFluidHandler> handlers = new ArrayList<>();
        for (GateBufferBlockEntity buffer : touchingBuffers(level, portPos)) {
            IFluidHandler handler = buffer.getExposedFluidHandler();
            if (handler != null) handlers.add(handler);
        }
        if (handlers.isEmpty()) return null;
        if (handlers.size() == 1) return handlers.get(0);
        return new CombinedFluidHandler(handlers);
    }

}
