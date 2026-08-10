package net.scruffy.dermicraft.datagen.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
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

        // Every Lab Floor variant -- what FloorNetwork walks across to find a station's shared pool.
        // A tag rather than a hardcoded block list so the walk needs no changes when a new floor
        // variant is added, and so a future tier split can just become child tags of this one
        // (tier lives on the block TYPE, which is why floors need no block entity at all).
        public static final TagKey<Block> LAB_FLOOR = createTag("lab_floor");


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

        // Gadgets with a genuine onboard fuel tank (biofuel-only capability, same restriction Fuel
        // Bladder uses) -- what Workbench Duty 2's passive auto-refill targets. Deliberately NOT
        // every IGadget with a fluid capability: Drinker/Sipping's tanks hold arbitrary payload
        // fluid (drink content, vacuumed liquid) rather than fuel, and would accept a passive
        // biofuel top-off same as any other fluid, silently draining the shared pool dry for a tank
        // that was never meant to burn fuel at all. See Sunder's own fluid-handler registration in
        // ModBusEvents for the capability-side counterpart of this restriction.
        public static final TagKey<Item> FUEL_CONSUMING_GADGETS = createTag("fuel_consuming_gadgets");

        // Gadget Modules (see dermicraft-gadget-notes.md -> Gadget upgrade points -> Modules
        // direction note). Deliberately tags, not a Java enum, so a third-party mod's item can join
        // an existing category via a datapack tag add alone -- e.g. Eater should be able to pick up
        // another mod's tagged item as a working module with zero code on either side.
        public static final TagKey<Item> MODULES = createTag("modules");

        // "Target" family (material/specialty modules -- what Eater's mouth is currently configured
        // to do). Each kind gets its own specific tag; membership IS the whole behavior, no extra
        // data map needed the way Safety Modules need one below.
        public static final TagKey<Item> MODULE_AGGREGATE = createTag("module/aggregate");

        // "Fluid" family -- mundane "can target through plain fluid at all," kept deliberately
        // separate from hazard tolerance (see MODULE_SAFETY below) so kelp/fish-style combos (which
        // live in ordinary water) stay cheap regardless of hazard progression.
        public static final TagKey<Item> MODULE_FLUID_BYPASS = createTag("module/fluid_bypass");

        // "Hazard" family -- generic membership only ("this is A Safety Module"); WHICH hazard(s) a
        // specific one grants lives in ModDataMaps.SAFETY_MODULE_PROPERTIES instead of a per-hazard
        // tag, so adding a new hazard kind later is a data map entry, not a new tag + new dispatch.
        public static final TagKey<Item> MODULE_SAFETY = createTag("module/safety");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
        }
    }

    public static class EntityTypes {
        // Denylist, not an allowlist -- most living things bleed, so exceptions are named
        // explicitly rather than requiring every bleedable mob to be tagged in. A new mob added to
        // the game (or another mod) is bleedable by default without needing to be added here.
        public static final TagKey<EntityType<?>> NOT_BLEEDABLE = createTag("not_bleedable");

        private static TagKey<EntityType<?>> createTag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
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
