package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.Channel;
import org.jetbrains.annotations.Nullable;

/**
 * The Gate Buffer's block entity -- a universal single-channel relay store. It holds BOTH an item
 * slot and a fluid tank internally, but only the one matching its currently-assigned channel kind is
 * ever used; the other stays empty. Kind can flip freely while empty (see design notes), which is
 * enforced by the Controller only reassigning empty Buffers.
 *
 * <p><b>Scope of this first pass:</b> storage + assignment bookkeeping + NBT only. The Controller
 * drives what actually moves in/out (Stage 2); a touching Port exposes the active handler outward
 * (Stage 2). No transfer logic lives here yet.
 */
public class GateBufferBlockEntity extends MachineBaseBlockEntity {

    public static final int ITEM_CAPACITY = 64;
    public static final int FLUID_CAPACITY = 1000; // mB

    // The channel this Buffer is currently assigned to service, as declared by the target machine's
    // getChannels() (id is stable per machine). null = unassigned/idle. Set only by the Controller.
    @Nullable
    private String assignedChannelId = null;

    // Which internal store is live. null when unassigned. Mirrors the assigned channel's kind so a
    // Port/Controller knows which handler to touch without re-consulting the machine.
    @Nullable
    private Channel.Kind assignedKind = null;

    private final ItemStackHandler ITEM = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return ITEM_CAPACITY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide) {
                setChanged();
                updateBlock();
            }
        }
    };

    private final FluidTank FLUID = new FluidTank(FLUID_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            if (level != null && !level.isClientSide) {
                setChanged();
                updateBlock();
            }
        }
    };

    public GateBufferBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INNARDS_GATE_BUFFER_BE.get(), pos, blockState);
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Nullable
    public String getAssignedChannelId() {
        return assignedChannelId;
    }

    @Nullable
    public Channel.Kind getAssignedKind() {
        return assignedKind;
    }

    public ItemStackHandler getItemStore() {
        return ITEM;
    }

    public FluidTank getFluidStore() {
        return FLUID;
    }

    /** External capability view (a touching Gate Port) -- only exposes the store matching the
     * currently assigned kind, so an idle/unassigned Buffer (or one assigned the other kind) never
     * leaks a handler for the store it isn't using. */
    @Nullable
    public IItemHandler getExposedItemHandler() {
        return assignedKind == Channel.Kind.ITEM ? ITEM : null;
    }

    @Nullable
    public IFluidHandler getExposedFluidHandler() {
        return assignedKind == Channel.Kind.FLUID ? FLUID : null;
    }

    /** True when neither internal store holds anything -- the only state in which a reassignment
     * (including a kind flip) is allowed, so contents are never silently voided or orphaned. */
    public boolean isEmpty() {
        return ITEM.getStackInSlot(0).isEmpty() && FLUID.isEmpty();
    }

    /** Assign (or clear, with null) this Buffer to a channel. Caller (the Controller) guarantees the
     * Buffer is empty before any kind-changing reassignment. */
    public void assign(@Nullable String channelId, @Nullable Channel.Kind kind) {
        this.assignedChannelId = channelId;
        this.assignedKind = kind;
        setChanged();
        updateBlock();
    }

    /** Drops buffered items on break; deliberately does NOT spill the fluid (Node precedent). */
    public void drops() {
        super.drops(ITEM);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("item", ITEM.serializeNBT(registries));
        tag.put("fluid", FLUID.writeToNBT(registries, new CompoundTag()));
        if (assignedChannelId != null) tag.putString("channel", assignedChannelId);
        if (assignedKind != null) tag.putString("kind", assignedKind.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("item")) ITEM.deserializeNBT(registries, tag.getCompound("item"));
        if (tag.contains("fluid")) FLUID.readFromNBT(registries, tag.getCompound("fluid"));
        assignedChannelId = tag.contains("channel") ? tag.getString("channel") : null;
        assignedKind = tag.contains("kind") ? Channel.Kind.valueOf(tag.getString("kind")) : null;
    }
}
