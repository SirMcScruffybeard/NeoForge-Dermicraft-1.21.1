package net.scruffy.dermicraft.recipe.puddle_crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

public record PuddleCraftingRecipeInput(List<ItemStack> items, Fluid puddle) implements RecipeInput {

    // Compact constructor to ensure safety and immutability
    public PuddleCraftingRecipeInput {
        // Protect against null pointers; copy the list to keep it decoupled from world entities
        if (items == null) {
            items = List.of();
        } else {
            items = List.copyOf(items);
        }
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(index);
    }

    public List<ItemStack> getItems() {
        return items.stream().filter(stack -> !stack.isEmpty()).toList();
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Fluid getFluid() {
        return puddle;
    }
}
