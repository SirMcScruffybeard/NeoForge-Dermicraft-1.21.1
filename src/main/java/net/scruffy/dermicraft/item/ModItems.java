package net.scruffy.dermicraft.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.item.custom.ForcepsItem;
import net.scruffy.dermicraft.item.custom.SyringeItem;
import net.scruffy.dermicraft.item.custom.base.PartItem;
import net.scruffy.dermicraft.item.custom.ScalpelItem;
import net.scruffy.dermicraft.item.custom.SutureKitItem;
import net.scruffy.dermicraft.item.property.ModFoodProperties;
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


    ////////////////////Parts\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> EYE = ITEMS.register("eye",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.EYE)));

    public static  DeferredItem<Item> NERVE_CLUSTER = ITEMS.register("nerve_cluster",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.NERVE_CLUSTER)));

    public static DeferredItem<Item> DENSE_MUSCLE = ITEMS.register("dense_muscle",
            () -> new PartItem(new Item.Properties().food(ModFoodProperties.DENSE_MUSCLE)));





    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\

}
