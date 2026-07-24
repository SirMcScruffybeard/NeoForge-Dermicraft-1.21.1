package net.scruffy.dermicraft.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.item.custom.*;
import net.scruffy.dermicraft.item.custom.base.PartItem;
import net.scruffy.dermicraft.item.custom.test.TestRigItem;
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

    // Mode/hazard/fluid-buffer game logic not yet implemented -- see SippingItem's class javadoc.
    public static final DeferredItem<Item> SIPPING = ITEMS.register("sipping", () -> new SippingItem(new Item.Properties().stacksTo(1)));

    ////////////////////Parts\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> EYE = ITEMS.register("eye",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.EYE)));

    public static  DeferredItem<Item> NERVE_CLUSTER = ITEMS.register("nerve_cluster",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.NERVE_CLUSTER)));

    public static DeferredItem<Item> DENSE_MUSCLE = ITEMS.register("dense_muscle",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.DENSE_MUSCLE)));

    ////////////////////Food\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> MRE = ITEMS.register("mre",
            () -> new Item(new Item.Properties().food(ModFoodProperties.MRE)));

    public static final DeferredItem<Item> MEAT_FLAVORED_MEAT = ITEMS.register("meat_flavored_meat",
            () -> new Item(new Item.Properties().food(ModFoodProperties.MEAT_FLAVORED_MEAT)));



    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\

    // TEMP -- GeckoLib pipeline validation only, remove once TestRigItem is confirmed rendering/animating.
    public static final DeferredItem<Item> TEST_RIG = ITEMS.register("test_rig", () -> new TestRigItem(new Item.Properties()));
}
