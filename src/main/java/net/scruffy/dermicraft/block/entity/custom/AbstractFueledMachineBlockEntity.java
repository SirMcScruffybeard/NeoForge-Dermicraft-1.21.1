package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.entity.custom.base.MachineBaseBlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IHasHazardModuleData;
import net.scruffy.dermicraft.item.custom.module.EvolutionModuleItem;
import net.scruffy.dermicraft.machine.EvolutionEffects;
import net.scruffy.dermicraft.machine.EvolutionResult;
import net.scruffy.dermicraft.machine.HazardEffects;
import net.scruffy.dermicraft.machine.MachineStatKey;
import net.scruffy.dermicraft.machine.MachineTier;
import net.scruffy.dermicraft.machine.SimpleMachineStats;
import net.scruffy.dermicraft.machine.TieredMachine;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Shared groundwork for every fuel-powered crafting machine (Masticator, Metastasizer, Effluentcer,
 * ...). Owns the fuel economy, the health/starvation model, the heal-while-fueled behaviour, the
 * per-cycle tick template, and generic saved-recipe handling.
 *
 * <p>It deliberately does NOT own the machine's IO layout: subclasses declare their own tanks and
 * inventory slots and plug into the tick loop through a handful of hooks. That is what lets a future
 * upgrade tier add another tank or item slot by overriding a hook, without disturbing this engine.
 *
 * <p>Numeric per-tier stats (tank capacity, max health, speed/heal multipliers) come from the
 * block's {@link MachineTier}, read off the block state -- so a stat-only tier needs no code here.
 *
 * @param <R> the recipe type this machine crafts
 */
public abstract class AbstractFueledMachineBlockEntity<R extends Recipe<?>> extends MachineBaseBlockEntity {

    protected static final int FUEL_USE_DEFAULT = 1;
    protected static final int HUNGER_RATE = 1;                  // HP lost per cycle while unfueled and processing
    protected static final float UNFUELED_SPEED_MODIFIER = 0.1f; // flat rate when running with no fuel at all
    protected static final float RECOVERY_SPEED_FACTOR = 0.1f;   // fraction of normal speed while also healing
    protected static final int BASE_HEAL_RATE = 2;               // HP restored per cycle at heal modifier 1.0

    protected final FuelTank FUEL_TANK = createFuelTank();

    protected int fuelUseRate = FUEL_USE_DEFAULT;
    protected float speed = 1f;

    protected RecipeHolder<R> activeRecipe = null;

    @Nullable
    protected ResourceLocation pendingRecipeId = null;

    protected AbstractFueledMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.maxHealth = getBaseMaxHealth();
        this.health = maxHealth;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    /** The tier this machine is running as, read from its block (defaults to BASIC if untiered). */
    public MachineTier getTier() {
        return tierOf(getBlockState());
    }

    private static MachineTier tierOf(BlockState state) {
        return state.getBlock() instanceof TieredMachine tiered ? tiered.getTier() : MachineTier.BASIC;
    }

    public FuelTank getFuelTank() {
        return FUEL_TANK;
    }

    // ---- Stat sources ------------------------------------------------------------------------
    // A machine that has moved onto the evolution state (see usesEvolutionState) takes these numbers from
    // SimpleMachineStats(getEvolutionState()); any other machine takes them from its block's MachineTier. A machine
    // whose numbers differ from the standard table (no health system, a different tank count) overrides just the
    // hook that differs.

    /** Per-tank capacity in mB, before any Capacity Module bonus. */
    protected int getBaseTankCapacity() {
        return usesEvolutionState() ? SimpleMachineStats.getTankCapacityMb(getStatKey(), getEvolutionState()) : getTier().tankCapacity();
    }

    protected int getBaseMaxHealth() {
        return usesEvolutionState() ? SimpleMachineStats.getMaxHealth(getStatKey(), getEvolutionState()) : getTier().maxHealth();
    }

    protected float getTierSpeedMultiplier() {
        return usesEvolutionState() ? SimpleMachineStats.getSpeedMultiplier(getStatKey(), getEvolutionState()) : getTier().speedMultiplier();
    }

    protected float getTierHealMultiplier() {
        return getTier().healMultiplier();
    }

    /** Multiplier on the fuel spent per cycle; 1.0 until the machine uses the evolution state. */
    protected float getTierFuelUseMultiplier() {
        return usesEvolutionState() ? SimpleMachineStats.getFuelUseMultiplier(getStatKey(), getEvolutionState()) : 1.0f;
    }

    /** General Module slots (the dedicated Evolution slot is separate): from the stat table once the machine uses the
     * evolution state, otherwise the base default. */
    @Override
    public int getModuleSlotCount() {
        return usesEvolutionState() ? SimpleMachineStats.getModuleSlots(getStatKey(), getEvolutionState()) : super.getModuleSlotCount();
    }

    /** Re-reads the stats a new evolution state changes: max health (a full machine stays full) and the speed and
     * fuel use derived from the tier. A machine overrides this to also resize what depends on the tier, such as
     * its Module slots and tanks. */
    @Override
    protected void onEvolutionStateChanged() {
        int newMaxHealth = getBaseMaxHealth();
        if (newMaxHealth != maxHealth) {
            boolean wasFull = health >= maxHealth;
            maxHealth = newMaxHealth;
            health = wasFull ? newMaxHealth : Math.min(health, newMaxHealth);
        }
        setSpeed();
        setUseRate();
    }

    // ---- Gradual evolution (an Evolution Module in the Evolution slot raises the machine in place) ----------------
    // For machines that process, evolution is gradual: real ticks of production accrue toward the module's upgrade
    // time (from EvolutionRules), pausing on a fuel stall or zero HP, and resetting only when the module comes out
    // or the machine is picked up. When the time is served a short flourish plays, then the new evolution state is
    // applied in place and the module is consumed. A machine opts in with usesEvolutionState().

    private static final int GRADUAL_FLOURISH_CYCLES = 4; // 4 * CRAFT_TICKS(10) = 40 ticks, about 2s

    /** The dedicated Evolution Module slot: one slot, Evolution Modules only. Unused (and unexposed) on a machine
     * that has not opted in with {@link #usesEvolutionState()}. */
    public final ItemStackHandler EVOLUTION_INVENTORY = createEvolutionInventory();

    /** Real production ticks accrued toward the pending upgrade. */
    private int gradualProgress = 0;
    /** The pending upgrade's time when progress last advanced, saved so a client (which has no server config) can
     * draw the progress fraction. */
    private int gradualTarget = 1;
    /** Cycles left in the end-of-evolution flourish; negative when it is not running. */
    private int gradualFlourishCycles = -1;

    /** Whether this machine's stats and evolution come from its evolution state and Evolution slot, not from its
     * block's {@link MachineTier}. False until a machine is migrated. */
    protected boolean usesEvolutionState() {
        return false;
    }

    /** The machine's key in the stat config (its own section of overrides), or null for the shared values. A machine
     * that uses the evolution state returns its {@link MachineStatKey}. */
    @Nullable
    protected MachineStatKey getStatKey() {
        return null;
    }

    /** Whether a stack is an Evolution Module (the frame or any loaded module). */
    public static boolean isEvolutionModule(ItemStack stack) {
        return stack.getItem() instanceof EvolutionModuleItem;
    }

    private ItemStackHandler createEvolutionInventory() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return usesEvolutionState() && isEvolutionModule(stack);
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
    }

    /** Whatever was accrued belongs to the old module, so insert, removal and swap all start over. */
    private void onEvolutionSlotChanged() {
        gradualProgress = 0;
        gradualFlourishCycles = -1;
        gradualTarget = getPendingUpgrade().map(EvolutionResult.Upgrade::durationTicks).orElse(1);
    }

    /** What the Evolution slot's module would do on this machine right now: empty for an empty slot, the bare frame
     * or a machine that does not use the evolution state; otherwise the {@link EvolutionRules} answer. */
    public Optional<EvolutionResult> getEvolutionStatus() {
        if (!usesEvolutionState()) return Optional.empty();
        ItemStack module = EVOLUTION_INVENTORY.getStackInSlot(0);
        if (!(module.getItem() instanceof EvolutionModuleItem evolutionModule)) return Optional.empty();
        return evolutionModule.evaluate(getStatKey(), getEvolutionState(), module);
    }

    /** The upgrade the Evolution slot's module would apply, or empty if it is blocked, inert or absent. */
    protected Optional<EvolutionResult.Upgrade> getPendingUpgrade() {
        return getEvolutionStatus()
                .filter(EvolutionResult.Upgrade.class::isInstance)
                .map(EvolutionResult.Upgrade.class::cast);
    }

    /** Production is frozen while the flourish plays, so the swap never lands mid-craft. */
    protected boolean isEvolutionFlourishing() {
        return gradualFlourishCycles >= 0;
    }

    /** How far the pending evolution is, 0 to 1 (0 with no live module). Read by the evolution overlay renderer and
     * the Evolution slot's fill. */
    public float getGradualEvolutionFraction() {
        if (!usesEvolutionState() || getPendingUpgrade().isEmpty()) return 0f;
        return Math.clamp(gradualProgress / (float) Math.max(1, gradualTarget), 0f, 1f);
    }

    /** Adds one cycle of real production to the pending evolution; called when the machine actually worked this
     * cycle (fueled and not starved). */
    private void tickGradualProgress() {
        if (!usesEvolutionState() || isEvolutionFlourishing()) return;

        Optional<EvolutionResult.Upgrade> pending = getPendingUpgrade();
        if (pending.isEmpty()) {
            gradualProgress = 0;
            return;
        }
        gradualTarget = pending.get().durationTicks();
        gradualProgress += CRAFT_TICKS;
        if (gradualProgress >= gradualTarget) {
            gradualFlourishCycles = GRADUAL_FLOURISH_CYCLES;
            if (level != null) EvolutionEffects.playDefaultStartSound(level, worldPosition);
        }
    }

    /** One step of the end-of-evolution flourish per cycle; the upgrade is applied on its last step. */
    private void tickGradualFlourish() {
        if (!isEvolutionFlourishing()) return;

        if (level instanceof ServerLevel serverLevel) {
            EvolutionEffects.spawnParticles(serverLevel, worldPosition,
                    EvolutionEffects.getProgress(gradualFlourishCycles, GRADUAL_FLOURISH_CYCLES),
                    gradualFlourishCycles == 0, EvolutionEffects.DEFAULT_TINT, EvolutionEffects.Cadence.CYCLE);
        }

        gradualFlourishCycles--;
        if (gradualFlourishCycles < 0) completeGradualEvolution();
    }

    /** Applies the module's upgrade in place and consumes the module. It is evaluated again first in case the state
     * changed during the flourish; if it no longer applies the module stays in the slot. */
    private void completeGradualEvolution() {
        Optional<EvolutionResult.Upgrade> upgrade = getPendingUpgrade();
        gradualProgress = 0;
        gradualFlourishCycles = -1;
        if (upgrade.isEmpty()) return;

        EVOLUTION_INVENTORY.setStackInSlot(0, ItemStack.EMPTY);
        setEvolutionState(upgrade.get().newState());
        setChanged();
        updateBlock();
    }

    /** {@code profile} plus the permanent tolerance the machine has earned: every hazard family in its state, at the
     * level held. */
    protected HazardProfile addPermanentTolerance(HazardProfile profile) {
        for (HazardFamily family : HazardFamily.values()) {
            profile = profile.plusFamily(family, getEvolutionState().level(family));
        }
        return profile;
    }

    /** {@code profile} plus the tolerance of a hazard Evolution Module that is evolving the machine right now. It
     * counts while its upgrade is live (so a blocked module grants nothing) and becomes permanent when the
     * evolution completes. */
    protected HazardProfile addEvolvingTolerance(HazardProfile profile) {
        if (!usesEvolutionState() || getPendingUpgrade().isEmpty()) return profile;
        ItemStack module = EVOLUTION_INVENTORY.getStackInSlot(0);
        if (!(module.getItem() instanceof IHasHazardModuleData hazardModule)) return profile;
        HazardModuleData data = hazardModule.getHazardData(module);
        return data == null ? profile : profile.plusFamily(data.family(), data.level());
    }

    // ---- Hooks each machine (or future tier) implements --------------------------------------

    /** The recipe type this machine looks recipes up against; used to validate restored recipes. */
    protected abstract RecipeType<R> getRecipeType();

    /** Whether the current inputs satisfy the active recipe (ingredients present in sufficient amount). */
    protected abstract boolean hasCraftingInputs();

    /** Whether there is room to deposit the active recipe's result. */
    protected abstract boolean hasCraftingOutputRoom();

    /** Consume the recipe's inputs and emit its output. The engine handles resetting progress after. */
    protected abstract void onCraftComplete();

    /** Push finished output to the neighbour below. Called every 5s. Default: nothing. */
    protected void drainOutputs(Level level) {}

    /** Called after a saved recipe is restored from NBT, so a machine can rebuild cached amounts. */
    protected void onRecipeResolved(RecipeHolder<R> recipe) {}

    // ---- Tick template -----------------------------------------------------------------------

    public final void tick(Level level) {
        if (level.isClientSide) return;

        if (ModMath.Time.hasSecondsPassed(level, 5)) {
            drainOutputs(level);
        }

        resolvePendingRecipe(level);

        if (ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) {
            // Healing runs every cycle regardless of whether a recipe is active -- an idle,
            // fueled machine below max health should still recover.
            boolean fueled = FUEL_TANK.hasEnoughFuel(fuelUseRate);
            boolean healedThisCycle = tickHealing(fueled);
            tickGradualFlourish();

            if (!isRecipeValid(activeRecipe)) {
                if (progress > 0) {
                    resetProgress();
                }
            } else if (isMaxProgressValid() && hasCraftingInputs() && hasCraftingOutputRoom()) {
                if (isStillCrafting()) {
                    tickProgress(fueled, healedThisCycle);
                } else {
                    onCraftComplete();
                    resetProgress();
                }
            }
            // Always sync, even when idle -- otherwise healing that happens with no active recipe
            // never reaches the client and the GUI health bar appears frozen.
            setChanged();
            updateBlock();
        }
    }

    protected void resolvePendingRecipe(Level level) {
        if (pendingRecipeId == null) return;

        level.getRecipeManager().byKey(pendingRecipeId).ifPresent(recipeHolder -> {
            if (recipeHolder.value().getType() == getRecipeType()) {
                @SuppressWarnings("unchecked")
                RecipeHolder<R> holder = (RecipeHolder<R>) recipeHolder;
                this.activeRecipe = holder;
                onRecipeResolved(holder);
            }
        });
        pendingRecipeId = null;
    }

    // ---- Fuel economy ------------------------------------------------------------------------

    protected FuelTank createFuelTank() {
        return new FuelTank(getBaseTankCapacity(), 0) {
            @Override
            protected void onContentsChanged() {
                if (!level.isClientSide) {
                    setSpeed();
                    setUseRate();
                    setChanged();
                }
            }
        };
    }

    protected void setSpeed() {
        // `speed` is the fuel's raw multiplier (~1.0 for base Crude Slurry), scaled by the tier and
        // by any installed Work Speed Module(s). Progress advances CRAFT_TICKS per cycle at speed
        // 1.0, so a recipe's `ticks` maps 1:1 to real wall-clock ticks.
        speed = FUEL_TANK.getSpeed() * getTierSpeedMultiplier() * workSpeedMultiplier();
    }

    /** 1.0 (no bonus) by default -- overridden by a subclass with a real Module slot, mirroring
     * installedHazardProfile()'s own per-subclass pattern. See IHaveModules#workSpeedMultiplier
     * for the diminishing-return stacking rule this feeds into. */
    protected float workSpeedMultiplier() {
        return 1.0f;
    }

    protected void setUseRate() {
        fuelUseRate = Math.max(FUEL_USE_DEFAULT, Math.round(FUEL_TANK.getRawUseRate() * CRAFT_TICKS * getTierFuelUseMultiplier()));
    }

    protected void useFuel() {
        FUEL_TANK.useFuel(fuelUseRate);
    }

    protected void incrementProgress(float speedOverride) {
        int workDoneInCycle = Math.round(CRAFT_TICKS * speedOverride);
        progress += Math.max(1, workDoneInCycle);
    }

    private int getHealAmount() {
        return Math.round(BASE_HEAL_RATE * FUEL_TANK.getHeal() * getTierHealMultiplier());
    }

    /**
     * Runs every cycle regardless of recipe state -- heals an idle-but-fueled machine too. Returns
     * whether healing (and its fuel consumption) actually happened this cycle, so tickProgress()
     * knows not to consume fuel a second time for the same cycle.
     */
    protected boolean tickHealing(boolean fueled) {
        if (fueled && health < maxHealth) {
            useFuel();
            healMachine(getHealAmount());
            return true;
        }
        return false;
    }

    protected void tickProgress(boolean fueled, boolean healedThisCycle) {
        if (isStarved()) {
            return; // fuel/heal already handled by tickHealing(); no progress while starved
        }

        if (fueled) {
            if (healedThisCycle) {
                incrementProgress(RECOVERY_SPEED_FACTOR * speed); // fuel already spent healing this cycle
            } else {
                useFuel();
                incrementProgress(speed);
            }
            tickGradualProgress();
        } else {
            damageMachine(HUNGER_RATE);
            incrementProgress(UNFUELED_SPEED_MODIFIER);
        }
        applyHazardPenalties();
    }

    // ---- Hazard penalties -------------------------------------------------------------------
    // A machine that uses the evolution state does not refuse hazardous fluids; it pays for processing them, per work
    // cycle, unless it has the matching tolerance (see HazardEffects). A machine opts in by saying which fluids are in
    // play and what it tolerates; one that does neither is unaffected.

    /** The fluids this machine is processing right now, whose hazards decide the penalties: its ingredient fluids,
     * not its fuel or output. Empty by default. */
    protected List<FluidStack> getProcessingFluids() {
        return List.of();
    }

    /** What this machine tolerates, which cancels the matching penalties. Everything by default, so a machine that
     * has not moved onto the penalty rules never takes any. */
    protected HazardProfile getHazardProfile() {
        return HazardProfile.ALL;
    }

    /** Charges this cycle's hazard penalties; runs only on a cycle where the machine actually worked. */
    private void applyHazardPenalties() {
        if (!usesEvolutionState()) return;

        int thermalDamage = HazardEffects.getThermalDamage(getProcessingFluids(), getHazardProfile());
        if (thermalDamage > 0) damageMachine(thermalDamage);
    }

    // ---- Shared NBT (fuel + engine + saved recipe). Subclasses super-call, then add their IO. ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", FUEL_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("speed", speed);
        tag.putInt("use", fuelUseRate);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putInt("health", health);
        tag.put("evolution_inventory", EVOLUTION_INVENTORY.serializeNBT(registries));
        tag.putInt("gradual_progress", gradualProgress);
        tag.putInt("gradual_target", gradualTarget);
        tag.putInt("gradual_flourish_cycles", gradualFlourishCycles);
        if (isRecipeValid(activeRecipe)) tag.putString("saved_recipe", activeRecipe.id().toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("fuel")) FUEL_TANK.readFromNBT(registries, tag.getCompound("fuel"));
        speed = tag.getFloat("speed");
        fuelUseRate = tag.getInt("use");
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.getInt("maxProgress");
        this.health = tag.contains("health") ? tag.getInt("health") : maxHealth;
        if (tag.contains("evolution_inventory")) {
            loadItemHandler(EVOLUTION_INVENTORY, 1, registries, tag.getCompound("evolution_inventory"));
        }
        gradualProgress = tag.getInt("gradual_progress");
        gradualTarget = tag.contains("gradual_target") ? tag.getInt("gradual_target") : 1;
        gradualFlourishCycles = tag.contains("gradual_flourish_cycles") ? tag.getInt("gradual_flourish_cycles") : -1;

        if (tag.contains("saved_recipe", CompoundTag.TAG_STRING)) {
            pendingRecipeId = ResourceLocation.parse(tag.getString("saved_recipe"));
        }
    }
}
