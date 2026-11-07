package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.entity.custom.base.EvolvingMachineBlockEntity;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IHasHazardModuleData;
import net.scruffy.dermicraft.machine.FluidHazards;
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
public abstract class AbstractFueledMachineBlockEntity<R extends Recipe<?>> extends EvolvingMachineBlockEntity {

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

    /** Runs at the very end of each work cycle, after healing, progress and any hazard damage. A machine reads state
     * that should reflect the whole cycle here (such as its face texture). Nothing by default. */
    protected void onCycleEnd() {}

    /** Called after a saved recipe is restored from NBT, so a machine can rebuild cached amounts. */
    protected void onRecipeResolved(RecipeHolder<R> recipe) {}

    // ---- Tick template -----------------------------------------------------------------------

    public final void tick(Level level) {
        if (level.isClientSide) return;
        syncEvolutionBlockState();

        if (ModMath.Time.hasSecondsPassed(level, 5)) {
            drainOutputs(level);
        }

        resolvePendingRecipe(level);

        if (ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) {
            // Healing runs every cycle regardless of whether a recipe is active -- an idle,
            // fueled machine below max health should still recover.
            refreshHazardIntensities();
            metaphysicalRoll = rollMetaphysical(level);
            boolean fueled = FUEL_TANK.hasEnoughFuel(getEffectiveFuelUse());
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
            onCycleEnd();
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
        FUEL_TANK.useFuel(getEffectiveFuelUse());
    }

    protected void incrementProgress(float speedOverride) {
        int workDoneInCycle = Math.round(CRAFT_TICKS * speedOverride * getBiohazardFactor() * (float) metaphysicalRoll.speedFactor());
        progress += Math.max(1, workDoneInCycle);
    }

    private int getHealAmount() {
        return Math.round(BASE_HEAL_RATE * FUEL_TANK.getHeal() * getTierHealMultiplier() * getBiohazardFactor());
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
            tickGradualProgress(CRAFT_TICKS);
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

    /** The fluids this machine is processing right now, whose hazards decide the penalties: its ingredient fluids and
     * the fluids its recipe produces,
     * not its fuel. Empty by default. */
    protected List<FluidStack> getProcessingFluids() {
        return List.of();
    }

    /** What this machine tolerates, which cancels the matching penalties. Everything by default, so a machine that
     * has not moved onto the penalty rules never takes any. */
    protected HazardProfile getHazardProfile() {
        return HazardProfile.ALL;
    }

    /** Whether the machine is doing work right now that its fluids' hazards apply to. By default a running recipe with
     * its inputs in place; a machine with another way of working (the Mutator's Fill mode) overrides this to say when
     * that is happening. An idle machine sitting next to a hazardous fluid in its tank pays nothing. */
    protected boolean isHazardProcessing() {
        return usesEvolutionState() && isRecipeValid(activeRecipe) && hasCraftingInputs();
    }

    /** This cycle's Metaphysical work-speed roll (1.0 when none), for a machine that does its own work outside the
     * recipe engine and so applies the speed factor itself. */
    protected float getMetaphysicalSpeedFactor() {
        return (float) metaphysicalRoll.speedFactor();
    }

    /** Charges this cycle's hazard penalties; runs only on a cycle where the machine actually worked. */
    protected void applyHazardPenalties() {
        if (!usesEvolutionState()) return;

        int thermalDamage = HazardEffects.getThermalDamage(getProcessingFluids(), getHazardProfile());
        if (thermalDamage > 0) damageMachine(thermalDamage);

        int radiationDamage = getRadiationEffect().damage();
        if (radiationDamage > 0) damageMachine(radiationDamage);

        int healthChange = metaphysicalRoll.healthChange();
        if (healthChange < 0) damageMachine(-healthChange);
        else if (healthChange > 0) healMachine(healthChange);
    }

    /** The Radiation cost right now. Only while a recipe is actually running with its inputs in place, so a machine
     * sitting idle next to a radioactive fluid in its tank pays nothing. */
    private HazardEffects.RadiationEffect getRadiationEffect() {
        if (!isHazardProcessing()) {
            return HazardEffects.RadiationEffect.NONE;
        }
        return HazardEffects.getRadiationEffect(getProcessingFluids(), getHazardProfile());
    }

    /** The Biohazard slowdown factor on work speed and healing right now (1.0 for none). Gated like the Radiation cost:
     * only while a recipe is running with its inputs in place. */
    protected float getBiohazardFactor() {
        if (!isHazardProcessing()) return 1.0f;
        return (float) HazardEffects.getBiohazardEffect(getProcessingFluids(), getHazardProfile()).factor();
    }

    // ---- Hazard readout for the screen ------------------------------------------------------

    /** The worst intensity of each hazard family among the fluids in play, indexed by family ordinal. Synced to the
     * client so the screen can warn about a missing tolerance (the client cannot see the recipe's output fluid). */
    private int[] hazardIntensities = new int[HazardFamily.values().length];

    /** The worst intensity of {@code family} among this machine's processing fluids as of the last cycle (Thermal is
     * the kelvin beyond the safe range). 0 when none, or when the machine does not use the penalty rules. */
    public int getHazardIntensity(HazardFamily family) {
        return hazardIntensities[family.ordinal()];
    }

    /** Recomputes {@link #hazardIntensities} from the fluids in play, whether or not a recipe is running, so the
     * warning shows as soon as a hazardous fluid is in place. */
    private void refreshHazardIntensities() {
        int[] fresh = new int[HazardFamily.values().length];
        if (usesEvolutionState()) {
            for (FluidStack fluid : getProcessingFluids()) {
                for (HazardFamily family : HazardFamily.values()) {
                    fresh[family.ordinal()] = Math.max(fresh[family.ordinal()], FluidHazards.getIntensity(fluid, family));
                }
            }
        }
        hazardIntensities = fresh;
    }

    /** This cycle's Metaphysical dice, rolled once at the start of the cycle so the fuel check, the fuel spent, the
     * work done and the HP swing all agree. Not saved: a reload simply rolls again next cycle. */
    private HazardEffects.MetaphysicalRoll metaphysicalRoll = HazardEffects.MetaphysicalRoll.NONE;

    /** Rolls the Metaphysical randomness for a new cycle. Gated like the other effects: only while a recipe is running
     * with its inputs in place, so an idle machine is never erratic. */
    private HazardEffects.MetaphysicalRoll rollMetaphysical(Level level) {
        if (!isHazardProcessing()) {
            return HazardEffects.MetaphysicalRoll.NONE;
        }
        HazardEffects.MetaphysicalEffect effect = HazardEffects.getMetaphysicalEffect(getProcessingFluids(), getHazardProfile());
        return HazardEffects.rollMetaphysical(effect, level.random);
    }

    /** The fuel spent (and required) per cycle: the base use rate times the Radiation surcharge. */
    protected int getEffectiveFuelUse() {
        return Math.round(fuelUseRate * (float) getRadiationEffect().fuelMultiplier() * (float) metaphysicalRoll.fuelFactor());
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
        tag.putIntArray("hazard_intensities", hazardIntensities);
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
        int[] savedIntensities = tag.getIntArray("hazard_intensities");
        hazardIntensities = savedIntensities.length == HazardFamily.values().length ? savedIntensities : new int[HazardFamily.values().length];

        if (tag.contains("saved_recipe", CompoundTag.TAG_STRING)) {
            pendingRecipeId = ResourceLocation.parse(tag.getString("saved_recipe"));
        }
    }
}
