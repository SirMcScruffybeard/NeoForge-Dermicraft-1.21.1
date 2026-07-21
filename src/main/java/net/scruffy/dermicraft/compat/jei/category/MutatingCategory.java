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
import net.scruffy.dermicraft.recipe.mutating.MutatingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * Mirrors {@code MutatorScreen}'s "mutate" mode layout (reagent tank, input item slot, arrow,
 * output item slot), offset to a small local bounding box. Unlike the Metastasizer's pattern,
 * the input item here is fully consumed, so it's a regular {@link RecipeIngredientRole#INPUT}.
 * Fuel tank excluded (shared machine resource, not part of what the recipe itself consumes);
 * the Mutator's "fill" mode (which has no discrete recipe) has no JEI category.
 */
public class MutatingCategory implements IRecipeCategory<RecipeHolder<MutatingRecipe>> {

    private static final int REAGENT_X = 0, REAGENT_Y = 0;
    private static final int INPUT_X = 30, INPUT_Y = 23;
    private static final int ARROW_X = 60, ARROW_Y = 26;
    private static final int OUTPUT_X = 90, OUTPUT_Y = 23;
    private static final int WIDTH = OUTPUT_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable tankAndSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public MutatingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.MUTATOR.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tankAndSlot = JeiTextures.tankAndSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<MutatingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.MUTATING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("block.dermicraft.mutator");
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
    public void draw(RecipeHolder<MutatingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        tankAndSlot.draw(guiGraphics, REAGENT_X, REAGENT_Y);
        itemSlot.draw(guiGraphics, INPUT_X, INPUT_Y);
        itemSlot.draw(guiGraphics, OUTPUT_X, OUTPUT_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MutatingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        MutatingRecipe recipe = holder.value();
        int fluidAmount = Math.max(1, recipe.fluidAmount());
        FluidStack reagent = new FluidStack(recipe.fluid(), fluidAmount);

        builder.addSlot(RecipeIngredientRole.INPUT, REAGENT_X + 1, REAGENT_Y + 1)
                .setFluidRenderer(fluidAmount, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, reagent);

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X + 1, INPUT_Y + 1)
                .addIngredients(recipe.ingredient());

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, OUTPUT_Y + 1)
                .addItemStack(recipe.getResult());
    }
}
