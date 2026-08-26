package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.MutatorBlock;
import net.scruffy.dermicraft.block.custom.MutatorVisualState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.BaseFluidType;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IEvolvingMachine;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.property.EvolutionModuleProperties;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.mutating.MutatingRecipe;
import net.scruffy.dermicraft.screen.custom.mutator.MutatorMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import net.scruffy.dermicraft.util.ModItemUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Mutator: "apply a fluid to an item to change the item." Two modes, toggled by the player
 * (see {@link #setMode}):
 * <ul>
 *     <li>{@link Mode#MUTATE} -- fuel + reagent fluid (consumed) + item (consumed) -> a different,
 *     more complex item. Recipe-driven ({@link MutatingRecipe}), same shape as the Metastasizer's
 *     recipe but the input item is consumed rather than kept as a pattern.</li>
 *     <li>{@link Mode#FILL} -- no fuel/reagent cost beyond the fluid itself, which is packaged
 *     INTO the input item rather than consumed. Generic capability operation, not recipe-driven --
 *     see {@link #tickFill}.</li>
 * </ul>
 * Both modes are gated by the standard HP mechanic (see {@link AbstractFueledMachineBlockEntity}),
 * so a starved Mutator does neither.
 */
public class MutatorBlockEntity extends AbstractFueledMachineBlockEntity<MutatingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels, IEvolvingMachine {

    public static final int INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int INVENTORY_SIZE = 4;

    private static final int FILL_RATE = 250; // mB per cycle, standard rate

    public enum Mode { MUTATE, FILL }

    private final VulnerableTank REAGENT_TANK = createReagentTank();

    private boolean isTransferringFluids = false;

    public final ItemStackHandler INVENTORY = createInventory(INVENTORY_SIZE);

    // Module slot(s) -- own dedicated handler, not part of INVENTORY above. Declared here so
    // Charred Mutator inherits it for free; same tab-gated pattern as Masticator/Metastasizer/
    // Effluentcer's own (see MutatorMenu's MAIN_TAB/MODULE_TAB).
    public final ItemStackHandler MODULE_INVENTORY = createModuleInventory(moduleSlotCount());

    private ItemStack cachedResult = ItemStack.EMPTY;
    private int requiredFluid = 0;

    private Mode mode = Mode.MUTATE;

    // Fill-mode state -- see tickFill(). fillBudget is a rate-equivalent timer, not a real fluid
    // store (the reagent tank keeps holding the real fluid the whole time), so switching modes or
    // swapping the input item can just reset it to 0 with nothing to refund.
    private int fillBudget = 0;

    // Tracks whether the machine has bottomed out at 0 HP and not yet fully recovered -- fill is
    // blocked for the whole "recovering from 0" window, not just while HP == 0 (see setMode/tickFill
    // and the machine notes' HP-gated fill rule). Distinct from ordinary partial-HP recovery
    // (health < maxHealth but never hit 0), which only slows fill rather than blocking it.
    private boolean recoveringFromZero = false;

    // ---- Evolution (installed Evolution Module -> eventual Charred Mutator) ------------------
    // Mirrors Masticator/Metastasizer/Effluentcer's own evolution mechanic -- installing an
    // Evolution Module here grants its hazard tolerance IMMEDIATELY (see installedHazardProfile()),
    // and separately accumulates evolutionProgress toward eventually transforming this block into an
    // actual placed Charred Mutator at MachineTier.CHARRED's own faster speed/doubled capacity. Only
    // MUTATE mode's completed crafts count -- FILL mode never drives the base engine's recipe/
    // progress system (see the mode-branching note below), so it makes no evolution progress either.
    private int evolutionProgress = 0;
    private int flourishCyclesRemaining = -1;
    private static final int FLOURISH_DURATION_CYCLES = 4; // 4 * CRAFT_TICKS(10) = 40 ticks, ~2s

    // Which screen tab was last open -- same pattern as every other Module-tab machine, so
    // reopening the screen returns to the tab last viewed.
    private boolean moduleTabActive = false;

    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    public void setModuleTabActive(boolean active) {
        this.moduleTabActive = active;
    }

    public MutatorBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.MUTATOR_BE.get(), pos, blockState);
    }

    // Lets a capability-leap subclass (Charred Mutator) register under its own BlockEntityType
    // while reusing everything else this class provides -- see MachineTier's own javadoc on why a
    // genuine capability leap (here: hazard-tolerant tank) is a hook override, not a new MachineTier
    // constant.
    protected MutatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected RecipeType<MutatingRecipe> getRecipeType() {
        return ModRecipes.MUTATING_TYPE.get();
    }

    public VulnerableTank getReagentTank() {
        return REAGENT_TANK;
    }

    public Mode getMode() {
        return mode;
    }

    /** Server-side mode toggle, invoked from {@code MutatorModeClickPayload}. */
    public void toggleMode() {
        setMode(mode == Mode.MUTATE ? Mode.FILL : Mode.MUTATE);
    }

    public void setMode(Mode mode) {
        if (this.mode == mode) return;
        this.mode = mode;
        fillBudget = 0;
        resetActiveRecipe();
        // Switching back to MUTATE with valid contents already in place: re-resolve immediately
        // rather than waiting for the next inventory/tank change to trigger it.
        if (mode == Mode.MUTATE) resolveRecipe();
        setChanged();
        updateBlock();
    }

    public FluidStack getFluid(int slot) {
        if (slot == FUEL_TANK.SLOT) return FUEL_TANK.getFluid();
        else if (slot == REAGENT_TANK.SLOT) return REAGENT_TANK.getFluid();
        else return FluidStack.EMPTY;
    }

    /** See {@link IHasChannels#describeFluidFace} -- mirrors {@link #getTank} literally. */
    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable(face == Direction.UP
                ? "tooltip.dermicraft.tank.fuel" : "tooltip.dermicraft.tank.reagent");
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == Direction.UP) return FUEL_TANK;
        return REAGENT_TANK;
    }

    /** See {@link IHasChannels#describeFace} -- mirrors {@link #getTank}/{@link #getItemHandler} literally. */
    @Override
    public Component describeFace(Direction face) {
        return switch (face) {
            case UP -> Component.translatable("tooltip.dermicraft.idep.face.mutator_fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.idep.face.mutator_output");
            default -> Component.translatable("tooltip.dermicraft.idep.face.mutator_input");
        };
    }

    // Face routing: top = fuel (fluid + its bucket slot), bottom = result slot only,
    // sides = both the reagent fluid (via getTank's fluid capability) and the input item
    // (via this item capability) -- letting a hopper feed the input while a pipe fills the
    // reagent tank from the same side faces.
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;

        int targetSlot = switch (direction) {
            case UP -> FUEL_TANK.SLOT;
            case DOWN -> OUTPUT_SLOT;
            default -> INPUT_SLOT;
        };
        return new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                INVENTORY.setStackInSlot(targetSlot, stack);
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            @NotNull
            public ItemStack getStackInSlot(int slot) {
                return INVENTORY.getStackInSlot(targetSlot);
            }

            @Override
            @NotNull
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                // The output slot is machine-produced only -- never accept automation inserts there.
                if (targetSlot == OUTPUT_SLOT) return stack;
                return INVENTORY.insertItem(targetSlot, stack, simulate);
            }

            @Override
            @NotNull
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return INVENTORY.extractItem(targetSlot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return INVENTORY.getSlotLimit(targetSlot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return targetSlot != OUTPUT_SLOT && INVENTORY.isItemValid(targetSlot, stack);
            }
        };
    }

    /**
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}. Mirrors the
     * Metastasizer's scheme exactly (fuel = UP, reagent = everything except UP, input = sides,
     * output = DOWN), fuel listed first per the mod's starvation-risk-first channel-priority
     * convention.
     */
    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.UP)) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID,
                Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            channels.add(new Channel.FluidChannel("reagent", Component.literal("Reagent"), Channel.IO.IN, REAGENT_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM,
                Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            channels.add(new Channel.ItemChannel("input", Component.literal("Input"), Channel.IO.IN, singleSlotHandler(INPUT_SLOT, true)));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM, Direction.DOWN)) {
            channels.add(new Channel.ItemChannel("output", Component.literal("Output"), Channel.IO.OUT, singleSlotHandler(OUTPUT_SLOT, false)));
        }

        return channels;
    }

    /** A single-slot view of INVENTORY, insert-gated by {@code allowInsert} -- mirrors the OUTPUT_SLOT
     * "machine-produced only, never accept automation inserts" rule already enforced in getItemHandler. */
    private IItemHandler singleSlotHandler(int slot, boolean allowInsert) {
        return new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int i, ItemStack stack) {
                INVENTORY.setStackInSlot(slot, stack);
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int i) {
                return INVENTORY.getStackInSlot(slot);
            }

            @NotNull
            @Override
            public ItemStack insertItem(int i, ItemStack stack, boolean simulate) {
                if (!allowInsert) return stack;
                return INVENTORY.insertItem(slot, stack, simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int i, int amount, boolean simulate) {
                return INVENTORY.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int i) {
                return INVENTORY.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int i, ItemStack stack) {
                return allowInsert && INVENTORY.isItemValid(slot, stack);
            }
        };
    }

    // Direct right-click helpers, mirroring the same face routing as getItemHandler:
    // sides expose the input slot, bottom exposes the (pull-only) result slot.
    public ItemStack insertInput(ItemStack stack) {
        return insertItemStack(INVENTORY, INPUT_SLOT, stack);
    }

    public ItemStack extractInput() {
        return extractItemStack(INVENTORY, INPUT_SLOT);
    }

    public ItemStack extractResult() {
        return extractItemStack(INVENTORY, OUTPUT_SLOT);
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
        super.drops(MODULE_INVENTORY);
    }

    @Override
    protected void drainOutputs(Level level) {
        if (autoDrainEnabled && !INVENTORY.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            ModItemUtil.pushItemToBelowNeighbour(level, worldPosition, INVENTORY, OUTPUT_SLOT);
        }
    }

    // ---- Mode-branching engine hooks -----------------------------------------------------------
    //
    // FILL mode deliberately does NOT impersonate an active recipe to the base engine (an earlier
    // version overrode isRecipeValid() to return true in FILL mode, which also lied to the base
    // class's saveAdditional() guard -- `activeRecipe.id()` NPE on save). Instead, fill runs from
    // the tickHealing() hook (see the visual-state section below), which the base tick() calls
    // unconditionally every cycle; in FILL mode activeRecipe simply stays null and the base
    // recipe branch idles honestly.

    @Override
    protected boolean hasCraftingInputs() {
        if (mode == Mode.FILL) return hasFillCandidate(); // only reached via computeVisualState()
        return hasInput() && hasEnoughReagent();
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        if (mode == Mode.FILL) return true; // only reached via computeVisualState(); fill leaves the item in place if OUTPUT_SLOT is occupied
        return hasOutputRoom();
    }

    @Override
    protected void damageMachine(int amount) {
        super.damageMachine(amount);
        if (isStarved()) recoveringFromZero = true;
    }

    @Override
    protected void healMachine(int amount) {
        super.healMachine(amount);
        if (health >= maxHealth) recoveringFromZero = false;
    }

    // ---- Visual state (face texture) -------------------------------------------------------------
    // tickHealing() is the one hook the base tick() calls unconditionally every cycle regardless of
    // crafting state, so it doubles as the visual-state refresh point. Recovering (health < maxHealth)
    // takes priority over Running -- a damaged machine signals distress even mid-cycle.
    //
    // Debounced: a new state must be observed for VISUAL_STATE_STABLE_CYCLES consecutive cycles
    // (2 cycles = ~1s at CRAFT_TICKS=10) before the texture commits. Borderline conditions can
    // otherwise strobe -- e.g. a hopper feeding the input every 8 ticks against the 10-tick cycle
    // flaps RUNNING/IDLE -- and every commit is a setBlock (client update + chunk re-render), so
    // flapping is also wasted churn. Matching the current state resets any pending change, so a
    // one-cycle blip never flashes the face. Transient, deliberately not saved to NBT.

    private static final int VISUAL_STATE_STABLE_CYCLES = 2;

    @Nullable
    private MutatorVisualState pendingVisualState = null;
    private int pendingVisualCycles = 0;

    @Override
    protected boolean tickHealing(boolean fueled) {
        boolean healed = super.tickHealing(fueled);
        // FILL mode's work happens here rather than through the recipe branch -- tickHealing is
        // called unconditionally every cycle, and fill has no recipe for the base engine to track
        // (see the mode-branching note above). tickFill() gates itself on HP (fillBlocked()).
        if (mode == Mode.FILL) {
            tickFill(healed);
        }
        updateVisualState();
        tickEvolutionFlourish();
        return healed;
    }

    private void updateVisualState() {
        if (level == null) return;

        MutatorVisualState computed = computeVisualState();
        BlockState state = getBlockState();

        if (state.getValue(MutatorBlock.STATE) == computed) {
            pendingVisualState = null;
            pendingVisualCycles = 0;
            return;
        }

        if (pendingVisualState != computed) {
            pendingVisualState = computed;
            pendingVisualCycles = 1;
            return;
        }

        if (++pendingVisualCycles >= VISUAL_STATE_STABLE_CYCLES) {
            level.setBlock(worldPosition, state.setValue(MutatorBlock.STATE, computed), Block.UPDATE_CLIENTS);
            pendingVisualState = null;
            pendingVisualCycles = 0;
        }
    }

    private MutatorVisualState computeVisualState() {
        if (maxHealth > 0 && health < maxHealth) return MutatorVisualState.RECOVERING;
        if (!isStarved() && hasCraftingInputs() && hasCraftingOutputRoom()) return MutatorVisualState.RUNNING;
        return MutatorVisualState.IDLE;
    }

    /** Runs once per CRAFT_TICKS cycle (this class's natural cadence, same one visual-state/healing
     * already use) -- mirrors Masticator/Metastasizer/Effluentcer's identical mechanism. */
    private void tickEvolutionFlourish() {
        if (flourishCyclesRemaining < 0) return;

        if (level instanceof ServerLevel serverLevel) {
            spawnFlourishParticles(serverLevel, flourishCyclesRemaining);
        }

        flourishCyclesRemaining--;
        if (flourishCyclesRemaining < 0) {
            completeEvolution(level);
        }
    }

    private void startEvolutionFlourish() {
        flourishCyclesRemaining = FLOURISH_DURATION_CYCLES;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    private void spawnFlourishParticles(ServerLevel serverLevel, int cyclesRemaining) {
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;

        float progress = 1f - (cyclesRemaining / (float) FLOURISH_DURATION_CYCLES);
        int dustCount = 8 + Math.round(progress * 16);
        DustParticleOptions dust = tintedDust();
        serverLevel.sendParticles(dust, cx, cy, cz, dustCount, 0.3, 0.3, 0.3, 0.03);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 3 + Math.round(progress * 8), 0.35, 0.35, 0.35, 0.02);

        if (cyclesRemaining == 0) {
            serverLevel.sendParticles(dust, cx, cy, cz, 30, 0.5, 0.5, 0.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 16, 0.5, 0.5, 0.5, 0.04);
            serverLevel.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private DustParticleOptions tintedDust() {
        int tint = 0xFFCF4B12; // fallback: lava-orange, matches Thermal's own target fluid
        Optional<net.minecraft.world.level.material.Fluid> targetFluid = installedEvolutionProperties()
                .flatMap(EvolutionModuleProperties::targetFluid);
        if (targetFluid.isPresent() && targetFluid.get().getFluidType() instanceof BaseFluidType baseType) {
            tint = baseType.getTintColor();
        }
        return new DustParticleOptions(new Vector3f(
                ((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F), 1.4F);
    }

    /**
     * Transforms this block into a Charred Mutator in place, carrying over the input/output items,
     * current mode, and fuel/reagent tank contents -- but deliberately NOT the recipe-in-progress
     * bookkeeping (active recipe/craft progress) or fill-mode's own rate-timer/HP-recovery-window
     * state, same reasoning as Masticator/Metastasizer/Effluentcer's own completeEvolution: a
     * half-finished cycle just restarts cleanly on the new block instead. The Module itself is not
     * carried over -- this transform is what consumes it.
     */
    private void completeEvolution(Level level) {
        ItemStack inputItem = INVENTORY.getStackInSlot(INPUT_SLOT);
        ItemStack outputItem = INVENTORY.getStackInSlot(OUTPUT_SLOT);
        FluidStack fuelContents = FUEL_TANK.getFluid().copy();
        FluidStack reagentContents = REAGENT_TANK.getFluid().copy();
        Mode currentMode = mode;
        BlockState oldState = getBlockState();

        // Clear this instance's own contents BEFORE the block swap -- the block's own onRemove drops
        // whatever's still in INVENTORY when the block itself changes, which would otherwise
        // duplicate everything captured above once it's handed to the new instance.
        INVENTORY.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            MODULE_INVENTORY.setStackInSlot(slot, ItemStack.EMPTY);
        }
        if (!fuelContents.isEmpty()) FUEL_TANK.drain(fuelContents.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (!reagentContents.isEmpty()) REAGENT_TANK.drain(reagentContents.getAmount(), IFluidHandler.FluidAction.EXECUTE);

        BlockState newState = ModBlocks.CHARRED_MUTATOR.get().defaultBlockState()
                .setValue(MutatorBlock.FACING, oldState.getValue(MutatorBlock.FACING));
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);

        if (level.getBlockEntity(worldPosition) instanceof MutatorBlockEntity charred) {
            charred.INVENTORY.setStackInSlot(INPUT_SLOT, inputItem);
            charred.INVENTORY.setStackInSlot(OUTPUT_SLOT, outputItem);
            if (!fuelContents.isEmpty()) charred.FUEL_TANK.fill(fuelContents, IFluidHandler.FluidAction.EXECUTE);
            if (!reagentContents.isEmpty()) charred.REAGENT_TANK.fill(reagentContents, IFluidHandler.FluidAction.EXECUTE);
            charred.mode = currentMode;
            charred.setChanged();
            charred.updateBlock();
        }
    }

    /** Whether this instance can still evolve at all -- true for the base Mutator, overridden to
     * false by {@code CharredMutatorBlockEntity} (already evolved; installing an Evolution Module
     * there does nothing, since its tank is permanently hazard-tolerant regardless of any Module,
     * and there's nothing further for it to transform into). */
    protected boolean canEvolve() {
        return true;
    }

    /** Union of this machine's base (TIER_1) hazard tolerance with whatever the installed Module
     * grants -- a Safety Module (via the shared {@link IHaveModules} helper) or an Evolution Module
     * (read directly here, since {@code IHaveModules} only knows about Safety Modules). Recomputed
     * on demand, not cached, matching every other consumer of this data map. */
    protected HazardProfile installedHazardProfile() {
        HazardProfile profile = HazardProfile.TIER_1;
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            ItemStack module = MODULE_INVENTORY.getStackInSlot(slot);
            profile = IHaveModules.installedHazardProfile(profile, module);

            if (canEvolve() && !module.isEmpty()) {
                EvolutionModuleProperties evoProps = BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                        .getData(ModDataMaps.EVOLUTION_MODULE_PROPERTIES);
                if (evoProps != null) {
                    for (var hazard : evoProps.hazards()) {
                        profile = profile.plus(hazard);
                    }
                }
            }
        }
        return profile;
    }

    /** Work Speed Module bonus across this machine's Module slot(s) -- see
     * IHaveModules#workSpeedMultiplier for the diminishing-return stacking rule. */
    @Override
    protected float workSpeedMultiplier() {
        List<ItemStack> modules = new ArrayList<>();
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            modules.add(MODULE_INVENTORY.getStackInSlot(slot));
        }
        return IHaveModules.workSpeedMultiplier(modules);
    }

    /** 0 when not evolving at all (no Module, one with no real Evolution properties, or already a
     * Charred Mutator); otherwise how far {@link #evolutionProgress} is toward
     * {@code evolutionThreshold}, 0-1. Public purely for {@code EvolutionOverlayBlockEntityRenderer}'s
     * creeping overlay -- mirrors Masticator/Metastasizer/Effluentcer's identical method. */
    @Override
    public float getEvolutionProgressFraction() {
        return installedEvolutionProperties()
                .map(props -> Math.min(1f, evolutionProgress / (float) props.evolutionThreshold()))
                .orElse(0f);
    }

    /** Empty unless the Module slot holds an item with real {@code EvolutionModuleProperties} data
     * AND {@link #canEvolve()} -- a plain {@code MODULES}-tagged item with no such data (or any
     * Module at all once this is already a Charred Mutator) is inert here. */
    private Optional<EvolutionModuleProperties> installedEvolutionProperties() {
        if (!canEvolve()) return Optional.empty();
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            ItemStack module = MODULE_INVENTORY.getStackInSlot(slot);
            if (module.isEmpty()) continue;
            EvolutionModuleProperties props = BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                    .getData(ModDataMaps.EVOLUTION_MODULE_PROPERTIES);
            if (props != null) return Optional.of(props);
        }
        return Optional.empty();
    }

    /** Called whenever any Module slot's contents change at all -- full reset, matching every other
     * Evolution Module consumer's "pulling the Module wipes all progress" rule. */
    @Override
    protected void onModuleSlotChanged(int slot) {
        evolutionProgress = 0;
    }

    // ---- Mutate mode (recipe-driven, mirrors the Metastasizer's resolve/complete shape) ---------

    @Override
    protected void onCraftComplete() {
        // Captured before draining: useFluid() fires REAGENT_TANK.onContentsChanged()
        // synchronously, which re-resolves the recipe and can clear requiredFluid/
        // cachedResult before this method finishes.
        int amount = requiredFluid;
        ItemStack output = cachedResult.copy();
        int completedTicks = maxProgress;

        REAGENT_TANK.useFluid(amount);
        INVENTORY.extractItem(INPUT_SLOT, 1, false); // the input item IS consumed (unlike the Metastasizer's pattern)
        INVENTORY.insertItem(OUTPUT_SLOT, output, false);

        installedEvolutionProperties().ifPresent(props -> {
            evolutionProgress += completedTicks;
            if (evolutionProgress >= props.evolutionThreshold() && flourishCyclesRemaining < 0) {
                startEvolutionFlourish();
            }
        });
    }

    // Restores the cached result/fluid amount when a saved recipe is reloaded from NBT.
    @Override
    protected void onRecipeResolved(RecipeHolder<MutatingRecipe> recipe) {
        this.cachedResult = recipe.value().getResult();
        this.requiredFluid = recipe.value().getFluidAmount();
    }

    // protected (not private) so a capability-leap subclass (Charred Mutator) can call it directly
    // from its own overridden createReagentTank() -- see EffluentcerBlockEntity's identical split
    // for the same reason.
    protected void resolveRecipe() {
        if (mode == Mode.FILL) return;

        Optional<RecipeHolder<MutatingRecipe>> opt = getRecipeOptional();
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
            this.maxProgress = activeRecipe.value().getCraftingTime();
            this.cachedResult = activeRecipe.value().getResult();
            this.requiredFluid = activeRecipe.value().getFluidAmount();
        } else {
            resetActiveRecipe();
        }
    }

    private Optional<RecipeHolder<MutatingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getRecipeFor(ModRecipes.MUTATING_TYPE.get(),
                new OneFluidOneItemRecipeInput(INVENTORY.getStackInSlot(INPUT_SLOT), REAGENT_TANK.getFluid()), this.level);
    }

    public void resetActiveRecipe() {
        activeRecipe = null;
        resetMaxProgress();
        resetProgress();
        cachedResult = ItemStack.EMPTY;
        requiredFluid = 0;
    }

    private boolean hasInput() {
        return !INVENTORY.getStackInSlot(INPUT_SLOT).isEmpty();
    }

    private boolean hasEnoughReagent() {
        return REAGENT_TANK.hasEnoughFluid(requiredFluid);
    }

    private boolean hasOutputRoom() {
        if (cachedResult.isEmpty()) return false;
        return INVENTORY.insertItem(OUTPUT_SLOT, cachedResult.copy(), true).isEmpty();
    }

    // ---- Fill mode (generic capability operation -- no recipe system involved) -------------------
    //
    // Rate-limited (FILL_RATE mB/cycle, 0.1x while recovering from partial HP, blocked entirely at
    // HP 0 or while recovering from a past 0 -- see damageMachine/healMachine and fillBlocked()).
    // Every container fills through the same budget flow: probe what a SINGLE unit of the input
    // stack would accept, accumulate a rate-equivalent "budget" across cycles until it covers that
    // amount, then complete the fill in one atomic action -- verify (simulate fill + simulate output
    // insert) BEFORE any fluid moves, drain, move the filled unit to the output, shrink the input
    // stack by 1. Rigid containers (bucket/bottle-style, exact-volume only) and flexible ones both
    // ride the same path; the budget just covers different amounts. The reagent tank holds the real
    // fluid the whole time (nothing is set aside early), so the budget is just a timer, not a real
    // fluid store -- toggling modes or swapping the input item can reset it to 0 with nothing to
    // refund.

    private boolean hasFillCandidate() {
        ItemStack target = INVENTORY.getStackInSlot(INPUT_SLOT);
        return !target.isEmpty() && !REAGENT_TANK.isEmpty()
                && target.getCapability(Capabilities.FluidHandler.ITEM) != null;
    }

    private boolean fillBlocked() {
        return isStarved() || recoveringFromZero;
    }

    private void tickFill(boolean healedThisCycle) {
        if (fillBlocked()) return;

        ItemStack target = INVENTORY.getStackInSlot(INPUT_SLOT);
        if (target.isEmpty() || REAGENT_TANK.isEmpty()) {
            fillBudget = 0;
            return;
        }

        // Always operate on a SINGLE unit off the (possibly stacked) input. Filling mutates the
        // item's data, so the whole stack must never be replaced with the fill result -- an earlier
        // version overwrote the input stack with the single filled item, silently destroying the
        // rest of a stack of empties. One unit fills (via the budget below), then that unit alone
        // moves to the output and the input stack shrinks by 1; the next unit starts fresh.
        //
        // `required` is probed against a synthetic, effectively-unlimited sample of the tank's fluid
        // TYPE -- never the tank's current amount. Probing with the current amount was a real bug:
        // a flexible container would report its available (partial) room as `required` whenever the
        // tank ran low, so the fill would "complete" and ship out a container that isn't actually
        // full; a rigid container would instead reject the too-small sample outright and get pushed
        // through the output UNFILLED, misread as "wrong fluid." Both were the same root cause. The
        // fix always waits for a genuinely FULL fill -- if the player wants to drain the reagent tank
        // to something less than full, the tank's own bucket slot is the tool for that, not this slot.
        ItemStack single = target.copyWithCount(1);
        FluidStack fullProbe = new FluidStack(REAGENT_TANK.getFluid().getFluid(), Integer.MAX_VALUE);
        int required = simulateFill(single, fullProbe);
        if (required <= 0) {
            // Wrong fluid, or the container is already full -- pass one unit through to the output
            // (when there's room) so a pre-filled/mismatched item can't jam the input slot forever.
            fillBudget = 0;
            passThroughToOutput();
            return;
        }

        int rate = Math.round(FILL_RATE * (healedThisCycle ? RECOVERY_SPEED_FACTOR : 1f));
        if (rate <= 0) return;

        fillBudget += rate;
        setChanged();
        if (fillBudget < required) return;
        if (REAGENT_TANK.getFluidAmount() < required) return; // budget's ready, but the tank itself doesn't hold enough yet -- keep waiting

        // Budget AND tank both cover the full requirement. Verify the whole completion BEFORE any
        // fluid actually moves: simulate the fill, then simulate the output insert -- if either fails
        // (tank drained by something else this same tick, output blocked), hold with the budget
        // pinned and retry next cycle. Nothing is drained or lost while holding.
        FluidActionResult simulated = FluidUtil.tryFillContainer(single, REAGENT_TANK, required, null, false);
        if (!simulated.isSuccess()) return;
        if (!INVENTORY.insertItem(OUTPUT_SLOT, simulated.getResult(), true).isEmpty()) return;

        FluidActionResult filled = FluidUtil.tryFillContainer(single, REAGENT_TANK, required, null, true);
        if (!filled.isSuccess()) return;

        fillBudget = 0;
        INVENTORY.extractItem(INPUT_SLOT, 1, false);
        INVENTORY.insertItem(OUTPUT_SLOT, filled.getResult(), false);
        setChanged();
        updateBlock();
    }

    /** Moves one unit of the input stack to the output unchanged (fill can't act on it). Only fires
     * when the output can take it; otherwise the item just waits in the input slot. */
    private void passThroughToOutput() {
        ItemStack single = INVENTORY.getStackInSlot(INPUT_SLOT).copyWithCount(1);
        if (INVENTORY.insertItem(OUTPUT_SLOT, single, true).isEmpty()) {
            INVENTORY.extractItem(INPUT_SLOT, 1, false);
            INVENTORY.insertItem(OUTPUT_SLOT, single, false);
            setChanged();
            updateBlock();
        }
    }

    private int simulateFill(ItemStack target, FluidStack sample) {
        IFluidHandlerItem handler = target.copy().getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return 0;
        return handler.fill(sample, IFluidHandler.FluidAction.SIMULATE);
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.MUTATOR);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MutatorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("module_inventory", MODULE_INVENTORY.serializeNBT(registries));
        tag.put("reagent", REAGENT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("requiredFluid", requiredFluid);
        tag.putInt("mode", mode.ordinal());
        tag.putInt("fillBudget", fillBudget);
        tag.putBoolean("recoveringFromZero", recoveringFromZero);
        tag.putBoolean("module_tab_active", moduleTabActive);
        tag.putInt("evolution_progress", evolutionProgress);
        tag.putInt("evolution_flourish_cycles", flourishCyclesRemaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag oldInventoryTag = tag.getCompound("inventory");
        if (tag.contains("inventory")) {
            // Worlds saved before INPUT_SLOT/OUTPUT_SLOT existed have a smaller Size -- see
            // MachineBaseBlockEntity#loadItemHandler for why a plain deserializeNBT would shrink
            // INVENTORY back down and crash the menu (slot out of range).
            loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, oldInventoryTag);
        }

        if (tag.contains("module_inventory")) {
            loadItemHandler(MODULE_INVENTORY, moduleSlotCount(), registries, tag.getCompound("module_inventory"));
        } else {
            // Pre-split save: the Module item was the old combined INVENTORY's trailing slot
            // (index 4, back when INVENTORY_SIZE was 5) -- see
            // MachineBaseBlockEntity#extractLegacyModuleStack.
            ItemStack legacyModule = extractLegacyModuleStack(registries, oldInventoryTag, 4);
            if (!legacyModule.isEmpty()) MODULE_INVENTORY.setStackInSlot(0, legacyModule);
        }

        if (tag.contains("reagent")) REAGENT_TANK.readFromNBT(registries, tag.getCompound("reagent"));
        requiredFluid = tag.getInt("requiredFluid");
        mode = tag.getInt("mode") == Mode.FILL.ordinal() ? Mode.FILL : Mode.MUTATE;
        fillBudget = tag.getInt("fillBudget");
        recoveringFromZero = tag.getBoolean("recoveringFromZero");
        moduleTabActive = tag.getBoolean("module_tab_active");
        evolutionProgress = tag.getInt("evolution_progress");
        flourishCyclesRemaining = tag.contains("evolution_flourish_cycles")
                ? tag.getInt("evolution_flourish_cycles") : -1;
    }

    private ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

                if (isTransferringFluids) return;

                if (slot == INPUT_SLOT && mode == Mode.MUTATE) resolveRecipe();

                biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);
                biDirectionalFluidTransfer(REAGENT_TANK, REAGENT_TANK.SLOT);

                isTransferringFluids = false;

                setChanged();
                updateBlock();
            }

            private void biDirectionalFluidTransfer(ModFluidTank tank, int tankSlot) {
                if (tank.hasFluidHandlerInSlot(this, tankSlot)) {
                    isTransferringFluids = true;
                    tank.transferFluidToTank(this, tankSlot);
                } else if (tank.hasEmptyFluidHandlerInSlot(this, tankSlot)) {
                    isTransferringFluids = true;
                    tank.transferFluidFromTankToHandler(this, tankSlot);
                }
            }

            // Fuel/reagent tank slots hold exactly one container at a time, unconditionally -- not
            // just once already occupied (the old `hasEmptyFluidHandlerInSlot` condition inspected
            // the slot's CURRENT contents, which never fires on a slot that starts empty, so a whole
            // stack of empty containers could be inserted in one shot; the auto-fill transfer then
            // only ever fills and returns a single container, silently collapsing the rest of the
            // stack -- the exact bug the fuel/reagent slots shared with every other machine's tank
            // slots). INPUT_SLOT deliberately stays stack-capable regardless of item type: FILL mode
            // processes a whole stack of containers one unit at a time via its own budget/pass-through
            // logic (see tickFill), and MUTATE mode's ingredient slot follows the Masticator's
            // convention of allowing a stack even though only one item is consumed per cycle.
            @Override
            public int getSlotLimit(int slot) {
                if (slot == FUEL_TANK.SLOT || slot == REAGENT_TANK.SLOT) return 1;
                return super.getSlotLimit(slot);
            }
        };
    }

    protected VulnerableTank createReagentTank() {
        return new VulnerableTank(getTier().tankCapacity(), 1, this::installedHazardProfile) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    if (mode == Mode.MUTATE) resolveRecipe();
                    setChanged();
                }
            }
        };
    }
}
