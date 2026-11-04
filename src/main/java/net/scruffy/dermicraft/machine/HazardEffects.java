package net.scruffy.dermicraft.machine;

import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.hazard.HazardProfile;

import java.util.List;

/**
 * What a hazardous fluid does to a machine that processes it (the "hazard penalties instead of hard blocks" rules in
 * {@code dermicraft-machine-notes.md}). A machine accepts any fluid; the cost is paid per work cycle while it is
 * processing, and a matching tolerance cancels the effect. When several fluids are in play the effects of different
 * hazards add up, and within one hazard only the worst counts (two Thermal fluids do not double the burn).
 *
 * <p>Only the Thermal effect exists so far. Each hazard's numbers are in the server config.
 */
public final class HazardEffects {

    private HazardEffects() {}

    /**
     * HP a machine loses this cycle to the hot or cold fluids in {@code fluids}. The fluid's real temperature decides
     * (or its {@code fluid_hazard} data map override), not any tag, so any fluid from any mod hurts when it is far
     * enough from the reference. For the worst one, {@code modifier x (|temperature - reference| - safeRange) / 100},
     * at least 1. A fluid within the safe range of the reference does nothing. Zero when {@code tolerance} covers
     * Thermal, or when the config turns it off.
     */
    public static int getThermalDamage(List<FluidStack> fluids, HazardProfile tolerance) {
        if (tolerance.tolerates(HazardFamily.THERMAL)) return 0;

        double modifier = StatConfig.getCurrent().thermalDamageModifier();
        if (modifier <= 0) return 0;

        int worst = 0;
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) continue;
            int excess = FluidHazards.getThermalExcess(fluid);
            if (excess <= 0) continue;
            int damage = (int) Math.max(1, Math.round(modifier * excess / 100.0));
            worst = Math.max(worst, damage);
        }
        return worst;
    }
}
