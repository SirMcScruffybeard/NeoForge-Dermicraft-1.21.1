package net.scruffy.dermicraft.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Which face texture the Render Kiln shows, driven from {@code RenderKilnBlockEntity}'s tick.
 * Recovering takes priority over Running -- a machine below max HP is signalling distress even if
 * it's still limping through a cycle, so the error texture wins over the "working" one. Mirrors
 * {@link MasticatorVisualState}/{@link MutatorVisualState}.
 */
public enum RenderKilnVisualState implements StringRepresentable {
    IDLE("idle"),
    RUNNING("running"),
    RECOVERING("recovering");

    private final String name;

    RenderKilnVisualState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
