package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Marks an item as a <b>Gadget</b> -- a complex, quasi-living handheld machine, as opposed to a
 * simple Tool. Gadgets get a regenerating health pool in place of ordinary durability: they take
 * damage from being thrown around, heal while carried, and eventually die rather than merely
 * breaking.
 *
 * <p>The health pool <em>is</em> vanilla's durability bar, so there's no custom UI and no second
 * number to keep in sync -- {@code Item.Properties#durability} sets max HP and is the single source
 * of truth. Everything else is driven from {@code net.scruffy.dermicraft.event.GadgetEvents}, so
 * adding this interface to an item is all that's needed to opt in.
 *
 * <p>Not applied to simple Tools (I.D.E.P., Scalpel, and so on) -- the distinction is deliberate,
 * and gadget-tier treatment is meant to be earned by an item's complexity, not handed out by
 * default.
 */
public interface IGadget {

    /** Max HP is whatever durability the item registered with -- see the class javadoc. */
    default int maxHp(ItemStack stack) {
        return stack.getMaxDamage();
    }

    /**
     * Fired once, on the impact that kills the gadget, just before its entity is removed. Contents
     * die with it: discarding the entity takes the stack and every component on it, so nothing
     * needs to explicitly void the fluid inside.
     *
     * <p><b>Every gadget is expected to override this with its own send-off.</b> The default here is
     * a deliberately plain fallback so a new gadget is never silent -- it is not meant to become any
     * particular gadget's identity by accident.
     */
    default void onGadgetDeath(ServerLevel level, ItemEntity entity, ItemStack stack) {
        deathFlourish(level, entity, ParticleTypes.SMOKE, 14, 0.2,
                SoundEvents.GHAST_HURT, 0.8F, 1.0F);
    }

    /**
     * Shared shape of a death: a burst of particles and one cry. Keeps each gadget's override down
     * to its own choice of particle and pitch instead of repeated spawn boilerplate.
     *
     * <p>PLACEHOLDER SOUNDS throughout: these are pitched vanilla cries, not bespoke audio.
     * Registering custom {@code SoundEvent}s with no {@code .ogg} behind them would log an error on
     * every death, so the real screams are a one-line swap per gadget once the files exist.
     */
    static void deathFlourish(ServerLevel level, ItemEntity entity, ParticleOptions particle, int count,
                              double spread, SoundEvent sound, float volume, float pitch) {
        level.sendParticles(particle, entity.getX(), entity.getY() + 0.2, entity.getZ(),
                count, spread, spread, spread, 0.02);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }
}
