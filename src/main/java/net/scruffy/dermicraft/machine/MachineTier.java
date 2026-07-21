package net.scruffy.dermicraft.machine;

import net.neoforged.neoforge.fluids.FluidType;

/**
 * The tunable, stat-only knobs that distinguish one member of a machine family from the next
 * (e.g. a Masticator Mk.I from a Mk.II). A block carries a MachineTier (see {@link TieredMachine})
 * and its block entity reads its numbers from it, so a purely-numeric upgrade tier needs no new
 * block-entity code at all -- just a new block registration and a new MachineTier constant here.
 *
 * <p>Anything that is a genuine <em>capability leap</em> (an extra tank, a new processing rule,
 * a different IO layout) is NOT a field here -- that is done by overriding a hook on the family's
 * abstract block entity, which is the deliberate "power is earned via capability leaps" split.
 *
 * @param tankCapacity     per-tank fluid capacity, in mB
 * @param maxHealth        starting/maximum machine health (0 disables the health mechanic)
 * @param speedMultiplier  multiplies the fuel's own crafting-speed multiplier (1.0 = unchanged)
 * @param healMultiplier   multiplies the fuel's own heal rate (1.0 = unchanged)
 */
public record MachineTier(int tankCapacity, int maxHealth, float speedMultiplier, float healMultiplier) {

    /** The baseline every current progenitor machine uses. Matches the old hard-coded values. */
    public static final MachineTier BASIC = new MachineTier(FluidType.BUCKET_VOLUME * 5, 200, 1.0f, 1.0f);

    /** No HP mechanic at all -- the machine simply doesn't process without fuel, rather than limping
     * along at reduced speed while taking damage. Used by the minor convenience machines (Render
     * Furnace, Grafting Table) that deliberately skip the health system entirely. */
    public static final MachineTier NO_HEALTH = new MachineTier(FluidType.BUCKET_VOLUME * 5, 0, 1.0f, 1.0f);

    // To add a stat-only upgrade tier later, declare it here and hand it to a new block, e.g.:
    // public static final MachineTier ADVANCED = new MachineTier(FluidType.BUCKET_VOLUME * 10, 400, 1.5f, 1.5f);
}
