package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronMenu;
import net.scruffy.dermicraft.tank.WaterTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DroolingCauldronBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory {

    public final static int INPUT = 0;
    public final static int OUTPUT = 1;

    public final ItemStackHandler INVENTORY = createInventory();
    private final WaterTank TANK = createTank();

    private RecipeHolder<VagueDroolingRecipe> activeRecipe = null;
    private Item activeItem = Items.AIR;

    private int resultAmount = 0;

    @Nullable
    private ResourceLocation pendingRecipeId = null;

    public DroolingCauldronBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DROOLING_CAULDRON_BE.get(), pos, blockState);
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    public IFluidHandler getTank(@Nullable Direction face) {
        return TANK;
    }

    public IItemHandler getItemHandler(@Nullable Direction face) {
        if (face == null) return INVENTORY;

        if (face == Direction.UP) {
            return new IItemHandlerModifiable() {
                @Override
                public void setStackInSlot(int slot, ItemStack itemStack) {
                    INVENTORY.setStackInSlot(INPUT, itemStack);
                }

                @Override
                public int getSlots() {
                    return 1;
                }

                @Override
                public ItemStack getStackInSlot(int slot) {
                    return INVENTORY.getStackInSlot(INPUT);
                }

                @Override
                public ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate) {
                    return INVENTORY.insertItem(INPUT, itemStack, simulate);
                }

                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return INVENTORY.extractItem(INPUT, amount, simulate);
                }

                @Override
                public int getSlotLimit(int i) {
                    return INVENTORY.getSlotLimit(INPUT);
                }

                @Override
                public boolean isItemValid(int i, ItemStack itemStack) {
                    return INVENTORY.isItemValid(INPUT, itemStack);
                }
            };
        }
        return null;
    }

    @Override
    public void drops() {
        dropItems(level, INVENTORY, worldPosition);
    }

    public ItemStack insertItemStack(ItemStack stack) {
        return insertItemStack(INVENTORY, INPUT, stack);
    }

    public ItemStack extractItemStack() {
        return extractItem(INVENTORY, INPUT, Integer.MAX_VALUE);
    }

    public void tick(Level level) {
        if (level.isClientSide) return;

        resolvePendingRecipe(level);

        //////////Every Second (20 ticks)\\\\\\\\\\
        if (ModMath.Time.hasTicksPassed(level, ModMath.Time.getSecondsToTicks(1))) {
            TANK.safeFill(new FluidStack(Fluids.WATER, 1));
            setChanged();
            updateBlock();
        }

        //////////Craft\\\\\\\\\\
        if (ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) {
            if (!isRecipeValid(activeRecipe)) {
                if (progress > 0) {
                    resetProgress();
                }
                return;
            }

            if (isMaxProgressValid() && hasIngredients() && TANK.hasRoom(resultAmount)) {
                if (isStillCrafting()) {
                    incrementProgress();
                    setChanged();

                } else {
                    TANK.fill(activeRecipe.value().getResultFluidStack(resultAmount), IFluidHandler.FluidAction.EXECUTE);
                    INVENTORY.extractItem(INPUT, 1, false);
                    resetProgress();
                }
            } else {
                resetProgress();
            }
        }
    }

    private boolean hasIngredients() {
        return !INVENTORY.getStackInSlot(INPUT).isEmpty();
    }

    private void resolvePendingRecipe(Level level) {
        if (pendingRecipeId == null) return;

        level.getRecipeManager().byKey(pendingRecipeId).ifPresent(recipeHolder -> {
            if (recipeHolder.value() instanceof VagueDroolingRecipe) {
                this.activeRecipe = (RecipeHolder<VagueDroolingRecipe>) recipeHolder;
            }
        });
        pendingRecipeId = null;
    }

    private void setActiveRecipe() {
        if (level == null) {
            activeRecipe = null;
            return;
        }

        Optional<RecipeHolder<VagueDroolingRecipe>> opt = level.getRecipeManager()
                .getRecipeFor(ModRecipes.VAGUE_DROOLING_TYPE.get(),
                        new SingleRecipeInput(INVENTORY.getStackInSlot(INPUT)), level);

        opt.ifPresent(vagueDroolingRecipeRecipeHolder -> activeRecipe = vagueDroolingRecipeRecipeHolder);

    }

    public void setResultAmount() {
        resultAmount = activeRecipe.value().getCraftingAmount(INVENTORY.getStackInSlot(INPUT));
    }

    private void resetActiveRecipe() {
        activeRecipe = null;
    }

    private void resetActiveItem() {
        activeItem = Items.AIR;
    }

    private void resetResultAmount() {
        resultAmount = 0;
    }

    private void setMaxProgress() {
        maxProgress = activeRecipe.value().getCraftingTime(INVENTORY.getStackInSlot(INPUT));
    }

    private void incrementProgress() {
        progress += CRAFT_TICKS;
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return super.getDisplayName(ModBlocks.DROOLING_CAULDRON);
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroolingCauldronMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("dc_items", INVENTORY.serializeNBT(registries));
        tag.put("dc_tank", TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("dc_progress", progress);
        tag.putInt("dc_max", maxProgress);
        tag.putInt("resultFluid", resultAmount);
        if (isRecipeValid(activeRecipe)) tag.putString("saved_recipe", activeRecipe.id().toString());
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(this.activeItem);
        tag.putString("activeItem", itemKey.toString());
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("dc_items")) INVENTORY.deserializeNBT(registries, tag.getCompound("dc_items"));
        if (tag.contains("dc_tank")) TANK.readFromNBT(registries, tag.getCompound("dc_tank"));
        this.progress = tag.getInt("dc_progress");
        this.maxProgress = tag.getInt("dc_max");
        resultAmount = tag.getInt("resultFluid");

        if (tag.contains("saved_recipe", CompoundTag.TAG_STRING)) {
            pendingRecipeId = ResourceLocation.parse(tag.getString("saved_recipe"));
        }
    }

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(2) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide()) {

                    if (slot == INPUT) {
                        ItemStack stack = getStackInSlot(INPUT);
                        Item currentItem = stack.getItem();

                        if (stack.isEmpty()) {
                            resetProgress();
                            resetMaxProgress();
                            resetActiveItem();
                            resetActiveRecipe();
                            resetResultAmount();
                        }

                        if (currentItem != activeItem) {
                            activeItem = currentItem;
                            resetProgress();
                            resetMaxProgress();

                            setActiveRecipe();

                            if (isRecipeValid(activeRecipe)) {
                                setMaxProgress();
                                setResultAmount();
                            } else {
                                resetActiveRecipe();
                                resetProgress();
                                resetMaxProgress();
                                resetResultAmount();
                            }
                        }
                    }

                    if (slot == OUTPUT) {
                        if (TANK.hasEmptyFluidHandlerInSlot(INVENTORY, OUTPUT)) {
                            TANK.transferFluidFromTankToHandler(INVENTORY, OUTPUT);
                        }
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

    private WaterTank createTank() {
        return new WaterTank(WaterTank.BUCKET_VOLUME * 5, -1) {

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
