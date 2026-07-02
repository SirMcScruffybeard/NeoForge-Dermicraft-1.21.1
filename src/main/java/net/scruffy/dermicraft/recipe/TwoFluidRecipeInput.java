package net.scruffy.dermicraft.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public record TwoFluidRecipeInput(FluidStack fluidA, FluidStack fluidB) implements RecipeInput {
    @Override @NotNull
    public ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
