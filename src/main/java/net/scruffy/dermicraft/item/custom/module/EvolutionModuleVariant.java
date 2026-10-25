package net.scruffy.dermicraft.item.custom.module;

import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.EvolutionModuleData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.item.ModItems;

import javax.annotation.Nullable;

/**
 * Every look an Evolution Module can have: one model (frame plus an icon layer) per variant. The list is
 * the single source for the client item property, the generated model overrides and the creative tab, so
 * they cannot disagree. A bare module, or a component that matches no variant, is not listed and keeps the
 * plain frame model. Overgrowth has one shared icon whatever its level.
 */
public enum EvolutionModuleVariant {
    TIER_2("tier_2", "l2", EvolutionModuleData.tier(2)),
    TIER_3("tier_3", "l3", EvolutionModuleData.tier(3)),
    TIER_4("tier_4", "l4", EvolutionModuleData.tier(4)),
    THERMAL("thermal", "thermal", EvolutionModuleData.hazard(HazardFamily.THERMAL, 1)),
    RADIATION_MILD("radiation_mild", "radiation_mild", EvolutionModuleData.hazard(HazardFamily.RADIATION, 1)),
    RADIATION_SEVERE("radiation_severe", "radiation_severe", EvolutionModuleData.hazard(HazardFamily.RADIATION, 2)),
    BIOHAZARD("biohazard", "biohazard", EvolutionModuleData.hazard(HazardFamily.BIOHAZARD, 1)),
    METAPHYSICAL_MILD("metaphysical_mild", "metaphysical_mild", EvolutionModuleData.hazard(HazardFamily.METAPHYSICAL, 1)),
    METAPHYSICAL_SEVERE("metaphysical_severe", "metaphysical_severe", EvolutionModuleData.hazard(HazardFamily.METAPHYSICAL, 2)),
    OVERGROWTH("overgrowth", "o", EvolutionModuleData.overgrowth(1));

    private final String NAME;
    private final String ICON;
    private final EvolutionModuleData SAMPLE;

    EvolutionModuleVariant(String name, String icon, EvolutionModuleData sample) {
        NAME = name;
        ICON = icon;
        SAMPLE = sample;
    }

    /** The generated model's name, {@code evolution_module_<name>}. */
    public String getModelName() {
        return "evolution_module_" + NAME;
    }

    /** The icon texture's name under {@code item/module/icons/}. */
    public String getIconName() {
        return ICON;
    }

    /** The value of the client "variant" item property that selects this variant's model. Listed in
     * ascending order because item overrides pick the last entry whose value is at or below the property. */
    public int getPredicateValue() {
        return ordinal() + 1;
    }

    /** A stack of this variant, for the creative tab and JEI. Overgrowth is level 1. */
    public ItemStack createStack() {
        ItemStack stack = new ItemStack(ModItems.EVOLUTION_MODULE.get());
        stack.set(ModDataComponentTypes.EVOLUTION_MODULE_DATA, SAMPLE);
        return stack;
    }

    private boolean matches(EvolutionModuleData data) {
        if (SAMPLE.type() == EvolutionModuleData.Type.OVERGROWTH) {
            return data.type() == EvolutionModuleData.Type.OVERGROWTH;
        }
        return SAMPLE.equals(data);
    }

    /** The item property value for a stack's component: 0 (the plain frame) for none or no match. */
    public static int getPredicateValue(@Nullable EvolutionModuleData data) {
        if (data == null) return 0;
        for (EvolutionModuleVariant variant : values()) {
            if (variant.matches(data)) return variant.getPredicateValue();
        }
        return 0;
    }

}
