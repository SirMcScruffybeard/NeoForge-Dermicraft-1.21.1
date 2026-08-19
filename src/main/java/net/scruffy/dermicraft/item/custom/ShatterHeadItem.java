package net.scruffy.dermicraft.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.ModDataComponentTypes;

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

    /** Current Mutator-upgrade tier (0 = base/unupgraded) -- see
     * {@code project_shatter_head_upgrade_design} design notes. Missing component reads as 0 rather
     * than every head needing an explicit zero written on creation. */
    public static int getUpgradeTier(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.PART_UPGRADE_TIER.get(), 0);
    }

    /** Appends " +N" for an upgraded head (e.g. "Bone Shatter Head +1") -- the tier is baked into
     * the display name itself, not just a tooltip line, per direct instruction. */
    @Override
    public Component getName(ItemStack stack) {
        int tier = getUpgradeTier(stack);
        Component base = super.getName(stack);
        return tier <= 0 ? base : base.copy().append(" +" + tier);
    }
}
