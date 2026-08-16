package net.scruffy.dermicraft.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.recipe.hand_shredding.HandShreddingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * "Tool in one hand, item in the other, right-click in air" -- generic layout for
 * {@link HandShreddingRecipe}, styled like {@code HarvestingCategory} but with a real tool slot
 * (consumed or damaged per-recipe, unlike Harvesting's always-static Scalpel) since the tool
 * isn't a single shared catalyst here.
 */
public class HandShreddingCategory implements IRecipeCategory<RecipeHolder<HandShreddingRecipe>> {

    private static final int ROW_Y = 13;
    private static final int TOOL_X = 0;
    private static final int INPUT_X = 29;
    private static final int ARROW_X = 58, ARROW_Y = ROW_Y + 4;
    private static final int RESULT_X = 85;
    private static final int WIDTH = RESULT_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = ROW_Y + JeiTextures.ITEM_SLOT_SIZE + 12;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public HandShreddingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModItems.SCALPEL.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<HandShreddingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.HAND_SHREDDING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.dermicraft.category.hand_shredding");
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
    public void draw(RecipeHolder<HandShreddingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        itemSlot.draw(guiGraphics, TOOL_X, ROW_Y);
        itemSlot.draw(guiGraphics, INPUT_X, ROW_Y);
        itemSlot.draw(guiGraphics, RESULT_X, ROW_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<HandShreddingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        HandShreddingRecipe recipe = holder.value();

        builder.addSlot(recipe.consumeTool() ? RecipeIngredientRole.INPUT : RecipeIngredientRole.CATALYST,
                        TOOL_X + 1, ROW_Y + 1)
                .addIngredients(recipe.tool());

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X + 1, ROW_Y + 1)
                .addIngredients(recipe.input());

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, ROW_Y + 1)
                .addItemStack(recipe.getResultItem(null));
    }
}
