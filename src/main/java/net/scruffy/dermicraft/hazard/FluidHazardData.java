package net.scruffy.dermicraft.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How hazardous one fluid is, attached to the fluid through the {@code fluid_hazard} data map. A fluid with no entry
 * carries no Radiation, Biohazard or Metaphysical hazard, and its Thermal hazard comes from its real temperature.
 * A data pack can put any fluid, from any mod, into the hazard rules by adding an entry.
 *
 * <p>The numbers are intensities, compared against the thresholds in the server config (see {@link FluidHazards}).
 * By default Radiation and Metaphysical run 1 to 100 for Mild and 100 to 200 for Severe, and Biohazard runs 1 to 100.
 *
 * @param thermal       the temperature in kelvin to treat the fluid as, or -1 to read it from the fluid itself. For
 *                      modded fluids whose author never set a temperature, or set a wrong one
 * @param radiation     Radiation intensity, 0 for none
 * @param biohazard     Biohazard intensity, 0 for none
 * @param metaphysical  Metaphysical intensity, 0 for none
 */
public record FluidHazardData(int thermal, int radiation, int biohazard, int metaphysical) {

    /** The thermal value meaning "use the fluid's own temperature". */
    public static final int USE_FLUID_TEMPERATURE = -1;

    public static final FluidHazardData NONE = new FluidHazardData(USE_FLUID_TEMPERATURE, 0, 0, 0);

    public static final Codec<FluidHazardData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("thermal", USE_FLUID_TEMPERATURE).forGetter(FluidHazardData::thermal),
                    Codec.INT.optionalFieldOf("radiation", 0).forGetter(FluidHazardData::radiation),
                    Codec.INT.optionalFieldOf("biohazard", 0).forGetter(FluidHazardData::biohazard),
                    Codec.INT.optionalFieldOf("metaphysical", 0).forGetter(FluidHazardData::metaphysical))
            .apply(instance, FluidHazardData::new));

    /** An entry with only a thermal override. */
    public static FluidHazardData thermal(int kelvin) {
        return new FluidHazardData(kelvin, 0, 0, 0);
    }

    /** An entry with only a Radiation intensity. */
    public static FluidHazardData radiation(int intensity) {
        return new FluidHazardData(USE_FLUID_TEMPERATURE, intensity, 0, 0);
    }

    /** An entry with only a Biohazard intensity. */
    public static FluidHazardData biohazard(int intensity) {
        return new FluidHazardData(USE_FLUID_TEMPERATURE, 0, intensity, 0);
    }

    /** An entry with only a Metaphysical intensity. */
    public static FluidHazardData metaphysical(int intensity) {
        return new FluidHazardData(USE_FLUID_TEMPERATURE, 0, 0, intensity);
    }

    /** The intensity stored for {@code family}; Thermal has no intensity here (see {@link #thermal}), so it gives 0. */
    public int getIntensity(HazardFamily family) {
        return switch (family) {
            case RADIATION -> radiation;
            case BIOHAZARD -> biohazard;
            case METAPHYSICAL -> metaphysical;
            case THERMAL -> 0;
        };
    }
}
