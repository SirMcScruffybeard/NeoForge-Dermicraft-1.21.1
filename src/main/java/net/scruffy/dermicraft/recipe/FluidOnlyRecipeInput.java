package net.scruffy.dermicraft.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public record FluidOnlyRecipeInput(FluidStack fluid) implements RecipeInput {
    // size() is 0, so nothing in-engine should call this with a valid index -- but returning EMPTY
    // rather than throwing keeps any incidental caller (recipe-book placement, JEI/other mods probing
    // the recipe manager, a future engine optimization) from taking down this block entity's tick loop
    // with an uncaught exception. A thrown exception here silently stops the machine from ever ticking
    // again with no visible error -- exactly the "nothing happens, no crash shown" failure mode.
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
    // Mirrors TwoFluidRecipeInput's identical fix.
    @Override
    public boolean isEmpty() {
        return fluid.isEmpty();
    }

    public FluidStack getFluid() {
        return fluid;
    }
}
