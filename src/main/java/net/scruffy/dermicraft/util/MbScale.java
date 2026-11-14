package net.scruffy.dermicraft.util;

/**
 * The millibucket scale shared by the material fluids (the Pulps and the molten fluids): one ingot is {@value #INGOT} mB,
 * one nugget {@value #NUGGET} mB, and one storage block {@value #BLOCK} mB. Recipes build their amounts from these
 * instead of writing the numbers, so a rescale is a change in one place.
 *
 * <p>Rules every amount follows (decided in the rescale overhaul, see {@code dermicraft-rescale-overhaul-plan.md}):
 * <ul>
 *   <li>Rounding is always up, for costs and for yields alike. {@link #roundUp} does it. The small loop this allows (the
 *   Clay Ball and Block cycle gains 2 mB) is accepted and deliberately not guarded.</li>
 *   <li>An item with a storage block: the item is {@value #INGOT} and the block is 9:1 ({@value #BLOCK}) or 4:1
 *   ({@value #BLOCK_OF_FOUR}).</li>
 *   <li>A plain block has no flat rate; it is priced per family from the units it is made of.</li>
 *   <li>A slab is half the block it is made from. Stairs equal the block they are made from (the stonecutter rule).</li>
 *   <li>A crafted functional block is priced by the ingots in its crafting recipe (Hopper 450, Cauldron 630), not as a
 *   storage block.</li>
 * </ul>
 */
public final class MbScale {

    /** One ingot (and any 1:1 item such as a gem, a dust or a shard that has a storage block). */
    public static final int INGOT = 90;
    /** One nugget, a ninth of an ingot. */
    public static final int NUGGET = 10;
    /** A 9:1 storage block (iron, gold, copper, coal, diamond and the like). */
    public static final int BLOCK = 810;
    /** A 4:1 storage block (quartz, glowstone, amethyst, and the stone family's four-unit blocks). */
    public static final int BLOCK_OF_FOUR = 360;

    private MbScale() {}

    /** {@code ingots} ingots' worth, for a crafted item priced by its recipe (Bucket 3 ingots = 270). */
    public static int ofIngots(int ingots) {
        return ingots * INGOT;
    }

    /** {@code value} rounded up to the next whole millibucket, the rounding rule for costs and yields. */
    public static int roundUp(double value) {
        return (int) Math.ceil(value - 1.0E-9);
    }

    /** {@code ingots} ingots' worth for a recipe whose crafting yields several items, shared out and rounded up
     * (Iron Bars: 6 ingots make 16 bars, so {@code ofIngotsShared(6, 16)} is 34). */
    public static int ofIngotsShared(int ingots, int items) {
        return roundUp((double) ingots * INGOT / items);
    }
}
