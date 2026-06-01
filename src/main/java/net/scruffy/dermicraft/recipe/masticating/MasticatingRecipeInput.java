package net.scruffy.dermicraft.recipe.masticating;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public record MasticatingRecipeInput(ItemStack item, FluidStack fluid) implements RecipeInput {
    @Override @NotNull
    public  ItemStack getItem(int index) {
        return item;
    }

    @Override
    public int size() {
        return 1;
    }

    public FluidStack getFluid() {
        return fluid;
    }
}
