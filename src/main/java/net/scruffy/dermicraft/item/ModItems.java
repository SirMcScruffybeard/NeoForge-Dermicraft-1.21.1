package net.scruffy.dermicraft.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.item.custom.*;
import net.scruffy.dermicraft.item.custom.base.PartItem;
import net.scruffy.dermicraft.property.ModFoodProperties;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Dermicraft.MOD_ID);
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


    ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> FORCEPS = ITEMS.register("forceps", () -> new ForcepsItem());
    public static final DeferredItem<Item> PRIMITIVE_FORCEPS = ITEMS.register("primitive_forceps",
            () -> new ForcepsItem(ForcepsItem.PRIMITIVE_DURABILITY));
    public static final DeferredItem<Item> SCALPEL = ITEMS.register("scalpel",
            () -> new ScalpelItem(ScalpelItem.IRON_DURABILITY));
    // Primitive tools: no-iron alternates for the surgical toolkit, so Machine bootstrapping
    // (Masticator/Metastasizer implants) doesn't require mining. Same behavior as their iron
    // counterparts, deliberately weaker stats.
    public static final DeferredItem<Item> PRIMITIVE_SCALPEL = ITEMS.register("primitive_scalpel",
            () -> new ScalpelItem(ScalpelItem.PRIMITIVE_DURABILITY));
    public static final DeferredItem<Item> SUTURE_KIT = ITEMS.register("suture_kit",
            () -> new SutureKitItem(SutureKitItem.IRON_DURABILITY));
    public static final DeferredItem<Item> PRIMITIVE_SUTURE_KIT = ITEMS.register("primitive_suture_kit",
            () -> new SutureKitItem(SutureKitItem.PRIMITIVE_DURABILITY));
    public static final DeferredItem<Item> SYRINGE = ITEMS.register("syringe", () -> new SyringeItem());
    public static final DeferredItem<Item> PRIMITIVE_SYRINGE = ITEMS.register("primitive_syringe",
            () -> new SyringeItem(SyringeItem.PRIMITIVE_DURABILITY));
    public static final DeferredItem<Item> GLASS_FLASK = ITEMS.register("glass_flask", GlassFlaskItem::new);
    public static final DeferredItem<Item> IDEP = ITEMS.register("idep", IdepItem::new);
    public static final DeferredItem<Item> BLADDER = ITEMS.register("bladder", () -> new BladderItem(false));
    public static final DeferredItem<Item> FUEL_BLADDER = ITEMS.register("fuel_bladder", () -> new BladderItem(false));
    public static final DeferredItem<Item> FEEDER_BLADDER = ITEMS.register("feeder_bladder", () -> new BladderItem(true));

    // Thermal-hazard-tolerant Bladder tier -- double capacity (matches Charred Tank/Craw's own
    // convention), mutated from the base Bladder in the Mutator.
    public static final DeferredItem<Item> CHARRED_BLADDER = ITEMS.register("charred_bladder",
            () -> new BladderItem(false, BladderItem.CAPACITY * 2));
    public static final DeferredItem<Item> CHARRED_FUEL_BLADDER = ITEMS.register("charred_fuel_bladder",
            () -> new BladderItem(false, BladderItem.CAPACITY * 2));
    public static final DeferredItem<Item> CHARRED_FEEDER_BLADDER = ITEMS.register("charred_feeder_bladder",
            () -> new BladderItem(true, BladderItem.CAPACITY * 2));

    // Generous durability, deliberately -- see ScrenchItem's class javadoc. 1 point of wear per
    // completed chain swap only (see ScrenchMenu#applyCompletedSwapCosts), so this is a long-tail
    // flavor mechanic, not a real balancing lever.
    public static final DeferredItem<Item> SCRENCH = ITEMS.register("scrench", () -> new ScrenchItem(new Item.Properties().durability(250)));

    // Placeholder durability (per material, matching each one's own ChainProperties durability
    // value -- see ModDataMapProvider) -- see SunderChainItem's class javadoc.
    public static final DeferredItem<Item> IRON_SUNDER_CHAIN = ITEMS.register("iron_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(250)));
    public static final DeferredItem<Item> COPPER_SUNDER_CHAIN = ITEMS.register("copper_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(150)));
    public static final DeferredItem<Item> GOLD_SUNDER_CHAIN = ITEMS.register("gold_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(100)));
    public static final DeferredItem<Item> DIAMOND_SUNDER_CHAIN = ITEMS.register("diamond_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(400)));
    public static final DeferredItem<Item> BONE_SUNDER_CHAIN = ITEMS.register("bone_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(75)));

    // Netherite -- the capstone material above Diamond. Durability 520: vanilla's own
    // Netherite:Diamond tool-durability ratio (2031/1561 =~ 1.301) applied to Diamond's own 400
    // here, since (unlike Shatter's heads) chain durability was never derived from vanilla tool
    // numbers to begin with, so there's no existing 1:1/2x rule to extend directly.
    // .fireResistant() -- real vanilla Netherite parity: a dropped item survives lava/fire.
    // Durability is NOT vanilla-parity (vanilla Netherite tools still take normal wear, just with
    // a bigger number) -- deliberately not made unbreakable, see the design discussion.
    public static final DeferredItem<Item> NETHERITE_SUNDER_CHAIN = ITEMS.register("netherite_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(520).fireResistant()));

    // Emerald -- a toned-down Diamond, not a capstone: durability 300 sits between Iron's 250 and
    // Diamond's 400, matching every other stat's own partial step toward Diamond (see
    // ModDataMapProvider). Only reachable by mutating from the Gold chain (a direct upgrade), no
    // crafting-table recipe of its own.
    public static final DeferredItem<Item> EMERALD_SUNDER_CHAIN = ITEMS.register("emerald_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(300)));

    // Blaze Essence -- a mid-tier flavor material (baseline stats match Iron exactly), not a power
    // tier: the value is entirely in its two signature traits (ignite on hit, log->charcoal while
    // sawing -- see ChainProperties' own javadoc). Only reachable by mutating from the Iron chain.
    public static final DeferredItem<Item> BLAZE_ESSENCE_SUNDER_CHAIN = ITEMS.register("blaze_essence_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(250)));

    // Mode/hazard/fluid-buffer game logic not yet implemented -- see SippingItem's class javadoc.
    public static final DeferredItem<Item> SIPPING = ITEMS.register("sipping", () -> new SippingItem(new Item.Properties().durability(SippingItem.MAX_HP)));

    // Durability is gadget HP, not wear -- see IGadget.
    public static final DeferredItem<Item> DRINKER = ITEMS.register("drinker", () -> new DrinkerItem(new Item.Properties().durability(DrinkerItem.MAX_HP)));

    // Durability is gadget HP, not wear -- see IGadget. Base tier only, see EaterItem's class javadoc.
    public static final DeferredItem<Item> EATER = ITEMS.register("eater", () -> new EaterItem(new Item.Properties().durability(EaterItem.MAX_HP)));

    ////////////////////Weapons\\\\\\\\\\\\\\\\\\\\
    // No .attributes() here on purpose -- Sunder's combat stats vary with the mounted chain, so they
    // come from its getDefaultAttributeModifiers(ItemStack) override instead. Setting them here would
    // shadow that override entirely (see the method's javadoc). durability(MAX_HP) is unrelated to
    // that concern (a different data component, DataComponents.MAX_DAMAGE) -- see IGadget.
    public static final DeferredItem<Item> SUNDER = ITEMS.register("sunder",
            () -> new SunderItem(new Item.Properties().stacksTo(1).durability(SunderItem.MAX_HP)));

    // Registration/visibility pass only -- see ShatterItem's class javadoc.
    public static final DeferredItem<Item> SHATTER = ITEMS.register("shatter",
            () -> new ShatterItem(new Item.Properties().stacksTo(1).durability(ShatterItem.MAX_HP)));

    // Durability is 2x its corresponding vanilla pickaxe's own max damage (2026-08-15, revised from
    // an exact 1:1 match -- bumped to offset the heavier recipe cost, deliberately kept as a clean
    // multiplier of the vanilla value rather than an arbitrary new number, see the design notes).
    // Head wear is per-block-mined/per-attack, see ShatterItem#wearHead. Iron: 500 (2x Iron Pickaxe's
    // 250) -- the baseline material, see the Shatter design notes.
    public static final DeferredItem<Item> IRON_SHATTER_HEAD = ITEMS.register("iron_shatter_head",
            () -> new ShatterHeadItem(new Item.Properties().durability(500)));

    // Copper -- same mining tier and damage as Iron (no combat/mining identity of its own), but
    // durability closer to Bone/Stone's 262 than Iron's 500 -- a cheaper, weaker-wearing alternative
    // to Iron rather than a distinct material tier. Deliberately no dedicated special (2026-08-17,
    // decided): just tier + durability, nothing else.
    public static final DeferredItem<Item> COPPER_SHATTER_HEAD = ITEMS.register("copper_shatter_head",
            () -> new ShatterHeadItem(new Item.Properties().durability(230)));

    // Bone -- hand-painted face/tail texture (not the shared grayscale one), so no runtime tint is
    // registered for its item icon; the mounted weapon still reads its tint from ShatterHeadProperties
    // (set untinted, same as Iron) so the piston bones show the bare tool texture until a real tint
    // is decided. Durability 262 (2x Stone Pickaxe's 131) -- Bone stands in for the Stone mining tier
    // (see ShatterHeadProperties' mining_tier entry for this material).
    public static final DeferredItem<Item> BONE_SHATTER_HEAD = ITEMS.register("bone_shatter_head",
            () -> new ShatterHeadItem(new Item.Properties().durability(262)));

    // Gold -- shared grayscale face/tail texture, tinted at runtime (same technique as Iron, just a
    // real color this time instead of a no-op white). Durability 64 (2x Gold Pickaxe's 32) -- gold is
    // a soft, fragile metal, lowest durability of any vanilla tool material, still true relatively
    // even after the 2x bump.
    public static final DeferredItem<Item> GOLD_SHATTER_HEAD = ITEMS.register("gold_shatter_head",
            () -> new ShatterHeadItem(new Item.Properties().durability(64)));

    // Diamond -- hand-painted face/tail texture, same as Bone. Durability 3122 (2x Diamond Pickaxe's
    // 1561).
    public static final DeferredItem<Item> DIAMOND_SHATTER_HEAD = ITEMS.register("diamond_shatter_head",
            () -> new ShatterHeadItem(new Item.Properties().durability(3122)));

    // Emerald -- a toned-down Diamond, not a capstone: shares Diamond's mining tier (3) but weaker
    // combat stats, durability 1800 (a felt number, no vanilla Emerald Pickaxe exists to derive a
    // ratio from the way Netherite's did). Treated as a metal (tinted shared texture), same as
    // Iron/Copper/Gold/Netherite. Only reachable by mutating from the Gold head (a direct upgrade),
    // no crafting-table recipe of its own.
    public static final DeferredItem<Item> EMERALD_SHATTER_HEAD = ITEMS.register("emerald_shatter_head",
            () -> new ShatterHeadItem(new Item.Properties().durability(1800)));

    // Blaze Essence -- a mid-tier flavor material (baseline stats match Iron exactly, durability
    // 500 = 2x Iron Pickaxe's 250, same rule every non-capstone material follows), not a power
    // tier: the value is entirely in its two signature traits (ignite on hit, universal auto-smelt
    // -- see ShatterHeadProperties' own javadoc). Only reachable by mutating from the Iron head.
    public static final DeferredItem<Item> BLAZE_ESSENCE_SHATTER_HEAD = ITEMS.register("blaze_essence_shatter_head",
            () -> new ShatterHeadItem(new Item.Properties().durability(500)));

    // Netherite -- the capstone material above Diamond, treated as a metal (shared grayscale
    // texture, tinted at runtime same as Iron/Copper/Gold) rather than getting its own hand-painted
    // texture like Bone/Diamond. Durability 4062 (2x Netherite Pickaxe's 2031), continuing the same
    // 2x-vanilla-pickaxe rule every other material already follows exactly.
    // .fireResistant() -- real vanilla Netherite parity: a dropped item survives lava/fire.
    public static final DeferredItem<Item> NETHERITE_SHATTER_HEAD = ITEMS.register("netherite_shatter_head",
            () -> new ShatterHeadItem(new Item.Properties().durability(4062).fireResistant()));

    ////////////////////Parts\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> EYE = ITEMS.register("eye",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.EYE)));

    public static  DeferredItem<Item> NERVE_CLUSTER = ITEMS.register("nerve_cluster",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.NERVE_CLUSTER)));

    public static DeferredItem<Item> DENSE_MUSCLE = ITEMS.register("dense_muscle",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.DENSE_MUSCLE)));

    // Blood Nugget: trace-iron intermediate for the Protein Blend -> Ferrous Blend alternate route
    // (Metastasizer: Iron Nugget pattern + Protein Blend -> Blood Nugget; Masticator: Blood Nugget +
    // Primitive Catalyst -> Ferrous Blend). Plain crafting item, not food.
    public static final DeferredItem<Item> BLOOD_NUGGET = ITEMS.register("blood_nugget",
            () -> new Item(new Item.Properties()));

    // Proto Brain: immature Brain-derived tissue, grown from Nerve Cluster via Craw injection.
    // Shared crafting ingredient across Gadgets and (eventually) Brain Block -- plain crafting
    // item, not food, matching Blood Nugget's precedent as a synthesized (not harvested) part.
    public static final DeferredItem<Item> PROTO_BRAIN = ITEMS.register("proto_brain",
            () -> new Item(new Item.Properties()));

    // Chassis: shared structural item across Gadgets (quantity scales per-gadget by size,
    // judgment call) and the universal Module Frame below. Plain crafting item.
    public static final DeferredItem<Item> CHASSIS = ITEMS.register("chassis",
            () -> new Item(new Item.Properties()));

    // Module Frame (renamed from "Add-on Frame" 2026-08-09): single universal item that lets any
    // equipment-origin item dock into a suit module slot (see dermicraft-suit-notes.md) --
    // deliberately priced above Chassis itself, since it's permanent swappable infrastructure, not
    // a one-time consumable.
    public static final DeferredItem<Item> MODULE_FRAME = ITEMS.register("module_frame",
            () -> new Item(new Item.Properties()));

    // Gadget Modules -- first slice (2026-08-09), see dermicraft-gadget-notes.md -> Gadget upgrade
    // points -> Modules direction note's worked Eater example. Plain items for now: no durability/
    // wear axis has been decided (loss is total -- destroyed means gone, not gradually worn), and
    // no per-instance data yet (kind + hazard grant both come from tags/data maps, not the stack
    // itself). Tag membership (see ModTags.Items) is what actually makes these behave as modules;
    // Java identity here is just for recipes/creative-tab/model wiring.
    public static final DeferredItem<Item> AGGREGATE_MODULE = ITEMS.register("aggregate_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BEAM_MODULE = ITEMS.register("beam_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FLUID_BYPASS_MODULE = ITEMS.register("fluid_bypass_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HEAT_SAFETY_MODULE = ITEMS.register("heat_safety_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> METAPHYSICAL_SAFETY_MODULE = ITEMS.register("metaphysical_safety_module",
            () -> new Item(new Item.Properties()));

    // Evolution Module family (dermicraft-progression-notes.md / dermicraft-machine-notes.md,
    // Drooling Cauldron entry "Evolution Module family") -- item only for now, no mechanics wired
    // up yet. Not tagged MODULE_SAFETY like the Safety Modules above: that tag is what makes
    // IHaveModules#installedHazardProfile treat an item as granting hazard tolerance, and this
    // doesn't do that (yet) -- just MODULES, so it can sit in a Module slot.
    public static final DeferredItem<Item> HEAT_EVOLUTION_MODULE = ITEMS.register("heat_evolution_module",
            () -> new Item(new Item.Properties()));

    // Work Speed Module -- tagged MODULE_WORK_SPEED (not MODULE_SAFETY), same "kind via tag, data
    // via data map" split as the Safety Modules above.
    public static final DeferredItem<Item> WORK_SPEED_MODULE = ITEMS.register("work_speed_module",
            () -> new Item(new Item.Properties()));

    // Keep-on-Death family -- tagged MODULE_KEEP_ON_DEATH, not MODULE_SAFETY/MODULE_WORK_SPEED.
    // Salvage: cheap, consumed on trigger (Totem of Undying shape). Anchor: expensive, never
    // consumed, gated only by Config#ANCHOR_MODULE_COOLDOWN_SECONDS (0 = no cooldown).
    public static final DeferredItem<Item> SALVAGE_MODULE = ITEMS.register("salvage_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ANCHOR_MODULE = ITEMS.register("anchor_module",
            () -> new Item(new Item.Properties()));

    // Capacity Module -- tagged MODULE_CAPACITY. Fluid-only for now (see CapacityModuleProperties);
    // machines first, gadget fluid tanks (Drinker/Sipping/Sunder/Shatter) a deliberate follow-up.
    public static final DeferredItem<Item> CAPACITY_MODULE = ITEMS.register("capacity_module",
            () -> new Item(new Item.Properties()));

    ////////////////////Food\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> MRE = ITEMS.register("mre",
            () -> new Item(new Item.Properties().food(ModFoodProperties.MRE)));

    public static final DeferredItem<Item> MEAT_FLAVORED_MEAT = ITEMS.register("meat_flavored_meat",
            () -> new Item(new Item.Properties().food(ModFoodProperties.MEAT_FLAVORED_MEAT)));



    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\

}
