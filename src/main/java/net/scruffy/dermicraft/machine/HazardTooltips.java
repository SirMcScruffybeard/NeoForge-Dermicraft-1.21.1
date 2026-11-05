package net.scruffy.dermicraft.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.hazard.HazardFamily;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Hazard:" lines added to a fluid's tooltip, wherever a fluid is shown (buckets, Beakers, Glass Flasks, Syringes
 * and other fluid-holding items, machine tank gauges). One line per hazard the fluid carries, read from the same
 * numbers the machines use ({@link FluidHazards}): Thermal says Hot or Cold when the fluid's temperature is outside
 * the safe range, Radiation and Metaphysical say Mild or Severe by the config thresholds, and Biohazard has a single
 * level so it just names itself.
 */
public final class HazardTooltips {

    private HazardTooltips() {}

    /**
     * Whether the player may see hazard information. Always true for now; once the Notebook/Tablet exists this is the
     * one place that checks the player's inventory for it.
     */
    public static boolean canSeeHazards() {
        return true;
    }

    /** The hazard lines for {@code fluid}; empty for an empty or harmless fluid, or when hazards are hidden. */
    public static List<Component> getLines(FluidStack fluid) {
        List<Component> lines = new ArrayList<>();
        if (fluid.isEmpty() || !canSeeHazards()) return lines;

        for (HazardFamily family : HazardFamily.values()) {
            int level = FluidHazards.getLevel(fluid, family);
            if (level <= 0) continue;

            String key = switch (family) {
                case THERMAL -> FluidHazards.getTemperature(fluid) > StatConfig.getCurrent().thermalReferenceTemperature()
                        ? "thermal.hot" : "thermal.cold";
                case BIOHAZARD -> "biohazard";
                case RADIATION -> level >= 2 ? "radiation.severe" : "radiation.mild";
                case METAPHYSICAL -> level >= 2 ? "metaphysical.severe" : "metaphysical.mild";
            };
            lines.add(Component.translatable("tooltip.dermicraft.fluid_hazard",
                    Component.translatable("tooltip.dermicraft.fluid_hazard." + key)).withStyle(ChatFormatting.RED));
        }
        return lines;
    }
}
