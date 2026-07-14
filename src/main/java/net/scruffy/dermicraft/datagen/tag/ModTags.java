package net.scruffy.dermicraft.datagen.tag;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.List;

public class ModTags {

    public static class Blocks {

        public static final TagKey<Block> COLLECTIBLE = createTag("collectible");
        public static final TagKey<Block> EXTRACTABLE = createTag("harvestable");
        public static final TagKey<Block> INJECTABLE = createTag("injectable");
        public static final TagKey<Block> HAS_SCREEN = createTag("has_screen");
        public static final TagKey<Block> SUTABLE = createTag("sutable");
        public static final TagKey<Block> TUMORS = createTag("tumors");
        public static final TagKey<Block> DIPPING_TANKS = createTag("dipping_tanks");


        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> COLLECTION_TOOLS = createTag("collection_tools");
        public static final TagKey<Item> EXTRACTION_TOOLS = createTag("harvest_tools");
        public static final TagKey<Item> INJECTION_TOOLS = createTag("injection_tools");
        public static final TagKey<Item> SUTURE_TOOLS = createTag("suture_tools");


        public static final TagKey<Item> ANIMAL_MEATS = createTag("animal_meats");


        public static final TagKey<Item> PLANT_FOOD = createTag("plant_food");
        public static final TagKey<Item> MEAT_FOOD = createTag("meat_food");

        public static final TagKey<Item> PART_ITEMS = createTag("part_items");

        public static final TagKey<Item> BIOFUELS = createTag("biofuels");

        public static final TagKey<Item> STONE_BLEND_ROSTER = createTag("stone_blend_roster");
        public static final TagKey<Item> SILICA_BLEND_ROSTER = createTag("silica_blend_roster");
        public static final TagKey<Item> CLAY_BLEND_ROSTER = createTag("clay_blend_roster");

        public static final TagKey<Item> SILICA_BLEND_RECYCLING = createTag("silica_blend_recycling");
        public static final TagKey<Item> CLAY_BLEND_RECYCLING = createTag("clay_blend_recycling");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
        }
    }

    public static class Fluids {
        public static final TagKey<Fluid> BIOFUELS = createTag("biofuels");

        // Union tag: membership = "in any child hazard tag below". Coarse "is this dangerous at all?" check.
        public static final TagKey<Fluid> HAZARDOUS = createTag("hazardous");

        // Specific hazard kinds. A fluid may belong to more than one.
        public static final TagKey<Fluid> EXTREME_HEAT = createTag("hazard/extreme_heat");
        public static final TagKey<Fluid> RADIATION_MILD = createTag("hazard/radiation_mild");
        public static final TagKey<Fluid> RADIATION_SEVERE = createTag("hazard/radiation_severe");
        public static final TagKey<Fluid> BIOHAZARD = createTag("hazard/biohazard");
        public static final TagKey<Fluid> METAPHYSICAL_MILD = createTag("hazard/metaphysical_mild");
        public static final TagKey<Fluid> METAPHYSICAL_SEVERE = createTag("hazard/metaphysical_severe");

        // Java-side roster of every hazard KIND, for code that must iterate the individual
        // hazard tags (e.g. HazardProfile). The HAZARDOUS union tag only answers the runtime
        // yes/no "is this fluid dangerous"; it can't hand back its child TagKeys.
        public static final List<TagKey<Fluid>> ALL_HAZARDS = List.of(
                EXTREME_HEAT, RADIATION_MILD, RADIATION_SEVERE, BIOHAZARD,
                METAPHYSICAL_MILD, METAPHYSICAL_SEVERE);

        public static final TagKey<Fluid> THICK = createTag("thick");
        public static final TagKey<Fluid> THIN = createTag("thin");

        private static TagKey<Fluid> createTag(String name) {
            return FluidTags.create(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
        }
    }
}
