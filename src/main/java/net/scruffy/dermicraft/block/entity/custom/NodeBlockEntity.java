package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.duct.AbstractInnardsDuctBlock;
import net.scruffy.dermicraft.block.custom.duct.DuctConnection;
import net.scruffy.dermicraft.block.custom.duct.DuctRunResolver;
import net.scruffy.dermicraft.block.custom.duct.NodeDirectionMode;
import net.scruffy.dermicraft.block.custom.duct.NodeDistributionMode;
import net.scruffy.dermicraft.block.custom.duct.NodeTier;
import net.scruffy.dermicraft.block.custom.duct.TieredNode;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.screen.custom.node.NodeMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Node's block entity — the multi-way "pump"/router of the Innards Duct system.
 *
 * <p>Holds the relay buffer (1 transport item slot + a 1000mB tank), a GUI-only fluid-handler slot
 * for the manual jam-clear / test interaction, per-face In/Out/Off routing, per-face independent
 * item/fluid enable toggles ({@link #toggleItems}/{@link #toggleFluids} — replaces the earlier
 * planned per-run ITEM/FLUID mode-lock/idle-timer design; both types can flow on the same leg
 * simultaneously, each already rate-capped per cycle, so no lock is needed), a Round-Robin/
 * Equal-Spread distribution toggle, and the transfer tick itself. Break behaviour: drops the
 * buffered items, never spills the fluid (see design notes).
 *
 * <p><b>Node-to-Node chaining:</b> a run may end at another Node instead of a machine. Transfer is
 * gated by the *target* Node's own leg mode — a push only lands if the target's matching leg is
 * {@code IN}, a pull only succeeds if the target's matching leg is {@code OUT} (see
 * {@link #targetNodeAccepts}) — so a two-Node run behaves exactly like the documented "conflicting
 * direction = inert" rule, and chaining through several Nodes to a distant machine falls out for
 * free: each Node just forwards whatever lands in its buffer on its own next tick.
 * Item/fluid moved per leg per cycle is rate-capped (see {@link #itemTransferPerCycle()} /
 * {@link #fluidTransferPerCycle()}) rather than unlimited — tune those constants as needed.
 */
public class NodeBlockEntity extends MachineBaseBlockEntity implements MenuProvider {

    public static final int BUFFER_SLOT = 0;     // transport item buffer (automation-facing later)
    public static final int FLUID_ITEM_SLOT = 1; // GUI-only slot for a bucket/Beaker; never exposed to automation

    // The tank is a transport buffer, not storage -- its capacity is fixed across every Node tier
    // (only throughput and hazard tolerance vary; see NodeTier).
    public static final int TANK_CAPACITY = 1000;

    private boolean isTransferringFluids = false;

    // The tank's hazard profile comes from this Node's tier (TIER_1 rejects HAZARDOUS-tagged fluids,
    // the mod-wide baseline; higher tiers accept more). Since a duct has no block entity/fluid state
    // of its own, this tank is the ONLY place fluid ever sits in the whole system (every hop, in or
    // out, passes through it), so gating it here is enough to protect the entire duct network for
    // free -- no per-duct check needed.
    private final ModFluidTank TANK = new ModFluidTank(TANK_CAPACITY, 0, getNodeTier().hazardProfile()) {
        @Override
        protected void onContentsChanged() {
            if (level != null && !level.isClientSide) {
                setChanged();
                updateBlock();
            }
        }
    };

    // Per-face, per-type routing state -- only meaningful on a face that's actually connected (see
    // isConnected()); an unconnected face just isn't shown/clickable in the GUI. Item and fluid
    // direction are fully independent (2026-08-27 rework) -- a leg can pull items in while pushing
    // fluid out, or any other combination, since each type already moves through its own separate
    // capability (item slot vs tank) with no shared resource to conflict over. Both act on the same
    // tick cadence -- no alternation between types, since that would only spread the same total
    // work across more ticks (halving throughput) rather than actually reducing server cost; a
    // Node's per-cycle work is already bounded/cheap regardless of how many legs are active.
    private final Map<Direction, NodeDirectionMode> itemDirectionModes = new EnumMap<>(Direction.class);
    private final Map<Direction, NodeDirectionMode> fluidDirectionModes = new EnumMap<>(Direction.class);
    private NodeDistributionMode distributionMode = NodeDistributionMode.ROUND_ROBIN;

    // Per-leg item/fluid toggles -- replaces the earlier planned per-run ITEM/FLUID mode-lock with
    // an idle timer. Both default OFF: a leg set to In/Out carries nothing until the player
    // explicitly enables a type, rather than immediately carrying both. Fully independent of
    // direction mode and of each other -- a leg can carry both, either, or neither. Both types are
    // already rate-capped per leg per cycle (see itemTransferPerCycle()/fluidTransferPerCycle()),
    // so running both simultaneously on a leg is not a server-load concern. This is now the SOLE
    // on/off switch per type (see NodeDirectionMode's own javadoc for why OFF was retired from
    // direction itself).
    private final Map<Direction, Boolean> itemsEnabled = new EnumMap<>(Direction.class);
    private final Map<Direction, Boolean> fluidsEnabled = new EnumMap<>(Direction.class);

    {
        for (Direction dir : Direction.values()) {
            itemDirectionModes.put(dir, NodeDirectionMode.IN);
            fluidDirectionModes.put(dir, NodeDirectionMode.IN);
            itemsEnabled.put(dir, false);
            fluidsEnabled.put(dir, false);
        }
    }

    private final ItemStackHandler INVENTORY = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide()) {
                // Manual fluid interaction: a container in the GUI-only slot fills/drains the tank.
                if (slot == FLUID_ITEM_SLOT && !isTransferringFluids) {
                    isTransferringFluids = true;
                    if (TANK.hasFluidHandlerInSlot(this, FLUID_ITEM_SLOT)) {
                        TANK.transferFluidToTank(this, FLUID_ITEM_SLOT);
                    } else if (TANK.hasEmptyFluidHandlerInSlot(this, FLUID_ITEM_SLOT)) {
                        TANK.transferFluidFromTankToHandler(this, FLUID_ITEM_SLOT);
                    }
                    isTransferringFluids = false;
                }
                setChanged();
                updateBlock();
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == FLUID_ITEM_SLOT ? 1 : super.getSlotLimit(slot);
        }
    };

    public NodeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INNARDS_NODE_BE.get(), pos, blockState);
    }

    /** This Node's tier, read from its block (defaults to TIER_1 if the block isn't tiered). */
    public NodeTier getNodeTier() {
        return getBlockState().getBlock() instanceof TieredNode tiered ? tiered.getTier() : NodeTier.TIER_1;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    // Capability-facing view: only the transport buffer slot, never the GUI-only fluid-handler slot
    // -- keeps that slot unreachable by hoppers/ducts/other Nodes even now that item automation is
    // registered externally (see ModBusEvents), matching the same face-restricted-wrapper pattern
    // other machine BEs use for getItemHandler(Direction).
    private final IItemHandler CAPABILITY_ITEM_HANDLER = new RangedWrapper(INVENTORY, BUFFER_SLOT, BUFFER_SLOT + 1);

    /**
     * {@code null} (menu/GUI) gets the full buffer (both slots); any real direction (capability
     * query -- hoppers, another Node's push/pull, etc.) is restricted to the transport slot only.
     */
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        return direction == null ? INVENTORY : CAPABILITY_ITEM_HANDLER;
    }

    /** Capability-side access to the buffer tank. */
    public IFluidHandler getTank(@Nullable Direction direction) {
        return TANK;
    }

    /** Screen-side access to the concrete tank (capacity + fluid for rendering). */
    public ModFluidTank getFluidTank() {
        return TANK;
    }

    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    /** Drops the buffered items on break — deliberately does NOT spill the tank fluid. */
    public void drops() {
        super.drops(INVENTORY);
    }

    /**
     * The "before" half of a tier swap (see {@code AbstractNodeBlock#useItemOn}) -- snapshots this
     * Node's full persisted state (inventory, tank, per-leg toggles, distribution mode) via the same
     * NBT this class already round-trips through {@link #saveAdditional}/{@link #loadAdditional},
     * then clears this instance's own item inventory. The clear matters: the block swap that follows
     * changes the actual Block, which fires {@code AbstractNodeBlock#onRemove} -> {@link #drops()} on
     * THIS (about-to-be-discarded) instance -- without emptying it first, the buffered items would
     * spill on the ground on top of the copy already captured here, duplicating them.
     */
    public CompoundTag exportStateForSwap(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        INVENTORY.setStackInSlot(BUFFER_SLOT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(FLUID_ITEM_SLOT, ItemStack.EMPTY);
        return tag;
    }

    /** The "after" half of a tier swap -- applies a snapshot from {@link #exportStateForSwap} to
     * this (freshly constructed) instance. */
    public void importState(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    /**
     * Whether {@code dir} is a valid leg for this Node — either a duct run (reads the neighbouring
     * duct's own connection state facing back toward us, rather than deciding independently, since
     * the duct already applied its 2-connection cap and is the single source of truth for that), or
     * a capability-exposing block (machine, or another Node) sitting directly against this face with
     * no duct in between at all. {@link DuctRunResolver#resolve} already treats a directly-adjacent
     * non-duct block as a valid zero-hop endpoint; this just lets the GUI/toggle gate recognise the
     * same case instead of requiring a duct segment to exist purely to make the leg visible.
     * Works identically client- and server-side since it only reads block state / capabilities.
     */
    public boolean isConnected(Direction dir) {
        if (level == null) return false;
        BlockPos neighborPos = worldPosition.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);

        if (neighborState.getBlock() instanceof AbstractInnardsDuctBlock) {
            EnumProperty<DuctConnection> property = AbstractInnardsDuctBlock.PROPERTY_BY_DIRECTION.get(dir.getOpposite());
            return neighborState.getValue(property) == DuctConnection.PIPE;
        }

        Direction opposite = dir.getOpposite();
        return level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, opposite) != null
                || level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, opposite) != null;
    }

    public NodeDirectionMode getItemDirectionMode(Direction dir) {
        return itemDirectionModes.get(dir);
    }

    public NodeDirectionMode getFluidDirectionMode(Direction dir) {
        return fluidDirectionModes.get(dir);
    }

    public NodeDistributionMode getDistributionMode() {
        return distributionMode;
    }

    /** Cycles a connected leg's item direction In <-> Out. No-ops on an unconnected face
     * (server-side guard). Fully independent of {@link #cycleFluidDirection}. */
    public void cycleItemDirection(Direction dir) {
        if (!isConnected(dir)) return;
        itemDirectionModes.put(dir, itemDirectionModes.get(dir).next());
        setChanged();
        updateBlock();
    }

    /** Cycles a connected leg's fluid direction In <-> Out. No-ops on an unconnected face
     * (server-side guard). Fully independent of {@link #cycleItemDirection}. */
    public void cycleFluidDirection(Direction dir) {
        if (!isConnected(dir)) return;
        fluidDirectionModes.put(dir, fluidDirectionModes.get(dir).next());
        setChanged();
        updateBlock();
    }

    public void cycleDistribution() {
        distributionMode = distributionMode.next();
        setChanged();
        updateBlock();
    }

    public boolean isItemsEnabled(Direction dir) {
        return itemsEnabled.get(dir);
    }

    public boolean isFluidsEnabled(Direction dir) {
        return fluidsEnabled.get(dir);
    }

    /** Toggles a connected leg's item flow on/off. No-ops on an unconnected face (server-side guard). */
    public void toggleItems(Direction dir) {
        if (!isConnected(dir)) return;
        itemsEnabled.put(dir, !itemsEnabled.get(dir));
        setChanged();
        updateBlock();
    }

    /** Toggles a connected leg's fluid flow on/off. No-ops on an unconnected face (server-side guard). */
    public void toggleFluids(Direction dir) {
        if (!isConnected(dir)) return;
        fluidsEnabled.put(dir, !fluidsEnabled.get(dir));
        setChanged();
        updateBlock();
    }

    // Flow amount restrictions -- per leg, per CRAFT_TICKS cycle. Sourced from this Node's tier.
    private int itemTransferPerCycle() {
        return getNodeTier().itemThroughput();
    }

    private int fluidTransferPerCycle() {
        return getNodeTier().fluidThroughput();
    }

    // Independent round-robin cursors -- items and fluids can be enabled on entirely different leg
    // sets, so each type rotates through its own eligible legs rather than sharing one index.
    private int itemRoundRobinIndex = 0;
    private int fluidRoundRobinIndex = 0;

    public void tick(Level level) {
        if (level.isClientSide) return;
        if (!ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) return;

        // Redstone signal fully halts relay (same "powered = off" convention as a vanilla Hopper)
        // -- deliberately just a live check, not a persisted flag, so every leg's IN/OUT/item/fluid
        // toggle and the distribution mode stay exactly as configured; power removal resumes with
        // no reconfiguration needed. Doesn't touch capability exposure -- a duct or another block
        // that actively pushes into this Node's own buffer isn't blocked by this, only the Node's
        // own pull/push cycle is (matches vanilla Hopper's own scope).
        if (level.hasNeighborSignal(worldPosition)) return;

        pullPhase(level);
        pushPhase(level);

        setChanged();
        updateBlock();
    }

    private void pullPhase(Level level) {
        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;
            boolean wantsFluid = fluidsEnabled.get(dir) && fluidDirectionModes.get(dir) == NodeDirectionMode.IN;
            boolean wantsItems = itemsEnabled.get(dir) && itemDirectionModes.get(dir) == NodeDirectionMode.IN;
            if (!wantsFluid && !wantsItems) continue;

            Optional<DuctRunResolver.Endpoint> endpoint = DuctRunResolver.resolve(level, worldPosition, dir);
            if (endpoint.isEmpty()) continue;
            if (wantsFluid) pullFluidFrom(level, endpoint.get());
            if (wantsItems) pullItemFrom(level, endpoint.get());
        }
    }

    private void pushPhase(Level level) {
        List<Direction> fluidOutLegs = new ArrayList<>();
        List<Direction> itemOutLegs = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;
            if (fluidsEnabled.get(dir) && fluidDirectionModes.get(dir) == NodeDirectionMode.OUT) fluidOutLegs.add(dir);
            if (itemsEnabled.get(dir) && itemDirectionModes.get(dir) == NodeDirectionMode.OUT) itemOutLegs.add(dir);
        }

        if (distributionMode == NodeDistributionMode.EQUAL_SPREAD) {
            if (!fluidOutLegs.isEmpty()) {
                int fluidShare = Math.max(1, Math.min(fluidTransferPerCycle(), TANK.getFluidAmount()) / fluidOutLegs.size());
                for (Direction dir : fluidOutLegs) pushFluidTo(level, dir, fluidShare);
            }
            if (!itemOutLegs.isEmpty()) {
                int itemShare = Math.max(1, Math.min(itemTransferPerCycle(), INVENTORY.getStackInSlot(BUFFER_SLOT).getCount()) / itemOutLegs.size());
                for (Direction dir : itemOutLegs) pushItemTo(level, dir, itemShare);
            }
        } else {
            if (!fluidOutLegs.isEmpty()) {
                Direction dir = fluidOutLegs.get(fluidRoundRobinIndex % fluidOutLegs.size());
                pushFluidTo(level, dir, fluidTransferPerCycle());
                fluidRoundRobinIndex = (fluidRoundRobinIndex + 1) % fluidOutLegs.size();
            }
            if (!itemOutLegs.isEmpty()) {
                Direction dir = itemOutLegs.get(itemRoundRobinIndex % itemOutLegs.size());
                pushItemTo(level, dir, itemTransferPerCycle());
                itemRoundRobinIndex = (itemRoundRobinIndex + 1) % itemOutLegs.size();
            }
        }
    }

    /**
     * Whether a transfer against {@code endpoint} is allowed, when that endpoint turns out to be
     * another Node's leg. A plain machine endpoint has no leg concept, so it's always allowed.
     * A Node endpoint only accepts a push if its matching leg is {@code IN}, and only allows a
     * pull (i.e. it's actively supplying) if its matching leg is {@code OUT} -- this is what makes
     * the documented "two Nodes disagreeing on a shared leg = inert" rule actually enforced now
     * that Node-to-Node runs resolve instead of being rejected outright. Checks the TARGET's
     * direction for the SAME type being moved ({@code fluid}) -- independent per type, same as
     * this Node's own legs.
     */
    private boolean targetNodeAccepts(Level level, DuctRunResolver.Endpoint endpoint, NodeDirectionMode requiredMode, boolean fluid) {
        if (!(level.getBlockEntity(endpoint.pos()) instanceof NodeBlockEntity targetNode)) return true;
        NodeDirectionMode targetMode = fluid ? targetNode.getFluidDirectionMode(endpoint.accessDirection())
                : targetNode.getItemDirectionMode(endpoint.accessDirection());
        return targetMode == requiredMode;
    }

    private void pullFluidFrom(Level level, DuctRunResolver.Endpoint endpoint) {
        if (!TANK.hasRoom(1)) return;
        if (!targetNodeAccepts(level, endpoint, NodeDirectionMode.OUT, true)) return;
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, endpoint.pos(), endpoint.accessDirection());
        if (handler == null) return;

        int amount = Math.min(fluidTransferPerCycle(), TANK.getSpace());
        FluidStack simulated = handler.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) return;

        // Two independent tier checks: the run's weakest-duct filter (a duct upgrade tier may
        // accept fluids this Node's own tank doesn't, or vice versa -- both must agree) and the
        // Node's own tank tier. Don't actually drain the source unless both will accept it --
        // otherwise a rejected fluid would be pulled out and then silently voided.
        if (!endpoint.hazardProfile().accepts(simulated)) return;
        if (TANK.fill(simulated, IFluidHandler.FluidAction.SIMULATE) <= 0) return;

        FluidStack drained = handler.drain(simulated.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        TANK.fill(drained, IFluidHandler.FluidAction.EXECUTE);
    }

    private void pullItemFrom(Level level, DuctRunResolver.Endpoint endpoint) {
        if (!INVENTORY.getStackInSlot(BUFFER_SLOT).isEmpty()) return;
        if (!targetNodeAccepts(level, endpoint, NodeDirectionMode.OUT, false)) return;
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, endpoint.pos(), endpoint.accessDirection());
        if (handler == null) return;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack simulated = handler.extractItem(i, itemTransferPerCycle(), true);
            if (simulated.isEmpty()) continue;
            ItemStack extracted = handler.extractItem(i, simulated.getCount(), false);
            INVENTORY.setStackInSlot(BUFFER_SLOT, extracted);
            return;
        }
    }

    private void pushFluidTo(Level level, Direction dir, int cap) {
        if (TANK.isEmpty()) return;
        Optional<DuctRunResolver.Endpoint> endpointOpt = DuctRunResolver.resolve(level, worldPosition, dir);
        if (endpointOpt.isEmpty()) return;
        DuctRunResolver.Endpoint endpoint = endpointOpt.get();
        if (!targetNodeAccepts(level, endpoint, NodeDirectionMode.IN, true)) return;

        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, endpoint.pos(), endpoint.accessDirection());
        if (handler == null) return;

        int amount = Math.min(cap, TANK.getFluidAmount());
        if (amount <= 0) return;
        FluidStack simulated = TANK.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) return;
        if (!endpoint.hazardProfile().accepts(simulated)) return;

        int accepted = handler.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;

        FluidStack actuallyDrained = TANK.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        handler.fill(actuallyDrained, IFluidHandler.FluidAction.EXECUTE);
    }

    private void pushItemTo(Level level, Direction dir, int cap) {
        ItemStack current = INVENTORY.getStackInSlot(BUFFER_SLOT);
        if (current.isEmpty()) return;
        Optional<DuctRunResolver.Endpoint> endpointOpt = DuctRunResolver.resolve(level, worldPosition, dir);
        if (endpointOpt.isEmpty()) return;
        DuctRunResolver.Endpoint endpoint = endpointOpt.get();
        if (!targetNodeAccepts(level, endpoint, NodeDirectionMode.IN, false)) return;

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, endpoint.pos(), endpoint.accessDirection());
        if (handler == null) return;

        int amount = Math.min(cap, current.getCount());
        ItemStack toSend = current.copyWithCount(amount);
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, toSend, false);
        int inserted = amount - leftover.getCount();
        if (inserted > 0) {
            current.shrink(inserted);
            INVENTORY.setStackInSlot(BUFFER_SLOT, current);
        }
    }

    /** Parses a serialized NodeDirectionMode name, tolerating an unrecognized value (e.g. the
     * retired legacy "off") by returning empty rather than throwing. */
    private static Optional<NodeDirectionMode> readDirectionMode(String serialized) {
        for (NodeDirectionMode mode : NodeDirectionMode.values()) {
            if (mode.getSerializedName().equals(serialized)) return Optional.of(mode);
        }
        return Optional.empty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("tank", TANK.writeToNBT(registries, new CompoundTag()));
        for (Direction dir : Direction.values()) {
            tag.putString("item_mode_" + dir.getSerializedName(), itemDirectionModes.get(dir).getSerializedName());
            tag.putString("fluid_mode_" + dir.getSerializedName(), fluidDirectionModes.get(dir).getSerializedName());
            tag.putBoolean("items_" + dir.getSerializedName(), itemsEnabled.get(dir));
            tag.putBoolean("fluids_" + dir.getSerializedName(), fluidsEnabled.get(dir));
        }
        tag.putString("distribution", distributionMode.getSerializedName());
        tag.putInt("itemRoundRobinIndex", itemRoundRobinIndex);
        tag.putInt("fluidRoundRobinIndex", fluidRoundRobinIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("tank")) TANK.readFromNBT(registries, tag.getCompound("tank"));
        for (Direction dir : Direction.values()) {
            // Legacy migration (pre-2026-08-27 saves): a single shared "mode_<dir>" key covered
            // both types. Read it as a fallback ONLY when the new per-type keys aren't present yet,
            // so a legacy save's existing In/Out choice carries over to both types on first load
            // rather than silently resetting. Legacy "off" has no modern equivalent (see
            // NodeDirectionMode's own javadoc) -- the per-type keys just default to IN in that case,
            // harmless since the legacy leg's itemsEnabled/fluidsEnabled were already false.
            String legacyKey = "mode_" + dir.getSerializedName();
            String legacyValue = tag.contains(legacyKey) ? tag.getString(legacyKey) : null;

            String itemKey = "item_mode_" + dir.getSerializedName();
            if (tag.contains(itemKey)) {
                readDirectionMode(tag.getString(itemKey)).ifPresent(mode -> itemDirectionModes.put(dir, mode));
            } else if (legacyValue != null) {
                readDirectionMode(legacyValue).ifPresent(mode -> itemDirectionModes.put(dir, mode));
            }

            String fluidKey = "fluid_mode_" + dir.getSerializedName();
            if (tag.contains(fluidKey)) {
                readDirectionMode(tag.getString(fluidKey)).ifPresent(mode -> fluidDirectionModes.put(dir, mode));
            } else if (legacyValue != null) {
                readDirectionMode(legacyValue).ifPresent(mode -> fluidDirectionModes.put(dir, mode));
            }

            String itemsKey = "items_" + dir.getSerializedName();
            if (tag.contains(itemsKey)) itemsEnabled.put(dir, tag.getBoolean(itemsKey));
            String fluidsKey = "fluids_" + dir.getSerializedName();
            if (tag.contains(fluidsKey)) fluidsEnabled.put(dir, tag.getBoolean(fluidsKey));
        }
        if (tag.contains("distribution")) {
            for (NodeDistributionMode mode : NodeDistributionMode.values()) {
                if (mode.getSerializedName().equals(tag.getString("distribution"))) {
                    distributionMode = mode;
                    break;
                }
            }
        }
        if (tag.contains("itemRoundRobinIndex")) itemRoundRobinIndex = tag.getInt("itemRoundRobinIndex");
        if (tag.contains("fluidRoundRobinIndex")) fluidRoundRobinIndex = tag.getInt("fluidRoundRobinIndex");
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.INNARDS_NODE);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NodeMenu(containerId, playerInventory, this);
    }
}
