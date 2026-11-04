package net.scruffy.dermicraft.machine;

/**
 * The intensity thresholds of one hazard family (Radiation, Biohazard or Metaphysical), from the server config. A fluid's
 * intensity places it in a band: below {@code mildStart} it is harmless (level 0), from {@code mildStart} up to
 * {@code severeStart} it is Mild (level 1), and from {@code severeStart} on it is Severe (level 2). Effects grow across
 * their band and stop growing at {@code max}. Biohazard has only one band, so its {@code severeStart} equals its
 * {@code max} and the family's top level of 1 keeps it from ever reading as Severe.
 *
 * @param mildStart   the lowest intensity that counts as hazardous
 * @param severeStart the lowest intensity that counts as Severe
 * @param max         the intensity where the effect stops growing
 */
public record HazardBand(int mildStart, int severeStart, int max) {

    /** The level (0 none, 1 mild, 2 severe) a fluid of this intensity falls in. */
    public int getLevel(int intensity) {
        if (intensity >= severeStart) return 2;
        if (intensity >= mildStart) return 1;
        return 0;
    }

    /**
     * How far through the Mild band {@code intensity} is, 0 at the start and 1 at the Severe start, clamped. The
     * input is capped first by the caller to what the machine's tolerance leaves exposed.
     */
    public double getMildFraction(int intensity) {
        return fraction(intensity, mildStart, severeStart);
    }

    /** How far through the Severe band {@code intensity} is, 0 at its start and 1 at {@code max}, clamped. */
    public double getSevereFraction(int intensity) {
        return fraction(intensity, severeStart, max);
    }

    private static double fraction(int intensity, int from, int to) {
        if (to <= from) return intensity >= to ? 1.0 : 0.0;
        return Math.clamp((intensity - from) / (double) (to - from), 0.0, 1.0);
    }
}
