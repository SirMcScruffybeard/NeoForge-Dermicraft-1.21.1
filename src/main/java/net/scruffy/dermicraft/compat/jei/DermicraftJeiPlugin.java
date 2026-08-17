package net.scruffy.dermicraft.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.compat.jei.category.DippingCategory;
import net.scruffy.dermicraft.compat.jei.category.DroolingCauldronCategory;
import net.scruffy.dermicraft.compat.jei.category.EarlyImplantCategory;
import net.scruffy.dermicraft.compat.jei.category.EarlyIncubatingCategory;
import net.scruffy.dermicraft.compat.jei.category.EffluencingCategory;
import net.scruffy.dermicraft.compat.jei.category.HandShreddingCategory;
import net.scruffy.dermicraft.compat.jei.category.HarvestingCategory;
import net.scruffy.dermicraft.compat.jei.category.MasticatingCategory;
import net.scruffy.dermicraft.compat.jei.category.MetastasizingCategory;
import net.scruffy.dermicraft.compat.jei.category.MutatingCategory;
import net.scruffy.dermicraft.compat.jei.category.PuddleCraftingCategory;
import net.scruffy.dermicraft.compat.jei.category.RenderingCategory;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JeiPlugin
public class DermicraftJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new DroolingCauldronCategory(gui),
                new MasticatingCategory(gui),
                new EffluencingCategory(gui),
                new MetastasizingCategory(gui),
                new MutatingCategory(gui),
                new DippingCategory(gui),
                new PuddleCraftingCategory(gui),
                new EarlyImplantCategory(gui),
                new EarlyIncubatingCategory(gui),
                new RenderingCategory(gui),
                new HarvestingCategory(gui),
                new HandShreddingCategory(gui)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();

        registration.addRecipes(DermicraftRecipeTypes.DROOLING_CAULDRON, buildDroolingDisplays(recipeManager));
        registration.addRecipes(DermicraftRecipeTypes.MASTICATING, buildMasticatingDisplays(recipeManager));
        registration.addRecipes(DermicraftRecipeTypes.EFFLUENCING,
                recipeManager.getAllRecipesFor(ModRecipes.EFFLUENCING_TYPE.get()));
        registration.addRecipes(DermicraftRecipeTypes.METASTASIZING,
                recipeManager.getAllRecipesFor(ModRecipes.METASTASIZING_TYPE.get()));
        registration.addRecipes(DermicraftRecipeTypes.MUTATING,
                recipeManager.getAllRecipesFor(ModRecipes.MUTATING_TYPE.get()));
        registration.addRecipes(DermicraftRecipeTypes.DIPPING,
                recipeManager.getAllRecipesFor(ModRecipes.DIPPING_TYPE.get()));
        registration.addRecipes(DermicraftRecipeTypes.PUDDLE_CRAFTING,
                recipeManager.getAllRecipesFor(ModRecipes.PUDDLE_FLUID_CRAFTING_TYPE.get()));
        registration.addRecipes(DermicraftRecipeTypes.EARLY_IMPLANT,
                recipeManager.getAllRecipesFor(ModRecipes.EARLY_IMPLANT_TYPE.get()));
        registration.addRecipes(DermicraftRecipeTypes.EARLY_INCUBATING,
                recipeManager.getAllRecipesFor(ModRecipes.EARLY_INCUBATING_TYPE.get()));
        registration.addRecipes(DermicraftRecipeTypes.RENDERING,
                recipeManager.getAllRecipesFor(ModRecipes.RENDERING_TYPE.get()));
        registration.addRecipes(DermicraftRecipeTypes.HARVESTING, buildHarvestDisplays());
        registration.addRecipes(DermicraftRecipeTypes.HAND_SHREDDING,
                recipeManager.getAllRecipesFor(ModRecipes.HAND_SHREDDING_TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DROOLING_CAULDRON.get()), DermicraftRecipeTypes.DROOLING_CAULDRON);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MASTICATOR.get()), DermicraftRecipeTypes.MASTICATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EFFLUENTCER.get()), DermicraftRecipeTypes.EFFLUENCING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.METASTASIZER.get()), DermicraftRecipeTypes.METASTASIZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MUTATOR.get()), DermicraftRecipeTypes.MUTATING);
        registration.addRecipeCatalyst(new ItemStack(ModItems.SUTURE_KIT.get()), DermicraftRecipeTypes.EARLY_IMPLANT);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRAW.get()), DermicraftRecipeTypes.EARLY_INCUBATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.RENDER_KILN.get()), DermicraftRecipeTypes.RENDERING);
        registration.addRecipeCatalyst(new ItemStack(ModItems.SCALPEL.get()), DermicraftRecipeTypes.HARVESTING);
    }

    // Drooling Cauldron recipes are always nutrition-scaled (IVagueRecipe) -- expand each
    // matching item into its own display entry so JEI shows the real yield/time per item
    // instead of one ambiguous number for an entire Ingredient tag.
    private static List<DynamicRecipeDisplay<VagueDroolingRecipe>> buildDroolingDisplays(RecipeManager recipeManager) {
        List<DynamicRecipeDisplay<VagueDroolingRecipe>> displays = new ArrayList<>();
        for (RecipeHolder<VagueDroolingRecipe> holder : recipeManager.getAllRecipesFor(ModRecipes.VAGUE_DROOLING_TYPE.get())) {
            VagueDroolingRecipe recipe = holder.value();
            for (ItemStack stack : recipe.ingredient().getItems()) {
                if (!recipe.testIngredient(stack)) continue;
                int amount = recipe.getCraftingAmount(stack);
                if (amount <= 0) continue;
                int ticks = recipe.getCraftingTime(stack);
                displays.add(new DynamicRecipeDisplay<>(holder, stack.copyWithCount(1),
                        recipe.getResultFluidStack(amount), ticks));
            }
        }
        return displays;
    }

    // Masticating recipes may be fixed-amount/fixed-time or dynamic (nutrition-scaled) -- expand
    // uniformly into one display entry per matching item either way, so the display logic
    // doesn't need to branch on which mode a given recipe uses.
    private static List<DynamicRecipeDisplay<MasticatingRecipe>> buildMasticatingDisplays(RecipeManager recipeManager) {
        List<DynamicRecipeDisplay<MasticatingRecipe>> displays = new ArrayList<>();
        for (RecipeHolder<MasticatingRecipe> holder : recipeManager.getAllRecipesFor(ModRecipes.MASTICATING_TYPE.get())) {
            MasticatingRecipe recipe = holder.value();
            for (ItemStack stack : recipe.ingredient().getItems()) {
                if (!recipe.testIngredient(stack)) continue;
                int amount = recipe.getCraftingAmount(stack);
                if (amount <= 0) continue;
                int ticks = recipe.getCraftingTime(stack);
                displays.add(new DynamicRecipeDisplay<>(holder, stack.copyWithCount(1),
                        recipe.getResultFluidStack(amount), ticks));
            }
        }
        return displays;
    }

    // Tumor harvesting is now loot-table driven (see ModBlockLootTableProvider), but loot tables
    // live in the server's reloadable registries and aren't synced to the client the way
    // RecipeManager recipes are -- unlike buildDroolingDisplays/buildMasticatingDisplays above,
    // there's no client-visible source to read this from. This list is hand-authored to mirror
    // the loot table pools; keep it in sync if those pools change.
    private static List<HarvestDisplay> buildHarvestDisplays() {
        List<HarvestDisplay> displays = new ArrayList<>();
        displays.add(new HarvestDisplay(new ItemStack(ModBlocks.EYE_TUMOR.get()),
                List.of(new ItemStack(ModItems.EYE.get()), new ItemStack(Items.ROTTEN_FLESH))));
        displays.add(new HarvestDisplay(new ItemStack(ModBlocks.MUSCLE_TUMOR.get()),
                List.of(new ItemStack(ModItems.DENSE_MUSCLE.get()), new ItemStack(Items.ROTTEN_FLESH))));
        displays.add(new HarvestDisplay(new ItemStack(ModBlocks.NERVE_TUMOR.get()),
                List.of(new ItemStack(ModItems.NERVE_CLUSTER.get()), new ItemStack(Items.ROTTEN_FLESH))));
        displays.add(new HarvestDisplay(new ItemStack(ModBlocks.INERT_TUMOR.get()),
                List.of(new ItemStack(ModItems.DENSE_MUSCLE.get()),
                        new ItemStack(ModItems.NERVE_CLUSTER.get()),
                        new ItemStack(ModItems.EYE.get()),
                        new ItemStack(Items.ROTTEN_FLESH))));
        return displays;
    }
}
