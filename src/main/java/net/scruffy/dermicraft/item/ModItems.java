package net.scruffy.dermicraft.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.entity.custom.tumor.InertTumorBlock;
import net.scruffy.dermicraft.item.custom.PartItem;
import net.scruffy.dermicraft.item.custom.ScalpelItem;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Dermicraft.MOD_ID);
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


    ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> SCALPEL = ITEMS.register("scalpel",
            () -> new ScalpelItem(new Item.Properties()));




    ////////////////////Parts\\\\\\\\\\\\\\\\\\\\
    public static final DeferredItem<Item> EYE = ITEMS.register("eye",
            () -> new PartItem(new Item.Properties()));





    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\

}
