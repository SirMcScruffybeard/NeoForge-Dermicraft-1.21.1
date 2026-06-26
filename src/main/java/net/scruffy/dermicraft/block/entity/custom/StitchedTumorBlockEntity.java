package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import org.jetbrains.annotations.NotNull;

public class StitchedTumorBlockEntity extends EarlySurgeryTumorBlockEntity {

    private static final int INVENTORY_SIZE = 16;
    private static final String RECIPE_KEY = "evolving_recipe_id";

    private final ItemStackHandler INVENTORY = createInventory(INVENTORY_SIZE);

    public StitchedTumorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STITCHED_TUMOR_BE.get(), pos, blockState);
    }

    public ItemStackHandler getInventory() {
        return this.INVENTORY;
    }

    public void setEvolvingRecipe(RecipeHolder<EarlyImplantRecipe> recipeHolder) {
        this.cachedRecipeHolder = recipeHolder;
        setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("incubating_inventory", this.INVENTORY.serializeNBT(registries));
        saveRecipeHolder(tag, RECIPE_KEY);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("incubating_inventory")) {
            INVENTORY.deserializeNBT(registries, tag.getCompound("incubating_inventory"));
        }
        loadLazyRecipeId(tag, RECIPE_KEY);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Re-bind the cached recipe instance when the chunk loads back into memory
        resolveLazyRecipeHolder();
    }
}
