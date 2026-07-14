package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.scruffy.dermicraft.block.custom.gate.GateBufferBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IGateBlock;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Gate Controller's block entity. Every poll cycle it re-derives its bonded structure (adjacency
 * chain of Gate blocks) and its target machine's ({@link IHasChannels}) current channel list, prunes
 * any assignment that's no longer valid, greedily claims free Buffers for still-unserviced channels
 * in the priority order the channel list declares (NO GUI -- see design notes), then relays each
 * assigned channel against its Buffer. Bonding is positional and recomputed each poll -- only the
 * channel->Buffer assignments are persisted, so they survive a temporary disconnect (the assignment
 * is kept but skipped until the bond returns), matching the "sticky, re-evaluated on change" rule.
 *
 * <p>Breaking and replacing the Controller itself is non-destructive: the fresh Controller's
 * assignment map starts empty, but {@link #adoptRememberedAssignments} reunites it with every bonded
 * Buffer's own persisted memory on the very next poll, whether or not that Buffer is currently empty.
 *
 * <p>Known accepted limitation: if a channel disappears from the list (e.g. the self-filter finds it
 * newly serviced elsewhere) while its Buffer still holds leftover contents, that Buffer is simply no
 * longer relayed -- it isn't drained or reclaimed automatically. A player can break the Buffer to
 * recover the contents. Not expected to be a common case; revisit if it bites in practice.
 */
public class GateControllerBlockEntity extends MachineBaseBlockEntity {

    // Bond verification / relay cadence. Bonding only changes on block break/place, so polling well
    // below every-tick is plenty -- 10 ticks (0.5s) keeps it responsive without per-tick scans.
    private static final int POLL_TICKS = 10;

    // Guard on the adjacency-chain flood-fill so a pathological Gate sprawl can't spin forever.
    private static final int MAX_STRUCTURE_SIZE = 64;

    // Persisted, sticky: channel id -> the Buffer position servicing it. Survives a temporary
    // disconnect; pruned only when the Buffer is truly gone or the channel stops needing service.
    private final Map<String, BlockPos> assignments = new HashMap<>();

    // Transient, rebuilt every poll from world state -- never persisted (bonding is positional).
    private final Set<BlockPos> bondedBuffers = new HashSet<>();
    private final Set<BlockPos> bondedPorts = new HashSet<>();
    @Nullable
    private BlockPos targetMachinePos = null;

    public GateControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INNARDS_GATE_CONTROLLER_BE.get(), pos, blockState);
    }

    // Flow amount restrictions -- per channel, per poll cycle. Mirrors the Node's own constants.
    private static final int ITEM_TRANSFER_PER_CYCLE = 4;
    private static final int FLUID_TRANSFER_PER_CYCLE = 100; // mB

    public void tick(Level level) {
        if (level.isClientSide) return;
        if (!ModMath.Time.hasTicksPassed(level, POLL_TICKS)) return;

        discoverStructure(level);
        List<Channel> channels = currentChannels(level);
        pruneAssignments(level, channels);
        adoptRememberedAssignments(level, channels);
        claimUnservicedChannels(level, channels);
        relay(level, channels);

        setChanged();
    }

    /** The target machine's current channel list, or empty if unbonded/gone. Fetched fresh every
     * poll since it can be neighbour-dependent (Stage 3's self-filter). */
    private List<Channel> currentChannels(Level level) {
        if (targetMachinePos == null) return List.of();
        if (level.getBlockEntity(targetMachinePos) instanceof IHasChannels target) return target.getChannels();
        return List.of();
    }

    /**
     * Reunites this Controller's assignment map with Buffers that already remember servicing a
     * still-valid channel -- a Buffer persists its own {@code assignedChannelId}/{@code assignedKind}
     * independently of the Controller (see {@link GateBufferBlockEntity}), so this survives the
     * Controller itself being broken and replaced: a fresh Controller has an empty assignment map,
     * but the very next poll re-adopts every bonded Buffer's own memory here, before any fresh
     * empty-Buffer claiming happens below. Unlike {@link #findFreeBuffer}, non-empty Buffers are
     * eligible -- adoption doesn't risk contents, it's just recognising a pairing that already exists.
     */
    private void adoptRememberedAssignments(Level level, List<Channel> channels) {
        Set<String> validIds = new HashSet<>();
        for (Channel channel : channels) validIds.add(channel.id());

        for (BlockPos pos : bondedBuffers) {
            if (assignments.containsValue(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof GateBufferBlockEntity buffer)) continue;

            String channelId = buffer.getAssignedChannelId();
            if (channelId == null || !validIds.contains(channelId)) continue;
            if (assignments.containsKey(channelId)) continue; // some other buffer already claims it

            assignments.put(channelId, pos);
        }
    }

    /** Greedily claims a free (bonded, empty) Buffer for each still-unassigned channel, in the
     * priority order {@code channels} declares. Stops as soon as no free Buffer remains -- lower
     * priority channels beyond that point just stay unserviced until more Buffers are added. */
    private void claimUnservicedChannels(Level level, List<Channel> channels) {
        for (Channel channel : channels) {
            if (assignments.containsKey(channel.id())) continue;
            BlockPos freeBuffer = findFreeBuffer(level);
            if (freeBuffer == null) return;
            assignments.put(channel.id(), freeBuffer);
            if (level.getBlockEntity(freeBuffer) instanceof GateBufferBlockEntity buffer) {
                buffer.assign(channel.id(), channel.kind());
            }
        }
    }

    /** Any bonded Buffer not already claimed and currently empty -- kind is irrelevant here since an
     * empty Buffer can flip freely (see design notes). */
    @Nullable
    private BlockPos findFreeBuffer(Level level) {
        for (BlockPos pos : bondedBuffers) {
            if (assignments.containsValue(pos)) continue;
            if (level.getBlockEntity(pos) instanceof GateBufferBlockEntity buffer && buffer.isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    /** Moves contents between each assigned Buffer and its channel's handler, per the channel's own
     * declared IO -- IN pushes Buffer -> channel, OUT pulls channel -> Buffer, BOTH picks whichever
     * direction the Buffer's current state calls for (non-empty -> push, empty -> pull). */
    private void relay(Level level, List<Channel> channels) {
        Map<String, Channel> channelsById = new HashMap<>();
        for (Channel channel : channels) channelsById.put(channel.id(), channel);

        for (Map.Entry<String, BlockPos> entry : assignments.entrySet()) {
            Channel channel = channelsById.get(entry.getKey());
            if (channel == null) continue; // filtered out this poll; assignment stays sticky, just idle
            if (!(level.getBlockEntity(entry.getValue()) instanceof GateBufferBlockEntity buffer)) continue;

            switch (channel.io()) {
                case IN -> pushBufferToChannel(buffer, channel);
                case OUT -> pullChannelToBuffer(buffer, channel);
                case BOTH -> {
                    if (buffer.isEmpty()) pullChannelToBuffer(buffer, channel);
                    else pushBufferToChannel(buffer, channel);
                }
            }
        }
    }

    private void pushBufferToChannel(GateBufferBlockEntity buffer, Channel channel) {
        if (channel instanceof Channel.FluidChannel fluidChannel) {
            FluidTank store = buffer.getFluidStore();
            if (store.isEmpty()) return;

            int amount = Math.min(FLUID_TRANSFER_PER_CYCLE, store.getFluidAmount());
            FluidStack simulated = store.drain(amount, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) return;

            int accepted = fluidChannel.handler().fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) return;

            FluidStack drained = store.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            fluidChannel.handler().fill(drained, IFluidHandler.FluidAction.EXECUTE);
        } else if (channel instanceof Channel.ItemChannel itemChannel) {
            ItemStack current = buffer.getItemStore().getStackInSlot(0);
            if (current.isEmpty()) return;

            ItemStack toSend = current.copyWithCount(Math.min(ITEM_TRANSFER_PER_CYCLE, current.getCount()));
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(itemChannel.handler(), toSend, false);
            int inserted = toSend.getCount() - leftover.getCount();
            if (inserted > 0) buffer.getItemStore().extractItem(0, inserted, false);
        }
    }

    private void pullChannelToBuffer(GateBufferBlockEntity buffer, Channel channel) {
        if (channel instanceof Channel.FluidChannel fluidChannel) {
            FluidTank store = buffer.getFluidStore();
            if (store.getSpace() <= 0) return;

            IFluidHandler handler = fluidChannel.handler();
            int amount = Math.min(FLUID_TRANSFER_PER_CYCLE, store.getSpace());
            FluidStack simulated = handler.drain(amount, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) return;
            if (store.fill(simulated, IFluidHandler.FluidAction.SIMULATE) <= 0) return;

            FluidStack drained = handler.drain(simulated.getAmount(), IFluidHandler.FluidAction.EXECUTE);
            store.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        } else if (channel instanceof Channel.ItemChannel itemChannel) {
            if (!buffer.getItemStore().getStackInSlot(0).isEmpty()) return; // single-slot; wait for drain

            IItemHandler handler = itemChannel.handler();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack simulated = handler.extractItem(i, ITEM_TRANSFER_PER_CYCLE, true);
                if (simulated.isEmpty()) continue;
                ItemStack extracted = handler.extractItem(i, simulated.getCount(), false);
                buffer.getItemStore().setStackInSlot(0, extracted);
                return;
            }
        }
    }

    /** BFS out from the Controller over adjacent Gate blocks (Buffers/Ports/further Controllers are
     * all valid links), collecting the bonded Buffer and Port positions, and separately locating the
     * one target {@link IHasChannels} machine adjacent to the Controller itself. */
    private void discoverStructure(Level level) {
        bondedBuffers.clear();
        bondedPorts.clear();
        targetMachinePos = null;

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(worldPosition);
        queue.add(worldPosition);

        while (!queue.isEmpty() && visited.size() <= MAX_STRUCTURE_SIZE) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;

                BlockState state = level.getBlockState(neighbor);

                // Locate the target machine only off the Controller's own faces -- the machine is
                // what the Controller is plugged into, not something reached through the Gate sprawl.
                if (current.equals(worldPosition) && targetMachinePos == null
                        && level.getBlockEntity(neighbor) instanceof IHasChannels) {
                    targetMachinePos = neighbor;
                }

                if (state.getBlock() instanceof IGateBlock) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    if (state.getBlock() instanceof GateBufferBlock) {
                        bondedBuffers.add(neighbor);
                    } else if (!neighbor.equals(worldPosition)) {
                        // A Gate block that isn't a Buffer and isn't us: a Port (or another Controller,
                        // which just extends the chain and is otherwise ignored here).
                        bondedPorts.add(neighbor);
                    }
                }
            }
        }
    }

    /** Drop assignments whose Buffer is no longer a bonded Gate Buffer at the recorded position, or
     * whose channel id is no longer in the target's current channel list (e.g. Stage 3's self-filter
     * found it newly serviced elsewhere). An assignment to a still-bonded-but-momentarily-unreachable
     * Buffer is otherwise kept (sticky). */
    private void pruneAssignments(Level level, List<Channel> channels) {
        Set<String> validIds = new HashSet<>();
        for (Channel channel : channels) validIds.add(channel.id());

        assignments.entrySet().removeIf(entry -> {
            if (!validIds.contains(entry.getKey())) return true;
            BlockPos bufferPos = entry.getValue();
            if (!bondedBuffers.contains(bufferPos)) return true;
            return !(level.getBlockEntity(bufferPos) instanceof GateBufferBlockEntity);
        });
    }

    @Nullable
    public BlockPos getTargetMachinePos() {
        return targetMachinePos;
    }

    public Set<BlockPos> getBondedBuffers() {
        return bondedBuffers;
    }

    public Set<BlockPos> getBondedPorts() {
        return bondedPorts;
    }

    /** Lean diagnostic for the I.D.E.P. tool: the channels the target currently declares that this
     * Controller has NOT (yet) claimed a Buffer for -- i.e. genuinely waiting on more Buffers, not
     * the full state dump {@link #debugSummaryLines} gives. Empty list also covers "no target".
     * Returns the full {@link Channel} (not just the id) so callers can use its friendly label(). */
    public List<Channel> getUnservicedChannels() {
        if (level == null) return List.of();
        List<Channel> channels = currentChannels(level);
        List<Channel> unserviced = new ArrayList<>();
        for (Channel channel : channels) {
            if (!assignments.containsKey(channel.id())) unserviced.add(channel);
        }
        return unserviced;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Map.Entry<String, BlockPos> entry : assignments.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putString("channel", entry.getKey());
            e.putLong("pos", entry.getValue().asLong());
            list.add(e);
        }
        tag.put("assignments", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        assignments.clear();
        ListTag list = tag.getList("assignments", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            assignments.put(e.getString("channel"), BlockPos.of(e.getLong("pos")));
        }
    }

    /** Convenience for Stage 2 / debugging: the current channel->Buffer assignment view. */
    public List<Map.Entry<String, BlockPos>> getAssignments() {
        return new ArrayList<>(assignments.entrySet());
    }

    /** Temporary diagnostic (no GUI exists to show this otherwise) -- a plain-text snapshot of what
     * this Controller currently sees: target machine, bonded structure size, the target's live
     * channel list (post self-filter), and the current channel->Buffer assignment map. */
    public List<String> debugSummaryLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Target machine: " + (targetMachinePos == null ? "NONE" : targetMachinePos.toShortString()));
        lines.add("Bonded: " + bondedBuffers.size() + " buffer(s), " + bondedPorts.size() + " port(s)");

        if (level != null) {
            List<Channel> channels = currentChannels(level);
            StringBuilder channelLine = new StringBuilder("Channels now: ");
            if (channels.isEmpty()) {
                channelLine.append("(none)");
            } else {
                for (Channel channel : channels) {
                    channelLine.append(channel.id()).append("[").append(channel.kind()).append("/").append(channel.io()).append("] ");
                }
            }
            lines.add(channelLine.toString());
        } else {
            lines.add("Channels now: (level is null)");
        }

        StringBuilder assignLine = new StringBuilder("Assignments: ");
        if (assignments.isEmpty()) {
            assignLine.append("(none)");
        } else {
            for (Map.Entry<String, BlockPos> entry : assignments.entrySet()) {
                assignLine.append(entry.getKey()).append("->").append(entry.getValue().toShortString()).append(" ");
            }
        }
        lines.add(assignLine.toString());

        return lines;
    }
}
