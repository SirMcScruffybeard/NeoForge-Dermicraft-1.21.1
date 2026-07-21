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
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.DynamicRecipeDisplay;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * Mirrors {@code MasticatorScreen}'s layout (item slot, reagent tank, arrow, result tank),
 * offset to a small local bounding box. The fuel tank is deliberately excluded -- fuel is a
 * shared machine resource, not part of what a given recipe consumes/produces.
 */
public class MasticatingCategory implements IRecipeCategory<DynamicRecipeDisplay<MasticatingRecipe>> {

    private static final int ITEM_X = 0, ITEM_Y = 23;
    private static final int REAGENT_X = 29, REAGENT_Y = 0;
    private static final int ARROW_X = 58, ARROW_Y = 27;
    private static final int RESULT_X = 85, RESULT_Y = 0;
    private static final int WIDTH = RESULT_X + JeiTextures.TANK_WIDTH;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable tankAndSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public MasticatingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.MASTICATOR.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tankAndSlot = JeiTextures.tankAndSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<DynamicRecipeDisplay<MasticatingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.MASTICATING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("block.dermicraft.masticator");
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
    public void draw(DynamicRecipeDisplay<MasticatingRecipe> display, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        itemSlot.draw(guiGraphics, ITEM_X, ITEM_Y);
        tankAndSlot.draw(guiGraphics, REAGENT_X, REAGENT_Y);
        tankAndSlot.draw(guiGraphics, RESULT_X, RESULT_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DynamicRecipeDisplay<MasticatingRecipe> display,
                           @NotNull IFocusGroup focuses) {
        MasticatingRecipe recipe = display.source().value();
        int reagentAmount = Math.max(1, recipe.ingredientFluidAmount());
        FluidStack reagent = new FluidStack(recipe.ingredientFluid(), reagentAmount);
        FluidStack result = display.computedResult();

        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_X + 1, ITEM_Y + 1)
                .addItemStack(display.concreteIngredient());

        builder.addSlot(RecipeIngredientRole.INPUT, REAGENT_X + 1, REAGENT_Y + 1)
                .setFluidRenderer(reagentAmount, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, reagent);

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
                .setFluidRenderer(Math.max(1, result.getAmount()), false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, result);
    }
}
