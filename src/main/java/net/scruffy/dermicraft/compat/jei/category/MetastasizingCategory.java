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
import net.scruffy.dermicraft.recipe.metastasizing.MetastasizingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * Mirrors {@code MetastasizerScreen}'s layout (reagent tank, pattern item slot, arrow, output
 * item slot), offset to a small local bounding box. The pattern item is not consumed by the
 * recipe, so it's registered as a JEI {@link RecipeIngredientRole#CATALYST} rather than a
 * regular input -- JEI renders catalyst slots distinctly to signal "stays put." Fuel tank
 * excluded (shared machine resource, not part of what the recipe itself consumes).
 */
public class MetastasizingCategory implements IRecipeCategory<RecipeHolder<MetastasizingRecipe>> {

    private static final int REAGENT_X = 0, REAGENT_Y = 0;
    private static final int PATTERN_X = 30, PATTERN_Y = 23;
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

    public MetastasizingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.METASTASIZER.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tankAndSlot = JeiTextures.tankAndSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<MetastasizingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.METASTASIZING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("block.dermicraft.metastasizer");
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
    public void draw(RecipeHolder<MetastasizingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        tankAndSlot.draw(guiGraphics, REAGENT_X, REAGENT_Y);
        itemSlot.draw(guiGraphics, PATTERN_X, PATTERN_Y);
        itemSlot.draw(guiGraphics, OUTPUT_X, OUTPUT_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MetastasizingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        MetastasizingRecipe recipe = holder.value();
        int fluidAmount = Math.max(1, recipe.fluidAmount());
        FluidStack reagent = new FluidStack(recipe.fluid(), fluidAmount);

        builder.addSlot(RecipeIngredientRole.INPUT, REAGENT_X + 1, REAGENT_Y + 1)
                .setFluidRenderer(fluidAmount, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, reagent);

        builder.addSlot(RecipeIngredientRole.CATALYST, PATTERN_X + 1, PATTERN_Y + 1)
                .addIngredients(recipe.pattern());

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, OUTPUT_Y + 1)
                .addItemStack(recipe.getResult());
    }
}
