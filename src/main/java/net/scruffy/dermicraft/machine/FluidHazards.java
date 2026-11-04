package net.scruffy.dermicraft.machine;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.hazard.FluidHazardData;
import net.scruffy.dermicraft.hazard.HazardFamily;

/**
 * Reads how hazardous a fluid is, from the {@code fluid_hazard} data map and the server config. This is the one place
 * a fluid turns into hazard numbers: {@link net.scruffy.dermicraft.hazard.HazardProfile} asks it which band a fluid
 * falls in (to accept or refuse it), and {@link HazardEffects} asks it for the intensity (to size the penalty).
 *
 * <p>Thermal comes from temperature: the data map's {@code thermal} value if the fluid has one, otherwise the fluid's
 * own temperature, and only the distance beyond the safe range around the reference counts. The other families come
 * straight from the data map and are placed in a band by the config thresholds.
 */
public final class FluidHazards {

    private FluidHazards() {}

    /** The fluid's data map entry, or {@link FluidHazardData#NONE} when it has none. */
    public static FluidHazardData getData(FluidStack fluid) {
        FluidHazardData data = BuiltInRegistries.FLUID.wrapAsHolder(fluid.getFluid()).getData(ModDataMaps.FLUID_HAZARD);
        return data == null ? FluidHazardData.NONE : data;
    }

    /** The temperature in kelvin the rules treat the fluid as having. */
    public static int getTemperature(FluidStack fluid) {
        int override = getData(fluid).thermal();
        return override != FluidHazardData.USE_FLUID_TEMPERATURE ? override : fluid.getFluidType().getTemperature(fluid);
    }

    /** Kelvin beyond the safe range around the reference temperature, hot or cold; 0 inside the range. */
    public static int getThermalExcess(FluidStack fluid) {
        StatValues values = StatConfig.getCurrent();
        int distance = Math.abs(getTemperature(fluid) - values.thermalReferenceTemperature());
        return Math.max(0, distance - values.thermalSafeRange());
    }

    /**
     * The fluid's intensity for {@code family}: for Thermal the kelvin beyond the safe range, for the others the number
     * in the data map. 0 means none.
     */
    public static int getIntensity(FluidStack fluid, HazardFamily family) {
        if (fluid.isEmpty()) return 0;
        if (family == HazardFamily.THERMAL) return getThermalExcess(fluid);
        return getData(fluid).getIntensity(family);
    }

    /** The band a fluid falls in for {@code family}: 0 harmless, 1 Mild, 2 Severe (Thermal and Biohazard top out at 1). */
    public static int getLevel(FluidStack fluid, HazardFamily family) {
        int intensity = getIntensity(fluid, family);
        if (intensity <= 0) return 0;
        if (family == HazardFamily.THERMAL) return 1;
        return Math.min(getBand(family).getLevel(intensity), family.getMaxLevel());
    }

    /** The config thresholds for {@code family}. Thermal has no band, it works from the safe range. */
    public static HazardBand getBand(HazardFamily family) {
        StatValues values = StatConfig.getCurrent();
        return switch (family) {
            case RADIATION -> values.radiationBand();
            case BIOHAZARD -> values.biohazardBand();
            case METAPHYSICAL -> values.metaphysicalBand();
            case THERMAL -> throw new IllegalArgumentException("Thermal has no intensity band; it works from temperature");
        };
    }
}
