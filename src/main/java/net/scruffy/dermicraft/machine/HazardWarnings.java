package net.scruffy.dermicraft.machine;

import net.minecraft.network.chat.Component;
import net.scruffy.dermicraft.hazard.HazardFamily;

import java.util.ArrayList;
import java.util.List;

/**
 * What a machine's screen tells the player about hazards in play: which tolerance is needed for the fluids being
 * processed, and in plain words what the missing tolerance is costing. Works from the intensities the machine syncs
 * (see {@code AbstractFueledMachineBlockEntity#getHazardIntensity}) and the config, so the client needs no recipe or
 * tank knowledge. The wording is deliberately vague; the numbers are in {@link HazardEffects}.
 */
public final class HazardWarnings {

    private HazardWarnings() {}

    /**
     * The tolerance level {@code family} needs to be fully safe against a worst fluid of {@code intensity} (0 none,
     * 1 Mild, 2 Severe). For Thermal the intensity is the kelvin beyond the safe range, so anything above 0 needs
     * level 1.
     */
    public static int getNeededLevel(HazardFamily family, int intensity) {
        if (intensity <= 0) return 0;
        if (family == HazardFamily.THERMAL) return 1;
        return Math.min(FluidHazards.getBand(family).getLevel(intensity), family.getMaxLevel());
    }

    /**
     * One line per effect the machine suffers from {@code family} given it needs {@code needed} but only has
     * {@code tolerated}. Empty when nothing is wrong, or when the config has that effect turned off. Mirrors the rules
     * in {@link HazardEffects}: a Mild tolerance against a Severe fluid leaves only the Mild-band effects.
     */
    public static List<Component> getEffectLines(HazardFamily family, int needed, int tolerated) {
        List<Component> lines = new ArrayList<>();
        if (tolerated >= needed) return lines;

        StatValues values = StatConfig.getCurrent();
        boolean fullSevere = needed >= 2 && tolerated == 0;
        switch (family) {
            case THERMAL -> {
                if (values.thermalDamageModifier() > 0) lines.add(line("thermal.damage"));
            }
            case RADIATION -> {
                if (values.radiationEffectModifier() > 0) {
                    lines.add(line("radiation.fuel"));
                    if (fullSevere) lines.add(line("radiation.damage"));
                }
            }
            case BIOHAZARD -> {
                if (values.biohazardEffectModifier() > 0) {
                    lines.add(line("biohazard.speed"));
                    lines.add(line("biohazard.healing"));
                }
            }
            case METAPHYSICAL -> {
                if (values.metaphysicalEffectModifier() > 0) {
                    lines.add(line("metaphysical.speed"));
                    if (fullSevere) {
                        lines.add(line("metaphysical.fuel"));
                        lines.add(line("metaphysical.health"));
                    }
                }
            }
        }
        return lines;
    }

    private static Component line(String key) {
        return Component.translatable("tooltip.dermicraft.hazard_warning." + key);
    }
}
