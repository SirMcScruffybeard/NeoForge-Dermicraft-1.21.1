package net.scruffy.dermicraft.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Which face texture the Masticator shows, driven from {@code MasticatorBlockEntity}'s tick.
 * Recovering takes priority over Running -- a machine below max HP is signalling distress even if
 * it's still limping through a cycle, so the error texture wins over the "working" one. Mirrors
 * {@link MutatorVisualState}.
 */
public enum MasticatorVisualState implements StringRepresentable {
    IDLE("idle"),
    RUNNING("running"),
    RECOVERING("recovering");

    private final String name;

    MasticatorVisualState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
