package net.scruffy.dermicraft.recipe.gadget_fabricating;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record GadgetFabricatingRecipeInput(List<ItemStack> items, List<FluidStack> fluids) implements RecipeInput {

    public GadgetFabricatingRecipeInput {
        items = items == null ? List.of() : List.copyOf(items);
        fluids = fluids == null ? List.of() : List.copyOf(fluids);
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

    public List<FluidStack> getFluids() {
        return fluids.stream().filter(stack -> !stack.isEmpty()).toList();
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return getItems().isEmpty() && getFluids().isEmpty();
    }
}
