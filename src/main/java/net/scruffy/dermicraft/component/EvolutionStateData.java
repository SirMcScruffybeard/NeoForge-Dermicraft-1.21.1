package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.scruffy.dermicraft.hazard.HazardFamily;

/**
 * A machine's in-place evolution state: its tier (L1-L4), its Overgrowth level (O1+, only reachable
 * at L4), and the level it holds in each {@link HazardFamily} (0 none, 1 mild, 2 severe; Thermal and
 * Biohazard only reach 1). The same record lives in the block entity's save data and rides on the
 * machine's item as the {@code EVOLUTION_STATE} data component, so a Forceps pickup or creative pick
 * block carries the investment home. An item without the component is a plain L1 machine
 * ({@link #DEFAULT}). See the Machine Upgrade Redesign section of {@code dermicraft-machine-notes.md}.
 *
 * <p>Immutable: every {@code with...} method returns a new record. The compact constructor clamps
 * every field into its valid range, so a bad save or a buggy caller can never produce an impossible
 * machine; it clamps in the safe direction (it never grants power): overgrowth is zeroed below
 * {@link #MAX_TIER}. It is only a last line of defense; the upgrade logic is expected to check the
 * real rules (exact next level, tolerance cap) and report a blocked reason before calling it.
 *
 * <p>The tolerance cap equals the tier, so compare {@link #getHeldFamilyCount()} against
 * {@link #tier()}.
 */
public record EvolutionStateData(int tier, int overgrowth, int thermal,
                                 int radiation, int biohazard, int metaphysical) {

    public static final int MAX_TIER = 4;
    /** Placeholder cap far beyond anything reachable, there to keep Overgrowth arithmetic safe. */
    public static final int MAX_OVERGROWTH = 45;   // placeholder, notes suggest 40-50

    /** A plain L1 machine: no Overgrowth, no hazard tolerances. */
    public static final EvolutionStateData DEFAULT = new EvolutionStateData(1, 0, 0, 0, 0, 0);

    public EvolutionStateData {
        tier = Math.max(1, Math.min(tier, MAX_TIER));
        overgrowth = tier < MAX_TIER ? 0 : Math.max(0, Math.min(overgrowth, MAX_OVERGROWTH));
        thermal = Math.max(0, Math.min(thermal, HazardFamily.THERMAL.getMaxLevel()));
        radiation = Math.max(0, Math.min(radiation, HazardFamily.RADIATION.getMaxLevel()));
        biohazard = Math.max(0, Math.min(biohazard, HazardFamily.BIOHAZARD.getMaxLevel()));
        metaphysical = Math.max(0, Math.min(metaphysical, HazardFamily.METAPHYSICAL.getMaxLevel()));
    }

    public int level(HazardFamily family) {
        return switch (family) {
            case THERMAL -> thermal;
            case RADIATION -> radiation;
            case BIOHAZARD -> biohazard;
            case METAPHYSICAL -> metaphysical;
        };
    }

    public EvolutionStateData withLevel(HazardFamily family, int level) {
        return switch (family) {
            case THERMAL -> new EvolutionStateData(tier, overgrowth, level, radiation, biohazard, metaphysical);
            case RADIATION -> new EvolutionStateData(tier, overgrowth, thermal, level, biohazard, metaphysical);
            case BIOHAZARD -> new EvolutionStateData(tier, overgrowth, thermal, radiation, level, metaphysical);
            case METAPHYSICAL -> new EvolutionStateData(tier, overgrowth, thermal, radiation, biohazard, level);
        };
    }

    public EvolutionStateData withTier(int newTier) {
        return new EvolutionStateData(newTier, overgrowth, thermal, radiation, biohazard, metaphysical);
    }

    public EvolutionStateData withOvergrowth(int newOvergrowth) {
        return new EvolutionStateData(tier, newOvergrowth, thermal, radiation, biohazard, metaphysical);
    }

    /** How many hazard families are held at any level; this is what counts against the tier cap. */
    public int getHeldFamilyCount() {
        int count = 0;
        for (HazardFamily family: HazardFamily.values()) {
            if (level(family) > 0) count++;
        }
        return count;
    }

    /**
     * The shared display name, so machines, tooltips and the Flesh Lab never format it differently:
     * {@code L#} (tier), or {@code O#} once Overgrowth is above 0, then the held families in
     * bracketed initials separated by ".". A family below its top level (Mild) shows lowercase, at its
     * top level (Severe, or Thermal/Biohazard's only level) uppercase. Examples: {@code L2 [T]},
     * {@code O3 [T.r.M]}; no suffix when no family is held.
     */
    public String label() {
        StringBuilder sb = new StringBuilder();
        sb.append(overgrowth > 0 ? "O" + overgrowth : "L" + tier);

        StringBuilder initials = new StringBuilder();
        for (HazardFamily family: HazardFamily.values()) {
            int level = level(family);
            if (level == 0) continue;
            if (initials.length() > 0) initials.append("|");
            char c  = family.getInitial();
            initials.append(level < family.getMaxLevel() ? Character.toLowerCase(c) : c);
        }
        if (initials.length() > 0) sb.append(" [").append(initials).append(']');
        return sb.toString();

    }

    public static final Codec<EvolutionStateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("tier").forGetter(EvolutionStateData::tier),
                    Codec.INT.fieldOf("overgrowth").forGetter(EvolutionStateData::overgrowth),
                    Codec.INT.fieldOf("thermal").forGetter(EvolutionStateData::thermal),
                    Codec.INT.fieldOf("radiation").forGetter(EvolutionStateData::radiation),
                    Codec.INT.fieldOf("biohazard").forGetter(EvolutionStateData::biohazard),
                    Codec.INT.fieldOf("metaphysical").forGetter(EvolutionStateData::metaphysical))
            .apply(instance, EvolutionStateData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EvolutionStateData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EvolutionStateData::tier,
            ByteBufCodecs.VAR_INT, EvolutionStateData::overgrowth,
            ByteBufCodecs.VAR_INT, EvolutionStateData::thermal,
            ByteBufCodecs.VAR_INT, EvolutionStateData::radiation,
            ByteBufCodecs.VAR_INT, EvolutionStateData::biohazard,
            ByteBufCodecs.VAR_INT, EvolutionStateData::metaphysical,
            EvolutionStateData::new);
}
