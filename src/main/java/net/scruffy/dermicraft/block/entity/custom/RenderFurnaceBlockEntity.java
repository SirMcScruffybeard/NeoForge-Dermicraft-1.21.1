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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
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
import net.scruffy.dermicraft.block.custom.RenderFurnaceBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.BaseFluidType;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IEvolvingMachine;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.property.EvolutionModuleProperties;
import net.scruffy.dermicraft.screen.custom.render_furnace.RenderFurnaceMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModItemUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Render Furnace: automated smelting, resolved against VANILLA's own {@link SmeltingRecipe}
 * type -- no Dermicraft recipe authoring, works with any mod's smelting recipes for free (see the
 * machine notes for why this is the deliberate exception to the mod's own recipe types).
 *
 * <p>No HP mechanic ({@link net.scruffy.dermicraft.machine.MachineTier#NO_HEALTH}): {@link #hasCraftingInputs()}
 * requires fuel to be present, so an unfueled furnace simply freezes rather than limping along at
 * the standard 0.1x unfueled trickle every other machine uses.
 *
 * <p>Evolves into a Charred Render Furnace via an installed Evolution Module, same mechanic as
 * every other Charred machine -- but since this machine has no hazard-gated tank to begin with
 * (just a plain {@code FuelTank}), evolving grants no new hazard tolerance. It's a pure stat
 * upgrade: {@link net.scruffy.dermicraft.machine.MachineTier#CHARRED_NO_HEALTH}'s doubled fuel
 * capacity and 1.25x speed, same as {@link net.scruffy.dermicraft.machine.MachineTier#CHARRED}
 * grants every other Charred machine.
 */
public class RenderFurnaceBlockEntity extends AbstractFueledMachineBlockEntity<SmeltingRecipe>
        implements MenuProvider, IHaveInventory, IHasChannels, IEvolvingMachine {

    public static final int INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    // Module slot -- same tab-gated evolution pattern as every other machine's own, declared here so
    // Charred Render Furnace inherits it for free.
    public static final int MODULE = 3;
    public static final int INVENTORY_SIZE = 4;

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createInventory(INVENTORY_SIZE);

    private ItemStack cachedResult = ItemStack.EMPTY;

    private boolean moduleTabActive = false;

    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    public void setModuleTabActive(boolean active) {
        this.moduleTabActive = active;
    }

    // ---- Evolution (installed Evolution Module -> eventual Charred Render Furnace) -------------
    // Mirrors MetastasizerBlockEntity/RenderKilnBlockEntity's identical mechanic, minus any hazard
    // tolerance grant -- see this class's own javadoc for why.
    private int evolutionProgress = 0;
    private int flourishCyclesRemaining = -1;
    private static final int FLOURISH_DURATION_CYCLES = 4; // 4 * CRAFT_TICKS(10) = 40 ticks, ~2s

    public RenderFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.RENDER_FURNACE_BE.get(), pos, blockState);
    }

    // Lets a capability-leap subclass (Charred Render Furnace) register under its own
    // BlockEntityType while reusing everything else this class provides -- same pattern as
    // MasticatorBlockEntity's identical overload.
    protected RenderFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected RecipeType<SmeltingRecipe> getRecipeType() {
        return RecipeType.SMELTING;
    }

    public FluidStack getFluid(int slot) {
        return slot == FUEL_TANK.SLOT ? FUEL_TANK.getFluid() : FluidStack.EMPTY;
    }

    // Only one tank exists here (no reagent tank), so it's returned regardless of face.
    /** See {@link IHasChannels#describeFluidFace} -- mirrors {@link #getTank} literally, which hands
     * out the fuel tank on EVERY face here (unlike describeFace, which names the item slots). */
    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.tank.fuel");
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        return FUEL_TANK;
    }

    /** See {@link IHasChannels#describeFace} -- mirrors {@link #getItemHandler} literally. */
    @Override
    public Component describeFace(Direction face) {
        return switch (face) {
            case UP -> Component.translatable("tooltip.dermicraft.idep.face.render_furnace_fuel");
            case DOWN -> Component.translatable("tooltip.dermicraft.idep.face.render_furnace_output");
            default -> Component.translatable("tooltip.dermicraft.idep.face.render_furnace_input");
        };
    }

    // Face routing: top = fuel (fluid + its bucket slot), bottom = result slot only, sides = input.
    // MODULE is never exposed on any face -- GUI-only, same as every other machine's Module slot.
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

    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();

        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.UP)) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }
        if (level == null || !isFaceServiced(level, worldPosition, Channel.Kind.ITEM,
                Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            // Input and output share the "everything but the top" native face set (no dedicated
            // input-only side the way the Mutator has, since there's no reagent tank contending for
            // the sides here) -- listed as two channels regardless, so the Gate can service either
            // independently once a genuine face conflict exists.
            channels.add(new Channel.ItemChannel("input", Component.literal("Input"), Channel.IO.IN, singleSlotHandler(INPUT_SLOT, true)));
            channels.add(new Channel.ItemChannel("output", Component.literal("Output"), Channel.IO.OUT, singleSlotHandler(OUTPUT_SLOT, false)));
        }

        return channels;
    }

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

    @Override
    protected void drainOutputs(Level level) {
        if (autoDrainEnabled && !INVENTORY.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            ModItemUtil.pushItemToBelowNeighbour(level, worldPosition, INVENTORY, OUTPUT_SLOT);
        }
    }

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
    }

    // No HP mechanic (NO_HEALTH tier), so requiring fuel here -- rather than letting the base
    // engine's standard 0.1x-unfueled-trickle-plus-damage path run -- is what makes the furnace
    // actually STOP when fuel runs out instead of limping along forever. Progress simply freezes
    // (it isn't reset; resetProgress only fires from the base tick()'s "no active recipe" branch)
    // until fuel returns. Also frozen during the evolution flourish, same as every other machine.
    @Override
    protected boolean hasCraftingInputs() {
        return flourishCyclesRemaining < 0 && hasInput() && FUEL_TANK.hasEnoughFuel(fuelUseRate);
    }

    @Override
    protected boolean hasCraftingOutputRoom() {
        return hasOutputRoom();
    }

    @Override
    protected void onCraftComplete() {
        ItemStack output = cachedResult.copy();
        int completedTicks = maxProgress;

        INVENTORY.extractItem(INPUT_SLOT, 1, false);
        INVENTORY.insertItem(OUTPUT_SLOT, output, false);

        installedEvolutionProperties().ifPresent(props -> {
            evolutionProgress += completedTicks;
            if (evolutionProgress >= props.evolutionThreshold() && flourishCyclesRemaining < 0) {
                startEvolutionFlourish();
            }
        });
    }

    @Override
    protected void onRecipeResolved(RecipeHolder<SmeltingRecipe> recipe) {
        this.cachedResult = recipe.value().assemble(currentInput(), registryAccessOrNull());
    }

    // ---- Visual state (ACTIVE / lit front face) --------------------------------------------------
    // tickHealing() is called unconditionally every cycle regardless of crafting state, so it
    // doubles as the visual-state refresh point here too (same idiom as the Mutator), even though
    // this machine has no real healing to do (NO_HEALTH tier -- tickHealing is an inert call).

    @Override
    protected boolean tickHealing(boolean fueled) {
        boolean healed = super.tickHealing(fueled);
        updateActiveState(fueled);
        tickEvolutionFlourish();
        return healed;
    }

    private void updateActiveState(boolean fueled) {
        if (level == null) return;

        boolean shouldBeActive = fueled && isRecipeValid(activeRecipe) && isStillCrafting();
        BlockState state = getBlockState();
        if (state.getValue(RenderFurnaceBlock.ACTIVE) != shouldBeActive) {
            level.setBlock(worldPosition, state.setValue(RenderFurnaceBlock.ACTIVE, shouldBeActive), Block.UPDATE_CLIENTS);
        }
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
     * Transforms this block into a Charred Render Furnace in place, carrying over fuel tank
     * contents and the input/output item slots -- but deliberately NOT the recipe-in-progress
     * bookkeeping (active recipe, craft progress, cached result), same reasoning as every other
     * machine's completeEvolution: a half-finished cycle just restarts cleanly on the new block
     * instead. The Module itself is not carried over -- this transform is what consumes it.
     */
    private void completeEvolution(Level level) {
        ItemStack inputItem = INVENTORY.getStackInSlot(INPUT_SLOT);
        ItemStack outputItem = INVENTORY.getStackInSlot(OUTPUT_SLOT);
        FluidStack fuelContents = FUEL_TANK.getFluid().copy();
        BlockState oldState = getBlockState();

        INVENTORY.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(MODULE, ItemStack.EMPTY);
        if (!fuelContents.isEmpty()) FUEL_TANK.drain(fuelContents.getAmount(), IFluidHandler.FluidAction.EXECUTE);

        BlockState newState = ModBlocks.CHARRED_RENDER_FURNACE.get().defaultBlockState()
                .setValue(RenderFurnaceBlock.FACING, oldState.getValue(RenderFurnaceBlock.FACING));
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);

        if (level.getBlockEntity(worldPosition) instanceof RenderFurnaceBlockEntity charred) {
            charred.INVENTORY.setStackInSlot(INPUT_SLOT, inputItem);
            charred.INVENTORY.setStackInSlot(OUTPUT_SLOT, outputItem);
            if (!fuelContents.isEmpty()) charred.FUEL_TANK.fill(fuelContents, IFluidHandler.FluidAction.EXECUTE);
            charred.setChanged();
            charred.updateBlock();
        }
    }

    /** Whether this instance can still evolve at all -- true for the base Render Furnace, overridden
     * to false by {@link CharredRenderFurnaceBlockEntity} (already evolved). */
    protected boolean canEvolve() {
        return true;
    }

    /** 0 when not evolving at all (no Module, one with no real Evolution properties, or already a
     * Charred Render Furnace); otherwise how far {@link #evolutionProgress} is toward
     * {@code evolutionThreshold}, 0-1. Public purely for {@code EvolutionOverlayBlockEntityRenderer}'s
     * creeping overlay -- mirrors every other machine's identical method. */
    @Override
    public float getEvolutionProgressFraction() {
        return installedEvolutionProperties()
                .map(props -> Math.min(1f, evolutionProgress / (float) props.evolutionThreshold()))
                .orElse(0f);
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

    /** Called whenever the Module slot's contents change at all -- full reset, matching every other
     * machine's identical rule. */
    protected void onModuleChanged() {
        evolutionProgress = 0;
    }

    // ---- Recipe resolution -------------------------------------------------------------------

    private void resolveRecipe() {
        Optional<RecipeHolder<SmeltingRecipe>> opt = getRecipeOptional();
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
            this.maxProgress = activeRecipe.value().getCookingTime(); // vanilla's own duration -- no invented number needed
            this.cachedResult = activeRecipe.value().assemble(currentInput(), registryAccessOrNull());
        } else {
            resetActiveRecipe();
        }
    }

    private Optional<RecipeHolder<SmeltingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, currentInput(), level);
    }

    private SingleRecipeInput currentInput() {
        return new SingleRecipeInput(INVENTORY.getStackInSlot(INPUT_SLOT));
    }

    @Nullable
    private HolderLookup.Provider registryAccessOrNull() {
        return level == null ? null : level.registryAccess();
    }

    public void resetActiveRecipe() {
        activeRecipe = null;
        resetMaxProgress();
        resetProgress();
        cachedResult = ItemStack.EMPTY;
    }

    private boolean hasInput() {
        return !INVENTORY.getStackInSlot(INPUT_SLOT).isEmpty();
    }

    private boolean hasOutputRoom() {
        if (cachedResult.isEmpty()) return false;
        return INVENTORY.insertItem(OUTPUT_SLOT, cachedResult.copy(), true).isEmpty();
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.RENDER_FURNACE);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RenderFurnaceMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
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

                if (slot == INPUT_SLOT) resolveRecipe();

                biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);

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

            // Fuel slot holds exactly one container at a time, unconditionally -- see the mod-wide
            // tank-slot fix note in MutatorBlockEntity for why this must not be conditioned on the
            // slot's current contents. INPUT_SLOT/OUTPUT_SLOT keep the default stack limit.
            @Override
            public int getSlotLimit(int slot) {
                if (slot == FUEL_TANK.SLOT || slot == MODULE) return 1;
                return super.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot != MODULE || stack.is(ModTags.Items.MODULES);
            }
        };
    }
}
