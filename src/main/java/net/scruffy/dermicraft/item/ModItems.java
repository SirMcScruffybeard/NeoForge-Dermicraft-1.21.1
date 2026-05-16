package net.scruffy.dermicraft.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.item.custom.base.PartItem;
import net.scruffy.dermicraft.item.custom.ScalpelItem;
import net.scruffy.dermicraft.item.custom.SutureKitItem;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Dermicraft.MOD_ID);
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


    ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> SCALPEL = ITEMS.register("scalpel",
            () -> new ScalpelItem(new Item.Properties()));

    public static final DeferredItem<Item> SUTURE_KIT = ITEMS.register("suture_kit",
            () -> new SutureKitItem(new Item.Properties()));

    public static final DeferredItem<Item> FORCEPS = ITEMS.register("forceps", ForcepsItem::new);




    ////////////////////Parts\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> EYE = ITEMS.register("eye",
            () -> new PartItem(new Item.Properties()));

    public static  DeferredItem<Item> NERVE_CLUSTER = ITEMS.register("nerve_cluster",
            () -> new PartItem(new Item.Properties()));

    public static DeferredItem<Item> DENSE_MUSCLE = ITEMS.register("dense_muscle",
            () -> new PartItem(new Item.Properties()));





    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\

}
