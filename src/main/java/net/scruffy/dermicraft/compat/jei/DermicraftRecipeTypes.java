package net.scruffy.dermicraft.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.dipping.DippingRecipe;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingCrucibleRecipe;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.recipe.early_incubating.EarlyIncubatingRecipe;
import net.scruffy.dermicraft.recipe.effluencing.EffluencingRecipe;
import net.scruffy.dermicraft.recipe.hand_shredding.HandShreddingRecipe;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.recipe.metastasizing.MetastasizingRecipe;
import net.scruffy.dermicraft.recipe.mutating.MutatingRecipe;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipe;
import net.scruffy.dermicraft.recipe.rendering.RenderingRecipe;

/**
 * JEI {@link RecipeType} identifiers for every machine category this plugin registers.
 */
public final class DermicraftRecipeTypes {

    private DermicraftRecipeTypes() {
    }

    public static final RecipeType<DynamicRecipeDisplay<VagueDroolingRecipe>> DROOLING_CAULDRON =
            RecipeType.create(Dermicraft.MOD_ID, "drooling_cauldron", uncheckedCast(DynamicRecipeDisplay.class));
    public static final RecipeType<DynamicRecipeDisplay<VagueDroolingCrucibleRecipe>> DROOLING_CRUCIBLE =
            RecipeType.create(Dermicraft.MOD_ID, "drooling_crucible", uncheckedCast(DynamicRecipeDisplay.class));
    public static final RecipeType<DynamicRecipeDisplay<MasticatingRecipe>> MASTICATING =
            RecipeType.create(Dermicraft.MOD_ID, "masticating", uncheckedCast(DynamicRecipeDisplay.class));
    public static final RecipeType<RecipeHolder<EffluencingRecipe>> EFFLUENCING =
            RecipeType.create(Dermicraft.MOD_ID, "effluencing", uncheckedCast(RecipeHolder.class));
    public static final RecipeType<RecipeHolder<MetastasizingRecipe>> METASTASIZING =
            RecipeType.create(Dermicraft.MOD_ID, "metastasizing", uncheckedCast(RecipeHolder.class));
    public static final RecipeType<RecipeHolder<MutatingRecipe>> MUTATING =
            RecipeType.create(Dermicraft.MOD_ID, "mutating", uncheckedCast(RecipeHolder.class));
    public static final RecipeType<RecipeHolder<DippingRecipe>> DIPPING =
            RecipeType.create(Dermicraft.MOD_ID, "dipping", uncheckedCast(RecipeHolder.class));
    public static final RecipeType<RecipeHolder<PuddleCraftingRecipe>> PUDDLE_CRAFTING =
            RecipeType.create(Dermicraft.MOD_ID, "puddle_crafting", uncheckedCast(RecipeHolder.class));
    public static final RecipeType<RecipeHolder<EarlyImplantRecipe>> EARLY_IMPLANT =
            RecipeType.create(Dermicraft.MOD_ID, "early_implant", uncheckedCast(RecipeHolder.class));
    public static final RecipeType<RecipeHolder<EarlyIncubatingRecipe>> EARLY_INCUBATING =
            RecipeType.create(Dermicraft.MOD_ID, "early_incubating", uncheckedCast(RecipeHolder.class));
    public static final RecipeType<RecipeHolder<RenderingRecipe>> RENDERING =
            RecipeType.create(Dermicraft.MOD_ID, "rendering", uncheckedCast(RecipeHolder.class));
    public static final RecipeType<HarvestDisplay> HARVESTING =
            RecipeType.create(Dermicraft.MOD_ID, "harvesting", HarvestDisplay.class);
    public static final RecipeType<RecipeHolder<HandShreddingRecipe>> HAND_SHREDDING =
            RecipeType.create(Dermicraft.MOD_ID, "hand_shredding", uncheckedCast(RecipeHolder.class));

    @SuppressWarnings("unchecked")
    private static <T> Class<T> uncheckedCast(Class<?> clazz) {
        return (Class<T>) clazz;
    }
}
