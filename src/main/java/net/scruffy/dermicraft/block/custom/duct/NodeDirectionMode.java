package net.scruffy.dermicraft.block.custom.duct;

import net.minecraft.util.StringRepresentable;

/**
 * Per-face, per-type (item/fluid independently) routing state a Node's connected leg cycles
 * through when clicked: In -> Out. Only meaningful on a face the Node is actually connected to
 * (see {@code NodeBlockEntity#isConnected}) — an unconnected face has no mode to show at all.
 *
 * <p>{@code OFF} was retired (2026-08-27, the item/fluid-independent-direction rework) -- it was
 * redundant with the per-leg, per-type enabled toggles ({@code NodeBlockEntity#itemsEnabled}/
 * {@code #fluidsEnabled}), which are now the sole on/off switch. A leg's direction is meaningless
 * until that type is separately enabled, same as it always was in practice.
 */
public enum NodeDirectionMode implements StringRepresentable {
    IN("in"),
    OUT("out");

    private final String name;

    NodeDirectionMode(String name) {
        this.name = name;
    }

    public NodeDirectionMode next() {
        return switch (this) {
            case IN -> OUT;
            case OUT -> IN;
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
