package net.scruffy.dermicraft.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Which face texture the Mutator shows, driven from {@code MutatorBlockEntity}'s tick. Recovering
 * takes priority over Running -- a machine below max HP is signalling distress even if it's still
 * limping through a cycle, so the error texture wins over the "working" one.
 */
public enum MutatorVisualState implements StringRepresentable {
    IDLE("idle"),
    RUNNING("running"),
    RECOVERING("recovering");

    private final String name;

    MutatorVisualState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
