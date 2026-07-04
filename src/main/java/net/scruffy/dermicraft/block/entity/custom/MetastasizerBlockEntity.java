package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.metastasizing.MetastasizingRecipe;
import net.scruffy.dermicraft.screen.custom.metastasizer.MetastasizerMenu;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MetastasizerBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory {

    public static final int PATTERN_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;

    private final FuelTank FUEL_TANK = createFuelTank();
    private final VulnerableTank REAGENT_TANK = createReagentTank();

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createInventory(4);

    private final int FUEL_USE_DEFAULT = 1;
    private int fuelUseRate = FUEL_USE_DEFAULT;
    private final float SPEED_DEFAULT = 1f;
    private float speed = SPEED_DEFAULT;

    private static final int MAX_HEALTH = 200;
    private static final int HUNGER_RATE = 1; // HP lost per cycle while unfueled and processing
    private static final float UNFUELED_SPEED_MODIFIER = 0.1f; // flat rate when running with no fuel at all
    private static final float RECOVERY_SPEED_FACTOR = 0.1f; // 10% of the fuel's own normal speed while healing
    private static final int BASE_HEAL_RATE = 2; // HP restored per cycle at a heal modifier of 1.0 (provisional)

    private RecipeHolder<MetastasizingRecipe> activeRecipe = null;
    private ItemStack cachedResult = ItemStack.EMPTY;
    private int requiredFluid = 0;

    @Nullable
    private ResourceLocation pendingRecipeId = null;

    public MetastasizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.METASTASIZER_BE.get(), pos, blockState);
        maxHealth = MAX_HEALTH;
        health = MAX_HEALTH;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public FuelTank getFuelTank() {
        return FUEL_TANK;
    }

    public VulnerableTank getReagentTank() {
        return REAGENT_TANK;
    }

    public FluidStack getFluid(int slot) {
        if (slot == FUEL_TANK.SLOT) return FUEL_TANK.getFluid();
        else if (slot == REAGENT_TANK.SLOT) return REAGENT_TANK.getFluid();
        else return FluidStack.EMPTY;
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == Direction.UP) return FUEL_TANK;
        return REAGENT_TANK;
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

    public void tick(Level level) {
        if (level.isClientSide) return;

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
            } else if (isMaxProgressValid() && hasPattern() && hasEnoughReagent() && hasOutputRoom()) {
                if (isStillCrafting()) {
                    tickProgress(fueled, healedThisCycle);
                } else {
                    // Captured before draining: useFluid() fires REAGENT_TANK.onContentsChanged()
                    // synchronously, which re-resolves the recipe and can clear requiredFluid/
                    // cachedResult before this branch finishes.
                    int amount = requiredFluid;
                    ItemStack output = cachedResult.copy();

                    REAGENT_TANK.useFluid(amount);
                    INVENTORY.insertItem(OUTPUT_SLOT, output, false); // pattern is NOT consumed
                    resetProgress();
                }
            }
            // Always sync, even when idle -- otherwise healing that happens with no active
            // recipe never reaches the client, and the GUI health bar appears frozen.
            setChanged();
            updateBlock();
        }
    }

    private void resolvePendingRecipe(Level level) {
        if (pendingRecipeId == null) return;

        level.getRecipeManager().byKey(pendingRecipeId).ifPresent(recipeHolder -> {
            if (recipeHolder.value() instanceof MetastasizingRecipe recipe) {
                this.activeRecipe = (RecipeHolder<MetastasizingRecipe>) recipeHolder;
                this.cachedResult = recipe.getResult();
                this.requiredFluid = recipe.getFluidAmount();
            }
        });
        pendingRecipeId = null;
    }

    private void resolveRecipe() {
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

    // `speed` is the fuel's raw multiplier (~1.0 for base Crude Slurry). Progress is measured
    // in ticks and advances CRAFT_TICKS per cycle at speed 1.0, so a recipe's `ticks` maps
    // 1:1 to real wall-clock ticks -- deliberately NOT following the Masticator's legacy
    // double-CRAFT_TICKS scaling (which runs ~10x fast).
    private void setSpeed() {
        speed = FUEL_TANK.getSpeed();
    }

    private void setUseRate() {
        fuelUseRate = Math.max(FUEL_USE_DEFAULT, FUEL_TANK.getUseRate()) * CRAFT_TICKS;
    }

    private void useFuel() {
        FUEL_TANK.useFuel(fuelUseRate);
    }

    private void incrementProgress(float speedOverride) {
        int workDoneInCycle = Math.round(CRAFT_TICKS * speedOverride);
        progress += Math.max(1, workDoneInCycle);
    }

    private int getHealAmount() {
        return Math.round(BASE_HEAL_RATE * FUEL_TANK.getHeal());
    }

    // Runs every cycle regardless of recipe state -- heals an idle-but-fueled machine too.
    // Returns whether healing (and its fuel consumption) actually happened this cycle, so
    // tickProgress() knows not to consume fuel a second time for the same cycle.
    private boolean tickHealing(boolean fueled) {
        if (fueled && health < maxHealth) {
            useFuel();
            healMachine(getHealAmount());
            return true;
        }
        return false;
    }

    private void tickProgress(boolean fueled, boolean healedThisCycle) {
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
        tag.put("fuel", FUEL_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("reagent", REAGENT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("speed", speed);
        tag.putInt("use", fuelUseRate);
        tag.putInt("requiredFluid", requiredFluid);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putInt("health", health);
        if (isRecipeValid(activeRecipe)) tag.putString("saved_recipe", activeRecipe.id().toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("fuel")) FUEL_TANK.readFromNBT(registries, tag.getCompound("fuel"));
        if (tag.contains("reagent")) REAGENT_TANK.readFromNBT(registries, tag.getCompound("reagent"));
        speed = tag.getFloat("speed");
        fuelUseRate = tag.getInt("use");
        requiredFluid = tag.getInt("requiredFluid");
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.getInt("maxProgress");
        this.health = tag.contains("health") ? tag.getInt("health") : maxHealth;

        if (tag.contains("saved_recipe", CompoundTag.TAG_STRING)) {
            pendingRecipeId = ResourceLocation.parse(tag.getString("saved_recipe"));
        }
    }

    private ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

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

            @Override
            public int getSlotLimit(int slot) {
                if (slot == PATTERN_SLOT) return 1;
                return ModFluidUtil.hasEmptyFluidHandlerInSlot(this, slot) ? 1 : super.getSlotLimit(slot);
            }

            @Override
            @NotNull
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (ModFluidUtil.hasFluidHandlerInSlot(this, slot)) {
                    if (!getStackInSlot(slot).isEmpty()) {
                        return stack;
                    }
                    if (stack.getCount() > 1) {
                        if (simulate) {
                            ItemStack remainder = stack.copy();
                            remainder.shrink(1);
                            return remainder;
                        } else {
                            ItemStack singleInsert = stack.copyWithCount(1);
                            super.insertItem(slot, singleInsert, false);
                            ItemStack remainder = stack.copy();
                            remainder.shrink(1);
                            return remainder;
                        }
                    }
                }
                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    private FuelTank createFuelTank() {
        return new FuelTank(FluidType.BUCKET_VOLUME * 5, 0) {
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

    private VulnerableTank createReagentTank() {
        return new VulnerableTank(FluidType.BUCKET_VOLUME * 5, 1) {
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
