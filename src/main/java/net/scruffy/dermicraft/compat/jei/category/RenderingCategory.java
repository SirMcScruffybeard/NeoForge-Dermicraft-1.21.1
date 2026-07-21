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
import net.scruffy.dermicraft.recipe.rendering.RenderingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * Mirrors {@code RenderKilnScreen}'s layout (input tank, arrow, output item slot) -- like
 * {@code MetastasizingCategory} but with no pattern slot, since the Kiln's output is fixed per
 * input fluid rather than derived from a non-consumed pattern item. Fuel tank excluded (shared
 * machine resource, not part of what the recipe itself consumes).
 */
public class RenderingCategory implements IRecipeCategory<RecipeHolder<RenderingRecipe>> {

    private static final int TANK_X = 0, TANK_Y = 0;
    private static final int ARROW_X = 30, ARROW_Y = 26;
    private static final int OUTPUT_X = 60, OUTPUT_Y = 23;
    private static final int WIDTH = OUTPUT_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable tankAndSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public RenderingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.RENDER_KILN.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tankAndSlot = JeiTextures.tankAndSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<RenderingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.RENDERING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("block.dermicraft.render_kiln");
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
    public void draw(RecipeHolder<RenderingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        tankAndSlot.draw(guiGraphics, TANK_X, TANK_Y);
        itemSlot.draw(guiGraphics, OUTPUT_X, OUTPUT_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RenderingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        RenderingRecipe recipe = holder.value();
        int fluidAmount = Math.max(1, recipe.fluidAmount());
        FluidStack fluid = new FluidStack(recipe.fluid(), fluidAmount);

        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X + 1, TANK_Y + 1)
                .setFluidRenderer(fluidAmount, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, fluid);

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, OUTPUT_Y + 1)
                .addItemStack(recipe.getResult());
    }
}
