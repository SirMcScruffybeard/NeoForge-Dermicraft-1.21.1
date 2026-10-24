package net.scruffy.dermicraft.hazard;

import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.Fluid;
import net.scruffy.dermicraft.datagen.tag.ModTags;

import java.util.List;

/**
 * The four hazard families a machine can hold a permanent tolerance to (see the Machine Upgrade
 * Redesign notes). A family counts once against a machine's tolerance cap (the cap equals its tier)
 * regardless of the level held. Each family has a display initial and a top level: Thermal and
 * Biohazard are on/off (max 1), Radiation and Metaphysical go none/mild/severe (max 2). Levels are
 * plain ints (0 = none) so they fit {@link net.scruffy.dermicraft.component.EvolutionStateData}. Each
 * family lists the fluid hazard tag for each of its levels, mild first, so the max level is just the
 * number of tags and {@link #getTags(int)} turns a held level into the tags it tolerates.
 */
public enum HazardFamily implements StringRepresentable {
    THERMAL('T', ModTags.Fluids.THERMAL),
    RADIATION('R', ModTags.Fluids.RADIATION_MILD, ModTags.Fluids.RADIATION_SEVERE),
    BIOHAZARD('B', ModTags.Fluids.BIOHAZARD),
    METAPHYSICAL('M', ModTags.Fluids.METAPHYSICAL_MILD, ModTags.Fluids.METAPHYSICAL_SEVERE);

    private final char INITIAL;
    private final List<TagKey<Fluid>> LEVEL_TAGS;

    @SafeVarargs
    HazardFamily(char initial, TagKey<Fluid>... levelTags) {
        INITIAL = initial;
        LEVEL_TAGS = List.of(levelTags);
    }


    public char getInitial() {
        return INITIAL;
    }

    public int getMaxLevel() { return LEVEL_TAGS.size(); }

    /** The fluid hazard tags tolerated by holding this family at {@code level}: every level up to it,
     * so Severe also covers Mild. Level 0 (not held) gives none. */
    public List<TagKey<Fluid>> getTags(int level) {
        return LEVEL_TAGS.subList(0, Math.clamp(level, 0, LEVEL_TAGS.size()));
    }

    /** The name used when a family is written into data (an Evolution Module's component). */
    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
