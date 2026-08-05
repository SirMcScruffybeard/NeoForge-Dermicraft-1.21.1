package net.scruffy.dermicraft.recipe.early_incubating;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.Block;

/**
 * The block being injected (e.g. Craw) plus its single stored bulk stack -- mirrors what
 * {@code CrawBlockEntity}'s own storage slot actually holds, so matching never needs a
 * per-slot pool loop the way {@link net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe}
 * does for a multi-slot Tumor inventory.
 */
public record EarlyIncubatingRecipeInput(Block block, ItemStack stored) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? stored : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return stored.isEmpty();
    }
}
