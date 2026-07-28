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
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.HarvestDisplay;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.item.ModItems;
import org.jetbrains.annotations.NotNull;

/**
 * "Scalpel harvests part(s) from tumor" -- generic item(s) -> item(s) layout, matching
 * {@code DippingCategory}'s style, since there's no GUI block or data-driven recipe to mirror
 * (see {@link HarvestDisplay}). The required Scalpel is shown as a static, non-consumed
 * decoration between the tumor and its part(s) rather than a role-typed catalyst, since it's
 * one shared tool across every entry rather than something tied to a specific recipe.
 */
public class HarvestingCategory implements IRecipeCategory<HarvestDisplay> {

    private static final int ROW_Y = 13;
    private static final int TUMOR_X = 0;
    private static final int TOOL_X = 29;
    private static final int ARROW_X = 58, ARROW_Y = ROW_Y + 4;
    private static final int RESULT_X = 85;
    private static final int WIDTH = RESULT_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = ROW_Y + JeiTextures.ITEM_SLOT_SIZE + 12;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public HarvestingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModItems.SCALPEL.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<HarvestDisplay> getRecipeType() {
        return DermicraftRecipeTypes.HARVESTING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.dermicraft.category.harvesting");
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
    public void draw(HarvestDisplay display, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        itemSlot.draw(guiGraphics, TUMOR_X, ROW_Y);
        itemSlot.draw(guiGraphics, TOOL_X, ROW_Y);
        itemSlot.draw(guiGraphics, RESULT_X, ROW_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HarvestDisplay display,
                           @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, TUMOR_X + 1, ROW_Y + 1)
                .addItemStack(display.tumor());

        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, TOOL_X + 1, ROW_Y + 1)
                .addItemStack(new ItemStack(ModItems.SCALPEL.get()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, ROW_Y + 1)
                .addItemStacks(display.possibleParts());
    }
}
