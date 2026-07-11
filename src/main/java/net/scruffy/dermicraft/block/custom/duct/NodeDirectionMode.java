package net.scruffy.dermicraft.block.custom.duct;

import net.minecraft.util.StringRepresentable;

/**
 * Per-face routing state a Node's connected leg cycles through when clicked: In -> Out -> Off.
 * Only meaningful on a face the Node is actually connected to (see
 * {@code NodeBlockEntity#isConnected}) — an unconnected face has no mode to show at all.
 */
public enum NodeDirectionMode implements StringRepresentable {
    IN("in"),
    OUT("out"),
    OFF("off");

    private final String name;

    NodeDirectionMode(String name) {
        this.name = name;
    }

    public NodeDirectionMode next() {
        return switch (this) {
            case IN -> OUT;
            case OUT -> OFF;
            case OFF -> IN;
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
