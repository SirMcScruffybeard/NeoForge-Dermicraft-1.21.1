package net.scruffy.dermicraft.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.LevelModuleData;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.module.HazardModuleVariant;
import net.scruffy.dermicraft.item.custom.module.ModuleFamilies;
import net.scruffy.dermicraft.item.custom.module.ModuleVariant;
import net.scruffy.dermicraft.machine.EvolutionStats;

/**
 * The JEI description pages for the Evolution Modules: what each one does, what a machine needs for it to apply, and how
 * long it takes. The numbers are read from the stat config when JEI registers (it reloads when a player joins a world,
 * so a server's synced values show), so a retuned config changes the pages without touching the text.
 */
public final class EvolutionInfo {

    private EvolutionInfo() {}

    public static void register(IRecipeRegistration registration) {
        registration.addIngredientInfo(new ItemStack(ModItems.EVOLUTION_MODULE_FRAME.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.dermicraft.evolution.frame"));

        for (ModuleVariant<LevelModuleData> variant : ModuleFamilies.TIER.getVariants()) {
            int tier = variant.data().level();
            if (tier < 2) continue; // there is no Tier 1 module to obtain
            registration.addIngredientInfo(ModuleFamilies.TIER.createStack(variant), VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.dermicraft.evolution.tier.raise", tier),
                    Component.translatable("jei.dermicraft.evolution.tier.rule", tier - 1),
                    Component.translatable("jei.dermicraft.evolution.timing",
                            getDuration(EvolutionStats.getTierTicks(null, tier)),
                            getDuration(EvolutionStats.getCountdownTicks())),
                    Component.translatable("jei.dermicraft.evolution.tier.effect"));
        }

        for (ModuleVariant<LevelModuleData> variant : ModuleFamilies.OVERGROWTH.getVariants()) {
            registration.addIngredientInfo(ModuleFamilies.OVERGROWTH.createStack(variant), VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.dermicraft.evolution.overgrowth.raise", EvolutionStats.getOvergrowthMaxLevel()),
                    Component.translatable("jei.dermicraft.evolution.overgrowth.time",
                            getDuration(EvolutionStats.getOvergrowthTicks(null, 1)),
                            getDuration(EvolutionStats.getOvergrowthTicks(null, 2))),
                    Component.translatable("jei.dermicraft.evolution.overgrowth.storage",
                            getDuration(EvolutionStats.getCountdownTicks())),
                    Component.translatable("jei.dermicraft.evolution.overgrowth.effect",
                            getNumber(EvolutionStats.getOvergrowthStatPercent())));
        }

        for (HazardModuleVariant variant : HazardModuleVariant.values()) {
            registerHazard(registration, variant);
        }
    }

    private static void registerHazard(IRecipeRegistration registration, HazardModuleVariant variant) {
        HazardFamily family = variant.getData().family();
        boolean twoLevels = family.getMaxLevel() > 1;
        boolean severe = twoLevels && variant.getData().level() == family.getMaxLevel();
        Component name = Component.translatable(variant.getTranslationKey());

        java.util.List<Component> paragraphs = new java.util.ArrayList<>();
        paragraphs.add(Component.translatable("jei.dermicraft.evolution.hazard.grant", name));
        paragraphs.add(Component.translatable("jei.dermicraft.evolution.hazard.slot"));
        paragraphs.add(Component.translatable("jei.dermicraft.evolution.hazard.permanent"));
        if (severe) {
            paragraphs.add(Component.translatable("jei.dermicraft.evolution.hazard.severe"));
        } else if (twoLevels) {
            paragraphs.add(Component.translatable("jei.dermicraft.evolution.hazard.mild"));
        }
        paragraphs.add(twoLevels
                ? Component.translatable("jei.dermicraft.evolution.hazard.time_two_levels",
                        getDuration(EvolutionStats.getHazardNewTicks(null)),
                        getDuration(EvolutionStats.getHazardUpgradeTicks(null)))
                : Component.translatable("jei.dermicraft.evolution.hazard.time",
                        getDuration(EvolutionStats.getHazardNewTicks(null))));
        paragraphs.add(Component.translatable("jei.dermicraft.evolution.hazard.storage"));

        registration.addIngredientInfo(new ItemStack(ModItems.getHazardEvolutionModule(variant)), VanillaTypes.ITEM_STACK,
                paragraphs.toArray(new Component[0]));
    }

    /** A time in the most readable unit: seconds under a minute, minutes under an hour, otherwise hours. */
    private static Component getDuration(int ticks) {
        double seconds = ticks / 20.0;
        if (seconds < 60) return Component.translatable("jei.dermicraft.evolution.seconds", getNumber(seconds));
        double minutes = seconds / 60.0;
        if (minutes < 60) return Component.translatable("jei.dermicraft.evolution.minutes", getNumber(minutes));
        return Component.translatable("jei.dermicraft.evolution.hours", getNumber(minutes / 60.0));
    }

    /** A number without a trailing ".0". */
    private static String getNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }
}
