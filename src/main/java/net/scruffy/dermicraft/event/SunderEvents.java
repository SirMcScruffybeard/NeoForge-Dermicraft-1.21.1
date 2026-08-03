package net.scruffy.dermicraft.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.property.ChainProperties;

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

    /**
     * Decapitation-on-kill -- rolled here rather than in {@code SunderItem#hurtEnemy} since it only
     * matters on the killing blow, not every hit. A missing/broken chain rolls a flat 0% for free
     * ({@link SunderItem#chainProperties} returns {@code null} in that case). Scoped to mobs with a
     * real vanilla head item already -- see {@link ModDataMaps#DECAPITATION_HEADS}.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof SunderItem)) return;

        ChainProperties chain = SunderItem.chainProperties(weapon);
        if (chain == null) return;

        LivingEntity target = event.getEntity();
        if (target.getRandom().nextFloat() >= chain.decapChance()) return;

        Item head = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).getData(ModDataMaps.DECAPITATION_HEADS);
        if (head == null) return;

        event.getDrops().add(new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(), new ItemStack(head)));
    }
}
