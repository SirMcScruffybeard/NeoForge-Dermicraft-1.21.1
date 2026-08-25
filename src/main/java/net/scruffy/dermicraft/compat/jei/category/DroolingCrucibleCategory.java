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
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.DynamicRecipeDisplay;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingCrucibleRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * Sibling of {@link DroolingCauldronCategory} -- identical layout (Crucible's screen is a
 * straight copy of Cauldron's), only the recipe class/type differ. See that class's own javadoc.
 */
public class DroolingCrucibleCategory implements IRecipeCategory<DynamicRecipeDisplay<VagueDroolingCrucibleRecipe>> {

    private static final int ITEM_X = 0, ITEM_Y = 26;
    private static final int TANK_X = 37, TANK_Y = 0;
    private static final int ARROW_X = 19, ARROW_Y = 30;
    private static final int WIDTH = TANK_X + JeiTextures.TANK_WIDTH;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable tallTank;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public DroolingCrucibleCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.DROOLING_CRUCIBLE.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tallTank = JeiTextures.tallTank(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<DynamicRecipeDisplay<VagueDroolingCrucibleRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.DROOLING_CRUCIBLE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("block.dermicraft.drooling_crucible");
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
    public void draw(DynamicRecipeDisplay<VagueDroolingCrucibleRecipe> display, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        itemSlot.draw(guiGraphics, ITEM_X, ITEM_Y);
        tallTank.draw(guiGraphics, TANK_X, TANK_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DynamicRecipeDisplay<VagueDroolingCrucibleRecipe> display,
                           @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_X + 1, ITEM_Y + 1)
                .addItemStack(display.concreteIngredient());

        builder.addSlot(RecipeIngredientRole.OUTPUT, TANK_X + 1, TANK_Y + 1)
                .setFluidRenderer(Math.max(1, display.computedResult().getAmount()), false, 16, 64)
                .addIngredient(NeoForgeTypes.FLUID_STACK, display.computedResult());
    }
}
