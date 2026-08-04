package net.scruffy.dermicraft.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.property.ChainProperties;

import java.util.List;

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
     *
     * <p>Guaranteed, not rolled, if the weapon's mode is still SAWING at the moment this fires --
     * safe to check directly rather than needing a separate signal, since {@code inventoryTick}'s
     * own SAWING-exit only runs on a later tick, not synchronously inside the {@code target.hurt()}
     * call that triggers this event, so the stack still reads SAWING for a genuine dig-in kill.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof SunderItem)) return;

        ChainProperties chain = SunderItem.chainProperties(weapon);
        if (chain == null) return;

        LivingEntity target = event.getEntity();
        SunderModeData mode = weapon.getOrDefault(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT);
        boolean guaranteed = mode.stateEnum() == SunderModeData.State.SAWING;
        if (!guaranteed && target.getRandom().nextFloat() >= chain.decapChance()) return;

        Item head = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).getData(ModDataMaps.DECAPITATION_HEADS);
        if (head == null) return;

        event.getDrops().add(new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(), new ItemStack(head)));
    }

    /**
     * Gold's signature trait -- a "weak Fortune/Looting" bonus, see {@link ChainProperties}' own
     * javadoc for why this isn't trying to reuse vanilla's real enchantment mechanic. Separate
     * {@code @SubscribeEvent} method, not folded into {@link #onLivingDrops} above -- rolls against
     * a snapshot of the drops list (via {@code List.copyOf}) rather than the live one, since adding
     * to a collection while iterating it throws {@code ConcurrentModificationException}; this also
     * means it naturally sees decapitation's own head drop too if that handler ran first (both are
     * registered on the same event, order not guaranteed either way, but harmless regardless of
     * which fires first).
     */
    @SubscribeEvent
    public static void onLivingDropsLootBonus(LivingDropsEvent event) {
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof SunderItem)) return;

        ChainProperties chain = SunderItem.chainProperties(weapon);
        if (chain == null || chain.lootBonusChance() <= 0.0f) return;

        LivingEntity target = event.getEntity();
        for (ItemEntity drop : List.copyOf(event.getDrops())) {
            if (target.getRandom().nextFloat() < chain.lootBonusChance()) {
                event.getDrops().add(new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(), drop.getItem().copy()));
            }
        }
    }

    /**
     * SAWING's interruption trigger -- knockback dealt TO THE PLAYER, not knockback dealt to the
     * target and not damage taken (a hit that doesn't impart knockback shouldn't cancel it). Checks
     * both hands since the player could be holding Sunder in either. Branches on which of {@code
     * target}/{@code treeOrigin} is present -- see {@code SunderModeData}'s own javadoc.
     */
    @SubscribeEvent
    public static void onPlayerKnockedBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        ServerLevel level = (ServerLevel) player.level();

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof SunderItem sunder)) continue;

            SunderModeData mode = stack.getOrDefault(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT);
            if (mode.stateEnum() != SunderModeData.State.SAWING) continue;

            long now = level.getGameTime();
            long elapsed = now - mode.since();
            boolean holdingTrigger = player.isUsingItem() && player.getUseItem() == stack;

            if (mode.target().isPresent()) {
                Entity resolved = mode.target().map(level::getEntity).orElse(null);
                LivingEntity target = resolved instanceof LivingEntity living && living.isAlive() ? living : null;
                sunder.endSawing(stack, level, player, target, elapsed, holdingTrigger, now);
            } else {
                // No partial-harvest payout needed here (unlike mob SAWING's Bleed-guarantee check)
                // -- tree felling hands out logs as they're cut, not batched for the end, so whatever
                // was already cut is already in the player's hands. Just the shared exit tail.
                sunder.exitFellingState(stack, level, player, holdingTrigger, now);
            }
        }
    }
}
