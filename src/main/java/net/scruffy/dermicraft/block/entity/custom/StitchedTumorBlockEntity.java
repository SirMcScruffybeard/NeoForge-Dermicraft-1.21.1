package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StitchedTumorBlockEntity extends EarlySurgeryTumorBlockEntity {

    private @Nullable EarlyImplantRecipe cachedRecipe = null;

    private final ItemStackHandler INVENTORY = new ItemStackHandler(16) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public StitchedTumorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STITCHED_TUMOR_BE.get(), pos, blockState);
    }

    public ItemStackHandler getInventory() {
        return this.INVENTORY;
    }

    public void setEvolvingRecipe(EarlyImplantRecipe recipe) {
        cachedRecipe = recipe;
        setChanged();
    }

    public @Nullable EarlyImplantRecipe getCachedRecipe() {
        return this.cachedRecipe;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("incubating_inventory", this.INVENTORY.serializeNBT(registries));

        if (this.cachedRecipe != null) {
            // Using ModRecipes registry lookup shortcut or standard serialization matching
            ResourceLocation recipeId = this.level != null ? this.level.getRecipeManager().getAllRecipesFor(ModRecipes.EARLY_IMPLANT_TYPE.get()).stream().filter(holder -> holder.value() == this.cachedRecipe).map(net.minecraft.world.item.crafting.RecipeHolder::id).findFirst().orElse(null) : null;

            if (recipeId != null) {
                tag.putString("evolving_recipe_id", recipeId.toString());
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("incubating_inventory")) {
            INVENTORY.deserializeNBT(registries, tag.getCompound("incubating_inventory"));
        }
        if (tag.contains("evolving_recipe_id")) {
            this.lazyRecipeId = ResourceLocation.parse(tag.getString("evolving_recipe_id"));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Re-bind the cached recipe instance when the chunk loads back into memory
        if (this.level != null && !this.level.isClientSide && this.lazyRecipeId != null) {
            this.level.getRecipeManager().byKey(this.lazyRecipeId).ifPresent(holder -> {
                if (holder.value() instanceof EarlyImplantRecipe earlyRecipe) {
                    this.cachedRecipe = earlyRecipe;
                }
            });
            this.lazyRecipeId = null;
        }
    }
}
