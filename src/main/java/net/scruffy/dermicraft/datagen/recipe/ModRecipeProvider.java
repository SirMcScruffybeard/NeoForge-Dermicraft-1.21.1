package net.scruffy.dermicraft.datagen.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FORCEPS)
                .pattern("I I")
                .pattern(" N ")
                .pattern("I I")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("forceps_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SCALPEL.get())
                .pattern("  I")
                .pattern(" I ")
                .pattern("I  ")
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("scalpel_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SUTURE_KIT)
                .pattern("SS ").pattern("SI ")
                .pattern("ISI")
                .define('S', Tags.Items.STRINGS)
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON)).
                save(recipeOutput, RecipeBuilders.getResourceLocation("suture_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SYRINGE)
                .pattern("  N")
                .pattern(" G ")
                .pattern("I  ")
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("syringe_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.GLASS_FLASK, 4)
                .pattern(" G ")
                .pattern(" G ")
                .pattern("G G")
                .define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .unlockedBy("has_glass", has(Tags.Items.GLASS_BLOCKS_CHEAP))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("flask_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModBlocks.BEAKER_ITEM)
                .pattern("G G")
                .pattern(" G ")
                .define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .unlockedBy("has_glass", has(Tags.Items.GLASS_BLOCKS_CHEAP))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("beaker_crafting_table"));


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.OUTERFACE)
                .pattern("III")
                .pattern("IEI")
                .pattern("INI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('E', ModItems.EYE)
                .define('N', ModItems.NERVE_CLUSTER)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("outerface_crafting_table"));

        RecipeBuilders.simpleEarlyImplant(recipeOutput, Tags.Items.FOODS_RAW_MEAT, "inert_tumor_implant", ModBlocks.INERT_TUMOR.asItem());

        RecipeBuilders.simpleEarlyImplant(recipeOutput, ModItems.DENSE_MUSCLE.get(), "muscle_tumor_from_implant", ModBlocks.MUSCLE_TUMOR.asItem());
        RecipeBuilders.simpleEarlyImplant(recipeOutput, ModItems.EYE.get(), "eye_tumor_implant", ModBlocks.EYE_TUMOR.asItem());
        RecipeBuilders.simpleEarlyImplant(recipeOutput, ModItems.NERVE_CLUSTER.get(), "nerve_tumor_implant", ModBlocks.NERVE_TUMOR.asItem());

        RecipeBuilders.buildEarlyImplant(recipeOutput,"drooling_cauldron_implant",
                List.of(Ingredient.of(Blocks.CAULDRON),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.DROOLING_CAULDRON.asItem());

        RecipeBuilders.buildEarlyImplant(recipeOutput, "masticator_implant",
                List.of(
                        Ingredient.of(Items.BONE),
                        Ingredient.of(Items.BONE),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.MASTICATOR.asItem());

        RecipeBuilders.buildEarlyImplant(recipeOutput, "skin_tank_implant",
                List.of(
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModBlocks.BEAKER_ITEM.get())),
                Ingredient.of(ModItems.SUTURE_KIT.get()), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.SKIN_TANK.asItem());

        RecipeBuilders.buildEarlyImplant(recipeOutput, "effluentcer_implant",
                List.of(
                        Ingredient.of(ModItems.GLASS_FLASK.get()),
                        Ingredient.of(ModItems.GLASS_FLASK.get()),
                        Ingredient.of(ModItems.GLASS_FLASK.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get())),
                Ingredient.of(ModItems.SUTURE_KIT.get()), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.EFFLUENTCER.asItem());

        RecipeBuilders.buildEarlyImplant(recipeOutput, "craw_implant",
                List.of(
                        Ingredient.of(Items.CHEST),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get())),
                Ingredient.of(ModItems.SUTURE_KIT.get()), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.CRAW.asItem());

        // F-Stuff/C-Stuff: 30-second (600-tick) craft time, dynamically scaled. `ticks` is
        // negative -- EffluencingRecipe.getCraftingTime() scales it per 100 mB of result
        // output, so the value here is "ticks per 100 mB" (600 / (resultAmount / 100)), not
        // a raw tick count.
        RecipeBuilders.buildEffluencing(recipeOutput, "f_stuff_effluencing",
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 250, ModFluids.SOURCE_PROTEIN_BLEND.get(), 250,
                ModFluids.SOURCE_F_STUFF.get(), 500, -120);

        RecipeBuilders.buildEffluencing(recipeOutput, "c_stuff_effluencing",
                ModFluids.SOURCE_CARBON_BLEND.get(), 250, ModFluids.SOURCE_CALCIUM_BLEND.get(), 250,
                ModFluids.SOURCE_C_STUFF.get(), 500, -120);

        // Primitive Catalyst: fixed 45-second craft time, not dynamic (positive ticks are
        // used directly by EffluencingRecipe.getCraftingTime()).
        RecipeBuilders.buildEffluencing(recipeOutput, "primitive_catalyst_effluencing",
                ModFluids.SOURCE_F_STUFF.get(), 500, ModFluids.SOURCE_C_STUFF.get(), 500,
                ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 750, ModMath.Time.getSecondsToTicks(45));

        RecipeBuilders.buildVagueDrooling(recipeOutput, "water_drooling", Ingredient.of(Tags.Items.FOODS), 1, Fluids.WATER);

        RecipeBuilders.buildMasticating(recipeOutput, "calcium_blend_bone_masticating", Ingredient.of(Items.BONE), 1,
                Fluids.WATER, 1000, ModFluids.SOURCE_CALCIUM_BLEND.get(), 1000, -1,
                ModMath.Time.getMinutesToTicks(1));

        RecipeBuilders.buildMasticating(recipeOutput, "calcium_blend_bone_meal_masticating", Ingredient.of(Items.BONE_MEAL), 1,
                Fluids.WATER, 334, ModFluids.SOURCE_CALCIUM_BLEND.get(), 330, -1,
                ModMath.Time.getMinutesToTicks(1));


        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_coal_block", Items.COAL_BLOCK, 1000,
                ModFluids.SOURCE_CARBON_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_coal", Items.COAL, 110,
                ModFluids.SOURCE_CARBON_BLEND.get(), 112, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_charcoal", Items.CHARCOAL, 110,
                ModFluids.SOURCE_CARBON_BLEND.get(), 110, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.vagueMasticateWithTagAndWater(recipeOutput, "crude_slurry_vague_masticating", ModTags.Items.PLANT_FOOD, 1,
                ModFluids.SOURCE_CRUDE_SLURRY.get());

        RecipeBuilders.vagueMasticateWithTagAndWater(recipeOutput, "protein_blend_vague_masticating", ModTags.Items.MEAT_FOOD, 1,
                ModFluids.SOURCE_PROTEIN_BLEND.get());

        // Placeholder yields - Sediment Blend balance values not yet finalized, see crafting notes.
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating", ModTags.Items.STONE_BLEND_ROSTER, 1000,
                ModFluids.SOURCE_STONE_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "silica_blend_masticating", ModTags.Items.SILICA_BLEND_ROSTER, 1000,
                ModFluids.SOURCE_SILICA_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "clay_blend_masticating", ModTags.Items.CLAY_BLEND_ROSTER, 1000,
                ModFluids.SOURCE_CLAY_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "silica_blend_recycling_masticating", ModTags.Items.SILICA_BLEND_RECYCLING, 1000,
                ModFluids.SOURCE_SILICA_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "clay_blend_recycling_masticating", ModTags.Items.CLAY_BLEND_RECYCLING, 1000,
                ModFluids.SOURCE_CLAY_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));

        // Cross-feed recipes - sibling-fluid feed instead of water, boosted yield (placeholder +25% over base 1000 mB).
        // Silica Blend is the hub: cross-feeds with both Stone Blend and Clay Blend.
        RecipeBuilders.buildMasticating(recipeOutput, "silica_blend_masticating_boosted_with_stone_blend",
                Ingredient.of(ModTags.Items.SILICA_BLEND_ROSTER), 1,
                ModFluids.SOURCE_STONE_BLEND.get(), 1000, ModFluids.SOURCE_SILICA_BLEND.get(), 1250, -1,
                ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.buildMasticating(recipeOutput, "stone_blend_masticating_boosted_with_silica_blend",
                Ingredient.of(ModTags.Items.STONE_BLEND_ROSTER), 1,
                ModFluids.SOURCE_SILICA_BLEND.get(), 1000, ModFluids.SOURCE_STONE_BLEND.get(), 1250, -1,
                ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.buildMasticating(recipeOutput, "silica_blend_masticating_boosted_with_clay_blend",
                Ingredient.of(ModTags.Items.SILICA_BLEND_ROSTER), 1,
                ModFluids.SOURCE_CLAY_BLEND.get(), 1000, ModFluids.SOURCE_SILICA_BLEND.get(), 1250, -1,
                ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.buildMasticating(recipeOutput, "clay_blend_masticating_boosted_with_silica_blend",
                Ingredient.of(ModTags.Items.CLAY_BLEND_ROSTER), 1,
                ModFluids.SOURCE_SILICA_BLEND.get(), 1000, ModFluids.SOURCE_CLAY_BLEND.get(), 1250, -1,
                ModMath.Time.getSecondsToTicks(60));

        // Metastasizer sediment duplication - one copy of the pattern block, fluid consumed, pattern retained.
        // Cost by tier: aggregate 750, cobble 900, solid 1000, small/light 250. Craft time by metaphorical
        // density of the result (lighter = faster; solid blocks = 10s), see crafting notes.
        int aggregateTicks = ModMath.Time.getSecondsToTicks(6);
        int cobbleTicks = ModMath.Time.getSecondsToTicks(8);
        int solidTicks = ModMath.Time.getSecondsToTicks(10);
        int lightTicks = ModMath.Time.getSecondsToTicks(2.5f);

        // Stone Blend roster
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_gravel", Items.GRAVEL, ModFluids.SOURCE_STONE_BLEND.get(), 750, aggregateTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cobblestone", Items.COBBLESTONE, ModFluids.SOURCE_STONE_BLEND.get(), 900, cobbleTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cobbled_deepslate", Items.COBBLED_DEEPSLATE, ModFluids.SOURCE_STONE_BLEND.get(), 900, cobbleTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_stone", Items.STONE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_andesite", Items.ANDESITE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_diorite", Items.DIORITE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_granite", Items.GRANITE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_deepslate", Items.DEEPSLATE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_calcite", Items.CALCITE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_tuff", Items.TUFF, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_dripstone_block", Items.DRIPSTONE_BLOCK, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_pointed_dripstone", Items.POINTED_DRIPSTONE, ModFluids.SOURCE_STONE_BLEND.get(), 250, lightTicks);

        // Silica Blend roster
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_sand", Items.SAND, ModFluids.SOURCE_SILICA_BLEND.get(), 750, aggregateTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_red_sand", Items.RED_SAND, ModFluids.SOURCE_SILICA_BLEND.get(), 750, aggregateTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_sandstone", Items.SANDSTONE, ModFluids.SOURCE_SILICA_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_red_sandstone", Items.RED_SANDSTONE, ModFluids.SOURCE_SILICA_BLEND.get(), 1000, solidTicks);

        // Clay Blend roster
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_clay", Items.CLAY, ModFluids.SOURCE_CLAY_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_clay_ball", Items.CLAY_BALL, ModFluids.SOURCE_CLAY_BLEND.get(), 250, lightTicks);

        // MRE - F-Stuff (900 mB) + an existing MRE (non-consumed pattern) -> another MRE.
        RecipeBuilders.duplicate(recipeOutput, "mre_metastasizing", ModItems.MRE.get(), ModFluids.SOURCE_F_STUFF.get(), 900, cobbleTicks);

        // Meat Flavored Meat - same trio as MRE, using Protein Blend as the ingredient instead of F-Stuff.
        RecipeBuilders.duplicate(recipeOutput, "meat_flavored_meat_metastasizing", ModItems.MEAT_FLAVORED_MEAT.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 900, cobbleTicks);

        // Tumor/part duplication - all Protein Blend. Inert Tumor at the solid 1000 mB tier, the three
        // tumor-drop parts (Dense Muscle, Nerve Cluster, Eye) at the light 250 mB tier.
        RecipeBuilders.duplicate(recipeOutput, "inert_tumor_metastasizing", ModBlocks.INERT_TUMOR.get(), ModFluids.SOURCE_PROTEIN_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "dense_muscle_metastasizing", ModItems.DENSE_MUSCLE.get(), ModFluids.SOURCE_PROTEIN_BLEND.get(), 250, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "nerve_cluster_metastasizing", ModItems.NERVE_CLUSTER.get(), ModFluids.SOURCE_PROTEIN_BLEND.get(), 250, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "eye_metastasizing", ModItems.EYE.get(), ModFluids.SOURCE_PROTEIN_BLEND.get(), 250, lightTicks);

        // Bone/Bone Meal - mirrors the Calcium Blend Masticator recipes above, using the same output fluid amounts.
        RecipeBuilders.duplicate(recipeOutput, "bone_metastasizing", Items.BONE, ModFluids.SOURCE_CALCIUM_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "bone_meal_metastasizing", Items.BONE_MEAL, ModFluids.SOURCE_CALCIUM_BLEND.get(), 330, lightTicks);

        // Metal Blends - Ingot/Nugget tiers use Water 1:1; Raw tier uses a flat 250 mB Primitive Catalyst dose for 2000 mB output.
        RecipeBuilders.masticateWithWater(recipeOutput, "ferrous_blend_masticating_ingot", Items.IRON_INGOT, 1000,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.masticateWithWater(recipeOutput, "ferrous_blend_masticating_nugget", Items.IRON_NUGGET, 110,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 110, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.buildMasticating(recipeOutput, "ferrous_blend_masticating_raw",
                Ingredient.of(Items.RAW_IRON), 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 250,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 2000, -1, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_ingot", Items.COPPER_INGOT, 1000,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.buildMasticating(recipeOutput, "cuprous_blend_masticating_raw",
                Ingredient.of(Items.RAW_COPPER), 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 250,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 2000, -1, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "aurous_blend_masticating_ingot", Items.GOLD_INGOT, 1000,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.masticateWithWater(recipeOutput, "aurous_blend_masticating_nugget", Items.GOLD_NUGGET, 110,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 110, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.buildMasticating(recipeOutput, "aurous_blend_masticating_raw",
                Ingredient.of(Items.RAW_GOLD), 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 250,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 2000, -1, ModMath.Time.getSecondsToTicks(60));

        // Metal Blends - Metastasizer reverse route (Blend -> Ingot/Nugget), mirroring the Masticator's
        // Ingot/Nugget fluid amounts above 1:1. No Cuprous Nugget, same reason as the Masticator side.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_ferrous_ingot", Items.IRON_INGOT, ModFluids.SOURCE_FERROUS_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_ferrous_nugget", Items.IRON_NUGGET, ModFluids.SOURCE_FERROUS_BLEND.get(), 110, lightTicks);

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cuprous_ingot", Items.COPPER_INGOT, ModFluids.SOURCE_CUPROUS_BLEND.get(), 1000, solidTicks);

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_aurous_ingot", Items.GOLD_INGOT, ModFluids.SOURCE_AUROUS_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_aurous_nugget", Items.GOLD_NUGGET, ModFluids.SOURCE_AUROUS_BLEND.get(), 110, lightTicks);


        RecipeBuilders.simpleDipping(recipeOutput, "torch_dipping", Tags.Items.RODS_WOODEN, 1,
                ModFluids.SOURCE_CARBON_BLEND.get(), 75, Items.TORCH, 4);

        RecipeBuilders.PuddleCraft.MakeFluids.makeFromTag(recipeOutput, "crude_slurry_puddle", ModTags.Items.PLANT_FOOD, 4, Fluids.WATER,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), ModMath.Time.getSecondsToTicks(10));

        RecipeBuilders.PuddleCraft.MakeFluids.make(recipeOutput, "primitive_catalyst_puddle_coal",
                List.of(Ingredient.of(Items.COAL), Ingredient.of(Items.BONE), Ingredient.of(Tags.Items.FOODS_RAW_MEAT)),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(),
                ModMath.Time.getSecondsToTicks(10));

        RecipeBuilders.PuddleCraft.MakeFluids.make(recipeOutput, "primitive_catalyst_puddle_charcoal",
                List.of(Ingredient.of(Items.CHARCOAL), Ingredient.of(Items.BONE), Ingredient.of(Tags.Items.FOODS_RAW_MEAT)),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(),
                ModMath.Time.getSecondsToTicks(10));

        RecipeBuilders.PuddleCraft.MakeItems.makeFromTag(recipeOutput, "inert_tumor_puddle", ModTags.Items.ANIMAL_MEATS, 4, ModFluids.SOURCE_CRUDE_SLURRY.get(),
                ModBlocks.INERT_TUMOR.asItem(), 1, ModMath.Time.getSecondsToTicks(10));

        RecipeBuilders.PuddleCraft.MakeItems.makeFromOneItem(recipeOutput, "calcium_glass_puddle", Items.BONE_MEAL, 1, ModFluids.SOURCE_CALCIUM_BLEND.get(),
                ModBlocks.CALCIUM_GLASS.asItem(), 1, ModMath.Time.getSecondsToTicks(20));

        // MRE - bootstrap route: 1 Filled F-Stuff Bucket smelts into 1 MRE.
        // Cook times mirror vanilla raw beef/chicken: 200 ticks (furnace), 100 ticks (smoker).
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModFluids.F_STUFF_BUCKET.get()), RecipeCategory.FOOD,
                        ModItems.MRE.get(), 0.35f, ModMath.Time.getSecondsToTicks(10))
                .unlockedBy("has_f_stuff_bucket", has(ModFluids.F_STUFF_BUCKET.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("mre_smelting"));

        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModFluids.F_STUFF_BUCKET.get()), RecipeCategory.FOOD,
                        ModItems.MRE.get(), 0.35f, ModMath.Time.getSecondsToTicks(5))
                .unlockedBy("has_f_stuff_bucket", has(ModFluids.F_STUFF_BUCKET.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("mre_smoking"));

        // Meat Flavored Meat - same trio as MRE, using Protein Blend Bucket as the ingredient instead of F-Stuff.
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModFluids.PROTEIN_BLEND_BUCKET.get()), RecipeCategory.FOOD,
                        ModItems.MEAT_FLAVORED_MEAT.get(), 0.35f, ModMath.Time.getSecondsToTicks(10))
                .unlockedBy("has_protein_blend_bucket", has(ModFluids.PROTEIN_BLEND_BUCKET.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("meat_flavored_meat_smelting"));

        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModFluids.PROTEIN_BLEND_BUCKET.get()), RecipeCategory.FOOD,
                        ModItems.MEAT_FLAVORED_MEAT.get(), 0.35f, ModMath.Time.getSecondsToTicks(5))
                .unlockedBy("has_protein_blend_bucket", has(ModFluids.PROTEIN_BLEND_BUCKET.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("meat_flavored_meat_smoking"));
    }

    ////////////////////Other Crafting Methods\\\\\\\\\\\\\\\\\\\\
    protected static void oreSmelting(RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult, pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput pRecipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for (ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike)).save(pRecipeOutput, Dermicraft.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}
