package net.scruffy.dermicraft.datagen.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
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

        // Primitive Forceps - no-iron alternate. Bone pincers meeting at a Stick handle, mirroring
        // the iron version's silhouette but compressed like the Primitive Scalpel.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.PRIMITIVE_FORCEPS.get())
                .pattern("B B")
                .pattern(" S ")
                .define('B', Items.BONE)
                .define('S', Items.STICK)
                .unlockedBy("has_bone", has(Items.BONE))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("primitive_forceps_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SCALPEL.get())
                .pattern("  I")
                .pattern(" I ")
                .pattern("I  ")
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("scalpel_crafting_table"));

        // Primitive Scalpel - no-iron alternate. Flint upper-left of the Stick, everything else empty.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.PRIMITIVE_SCALPEL.get())
                .pattern("F ")
                .pattern(" S")
                .define('F', Items.FLINT)
                .define('S', Items.STICK)
                .unlockedBy("has_flint", has(Items.FLINT))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("primitive_scalpel_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SUTURE_KIT)
                .pattern("SS ").pattern("SI ")
                .pattern("ISI")
                .define('S', Tags.Items.STRINGS)
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON)).
                save(recipeOutput, RecipeBuilders.getResourceLocation("suture_crafting_table"));

        // Primitive Suture Kit - no-iron alternate. Exact same shape as the iron recipe, Bone Meal
        // standing in for Iron Nugget in all 3 cells.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.PRIMITIVE_SUTURE_KIT.get())
                .pattern("SS ").pattern("SB ")
                .pattern("BSB")
                .define('S', Tags.Items.STRINGS)
                .define('B', Items.BONE_MEAL)
                .unlockedBy("has_bone_meal", has(Items.BONE_MEAL))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("primitive_suture_kit_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SYRINGE)
                .pattern("  N")
                .pattern(" G ")
                .pattern("I  ")
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("syringe_crafting_table"));

        // Primitive Syringe - no-iron alternate. Same shape as the iron recipe; Bone Meal stands in
        // for the small Nugget slot, whole Bone for the large Ingot slot, Glass unchanged.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.PRIMITIVE_SYRINGE.get())
                .pattern("  N")
                .pattern(" G ")
                .pattern("I  ")
                .define('N', Items.BONE_MEAL)
                .define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .define('I', Items.BONE)
                .unlockedBy("has_bone", has(Items.BONE))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("primitive_syringe_crafting_table"));

        // Scrench -- 5 nuggets wrapping from left-center, around the top, to right-center; 2 ingots
        // (center, bottom-center) forming the tool's own body/handle.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SCRENCH)
                .pattern("NNN")
                .pattern("NIN")
                .pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("scrench_crafting_table"));

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
                .pattern("G G")
                .define('G', Items.GLASS_PANE)
                .unlockedBy("has_glass_pane", has(Items.GLASS_PANE))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("beaker_crafting_table"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.BLADDER)
                .requires(ModItems.DENSE_MUSCLE, 5)
                .requires(ModItems.SUTURE_KIT)
                .unlockedBy("has_dense_muscle", has(ModItems.DENSE_MUSCLE))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("bladder_crafting_table"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.FUEL_BLADDER)
                .requires(ModItems.BLADDER)
                .requires(Tags.Items.INGOTS_COPPER)
                .unlockedBy("has_bladder", has(ModItems.BLADDER))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("fuel_bladder_crafting_table"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.FEEDER_BLADDER)
                .requires(ModItems.BLADDER)
                .requires(ModItems.DENSE_MUSCLE)
                .requires(ModItems.SUTURE_KIT)
                .unlockedBy("has_bladder", has(ModItems.BLADDER))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("feeder_bladder_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.IDEP)
                .pattern("NII")
                .pattern("IFI")
                .pattern("NI ")
                .define('N', ModItems.NERVE_CLUSTER)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', ModItems.GLASS_FLASK)
                .unlockedBy("has_nerve_cluster", has(ModItems.NERVE_CLUSTER))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("idep_crafting_table"));


        ////////////////////Sunder Chains\\\\\\\\\\\\\\\\\\\\
        // Shared pattern across every material -- an ingot (or ingot-equivalent -- Diamond has no
        // ingot tier) at N/S/E/W, an Iron Nugget at each corner regardless of material (the nugget
        // is the chain's universal hardware, only the "blade" material itself changes).
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.IRON_SUNDER_CHAIN)
                .pattern("NIN").pattern("I I").pattern("NIN")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("iron_sunder_chain_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COPPER_SUNDER_CHAIN)
                .pattern("NIN").pattern("I I").pattern("NIN")
                .define('I', Tags.Items.INGOTS_COPPER)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("copper_sunder_chain_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GOLD_SUNDER_CHAIN)
                .pattern("NIN").pattern("I I").pattern("NIN")
                .define('I', Tags.Items.INGOTS_GOLD)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_gold_ingot", has(Tags.Items.INGOTS_GOLD))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("gold_sunder_chain_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.DIAMOND_SUNDER_CHAIN)
                .pattern("NIN").pattern("I I").pattern("NIN")
                .define('I', Tags.Items.GEMS_DIAMOND)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_diamond", has(Tags.Items.GEMS_DIAMOND))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("diamond_sunder_chain_crafting_table"));

        // Bone - the renewable budget option (mob drops, no mining), same shared pattern.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.BONE_SUNDER_CHAIN)
                .pattern("NIN").pattern("I I").pattern("NIN")
                .define('I', Tags.Items.BONES)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_bone", has(Tags.Items.BONES))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("bone_sunder_chain_crafting_table"));


        ////////////////////Shatter Heads\\\\\\\\\\\\\\\\\\\\
        // Shared pattern across every material (2026-08-15) -- Bone (vanilla, always) at center as
        // the structural core, Dense Muscle (always) top/bottom-center as the binding flesh, and the
        // head's own material filling BOTH side columns entirely (6 units) -- deliberately heavier
        // than Sunder's own chain recipe (which only uses 4 units of its varying material) since a
        // Shatter head gates the whole weapon's mining tier and durability the way a chain doesn't.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.IRON_SHATTER_HEAD)
                .pattern("XMX").pattern("XBX").pattern("XMX")
                .define('X', Tags.Items.INGOTS_IRON)
                .define('M', ModItems.DENSE_MUSCLE)
                .define('B', Tags.Items.BONES)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("iron_shatter_head_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GOLD_SHATTER_HEAD)
                .pattern("XMX").pattern("XBX").pattern("XMX")
                .define('X', Tags.Items.INGOTS_GOLD)
                .define('M', ModItems.DENSE_MUSCLE)
                .define('B', Tags.Items.BONES)
                .unlockedBy("has_gold_ingot", has(Tags.Items.INGOTS_GOLD))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("gold_shatter_head_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.DIAMOND_SHATTER_HEAD)
                .pattern("XMX").pattern("XBX").pattern("XMX")
                .define('X', Tags.Items.GEMS_DIAMOND)
                .define('M', ModItems.DENSE_MUSCLE)
                .define('B', Tags.Items.BONES)
                .unlockedBy("has_diamond", has(Tags.Items.GEMS_DIAMOND))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("diamond_shatter_head_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COPPER_SHATTER_HEAD)
                .pattern("XMX").pattern("XBX").pattern("XMX")
                .define('X', Tags.Items.INGOTS_COPPER)
                .define('M', ModItems.DENSE_MUSCLE)
                .define('B', Tags.Items.BONES)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("copper_shatter_head_crafting_table"));

        // Bone head -- the material itself is also Bone, same "the material IS the structural core"
        // overlap the pattern allows for naturally (7 total Bone between the X columns and center,
        // no special-casing needed to keep the shared pattern's slots consistent).
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.BONE_SHATTER_HEAD)
                .pattern("XMX").pattern("XBX").pattern("XMX")
                .define('X', Tags.Items.BONES)
                .define('M', ModItems.DENSE_MUSCLE)
                .define('B', Tags.Items.BONES)
                .unlockedBy("has_bone", has(Tags.Items.BONES))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("bone_shatter_head_crafting_table"));


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.OUTERFACE)
                .pattern("III")
                .pattern("IEI")
                .pattern("INI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('E', ModItems.EYE)
                .define('N', ModItems.NERVE_CLUSTER)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("outerface_crafting_table"));

        RecipeBuilders.buildHandShredding(recipeOutput, "wool_to_string_scalpel",
                Ingredient.of(ModItems.SCALPEL.get()), Ingredient.of(ItemTags.WOOL),
                new ItemStack(Items.STRING, 4), 0, false);
        RecipeBuilders.buildHandShredding(recipeOutput, "wool_to_string_primitive_scalpel",
                Ingredient.of(ModItems.PRIMITIVE_SCALPEL.get()), Ingredient.of(ItemTags.WOOL),
                new ItemStack(Items.STRING, 4), 1, false);
        RecipeBuilders.buildHandShredding(recipeOutput, "wool_to_string_flint",
                Ingredient.of(Items.FLINT), Ingredient.of(ItemTags.WOOL),
                new ItemStack(Items.STRING, 4), 0, true);

        // Carved Pumpkin -- same 3-tool roster as Wool->String above, Shears matching its real
        // vanilla carving-tool role (1 durability, same as vanilla's own shears-on-pumpkin
        // interaction) alongside the surgical-toolkit alternates.
        RecipeBuilders.buildHandShredding(recipeOutput, "carved_pumpkin_shears",
                Ingredient.of(Items.SHEARS), Ingredient.of(Items.PUMPKIN),
                new ItemStack(Items.CARVED_PUMPKIN), 1, false);
        RecipeBuilders.buildHandShredding(recipeOutput, "carved_pumpkin_scalpel",
                Ingredient.of(ModItems.SCALPEL.get()), Ingredient.of(Items.PUMPKIN),
                new ItemStack(Items.CARVED_PUMPKIN), 0, false);
        RecipeBuilders.buildHandShredding(recipeOutput, "carved_pumpkin_primitive_scalpel",
                Ingredient.of(ModItems.PRIMITIVE_SCALPEL.get()), Ingredient.of(Items.PUMPKIN),
                new ItemStack(Items.CARVED_PUMPKIN), 1, false);

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
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
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
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.EFFLUENTCER.asItem());

        RecipeBuilders.buildEarlyImplant(recipeOutput, "craw_implant",
                List.of(
                        Ingredient.of(Items.CHEST),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.CRAW.asItem());

        ////////////////////Gear Stations\\\\\\\\\\\\\\\\\\\\
        // Workbench: reworked 2026-08-09 -- reduced to a single, simple implant. WorkbenchBlock
        // #setPlacedBy already spawns WORKBENCH_TOP for free (no second item/implant involved), so
        // there's only ever one thing for the player to actually craft; the earlier version stacking
        // both design-table halves' costs into one 15-ingredient implant was needlessly heavy and
        // apparently confused JEI's display. Same shape as the Craw's own implant (its closest
        // analog -- Chest is this recipe's defining item too).
        RecipeBuilders.buildEarlyImplant(recipeOutput, "workbench_implant",
                List.of(
                        Ingredient.of(Items.CHEST),
                        Ingredient.of(ModBlocks.BRAIN.asItem()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.WORKBENCH.asItem());

        ////////////////////Gadget Modules\\\\\\\\\\\\\\\\\\\\
        // Aggregate Module: a Hopper (the actual intake mechanism -- same "a Hopper's real function
        // already is 'sucks items in'" precedent Eater's own Fabrication recipe uses) topped with an
        // Iron Shovel (material identity -- mirrors EaterItem's own AGGREGATE_SPEED_TOOL_STAND_IN,
        // same reasoning as Beam Module's pickaxe below, not just flavor) on a Module Frame --
        // crafting-table, not implant/Mutator, per the Modules direction note.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.AGGREGATE_MODULE.get())
                .pattern("S")
                .pattern("H")
                .pattern("F")
                .define('S', Items.IRON_SHOVEL)
                .define('H', Items.HOPPER)
                .define('F', ModItems.MODULE_FRAME.get())
                .unlockedBy("has_module_frame", has(ModItems.MODULE_FRAME.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("aggregate_module_crafting_table"));

        // Beam Module: built FROM the Metaphysical Safety Module rather than a Module Frame directly
        // -- the metaphysical destabilization mechanic is the actual capability being crafted in, the
        // Safety Module just already carries that material identity (see the beam design discussion's
        // recipe-carries-the-crossover reasoning). Iron Pickaxe on top mirrors EaterItem's own
        // BEAM_SPEED_TOOL_STAND_IN, same "recipe material mirrors the simulated tool" pattern Aggregate's
        // shovel follows above. Metaphysical Safety Module itself has no recipe yet -- see that
        // item's own open recipe question -- so this stays uncraftable until that lands, by design.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BEAM_MODULE.get())
                .pattern(" P ")
                .pattern(" H ")
                .pattern(" M ")
                .define('P', Items.IRON_PICKAXE)
                .define('H', Items.HOPPER)
                .define('M', ModItems.METAPHYSICAL_SAFETY_MODULE.get())
                .unlockedBy("has_metaphysical_safety_module", has(ModItems.METAPHYSICAL_SAFETY_MODULE.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("beam_module_crafting_table"));

        // Fluid Bypass Module: deliberately cheap relative to Aggregate/Thermal Safety -- see the
        // Modules direction discussion. Bypass doesn't unlock anything otherwise unreachable (every
        // target it lets you reach through fluid is already obtainable by ordinary means), it's pure
        // convenience, not a capability skip the way Thermal Safety's hazard bypass is -- so it doesn't
        // deserve capability-tier cost. Wool (soft, permeable material -- a filter/membrane) + Water
        // Bucket for the water itself, not the bucket's iron -- vanilla's own crafting-remainder
        // returns the bucket empty, same as Cake/Mushroom Stew, so this genuinely only spends the
        // Wool and the Frame.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FLUID_BYPASS_MODULE.get())
                .pattern("W")
                .pattern("B")
                .pattern("F")
                .define('W', ItemTags.WOOL)
                .define('B', Items.WATER_BUCKET)
                .define('F', ModItems.MODULE_FRAME.get())
                .unlockedBy("has_module_frame", has(ModItems.MODULE_FRAME.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("fluid_bypass_module_crafting_table"));

        // Thermal Safety Module: the priciest of the three, deliberately -- this is the one that
        // actually skips a real progression gate (early hazard tolerance), not just convenience
        // (Fluid Bypass) or a harvesting-category unlock (Aggregate). Magma Block (heat-resistant
        // identity) over a Lava Bucket (the hazard itself, not the bucket's iron -- same "value the
        // content, not the container" framing as Fluid Bypass's water) over the Module Frame, cased
        // in 6 Iron Ingots as a protective housing around the heat core.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.HEAT_SAFETY_MODULE.get())
                .pattern("IMI")
                .pattern("ILI")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('M', Blocks.MAGMA_BLOCK)
                .define('L', Items.LAVA_BUCKET)
                .define('F', ModItems.MODULE_FRAME.get())
                .unlockedBy("has_module_frame", has(ModItems.MODULE_FRAME.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("heat_safety_module_crafting_table"));

        // Metaphysical Safety Module: deliberately mirrors Thermal Safety Module's own shape/weight --
        // an Ender Pearl (metaphysical identity, playing Magma Block's role) over a Molten Soul Silica
        // Bucket (the hazard's real content -- the fluid actually tagged METAPHYSICAL_MILD, same
        // "value the content, not the container" framing as every other bucket recipe here) over the
        // Module Frame, cased in 6 Iron Ingots. Tune later if it ends up feeling off relative to Thermal.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.METAPHYSICAL_SAFETY_MODULE.get())
                .pattern("IPI")
                .pattern("ISI")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('P', Items.ENDER_PEARL)
                .define('S', ModFluids.MOLTEN_SOUL_SILICA_BUCKET.get())
                .define('F', ModItems.MODULE_FRAME.get())
                .unlockedBy("has_module_frame", has(ModItems.MODULE_FRAME.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("metaphysical_safety_module_crafting_table"));

        // Work Speed Module -- same weight/shape as the Safety Modules above (6 Iron Ingot casing +
        // an identity item + the fluid's own bucket + Module Frame). Sugar stands in for the
        // "speed" identity, mirroring Ender Pearl/Magma Block's role -- real vanilla precedent
        // (Speed potions use Sugar), same "borrow vanilla's own logic" convention Metaphysical's
        // Ender Pearl choice used.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.WORK_SPEED_MODULE.get())
                .pattern("ISI")
                .pattern("IKI")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.SUGAR)
                .define('K', ModFluids.KINETIC_CATALYST_BUCKET.get())
                .define('F', ModItems.MODULE_FRAME.get())
                .unlockedBy("has_module_frame", has(ModItems.MODULE_FRAME.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("work_speed_module_crafting_table"));

        // Salvage Module -- cheap, consumed-on-trigger tier. Nerve Cluster casing (the nervous
        // system's "signal survives" theme, matching the module tethering the gadget back to you
        // through death) around an Ender Pearl core (a life-cheating teleport item, on-theme for
        // cheating death once).
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SALVAGE_MODULE.get())
                .pattern("NNN")
                .pattern("NEN")
                .pattern("NFN")
                .define('N', ModItems.NERVE_CLUSTER.get())
                .define('E', Items.ENDER_PEARL)
                .define('F', ModItems.MODULE_FRAME.get())
                .unlockedBy("has_module_frame", has(ModItems.MODULE_FRAME.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("salvage_module_crafting_table"));

        // Anchor Module -- direct upgrade from Salvage Module, not a separate crafting-table
        // recipe: a full bucket (1000 mB) of Evolution Catalyst in the Mutator mutates the
        // already-tethered Salvage Module into the permanent, never-consumed Anchor tier.
        RecipeBuilders.buildMutating(recipeOutput, "mutating_anchor_module",
                Ingredient.of(ModItems.SALVAGE_MODULE.get()),
                ModFluids.SOURCE_EVOLUTION_CATALYST.get(), 1000, new ItemStack(ModItems.ANCHOR_MODULE.get()),
                ModMath.Time.getSecondsToTicks(10));

        // Capacity Module -- same 6-Iron-casing weight as the Safety Modules, Bucket as the
        // identity item (matches the icon, "holds more" made literal).
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CAPACITY_MODULE.get())
                .pattern("III")
                .pattern("IBI")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('B', Items.BUCKET)
                .define('F', ModItems.MODULE_FRAME.get())
                .unlockedBy("has_module_frame", has(ModItems.MODULE_FRAME.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("capacity_module_crafting_table"));

        // Capacity Module -- alternate recipe, Beaker in place of Bucket as the identity item.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CAPACITY_MODULE.get())
                .pattern("III")
                .pattern("IBI")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('B', ModBlocks.BEAKER_ITEM.get())
                .define('F', ModItems.MODULE_FRAME.get())
                .unlockedBy("has_module_frame", has(ModItems.MODULE_FRAME.get()))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("capacity_module_beaker_crafting_table"));

        ////////////////////EarlyIncubating\\\\\\\\\\\\\\\\\\\\
        // Proto Brain: 10 Nerve Cluster bulk-loaded into a Craw, triggered by a 100 mB Synapse
        // Catalyst injection (100 mB is the syringe's fixed physical volume, not a cost lever).
        // Craw is never consumed -- see EarlyIncubatingRecipe/CrawBlockEntity.
        RecipeBuilders.buildEarlyIncubating(recipeOutput, "proto_brain_incubating", ModBlocks.CRAW.get(),
                ModItems.NERVE_CLUSTER.get(), 10, ModFluids.SOURCE_SYNAPSE_CATALYST.get(), 100,
                ModItems.PROTO_BRAIN.get(), 1);

        // Decapitation head alt routes -- Zombie/Piglin Head standing in for the full 10 Nerve
        // Cluster requirement above. Deliberately Primitive Catalyst here, not Synapse Catalyst:
        // reads as the crude/salvaged route (a scavenged head) against the engineered route's
        // calibrated Nerve Cluster + Synapse Catalyst pairing. Fluid amount is fixed at 100 mB
        // regardless (Syringe's physical CAPACITY, not a cost lever -- see SyringeItem).
        RecipeBuilders.buildEarlyIncubating(recipeOutput, "proto_brain_incubating_zombie_head", ModBlocks.CRAW.get(),
                Items.ZOMBIE_HEAD, 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModItems.PROTO_BRAIN.get(), 1);
        RecipeBuilders.buildEarlyIncubating(recipeOutput, "proto_brain_incubating_piglin_head", ModBlocks.CRAW.get(),
                Items.PIGLIN_HEAD, 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModItems.PROTO_BRAIN.get(), 1);

        // Creeper Head -> Gunpowder -- Masticator only produces fluids, so this goes through the
        // Craw instead. 5 Gunpowder is exactly one TNT block's worth (5 Gunpowder + 4 Sand,
        // vanilla recipe), a real payoff for a decapitation-chance drop that's genuinely hard to
        // get even with Sunder's own ability.
        RecipeBuilders.buildEarlyIncubating(recipeOutput, "gunpowder_incubating_creeper_head", ModBlocks.CRAW.get(),
                Items.CREEPER_HEAD, 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                Items.GUNPOWDER, 5);

        // Pricier than the other implants (8 items) to reflect its powerful duplication ability;
        // the Eye fits the "3D printer" framing as the scanner that reads the pattern.
        RecipeBuilders.buildEarlyImplant(recipeOutput, "metastasizer_implant",
                List.of(
                        Ingredient.of(ModBlocks.BEAKER_ITEM.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.EYE.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.METASTASIZER.asItem());

        // F-Stuff/C-Stuff: 25% faster than the original 30-second (600-tick) craft time ->
        // 22.5s (450 ticks). `ticks` is negative -- EffluencingRecipe.getCraftingTime() scales
        // it per 100 mB of result output, so the value here is "ticks per 100 mB"
        // (450 / (resultAmount / 100)), not a raw tick count.
        RecipeBuilders.buildEffluencing(recipeOutput, "f_stuff_effluencing",
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 250, ModFluids.SOURCE_PROTEIN_BLEND.get(), 250,
                ModFluids.SOURCE_F_STUFF.get(), 500, -90);

        RecipeBuilders.buildEffluencing(recipeOutput, "c_stuff_effluencing",
                ModFluids.SOURCE_CARBON_BLEND.get(), 250, ModFluids.SOURCE_CALCIUM_BLEND.get(), 250,
                ModFluids.SOURCE_C_STUFF.get(), 500, -90);

        // Primitive Catalyst: 25% faster than the original fixed 45-second craft time -> 33.75s
        // (positive ticks are used directly by EffluencingRecipe.getCraftingTime()).
        RecipeBuilders.buildEffluencing(recipeOutput, "primitive_catalyst_effluencing",
                ModFluids.SOURCE_F_STUFF.get(), 500, ModFluids.SOURCE_C_STUFF.get(), 500,
                ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 750, ModMath.Time.getSecondsToTicks(33.75f));

        // Concentrated Slurry: fixed 50-tick craft time (steady-trickle design -- small batch,
        // short cycle, see dermicraft-slurry-notes.md). 10:1 Crude:Catalyst, a minor catalytic
        // addition rather than an equal-parts ingredient.
        RecipeBuilders.buildEffluencing(recipeOutput, "concentrated_slurry_effluencing",
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 50, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 5,
                ModFluids.SOURCE_CONCENTRATED_SLURRY.get(), 50, 50);

        // Reinforcing Catalyst: same fixed 50-tick, 10:1-minor-addition shape as Concentrated
        // Slurry, same exact numbers -- Chassis's reagent (see dermicraft's gadget-chassis notes).
        RecipeBuilders.buildEffluencing(recipeOutput, "reinforcing_catalyst_effluencing",
                ModFluids.SOURCE_C_STUFF.get(), 50, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 5,
                ModFluids.SOURCE_REINFORCING_CATALYST.get(), 50, 50);

        // Synapse Catalyst: even-parts mix mirroring Primitive Catalyst's own fixed 45-second
        // recipe shape (not a minor addition like Reinforcing Catalyst above) -- the mod's
        // general "smart component" reagent (Proto Brain, Brain Block, the Module Frame).
        RecipeBuilders.buildEffluencing(recipeOutput, "synapse_catalyst_effluencing",
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 500, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 500,
                ModFluids.SOURCE_SYNAPSE_CATALYST.get(), 750, ModMath.Time.getSecondsToTicks(45));

        // Kinetic Catalyst: same even-parts 500+500->750/45s shape as Synapse Catalyst above --
        // Molten Redstone + Molten Quartz instead of Cuprous Blend + Primitive Catalyst. Work Speed
        // Module's reagent, Stage 2-reachable (both inputs are Stage 2 "Molten" family fluids).
        RecipeBuilders.buildEffluencing(recipeOutput, "kinetic_catalyst_effluencing",
                ModFluids.SOURCE_MOLTEN_REDSTONE.get(), 500, ModFluids.SOURCE_MOLTEN_QUARTZ.get(), 500,
                ModFluids.SOURCE_KINETIC_CATALYST.get(), 750, ModMath.Time.getSecondsToTicks(45));

        // Zombie/Piglin Head -> Synapse Catalyst -- rarity-gated 1:1 substitute for one full batch
        // of the recipe above (same 750 mB output, same 45s), not a cheaper bypass: the head pays
        // for the whole Cuprous Blend + Primitive Catalyst cost by being scarce (decapitation
        // chance only), not by being efficient. Zombie/Piglin deliberately identical -- both read
        // as "a head's worth of neural material," same framing as their shared Proto Brain alt
        // route above.
        RecipeBuilders.masticateWithWater(recipeOutput, "synapse_catalyst_masticating_zombie_head", Items.ZOMBIE_HEAD, 750,
                ModFluids.SOURCE_SYNAPSE_CATALYST.get(), 750, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.masticateWithWater(recipeOutput, "synapse_catalyst_masticating_piglin_head", Items.PIGLIN_HEAD, 750,
                ModFluids.SOURCE_SYNAPSE_CATALYST.get(), 750, ModMath.Time.getSecondsToTicks(45));

        // Molten Netherite: mirrors the real 4 Scrap + 4 Gold Ingot -> 1 Netherite Ingot vanilla
        // smithing ratio (each fluid is 1000 mB per item). 60s matches the mod's other 4:1-ratio
        // recipes (Quartz Block/Glowstone/Amethyst Block). Requires the Charred Effluentcer --
        // both inputs carry Thermal Hazard.
        RecipeBuilders.buildEffluencing(recipeOutput, "molten_netherite_effluencing",
                ModFluids.SOURCE_MOLTEN_RAW_NETHERITE_SCRAP.get(), 4000, ModFluids.SOURCE_AUROUS_BLEND.get(), 4000,
                ModFluids.SOURCE_MOLTEN_NETHERITE.get(), 1000, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.buildVagueDrooling(recipeOutput, "water_drooling", Ingredient.of(Tags.Items.FOODS), 2, Fluids.WATER);
        // Same ingredient/modifier as Cauldron's own -- "they produce what they produce regardless
        // of food... exposure to food drives their hunger more" (dermicraft-machine-notes.md).
        // Separate recipe type only, not a different ingredient list.
        RecipeBuilders.buildVagueDroolingCrucible(recipeOutput, "lava_drooling", Ingredient.of(Tags.Items.FOODS), 2, Fluids.LAVA);

        RecipeBuilders.buildMasticating(recipeOutput, "calcium_blend_bone_masticating", Ingredient.of(Items.BONE), 1,
                Fluids.WATER, 1000, ModFluids.SOURCE_CALCIUM_BLEND.get(), 1000, -1,
                ModMath.Time.getSecondsToTicks(45));

        RecipeBuilders.buildMasticating(recipeOutput, "calcium_blend_bone_meal_masticating", Ingredient.of(Items.BONE_MEAL), 1,
                Fluids.WATER, 334, ModFluids.SOURCE_CALCIUM_BLEND.get(), 334, -1,
                ModMath.Time.getSecondsToTicks(15));

        // Skeleton Skull -- 3x Bone's rate (1000 -> 3000 mB, time scaled proportionally), a real
        // payoff for a decapitation-chance drop rather than parity with grinding plain Bone.
        RecipeBuilders.buildMasticating(recipeOutput, "calcium_blend_skeleton_skull_masticating", Ingredient.of(Items.SKELETON_SKULL), 1,
                Fluids.WATER, 3000, ModFluids.SOURCE_CALCIUM_BLEND.get(), 3000, -1,
                ModMath.Time.getSecondsToTicks(135));


        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_coal_block", Items.COAL_BLOCK, 9000,
                ModFluids.SOURCE_CARBON_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_coal", Items.COAL, 1000,
                ModFluids.SOURCE_CARBON_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(15));

        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_charcoal", Items.CHARCOAL, 1000,
                ModFluids.SOURCE_CARBON_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(15));

        RecipeBuilders.vagueMasticateWithTagAndWater(recipeOutput, "crude_slurry_vague_masticating", ModTags.Items.PLANT_FOOD, 2.6f,
                ModFluids.SOURCE_CRUDE_SLURRY.get());

        // Wheat has no vanilla FoodProperties, so it can't use the nutrition-scaled vague formula
        // above (that's what already covers Bread, via the PLANT_FOOD tag) -- a fixed yield instead.
        // Deliberately just under/over a third of Bread's own vague-formula numbers (520 mB / 200
        // ticks) -- raw Wheat should be a slightly worse deal (both slower AND lower-yield) than
        // baking it into Bread first and masticating that.
        RecipeBuilders.masticateWithWater(recipeOutput, "crude_slurry_masticating_wheat", Items.WHEAT, 170,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 170, ModMath.Time.getSecondsToTicks(3.5f));

        // Melon/Pumpkin blocks have no vanilla FoodProperties either (only Melon Slice is real
        // food), so they're pinned to a fixed value like Wheat above -- set equal to 5 Melon Slices
        // processed individually through the vague formula (nutrition 2, saturation 0.3 ->
        // foodWeight 2.6 -> 169 mB / 65 ticks each; x5 = 845 mB / 325 ticks). Pumpkin matched to the
        // same value for parity -- it has no edible sub-product to derive an equivalent from.
        RecipeBuilders.masticateWithWater(recipeOutput, "crude_slurry_masticating_melon", Items.MELON, 845,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 845, 325);
        RecipeBuilders.masticateWithWater(recipeOutput, "crude_slurry_masticating_pumpkin", Items.PUMPKIN, 845,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 845, 325);

        RecipeBuilders.vagueMasticateWithTagAndWater(recipeOutput, "protein_blend_vague_masticating", ModTags.Items.MEAT_FOOD, 2.6f,
                ModFluids.SOURCE_PROTEIN_BLEND.get());

        // Protein Blend reverse route -- one Metastasizer duplication recipe per MEAT_FOOD item
        // (the vague Masticator recipe's own tag roster: 5 raw meats, 5 cooked meats, Rotten Flesh),
        // each costed by plugging that specific item's vanilla nutrition/saturation into the same
        // vague formula the forward recipe uses (IVagueRecipe: mB = round(25 * modifier * nutrition
        // * (saturation + 1)), same 2.6 modifier) rather than a flat/tiered guess. Same formula
        // already backs "metastasizing_bread" (Crude Slurry family) for a cross-check: nutrition 5,
        // saturation 0.6 -> round(65 * 5 * 1.6) = 520 mB, matching the existing hardcoded value.
        // Ticks use the same formula's craft-time half (baseTicks 25 instead of the 25 mB multiplier).
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_beef", Items.BEEF, ModFluids.SOURCE_PROTEIN_BLEND.get(), 254, 98);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_porkchop", Items.PORKCHOP, ModFluids.SOURCE_PROTEIN_BLEND.get(), 254, 98);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_chicken", Items.CHICKEN, ModFluids.SOURCE_PROTEIN_BLEND.get(), 169, 65);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_rabbit", Items.RABBIT, ModFluids.SOURCE_PROTEIN_BLEND.get(), 254, 117);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_mutton", Items.MUTTON, ModFluids.SOURCE_PROTEIN_BLEND.get(), 169, 78);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cooked_beef", Items.COOKED_BEEF, ModFluids.SOURCE_PROTEIN_BLEND.get(), 936, 432);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cooked_porkchop", Items.COOKED_PORKCHOP, ModFluids.SOURCE_PROTEIN_BLEND.get(), 936, 432);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cooked_chicken", Items.COOKED_CHICKEN, ModFluids.SOURCE_PROTEIN_BLEND.get(), 624, 288);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cooked_rabbit", Items.COOKED_RABBIT, ModFluids.SOURCE_PROTEIN_BLEND.get(), 520, 240);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cooked_mutton", Items.COOKED_MUTTON, ModFluids.SOURCE_PROTEIN_BLEND.get(), 702, 324);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_rotten_flesh", Items.ROTTEN_FLESH, ModFluids.SOURCE_PROTEIN_BLEND.get(), 286, 132);

        // Fluid Bladder -- same Protein Blend duplication family as the meats above, ticks kept on
        // the same ~0.4615 ticks-per-mB ratio (e.g. cooked beef: 936 mB / 432 ticks) for consistency.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_bladder", ModItems.BLADDER.get(), ModFluids.SOURCE_PROTEIN_BLEND.get(), 1200, 554);

        // Placeholder yields - Sediment Blend balance values not yet finalized, see crafting notes.
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating", ModTags.Items.STONE_BLEND_ROSTER, 1000,
                ModFluids.SOURCE_STONE_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(30));

        // Slabs of roster blocks -- half value/time each, matching the real 2-slabs-per-block
        // crafting ratio (same precedent as Cut Copper -> Cut Copper Slab: 9000 -> 4500 mB).
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_stone_slab", Items.STONE_SLAB, 500,
                ModFluids.SOURCE_STONE_BLEND.get(), 500, ModMath.Time.getSecondsToTicks(15));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_cobblestone_slab", Items.COBBLESTONE_SLAB, 500,
                ModFluids.SOURCE_STONE_BLEND.get(), 500, ModMath.Time.getSecondsToTicks(15));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_andesite_slab", Items.ANDESITE_SLAB, 500,
                ModFluids.SOURCE_STONE_BLEND.get(), 500, ModMath.Time.getSecondsToTicks(15));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_diorite_slab", Items.DIORITE_SLAB, 500,
                ModFluids.SOURCE_STONE_BLEND.get(), 500, ModMath.Time.getSecondsToTicks(15));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_granite_slab", Items.GRANITE_SLAB, 500,
                ModFluids.SOURCE_STONE_BLEND.get(), 500, ModMath.Time.getSecondsToTicks(15));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_cobbled_deepslate_slab", Items.COBBLED_DEEPSLATE_SLAB, 500,
                ModFluids.SOURCE_STONE_BLEND.get(), 500, ModMath.Time.getSecondsToTicks(15));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_smooth_stone_slab", Items.SMOOTH_STONE_SLAB, 500,
                ModFluids.SOURCE_STONE_BLEND.get(), 500, ModMath.Time.getSecondsToTicks(15));

        // Stairs of roster blocks -- same 0.375x ratio Pulp Blend uses for wood stairs (375 mB from
        // a 1000 mB full-block base), applied to both amount and time here.
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_stone_stairs", Items.STONE_STAIRS, 375,
                ModFluids.SOURCE_STONE_BLEND.get(), 375, ModMath.Time.getSecondsToTicks(11.25f));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_cobblestone_stairs", Items.COBBLESTONE_STAIRS, 375,
                ModFluids.SOURCE_STONE_BLEND.get(), 375, ModMath.Time.getSecondsToTicks(11.25f));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_andesite_stairs", Items.ANDESITE_STAIRS, 375,
                ModFluids.SOURCE_STONE_BLEND.get(), 375, ModMath.Time.getSecondsToTicks(11.25f));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_diorite_stairs", Items.DIORITE_STAIRS, 375,
                ModFluids.SOURCE_STONE_BLEND.get(), 375, ModMath.Time.getSecondsToTicks(11.25f));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_granite_stairs", Items.GRANITE_STAIRS, 375,
                ModFluids.SOURCE_STONE_BLEND.get(), 375, ModMath.Time.getSecondsToTicks(11.25f));
        RecipeBuilders.masticateWithWater(recipeOutput, "stone_blend_masticating_cobbled_deepslate_stairs", Items.COBBLED_DEEPSLATE_STAIRS, 375,
                ModFluids.SOURCE_STONE_BLEND.get(), 375, ModMath.Time.getSecondsToTicks(11.25f));

        RecipeBuilders.masticateWithWater(recipeOutput, "silica_blend_masticating", ModTags.Items.SILICA_BLEND_ROSTER, 1000,
                ModFluids.SOURCE_SILICA_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.masticateWithWater(recipeOutput, "clay_blend_masticating", ModTags.Items.CLAY_BLEND_ROSTER, 1000,
                ModFluids.SOURCE_CLAY_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.masticateWithWater(recipeOutput, "silica_blend_recycling_masticating", ModTags.Items.SILICA_BLEND_RECYCLING, 1000,
                ModFluids.SOURCE_SILICA_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.masticateWithWater(recipeOutput, "clay_blend_recycling_masticating", ModTags.Items.CLAY_BLEND_RECYCLING, 1000,
                ModFluids.SOURCE_CLAY_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(30));

        // Cross-feed recipes - sibling-fluid feed instead of water, boosted yield (placeholder +25% over base 1000 mB).
        // Silica Blend is the hub: cross-feeds with both Stone Blend and Clay Blend.
        RecipeBuilders.buildMasticating(recipeOutput, "silica_blend_masticating_boosted_with_stone_blend",
                Ingredient.of(ModTags.Items.SILICA_BLEND_ROSTER), 1,
                ModFluids.SOURCE_STONE_BLEND.get(), 1000, ModFluids.SOURCE_SILICA_BLEND.get(), 1250, -1,
                ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "stone_blend_masticating_boosted_with_silica_blend",
                Ingredient.of(ModTags.Items.STONE_BLEND_ROSTER), 1,
                ModFluids.SOURCE_SILICA_BLEND.get(), 1000, ModFluids.SOURCE_STONE_BLEND.get(), 1250, -1,
                ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "silica_blend_masticating_boosted_with_clay_blend",
                Ingredient.of(ModTags.Items.SILICA_BLEND_ROSTER), 1,
                ModFluids.SOURCE_CLAY_BLEND.get(), 1000, ModFluids.SOURCE_SILICA_BLEND.get(), 1250, -1,
                ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "clay_blend_masticating_boosted_with_silica_blend",
                Ingredient.of(ModTags.Items.CLAY_BLEND_ROSTER), 1,
                ModFluids.SOURCE_SILICA_BLEND.get(), 1000, ModFluids.SOURCE_CLAY_BLEND.get(), 1250, -1,
                ModMath.Time.getSecondsToTicks(30));

        // Metastasizer sediment duplication - one copy of the pattern block, fluid consumed, pattern retained.
        // Cost by tier: aggregate 750, cobble 900, solid 1000, small/light 250. Craft time by metaphorical
        // density of the result (lighter = faster; solid blocks = 10s), see crafting notes.
        int aggregateTicks = ModMath.Time.getSecondsToTicks(6);
        int cobbleTicks = ModMath.Time.getSecondsToTicks(8);
        int solidTicks = ModMath.Time.getSecondsToTicks(10);
        int lightTicks = ModMath.Time.getSecondsToTicks(2.5f);

        // Brick, Bricks, and Flower Pot -- pulled out of the flat-rate CLAY_BLEND_RECYCLING tag
        // (see ModItemTagProvider) and given individually-tiered Masticator recipes that mirror
        // their own Metastasizer duplication costs below, same "forward mirrors reverse" convention
        // as the Ingot/Nugget/Bone families (unlike Clay Ball/Terracotta, which stay on the flat rate).
        RecipeBuilders.masticateWithWater(recipeOutput, "clay_blend_masticating_brick", Items.BRICK, 250,
                ModFluids.SOURCE_CLAY_BLEND.get(), 250, lightTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "clay_blend_masticating_bricks", Items.BRICKS, 1000,
                ModFluids.SOURCE_CLAY_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "clay_blend_masticating_flower_pot", Items.FLOWER_POT, 750,
                ModFluids.SOURCE_CLAY_BLEND.get(), 750, aggregateTicks);

        // Carbon Blend reverse route -- mirrors each forward recipe's own output amount, same
        // convention as the Metal Blends' Ingot/Nugget reverse recipes above.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_coal", Items.COAL, ModFluids.SOURCE_CARBON_BLEND.get(), 1000, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_charcoal", Items.CHARCOAL, ModFluids.SOURCE_CARBON_BLEND.get(), 1000, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_coal_block", Items.COAL_BLOCK, ModFluids.SOURCE_CARBON_BLEND.get(), 9000, solidTicks);

        // Stone Blend roster
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_gravel", Items.GRAVEL, ModFluids.SOURCE_STONE_BLEND.get(), 750, aggregateTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cobblestone", Items.COBBLESTONE, ModFluids.SOURCE_STONE_BLEND.get(), 900, cobbleTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cobbled_deepslate", Items.COBBLED_DEEPSLATE, ModFluids.SOURCE_STONE_BLEND.get(), 900, cobbleTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_stone", Items.STONE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_smooth_stone", Items.SMOOTH_STONE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_andesite", Items.ANDESITE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_diorite", Items.DIORITE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_granite", Items.GRANITE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_deepslate", Items.DEEPSLATE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_calcite", Items.CALCITE, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_tuff", Items.TUFF, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_dripstone_block", Items.DRIPSTONE_BLOCK, ModFluids.SOURCE_STONE_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_pointed_dripstone", Items.POINTED_DRIPSTONE, ModFluids.SOURCE_STONE_BLEND.get(), 250, lightTicks);

        // Slabs of roster blocks -- reverse of the masticating slab recipes above, same half
        // value/time convention (500 mB / half of solidTicks).
        int slabTicks = ModMath.Time.getSecondsToTicks(5);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_stone_slab", Items.STONE_SLAB, ModFluids.SOURCE_STONE_BLEND.get(), 500, slabTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cobblestone_slab", Items.COBBLESTONE_SLAB, ModFluids.SOURCE_STONE_BLEND.get(), 500, slabTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_andesite_slab", Items.ANDESITE_SLAB, ModFluids.SOURCE_STONE_BLEND.get(), 500, slabTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_diorite_slab", Items.DIORITE_SLAB, ModFluids.SOURCE_STONE_BLEND.get(), 500, slabTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_granite_slab", Items.GRANITE_SLAB, ModFluids.SOURCE_STONE_BLEND.get(), 500, slabTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cobbled_deepslate_slab", Items.COBBLED_DEEPSLATE_SLAB, ModFluids.SOURCE_STONE_BLEND.get(), 500, slabTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_smooth_stone_slab", Items.SMOOTH_STONE_SLAB, ModFluids.SOURCE_STONE_BLEND.get(), 500, slabTicks);

        // Silica Blend roster
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_sand", Items.SAND, ModFluids.SOURCE_SILICA_BLEND.get(), 750, aggregateTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_red_sand", Items.RED_SAND, ModFluids.SOURCE_SILICA_BLEND.get(), 750, aggregateTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_sandstone", Items.SANDSTONE, ModFluids.SOURCE_SILICA_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_red_sandstone", Items.RED_SANDSTONE, ModFluids.SOURCE_SILICA_BLEND.get(), 1000, solidTicks);

        // Clay Blend roster
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_clay", Items.CLAY, ModFluids.SOURCE_CLAY_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_clay_ball", Items.CLAY_BALL, ModFluids.SOURCE_CLAY_BLEND.get(), 250, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_brick", Items.BRICK, ModFluids.SOURCE_CLAY_BLEND.get(), 250, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_bricks", Items.BRICKS, ModFluids.SOURCE_CLAY_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_flower_pot", Items.FLOWER_POT, ModFluids.SOURCE_CLAY_BLEND.get(), 750, aggregateTicks);

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
        RecipeBuilders.duplicate(recipeOutput, "bone_meal_metastasizing", Items.BONE_MEAL, ModFluids.SOURCE_CALCIUM_BLEND.get(), 334, lightTicks);

        // Metal Blends - Ingot/Nugget tiers use Water 1:1; Raw tier uses a flat 250 mB Primitive Catalyst dose for 2000 mB output.
        RecipeBuilders.masticateWithWater(recipeOutput, "ferrous_blend_masticating_ingot", Items.IRON_INGOT, 1000,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.masticateWithWater(recipeOutput, "ferrous_blend_masticating_nugget", Items.IRON_NUGGET, 110,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 110, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.buildMasticating(recipeOutput, "ferrous_blend_masticating_raw",
                Ingredient.of(Items.RAW_IRON), 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 250,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 2000, -1, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.masticateWithWater(recipeOutput, "ferrous_blend_masticating_bucket", Items.BUCKET, 3000,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 3000, ModMath.Time.getSecondsToTicks(45));

        // Iron Block -- mirrors the Copper Block family's own vanilla-ratio-anchored 9000 mB (9
        // Ingots) at the Sediment Blend's 5 mB/tick rate, same convention as Copper Block/Cut Copper.
        RecipeBuilders.masticateWithWater(recipeOutput, "ferrous_blend_masticating_iron_block", Items.IRON_BLOCK, 9000,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(90));

        // Blood Nugget -- second half of the Protein Blend alternate route (see the Metastasizer's
        // metastasizing_blood_nugget). 25 mB/nugget -- well under a plain Iron Nugget's own 110 mB,
        // per the design notes' explicit "should read as trace extraction, not a real iron source"
        // target. Reaching one Ingot-equivalent (1000 mB) costs 40 Blood Nuggets = 10,000 mB Protein
        // Blend + 1000 mB Primitive Catalyst -- a genuine slow trickle, not a mining replacement.
        RecipeBuilders.buildMasticating(recipeOutput, "ferrous_blend_masticating_blood_nugget",
                Ingredient.of(ModItems.BLOOD_NUGGET.get()), 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 25,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 25, -1, ModMath.Time.getSecondsToTicks(30));

        // Heavy Weighted Pressure Plate -- 2 Iron Ingots' worth (real vanilla recipe cost), so 2000 mB.
        // Craft time stays at Ingot's own 60s rather than doubling -- same precedent as Raw (also
        // 2000 mB, also 60s): one item processed per cycle, regardless of its ingot-equivalent value.
        RecipeBuilders.masticateWithWater(recipeOutput, "ferrous_blend_masticating_heavy_weighted_pressure_plate",
                Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 2000,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 2000, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_ingot", Items.COPPER_INGOT, 1000,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.buildMasticating(recipeOutput, "cuprous_blend_masticating_raw",
                Ingredient.of(Items.RAW_COPPER), 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 250,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 2000, -1, ModMath.Time.getSecondsToTicks(45));

        // Copper building block family -- unaffected (fresh) state only, deliberately not covering
        // Exposed/Weathered/Oxidized or Waxed variants (oxidation is a passive weathering effect, not
        // a recipe -- a Metastasizer route would only ever duplicate a state the player already has a
        // real sample of, never skip the wait, but the 4-state x 2-wax multiplier was cut for scope).
        // Copper Bulb also excluded -- its real recipe (3 Ingot + Blaze Rod + Redstone) doesn't fit
        // the Masticator's one-item-one-fluid shape. Priced by real vanilla crafting ratio relative
        // to Copper Block (9 Ingots = 9000 mB, the anchor), same "vanilla ratio" methodology as the
        // Pulp Blend wood family -- Cut Copper is a flat 1:1 conversion (no material lost cutting a
        // block into panels, unlike Log -> Planks), not a fractional discount. Craft time at the
        // Sediment Blend's 5 mB/tick rate (matches the existing Cauldron precedent), not the Metal
        // Blend family's own slower Ingot rate.
        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_copper_block", Items.COPPER_BLOCK, 9000,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(90));
        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_cut_copper", Items.CUT_COPPER, 9000,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(90));
        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_cut_copper_stairs", Items.CUT_COPPER_STAIRS, 13500,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 13500, ModMath.Time.getSecondsToTicks(135));
        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_cut_copper_slab", Items.CUT_COPPER_SLAB, 4500,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 4500, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_chiseled_copper", Items.CHISELED_COPPER, 18000,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 18000, ModMath.Time.getSecondsToTicks(180));
        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_copper_grate", Items.COPPER_GRATE, 1000,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_copper_door", Items.COPPER_DOOR, 2000,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 2000, ModMath.Time.getSecondsToTicks(20));
        RecipeBuilders.masticateWithWater(recipeOutput, "cuprous_blend_masticating_copper_trapdoor", Items.COPPER_TRAPDOOR, 2000,
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 2000, ModMath.Time.getSecondsToTicks(20));

        RecipeBuilders.masticateWithWater(recipeOutput, "aurous_blend_masticating_ingot", Items.GOLD_INGOT, 1000,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.masticateWithWater(recipeOutput, "aurous_blend_masticating_nugget", Items.GOLD_NUGGET, 110,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 110, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.buildMasticating(recipeOutput, "aurous_blend_masticating_raw",
                Ingredient.of(Items.RAW_GOLD), 1, ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 250,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 2000, -1, ModMath.Time.getSecondsToTicks(45));

        // Light Weighted Pressure Plate -- 2 Gold Ingots' worth, same treatment as its Iron counterpart above.
        RecipeBuilders.masticateWithWater(recipeOutput, "aurous_blend_masticating_light_weighted_pressure_plate",
                Items.LIGHT_WEIGHTED_PRESSURE_PLATE, 2000,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 2000, ModMath.Time.getSecondsToTicks(60));

        // Gold Block -- mirrors the Copper Block family's own vanilla-ratio-anchored 9000 mB (9
        // Ingots) at the Sediment Blend's 5 mB/tick rate, same convention as Copper Block/Cut Copper.
        RecipeBuilders.masticateWithWater(recipeOutput, "aurous_blend_masticating_gold_block", Items.GOLD_BLOCK, 9000,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(90));

        // Metal Blends - Metastasizer reverse route (Blend -> Ingot/Nugget), mirroring the Masticator's
        // Ingot/Nugget fluid amounts above 1:1. No Cuprous Nugget, same reason as the Masticator side.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_ferrous_ingot", Items.IRON_INGOT, ModFluids.SOURCE_FERROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_ferrous_nugget", Items.IRON_NUGGET, ModFluids.SOURCE_FERROUS_BLEND.get(), 110, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_bucket", Items.BUCKET, ModFluids.SOURCE_FERROUS_BLEND.get(), 3000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_iron_block", Items.IRON_BLOCK, ModFluids.SOURCE_FERROUS_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(90));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_hopper", Items.HOPPER, ModFluids.SOURCE_FERROUS_BLEND.get(), 5000, ModMath.Time.getSecondsToTicks(50));

        // Blood Nugget -- alternate trace-iron route from Protein Blend. Iron Nugget is a
        // non-consumed pattern (same as every other Metastasizer recipe here); Protein Blend is the
        // real cost. Deliberately weaker than the direct Ferrous Blend family above: this is a
        // renewable fallback for players without iron access yet, not a replacement for mining.
        RecipeBuilders.buildMetastasizing(recipeOutput, "metastasizing_blood_nugget",
                Ingredient.of(Items.IRON_NUGGET), ModFluids.SOURCE_PROTEIN_BLEND.get(), 250,
                new ItemStack(ModItems.BLOOD_NUGGET.get()), solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_heavy_weighted_pressure_plate", Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 2000, ModMath.Time.getSecondsToTicks(60));

        // Cauldron via Ferrous Blend -- the Cauldron is metal (unlike the glass items Silica/Calcium
        // Blend duplicate), so it follows the same 1000 mB/Ingot rate literally against the real
        // vanilla recipe cost (7 Iron Ingots). Steep, but deliberately consistent rather than a
        // discounted one-off. Unblocks the Drooling Cauldron's OT-native recipe.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cauldron", Items.CAULDRON, ModFluids.SOURCE_FERROUS_BLEND.get(), 7000, ModMath.Time.getSecondsToTicks(70));

        // Forward route back to Ferrous Blend -- matches the reverse metastasizing_cauldron above
        // 1:1 (7000 mB, 70s), same convention as every other Masticator/Metastasizer pair.
        RecipeBuilders.masticateWithWater(recipeOutput, "ferrous_blend_masticating_cauldron", Items.CAULDRON, 7000,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 7000, ModMath.Time.getSecondsToTicks(70));

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cuprous_ingot", Items.COPPER_INGOT, ModFluids.SOURCE_CUPROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(45));

        // Copper building block family -- mirrors the Masticator recipes above 1:1, same convention
        // as every other Metal/Sediment Blend reverse route. Unaffected state only (see forward
        // recipes' comment for the oxidation/waxed/Copper Bulb scoping reasoning).
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_copper_block", Items.COPPER_BLOCK, ModFluids.SOURCE_CUPROUS_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(90));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cut_copper", Items.CUT_COPPER, ModFluids.SOURCE_CUPROUS_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(90));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cut_copper_stairs", Items.CUT_COPPER_STAIRS, ModFluids.SOURCE_CUPROUS_BLEND.get(), 13500, ModMath.Time.getSecondsToTicks(135));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cut_copper_slab", Items.CUT_COPPER_SLAB, ModFluids.SOURCE_CUPROUS_BLEND.get(), 4500, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_chiseled_copper", Items.CHISELED_COPPER, ModFluids.SOURCE_CUPROUS_BLEND.get(), 18000, ModMath.Time.getSecondsToTicks(180));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_copper_grate", Items.COPPER_GRATE, ModFluids.SOURCE_CUPROUS_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_copper_door", Items.COPPER_DOOR, ModFluids.SOURCE_CUPROUS_BLEND.get(), 2000, ModMath.Time.getSecondsToTicks(20));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_copper_trapdoor", Items.COPPER_TRAPDOOR, ModFluids.SOURCE_CUPROUS_BLEND.get(), 2000, ModMath.Time.getSecondsToTicks(20));

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_aurous_ingot", Items.GOLD_INGOT, ModFluids.SOURCE_AUROUS_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_aurous_nugget", Items.GOLD_NUGGET, ModFluids.SOURCE_AUROUS_BLEND.get(), 110, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_light_weighted_pressure_plate", Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 2000, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_gold_block", Items.GOLD_BLOCK, ModFluids.SOURCE_AUROUS_BLEND.get(), 9000, ModMath.Time.getSecondsToTicks(90));


        RecipeBuilders.simpleDipping(recipeOutput, "torch_dipping", Tags.Items.RODS_WOODEN, 1,
                ModFluids.SOURCE_CARBON_BLEND.get(), 500, Items.TORCH, 4);

        // Redstone torch's own dipping recipe -- mirrors vanilla's 1 stick + 1 redstone dust -> 1
        // redstone torch ratio (unlike the plain torch's 4-output). 110 mB Molten Redstone matches
        // the same "1 dust" rate metastasizing_molten_redstone_dust already uses below.
        RecipeBuilders.simpleDipping(recipeOutput, "redstone_torch_dipping", Tags.Items.RODS_WOODEN, 1,
                ModFluids.SOURCE_MOLTEN_REDSTONE.get(), 500, Items.REDSTONE_TORCH, 1);

        // Mutator copies of both dipping recipes above -- same ingredient/fluid/amount, just routed
        // through the Mutator's MUTATE mode instead of a dipping tank. lightTicks matches the same
        // "cheap single-dust-scale conversion" cadence metastasizing_molten_redstone_dust uses.
        RecipeBuilders.buildMutating(recipeOutput, "mutating_torch", Ingredient.of(Tags.Items.RODS_WOODEN),
                ModFluids.SOURCE_CARBON_BLEND.get(), 500, new ItemStack(Items.TORCH, 4), lightTicks);
        RecipeBuilders.buildMutating(recipeOutput, "mutating_redstone_torch", Ingredient.of(Tags.Items.RODS_WOODEN),
                ModFluids.SOURCE_MOLTEN_REDSTONE.get(), 500, new ItemStack(Items.REDSTONE_TORCH, 1), lightTicks);

        // Magma Block: any c:stones-tagged block baked in Lava. solidTicks matches the mod's usual
        // "whole block, real transformation" cadence rather than the lighter dust-scale conversions
        // above.
        RecipeBuilders.buildMutating(recipeOutput, "mutating_magma_block", Ingredient.of(Tags.Items.STONES),
                Fluids.LAVA, 1000, new ItemStack(Items.MAGMA_BLOCK), solidTicks);

        // Charred Grafting Table: pure stat-tier bump (see ModBlocks#CHARRED_GRAFTING_TABLE), same
        // "whole block baked in Lava" shape as Magma Block above -- not an in-place Evolution
        // Module transform like every other Charred machine, since this one has no Module slot at all.
        RecipeBuilders.buildMutating(recipeOutput, "mutating_charred_grafting_table",
                Ingredient.of(ModBlocks.GRAFTING_TABLE.get()),
                Fluids.LAVA, 1000, new ItemStack(ModBlocks.CHARRED_GRAFTING_TABLE.get()), solidTicks);

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

        RecipeBuilders.PuddleCraft.MakeFluids.make(recipeOutput, "evolution_catalyst_puddle",
                List.of(Ingredient.of(Items.PHANTOM_MEMBRANE), Ingredient.of(Items.QUARTZ), Ingredient.of(Items.GLOWSTONE_DUST)),
                ModFluids.SOURCE_SYNAPSE_CATALYST.get(), ModFluids.SOURCE_EVOLUTION_CATALYST.get(),
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

        // Mutator - same 8-item implant shape as the Metastasizer's, Chest swapped in as the
        // defining/structural item (the item goes into the box and comes out changed).
        RecipeBuilders.buildEarlyImplant(recipeOutput, "mutator_implant",
                List.of(
                        Ingredient.of(Items.CHEST),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.EYE.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.MUTATOR.asItem());

        // Render Furnace and Grafting Table - deliberately the cheapest implants in the mod: just the
        // vanilla structural item, sutured, no flesh at all (see machine notes -- these are barely-
        // modified vanilla objects, so their "birth" doesn't need much biological investment).
        RecipeBuilders.buildEarlyImplant(recipeOutput, "render_furnace_implant",
                List.of(Ingredient.of(Items.FURNACE)),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.RENDER_FURNACE.asItem());

        RecipeBuilders.buildEarlyImplant(recipeOutput, "grafting_table_implant",
                List.of(Ingredient.of(Items.CRAFTING_TABLE)),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.GRAFTING_TABLE.asItem());

        // Mutator recipe roster - all values provisional (functional-not-final, see machine notes),
        // Tier 1 only. Stage 2 (Magma Block, Tinted Glass, Eye of Ender) is deliberately NOT built yet.
        //
        // Glass family collision fix: dye + Silica Blend can't produce BOTH stained glass and stained
        // glass pane from the Mutator -- both would share the same {dye, fluid} input pair, which
        // collides under OneFluidOneItemRecipeInput's fluid-amount "at least" matching (whichever
        // recipe the recipe manager iterates first would win, arbitrarily). Resolved by splitting the
        // family across machines instead of inventing new matching semantics: the Mutator makes ONLY
        // the stained glass BLOCK from dye (below); dyed panes come from the Metastasizer duplicating
        // an existing pane (see the Metastasizer additions further down) -- the player bridges block
        // to pane via vanilla's own 8-stained-glass -> 16-stained-glass-pane table recipe. The colored/
        // glazed terracotta pair has the same collision and is deferred until that family gets the
        // same treatment.
        int quickTicks = lightTicks; // 50 ticks -- reused for the sub-block-volume (slab) mossy recipes

        // Bladder family
        RecipeBuilders.mutate(recipeOutput, "mutating_fuel_bladder", ModItems.BLADDER.get(),
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 750, ModItems.FUEL_BLADDER.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_feeder_bladder", ModItems.BLADDER.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 750, ModItems.FEEDER_BLADDER.get(), solidTicks);

        // Charred (thermal-hazard-tolerant) Bladder -- quenched/hardened in a full bucket of Lava,
        // matching the Charred Duct/Node's own convention.
        RecipeBuilders.mutate(recipeOutput, "charred_bladder_mutating", ModItems.BLADDER.get(),
                Fluids.LAVA, 1000, ModItems.CHARRED_BLADDER.get(), solidTicks);

        // Charred Fuel/Feeder Bladder -- two paths in, same as the base Fuel/Feeder Bladder having
        // its own quench recipe plus a specialization recipe off the plain Bladder: quench the
        // already-specialized base variant in Lava, OR specialize an already-quenched Charred Bladder
        // with the same conversion fluid/amount the base specialization recipe uses. Either order
        // reaches the same result.
        RecipeBuilders.mutate(recipeOutput, "charred_fuel_bladder_from_fuel_bladder_mutating", ModItems.FUEL_BLADDER.get(),
                Fluids.LAVA, 1000, ModItems.CHARRED_FUEL_BLADDER.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "charred_fuel_bladder_from_charred_bladder_mutating", ModItems.CHARRED_BLADDER.get(),
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 750, ModItems.CHARRED_FUEL_BLADDER.get(), solidTicks);

        RecipeBuilders.mutate(recipeOutput, "charred_feeder_bladder_from_feeder_bladder_mutating", ModItems.FEEDER_BLADDER.get(),
                Fluids.LAVA, 1000, ModItems.CHARRED_FEEDER_BLADDER.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "charred_feeder_bladder_from_charred_bladder_mutating", ModItems.CHARRED_BLADDER.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 750, ModItems.CHARRED_FEEDER_BLADDER.get(), solidTicks);

        // Chassis: Iron Bars (mechanical scaffold) + Calcium-Blend-derived Reinforcing Catalyst
        // (skeletal reagent) -- shared structural item across Gadgets and the Module Frame below.
        RecipeBuilders.mutate(recipeOutput, "chassis_mutating", Items.IRON_BARS,
                ModFluids.SOURCE_REINFORCING_CATALYST.get(), 1000, ModItems.CHASSIS.get(), solidTicks);

        // Module Frame (renamed from "Add-on Frame" 2026-08-09): 1 Chassis + 3000 mB Synapse
        // Catalyst -- priced above a naive cheap-since-repeatable instinct, since a Frame is
        // permanent swappable infrastructure (dock any equipment-origin item, get that capability)
        // rather than a one-time build.
        RecipeBuilders.mutate(recipeOutput, "module_frame_mutating", ModItems.CHASSIS.get(),
                ModFluids.SOURCE_SYNAPSE_CATALYST.get(), 3000, ModItems.MODULE_FRAME.get(), solidTicks);

        // Evolution Modules: matching Safety Module + a bucket (1000 mB) of Evolution Catalyst --
        // same cost tier as Chassis, since it's an upgrade-part item rather than permanent
        // infrastructure like the Module Frame above.
        RecipeBuilders.mutate(recipeOutput, "heat_evolution_module_mutating", ModItems.HEAT_SAFETY_MODULE.get(),
                ModFluids.SOURCE_EVOLUTION_CATALYST.get(), 1000, ModItems.HEAT_EVOLUTION_MODULE.get(), solidTicks);

        // Shatter head upgrades (see project_shatter_head_upgrade_design) -- 3 tiers max, durability
        // only. Cost escalates by a full bucket per tier (1000/2000/3000 mB); durability multiplier
        // is 1.5x/2.0x/2.5x of the material's OWN base durability (not compounding). Bone uses
        // Reinforcing Catalyst (same reagent Chassis uses above); the metals each use their own
        // Blend fluid (Ferrous=Iron, Cuprous=Copper, Aurous=Gold) -- Diamond is deferred until it has
        // its own Blend fluid (only Molten Diamond exists today, a different fluid family).
        buildPartUpgradeTiers(recipeOutput, "bone_shatter_head", ModItems.BONE_SHATTER_HEAD.get(),
                ModFluids.SOURCE_REINFORCING_CATALYST.get(), 262, solidTicks);
        buildPartUpgradeTiers(recipeOutput, "copper_shatter_head", ModItems.COPPER_SHATTER_HEAD.get(),
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 230, solidTicks);
        buildPartUpgradeTiers(recipeOutput, "gold_shatter_head", ModItems.GOLD_SHATTER_HEAD.get(),
                ModFluids.SOURCE_AUROUS_BLEND.get(), 64, solidTicks);
        buildPartUpgradeTiers(recipeOutput, "iron_shatter_head", ModItems.IRON_SHATTER_HEAD.get(),
                ModFluids.SOURCE_FERROUS_BLEND.get(), 500, solidTicks);

        // Sunder chain upgrades -- same shared 3-tier ladder, mechanics, and fluid-per-material
        // mapping as the Shatter heads above (Bone = Reinforcing Catalyst, metals = their own Blend
        // fluid). Diamond is deferred for the same reason as Shatter's Diamond head: no Diamond Blend
        // fluid exists yet (only Molten Diamond, a different fluid family) -- revisit once it does.
        buildPartUpgradeTiers(recipeOutput, "bone_sunder_chain", ModItems.BONE_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_REINFORCING_CATALYST.get(), 75, solidTicks);
        buildPartUpgradeTiers(recipeOutput, "copper_sunder_chain", ModItems.COPPER_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 150, solidTicks);
        buildPartUpgradeTiers(recipeOutput, "gold_sunder_chain", ModItems.GOLD_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_AUROUS_BLEND.get(), 100, solidTicks);
        buildPartUpgradeTiers(recipeOutput, "iron_sunder_chain", ModItems.IRON_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_FERROUS_BLEND.get(), 250, solidTicks);

        // Glassmaking family (Silica Blend) - Mutator half: Dye + Silica Blend -> Stained Glass BLOCK
        // only (see the collision note above for why the pane isn't here). Priced at parity with the
        // plain Glass Block's own (undocumented-in-code, but design-confirmed) 1000 mB Silica Blend
        // cost - putting Silica Blend to work on its core silica-to-glass identity.
        for (DyeColor color : DyeColor.values()) {
            String c = color.getName();
            RecipeBuilders.mutate(recipeOutput, "mutating_stained_glass_" + c, dyeItem(c),
                    ModFluids.SOURCE_SILICA_BLEND.get(), 1000, stainedGlassItem(c), solidTicks);
        }

        // Ceramics family (Clay Blend) - Colored & Glazed Terracotta. Collision fix: Glazed's
        // ingredient is the COLORED TERRACOTTA item itself, not the dye -- Colored and Glazed no
        // longer share an {ingredient, fluid} pair (Colored keys off dye, Glazed keys off the
        // colored-terracotta item), so both can coexist without the ambiguous-match problem the
        // glass family hit. This also happens to be more true to the real process (clay -> terracotta
        // -> dye -> COLORED -> fire again -> GLAZED -- Glazed is made FROM Colored, not from raw dye).
        // Fluid cost follows from the ingredient change: Glazed's input already embeds Colored's own
        // 1000 mB Clay Blend cost, so the marginal dose for firing-only is priced LOWER than a
        // from-scratch estimate would be (500 mB, half of Colored's), while total investment across
        // the two steps together still ends up pricier than Colored alone. Time still runs longer
        // (300 ticks vs. Colored's 200) -- firing takes longer, independent of the ingredient swap.
        for (DyeColor color : DyeColor.values()) {
            String c = color.getName();
            RecipeBuilders.mutate(recipeOutput, "mutating_terracotta_" + c, dyeItem(c),
                    ModFluids.SOURCE_CLAY_BLEND.get(), 1000, coloredTerracottaItem(c), solidTicks);
            RecipeBuilders.mutate(recipeOutput, "mutating_glazed_terracotta_" + c, coloredTerracottaItem(c),
                    ModFluids.SOURCE_CLAY_BLEND.get(), 500, glazedTerracottaItem(c), ModMath.Time.getSecondsToTicks(15));
        }

        // Variant tumors - migrated from the Brain's OT-only candidate list (still OT-invokable).
        RecipeBuilders.mutate(recipeOutput, "mutating_eye_tumor", ModItems.EYE.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 1000, ModBlocks.EYE_TUMOR.asItem(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_nerve_tumor", ModItems.NERVE_CLUSTER.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 1000, ModBlocks.NERVE_TUMOR.asItem(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_muscle_tumor", ModItems.DENSE_MUSCLE.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 1000, ModBlocks.MUSCLE_TUMOR.asItem(), solidTicks);

        // Stitched Tumor - Mutator route: Marred Tumor + Protein Blend, priced at String's own
        // Metastasizer cost (100 mB, see metastasizing_string above) since it's the "stitching it back
        // together" step -- same fleshy-fix-up cost as a single strand of thread.
        RecipeBuilders.mutate(recipeOutput, "mutating_stitched_tumor", ModBlocks.MARRED_TUMOR.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 100, ModBlocks.STITCHED_TUMOR.asItem(), lightTicks);

        // Flesh Lab Floor - Mutator route: structural block + 2000 mB Protein Blend -> the matching
        // Lab Floor variant. Same structural-block-defines-the-variant pattern as the crafting-table
        // recipes, priced at 2x the tumors' flat 1000 mB since it's producing infrastructure, not a print.
        RecipeBuilders.mutate(recipeOutput, "mutating_stone_lab_floor", Items.STONE,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2000, ModBlocks.STONE_LAB_FLOOR.asItem(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_cobblestone_lab_floor", Items.COBBLESTONE,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2000, ModBlocks.COBBLESTONE_LAB_FLOOR.asItem(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_deepslate_lab_floor", Items.DEEPSLATE,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2000, ModBlocks.DEEPSLATE_LAB_FLOOR.asItem(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_cobbled_deepslate_lab_floor", Items.COBBLED_DEEPSLATE,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2000, ModBlocks.COBBLED_DEEPSLATE_LAB_FLOOR.asItem(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_diorite_lab_floor", Items.DIORITE,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2000, ModBlocks.DIORITE_LAB_FLOOR.asItem(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_andesite_lab_floor", Items.ANDESITE,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2000, ModBlocks.ANDESITE_LAB_FLOOR.asItem(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_granite_lab_floor", Items.GRANITE,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2000, ModBlocks.GRANITE_LAB_FLOOR.asItem(), solidTicks);

        // Craw - Mutator alternate route, parallel to craw_implant (not a replacement; the implant's
        // Chest + 2 Dense Muscle + 2 Nerve Cluster + Primitive Catalyst combine stays). Chest is
        // simple enough (single structural item, no second reagent needed) to be one of the few
        // FL-native machines that also fits the Mutator's single-reagent identity -- most of the six
        // don't (see "Explicit non-fits" in dermicraft-machine-notes.md). Priced above the Lab
        // Floors' flat 2000 mB since the Craw is a functional machine, not a floor tile.
        RecipeBuilders.mutate(recipeOutput, "mutating_craw", Items.CHEST,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2500, ModBlocks.CRAW.asItem(), solidTicks);

        // Hopper - Chest + Ferrous Blend, same "structural item + Mutator reagent" shape as Craw
        // above. 5000 mB matches vanilla's own 5 Iron Ingot cost exactly (1000 mB per ingot, the
        // mod's standard rate -- see metastasizing_ferrous_ingot), and mirrors metastasizing_hopper's
        // own 5000 mB duplication cost below.
        RecipeBuilders.mutate(recipeOutput, "mutating_hopper", Items.CHEST,
                ModFluids.SOURCE_FERROUS_BLEND.get(), 5000, Items.HOPPER, solidTicks);

        // Skin Tank - same reasoning and price as the Craw's Mutator route above: simple enough
        // (one structural item, Beaker, no second reagent needed) to fit the Mutator, parallel to
        // (not replacing) skin_tank_implant.
        RecipeBuilders.mutate(recipeOutput, "mutating_skin_tank", ModBlocks.BEAKER_ITEM.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2500, ModBlocks.SKIN_TANK.asItem(), solidTicks);

        // Brain Block - FL Tier 1 build (see dermicraft-machine-notes.md, Core -- Tier 1 build).
        // Same price/timing as the Craw/Skin Tank Mutator routes -- the Brain is the FL's control
        // block, at least as significant as either.
        RecipeBuilders.mutate(recipeOutput, "mutating_brain", ModItems.PROTO_BRAIN.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 2500, ModBlocks.BRAIN.asItem(), solidTicks);

        // Overgrowth family (Crude Slurry as the life/growth agent) - Mossy Cobblestone set. 10 mB
        // flat across the whole family (block/stairs/wall/slab alike) -- other mods gate this behind
        // plain water, so this is deliberately a token cost, not a scaled-by-volume one; the
        // block-vs-slab cost distinction is dropped along with it.
        RecipeBuilders.mutate(recipeOutput, "mutating_mossy_cobblestone", Items.COBBLESTONE,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 10, Items.MOSSY_COBBLESTONE, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_mossy_cobblestone_stairs", Items.COBBLESTONE_STAIRS,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 10, Items.MOSSY_COBBLESTONE_STAIRS, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_mossy_cobblestone_wall", Items.COBBLESTONE_WALL,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 10, Items.MOSSY_COBBLESTONE_WALL, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_mossy_cobblestone_slab", Items.COBBLESTONE_SLAB,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 10, Items.MOSSY_COBBLESTONE_SLAB, quickTicks);

        // Overgrowth family - Mossy Stone Bricks set. Same flat 10 mB.
        RecipeBuilders.mutate(recipeOutput, "mutating_mossy_stone_bricks", Items.STONE_BRICKS,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 10, Items.MOSSY_STONE_BRICKS, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_mossy_stone_brick_stairs", Items.STONE_BRICK_STAIRS,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 10, Items.MOSSY_STONE_BRICK_STAIRS, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_mossy_stone_brick_wall", Items.STONE_BRICK_WALL,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 10, Items.MOSSY_STONE_BRICK_WALL, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_mossy_stone_brick_slab", Items.STONE_BRICK_SLAB,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 10, Items.MOSSY_STONE_BRICK_SLAB, quickTicks);

        // Overgrowth family - Vines, and the botanical transmutation chain (biome-access payoff).
        RecipeBuilders.mutate(recipeOutput, "mutating_vines", Items.STRING,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 400, Items.VINE, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_bamboo", Items.SUGAR_CANE,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 500, Items.BAMBOO, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_cactus", Items.BAMBOO,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 500, Items.CACTUS, aggregateTicks);

        // Cobweb - Mutator half of the bootstrap-to-duplication loop (Metastasizer duplicates thereafter).
        RecipeBuilders.mutate(recipeOutput, "mutating_cobweb", Items.STRING,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 750, Items.COBWEB, aggregateTicks);

        // Leather - liming (Calcium Blend), Mutator half of its own bootstrap-to-duplication loop.
        RecipeBuilders.mutate(recipeOutput, "mutating_leather", Items.ROTTEN_FLESH,
                ModFluids.SOURCE_CALCIUM_BLEND.get(), 500, Items.LEATHER, solidTicks);

        // Gilding family (Aurous Blend) - golden foods, priced from vanilla's ingot/nugget cost with
        // the machine-efficiency discount applied (see machine notes for the worked math).
        RecipeBuilders.mutate(recipeOutput, "mutating_golden_apple", Items.APPLE,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 6000, Items.GOLDEN_APPLE, solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_golden_carrot", Items.CARROT,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 660, Items.GOLDEN_CARROT, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_glistering_melon_slice", Items.MELON_SLICE,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 660, Items.GLISTERING_MELON_SLICE, aggregateTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_gilded_blackstone", Items.BLACKSTONE,
                ModFluids.SOURCE_AUROUS_BLEND.get(), 550, Items.GILDED_BLACKSTONE, solidTicks);

        // Sunder Chains (Mutator) - hub-and-spoke, not a linear chain: Bone (crafting-table only,
        // no Mutator recipe of its own -- it's the universal entry point/"Calcium" hub) mutates
        // directly into EACH of Iron/Copper/Gold via that metal's own fluid, rather than funneling
        // through Iron first. Any of Iron/Copper/Gold can then separately advance on to Diamond via
        // Molten Diamond, all at the same flat 3000 mB regardless of source material -- Bone itself
        // never mutates directly into Diamond, it has to pass through a metal tier first.
        RecipeBuilders.mutate(recipeOutput, "mutating_iron_sunder_chain", ModItems.BONE_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_FERROUS_BLEND.get(), 3000, ModItems.IRON_SUNDER_CHAIN.get(), solidTicks);

        // Blaze Essence -- a direct upgrade from Iron specifically (thematically the best base for
        // the heat, and closest in stats), ONLY reachable this way -- no crafting-table recipe of
        // its own.
        RecipeBuilders.mutate(recipeOutput, "mutating_blaze_essence_sunder_chain", ModItems.IRON_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_BLAZE_ESSENCE.get(), 3000, ModItems.BLAZE_ESSENCE_SUNDER_CHAIN.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_copper_sunder_chain", ModItems.BONE_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 3000, ModItems.COPPER_SUNDER_CHAIN.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_gold_sunder_chain", ModItems.BONE_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_AUROUS_BLEND.get(), 3000, ModItems.GOLD_SUNDER_CHAIN.get(), solidTicks);

        // Emerald -- a direct upgrade from Gold specifically (not a Bone-hub spoke like the other
        // metals), ONLY reachable this way -- no crafting-table recipe of its own.
        RecipeBuilders.mutate(recipeOutput, "mutating_emerald_sunder_chain", ModItems.GOLD_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_MOLTEN_EMERALD.get(), 3000, ModItems.EMERALD_SUNDER_CHAIN.get(), solidTicks);
        // Diamond's fluid (Molten Diamond) has no production recipe of its own yet (see the crafting
        // notes) -- built anyway, matching the rest of the Molten family's current state (registered,
        // no recipe yet) rather than introducing a one-off special case; unreachable in survival
        // until Molten Diamond gets a real recipe.
        RecipeBuilders.mutate(recipeOutput, "mutating_diamond_sunder_chain_from_iron", ModItems.IRON_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 3000, ModItems.DIAMOND_SUNDER_CHAIN.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_diamond_sunder_chain_from_copper", ModItems.COPPER_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 3000, ModItems.DIAMOND_SUNDER_CHAIN.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_diamond_sunder_chain_from_gold", ModItems.GOLD_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 3000, ModItems.DIAMOND_SUNDER_CHAIN.get(), solidTicks);

        // Netherite -- the capstone above Diamond, ONLY reachable this way (no crafting-table
        // recipe of its own, unlike every material below it). Diamond -> Netherite via 3000 mB
        // Molten Netherite, same flat cost every other Diamond-tier transition in this chain uses.
        RecipeBuilders.mutate(recipeOutput, "mutating_netherite_sunder_chain", ModItems.DIAMOND_SUNDER_CHAIN.get(),
                ModFluids.SOURCE_MOLTEN_NETHERITE.get(), 3000, ModItems.NETHERITE_SUNDER_CHAIN.get(), solidTicks);

        // Shatter Heads (Mutator) -- identical hub-and-spoke shape as the Sunder Chains above,
        // newly built (Shatter previously had no Mutator recipes at all, only independent
        // crafting-table recipes for each material, which are untouched).
        RecipeBuilders.mutate(recipeOutput, "mutating_iron_shatter_head", ModItems.BONE_SHATTER_HEAD.get(),
                ModFluids.SOURCE_FERROUS_BLEND.get(), 3000, ModItems.IRON_SHATTER_HEAD.get(), solidTicks);

        // Blaze Essence -- a direct upgrade from Iron specifically (thematically the best base for
        // the heat, and closest in stats), ONLY reachable this way -- no crafting-table recipe of
        // its own.
        RecipeBuilders.mutate(recipeOutput, "mutating_blaze_essence_shatter_head", ModItems.IRON_SHATTER_HEAD.get(),
                ModFluids.SOURCE_BLAZE_ESSENCE.get(), 3000, ModItems.BLAZE_ESSENCE_SHATTER_HEAD.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_copper_shatter_head", ModItems.BONE_SHATTER_HEAD.get(),
                ModFluids.SOURCE_CUPROUS_BLEND.get(), 3000, ModItems.COPPER_SHATTER_HEAD.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_gold_shatter_head", ModItems.BONE_SHATTER_HEAD.get(),
                ModFluids.SOURCE_AUROUS_BLEND.get(), 3000, ModItems.GOLD_SHATTER_HEAD.get(), solidTicks);

        // Emerald -- a direct upgrade from Gold specifically (not a Bone-hub spoke like the other
        // metals), ONLY reachable this way -- no crafting-table recipe of its own.
        RecipeBuilders.mutate(recipeOutput, "mutating_emerald_shatter_head", ModItems.GOLD_SHATTER_HEAD.get(),
                ModFluids.SOURCE_MOLTEN_EMERALD.get(), 3000, ModItems.EMERALD_SHATTER_HEAD.get(), solidTicks);

        RecipeBuilders.mutate(recipeOutput, "mutating_diamond_shatter_head_from_iron", ModItems.IRON_SHATTER_HEAD.get(),
                ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 3000, ModItems.DIAMOND_SHATTER_HEAD.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_diamond_shatter_head_from_copper", ModItems.COPPER_SHATTER_HEAD.get(),
                ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 3000, ModItems.DIAMOND_SHATTER_HEAD.get(), solidTicks);
        RecipeBuilders.mutate(recipeOutput, "mutating_diamond_shatter_head_from_gold", ModItems.GOLD_SHATTER_HEAD.get(),
                ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 3000, ModItems.DIAMOND_SHATTER_HEAD.get(), solidTicks);

        // Netherite -- the capstone above Diamond, ONLY reachable this way (no crafting-table
        // recipe of its own, unlike every material below it). Diamond -> Netherite via 3000 mB
        // Molten Netherite, matching the Sunder chain's own identical treatment above.
        RecipeBuilders.mutate(recipeOutput, "mutating_netherite_shatter_head", ModItems.DIAMOND_SHATTER_HEAD.get(),
                ModFluids.SOURCE_MOLTEN_NETHERITE.get(), 3000, ModItems.NETHERITE_SHATTER_HEAD.get(), solidTicks);

        // Spider Eye - species rewrite via Primitive Catalyst (identity-change, not material-addition
        // -- see the reagent doctrine in the machine notes), the roster's first de-escalating recipe.
        RecipeBuilders.mutate(recipeOutput, "mutating_spider_eye", ModItems.EYE.get(),
                ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100, Items.SPIDER_EYE, lightTicks);

        // Metastasizer additions riding along in the same pass - plain Terracotta (the ceramics
        // family's plain-duplication half, parallel to the glass family), Cobweb, and Leather
        // duplication (both the steady-state halves of their Mutator bootstrap loops above).
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_terracotta", Items.TERRACOTTA, ModFluids.SOURCE_CLAY_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_cobweb", Items.COBWEB, ModFluids.SOURCE_PROTEIN_BLEND.get(), 250, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_leather", Items.LEATHER, ModFluids.SOURCE_PROTEIN_BLEND.get(), 900, solidTicks);

        // Wool and String - both animal-product duplication, Protein Blend. Revised down from the
        // initial Leather-tier/Nugget-tier pricing (900/110) to 400/100 -- still solid/light tier
        // timing, just cheaper mB, since both read as too pricey relative to how trivial they are to
        // farm in vanilla.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_wool", Items.WHITE_WOOL, ModFluids.SOURCE_PROTEIN_BLEND.get(), 400, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_string", Items.STRING, ModFluids.SOURCE_PROTEIN_BLEND.get(), 100, lightTicks);

        // Dyed glass family - Metastasizer half. Duplication is keyed by the pattern item itself (the
        // specific-colored block/pane), so there's no {ingredient, fluid} collision the way the
        // Mutator's dye-keyed recipes would have -- this is why panes live here instead. Glass Block
        // at the solid tier (1000 mB), matching the Mutator's stained-glass-block cost; Pane at 500 mB
        // (half the block, same volume-scaling logic used elsewhere) and the aggregate tier.
        for (DyeColor color : DyeColor.values()) {
            String c = color.getName();
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_stained_glass_" + c, stainedGlassItem(c), ModFluids.SOURCE_SILICA_BLEND.get(), 1000, solidTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_stained_glass_pane_" + c, stainedGlassPaneItem(c), ModFluids.SOURCE_SILICA_BLEND.get(), 500, aggregateTicks);
        }

        // Plain-glass duplication family (Silica Blend + Calcium Blend, sized by volume) - was
        // documented as "Confirmed" in the crafting notes but never actually built; added now
        // alongside the dyed-glass work discovering the gap. Real glass draws from both silica (sand)
        // and lime (calcium), so items with both a Silica and Calcium identity get two separate
        // recipes each (Metastasizer already supports multiple recipes per output item - no conflict,
        // since the fluid type itself distinguishes them).
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_glass_block", Items.GLASS, ModFluids.SOURCE_SILICA_BLEND.get(), 1000, solidTicks);

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_beaker_silica", ModBlocks.BEAKER_ITEM.get(), ModFluids.SOURCE_SILICA_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_beaker_calcium", ModBlocks.BEAKER_ITEM.get(), ModFluids.SOURCE_CALCIUM_BLEND.get(), 1000, solidTicks);

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_glass_flask_silica", ModItems.GLASS_FLASK.get(), ModFluids.SOURCE_SILICA_BLEND.get(), 250, lightTicks);
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_glass_flask_calcium", ModItems.GLASS_FLASK.get(), ModFluids.SOURCE_CALCIUM_BLEND.get(), 250, lightTicks);

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_glass_pane", Items.GLASS_PANE, ModFluids.SOURCE_SILICA_BLEND.get(), 500, aggregateTicks);

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_calcium_glass", ModBlocks.CALCIUM_GLASS.get(), ModFluids.SOURCE_CALCIUM_BLEND.get(), 1000, solidTicks);

        ////////////////////Pulp Blend\\\\\\\\\\\\\\\\\\\\
        // Masticator input deliberately excludes Nether stems/hyphae (LOGS_THAT_BURN, not the
        // broader LOGS tag) -- mirrors the Sediment Blends' Nether-material deferral, and keeps
        // the Masticator roster consistent with the Metastasizer duplication roster below.
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_masticating", ItemTags.LOGS_THAT_BURN, 1000,
                ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);

        int halfPlankTicks = ModMath.Time.getSecondsToTicks(1.25f); // Slab/Stick -- 125 mB
        int stairsTicks = ModMath.Time.getSecondsToTicks(3.75f);    // 375 mB
        int doorTicks = ModMath.Time.getSecondsToTicks(5);          // 500 mB
        int trapdoorTicks = ModMath.Time.getSecondsToTicks(7.5f);   // 750 mB
        int fenceTicks = ModMath.Time.getSecondsToTicks(12.5f);     // 1250 mB -> 3 Fence

        // Crafted derivatives recycle back into Pulp Blend -- same "no penalty for having already
        // been crafted" convention as the Sediment Blends' recycling recipes (yield matches the
        // forward duplication cost exactly, no lossy discount). Tag-based, so species-agnostic and
        // picks up any other mod's wood set that follows the vanilla tag convention for free.
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_planks", ItemTags.PLANKS, 250,
                ModFluids.SOURCE_PULP_BLEND.get(), 250, lightTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_slabs", ItemTags.WOODEN_SLABS, 125,
                ModFluids.SOURCE_PULP_BLEND.get(), 125, halfPlankTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_stairs", ItemTags.WOODEN_STAIRS, 375,
                ModFluids.SOURCE_PULP_BLEND.get(), 375, stairsTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_doors", ItemTags.WOODEN_DOORS, 500,
                ModFluids.SOURCE_PULP_BLEND.get(), 500, doorTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_trapdoors", ItemTags.WOODEN_TRAPDOORS, 750,
                ModFluids.SOURCE_PULP_BLEND.get(), 750, trapdoorTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_fence_gates", ItemTags.FENCE_GATES, 1000,
                ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_stick", Items.STICK, 125,
                ModFluids.SOURCE_PULP_BLEND.get(), 125, halfPlankTicks);
        // Pressure Plate (2 Planks -> 1) and Button (1 Plank -> 1), same real-vanilla-ratio
        // derivation as the rest of this family -- Pressure Plate costs Door's own 500 mB/100
        // ticks (also a 2-Plank item), Button costs Planks' own 250 mB/50 ticks exactly (it's
        // one whole Plank, no further processing).
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_pressure_plates", ItemTags.WOODEN_PRESSURE_PLATES, 500,
                ModFluids.SOURCE_PULP_BLEND.get(), 500, doorTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_buttons", ItemTags.WOODEN_BUTTONS, 250,
                ModFluids.SOURCE_PULP_BLEND.get(), 250, lightTicks);

        // Fence recycles at itemAmount=3 to match its own 1250 mB-for-3 forward ratio -- the first
        // real use of the Masticator's itemAmount fix outside Bone Meal's originally-blocked case.
        RecipeBuilders.buildMasticating(recipeOutput, "pulp_blend_recycling_fence",
                Ingredient.of(ItemTags.WOODEN_FENCES), 3, Fluids.WATER, 1250,
                ModFluids.SOURCE_PULP_BLEND.get(), 1250, -1, fenceTicks);

        // Species covered so far -- Nether Stems/Hyphae and Bamboo deferred, same "not yet
        // designed" status as the Sediment Blends' own deferred materials.
        String[] woodSpecies = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"};

        for (String species : woodSpecies) {
            // Log-tier items -- Log itself, the all-bark Wood form, and both Stripped variants.
            // Same cost as Log across all four; they're alternate forms of the same material tier,
            // not a different vanilla crafting ratio (same principle as Stone Blend duplicating
            // Andesite/Granite/Diorite individually despite sharing one tier).
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_log",
                    woodItem(species, "log"), ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_wood",
                    woodItem(species, "wood"), ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_stripped_" + species + "_log",
                    woodItem("stripped_" + species, "log"), ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_stripped_" + species + "_wood",
                    woodItem("stripped_" + species, "wood"), ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);

            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_planks",
                    woodItem(species, "planks"), ModFluids.SOURCE_PULP_BLEND.get(), 250, lightTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_slab",
                    woodItem(species, "slab"), ModFluids.SOURCE_PULP_BLEND.get(), 125, halfPlankTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_stairs",
                    woodItem(species, "stairs"), ModFluids.SOURCE_PULP_BLEND.get(), 375, stairsTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_door",
                    woodItem(species, "door"), ModFluids.SOURCE_PULP_BLEND.get(), 500, doorTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_trapdoor",
                    woodItem(species, "trapdoor"), ModFluids.SOURCE_PULP_BLEND.get(), 750, trapdoorTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_fence_gate",
                    woodItem(species, "fence_gate"), ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_pressure_plate",
                    woodItem(species, "pressure_plate"), ModFluids.SOURCE_PULP_BLEND.get(), 500, doorTicks);
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_button",
                    woodItem(species, "button"), ModFluids.SOURCE_PULP_BLEND.get(), 250, lightTicks);

            // Fence is the one item whose vanilla yield (3 per craft) doesn't divide evenly into
            // a per-item mB cost -- built as an explicit 3-count result rather than through the
            // duplicate() helper. Relies on the Metastasizer's existing support for a multi-count
            // result ItemStack (no engine change needed, unlike the Masticator's item-input side).
            RecipeBuilders.buildMetastasizing(recipeOutput, "metastasizing_" + species + "_fence",
                    Ingredient.of(woodItem(species, "fence")), ModFluids.SOURCE_PULP_BLEND.get(), 1250,
                    new ItemStack(woodItem(species, "fence"), 3), fenceTicks);
        }

        // Stick is species-agnostic in vanilla (one item regardless of source wood), so it gets a
        // single shared recipe rather than one per species.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_stick", Items.STICK, ModFluids.SOURCE_PULP_BLEND.get(), 125, halfPlankTicks);

        // Chest (8 Planks = 2000 mB) and Crafting Table (4 Planks = 1000 mB) -- also
        // species-agnostic in vanilla, same real-ratio treatment as everything else in the family.
        int chestTicks = ModMath.Time.getSecondsToTicks(20); // 2000 mB
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_chest", Items.CHEST, ModFluids.SOURCE_PULP_BLEND.get(), 2000, chestTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_chest", Items.CHEST, 2000,
                ModFluids.SOURCE_PULP_BLEND.get(), 2000, chestTicks);

        RecipeBuilders.duplicate(recipeOutput, "metastasizing_crafting_table", Items.CRAFTING_TABLE, ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);
        RecipeBuilders.masticateWithWater(recipeOutput, "pulp_blend_recycling_crafting_table", Items.CRAFTING_TABLE, 1000,
                ModFluids.SOURCE_PULP_BLEND.get(), 1000, solidTicks);

        ////////////////////Leaves Duplication (Crude Slurry -- living, not Pulp Blend)\\\\\\\\\\\\\\\\\\\\
        // Deliberately priced below the Metastasizer's existing lowest bracket (250 mB) -- Leaves
        // are purely decorative and more trivially renewable than anything else duplicated so far
        // (no digging required, just natural decay off any tree). Every overworld species has a
        // regular "<species>_leaves" item, including Mangrove and Cherry, so this reuses woodSpecies
        // directly (no naming exception like Mangrove's Sapling/Propagule split below).
        for (String species : woodSpecies) {
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_leaves",
                    woodItem(species, "leaves"), ModFluids.SOURCE_CRUDE_SLURRY.get(), 100, lightTicks);
        }

        ////////////////////Sapling Duplication (Crude Slurry -- living, not Pulp Blend)\\\\\\\\\\\\\\\\\\\\
        // Same cheap bracket as Leaves' own Crude Slurry duplication recipe (100 mB / 50 ticks --
        // lightTicks) -- Saplings are low-stakes wood-family items by the same reasoning.
        String[] saplingSpecies = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "cherry"};
        for (String species : saplingSpecies) {
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + species + "_sapling",
                    woodItem(species, "sapling"), ModFluids.SOURCE_CRUDE_SLURRY.get(), 100, lightTicks);
        }

        // Mangrove has no Sapling item -- Mangrove Propagule fills that role instead.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_mangrove_propagule",
                woodItem("mangrove", "propagule"), ModFluids.SOURCE_CRUDE_SLURRY.get(), 100, lightTicks);

        ////////////////////Bread Duplication (Crude Slurry)\\\\\\\\\\\\\\\\\\\\
        // Cost pegged to Bread's own existing Crude Slurry yield (crude_slurry_vague_masticating,
        // PLANT_FOOD-tagged) rather than picked independently -- keeps the loop closed, so
        // duplicating Bread never costs less Crude Slurry than masticating a real loaf produces.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_bread", Items.BREAD,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 520, ModMath.Time.getSecondsToTicks(5.2f));

        ////////////////////Dye Duplication (split across fluids by real source material)\\\\\\\\\\\\\\\\\\\\
        // Unlike every other Metastasizer family, dyes don't share one material identity -- vanilla's
        // 16 dyes come from genuinely different source categories, so this deliberately splits them
        // across fluids by what each color is *actually* made from, rather than forcing all 16 onto
        // one fluid (see dyeFluid() below for the per-color mapping and reasoning). All 16 share the
        // same cheap/decorative pricing as Leaves (100 mB / 50 ticks -- lightTicks) regardless of
        // fluid, since every route here is just as trivially renewable as a flower or a bonemeal.
        for (DyeColor color : DyeColor.values()) {
            String c = color.getName();
            RecipeBuilders.duplicate(recipeOutput, "metastasizing_" + c + "_dye", dyeItem(c), dyeFluid(color), 100, lightTicks);
        }

        ////////////////////Molten Family (Stage 2, Charred Masticator only)\\\\\\\\\\\\\\\\\\\\
        // Resolves the long-flagged reachability gap: Tier 1 Masticator tanks reject Lava, so none
        // of this family could actually be crafted until Charred Masticator existed. Item inputs/
        // yields below are placeholder-quality defaults (flat 110 mB/item, 1:1 Lava cost, 30s) for
        // the fluids `dermicraft-crafting-notes.md` still marks "not yet decided" -- easy to retune
        // later, the goal right now is reachability for testing. Molten Redstone is the one member
        // with real confirmed numbers (Redstone Block -> 1000 mB, loose Dust -> 110 mB).
        RecipeBuilders.buildMasticating(recipeOutput, "molten_redstone_masticating_block",
                Ingredient.of(Items.REDSTONE_BLOCK), 1, Fluids.LAVA, 9000,
                ModFluids.SOURCE_MOLTEN_REDSTONE.get(), 9000, -1, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.buildMasticating(recipeOutput, "molten_redstone_masticating_dust",
                Ingredient.of(Items.REDSTONE), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_REDSTONE.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "molten_quartz_masticating",
                Ingredient.of(Items.QUARTZ), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_QUARTZ.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        // Quartz Block -- real vanilla ratio is 4 Quartz (unlike Coal/Redstone/the metal blocks'
        // 9:1), so 4000 mB at double the item's own time, same "block ~2x item time" convention.
        RecipeBuilders.buildMasticating(recipeOutput, "molten_quartz_masticating_block",
                Ingredient.of(Items.QUARTZ_BLOCK), 1, Fluids.LAVA, 4000,
                ModFluids.SOURCE_MOLTEN_QUARTZ.get(), 4000, -1, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.buildMasticating(recipeOutput, "molten_glowstone_masticating",
                Ingredient.of(Items.GLOWSTONE_DUST), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_GLOWSTONE.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        // Glowstone (the block) -- real vanilla ratio is 4 Glowstone Dust, same reasoning as Quartz
        // Block above.
        RecipeBuilders.buildMasticating(recipeOutput, "molten_glowstone_masticating_block",
                Ingredient.of(Items.GLOWSTONE), 1, Fluids.LAVA, 4000,
                ModFluids.SOURCE_MOLTEN_GLOWSTONE.get(), 4000, -1, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.buildMasticating(recipeOutput, "molten_amethyst_masticating",
                Ingredient.of(Items.AMETHYST_SHARD), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_AMETHYST.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        // Block of Amethyst -- real 4:1 vanilla ratio, same convention as Quartz Block/Glowstone.
        RecipeBuilders.buildMasticating(recipeOutput, "molten_amethyst_masticating_block",
                Ingredient.of(Items.AMETHYST_BLOCK), 1, Fluids.LAVA, 4000,
                ModFluids.SOURCE_MOLTEN_AMETHYST.get(), 4000, -1, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.buildMasticating(recipeOutput, "molten_diamond_masticating",
                Ingredient.of(Items.DIAMOND), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        // Diamond Block -- real 9:1 vanilla ratio, same convention as Coal/Redstone/the metal blocks.
        RecipeBuilders.buildMasticating(recipeOutput, "molten_diamond_masticating_block",
                Ingredient.of(Items.DIAMOND_BLOCK), 1, Fluids.LAVA, 9000,
                ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 9000, -1, ModMath.Time.getSecondsToTicks(90));

        RecipeBuilders.buildMasticating(recipeOutput, "molten_emerald_masticating",
                Ingredient.of(Items.EMERALD), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_EMERALD.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        // Emerald Block -- real 9:1 vanilla ratio, same convention as Diamond/Coal/Redstone/the metal blocks.
        RecipeBuilders.buildMasticating(recipeOutput, "molten_emerald_masticating_block",
                Ingredient.of(Items.EMERALD_BLOCK), 1, Fluids.LAVA, 9000,
                ModFluids.SOURCE_MOLTEN_EMERALD.get(), 9000, -1, ModMath.Time.getSecondsToTicks(90));

        RecipeBuilders.buildMasticating(recipeOutput, "molten_lapis_masticating",
                Ingredient.of(Items.LAPIS_LAZULI), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_LAPIS.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        // Lapis Block -- real 9:1 vanilla ratio, same convention as Coal/Redstone/Diamond/Emerald.
        RecipeBuilders.buildMasticating(recipeOutput, "molten_lapis_masticating_block",
                Ingredient.of(Items.LAPIS_BLOCK), 1, Fluids.LAVA, 9000,
                ModFluids.SOURCE_MOLTEN_LAPIS.get(), 9000, -1, ModMath.Time.getSecondsToTicks(90));

        RecipeBuilders.buildMasticating(recipeOutput, "molten_raw_netherite_scrap_masticating",
                Ingredient.of(Items.NETHERITE_SCRAP), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_RAW_NETHERITE_SCRAP.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "blaze_essence_masticating",
                Ingredient.of(Items.BLAZE_POWDER), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_BLAZE_ESSENCE.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "ghast_essence_masticating",
                Ingredient.of(Items.GHAST_TEAR), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_GHAST_ESSENCE.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "wither_essence_masticating",
                Ingredient.of(Items.WITHER_SKELETON_SKULL), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_WITHER_ESSENCE.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "ender_essence_masticating",
                Ingredient.of(Items.ENDER_PEARL), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_ENDER_ESSENCE.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.buildMasticating(recipeOutput, "molten_soul_silica_masticating",
                Ingredient.of(Items.SOUL_SAND), 1, Fluids.LAVA, 1000,
                ModFluids.SOURCE_MOLTEN_SOUL_SILICA.get(), 1000, -1, ModMath.Time.getSecondsToTicks(30));

        // Reverse route (Charred Metastasizer, pattern-based duplication) -- mirrors every Molten
        // Masticating recipe above 1:1 on item and mB, same "forward mirrors reverse" convention as
        // the Ingot/Nugget/Coal/Stone families elsewhere in this file. Needs Charred Metastasizer
        // since these fluids all carry Thermal Hazard, same reachability logic as the forward route.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_redstone_block",
                Items.REDSTONE_BLOCK, ModFluids.SOURCE_MOLTEN_REDSTONE.get(), 9000, ModMath.Time.getSecondsToTicks(45));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_redstone_dust",
                Items.REDSTONE, ModFluids.SOURCE_MOLTEN_REDSTONE.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_quartz",
                Items.QUARTZ, ModFluids.SOURCE_MOLTEN_QUARTZ.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_quartz_block",
                Items.QUARTZ_BLOCK, ModFluids.SOURCE_MOLTEN_QUARTZ.get(), 4000, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_glowstone",
                Items.GLOWSTONE_DUST, ModFluids.SOURCE_MOLTEN_GLOWSTONE.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_glowstone_block",
                Items.GLOWSTONE, ModFluids.SOURCE_MOLTEN_GLOWSTONE.get(), 4000, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_amethyst",
                Items.AMETHYST_SHARD, ModFluids.SOURCE_MOLTEN_AMETHYST.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_amethyst_block",
                Items.AMETHYST_BLOCK, ModFluids.SOURCE_MOLTEN_AMETHYST.get(), 4000, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_diamond",
                Items.DIAMOND, ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_diamond_block",
                Items.DIAMOND_BLOCK, ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 9000, ModMath.Time.getSecondsToTicks(90));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_emerald",
                Items.EMERALD, ModFluids.SOURCE_MOLTEN_EMERALD.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_emerald_block",
                Items.EMERALD_BLOCK, ModFluids.SOURCE_MOLTEN_EMERALD.get(), 9000, ModMath.Time.getSecondsToTicks(90));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_lapis",
                Items.LAPIS_LAZULI, ModFluids.SOURCE_MOLTEN_LAPIS.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_lapis_block",
                Items.LAPIS_BLOCK, ModFluids.SOURCE_MOLTEN_LAPIS.get(), 9000, ModMath.Time.getSecondsToTicks(90));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_raw_netherite_scrap",
                Items.NETHERITE_SCRAP, ModFluids.SOURCE_MOLTEN_RAW_NETHERITE_SCRAP.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        // Pattern is Netherite Ingot, not Scrap -- Molten Netherite is the refined-stage fluid
        // (Scrap + Aurous Blend via Effluencing), so its duplication pattern mirrors that stage.
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_netherite",
                Items.NETHERITE_INGOT, ModFluids.SOURCE_MOLTEN_NETHERITE.get(), 1000, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_blaze_essence",
                Items.BLAZE_POWDER, ModFluids.SOURCE_BLAZE_ESSENCE.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_ghast_essence",
                Items.GHAST_TEAR, ModFluids.SOURCE_GHAST_ESSENCE.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_wither_essence",
                Items.WITHER_SKELETON_SKULL, ModFluids.SOURCE_WITHER_ESSENCE.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_ender_essence",
                Items.ENDER_PEARL, ModFluids.SOURCE_ENDER_ESSENCE.get(), 1000, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.duplicate(recipeOutput, "metastasizing_molten_soul_silica",
                Items.SOUL_SAND, ModFluids.SOURCE_MOLTEN_SOUL_SILICA.get(), 1000, ModMath.Time.getSecondsToTicks(30));

        ////////////////////Render Kiln\\\\\\\\\\\\\\\\\\\\
        // Fluid alone -> a fixed default item, no pattern/no ingredient item -- see machine notes and
        // the Render Kiln build plan doc. Deliberately mirrors the existing Metastasizer reverse-
        // duplication recipes' exact mB/ticks (no discount -- the missing pattern requirement is the
        // reward on its own).
        //
        // Two implant routes to the same machine: 2 Dense Muscle + 2 Nerve Cluster + Furnace +
        // (Bucket or Beaker), sutured, injected with 100 mB Primitive Catalyst.
        RecipeBuilders.buildEarlyImplant(recipeOutput, "render_kiln_implant",
                List.of(
                        Ingredient.of(Items.FURNACE),
                        Ingredient.of(Items.BUCKET),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.RENDER_KILN.asItem());

        ////////////////////Mr. Farmer\\\\\\\\\\\\\\\\\\\\
        // Carved Pumpkin as the structural item -- matches the "head in pain" flavor -- plus a Hoe
        // for the farming function, sutured with the standard Masticator-tier 5-item cost.
        // Proto Brain instead of a raw Nerve Cluster -- Mr. Farmer is full unattended automation
        // (an entire gameplay loop run hands-off), so it's gated behind the real Proto Brain
        // production chain (10 Nerve Clusters + Synapse Catalyst + Craw incubation, see
        // proto_brain_incubating) rather than one cheap organ part.
        RecipeBuilders.buildEarlyImplant(recipeOutput, "mr_farmer_implant",
                List.of(
                        Ingredient.of(Items.CARVED_PUMPKIN),
                        Ingredient.of(Items.IRON_HOE),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.PROTO_BRAIN.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.MR_FARMER.asItem());

        ////////////////////Mr. Shepard\\\\\\\\\\\\\\\\\\\\
        // Shears as the structural/defining item (matches the shearing duty), same 5-item implant shape.
        // Proto Brain in place of the Nerve Cluster -- see Mr. Farmer's note above, same reasoning.
        RecipeBuilders.buildEarlyImplant(recipeOutput, "mr_shepard_implant",
                List.of(
                        Ingredient.of(Items.CARVED_PUMPKIN),
                        Ingredient.of(Items.SHEARS),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.PROTO_BRAIN.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.MR_SHEPARD.asItem());

        ////////////////////Flesh Lab Floor\\\\\\\\\\\\\\\\\\\\
        // Crafting-table roster: structural block in the center, Nerve Cluster on the four edge-middles
        // (plumbing), Dense Muscle on the four corners (connective casing). Structural block is the
        // only thing that varies per variant.
        buildLabFloorRecipe(recipeOutput, "stone_lab_floor", Blocks.STONE, ModBlocks.STONE_LAB_FLOOR);
        buildLabFloorRecipe(recipeOutput, "cobblestone_lab_floor", Blocks.COBBLESTONE, ModBlocks.COBBLESTONE_LAB_FLOOR);
        buildLabFloorRecipe(recipeOutput, "deepslate_lab_floor", Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_LAB_FLOOR);
        buildLabFloorRecipe(recipeOutput, "cobbled_deepslate_lab_floor", Blocks.COBBLED_DEEPSLATE, ModBlocks.COBBLED_DEEPSLATE_LAB_FLOOR);
        buildLabFloorRecipe(recipeOutput, "diorite_lab_floor", Blocks.DIORITE, ModBlocks.DIORITE_LAB_FLOOR);
        buildLabFloorRecipe(recipeOutput, "andesite_lab_floor", Blocks.ANDESITE, ModBlocks.ANDESITE_LAB_FLOOR);
        buildLabFloorRecipe(recipeOutput, "granite_lab_floor", Blocks.GRANITE, ModBlocks.GRANITE_LAB_FLOOR);

        ////////////////////Innards Duct/Node/Gate\\\\\\\\\\\\\\\\\\\\
        // Innards Duct: literally intestines -- a ring of 8 Dense Muscle around an empty center,
        // crafts 8 at once. Cheap connective-tissue infrastructure, not a biological "implant."
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INNARDS_DUCT, 8)
                .pattern("MMM")
                .pattern("M M")
                .pattern("MMM")
                .define('M', ModItems.DENSE_MUSCLE)
                .unlockedBy("has_dense_muscle", has(ModItems.DENSE_MUSCLE))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("innards_duct_crafting_table"));

        // Matches Dense Muscle's own Metastasizer price exactly -- the duct is just shaped muscle.
        RecipeBuilders.duplicate(recipeOutput, "innards_duct_metastasizing", ModBlocks.INNARDS_DUCT.get(),
                ModFluids.SOURCE_PROTEIN_BLEND.get(), 250, lightTicks);

        // Charred (thermal-hazard-tolerant) Duct -- a Tier 1 Duct quenched/hardened in 100mB Lava.
        RecipeBuilders.mutate(recipeOutput, "charred_innards_duct_mutating", ModBlocks.INNARDS_DUCT.get(),
                Fluids.LAVA, 100, ModBlocks.CHARRED_INNARDS_DUCT.get(), solidTicks);

        // Innards Node: Redstone (routing/signal) above a Beaker (the body it routes through),
        // an Inert Tumor at the base (the raw biological seed it's grown from).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INNARDS_NODE)
                .pattern(" R ")
                .pattern(" B ")
                .pattern(" T ")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('B', ModBlocks.BEAKER_ITEM)
                .define('T', ModBlocks.INERT_TUMOR)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("innards_node_crafting_table"));

        // Charred (thermal-hazard-tolerant) Node -- a Tier 1 Node quenched/hardened in a full
        // bucket's worth of Lava, matching the Charred Duct's own convention at 10x the fluid cost
        // (the Node is the one place fluid actually sits in the whole system).
        RecipeBuilders.mutate(recipeOutput, "charred_innards_node_mutating", ModBlocks.INNARDS_NODE.get(),
                Fluids.LAVA, 1000, ModBlocks.CHARRED_INNARDS_NODE.get(), solidTicks);

        // Innards Gate Controller: Redstone Repeater (the logic/priority core) flanked by Nerve
        // Clusters (signal-routing tissue), Inert Tumor at the bottom center (biological seed).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INNARDS_GATE_CONTROLLER)
                .pattern("NRN")
                .pattern("   ")
                .pattern(" T ")
                .define('N', ModItems.NERVE_CLUSTER)
                .define('R', Items.REPEATER)
                .define('T', ModBlocks.INERT_TUMOR)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("innards_gate_controller_crafting_table"));

        // Innards Gate Buffer: Beaker (holding capacity) above a Chest (storage), above an Inert
        // Tumor (biological seed).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INNARDS_GATE_BUFFER)
                .pattern(" B ")
                .pattern(" C ")
                .pattern(" T ")
                .define('B', ModBlocks.BEAKER_ITEM)
                .define('C', Items.CHEST)
                .define('T', ModBlocks.INERT_TUMOR)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("innards_gate_buffer_crafting_table"));

        // Innards Gate Port: Hopper (the I/O interface) above an Inert Tumor (biological seed).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INNARDS_GATE_PORT)
                .pattern(" H ")
                .pattern("   ")
                .pattern(" T ")
                .define('H', Items.HOPPER)
                .define('T', ModBlocks.INERT_TUMOR)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("innards_gate_port_crafting_table"));

        RecipeBuilders.buildEarlyImplant(recipeOutput, "render_kiln_implant_alt",
                List.of(
                        Ingredient.of(Items.FURNACE),
                        Ingredient.of(ModBlocks.BEAKER_ITEM.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.RENDER_KILN.asItem());

        RecipeBuilders.render(recipeOutput, "render_kiln_stone", ModFluids.SOURCE_STONE_BLEND.get(), 1000, Items.STONE, solidTicks);
        RecipeBuilders.render(recipeOutput, "render_kiln_sand", ModFluids.SOURCE_SILICA_BLEND.get(), 750, Items.SAND, aggregateTicks);
        RecipeBuilders.render(recipeOutput, "render_kiln_clay_ball", ModFluids.SOURCE_CLAY_BLEND.get(), 250, Items.CLAY_BALL, lightTicks);
        RecipeBuilders.render(recipeOutput, "render_kiln_iron_ingot", ModFluids.SOURCE_FERROUS_BLEND.get(), 1000, Items.IRON_INGOT, solidTicks);
        RecipeBuilders.render(recipeOutput, "render_kiln_copper_ingot", ModFluids.SOURCE_CUPROUS_BLEND.get(), 1000, Items.COPPER_INGOT, solidTicks);
        RecipeBuilders.render(recipeOutput, "render_kiln_gold_ingot", ModFluids.SOURCE_AUROUS_BLEND.get(), 1000, Items.GOLD_INGOT, solidTicks);
        RecipeBuilders.render(recipeOutput, "render_kiln_coal", ModFluids.SOURCE_CARBON_BLEND.get(), 1000, Items.COAL, lightTicks);
        RecipeBuilders.render(recipeOutput, "render_kiln_redstone_dust", ModFluids.SOURCE_MOLTEN_REDSTONE.get(), 1000,
                Items.REDSTONE, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_quartz", ModFluids.SOURCE_MOLTEN_QUARTZ.get(), 1000,
                Items.QUARTZ, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_glowstone_dust", ModFluids.SOURCE_MOLTEN_GLOWSTONE.get(), 1000,
                Items.GLOWSTONE_DUST, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_diamond", ModFluids.SOURCE_MOLTEN_DIAMOND.get(), 1000,
                Items.DIAMOND, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_emerald", ModFluids.SOURCE_MOLTEN_EMERALD.get(), 1000,
                Items.EMERALD, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_soul_sand", ModFluids.SOURCE_MOLTEN_SOUL_SILICA.get(), 1000,
                Items.SOUL_SAND, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_amethyst_shard", ModFluids.SOURCE_MOLTEN_AMETHYST.get(), 1000,
                Items.AMETHYST_SHARD, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_lapis_lazuli", ModFluids.SOURCE_MOLTEN_LAPIS.get(), 1000,
                Items.LAPIS_LAZULI, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_netherite_scrap", ModFluids.SOURCE_MOLTEN_RAW_NETHERITE_SCRAP.get(), 1000,
                Items.NETHERITE_SCRAP, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_netherite_ingot", ModFluids.SOURCE_MOLTEN_NETHERITE.get(), 1000,
                Items.NETHERITE_INGOT, ModMath.Time.getSecondsToTicks(60));
        RecipeBuilders.render(recipeOutput, "render_kiln_blaze_powder", ModFluids.SOURCE_BLAZE_ESSENCE.get(), 1000,
                Items.BLAZE_POWDER, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_ghast_tear", ModFluids.SOURCE_GHAST_ESSENCE.get(), 1000,
                Items.GHAST_TEAR, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_wither_skeleton_skull", ModFluids.SOURCE_WITHER_ESSENCE.get(), 1000,
                Items.WITHER_SKELETON_SKULL, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_ender_pearl", ModFluids.SOURCE_ENDER_ESSENCE.get(), 1000,
                Items.ENDER_PEARL, ModMath.Time.getSecondsToTicks(30));
        RecipeBuilders.render(recipeOutput, "render_kiln_bone_meal", ModFluids.SOURCE_CALCIUM_BLEND.get(), 334, Items.BONE_MEAL, lightTicks);
        // Mirrors the Metastasizer's metastasizing_blood_nugget (250 mB Protein Blend, solidTicks) --
        // same amount/timing, no pattern item since the Render Kiln is fluid-only input.
        RecipeBuilders.render(recipeOutput, "render_kiln_blood_nugget", ModFluids.SOURCE_PROTEIN_BLEND.get(), 250, ModItems.BLOOD_NUGGET.get(), solidTicks);
        RecipeBuilders.render(recipeOutput, "render_kiln_mre", ModFluids.SOURCE_F_STUFF.get(), 900, ModItems.MRE.get(), 160);
        // Deliberately excluded: Crude Slurry (no solid form defined anywhere in the design notes).

        ////////////////////Gadget Fabricating\\\\\\\\\\\\\\\\\\\\
        // Both Tier 1 Workbench -- every current gadget is Tier 1. No Suture Kit on either: these
        // are machine-fabricated, not hand-stitched. Protein Blend on both represents the organic
        // tissue binding around the Chassis frame (same fluid family Bladder's own Fuel/Feeder
        // mutations draw from); Primitive Catalyst is the standard final-assembly trigger fluid.
        RecipeBuilders.fabricateGadget(recipeOutput, "sipping_fabricating",
                List.of(
                        new ItemStack(ModItems.CHASSIS.get()),
                        new ItemStack(ModItems.PROTO_BRAIN.get()),
                        new ItemStack(ModItems.BLADDER.get())),
                List.of(
                        new FluidStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 250),
                        new FluidStack(ModFluids.SOURCE_PROTEIN_BLEND.get(), 250)),
                ModItems.SIPPING.get(), ModMath.Time.getSecondsToTicks(15), 1);

        // Silica Blend (250 mB, a quarter of the Glass Block's own 1000 mB Mutator cost) is the
        // screen glass for the target-scan display -- Eye justifies the eye_mount/screen lore.
        RecipeBuilders.fabricateGadget(recipeOutput, "drinker_fabricating",
                List.of(
                        new ItemStack(ModItems.CHASSIS.get(), 2),
                        new ItemStack(ModItems.PROTO_BRAIN.get()),
                        new ItemStack(ModItems.EYE.get()),
                        new ItemStack(ModItems.BLADDER.get())),
                List.of(
                        new FluidStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 500),
                        new FluidStack(ModFluids.SOURCE_PROTEIN_BLEND.get(), 500),
                        new FluidStack(ModFluids.SOURCE_SILICA_BLEND.get(), 250)),
                ModItems.DRINKER.get(), ModMath.Time.getSecondsToTicks(30), 1);

        // Eater: same 2-Chassis Primitive Catalyst/Protein Blend rate as Drinker (both scale with
        // Chassis count, not overall item complexity). No Eye/Bladder -- Eater's buffer is
        // item-based, not fluid-based, so neither applies. Craw x4 supplies the item-handling guts
        // (Eater's whole identity), Hopper is the intake justification (no invented lore needed --
        // a Hopper's vanilla function already is "sucks items in"), Silica Blend covers the 4
        // screen bones but isn't a literal 4x of Drinker's single gauge -- Eater's screens are
        // small item-icon windows, not a full target-scan display assembly.
        RecipeBuilders.fabricateGadget(recipeOutput, "eater_fabricating",
                List.of(
                        new ItemStack(ModItems.CHASSIS.get(), 2),
                        new ItemStack(ModBlocks.CRAW.asItem(), 4),
                        new ItemStack(Items.HOPPER),
                        new ItemStack(ModItems.PROTO_BRAIN.get())),
                List.of(
                        new FluidStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 500),
                        new FluidStack(ModFluids.SOURCE_PROTEIN_BLEND.get(), 500),
                        new FluidStack(ModFluids.SOURCE_SILICA_BLEND.get(), 500)),
                ModItems.EATER.get(), ModMath.Time.getSecondsToTicks(30), 1);

        // Sunder: no Proto Brain, deliberately -- unlike Sipping/Drinker/Eater, Sunder isn't a smart
        // device. The auto-swing/auto-targeting logic exists to work around vanilla blocking the
        // attack key while revved (see SunderItem's own class javadoc), not because the weapon
        // itself is making decisions -- the player is still the one aiming it. Fuel Bladder, not the
        // plain Bladder every other gadget uses -- Sunder's own tank is explicitly a fuel tank, not a
        // general buffer. Protein Blend held at the same 500 mB as everything else -- "there isn't
        // much meat to it" compared to a full sensor/buffer gadget. Ferrous Blend bumped hard to
        // 2500 mB (by far the largest single fluid line in the roster) and no separate Iron Ingots --
        // this is what actually builds the bar/housing/guards, no raw-ingot component needed on top
        // of it. Comes out chainless, same as every other path that produces a Sunder -- mounted
        // afterward via Scrench, not baked into the recipe (the generic result type can't carry
        // nested chain data anyway).
        RecipeBuilders.fabricateGadget(recipeOutput, "sunder_fabricating",
                List.of(
                        new ItemStack(ModItems.CHASSIS.get(), 2),
                        new ItemStack(ModItems.FUEL_BLADDER.get())),
                List.of(
                        new FluidStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 500),
                        new FluidStack(ModFluids.SOURCE_PROTEIN_BLEND.get(), 500),
                        new FluidStack(ModFluids.SOURCE_FERROUS_BLEND.get(), 2500)),
                ModItems.SUNDER.get(), ModMath.Time.getSecondsToTicks(30), 1);

        // Shatter -- mirrors Sunder's own recipe exactly (same "not a smart device, no Proto Brain"
        // reasoning, same Fuel Bladder rather than a general buffer, same chainless/headless-on-
        // completion rule -- the head is mounted afterward via Scrench, not baked into the recipe).
        // Only difference: Ferrous Blend bumped from Sunder's 2500 to 3000 mB, to account for the
        // extra mass of the piston hammer head housing itself.
        RecipeBuilders.fabricateGadget(recipeOutput, "shatter_fabricating",
                List.of(
                        new ItemStack(ModItems.CHASSIS.get(), 2),
                        new ItemStack(ModItems.FUEL_BLADDER.get())),
                List.of(
                        new FluidStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 500),
                        new FluidStack(ModFluids.SOURCE_PROTEIN_BLEND.get(), 500),
                        new FluidStack(ModFluids.SOURCE_FERROUS_BLEND.get(), 3000)),
                ModItems.SHATTER.get(), ModMath.Time.getSecondsToTicks(30), 1);
    }

    ////////////////////Vanilla item lookup helpers (for the dye-keyed loops above)\\\\\\\\\\\\\\\\\\\\
    // Fixed 3-tier ladder shared by every upgradeable equipment part (Shatter heads, Sunder chains)
    // -- see project_shatter_head_upgrade_design. Multiplier is stepped (1.5x/2.0x/2.5x of the
    // material's OWN base durability), not compounding; cost escalates 1 bucket per tier
    // (1000/2000/3000 mB).
    private static final float[] PART_UPGRADE_MULTIPLIERS = {1.5f, 2.0f, 2.5f};

    private static void buildPartUpgradeTiers(RecipeOutput recipeOutput, String idPrefix, ItemLike partItem,
                                              Fluid fluid, int baseDurability, int ticks) {
        for (int tier = 0; tier < PART_UPGRADE_MULTIPLIERS.length; tier++) {
            int fluidAmount = (tier + 1) * 1000;
            RecipeBuilders.equipmentPartUpgrade(recipeOutput, idPrefix + "_upgrade_" + (tier + 1) + "_mutating",
                    partItem, fluid, fluidAmount, tier, baseDurability, PART_UPGRADE_MULTIPLIERS[tier], ticks);
        }
    }

    private static Item dyeItem(String colorName) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(colorName + "_dye"));
    }

    // Dye Metastasizer duplication -- per-color fluid, chosen by real source material rather than
    // one blanket fluid for all 16:
    // - White: Calcium Blend, mirroring the vanilla Bone Meal -> White Dye craft (Calcium Blend
    //   already duplicates Bone Meal itself -- see calcium_blend_bone_meal_masticating).
    // - Black: Carbon Blend -- soot/charcoal-black identity.
    // - Gray: Stone Blend -- literal stone-gray.
    // - Light Gray: C-Stuff (Carbon Blend + Calcium Blend, via the Effluentcer) -- a lighter tone
    //   than plain Gray, so the carbon+calcium mix reads as "Gray diluted by White," matching the
    //   fluid's own composition.
    // - Every other color: Crude Slurry (living plant material) -- the flower/plant-derived
    //   majority, including Purple (no single-flower source in vanilla, but purple flowers are
    //   common enough in reality -- lilac, violet, lavender -- to fit the same bucket).
    private static Fluid dyeFluid(DyeColor color) {
        return switch (color) {
            case WHITE -> ModFluids.SOURCE_CALCIUM_BLEND.get();
            case BLACK -> ModFluids.SOURCE_CARBON_BLEND.get();
            case GRAY -> ModFluids.SOURCE_STONE_BLEND.get();
            case LIGHT_GRAY -> ModFluids.SOURCE_C_STUFF.get();
            default -> ModFluids.SOURCE_CRUDE_SLURRY.get();
        };
    }

    private static Item stainedGlassItem(String colorName) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(colorName + "_stained_glass"));
    }

    private static Item stainedGlassPaneItem(String colorName) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(colorName + "_stained_glass_pane"));
    }

    private static Item coloredTerracottaItem(String colorName) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(colorName + "_terracotta"));
    }

    private static Item glazedTerracottaItem(String colorName) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(colorName + "_glazed_terracotta"));
    }

    private static Item woodItem(String species, String suffix) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(species + "_" + suffix));
    }

    private static void buildLabFloorRecipe(RecipeOutput recipeOutput, String name, ItemLike structuralBlock, ItemLike result) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("DND")
                .pattern("NSN")
                .pattern("DND")
                .define('D', ModItems.DENSE_MUSCLE.get())
                .define('N', ModItems.NERVE_CLUSTER.get())
                .define('S', structuralBlock)
                .unlockedBy("has_nerve_cluster", has(ModItems.NERVE_CLUSTER))
                .save(recipeOutput, RecipeBuilders.getResourceLocation(name + "_crafting_table"));
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
