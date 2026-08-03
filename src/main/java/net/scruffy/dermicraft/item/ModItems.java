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
    public static final DeferredItem<Item> FORCEPS = ITEMS.register("forceps", ForcepsItem::new);
    public static final DeferredItem<Item> SCALPEL = ITEMS.register("scalpel", ScalpelItem::new);
    public static final DeferredItem<Item> SUTURE_KIT = ITEMS.register("suture_kit", SutureKitItem::new);
    public static final DeferredItem<Item> SYRINGE = ITEMS.register("syringe", SyringeItem::new);
    public static final DeferredItem<Item> GLASS_FLASK = ITEMS.register("glass_flask", GlassFlaskItem::new);
    public static final DeferredItem<Item> IDEP = ITEMS.register("idep", IdepItem::new);
    public static final DeferredItem<Item> BLADDER = ITEMS.register("bladder", () -> new BladderItem(false));
    public static final DeferredItem<Item> FUEL_BLADDER = ITEMS.register("fuel_bladder", () -> new BladderItem(false));
    public static final DeferredItem<Item> FEEDER_BLADDER = ITEMS.register("feeder_bladder", () -> new BladderItem(true));

    // Generous durability, deliberately -- see ScrenchItem's class javadoc. 1 point of wear per
    // completed chain swap only (see ScrenchMenu#applyCompletedSwapCosts), so this is a long-tail
    // flavor mechanic, not a real balancing lever.
    public static final DeferredItem<Item> SCRENCH = ITEMS.register("scrench", () -> new ScrenchItem(new Item.Properties().durability(250)));

    // Placeholder durability -- see SunderChainItem's class javadoc.
    public static final DeferredItem<Item> IRON_SUNDER_CHAIN = ITEMS.register("iron_sunder_chain",
            () -> new SunderChainItem(new Item.Properties().durability(250)));

    // Mode/hazard/fluid-buffer game logic not yet implemented -- see SippingItem's class javadoc.
    public static final DeferredItem<Item> SIPPING = ITEMS.register("sipping", () -> new SippingItem(new Item.Properties().durability(SippingItem.MAX_HP)));

    // Durability is gadget HP, not wear -- see IGadget.
    public static final DeferredItem<Item> DRINKER = ITEMS.register("drinker", () -> new DrinkerItem(new Item.Properties().durability(DrinkerItem.MAX_HP)));

    // Durability is gadget HP, not wear -- see IGadget. Base tier only, see EaterItem's class javadoc.
    public static final DeferredItem<Item> EATER = ITEMS.register("eater", () -> new EaterItem(new Item.Properties().durability(EaterItem.MAX_HP)));

    ////////////////////Weapons\\\\\\\\\\\\\\\\\\\\
    // Placeholder -- no rev/dig-in mechanic yet, see SunderItem's class javadoc. In-game only for model preview.
    // No .attributes() here on purpose -- Sunder's combat stats vary with the mounted chain, so they
    // come from its getDefaultAttributeModifiers(ItemStack) override instead. Setting them here would
    // shadow that override entirely (see the method's javadoc).
    public static final DeferredItem<Item> SUNDER = ITEMS.register("sunder",
            () -> new SunderItem(new Item.Properties().stacksTo(1)));

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

    ////////////////////Food\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> MRE = ITEMS.register("mre",
            () -> new Item(new Item.Properties().food(ModFoodProperties.MRE)));

    public static final DeferredItem<Item> MEAT_FLAVORED_MEAT = ITEMS.register("meat_flavored_meat",
            () -> new Item(new Item.Properties().food(ModFoodProperties.MEAT_FLAVORED_MEAT)));



    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\

}
