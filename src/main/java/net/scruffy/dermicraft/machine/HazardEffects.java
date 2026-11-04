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

    /** What radioactive fluids cost a machine this cycle: a multiplier on the fuel it spends and HP it loses. */
    public record RadiationEffect(double fuelMultiplier, int damage) {
        public static final RadiationEffect NONE = new RadiationEffect(1.0, 0);
    }

    /**
     * The Radiation cost of the fluids in {@code fluids}, the worst one counting. Per fluid, by the band its intensity
     * falls in and the machine's Radiation tolerance level:
     * <ul>
     *   <li>Tolerance covers the fluid's band: no effect.</li>
     *   <li>A Mild-band fluid: fuel use grows x1 to x2 across the band.</li>
     *   <li>A Severe-band fluid: fuel use grows x2 to x3 across the band, and the machine loses 1 to 2 HP per cycle,
     *   growing with the intensity.</li>
     *   <li>A Severe-band fluid against a Mild tolerance: the effect is capped at the top of the Mild band, so the fuel
     *   surcharge of x2 and no HP loss, softened but not cancelled.</li>
     * </ul>
     * The configured modifier scales the extra fuel and the HP loss (0 turns the effect off).
     */
    public static RadiationEffect getRadiationEffect(List<FluidStack> fluids, HazardProfile tolerance) {
        StatValues values = StatConfig.getCurrent();
        double modifier = values.radiationEffectModifier();
        int tolerated = tolerance.getLevel(HazardFamily.RADIATION);
        if (modifier <= 0 || tolerated >= HazardFamily.RADIATION.getMaxLevel()) return RadiationEffect.NONE;

        HazardBand band = values.radiationBand();
        double fuel = 1.0;
        int damage = 0;
        for (FluidStack fluid : fluids) {
            int intensity = FluidHazards.getIntensity(fluid, HazardFamily.RADIATION);
            int level = band.getLevel(intensity);
            if (level <= tolerated) continue;

            double rawFuel;
            int rawDamage = 0;
            if (level == 1) {
                rawFuel = 1.0 + band.getMildFraction(intensity);
            } else if (tolerated >= 1) {
                rawFuel = 2.0;
            } else {
                double severe = band.getSevereFraction(intensity);
                rawFuel = 2.0 + severe;
                rawDamage = (int) Math.max(1, Math.ceil(modifier * 2.0 * severe));
            }
            fuel = Math.max(fuel, 1.0 + (rawFuel - 1.0) * modifier);
            damage = Math.max(damage, rawDamage);
        }
        return new RadiationEffect(fuel, damage);
    }
}
