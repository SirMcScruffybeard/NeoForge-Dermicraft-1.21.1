package net.scruffy.dermicraft.machine;

import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.datagen.tag.ModTags;
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
     * HP a machine loses this cycle to the hot or cold fluids in {@code fluids}. The fluid's real temperature decides,
     * not the Thermal tag, so any fluid from any mod hurts when it is far enough from the reference (the tag only
     * matters to what tolerates it and to the hard blocks in tanks and ducts). For the worst one,
     * {@code modifier x (|temperature - reference| - safeRange) / 100}, at least 1, so super-cooled fluids burn as well
     * as hot ones. A fluid within the safe range of the reference does nothing.
     * Zero when none of the fluids is Thermal, when {@code tolerance} covers Thermal, or when the config turns it off.
     */
    public static int getThermalDamage(List<FluidStack> fluids, HazardProfile tolerance) {
        if (tolerance.tolerates(ModTags.Fluids.THERMAL)) return 0;

        StatValues values = StatConfig.getCurrent();
        if (values.thermalDamageModifier() <= 0) return 0;

        int worst = 0;
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) continue;
            int temperature = fluid.getFluidType().getTemperature(fluid);
            double distance = Math.abs(temperature - values.thermalReferenceTemperature()) - values.thermalSafeRange();
            if (distance <= 0) continue;
            int damage = (int) Math.max(1, Math.round(values.thermalDamageModifier() * distance / 100.0));
            worst = Math.max(worst, damage);
        }
        return worst;
    }
}
