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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.recipe.dipping.DippingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * Dipping has no GUI block to mirror -- it's triggered by right-clicking a fluid-filled tank
 * (any block tagged {@code dermicraft:dipping_tanks}) with an item in hand, so this category
 * uses a generic item + fluid -> item layout with an instructional line instead of copying a
 * real screen.
 */
public class DippingCategory implements IRecipeCategory<RecipeHolder<DippingRecipe>> {

    private static final int ITEM_X = 0, ITEM_Y = 23;
    private static final int TANK_X = 29, TANK_Y = 0;
    private static final int ARROW_X = 58, ARROW_Y = 27;
    private static final int RESULT_X = 85, RESULT_Y = 23;
    private static final int WIDTH = RESULT_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT + 12;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable tankAndSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public DippingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new net.minecraft.world.item.ItemStack(Items.WATER_BUCKET));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tankAndSlot = JeiTextures.tankAndSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<DippingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.DIPPING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.dermicraft.category.dipping");
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
    public void draw(RecipeHolder<DippingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        itemSlot.draw(guiGraphics, ITEM_X, ITEM_Y);
        tankAndSlot.draw(guiGraphics, TANK_X, TANK_Y);
        itemSlot.draw(guiGraphics, RESULT_X, RESULT_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);

        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.dermicraft.category.dipping.description"),
                0, HEIGHT - 10, 0x808080, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<DippingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        DippingRecipe recipe = holder.value();
        int fluidAmount = Math.max(1, recipe.ingredientFluidAmount());
        FluidStack fluid = new FluidStack(recipe.ingredientFluid(), fluidAmount);

        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_X + 1, ITEM_Y + 1)
                .addIngredients(recipe.ingredient());

        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X + 1, TANK_Y + 1)
                .setFluidRenderer(fluidAmount, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, fluid);

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
                .addItemStack(recipe.getResultItem(null));
    }
}
