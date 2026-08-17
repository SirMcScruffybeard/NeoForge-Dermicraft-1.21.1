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
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.recipe.early_incubating.EarlyIncubatingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * Craw has no GUI -- a bulk item stack is loaded into its single storage slot, then a fluid
 * injection (Syringe) triggers the craft, matching {@link EarlyIncubatingRecipe}. Simpler than
 * {@code EarlyImplantCategory}: one ingredient slot (with its required count) instead of a grid,
 * and the Craw block itself shown as a static, non-consumed catalyst rather than a tool ingredient
 * -- the block is never consumed (see {@code EarlyIncubatingRecipe}'s class doc).
 */
public class EarlyIncubatingCategory implements IRecipeCategory<RecipeHolder<EarlyIncubatingRecipe>> {

    private static final int BLOCK_X = 0, BLOCK_Y = 21;
    private static final int ITEM_X = 29, ITEM_Y = 21;
    private static final int TANK_X = 58, TANK_Y = 0;
    // Matches EarlyImplantCategory's tank_and_slot pairing -- static Syringe icon in the baked
    // item-slot beneath the tank (fluid is administered via Syringe, not stored in a real
    // container item).
    private static final int SYRINGE_X = TANK_X + 1, SYRINGE_Y = TANK_Y + 49;
    private static final int ARROW_X = 87, ARROW_Y = 27;
    private static final int RESULT_X = 115, RESULT_Y = 23;
    private static final int WIDTH = RESULT_X + JeiTextures.ITEM_SLOT_SIZE;
    private static final int HEIGHT = JeiTextures.TANK_HEIGHT + 12;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlot;
    private final IDrawable tankAndSlot;
    private final IDrawable arrowBackground;
    private final IDrawable arrowFull;

    public EarlyIncubatingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.CRAW.get()));
        this.itemSlot = JeiTextures.itemSlot(gui);
        this.tankAndSlot = JeiTextures.tankAndSlot(gui);
        this.arrowBackground = JeiTextures.arrowBackground(gui);
        this.arrowFull = JeiTextures.arrowFull(gui);
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<EarlyIncubatingRecipe>> getRecipeType() {
        return DermicraftRecipeTypes.EARLY_INCUBATING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.dermicraft.category.early_incubating");
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
    public void draw(RecipeHolder<EarlyIncubatingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        itemSlot.draw(guiGraphics, BLOCK_X, BLOCK_Y);
        itemSlot.draw(guiGraphics, ITEM_X, ITEM_Y);
        tankAndSlot.draw(guiGraphics, TANK_X, TANK_Y);
        itemSlot.draw(guiGraphics, RESULT_X, RESULT_Y);
        arrowBackground.draw(guiGraphics, ARROW_X, ARROW_Y);
        arrowFull.draw(guiGraphics, ARROW_X, ARROW_Y);

        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.dermicraft.category.early_incubating.description"),
                0, HEIGHT - 10, 0x808080, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EarlyIncubatingRecipe> holder,
                           @NotNull IFocusGroup focuses) {
        EarlyIncubatingRecipe recipe = holder.value();

        builder.addSlot(RecipeIngredientRole.CATALYST, BLOCK_X + 1, BLOCK_Y + 1)
                .addItemStack(new ItemStack(ModBlocks.CRAW.get()));

        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_X + 1, ITEM_Y + 1)
                .addItemStack(recipe.ingredient());

        FluidStack fluid = recipe.fluidIngredient();
        int fluidAmount = Math.max(1, fluid.getAmount());
        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X + 1, TANK_Y + 1)
                .setFluidRenderer(fluidAmount, false, 16, 40)
                .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid.getFluid(), fluidAmount));

        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, SYRINGE_X, SYRINGE_Y)
                .addItemStack(new ItemStack(ModItems.SYRINGE.get()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
                .addItemStack(recipe.getResultItem(null));
    }
}
