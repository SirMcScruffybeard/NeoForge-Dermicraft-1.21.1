package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipeInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MarredTumorBlockEntity extends EarlySurgeryTumorBlockEntity {

    protected final ItemStackHandler INVENTORY = new ItemStackHandler(16) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                // Instantly re-evaluate if the items inside form a valid recipe
                updateRecipeCache();
                // Synchronize inventory changes to the client for rendering purposes
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    protected @Nullable RecipeHolder<EarlyImplantRecipe> cachedRecipeHolder = null;

    public MarredTumorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MARRED_TUMOR_BE.get(), pos, blockState);
    }

    public ItemStackHandler getInventory() {
        return this.INVENTORY;
    }

    public @Nullable EarlyImplantRecipe getCachedRecipe() {
        return this.cachedRecipeHolder != null ? this.cachedRecipeHolder.value() : null;
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(INVENTORY.getSlots());
        for (int i = 0; i < INVENTORY.getSlots(); i++) {
            if (!inv.getItem(i).isEmpty()) {
                inv.setItem(i, INVENTORY.getStackInSlot(i));
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
        if (this.currentState != TumorState.EMPTY) return false;

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
        tag.putString("state", this.currentState.name());

        if (this.cachedRecipeHolder != null) {
            tag.putString("held_recipe_id", this.cachedRecipeHolder.id().toString());
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            this.INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("state")) {
            try {
                this.currentState = TumorState.valueOf(tag.getString("state"));
            } catch (IllegalArgumentException e) {
                this.currentState = TumorState.EMPTY;
            }
        }
        if (tag.contains("held_recipe_id")) {
            this.lazyRecipeId = ResourceLocation.parse(tag.getString("held_recipe_id"));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Once the block entity is fully bound to the world, safely map our lazy loaded ID back to the recipe object
        if (this.level != null && !this.level.isClientSide) {
            if (this.lazyRecipeId != null) {
                this.level.getRecipeManager().byKey(this.lazyRecipeId).ifPresent(holder -> {
                    if (holder.value() instanceof EarlyImplantRecipe) {
                        // Suppress warnings by casting cleanly to our explicit type holder
                        //lmdx-escape-checked
                        @SuppressWarnings("unchecked") RecipeHolder<EarlyImplantRecipe> typeSafeHolder = (RecipeHolder<EarlyImplantRecipe>) holder;
                        this.cachedRecipeHolder = typeSafeHolder;
                    }
                });
                this.lazyRecipeId = null;
            } else {
                updateRecipeCache();
            }
        }
    }
}
