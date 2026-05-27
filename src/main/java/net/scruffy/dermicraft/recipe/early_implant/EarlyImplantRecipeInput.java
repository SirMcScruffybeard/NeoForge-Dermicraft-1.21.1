package net.scruffy.dermicraft.recipe.early_implant;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public class EarlyImplantRecipeInput implements RecipeInput {
    private final List<ItemStack> items;

    public EarlyImplantRecipeInput(List<ItemStack> items) {

        this.items = items.stream().filter(stack -> !stack.isEmpty()).toList();
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(index);
    }

    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }
}
