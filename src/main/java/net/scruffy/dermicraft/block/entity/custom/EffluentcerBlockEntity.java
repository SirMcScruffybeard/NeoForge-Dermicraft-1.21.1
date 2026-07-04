package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.EffluentcerBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.TwoFluidRecipeInput;
import net.scruffy.dermicraft.recipe.effluencing.EffluencingRecipe;
import net.scruffy.dermicraft.screen.custom.effluentcer.EffluentcerMenu;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class EffluentcerBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory {

    private final FuelTank FUEL_TANK = createFuelTank();
    private final VulnerableTank INPUT_A_TANK = createInputTank(1);
    private final VulnerableTank INPUT_B_TANK = createInputTank(2);
    private final VulnerableTank RESULT_TANK = createResultTank();

    private boolean isTransferringFluids = false;

    private final ItemStackHandler INVENTORY = createItemHandler();

    // Set by the menu whenever a player opens the GUI -- used only as an eject target for
    // the fill-and-eject item-slot behavior (see createItemHandler()).
    @Nullable
    private Player interactingPlayer;

    private final int FUEL_USE_DEFAULT = 1;
    private int fuelUseRate = FUEL_USE_DEFAULT;
    private final float SPEED_DEFAULT = 1f;
    private float speed = SPEED_DEFAULT;
    private int resultAmount = 0;
    private int requiredAmountForA = 0;
    private int requiredAmountForB = 0;

    private static final int MAX_HEALTH = 200;
    private static final int HUNGER_RATE = 1; // HP lost per cycle while unfueled and processing
    private static final float UNFUELED_SPEED_MODIFIER = 0.1f; // flat rate when running with no fuel at all
    private static final float RECOVERY_SPEED_FACTOR = 0.1f; // 10% of the fuel's own normal speed while healing
    private static final int BASE_HEAL_RATE = 2; // HP restored per cycle at a heal modifier of 1.0 (provisional)

    private RecipeHolder<EffluencingRecipe> activeRecipe = null;

    @Nullable
    private ResourceLocation pendingRecipeId = null;

    public EffluentcerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.EFFLUENTCER_BE.get(), pos, blockState);
        maxHealth = MAX_HEALTH;
        health = MAX_HEALTH;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        if (direction == null) return INPUT_A_TANK;

        if (direction == Direction.UP) return FUEL_TANK;
        if (direction == Direction.DOWN) return RESULT_TANK;

        Direction facing = getBlockState().getValue(EffluentcerBlock.FACING);
        if (direction == facing || direction == facing.getOpposite()) return INPUT_A_TANK;

        return INPUT_B_TANK;
    }

    public FuelTank getFuelTank() {
        return FUEL_TANK;
    }

    public VulnerableTank getInputATank() {
        return INPUT_A_TANK;
    }

    public VulnerableTank getInputBTank() {
        return INPUT_B_TANK;
    }

    public VulnerableTank getResultTank() {
        return RESULT_TANK;
    }

    public FluidStack getFluid(int slot) {

        if (slot == FUEL_TANK.SLOT) return FUEL_TANK.getFluid();

        else if (slot == INPUT_A_TANK.SLOT) return INPUT_A_TANK.getFluid();

        else if (slot == INPUT_B_TANK.SLOT) return INPUT_B_TANK.getFluid();

        else if (slot == RESULT_TANK.SLOT) return RESULT_TANK.getFluid();

        else return FluidStack.EMPTY;
    }

    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == null) return INVENTORY;

        int targetSlot;
        if (direction == Direction.UP) {
            targetSlot = FUEL_TANK.SLOT;
        } else if (direction == Direction.DOWN) {
            targetSlot = RESULT_TANK.SLOT;
        } else {
            Direction facing = getBlockState().getValue(EffluentcerBlock.FACING);
            targetSlot = (direction == facing || direction == facing.getOpposite())
                    ? INPUT_A_TANK.SLOT : INPUT_B_TANK.SLOT;
        }

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

            // Automation is deliberately capped at one container per slot (see getSlotLimit
            // below) -- a stack can never accumulate here, so there's nothing for the GUI's
            // fill-and-eject behavior to do on this path.
            @Override
            @NotNull
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!INVENTORY.getStackInSlot(targetSlot).isEmpty()) return stack;

                ItemStack single = stack.copyWithCount(1);
                ItemStack rejected = INVENTORY.insertItem(targetSlot, single, simulate);
                if (!rejected.isEmpty()) return stack;

                ItemStack remainder = stack.copy();
                remainder.shrink(1);
                return remainder;
            }

            @Override
            @NotNull
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return INVENTORY.extractItem(targetSlot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }
        };
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
    }

    public void setInteractingPlayer(@Nullable Player player) {
        this.interactingPlayer = player;
    }

    public void tick(Level level) {
        if (level.isClientSide) return;

        if (ModMath.Time.hasSecondsPassed(level, 5) && !RESULT_TANK.isEmpty()) {
            RESULT_TANK.pushFluidToBelowNeighbour(level, worldPosition);
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
                return;
            }

            if (isMaxProgressValid() && hasIngredients() && RESULT_TANK.hasRoom(resultAmount)) {
                if (isStillCrafting()) {
                    tickProgress(fueled, healedThisCycle);
                } else {
                    // Captured before draining: draining INPUT_A_TANK fires its
                    // onContentsChanged() synchronously, which re-resolves the recipe against
                    // the now-partially-drained tanks and can reset requiredAmountForB/
                    // resultAmount/activeRecipe to zero/null before this method reaches them.
                    int amountA = requiredAmountForA;
                    int amountB = requiredAmountForB;
                    FluidStack output = craftResult(resultAmount);

                    INPUT_A_TANK.useFluid(amountA);
                    INPUT_B_TANK.useFluid(amountB);
                    RESULT_TANK.fill(output, IFluidHandler.FluidAction.EXECUTE);
                    resetProgress();
                }
            }
            setChanged();
            updateBlock();
        }
    }

    private void resolvePendingRecipe(Level level) {
        if (pendingRecipeId == null) return;

        level.getRecipeManager().byKey(pendingRecipeId).ifPresent(recipeHolder -> {
            if (recipeHolder.value() instanceof EffluencingRecipe) {
                this.activeRecipe = (RecipeHolder<EffluencingRecipe>) recipeHolder;
            }
        });
        pendingRecipeId = null;
    }

    // `speed` is the fuel's raw multiplier (~1.0 for base Crude Slurry). Progress advances
    // CRAFT_TICKS per cycle at speed 1.0, so a recipe's `ticks` maps 1:1 to real wall-clock
    // ticks (previously this applied CRAFT_TICKS twice, running ~10x faster than stated;
    // fixed to match the Metastasizer/Masticator timing model).
    private void setSpeed() {
        speed = FUEL_TANK.getSpeed();
    }

    private void setUseRate() {
        fuelUseRate = Math.max(FUEL_USE_DEFAULT, FUEL_TANK.getUseRate()) * CRAFT_TICKS;
    }

    private void useFuel() {
        FUEL_TANK.useFuel(fuelUseRate);
    }

    private boolean hasIngredients() {
        return INPUT_A_TANK.hasEnoughFluid(requiredAmountForA) && INPUT_B_TANK.hasEnoughFluid(requiredAmountForB);
    }

    private Optional<RecipeHolder<EffluencingRecipe>> getRecipeOptional() {
        if (level == null) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getRecipeFor(ModRecipes.EFFLUENCING_TYPE.get(),
                new TwoFluidRecipeInput(INPUT_A_TANK.getFluid(), INPUT_B_TANK.getFluid()), this.level);
    }

    private void resolveRecipe() {
        Optional<RecipeHolder<EffluencingRecipe>> recipeOpt = getRecipeOptional();
        setActiveRecipe(recipeOpt);

        if (isRecipeValid(activeRecipe)) {
            setOrientation();
            setMaxProgress();
            setResultAmount();
        } else {
            resetActiveRecipe();
            resetMaxProgress();
            resetProgress();
            resetResultAmount();
        }
    }

    private void setActiveRecipe(Optional<RecipeHolder<EffluencingRecipe>> opt) {
        if (opt.isPresent()) {
            this.activeRecipe = opt.get();
        } else {
            this.resetActiveRecipe();
            resetMaxProgress(); // Ensures the machine safely idles
        }
    }

    // Recipe matching is order-independent (either input tank can hold either half of the
    // pair), so once a match is found we still need to know which orientation matched, in
    // order to draw the correct required amount from each tank.
    private void setOrientation() {
        EffluencingRecipe recipe = activeRecipe.value();
        Fluid fluidInA = INPUT_A_TANK.getFluid().getFluid();
        Fluid fluidInB = INPUT_B_TANK.getFluid().getFluid();

        if (recipe.isStraightOrientation(fluidInA, fluidInB)) {
            requiredAmountForA = recipe.fluidAAmount();
            requiredAmountForB = recipe.fluidBAmount();
        } else {
            requiredAmountForA = recipe.fluidBAmount();
            requiredAmountForB = recipe.fluidAAmount();
        }
    }

    private void setResultAmount() {
        resultAmount = activeRecipe.value().getCraftingAmount();
    }

    private void resetResultAmount() {
        resultAmount = 0;
        requiredAmountForA = 0;
        requiredAmountForB = 0;
    }

    public void resetActiveRecipe() {
        activeRecipe = null;
    }

    private FluidStack craftResult(int craftAmount) {
        if (isRecipeValid(activeRecipe)) {
            return activeRecipe.value().getResultFluidStack(craftAmount);
        }
        return FluidStack.EMPTY;
    }

    private void incrementProgress(float speedOverride) {
        int workDoneInCycle = Math.round(CRAFT_TICKS * speedOverride);
        progress += Math.max(1, workDoneInCycle);
    }

    private int getHealAmount() {
        return Math.round(BASE_HEAL_RATE * FUEL_TANK.getHeal());
    }

    // Runs every cycle regardless of recipe state -- heals an idle-but-fueled machine too.
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

    private void setMaxProgress() {
        maxProgress = activeRecipe.value().getCraftingTime();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("fuel", FUEL_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("inputA", INPUT_A_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("inputB", INPUT_B_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("output", RESULT_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("speed", speed);
        tag.putInt("use", fuelUseRate);
        tag.putInt("resultFluid", resultAmount);
        tag.putInt("requiredA", requiredAmountForA);
        tag.putInt("requiredB", requiredAmountForB);
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
        if (tag.contains("inputA")) INPUT_A_TANK.readFromNBT(registries, tag.getCompound("inputA"));
        if (tag.contains("inputB")) INPUT_B_TANK.readFromNBT(registries, tag.getCompound("inputB"));
        if (tag.contains("output")) RESULT_TANK.readFromNBT(registries, tag.getCompound("output"));
        speed = tag.getFloat("speed");
        fuelUseRate = tag.getInt("use");
        resultAmount = tag.getInt("resultFluid");
        requiredAmountForA = tag.getInt("requiredA");
        requiredAmountForB = tag.getInt("requiredB");
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.getInt("maxProgress");
        this.health = tag.contains("health") ? tag.getInt("health") : maxHealth;

        if (tag.contains("saved_recipe", CompoundTag.TAG_STRING)) {
            pendingRecipeId = ResourceLocation.parse(tag.getString("saved_recipe"));
        }
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.EFFLUENTCER);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EffluentcerMenu(containerId, playerInventory, this);
    }

    protected ItemStackHandler createItemHandler() {
        return new ItemStackHandler(4) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level == null || level.isClientSide()) return;

                if (isTransferringFluids) return;

                biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);
                biDirectionalFluidTransfer(INPUT_A_TANK, INPUT_A_TANK.SLOT);
                biDirectionalFluidTransfer(INPUT_B_TANK, INPUT_B_TANK.SLOT);
                biDirectionalFluidTransfer(RESULT_TANK, RESULT_TANK.SLOT);

                isTransferringFluids = false;

                setChanged();
                updateBlock();
            }

            private void biDirectionalFluidTransfer(ModFluidTank tank, int slot) {

                if (tank.hasFluidHandlerInSlot(this, slot)) {
                    isTransferringFluids = true;
                    tank.transferFluidToTank(this, slot);

                } else {
                    transferToHandlerWithEject(tank, slot);
                }

            }

            // Fills one empty container from the tank at a time. A stack of empties can only
            // ever reach this slot via the GUI (automation is capped at one item per slot by
            // getItemHandler(Direction)'s wrapper), so a stack here is by definition
            // player-placed -- the newly-filled container can't restack with the remaining
            // empties, so it's ejected to the player instead of being lost.
            private void transferToHandlerWithEject(ModFluidTank tank, int slot) {
                if (!tank.hasEmptyFluidHandlerInSlot(this, slot)) return;

                ItemStack stack = getStackInSlot(slot);
                if (stack.isEmpty()) return;

                isTransferringFluids = true;

                if (stack.getCount() > 1) {
                    ItemStack single = stack.copyWithCount(1);
                    ItemStack remainder = stack.copy();
                    remainder.shrink(1);
                    setStackInSlot(slot, remainder);

                    ItemStackHandler singleHandler = new ItemStackHandler(1);
                    singleHandler.setStackInSlot(0, single);
                    tank.transferFluidFromTankToHandler(singleHandler, 0);

                    ejectToPlayerOrDrop(singleHandler.getStackInSlot(0));
                } else {
                    tank.transferFluidFromTankToHandler(this, slot);
                }
            }
        };
    }

    private void ejectToPlayerOrDrop(ItemStack stack) {
        if (stack.isEmpty() || level == null) return;

        if (interactingPlayer != null && !interactingPlayer.isRemoved()) {
            if (!interactingPlayer.getInventory().add(stack)) {
                interactingPlayer.drop(stack, false);
            }
        } else {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), stack);
        }
    }

    protected FuelTank createFuelTank() {
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

    private VulnerableTank createInputTank(int slot) {
        return new VulnerableTank(FluidType.BUCKET_VOLUME * 5, slot) {
            @Override
            protected void onContentsChanged() {

                if (level != null && !level.isClientSide()) {
                    resolveRecipe();
                }
                setChanged();
            }
        };
    }

    private VulnerableTank createResultTank() {
        return new VulnerableTank(FluidType.BUCKET_VOLUME * 5, 3) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    this.pushFluidToBelowNeighbour(level, worldPosition);
                    setChanged();
                }
            }
        };
    }
}
