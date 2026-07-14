package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.machine.MachineTier;
import net.scruffy.dermicraft.machine.TieredMachine;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

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
        MachineTier tier = tierOf(blockState);
        this.maxHealth = tier.maxHealth();
        this.health = tier.maxHealth();
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
        return new FuelTank(getTier().tankCapacity(), 0) {
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
        // `speed` is the fuel's raw multiplier (~1.0 for base Crude Slurry), scaled by the tier.
        // Progress advances CRAFT_TICKS per cycle at speed 1.0, so a recipe's `ticks` maps 1:1 to
        // real wall-clock ticks.
        speed = FUEL_TANK.getSpeed() * getTier().speedMultiplier();
    }

    protected void setUseRate() {
        fuelUseRate = Math.max(FUEL_USE_DEFAULT, FUEL_TANK.getUseRate()) * CRAFT_TICKS;
    }

    protected void useFuel() {
        FUEL_TANK.useFuel(fuelUseRate);
    }

    protected void incrementProgress(float speedOverride) {
        int workDoneInCycle = Math.round(CRAFT_TICKS * speedOverride);
        progress += Math.max(1, workDoneInCycle);
    }

    private int getHealAmount() {
        return Math.round(BASE_HEAL_RATE * FUEL_TANK.getHeal() * getTier().healMultiplier());
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
        } else {
            damageMachine(HUNGER_RATE);
            incrementProgress(UNFUELED_SPEED_MODIFIER);
        }
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

        if (tag.contains("saved_recipe", CompoundTag.TAG_STRING)) {
            pendingRecipeId = ResourceLocation.parse(tag.getString("saved_recipe"));
        }
    }
}
