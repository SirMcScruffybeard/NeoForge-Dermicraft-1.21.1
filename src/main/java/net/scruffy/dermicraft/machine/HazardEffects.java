package net.scruffy.dermicraft.machine;

import net.minecraft.util.RandomSource;
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

    /** What Biohazard fluids do to a machine: one factor applied to both its work speed and its healing rate. */
    public record BiohazardEffect(double factor) {
        public static final BiohazardEffect NONE = new BiohazardEffect(1.0);
    }

    /** The lowest the Biohazard factor can fall, so a strong modifier slows a machine to a crawl, never a standstill. */
    private static final double BIOHAZARD_MIN_FACTOR = 0.05;

    /**
     * The Biohazard slowdown from the fluids in {@code fluids}, the worst one counting: both speed and healing are
     * multiplied by {@code 1 - 0.5 x modifier x fraction}, where the fraction is how far the fluid's intensity is
     * through the Biohazard band (so x1 at the start, x0.5 at the max). Biohazard has a single level, so any Biohazard
     * tolerance cancels it. It multiplies with the other effects, and does no HP damage of its own.
     */
    public static BiohazardEffect getBiohazardEffect(List<FluidStack> fluids, HazardProfile tolerance) {
        StatValues values = StatConfig.getCurrent();
        double modifier = values.biohazardEffectModifier();
        if (modifier <= 0 || tolerance.tolerates(HazardFamily.BIOHAZARD)) return BiohazardEffect.NONE;

        HazardBand band = values.biohazardBand();
        double factor = 1.0;
        for (FluidStack fluid : fluids) {
            int intensity = FluidHazards.getIntensity(fluid, HazardFamily.BIOHAZARD);
            if (band.getLevel(intensity) < 1) continue;
            double slowed = Math.max(BIOHAZARD_MIN_FACTOR, 1.0 - 0.5 * modifier * band.getMildFraction(intensity));
            factor = Math.min(factor, slowed);
        }
        return new BiohazardEffect(factor);
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

    /**
     * How erratic a machine is this cycle. Each spread is the half-width of a random swing around "normal": a speed
     * spread of 0.5 means the cycle's work speed is anywhere from x0.5 to x1.5. {@code healthSwing} is the most HP the
     * machine can gain or lose in the cycle.
     */
    public record MetaphysicalEffect(double speedSpread, double fuelSpread, int healthSwing) {
        public static final MetaphysicalEffect NONE = new MetaphysicalEffect(0, 0, 0);

        public boolean isNone() {
            return speedSpread <= 0 && fuelSpread <= 0 && healthSwing <= 0;
        }
    }

    /** What one cycle's dice came out as: multipliers on work speed and fuel use, and a signed HP change. */
    public record MetaphysicalRoll(double speedFactor, double fuelFactor, int healthChange) {
        public static final MetaphysicalRoll NONE = new MetaphysicalRoll(1.0, 1.0, 0);
    }

    /** The lowest a rolled speed factor can fall, so a bad roll stalls a cycle without freezing the machine. */
    private static final double METAPHYSICAL_MIN_SPEED_FACTOR = 0.05;

    /** Rolls one cycle of {@code effect}: uniform random speed and fuel factors within their spreads, and an HP change
     * from -swing to +swing. */
    public static MetaphysicalRoll rollMetaphysical(MetaphysicalEffect effect, RandomSource random) {
        if (effect.isNone()) return MetaphysicalRoll.NONE;
        double speed = Math.max(METAPHYSICAL_MIN_SPEED_FACTOR, 1.0 + (random.nextDouble() * 2 - 1) * effect.speedSpread());
        double fuel = Math.max(0.0, 1.0 + (random.nextDouble() * 2 - 1) * effect.fuelSpread());
        int health = effect.healthSwing() > 0 ? random.nextInt(-effect.healthSwing(), effect.healthSwing() + 1) : 0;
        return new MetaphysicalRoll(speed, fuel, health);
    }

    /**
     * The Metaphysical erratic-ness of the fluids in {@code fluids}, the worst one counting for each part. By the
     * fluid's band and the machine's tolerance level:
     * <ul>
     *   <li>Tolerance covers the band: no effect.</li>
     *   <li>Mild band: work speed varies by up to +-50% across the band.</li>
     *   <li>Severe band: speed varies +-50% to +-100% across the band, fuel use varies up to +-50%, and the machine
     *   gains or loses up to 2 HP per cycle, all growing across the band.</li>
     *   <li>Severe-band fluid against a Mild tolerance: capped at the top of the Mild band, so only the +-50% speed
     *   variation.</li>
     * </ul>
     * The configured modifier scales the spreads and the HP swing (0 turns the effect off).
     */
    public static MetaphysicalEffect getMetaphysicalEffect(List<FluidStack> fluids, HazardProfile tolerance) {
        StatValues values = StatConfig.getCurrent();
        double modifier = values.metaphysicalEffectModifier();
        int tolerated = tolerance.getLevel(HazardFamily.METAPHYSICAL);
        if (modifier <= 0 || tolerated >= HazardFamily.METAPHYSICAL.getMaxLevel()) return MetaphysicalEffect.NONE;

        HazardBand band = values.metaphysicalBand();
        double speed = 0;
        double fuel = 0;
        int swing = 0;
        for (FluidStack fluid : fluids) {
            int intensity = FluidHazards.getIntensity(fluid, HazardFamily.METAPHYSICAL);
            int level = band.getLevel(intensity);
            if (level <= tolerated) continue;

            if (level == 1) {
                speed = Math.max(speed, 0.5 * band.getMildFraction(intensity));
            } else if (tolerated >= 1) {
                speed = Math.max(speed, 0.5);
            } else {
                double severe = band.getSevereFraction(intensity);
                speed = Math.max(speed, 0.5 + 0.5 * severe);
                fuel = Math.max(fuel, 0.5 * severe);
                swing = Math.max(swing, (int) Math.max(1, Math.ceil(modifier * 2.0 * severe)));
            }
        }
        return new MetaphysicalEffect(speed * modifier, fuel * modifier, swing);
    }
}
