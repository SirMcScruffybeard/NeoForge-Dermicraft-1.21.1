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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.scruffy.dermicraft.compat.jei.DermicraftRecipeTypes;
import net.scruffy.dermicraft.compat.jei.JeiTextures;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Puddle Crafting has no GUI block -- it's triggered by dropping items into a fluid in the
 * world (see {@code PuddleCraftEvent}), so this category shows the ingredient list and puddle
 * fluid feeding an arrow toward the result fluid/item, with an instructional line, instead of
 * copying a real screen.
 */
public class PuddleCraftingCategory implements IRecipeCategory<RecipeHolder<PuddleCraftingRecipe>> {

    private static final int MAX_INGREDIENTS = 4;
    private static final int GRID_X = 0, GRID_Y = 4;
    private static final int TANK_X = 90, TANK_Y = 0;
    private static final int ARROW_X = 119, ARROW_Y = 27;
    private static final int RESULT_TANK_X = 148, RESULT_TANK_Y = 0;
    private static final int RESULT_ITEM_X = 173, RESULT_ITEM_Y = 23;
    private static final int WIDTH = RESULT_ITEM_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT + 12;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable tallTank;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public PuddleCraftingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(Items.WATER_BUCKET));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tallTank = JeiTextures.tallTank(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<PuddleCraftingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.PUDDLE_CRAFTING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.dermicraft.category.puddle_crafting");
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
    public void draw(RecipeHolder<PuddleCraftingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        PuddleCraftingRecipe recipe = holder.value();
        List<Ingredient> ingredients = recipe.ingredients();
        for (int i = 0; i < Math.min(ingredients.size(), MAX_INGREDIENTS); i++) {
            itemSlot.draw(guiGraphics, GRID_X + i * 20, GRID_Y);
        }
        tallTank.draw(guiGraphics, TANK_X, TANK_Y);
        if (recipe.resultFluid() != null && recipe.resultFluid() != Fluids.EMPTY) {
            tallTank.draw(guiGraphics, RESULT_TANK_X, RESULT_TANK_Y);
        }
        if (recipe.resultItem() != null && recipe.resultItem() != Items.AIR) {
            itemSlot.draw(guiGraphics, RESULT_ITEM_X, RESULT_ITEM_Y);
        }
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);

        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.dermicraft.category.puddle_crafting.description"),
                0, HEIGHT - 10, 0x808080, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PuddleCraftingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        PuddleCraftingRecipe recipe = holder.value();
        List<Ingredient> ingredients = recipe.ingredients();
        for (int i = 0; i < Math.min(ingredients.size(), MAX_INGREDIENTS); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, GRID_X + i * 20 + 1, GRID_Y + 1)
                    .addIngredients(ingredients.get(i));
        }

        Fluid puddle = recipe.puddle();
        FluidStack puddleStack = new FluidStack(puddle, FluidType.BUCKET_VOLUME);
        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X + 1, TANK_Y + 1)
                .setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 64)
                .addIngredient(NeoForgeTypes.FLUID_STACK, puddleStack);

        Fluid resultFluid = recipe.resultFluid();
        if (resultFluid != null && resultFluid != Fluids.EMPTY) {
            FluidStack resultStack = new FluidStack(resultFluid, FluidType.BUCKET_VOLUME);
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_TANK_X + 1, RESULT_TANK_Y + 1)
                    .setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 64)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, resultStack);
        }

        if (recipe.resultItem() != null && recipe.resultItem() != Items.AIR) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_ITEM_X + 1, RESULT_ITEM_Y + 1)
                    .addItemStack(new ItemStack(recipe.resultItem(), Math.max(1, recipe.resultItemAmount())));
        }
    }
}
