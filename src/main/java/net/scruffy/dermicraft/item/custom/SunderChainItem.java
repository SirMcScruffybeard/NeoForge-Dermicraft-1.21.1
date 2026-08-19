package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.property.ChainProperties;

import java.util.List;

/**
 * A spare chain material for Sunder, standalone/unmounted form -- plain vanilla durability (a
 * spare wearing out and being consumed at 0 is normal tool behavior, unlike the mounted-on-Sunder
 * representation, which can't use vanilla durability -- see the Chain durability design notes in
 * {@code dermicraft-gadget-notes.md}).
 *
 * <p>One class shared across materials -- per-material stats live in {@code ModDataMaps.
 * SUNDER_CHAIN_PROPERTIES} (see {@link ChainProperties}), keyed on the item itself, same lookup
 * shape {@code SunderItem#chainProperties} uses for a mounted chain.
 */
public class SunderChainItem extends Item {
    public SunderChainItem(Properties properties) {
        super(properties);
    }

    /** Current Mutator-upgrade tier (0 = base/unupgraded) -- see
     * {@code project_shatter_head_upgrade_design} design notes; same shared {@code PART_UPGRADE_TIER}
     * component {@code ShatterHeadItem} uses. Missing component reads as 0. */
    public static int getUpgradeTier(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.PART_UPGRADE_TIER.get(), 0);
    }

    /** Appends " +N" for an upgraded chain (e.g. "Iron Sunder Chain +1") -- same convention as
     * {@code ShatterHeadItem#getName}. */
    @Override
    public Component getName(ItemStack stack) {
        int tier = getUpgradeTier(stack);
        Component base = super.getName(stack);
        return tier <= 0 ? base : base.copy().append(" +" + tier);
    }

    /** Shift-gated stat readout -- damage/Bleed/decap always present, bonus loot chance only shown
     * if the material actually has one (0 for everything but Gold right now). Damage is shown as a
     * +/- percent relative to Iron's baseline, matching how {@code SunderItem#getDefaultAttributeModifiers}
     * itself computes the shift ({@code damageMultiplier - 1.0}), not the raw multiplier. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        ChainProperties chain = BuiltInRegistries.ITEM.wrapAsHolder(this).getData(ModDataMaps.SUNDER_CHAIN_PROPERTIES);
        if (chain == null) return;

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.dermicraft.hold_shift_for_stats"));
            return;
        }

        int damagePercent = Math.round((chain.damageMultiplier() - 1.0f) * 100);
        String damageSign = damagePercent >= 0 ? "+" : "";
        tooltip.add(Component.translatable("tooltip.dermicraft.sunder_chain.damage", damageSign + damagePercent)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dermicraft.sunder_chain.bleed_chance", Math.round(chain.bleedChance() * 100))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dermicraft.sunder_chain.decap_chance", Math.round(chain.decapChance() * 100))
                .withStyle(ChatFormatting.GRAY));
        if (chain.lootBonusChance() > 0.0f) {
            tooltip.add(Component.translatable("tooltip.dermicraft.sunder_chain.loot_bonus", Math.round(chain.lootBonusChance() * 100))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
