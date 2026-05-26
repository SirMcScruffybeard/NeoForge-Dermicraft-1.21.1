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

public class ModTags {

    public static class Blocks {

        public static final TagKey<Block>  COLLECTIBLE = createTag("collectible");
        public static final TagKey<Block> HARVESTABLE = createTag("harvestable");
        public static final TagKey<Block> INJECTABLE = createTag("injectable");
       public static final TagKey<Block> HAS_SCREEN = createTag("has_screen");
        public static final TagKey<Block> SUTABLE = createTag("sutable");
        public static final TagKey<Block> TUMORS = createTag("tumors");


        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> HARVEST_TOOLS = createTag("harvest_tools");
        public static final TagKey<Item> SUTURE_TOOLS = createTag("suture_tools");
        public static final TagKey<Item> INJECTION_TOOLS = createTag("injection_tools");

        public static final TagKey<Item> ANIMAL_MEATS = createTag("animal_meats");


        public static final TagKey<Item> PLANT_FOOD = createTag("plant_food");
        public static final TagKey<Item> MEAT_FOOD = createTag("meat_food");

        public static final TagKey<Item> PART_ITEMS = createTag("part_items");

        public static final TagKey<Item> BIOFUELS = createTag("biofuels");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
        }
    }

    public static class Fluids {
        public static final TagKey<Fluid> BIOFUELS = createTag("biofuels");

        public static final TagKey<Fluid> HAZARDOUS = createTag("hazardous");

        private static TagKey<Fluid> createTag(String name) {
            return FluidTags.create(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
        }
    }
}
