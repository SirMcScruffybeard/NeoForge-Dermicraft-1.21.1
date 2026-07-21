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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Early Implant has no GUI block -- ingredients are incubated inside a Stitched/Marred Tumor
 * block entity, unlocked by right-clicking with a Suture Kit (registered as this category's
 * catalyst), so this category shows an ingredient grid, the required suture tool as a
 * non-consumed catalyst slot, and a reagent fluid feeding the result item.
 */
public class EarlyImplantCategory implements IRecipeCategory<RecipeHolder<EarlyImplantRecipe>> {

    private static final int MAX_INGREDIENTS = 8;
    private static final int GRID_X = 0, GRID_Y_ROW1 = 0, GRID_Y_ROW2 = 20;
    private static final int SUTURE_X = 90, SUTURE_Y = 21;
    private static final int TANK_X = 119, TANK_Y = 0;
    private static final int ARROW_X = 148, ARROW_Y = 27;
    private static final int RESULT_X = 176, RESULT_Y = 23;
    private static final int WIDTH = RESULT_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT + 12;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable tankAndSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public EarlyImplantCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModItems.SUTURE_KIT.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tankAndSlot = JeiTextures.tankAndSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<EarlyImplantRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.EARLY_IMPLANT;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.dermicraft.category.early_implant");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    private static int gridX(int index) {
        return GRID_X + (index % 4) * 20;
    }

    private static int gridY(int index) {
        return index < 4 ? GRID_Y_ROW1 : GRID_Y_ROW2;
    }

    @Override
    public void draw(RecipeHolder<EarlyImplantRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        EarlyImplantRecipe recipe = holder.value();
        List<Ingredient> ingredients = recipe.ingredients();
        for (int i = 0; i < Math.min(ingredients.size(), MAX_INGREDIENTS); i++) {
            itemSlot.draw(guiGraphics, gridX(i), gridY(i));
        }
        itemSlot.draw(guiGraphics, SUTURE_X, SUTURE_Y);
        tankAndSlot.draw(guiGraphics, TANK_X, TANK_Y);
        itemSlot.draw(guiGraphics, RESULT_X, RESULT_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);

        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.dermicraft.category.early_implant.description"),
                0, HEIGHT - 10, 0x808080, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EarlyImplantRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        EarlyImplantRecipe recipe = holder.value();
        List<Ingredient> ingredients = recipe.ingredients();
        for (int i = 0; i < Math.min(ingredients.size(), MAX_INGREDIENTS); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, gridX(i) + 1, gridY(i) + 1)
                    .addIngredients(ingredients.get(i));
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, SUTURE_X + 1, SUTURE_Y + 1)
                .addIngredients(recipe.sutureTool());

        FluidStack fluid = recipe.fluidIngredient();
        int fluidAmount = Math.max(1, fluid.getAmount());
        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X + 1, TANK_Y + 1)
                .setFluidRenderer(fluidAmount, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid.getFluid(), fluidAmount));

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
                .addItemStack(recipe.getResultItem(null));
    }
}
