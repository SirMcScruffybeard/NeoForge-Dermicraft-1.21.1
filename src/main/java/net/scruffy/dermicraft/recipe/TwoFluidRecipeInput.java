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

    // RecipeInput.isEmpty() defaults to an item-based check (size()==0 short-circuits it to
    // true unconditionally, since this input carries no items) -- RecipeManager.getRecipeFor
    // uses isEmpty() as an early-out before matches() is ever called, so a fluid-only input
    // must override this or it always gets treated as empty regardless of fluid content.
    @Override
    public boolean isEmpty() {
        return fluidA.isEmpty() && fluidB.isEmpty();
    }
}
