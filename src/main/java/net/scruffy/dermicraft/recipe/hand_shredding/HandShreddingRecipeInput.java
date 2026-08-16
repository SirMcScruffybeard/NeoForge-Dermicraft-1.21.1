package net.scruffy.dermicraft.recipe.hand_shredding;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

public record HandShreddingRecipeInput(ItemStack tool, ItemStack input) implements RecipeInput {
    @Override @NotNull
    public ItemStack getItem(int index) {
        return index == 0 ? tool : input;
    }

    @Override
    public int size() {
        return 2;
    }
}
