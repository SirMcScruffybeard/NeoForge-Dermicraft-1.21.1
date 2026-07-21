package net.scruffy.dermicraft.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.recipe.effluencing.EffluencingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * Mirrors {@code EffluentcerScreen}'s layout (input A tank, input B tank, arrow, result tank),
 * offset to a small local bounding box. Fluid-only recipe, no item slots; fuel tank excluded
 * (shared machine resource, not part of what the recipe itself consumes).
 */
public class EffluencingCategory implements IRecipeCategory<RecipeHolder<EffluencingRecipe>> {

    private static final int INPUT_A_X = 0, INPUT_B_X = 20, RESULT_X = 72, TANK_Y = 0;
    private static final int ARROW_X = 46, ARROW_Y = 27;
    private static final int WIDTH = RESULT_X + JeiTextures.TANK_WIDTH;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable tankAndSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public EffluencingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.EFFLUENTCER.get()));
        this.tankAndSlot = JeiTextures.tankAndSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<EffluencingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.EFFLUENCING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("block.dermicraft.effluentcer");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(RecipeHolder<EffluencingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        tankAndSlot.draw(guiGraphics, INPUT_A_X, TANK_Y);
        tankAndSlot.draw(guiGraphics, INPUT_B_X, TANK_Y);
        tankAndSlot.draw(guiGraphics, RESULT_X, TANK_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EffluencingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        EffluencingRecipe recipe = holder.value();
        int amountA = Math.max(1, recipe.fluidAAmount());
        int amountB = Math.max(1, recipe.fluidBAmount());
        FluidStack fluidA = new FluidStack(recipe.fluidA(), amountA);
        FluidStack fluidB = new FluidStack(recipe.fluidB(), amountB);
        FluidStack result = recipe.getResultFluidStack(Math.max(1, recipe.resultAmount()));

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_A_X + 1, TANK_Y + 1)
                .setFluidRenderer(amountA, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, fluidA);

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_B_X + 1, TANK_Y + 1)
                .setFluidRenderer(amountB, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, fluidB);

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, TANK_Y + 1)
                .setFluidRenderer(result.getAmount(), false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, result);
    }
}
