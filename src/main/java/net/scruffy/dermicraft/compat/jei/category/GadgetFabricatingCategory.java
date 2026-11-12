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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.recipe.gadget_fabricating.GadgetFabricatingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * The Workbench's Fabrication page: a grid of every item and fluid the recipe consumes together, an arrow, and the
 * finished gadget, with the crafting time and the station tier it needs underneath. A recipe has any number of items
 * and fluids, so the inputs flow left to right, {@value #COLUMNS} to a row (items first, then fluids), and the output
 * stays beside the grid's middle row.
 */
public class GadgetFabricatingCategory implements IRecipeCategory<RecipeHolder<GadgetFabricatingRecipe>> {

    private static final int COLUMNS = 5;
    private static final int ROWS = 3;
    private static final int GRID_WIDTH = COLUMNS * JeiTextures.ITEM_SLOT_SIZE;
    private static final int GRID_HEIGHT = ROWS * JeiTextures.ITEM_SLOT_SIZE;
    private static final int ARROW_X = GRID_WIDTH + 4;
    private static final int ARROW_Y = (GRID_HEIGHT - JeiTextures.ARROW_HEIGHT) / 2;
    private static final int OUTPUT_X = ARROW_X + JeiTextures.ARROW_WIDTH + 4;
    private static final int OUTPUT_Y = (GRID_HEIGHT - JeiTextures.ITEM_SLOT_SIZE) / 2;
    private static final int TEXT_Y = GRID_HEIGHT + 3;
    private static final int TEXT_LINE = 10;
    private static final int TEXT_COLOR = 0xFF808080;
    private static final int WIDTH = OUTPUT_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = TEXT_Y + TEXT_LINE * 2;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public GadgetFabricatingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.WORKBENCH.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<GadgetFabricatingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.GADGET_FABRICATING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.dermicraft.category.gadget_fabricating");
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
    public void draw(RecipeHolder<GadgetFabricatingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GadgetFabricatingRecipe recipe = holder.value();
        int inputs = recipe.items().size() + recipe.fluids().size();
        for (int i = 0; i < inputs; i++) {
            itemSlot.draw(guiGraphics, getInputX(i), getInputY(i));
        }
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
        itemSlot.draw(guiGraphics, OUTPUT_X, OUTPUT_Y);

        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable("jei.dermicraft.gadget_fabricating.time",
                String.format("%.1f", recipe.getCraftingTime() / 20.0F)), 0, TEXT_Y, TEXT_COLOR, false);
        guiGraphics.drawString(font, Component.translatable("jei.dermicraft.gadget_fabricating.tier",
                recipe.requiredTier()), 0, TEXT_Y + TEXT_LINE, TEXT_COLOR, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GadgetFabricatingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        GadgetFabricatingRecipe recipe = holder.value();

        int index = 0;
        for (ItemStack required : recipe.items()) {
            builder.addSlot(RecipeIngredientRole.INPUT, getInputX(index) + 1, getInputY(index) + 1)
                    .addItemStack(required);
            index++;
        }
        for (FluidStack fluid : recipe.fluids()) {
            builder.addSlot(RecipeIngredientRole.INPUT, getInputX(index) + 1, getInputY(index) + 1)
                    .setFluidRenderer(Math.max(1, fluid.getAmount()), false, 16, 16)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, fluid);
            index++;
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, OUTPUT_Y + 1)
                .addItemStack(recipe.getResult());
    }

    private static int getInputX(int index) {
        return (index % COLUMNS) * JeiTextures.ITEM_SLOT_SIZE;
    }

    private static int getInputY(int index) {
        return (index / COLUMNS) * JeiTextures.ITEM_SLOT_SIZE;
    }
}
