package net.scruffy.dermicraft.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Drives the shared {@link IGadget} health mechanic: impact damage from being thrown away, slow
 * regeneration while carried, and death.
 *
 * <p>Lives entirely in events rather than per-item overrides, so an item opts in purely by
 * implementing {@link IGadget}.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class GadgetEvents {

    /** Marks an item entity as having been thrown by a player, so only deliberate discards hurt --
     * a gadget spilled by a broken container or spawned by a command shouldn't be penalised. */
    private static final String TOSSED_TAG = "dermicraft_gadget_tossed";

    /** One HP per minute. */
    private static final int REGEN_INTERVAL_TICKS = 1200;

    /**
     * Escalating impact damage, following 1, 1, 2, 3, 5, 8...
     *
     * <p>Stored as the running totals rather than the terms themselves, because the term to apply is
     * derived from how battered the gadget already is: find the first total the current damage
     * hasn't reached, and that position gives the next hit's severity. Deriving it this way means no
     * separate hit counter to persist and, more importantly, that <b>healing genuinely restores
     * resilience</b> -- a rested gadget takes small hits again instead of continuing where it left
     * off, which is the point of it being alive rather than merely worn.
     *
     * <p>Consequence worth knowing: from full health the totals run 1, 2, 4, 7, 12 -- so a 10 HP
     * gadget dies on its fifth consecutive drop. Lengthen this array or raise the durability to
     * soften that.
     */
    private static final int[] DAMAGE_TOTALS = {1, 2, 4, 7, 12, 20, 33};
    private static final int[] DAMAGE_TERMS = {1, 1, 2, 3, 5, 8, 13};

    @SubscribeEvent
    public static void onGadgetTossed(ItemTossEvent event) {
        ItemEntity entity = event.getEntity();
        if (entity.getItem().getItem() instanceof IGadget) {
            entity.getPersistentData().putBoolean(TOSSED_TAG, true);
        }
    }

    /** Damage lands on impact, not on the throw -- the death flourish reads far better as something
     * that happens where the gadget hits the ground than as it leaving the player's hand. */
    @SubscribeEvent
    public static void onItemEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity entity)) return;
        if (entity.level().isClientSide || !entity.onGround()) return;
        if (!entity.getPersistentData().getBoolean(TOSSED_TAG)) return;

        entity.getPersistentData().remove(TOSSED_TAG);
        applyImpact(entity);
    }

    private static void applyImpact(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (!(stack.getItem() instanceof IGadget gadget)) return;

        int maxHp = gadget.maxHp(stack);
        if (maxHp <= 0) return;

        int damaged = stack.getDamageValue() + nextImpactDamage(stack.getDamageValue());
        if (damaged >= maxHp) {
            if (entity.level() instanceof ServerLevel serverLevel) {
                gadget.onGadgetDeath(serverLevel, entity, stack);
            }
            // Contents die with it -- discarding takes the stack and everything on it.
            entity.discard();
            return;
        }

        stack.setDamageValue(damaged);
        entity.setItem(stack);
    }

    /** See {@link #DAMAGE_TOTALS} for why this reads off current damage rather than a hit counter. */
    private static int nextImpactDamage(int currentDamage) {
        for (int i = 0; i < DAMAGE_TOTALS.length; i++) {
            if (currentDamage < DAMAGE_TOTALS[i]) return DAMAGE_TERMS[i];
        }
        return DAMAGE_TERMS[DAMAGE_TERMS.length - 1];
    }

    /**
     * Heals carried gadgets, one HP a minute.
     *
     * <p>Gated on the world clock before touching the inventory, so this is a scan once per minute
     * rather than every tick. Deliberately keyed off {@code inventory} -- a gadget left in a chest
     * or lying on the ground doesn't recover, which is what ties healing to being carried.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.level().getGameTime() % REGEN_INTERVAL_TICKS != 0) return;

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof IGadget && stack.isDamaged()) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }
}
