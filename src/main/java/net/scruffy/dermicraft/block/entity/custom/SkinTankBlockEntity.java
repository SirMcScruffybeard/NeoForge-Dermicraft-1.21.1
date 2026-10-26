package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.base.MachineBaseBlockEntity;
import net.scruffy.dermicraft.item.custom.module.EvolutionModuleItem;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.*;
import net.scruffy.dermicraft.machine.*;
import net.scruffy.dermicraft.screen.custom.skin_tank.SkinTankMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Pilot for machine Module slots (dermicraft-progression-notes.md, Decision Point #2 -> sequencing
 * step 4) -- chosen as the simplest case: one plain tank, no crafting, nothing else competing for
 * screen space. Module slot(s) live in their own dedicated {@link #MODULE_INVENTORY} handler
 * (see {@code MachineBaseBlockEntity#createModuleInventory}), not the general item-container
 * {@code BulkItemData} component gadgets use (there's no {@link ItemStack} for a machine to carry
 * one on) -- see {@link IHaveModules#installedHazardProfile(HazardProfile, ItemStack)}, the
 * single-slot overload this reads from in a loop to support more than one slot.
 */
public class SkinTankBlockEntity extends MachineBaseBlockEntity
        implements MenuProvider, IHaveInventory, IHasChannels, IPreserveContentsOnPickup {

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    /** Single source of truth for the handler's slot count -- also re-asserted after
     * {@code deserializeNBT}, see {@link #loadAdditional} for why that's load-bearing. Module no
     * longer counts toward this -- see {@link #MODULE_INVENTORY}, its own dedicated handler. */
    public static final int INVENTORY_SIZE = 2;

    public final ItemStackHandler INVENTORY = createInventory();

    /** Dedicated Module-only handler, sized per {@link #getModuleSlotCount()} (1 to 4 from the tier; the
     * Charred Tank uses its L2 count of 2) and resized when the evolution state changes, see
     * {@link #onEvolutionStateChanged()} -- see {@code MachineBaseBlockEntity#createModuleInventory}. */
    public final ItemStackHandler MODULE_INVENTORY = createModuleInventory(getModuleSlotCount(),
            stack -> stack.is(net.scruffy.dermicraft.datagen.tag.ModTags.Items.MODULES) && !isEvolutionModule(stack));

    /** The dedicated Evolution Module slot: one slot, Evolution Modules only, so installing one never
     * disturbs a tuned setup in the general slots. Inert once this tank can no longer evolve. */
    public final ItemStackHandler EVOLUTION_INVENTORY = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canEvolve() && isEvolutionModule(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide) {
                onEvolutionSlotChanged();
                setChanged();
                updateBlock();
            }
        }
    };

    /** Whether a stack is an Evolution Module: an item with real {@code EvolutionModuleProperties} data. */
    public static boolean isEvolutionModule(ItemStack stack) {
        return stack.getItem() instanceof EvolutionModuleItem;
    }

    private final VulnerableTank TANK = createTank();

    /** Capability leap hook for a Tier 2 evolution (Charred Tank) to override -- higher capacity,
     * unconditional hazard tolerance -- same "hook override, not a new tier stat" split Masticator/
     * Metastasizer's own {@code createIngredientTank()}/{@code createReagentTank()} use. */
    protected VulnerableTank createTank() {
        return createVulnerableTank(getBaseCapacity(), -1, this::installedHazardProfile);
    }

    /** The evolution state whose tier and Overgrowth level pick this tank's stats. The Skin Tank uses its
     * own state; {@link CharredTankBlockEntity} overrides it to read the L2 row until it is folded into
     * the Skin Tank's L2-and-up tiers. */
    protected EvolutionStateData getStatState() {
        return getEvolutionState();
    }

    /** The un-bonused capacity, in mB, this instance would use with no Capacity Module installed, from
     * {@link SkinTankStats}. Used by {@link #createTank()}, {@link #applyCapacityBonus()} and
     * {@link #canRemoveModule(int)}; re-applied whenever the evolution state changes, see
     * {@link #onEvolutionStateChanged()}. */
    protected int getBaseCapacity() {
        return SkinTankStats.getTankCapacityMb(getStatState());
    }

    /** Millibuckets per second this tank may auto-push, from {@link SkinTankStats}. */
    protected int getFluidTransferPerSecond() {
        return SkinTankStats.getFluidTransferPerSecond(getStatState());
    }

    /** General Module slots, from {@link SkinTankStats} (the dedicated Evolution slot is separate). */
    @Override
    public int getModuleSlotCount() {
        return SkinTankStats.getModuleSlots(getStatState());
    }

    /** Capacity Module bonus, summed over every installed Module -- see
     * {@code MachineBaseBlockEntity#capacityModuleBonus}. */
    @Override
    protected int getCapacityBonus() {
        int total = 0;
        for (int i = 0; i < MODULE_INVENTORY.getSlots(); i++) {
            total += capacityModuleBonus(MODULE_INVENTORY.getStackInSlot(i));
        }
        return total;
    }

    @Override
    protected void onEvolutionStateChanged() {
        resizeModuleInventory(MODULE_INVENTORY, getModuleSlotCount());
        applyCapacityBonus();
    }

    @Override
    protected void applyCapacityBonus() {
        TANK.setCapacity(getBaseCapacity() + getCapacityBonus());
    }

    @Override
    protected boolean canRemoveModule(int slot) {
        int thisBonus = capacityModuleBonus(MODULE_INVENTORY.getStackInSlot(slot));
        if (thisBonus == 0) return true;
        int newCapacity = getBaseCapacity() + (getCapacityBonus() - thisBonus);
        return TANK.getFluid().getAmount() <= newCapacity;
    }

    /** The fluid hazards this tank tolerates: a Tier 1 base (none), plus whatever Safety Modules sit in the
     * general Module slots, plus the permanent tolerance it has earned by evolving (every hazard family
     * held in its {@link EvolutionStateData}, at the level held). Same "union, never reset" rule as every
     * gadget's identical method (DrinkerItem, SippingItem). An Evolution Module still waiting in its slot
     * grants nothing; the tolerance only arrives when the evolution completes. Public so a future
     * screen/tooltip can read it without duplicating the union logic. */
    public HazardProfile installedHazardProfile() {
        HazardProfile profile = HazardProfile.TIER_1;
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            ItemStack module = MODULE_INVENTORY.getStackInSlot(slot);
            profile = IHaveModules.installedHazardProfile(profile, module);
        }

        // Permanent tolerance earned by evolving: every family held, at the level held.
        for (HazardFamily family : HazardFamily.values()) {
            profile = profile.plusFamily(family, getEvolutionState().level(family));
        }

        return profile;
    }

    // ---- Evolution (an Evolution Module in the Evolution slot raises this tank in place) ------
    // Storage never processes, so there is no progress to accumulate: installing an Evolution Module
    // that applies (EvolutionRules says so) starts a short countdown (EvolutionStats.getCountdownTicks(),
    // 5s by default) with the shared EvolutionEffects particles, and when it ends the new evolution state
    // is applied in place. The window exists only so the player can pull the module back out if they did
    // not mean to commit; pulling it cancels the countdown, as does picking up or breaking the tank.
    // There is no progress overlay: a creeping overlay on a tank would fuse with, or hide, its
    // translucent fluid surface.
    private int flourishTicksRemaining = -1;
    /** The countdown's full length when it started, saved so the progress fraction stays right on the client
     * (which has no server config) and across a config reload. */
    private int flourishTotalTicks = 1;

    /** Whether this instance can still evolve at all -- false for {@link CharredTankBlockEntity} until
     * the Charred Tank is retired. */
    protected boolean canEvolve() {
        return true;
    }

    /** The upgrade the Evolution slot's module would apply right now, from {@link EvolutionRules}. Empty
     * for the bare frame, a module this tank's state does not allow (wrong tier, family cap, already
     * held), or an instance that cannot evolve -- those sit in the slot and do nothing. */
    private Optional<EvolutionResult.Upgrade> getPendingUpgrade() {
        return getEvolutionStatus()
                .filter(EvolutionResult.Upgrade.class::isInstance)
                .map(EvolutionResult.Upgrade.class::cast);
    }

    /** What the Evolution slot's module would do on this tank right now: empty for the bare frame, an empty
     * slot or an instance that cannot evolve; otherwise the {@link EvolutionRules} answer, an upgrade that
     * will apply or the reason it is blocked. The slot tooltip reads this. */
    public Optional<EvolutionResult> getEvolutionStatus() {
        if (!canEvolve()) return Optional.empty();
        ItemStack module = EVOLUTION_INVENTORY.getStackInSlot(0);
        if (!(module.getItem() instanceof EvolutionModuleItem evolutionModule)) return Optional.empty();
        return evolutionModule.evaluate(getEvolutionState(), module);
    }

    /** Called whenever the Evolution slot's contents change -- cancels any countdown already running
     * (covers the module being pulled back out), then starts a fresh one if the slot now holds a module
     * that would apply. Insert, remove, and swap all fall out of this one rule. */
    private void onEvolutionSlotChanged() {
        flourishTicksRemaining = -1;
        if (getPendingUpgrade().isPresent()) startEvolutionFlourish();
    }

    /** Runs every tick: the cancel window counts down, then the upgrade is applied. A tank never
     * processes, so the long config times do not apply here -- only the window. */
    private void tickEvolutionFlourish() {
        if (flourishTicksRemaining < 0) return;

        if (level instanceof ServerLevel serverLevel) {
            EvolutionEffects.spawnParticles(serverLevel, worldPosition, getEvolutionProgress(),
                    flourishTicksRemaining == 0, EvolutionEffects.DEFAULT_TINT, EvolutionEffects.Cadence.TICK);
        }

        flourishTicksRemaining--;
        if (flourishTicksRemaining < 0) {
            completeEvolution();
        } else {
            updateBlock(); // the Evolution slot's fill reads the progress on the client
        }
    }

    /** How far along the running evolution countdown is, 0 to 1 (0 when none), for the Evolution slot's
     * fill. Synced to the client through the block entity, so the screen can read it directly. */
    public float getEvolutionProgress() {
        if (flourishTicksRemaining < 0) return 0f;
        return EvolutionEffects.getProgress(flourishTicksRemaining, flourishTotalTicks);
    }

    private void startEvolutionFlourish() {
        flourishTotalTicks = EvolutionStats.getCountdownTicks();
        flourishTicksRemaining = flourishTotalTicks;
        if (level != null) EvolutionEffects.playDefaultStartSound(level, worldPosition);
    }

    /**
     * Applies the module's upgrade to this tank in place, and consumes the module. The upgrade is
     * evaluated again here in case the state changed during the window; if it no longer applies, the
     * module stays in the slot. The block, its contents and its general Modules are untouched --
     * {@link #onEvolutionStateChanged()} resizes the Module slots and capacity.
     */
    private void completeEvolution() {
        Optional<EvolutionResult.Upgrade> upgrade = getPendingUpgrade();
        if (upgrade.isEmpty()) return;

        EVOLUTION_INVENTORY.setStackInSlot(0, ItemStack.EMPTY);
        setEvolutionState(upgrade.get().newState());
        setChanged();
        updateBlock();
    }

    // Which screen tab was last open -- mirrors WorkbenchBlockEntity#isFabricationPageActive so
    // reopening this Skin Tank's screen returns to the tab last viewed instead of always resetting.
    private boolean moduleTabActive = false;

    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    public void setModuleTabActive(boolean active) {
        this.moduleTabActive = active;
    }

    public SkinTankBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.SKIN_TANK_BE.get(), pos, blockState);
    }

    // Lets a capability-leap subclass (Charred Tank) register under its own BlockEntityType while
    // reusing everything else this class provides -- see MachineTier's own javadoc on why a genuine
    // capability leap (here: higher capacity + hazard-tolerant tank) is a hook override, not a new
    // MachineTier constant (Skin Tank has no MachineTier concept at all, but the same split applies).
    protected SkinTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    /** See {@link IHasChannels#describeFluidFace} -- mirrors {@link #getTank} literally (one tank,
     * every face); describeFace names the fill/drain bucket slots alongside it. */
    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.tank.storage");
    }

    public IFluidHandler getTank(@Nullable Direction face) {
        return TANK;
    }

    /** See {@link IHasChannels#describeFace} -- getTank/getItemHandler ignore the face entirely. */
    @Override
    public Component describeFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.idep.face.skin_tank_storage");
    }

    public IFluidHandler getTank() {
        return getTank(null);
    }

    IItemHandler getItemHandler(@Nullable Direction face) {
        return INVENTORY;
    }

    /**
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}.
     * Unlike every other machine here, {@code getTank(Direction)} already ignores the face entirely
     * -- Skin Tank is a plain storage tank, not a machine with distinct crafting input/output tanks,
     * so it was never face-locked to begin with. Modelled as {@link Channel.IO#BOTH} rather than
     * IN or OUT, matching that existing bidirectional behaviour. Native faces are all 6; like the
     * Drooling Cauldron's result tank, this is rarely the actual bottleneck given how permissive it
     * already is, but the self-filter still applies correctly to the fully-isolated case.
     */
    @Override
    public List<Channel> getChannels() {
        if (level != null && isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.values())) {
            return List.of();
        }
        return List.of(
                new Channel.FluidChannel("storage", Component.literal("Storage"), Channel.IO.BOTH, TANK)
        );
    }

    public void drops(){
        dropItems(level, INVENTORY, worldPosition);
        // Module(s) moved out of INVENTORY into their own handler -- without this, a normal (non-
        // Forceps) break would silently delete whatever Module was installed instead of dropping it.
        dropItems(level, MODULE_INVENTORY, worldPosition);
        dropItems(level, EVOLUTION_INVENTORY, worldPosition);
    }

    public void tick(Level sLevel) {
        if (!sLevel.isClientSide) {
            // Early-return on a completed swap -- same as DroolingMachineBlockEntity#onTickStart --
            // this instance is now stale, its own TANK already drained by completeEvolution.
            tickEvolutionFlourish();

            if (autoDrainEnabled && ModMath.Time.hasTicksPassed(sLevel, 20)) {
                TANK.pushFluidToBelowNeighbour(level, worldPosition, getFluidTransferPerSecond());
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.SKIN_TANK);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SkinTankMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("skin_tank_inv", INVENTORY.serializeNBT(registries));
        tag.put("skin_tank_module_inv", MODULE_INVENTORY.serializeNBT(registries));
        tag.put("skin_tank_evolution_inv", EVOLUTION_INVENTORY.serializeNBT(registries));
        tag = TANK.writeToNBT(registries, tag);
        tag.putBoolean("module_tab_active", moduleTabActive);
        tag.putInt("evolution_flourish_ticks", flourishTicksRemaining);
        tag.putInt("evolution_flourish_total", flourishTotalTicks);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // NOT a plain INVENTORY.deserializeNBT -- a Skin Tank saved before the Module slot existed
        // carries Size=2 and would shrink this handler back to 2 slots, crashing on world load when
        // the menu adds its Module slot at index 2. See MachineBaseBlockEntity#loadItemHandler.
        CompoundTag oldInventoryTag = tag.getCompound("skin_tank_inv");
        loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, oldInventoryTag);

        if (tag.contains("skin_tank_module_inv")) {
            loadItemHandler(MODULE_INVENTORY, getModuleSlotCount(), registries, tag.getCompound("skin_tank_module_inv"));
        } else {
            // Pre-split save: the Module item was the old combined handler's trailing slot (index
            // 2). Recover it from the same tag loadItemHandler just read above, before that data
            // is gone for good -- see MachineBaseBlockEntity#extractLegacyModuleStack.
            ItemStack legacyModule = extractLegacyModuleStack(registries, oldInventoryTag, 2);
            if (!legacyModule.isEmpty()) MODULE_INVENTORY.setStackInSlot(0, legacyModule);
        }

        if (tag.contains("skin_tank_evolution_inv")) {
            loadItemHandler(EVOLUTION_INVENTORY, 1, registries, tag.getCompound("skin_tank_evolution_inv"));
        }

        applyCapacityBonus();
        TANK.readFromNBT(registries, tag);
        moduleTabActive = tag.getBoolean("module_tab_active");
        flourishTicksRemaining = tag.contains("evolution_flourish_ticks")
                ? tag.getInt("evolution_flourish_ticks") : -1;
        flourishTotalTicks = Math.max(1, tag.getInt("evolution_flourish_total"));
    }

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(INVENTORY_SIZE) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide) {

                    if (TANK.hasFluidHandlerInSlot(this, INPUT)) {
                        TANK.transferFluidToTank(this, INPUT);
                    }

                    if (TANK.hasEmptyFluidHandlerInSlot(this, OUTPUT)) {
                        TANK.transferFluidFromTankToHandler(this, OUTPUT);
                    }

                    setChanged();
                    updateBlock();
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == OUTPUT ? 1 : super.getSlotLimit(slot);
            }
        };
    }

}
