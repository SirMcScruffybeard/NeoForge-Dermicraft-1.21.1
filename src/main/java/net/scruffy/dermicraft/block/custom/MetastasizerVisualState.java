package net.scruffy.dermicraft.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Which face texture the Metastasizer shows, driven from {@code MetastasizerBlockEntity}'s tick.
 * Recovering takes priority over Running -- a machine below max HP is signalling distress even if
 * it's still limping through a cycle, so the error texture wins over the "working" one. Mirrors
 * {@link MutatorVisualState}/{@link MasticatorVisualState}/{@link EffluentcerVisualState}.
 */
public enum MetastasizerVisualState implements StringRepresentable {
    IDLE("idle"),
    RUNNING("running"),
    RECOVERING("recovering");

    private final String name;

    MetastasizerVisualState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
