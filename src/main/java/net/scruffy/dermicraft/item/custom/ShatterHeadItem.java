package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.property.ShatterHeadProperties;

import java.util.List;

/**
 * A spare head material for Shatter, standalone/unmounted form -- see {@code SunderChainItem} for
 * the equivalent Sunder concept this mirrors. One class shared across materials; per-material data
 * lives in {@code ModDataMaps.SHATTER_HEAD_PROPERTIES} (see {@link ShatterHeadProperties}), keyed
 * on the item itself.
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

    /** Shift-gated stat readout -- mining tier/speed/damage shift always present, the rest only
     * shown if this material actually has that trait (0/false for every material without it). Same
     * shape as {@code SunderChainItem#appendHoverText}. Damage is shown as the raw flat shift
     * (Iron's own baseline is 0), not an effective total -- unlike the mounted Shatter gadget's own
     * tooltip, this item has no {@code BASE_ATTACK_DAMAGE} context to compute one against. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        ShatterHeadProperties head = BuiltInRegistries.ITEM.wrapAsHolder(this).getData(ModDataMaps.SHATTER_HEAD_PROPERTIES);
        if (head == null) return;

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.dermicraft.hold_shift_for_stats"));
            return;
        }

        tooltip.add(Component.translatable("tooltip.dermicraft.shatter_head.mining_tier", head.miningTier())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dermicraft.shatter_head.mining_speed", String.format("%.1f", head.miningSpeed()))
                .withStyle(ChatFormatting.GRAY));

        String shiftSign = head.damageShift() >= 0 ? "+" : "";
        ChatFormatting damageColor = head.damageShift() > 0 ? ChatFormatting.GREEN
                : head.damageShift() < 0 ? ChatFormatting.RED : ChatFormatting.GRAY;
        tooltip.add(Component.translatable("tooltip.dermicraft.shatter_head.damage", shiftSign + String.format("%.1f", head.damageShift()))
                .withStyle(damageColor));

        // Gold, not gray -- these only ever show up conditionally (this material actually has the
        // trait), so the color itself flags "this is the special thing about this material" at a
        // glance, distinct from the baseline stats above.
        if (head.lootBonusChance() > 0.0f) {
            tooltip.add(Component.translatable("tooltip.dermicraft.shatter_head.loot_bonus", Math.round(head.lootBonusChance() * 100))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (head.igniteChance() > 0.0f) {
            tooltip.add(Component.translatable("tooltip.dermicraft.shatter_head.ignite",
                            Math.round(head.igniteChance() * 100), head.igniteFireSeconds())
                    .withStyle(ChatFormatting.GOLD));
        }
        if (head.autoSmelt()) {
            tooltip.add(Component.translatable("tooltip.dermicraft.shatter_head.auto_smelt")
                    .withStyle(ChatFormatting.GOLD));
        }
        if (head.xpBonusChance() > 0.0f) {
            tooltip.add(Component.translatable("tooltip.dermicraft.shatter_head.xp_bonus",
                            Math.round(head.xpBonusChance() * 100), String.format("%.0f", head.xpBonusAmount()))
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
