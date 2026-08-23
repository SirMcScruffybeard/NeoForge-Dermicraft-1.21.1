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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.MetastasizerBlock;
import net.scruffy.dermicraft.block.custom.MetastasizerVisualState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.BaseFluidType;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.property.EvolutionModuleProperties;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.metastasizing.MetastasizingRecipe;
import net.scruffy.dermicraft.screen.custom.metastasizer.MetastasizerMenu;
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

public class MetastasizerBlockEntity extends AbstractFueledMachineBlockEntity<MetastasizingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels {

    public static final int PATTERN_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    // Module slot -- same tab-gated pattern as MasticatorBlockEntity's own, declared here so
    // Charred Metastasizer inherits it for free.
    public static final int MODULE = 4;
    public static final int INVENTORY_SIZE = 5;

    private final VulnerableTank REAGENT_TANK = createReagentTank();

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createInventory(INVENTORY_SIZE);

    private ItemStack cachedResult = ItemStack.EMPTY;
    private int requiredFluid = 0;

    private boolean moduleTabActive = false;

    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    public void setModuleTabActive(boolean active) {
        this.moduleTabActive = active;
    }

    // ---- Evolution (installed Evolution Module -> eventual Charred Metastasizer) --------------
    // Mirrors MasticatorBlockEntity's identical mechanic -- see that class's own comment for the
    // full rationale.
    private int evolutionProgress = 0;
    private int flourishCyclesRemaining = -1;
    private static final int FLOURISH_DURATION_CYCLES = 4; // 4 * CRAFT_TICKS(10) = 40 ticks, ~2s

    public MetastasizerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.METASTASIZER_BE.get(), pos, blockState);
    }

    // Lets a capability-leap subclass (e.g. Charred Metastasizer) register under its own
    // BlockEntityType while reusing everything else this class provides -- same pattern as
    // MasticatorBlockEntity's identical overload.
    protected MetastasizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected RecipeType<MetastasizingRecipe> getRecipeType() {
        return ModRecipes.METASTASIZING_TYPE.get();
    }

    // ---- Visual state (face texture) --------------------------------------------------------
    // tickHealing runs every cycle regardless of crafting state, so it doubles as the visual-state
    // refresh point. Recovering (health < maxHealth) takes priority over Running -- a damaged
    // machine signals distress even mid-cycle. Mirrors MutatorBlockEntity/MasticatorBlockEntity/
    // EffluentcerBlockEntity's identical mechanism.
    //
    // Debounced: a new state must be observed for VISUAL_STATE_STABLE_CYCLES consecutive cycles
    // (2 cycles = ~1s at CRAFT_TICKS=10) before the texture commits, to avoid flicker on borderline
    // conditions; every commit is a setBlock (client update + chunk re-render), so flapping is also
    // wasted churn. Transient, deliberately not saved to NBT.
    //
    // Unlike the Masticator/Effluentcer, hasCraftingOutputRoom() (hasOutputRoom()) already gates on
    // cachedResult being non-empty, which is only populated by a genuinely resolved recipe -- so it
    // isn't vacuously true while idle. The isRecipeValid check is kept anyway for defensive
    // consistency with the other machines' visual-state hooks.
    private static final int VISUAL_STATE_STABLE_CYCLES = 2;

    @Nullable
    private MetastasizerVisualState pendingVisualState = null;
    private int pendingVisualCycles = 0;

    @Override
    protected boolean tickHealing(boolean fueled) {
        boolean healed = super.tickHealing(fueled);
        updateVisualState();
        tickEvolutionFlourish();
        return healed;
    }

    /** Runs once per CRAFT_TICKS cycle -- see MasticatorBlockEntity's identical method for the
     * full rationale (density ramps up, block swap fires in the densest part of the burst). */
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
     * Transforms this block into a Charred Metastasizer in place, carrying over fuel/reagent tank
     * contents and the pattern/output item slots -- but deliberately NOT the recipe-in-progress
     * bookkeeping (active recipe, craft progress, cached result), same reasoning as
     * MasticatorBlockEntity#completeEvolution: a half-finished cycle just restarts cleanly on the
     * new block instead. The Module itself is not carried over -- this transform is what consumes it.
     */
    private void completeEvolution(Level level) {
        ItemStack patternItem = INVENTORY.getStackInSlot(PATTERN_SLOT);
        ItemStack outputItem = INVENTORY.getStackInSlot(OUTPUT_SLOT);
        FluidStack fuelContents = FUEL_TANK.getFluid().copy();
        FluidStack reagentContents = REAGENT_TANK.getFluid().copy();
        BlockState oldState = getBlockState();

        INVENTORY.setStackInSlot(PATTERN_SLOT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(MODULE, ItemStack.EMPTY);
        if (!fuelContents.isEmpty()) FUEL_TANK.drain(fuelContents.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (!reagentContents.isEmpty()) REAGENT_TANK.drain(reagentContents.getAmount(), IFluidHandler.FluidAction.EXECUTE);

        BlockState newState = ModBlocks.CHARRED_METASTASIZER.get().defaultBlockState()
                .setValue(MetastasizerBlock.FACING, oldState.getValue(MetastasizerBlock.FACING));
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);

        if (level.getBlockEntity(worldPosition) instanceof MetastasizerBlockEntity charred) {
            charred.INVENTORY.setStackInSlot(PATTERN_SLOT, patternItem);
            charred.INVENTORY.setStackInSlot(OUTPUT_SLOT, outputItem);
            if (!fuelContents.isEmpty()) charred.FUEL_TANK.fill(fuelContents, IFluidHandler.FluidAction.EXECUTE);
            if (!reagentContents.isEmpty()) charred.REAGENT_TANK.fill(reagentContents, IFluidHandler.FluidAction.EXECUTE);
            charred.setChanged();
            charred.updateBlock();
        }
    }

    /** Whether this instance can still evolve at all -- true for the base Metastasizer, overridden
     * to false by {@link CharredMetastasizerBlockEntity} (already evolved). */
    protected boolean canEvolve() {
        return true;
    }

    /** Union of this machine's base (TIER_1) hazard tolerance with whatever the installed Module
     * grants -- see MasticatorBlockEntity#installedHazardProfile for the full rationale. */
    protected HazardProfile installedHazardProfile() {
        ItemStack module = INVENTORY.getStackInSlot(MODULE);
        HazardProfile profile = IHaveModules.installedHazardProfile(HazardProfile.TIER_1, module);

        if (canEvolve() && !module.isEmpty()) {
            EvolutionModuleProperties evoProps = BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                    .getData(ModDataMaps.EVOLUTION_MODULE_PROPERTIES);
            if (evoProps != null) {
                for (var hazard : evoProps.hazards()) {
                    profile = profile.plus(hazard);
                }
            }
        }
        return profile;
    }

    /** Empty unless the Module slot holds an item with real {@code EvolutionModuleProperties} data
     * AND {@link #canEvolve()}. */
    private Optional<EvolutionModuleProperties> installedEvolutionProperties() {
        if (!canEvolve()) return Optional.empty();
        ItemStack module = INVENTORY.getStackInSlot(MODULE);
        if (module.isEmpty()) return Optional.empty();
        return Optional.ofNullable(
                BuiltInRegistries.ITEM.wrapAsHolder(module.getItem()).getData(ModDataMaps.EVOLUTION_MODULE_PROPERTIES));
    }

    /** Called whenever the Module slot's contents change at all -- full reset, matching
     * MasticatorBlockEntity's identical rule. */
    protected void onModuleChanged() {
        evolutionProgress = 0;
    }

    private void updateVisualState() {
        if (level == null) return;

        MetastasizerVisualState computed = computeVisualState();
        BlockState state = getBlockState();

        if (state.getValue(MetastasizerBlock.STATE) == computed) {
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
            level.setBlock(worldPosition, state.setValue(MetastasizerBlock.STATE, computed), Block.UPDATE_CLIENTS);
            pendingVisualState = null;
            pendingVisualCycles = 0;
        }
    }

    private MetastasizerVisualState computeVisualState() {
        if (maxHealth > 0 && health < maxHealth) return MetastasizerVisualState.RECOVERING;
        if (!isStarved() && isRecipeValid(activeRecipe) && hasCraftingInputs() && hasCraftingOutputRoom()) {
            return MetastasizerVisualState.RUNNING;
        }
        return MetastasizerVisualState.IDLE;
    }

    public VulnerableTank getReagentTank() {
        return REAGENT_TANK;
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
            case UP -> Component.translatable("tooltip.dermicraft.idep.face.metastasizer_fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.idep.face.metastasizer_output");
            default -> Component.translatable("tooltip.dermicraft.idep.face.metastasizer_pattern");
        };
    }

    // Face routing: top = fuel (fluid + its bucket slot), bottom = result slot only,
    // sides = both the reagent fluid (via getTank's fluid capability) and the pattern item
    // (via this item capability) -- letting a hopper feed the pattern while a pipe fills the
    // reagent tank from the same side faces.
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;

        int targetSlot = switch (direction) {
            case UP -> FUEL_TANK.SLOT;
            case DOWN -> OUTPUT_SLOT;
            default -> PATTERN_SLOT;
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
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}.
     * Direction-unlocked from what {@code getTank(Direction)}/{@code getItemHandler(Direction)}
     * currently hard-bind to UP/DOWN/sides. Unlike the Masticator's ingredient slot, PATTERN_SLOT
     * is never wired into the bucket bidirectional-transfer logic (only FUEL_TANK.SLOT/
     * REAGENT_TANK.SLOT are, see {@code createInventory}), so no fluid-container filtering is
     * needed here -- a bucket inserted there is just an inert item that matches no recipe pattern.
     *
     * <p>Native faces per {@link #getTank}/{@link #getItemHandler}: fuel = UP only; reagent =
     * everything EXCEPT UP (DOWN + all 4 sides, since {@code getTank} only special-cases UP); pattern
     * = the 4 sides only; output = DOWN only. Each channel is omitted once its native face(s) already
     * have a direct connection (see {@link #isFaceServiced}).
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
            channels.add(new Channel.ItemChannel("pattern", Component.literal("Pattern"), Channel.IO.IN, singleSlotHandler(PATTERN_SLOT, true)));
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
    // sides expose the pattern slot, bottom exposes the (pull-only) result slot.
    public ItemStack insertPattern(ItemStack stack) {
        return insertItemStack(INVENTORY, PATTERN_SLOT, stack);
    }

    public ItemStack extractPattern() {
        return extractItemStack(INVENTORY, PATTERN_SLOT);
    }

    public ItemStack extractResult() {
        return extractItemStack(INVENTORY, OUTPUT_SLOT);
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
    }

    @Override
    protected void drainOutputs(Level level) {
        // Mirrors the fluid machines' RESULT_TANK.pushFluidToBelowNeighbour drain cadence -- the
        // Metastasizer's output is an item slot instead of a tank, so it uses the item counterpart.
        if (!INVENTORY.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            ModItemUtil.pushItemToBelowNeighbour(level, worldPosition, INVENTORY, OUTPUT_SLOT);
        }
    }

    @Override
    protected boolean hasCraftingInputs() {
        // Frozen during the evolution flourish, same as MasticatorBlockEntity.
        return flourishCyclesRemaining < 0 && hasPattern() && hasEnoughReagent();
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        return hasOutputRoom();
    }

    @Override
    protected void onCraftComplete() {
        // Captured before draining: useFluid() fires REAGENT_TANK.onContentsChanged()
        // synchronously, which re-resolves the recipe and can clear requiredFluid/
        // cachedResult before this method finishes.
        int amount = requiredFluid;
        ItemStack output = cachedResult.copy();
        int completedTicks = maxProgress;

        REAGENT_TANK.useFluid(amount);
        INVENTORY.insertItem(OUTPUT_SLOT, output, false); // pattern is NOT consumed

        installedEvolutionProperties().ifPresent(props -> {
            evolutionProgress += completedTicks;
            if (evolutionProgress >= props.evolutionThreshold() && flourishCyclesRemaining < 0) {
                startEvolutionFlourish();
            }
        });
    }

    // Restores the cached result/fluid amount when a saved recipe is reloaded from NBT.
    @Override
    protected void onRecipeResolved(RecipeHolder<MetastasizingRecipe> recipe) {
        this.cachedResult = recipe.value().getResult();
        this.requiredFluid = recipe.value().getFluidAmount();
    }

    protected void resolveRecipe() {
        Optional<RecipeHolder<MetastasizingRecipe>> opt = getRecipeOptional();
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
            this.maxProgress = activeRecipe.value().getCraftingTime();
            this.cachedResult = activeRecipe.value().getResult();
            this.requiredFluid = activeRecipe.value().getFluidAmount();
        } else {
            resetActiveRecipe();
        }
    }

    private Optional<RecipeHolder<MetastasizingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getRecipeFor(ModRecipes.METASTASIZING_TYPE.get(),
                new OneFluidOneItemRecipeInput(INVENTORY.getStackInSlot(PATTERN_SLOT), REAGENT_TANK.getFluid()), this.level);
    }

    public void resetActiveRecipe() {
        activeRecipe = null;
        resetMaxProgress();
        resetProgress();
        cachedResult = ItemStack.EMPTY;
        requiredFluid = 0;
    }

    private boolean hasPattern() {
        return !INVENTORY.getStackInSlot(PATTERN_SLOT).isEmpty();
    }

    private boolean hasEnoughReagent() {
        return REAGENT_TANK.hasEnoughFluid(requiredFluid);
    }

    private boolean hasOutputRoom() {
        if (cachedResult.isEmpty()) return false;
        return INVENTORY.insertItem(OUTPUT_SLOT, cachedResult.copy(), true).isEmpty();
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.METASTASIZER);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MetastasizerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("reagent", REAGENT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("requiredFluid", requiredFluid);
        tag.putBoolean("module_tab_active", moduleTabActive);
        tag.putInt("evolution_progress", evolutionProgress);
        tag.putInt("evolution_flourish_cycles", flourishCyclesRemaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Worlds saved before MODULE existed have a smaller Size -- see
        // MachineBaseBlockEntity#loadItemHandler for why a plain deserializeNBT would shrink
        // INVENTORY back down and crash the menu (slot out of range).
        if (tag.contains("inventory")) loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, tag.getCompound("inventory"));
        if (tag.contains("reagent")) REAGENT_TANK.readFromNBT(registries, tag.getCompound("reagent"));
        requiredFluid = tag.getInt("requiredFluid");
        moduleTabActive = tag.getBoolean("module_tab_active");
        evolutionProgress = tag.getInt("evolution_progress");
        flourishCyclesRemaining = tag.contains("evolution_flourish_cycles") ? tag.getInt("evolution_flourish_cycles") : -1;
    }

    private ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

                if (slot == MODULE) onModuleChanged();

                if (isTransferringFluids) return;

                if (slot == PATTERN_SLOT) resolveRecipe();

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
            // just once already occupied. The old condition (`hasEmptyFluidHandlerInSlot`, which
            // inspects the slot's CURRENT contents) never fires on a slot that starts empty, so a
            // whole stack of empty containers could be inserted in one shot; the auto-fill transfer
            // then only ever fills and returns a single container, silently collapsing the rest of
            // the stack. Capping unconditionally means vanilla's own insertItem correctly accepts
            // just 1 and returns the remainder, so the custom insertItem override below is no longer
            // needed.
            @Override
            public int getSlotLimit(int slot) {
                if (slot == PATTERN_SLOT || slot == FUEL_TANK.SLOT || slot == REAGENT_TANK.SLOT || slot == MODULE) return 1;
                return super.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot != MODULE || stack.is(ModTags.Items.MODULES);
            }
        };
    }

    protected VulnerableTank createReagentTank() {
        return new VulnerableTank(getTier().tankCapacity(), 1, this::installedHazardProfile) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    resolveRecipe();
                    setChanged();
                }
            }
        };
    }
}
