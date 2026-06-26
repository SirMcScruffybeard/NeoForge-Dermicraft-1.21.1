package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MarredTumorBlockEntity extends EarlySurgeryTumorBlockEntity {

    private static final int INVENTORY_SIZE = 16;
    private static final String RECIPE_KEY = "held_recipe_id";

    protected final ItemStackHandler INVENTORY = createInventory(INVENTORY_SIZE);

    public MarredTumorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MARRED_TUMOR_BE.get(), pos, blockState);
    }

    public ItemStackHandler getInventory() {
        return this.INVENTORY;
    }

    @Override
    protected void onInventoryChanged(int slot) {
        super.onInventoryChanged(slot);
        if (level != null && !level.isClientSide) {
            // Instantly re-evaluate if the items inside form a valid recipe
            updateRecipeCache();
            // Synchronize inventory changes to the client for rendering purposes
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(INVENTORY.getSlots());
        for (int i = 0; i < INVENTORY.getSlots(); i++) {
            ItemStack stack = INVENTORY.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inv.setItem(i, stack);
            }
        }
        Containers.dropContents(level, worldPosition, inv);
    }

    public void updateRecipeCache() {
        if (this.level == null || this.level.isClientSide) return;

        // Compile a clean list of existing item layers wrapped inside our custom recipe input
        List<ItemStack> currentItems = new ArrayList<>();
        for (int i = 0; i < INVENTORY.getSlots(); i++) {
            ItemStack stack = INVENTORY.getStackInSlot(i);
            if (!stack.isEmpty()) {
                currentItems.add(stack);
            }
        }

        EarlyImplantRecipeInput input = new EarlyImplantRecipeInput(currentItems);

        // Find the matching blueprint from the server registry data
        this.cachedRecipeHolder = this.level.getRecipeManager().getRecipeFor(ModRecipes.EARLY_IMPLANT_TYPE.get(), input, this.level).orElse(null);
    }


    /**
     * Attempts to push one single item unit from the player's hand into the tumor.
     *
     * @return true if the item was successfully taken and absorbed into the mass.
     */
    public boolean insertItem(ItemStack playerHandStack) {
        for (int i = 0; i < INVENTORY.getSlots(); i++) {
            if (INVENTORY.getStackInSlot(i).isEmpty()) {
                // Split 1 unit to insert cleanly into the empty slot
                ItemStack insertStack = playerHandStack.copyWithCount(1);
                ItemStack remainder = INVENTORY.insertItem(i, insertStack, false);

                if (remainder.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", this.INVENTORY.serializeNBT(registries));
        saveRecipeHolder(tag, RECIPE_KEY);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            this.INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        loadLazyRecipeId(tag, RECIPE_KEY);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Once the block entity is fully bound to the world, safely map our lazy loaded ID back to the recipe object
        if (this.level != null && !this.level.isClientSide) {
            if (this.lazyRecipeId != null) {
                resolveLazyRecipeHolder();
            } else {
                updateRecipeCache();
            }
        }
    }
}
