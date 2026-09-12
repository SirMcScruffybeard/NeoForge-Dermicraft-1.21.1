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
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.property.ChainProperties;
import net.scruffy.dermicraft.util.AutoSmeltUtil;

import java.util.List;
import java.util.Optional;

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
     * Knowledge's signature trait -- a per-kill chance to spawn a bonus XP orb at the target, on top
     * of whatever the kill already awards. Mirrors {@link #onLivingDropsLootBonus}'s exact shape
     * (same event, same "roll once per kill" cadence) but grants XP instead of duplicating a drop --
     * see {@link ChainProperties}' own javadoc. Reuses {@link AutoSmeltUtil#awardExperience} (Blaze
     * Essence's own XP-granting helper) rather than a bespoke orb spawn, same fractional-remainder
     * rounding.
     */
    @SubscribeEvent
    public static void onLivingDropsXpBonus(LivingDropsEvent event) {
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof SunderItem)) return;

        ChainProperties chain = SunderItem.chainProperties(weapon);
        if (chain == null || chain.xpBonusChance() <= 0.0f) return;

        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        if (target.getRandom().nextFloat() < chain.xpBonusChance()) {
            net.scruffy.dermicraft.util.AutoSmeltUtil.awardExperience(serverLevel, target.position(), chain.xpBonusAmount());
        }
    }

    /**
     * Normal (non-SAWING) mining's own auto-smelt -- every drop from a block Sunder mines the usual
     * way (left-click, same as any axe/sword) gets substituted for its real {@link
     * net.minecraft.world.item.crafting.SmeltingRecipe} result, XP included, same universal "any
     * block, not restricted to logs/ore" framing as {@link ShatterEvents#onBlockDropsAutoSmelt}.
     * SAWING felling has its own separate substitution ({@code SunderItem#tickFelling}, driven
     * directly rather than through this event) since it hands out drops as it cuts, not via a real
     * block-break -- this only covers the ordinary path. See {@code ShatterEvents
     * #onBlockDropsAutoSmelt} for the near-identical Shatter version. Gated on either a Blaze Essence
     * chain ({@code smeltsLogs}, now covering normal mining too, not just felling) -- free, as always
     * -- or the Smelting Module (see {@code ModTags.Items#MODULE_SMELTING}), which costs {@link
     * AutoSmeltUtil#SMELTING_MODULE_FUEL_PER_ITEM} mB/item from Sunder's own fuel tank (see {@link
     * AutoSmeltUtil#affordableSmeltCount}) -- a genuinely new fuel cost on ordinary mining, which
     * previously never touched the tank at all, but only when the Module (not the chain) is what's
     * actually providing the trait. Whatever the Module's fuel can't cover falls back to that item's
     * raw drop rather than blocking the whole break -- iterates a {@link List#copyOf} snapshot since
     * a partial-fuel drop spawns an extra {@link ItemEntity} for the raw leftover, same "copy first,
     * mutate the live list after" convention {@link #onLivingDropsLootBonus} already uses. Having both
     * a smelting chain AND the Module installed is harmless (chain's free path always wins first, no
     * fuel ever spent) since it only ever spends fuel to redo a smelt {@code freeSmelt} already gave
     * away.
     */
    @SubscribeEvent
    public static void onBlockDropsAutoSmelt(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!(tool.getItem() instanceof SunderItem)) return;

        ChainProperties chain = SunderItem.chainProperties(tool);
        boolean freeSmelt = chain != null && chain.smeltsLogs();
        boolean moduleSmelt = SunderItem.hasModule(tool, ModTags.Items.MODULE_SMELTING);
        if (!freeSmelt && !moduleSmelt) return;

        ServerLevel level = event.getLevel();
        float totalXp = 0f;
        for (ItemEntity drop : List.copyOf(event.getDrops())) {
            ItemStack original = drop.getItem();
            Optional<AutoSmeltUtil.SmeltResult> smelted = AutoSmeltUtil.smeltOne(level, original);
            if (smelted.isEmpty()) continue;

            AutoSmeltUtil.SmeltResult result = smelted.get();
            int smeltCount = original.getCount();
            if (!freeSmelt) {
                smeltCount = AutoSmeltUtil.affordableSmeltCount(
                        tool, AutoSmeltUtil.SMELTING_MODULE_FUEL_PER_ITEM, original.getCount());
                if (smeltCount <= 0) continue;
            }

            drop.setItem(result.result().copyWithCount(result.result().getCount() * smeltCount));
            totalXp += result.experience() * smeltCount;

            int rawRemainder = original.getCount() - smeltCount;
            if (rawRemainder > 0) {
                event.getDrops().add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(),
                        original.copyWithCount(rawRemainder)));
            }
        }

        if (totalXp > 0) {
            AutoSmeltUtil.awardExperience(level, event.getPos(), totalXp);
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
