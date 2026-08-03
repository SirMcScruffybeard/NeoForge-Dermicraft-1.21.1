package net.scruffy.dermicraft.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Hard-resets Sunder's rev state on toss -- without this, a dropped Sunder freezes wherever its
 * state machine happened to be (e.g. mid-revved) forever, since {@code inventoryTick} only runs
 * for stacks sitting in a container/inventory, never for a standalone {@link ItemEntity} lying on
 * the ground. The natural self-heal (holdingTrigger going false once no longer held) only fires
 * once the item is back in an inventory ticking again, which doesn't help while it's on the ground.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class SunderEvents {

    @SubscribeEvent
    public static void onSunderTossed(ItemTossEvent event) {
        ItemEntity entity = event.getEntity();
        ItemStack stack = entity.getItem();
        if (!(stack.getItem() instanceof SunderItem)) return;

        stack.set(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT);
        entity.setItem(stack);
    }
}
