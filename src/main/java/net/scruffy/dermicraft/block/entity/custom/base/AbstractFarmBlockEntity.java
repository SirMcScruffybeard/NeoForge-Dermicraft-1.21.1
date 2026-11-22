package net.scruffy.dermicraft.block.entity.custom.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.custom.AbstractFarmBlock;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IModuleTabMachine;
import net.scruffy.dermicraft.machine.MachineStatKey;
import net.scruffy.dermicraft.machine.SimpleMachineStats;
import net.scruffy.dermicraft.machine.MachineTier;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import net.scruffy.dermicraft.util.SlotRangeItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The block entity shared by the farm machines (Mr. Farmer, Mr. Shepherd and the ones that follow). It owns everything
 * they have in common: the fuel tank and its container slot, the item inventory (the fuel slot first, then the machine's
 * own slots, the output buffer after them), the field the machine works (a width and a length, both odd, 1 to 9, set by
 * the player), the fuel-gated tick, the wave pacing helpers, the range preview timer, the automation handler, the Gate
 * channels, and saving.
 *
 * <p>A machine supplies its own work ({@link #tickFarmWork}), what a field means for it, how its preview looks, and its
 * extra slots and tanks. Fuel does not set the field. An unfueled machine does nothing, whatever its field is.
 *
 * <p>The field's axes: for a horizontally facing machine the width runs sideways across the machine and the length runs
 * away from it. For a machine facing up or down there is no sideways, so the width runs along the world's X axis and the
 * length along Z.
 */
public abstract class AbstractFarmBlockEntity extends EvolvingMachineBlockEntity
        implements MenuProvider, IHaveInventory, IHasChannels, IModuleTabMachine,
        net.scruffy.dermicraft.interfaces.IEvolvingMachine {

    public static final int MIN_FIELD_SIZE = 1;
    public static final int DEFAULT_FIELD_SIZE = 3;
    /** How far the field sits from the machine, in blocks (see {@link #getFieldDistance}). */
    public static final int MIN_FIELD_DISTANCE = 1;

    /** An evolution's flourish lands one step per this many ticks (the 10 tick cycle of the processing machines). */
    private static final int EVOLUTION_FLOURISH_PERIOD_TICKS = 10;

    // --- Wave sweep pacing --- (WAVE_STEP_TICKS_BASE is the delay between steps at fuel speed 1.0 (Crude Slurry);
    // currentWaveStepTicks() scales it down for faster fuel so the work visibly speeds up.)
    protected static final int WAVE_STEP_TICKS_BASE = 40; // 2s at speed 1.0
    protected static final float MIN_SPEED_FOR_PACING = 0.1f; // floor so a near-zero speed can't divide-by-near-zero

    // --- Range preview (particle visualization, started on GUI close) ---
    protected static final int PREVIEW_DURATION_TICKS = 600; // ~30s the preview stays active after closing the GUI
    protected static final int PREVIEW_FADE_TICKS = 100;     // over the last ~5s, taper particle density to fade out

    protected final FuelTank FUEL_TANK = createFuelTank();
    protected final ItemStackHandler INVENTORY;

    private final int bufferStartSlot;
    private final int bufferSlotCount;

    // Drain-only view of the output buffer for the Gate's output channel. The fuel container slot (and every other
    // non-buffer slot) is excluded, staying player-only just as the automation handler enforces.
    protected final IItemHandler BUFFER_CHANNEL;

    private final IItemHandler AUTOMATION_HANDLER = createAutomationHandler();

    protected boolean isTransferringFluids = false;

    private int fieldWidth = DEFAULT_FIELD_SIZE;
    private int fieldLength = DEFAULT_FIELD_SIZE;
    private int fieldDistance = MIN_FIELD_DISTANCE;
    private int previewTicksRemaining = 0;
    // General Module slots: their own handler, sized by the tier (see EvolvingMachineBlockEntity#getModuleSlotCount). Only
    // Work Speed Modules do anything on a farm machine today, so they are the only ones the slots take. Evolution Modules
    // go in the dedicated Evolution slot (EVOLUTION_INVENTORY, in the engine).
    protected final ItemStackHandler MODULE_INVENTORY = createModuleInventory(getModuleSlotCount(),
            stack -> stack.is(net.scruffy.dermicraft.datagen.tag.ModTags.Items.MODULE_WORK_SPEED));
    private boolean moduleTabActive = false;
    private float fuelOwed = 0f; // the fractional part of the fuel a burn owes, carried to the next burn (not saved)

    /**
     * @param inventorySize   total slots: the fuel container slot (0), the machine's own slots, the output buffer, and
     *                        any further container slots after it
     * @param bufferStartSlot the first output buffer slot
     * @param bufferSlotCount how many output buffer slots there are
     */
    protected AbstractFarmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState,
                                      int inventorySize, int bufferStartSlot, int bufferSlotCount) {
        super(type, pos, blockState);
        this.bufferStartSlot = bufferStartSlot;
        this.bufferSlotCount = bufferSlotCount;
        this.INVENTORY = createFarmItemHandler(inventorySize);
        this.BUFFER_CHANNEL = new SlotRangeItemHandler(INVENTORY, bufferStartSlot, bufferSlotCount, false, true);
    }

    // ---- What a machine supplies ------------------------------------------------------------------

    /** The machine's key in the stat config (its section of overrides). */
    @Override
    protected abstract MachineStatKey getStatKey();

    /** The machine's block, for its display name and the menu's range check. */
    public abstract DeferredBlock<Block> getFarmBlock();

    /** The machine's registry name, such as {@code mr_farmer}, used for the Gate face tooltip key. */
    protected abstract String getMachineName();

    /** The machine's work, run on every tick it has enough fuel for. */
    protected abstract void tickFarmWork(Level level);

    /** Runs on every tick the machine does not have enough fuel for (drop a sweep, turn lights off, and so on). */
    protected void onOutOfFuel(Level level) {
    }

    /** Draws or emits the range preview for one tick; only called while the preview is running. */
    protected abstract void tickPreview(Level level);

    /** Runs when a preview starts (or restarts), so a sweep can be recomputed against the current field. */
    protected void onPreviewStarted() {
    }

    /** Runs when the field's width or length changed. */
    protected void onFieldChanged() {
    }

    /** Runs when the fuel tank's contents changed (server only), before the block is marked changed. */
    protected void onFuelContentsChanged() {
    }

    /** Adds this machine's Gate channels in priority order (starvation risk first: fuel, then what the machine eats,
     * then outputs). {@code exposeFluid} and {@code exposeItem} are false when a neighbor already services that kind. */
    protected abstract void addChannels(List<Channel> channels, boolean exposeFluid, boolean exposeItem);

    /** Spills anything the machine holds beyond its inventory; the inventory itself is dropped by the base. */
    protected void dropExtras() {
    }

    /** Whether automation (pipes, hoppers) may insert into this slot. The output buffer and the fuel slot never accept. */
    protected boolean canAutomationInsert(int slot, ItemStack stack) {
        return false;
    }

    /** The tank this inventory slot is the fill/drain container for, or null for an ordinary slot. */
    @Nullable
    protected ModFluidTank getTankForSlot(int slot) {
        return slot == FUEL_TANK.SLOT ? FUEL_TANK : null;
    }

    // ---- Evolution: tier, Evolution slot and Module slots ---------------------------------------------

    @Override
    protected boolean usesEvolutionState() {
        return true;
    }

    @Override
    public ItemStackHandler getModuleInventory() {
        return MODULE_INVENTORY;
    }

    @Override
    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    @Override
    public void setModuleTabActive(boolean active) {
        moduleTabActive = active;
    }

    /** A new tier can change the Module slot count, the fuel tank's size and the field's largest size and distance, so the
     * slots are resized and the field is clamped to what this tier allows (a field never outgrows its machine). */
    @Override
    protected void onEvolutionStateChanged() {
        super.onEvolutionStateChanged();
        resizeModuleInventory(MODULE_INVENTORY, getModuleSlotCount());
        FUEL_TANK.setCapacity(SimpleMachineStats.getTankCapacityMb(getStatKey(), getEvolutionState()));
        fieldWidth = snapFieldSize(fieldWidth);
        fieldLength = snapFieldSize(fieldLength);
        fieldDistance = snapFieldDistance(fieldDistance);
    }

    /** Work Speed Module bonus across the Module slots, with the diminishing-return stacking rule of
     * {@link IHaveModules#workSpeedMultiplier}. */
    protected float workSpeedMultiplier() {
        List<ItemStack> modules = new ArrayList<>();
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            modules.add(MODULE_INVENTORY.getStackInSlot(slot));
        }
        return IHaveModules.workSpeedMultiplier(modules);
    }

    /** The tier's speed multiplier, which multiplies with the fuel's speed and the Work Speed Modules. */
    protected float getTierSpeedMultiplier() {
        return SimpleMachineStats.getSpeedMultiplier(getStatKey(), getEvolutionState());
    }

    /** The tier's multiplier on fuel spent for the same work. */
    protected float getTierFuelUseMultiplier() {
        return SimpleMachineStats.getFuelUseMultiplier(getStatKey(), getEvolutionState());
    }

    /** How far along the pending evolution is, 0 to 1, for the creeping overlay (see IEvolvingMachine). */
    @Override
    public float getEvolutionProgressFraction() {
        return getGradualEvolutionFraction();
    }

    // ---- Basics ---------------------------------------------------------------------------------

    @Override
    public boolean hasTank() {
        return true;
    }

    public FuelTank getFuelTank() {
        return FUEL_TANK;
    }

    /** How many slots the inventory has in all (the fuel container, the machine's own, the buffer, any further containers). */
    public int getInventorySize() {
        return INVENTORY.getSlots();
    }

    public int getBufferStartSlot() {
        return bufferStartSlot;
    }

    public int getBufferSlotCount() {
        return bufferSlotCount;
    }

    protected Direction getFacing() {
        return getBlockState().getValue(AbstractFarmBlock.FACING);
    }

    /** Flips the ACTIVE blockstate (on/off texture) only when it actually changes; keeps the same BE. */
    protected void setActive(Level level, boolean active) {
        BlockState state = getBlockState();
        if (state.getValue(AbstractFarmBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(AbstractFarmBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Field (the area the machine works, set by the player) -------------------------------------

    public int getFieldWidth() {
        return fieldWidth;
    }

    public int getFieldLength() {
        return fieldLength;
    }

    /** Blocks either side of the machine's center line (0 for a width of 1, 4 for a width of 9). */
    protected int getHalfWidth() {
        return (fieldWidth - 1) / 2;
    }

    /** Blocks of length either side of the center for a field centered on the machine (a vertically facing machine). */
    protected int getHalfLength() {
        return (fieldLength - 1) / 2;
    }

    /** The largest width or length this machine's tier allows (odd). */
    public int getMaxFieldSize() {
        return SimpleMachineStats.getMaxFieldSize(getStatKey(), getEvolutionState());
    }

    /** The furthest this machine's tier lets the field sit from it. */
    public int getMaxFieldDistance() {
        return SimpleMachineStats.getMaxFieldDistance(getStatKey(), getEvolutionState());
    }

    /** Snaps a requested size to what a field can be: 1 up to the tier's largest, odd (an even size rounds up to the next
     * odd one, and the largest is odd, so the result never passes it). */
    public int snapFieldSize(int size) {
        return Math.max(MIN_FIELD_SIZE, Math.min(getMaxFieldSize(), size)) | 1;
    }

    private int snapFieldDistance(int distance) {
        return Math.max(MIN_FIELD_DISTANCE, Math.min(getMaxFieldDistance(), distance));
    }

    /** Sets the field's size, snapped to an odd 1 up to the tier's largest each way. updateBlock() so the GUI readout changes live. */
    public void setFieldSize(int width, int length) {
        int newWidth = snapFieldSize(width);
        int newLength = snapFieldSize(length);
        if (newWidth == fieldWidth && newLength == fieldLength) return;
        fieldWidth = newWidth;
        fieldLength = newLength;
        onFieldChanged();
        setChanged();
        updateBlock();
    }

    /** One step wider (positive) or narrower (negative); a step is two blocks, so the width stays odd. */
    public void stepFieldWidth(int direction) {
        setFieldSize(fieldWidth + 2 * Integer.signum(direction), fieldLength);
    }

    /** One step longer (positive) or shorter (negative); a step is two blocks, so the length stays odd. */
    public void stepFieldLength(int direction) {
        setFieldSize(fieldWidth, fieldLength + 2 * Integer.signum(direction));
    }

    /**
     * How far the field sits from the machine along its facing, 1 to 8. Facing up or down it is the number of blocks from
     * the machine to the field's work layer (the block right above the ground: the crop cells for Mr. Farmer), so 1 is the
     * block touching the machine. Facing sideways it is where the field starts: 1 is the first block in front.
     */
    public int getFieldDistance() {
        return fieldDistance;
    }

    /** Sets the field's distance from the machine, clamped to 1 up to the tier's furthest. */
    public void setFieldDistance(int distance) {
        int newDistance = snapFieldDistance(distance);
        if (newDistance == fieldDistance) return;
        fieldDistance = newDistance;
        onFieldChanged();
        setChanged();
        updateBlock();
    }

    /** One block further out (positive) or closer in (negative). */
    public void stepFieldDistance(int direction) {
        setFieldDistance(fieldDistance + Integer.signum(direction));
    }

    // ---- Pacing -----------------------------------------------------------------------------------

    /** The fuel's speed ratio times the tier's and the Work Speed Modules', floored so pacing math never divides by (near)
     * zero. */
    protected float pacingSpeed() {
        return Math.max(MIN_SPEED_FOR_PACING, FUEL_TANK.getSpeed() * getTierSpeedMultiplier() * workSpeedMultiplier());
    }

    /** Delay between work steps: BASE at fuel speed 1.0, faster with better fuel. */
    protected int currentWaveStepTicks() {
        return Math.max(1, Math.round(WAVE_STEP_TICKS_BASE / pacingSpeed()));
    }

    protected int fuelPerStep() {
        return Math.max(1, FUEL_TANK.getUseRate());
    }

    /**
     * Burns {@code steps} fuel steps' worth of fuel (a step costs {@link #fuelPerStep}); a fractional step is carried over to
     * the next burn, so a field a third of a step's work doesn't round up to a whole step every time. Fuel cost follows the
     * work: a machine reports how much it did, in steps, and the 3 x 3 field is the baseline where that is one step per
     * row (Farmer) or per cycle (Shepherd), the cost before the field was the player's to set.
     */
    protected void burnFuel(float steps) {
        fuelOwed += fuelPerStep() * steps * getTierFuelUseMultiplier();
        int whole = Math.min((int) fuelOwed, FUEL_TANK.getFluidAmount());
        if (whole <= 0) return;
        FUEL_TANK.useFuel(whole);
        fuelOwed -= whole;
    }

    // ---- Tick ---------------------------------------------------------------------------------------

    public void tick(Level level) {
        if (level.isClientSide) return;
        syncEvolutionBlockState(); // mirrors the tier and Overgrowth stage into the block state, which picks the plate and accents

        tickRangePreview(level); // free, runs unfueled: it's a siting guide, not machine work

        if (!FUEL_TANK.hasEnoughFuel(fuelPerStep())) {
            setActive(level, false); // no fuel: idle, "off" texture
            onOutOfFuel(level);
            return;
        }
        setActive(level, true); // fueled: "on" texture

        // Work is frozen while an evolution's flourish plays, so the new tier never lands mid-sweep.
        if (isEvolutionFlourishing()) {
            if (level.getGameTime() % EVOLUTION_FLOURISH_PERIOD_TICKS == 0) tickGradualFlourish();
            return;
        }
        tickFarmWork(level);
    }

    // ---- Range preview ----------------------------------------------------------------------------

    /** Starts (or restarts) the ~30s range preview. Called on GUI close; see AbstractFarmMenu. */
    public void startRangePreview() {
        previewTicksRemaining = PREVIEW_DURATION_TICKS;
        onPreviewStarted();
        setChanged();
    }

    private void tickRangePreview(Level level) {
        if (previewTicksRemaining <= 0) return;
        previewTicksRemaining--;
        tickPreview(level);
    }

    protected int getPreviewTicksRemaining() {
        return previewTicksRemaining;
    }

    /** Full density until the fade window, then a linear taper to 0 so the preview visibly fades out. */
    protected int previewParticleCount(int fullCount) {
        if (previewTicksRemaining >= PREVIEW_FADE_TICKS) return fullCount;
        return Math.round((float) fullCount * previewTicksRemaining / PREVIEW_FADE_TICKS);
    }

    protected void emitPreviewParticles(Level level, Iterable<BlockPos> cells, int count) {
        if (!(level instanceof ServerLevel serverLevel) || count <= 0) return;
        for (BlockPos p : cells) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    count, 0.25, 0.25, 0.25, 0.0);
        }
    }

    // ---- Buffer -------------------------------------------------------------------------------------

    /** Inserts into the output buffer slots; returns what did not fit. */
    protected ItemStack insertIntoBuffer(ItemStack stack) {
        ItemStack remaining = stack;
        for (int slot = bufferStartSlot; slot < bufferStartSlot + bufferSlotCount && !remaining.isEmpty(); slot++) {
            remaining = INVENTORY.insertItem(slot, remaining, false);
        }
        return remaining;
    }

    // ---- Capabilities / GUI -------------------------------------------------------------------------

    public IFluidHandler getTank(@Nullable Direction direction) {
        return FUEL_TANK;
    }

    /** Full-access handler used by the GUI (every slot, insert + extract). */
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        return INVENTORY;
    }

    /**
     * Automation-facing item handler (all faces identical): the output buffer is extract-only, and only the slots the
     * machine opens through {@link #canAutomationInsert} take input. Every container slot (fuel, XP) is player-only, so
     * containers can't be shuttled in or out by pipes; automated fueling goes through the FluidHandler capability.
     */
    public IItemHandler getAutomationItemHandler(@Nullable Direction direction) {
        return AUTOMATION_HANDLER;
    }

    private IItemHandler createAutomationHandler() {
        return new IItemHandler() {
            @Override public int getSlots() { return INVENTORY.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return INVENTORY.getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return INVENTORY.getSlotLimit(slot); }

            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return canAutomationInsert(slot, stack) && INVENTORY.isItemValid(slot, stack);
            }

            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!canAutomationInsert(slot, stack)) return stack;
                return INVENTORY.insertItem(slot, stack, simulate);
            }

            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot < bufferStartSlot || slot >= bufferStartSlot + bufferSlotCount) return ItemStack.EMPTY;
                return INVENTORY.extractItem(slot, amount, simulate);
            }
        };
    }

    // ---- Gate integration (IHasChannels) ------------------------------------------------------------

    /** See {@link IHasChannels#describeFace} -- every face routes identically on these machines. */
    @Override
    public Component describeFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.idep.face." + getMachineName());
    }

    /** See {@link IHasChannels#describeFluidFace} -- getTank ignores the face; it's always the fuel tank. */
    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.tank.fuel");
    }

    /**
     * Self-described channels for the Gate, all exposed on all six faces since the capabilities are registered
     * face-agnostically. The machine adds them in priority order via {@link #addChannels}.
     */
    @Override
    public List<Channel> getChannels() {
        boolean exposeFluid = level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.values());
        boolean exposeItem = level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM, Direction.values());
        List<Channel> channels = new ArrayList<>();
        addChannels(channels, exposeFluid, exposeItem);
        return channels;
    }

    /** The fuel channel every farm machine has. */
    protected Channel fuelChannel() {
        return new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK);
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
        super.drops(MODULE_INVENTORY);
        super.drops(EVOLUTION_INVENTORY);
        dropExtras();
    }

    // ---- Tank and inventory factories -----------------------------------------------------------------

    private FuelTank createFuelTank() {
        return new FuelTank(SimpleMachineStats.getTankCapacityMb(getStatKey(), getEvolutionState()), 0) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide) {
                    onFuelContentsChanged();
                    setChanged();
                    updateBlock();
                }
            }
        };
    }

    private ItemStackHandler createFarmItemHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide()) {
                    ModFluidTank tank = getTankForSlot(slot);
                    if (tank != null && !isTransferringFluids) {
                        biDirectionalFluidTransfer(tank, slot);
                        isTransferringFluids = false;
                    }
                    setChanged();
                    updateBlock();
                }
            }

            // Shared by every tank's container slot (fuel, XP): the same tank-agnostic ModFluidTank API either way.
            private void biDirectionalFluidTransfer(ModFluidTank tank, int slot) {
                if (tank.hasFluidHandlerInSlot(this, slot)) {
                    isTransferringFluids = true;
                    tank.transferFluidToTank(this, slot);
                } else {
                    transferToHandler(tank, slot);
                }
            }

            private void transferToHandler(ModFluidTank tank, int slot) {
                if (tank.hasEmptyFluidHandlerInSlot(this, slot)) {
                    isTransferringFluids = true;

                    ItemStack stack = getStackInSlot(slot);
                    if (stack.isEmpty()) return;

                    // Defensive fallback only -- getSlotLimit caps a container slot to 1 unconditionally, so a stack
                    // of more than 1 shouldn't be reachable via normal insertion. Kept in case something ever forces a
                    // multi-count stack into the slot directly (NBT load, a command, etc.).
                    if (ModFluidUtil.hasEmptyFluidHandlerInSlot(this, slot) && stack.getCount() > 1) {
                        ItemStack extra = stack.copy();
                        extra.shrink(1);

                        stack.setCount(1);
                        setStackInSlot(slot, stack);

                        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), extra);
                    } else {
                        setStackInSlot(slot, stack);
                    }
                    tank.transferFluidFromTankToHandler(this, slot);
                }
            }

            // A container slot holds exactly one container at a time, unconditionally -- not just once already
            // occupied (the old `hasEmptyFluidHandlerInSlot` condition inspects the slot's CURRENT contents, so it
            // never fires on a slot that starts empty and a whole stack of empty containers could be inserted in one
            // shot). Capping unconditionally means vanilla's own insertItem accepts just 1 and returns the remainder.
            @Override
            public int getSlotLimit(int slot) {
                return getTankForSlot(slot) != null ? 1 : super.getSlotLimit(slot);
            }

            // A container slot only takes fluid containers. Without this, quickMoveStack walks the machine's slots in
            // registration order -- fuel first -- so shift-clicking seeds or feed from the player's inventory would
            // drop one item into the fuel slot before the rest reached its own row. Slot.mayPlace() routes through
            // here, covering shift-click too.
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (getTankForSlot(slot) != null) {
                    return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
                }
                return super.isItemValid(slot, stack);
            }
        };
    }

    // ---- Saving -------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("fuel", FUEL_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("fieldWidth", fieldWidth);
        tag.putInt("fieldLength", fieldLength);
        tag.putInt("fieldDistance", fieldDistance);
        tag.put("module_inventory", MODULE_INVENTORY.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Not a plain INVENTORY.deserializeNBT: a machine saved before a slot was added carries the old Size and would
        // shrink this handler back down, crashing on world load when the menu adds a slot past it (same gotcha
        // documented on SkinTankBlockEntity). loadItemHandler resizes safely.
        if (tag.contains("inventory")) loadItemHandler(INVENTORY, INVENTORY.getSlots(), registries, tag.getCompound("inventory"));
        if (tag.contains("fuel")) FUEL_TANK.readFromNBT(registries, tag.getCompound("fuel"));
        if (tag.contains("fieldWidth")) fieldWidth = snapFieldSize(tag.getInt("fieldWidth"));
        if (tag.contains("fieldLength")) fieldLength = snapFieldSize(tag.getInt("fieldLength"));
        if (tag.contains("fieldDistance")) fieldDistance = snapFieldDistance(tag.getInt("fieldDistance"));
        if (tag.contains("module_inventory")) {
            loadItemHandler(MODULE_INVENTORY, getModuleSlotCount(), registries, tag.getCompound("module_inventory"));
        }
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(getFarmBlock());
    }
}
