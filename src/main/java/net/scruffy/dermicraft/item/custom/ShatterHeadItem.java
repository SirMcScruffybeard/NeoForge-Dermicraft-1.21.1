package net.scruffy.dermicraft.item.custom;

/**
 * A spare head material for Shatter, standalone/unmounted form -- see {@code SunderChainItem} for
 * the equivalent Sunder concept this mirrors. One class shared across materials; per-material data
 * (currently just tint) lives in {@code ModDataMaps.SHATTER_HEAD_PROPERTIES} (see
 * {@code ShatterHeadProperties}), keyed on the item itself.
 *
 * <p>No stat tooltip yet, unlike {@code SunderChainItem} -- Shatter's per-head special/mining-tier
 * design isn't decided (see the Shatter design notes), so there's nothing real to show yet. Add one
 * once those stats exist.
 */
public class ShatterHeadItem extends net.minecraft.world.item.Item {
    public ShatterHeadItem(Properties properties) {
        super(properties);
    }
}
