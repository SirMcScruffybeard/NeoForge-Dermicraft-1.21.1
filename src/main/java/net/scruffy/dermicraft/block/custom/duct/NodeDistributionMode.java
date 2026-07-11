package net.scruffy.dermicraft.block.custom.duct;

import net.minecraft.util.StringRepresentable;

/**
 * A Node's global distribution mode across whichever legs are currently marked {@code OUT}.
 * No priority ordering exists — see design notes ("plumbing solves prioritization, not software").
 */
public enum NodeDistributionMode implements StringRepresentable {
    ROUND_ROBIN("round_robin"),
    EQUAL_SPREAD("equal_spread");

    private final String name;

    NodeDistributionMode(String name) {
        this.name = name;
    }

    public NodeDistributionMode next() {
        return this == ROUND_ROBIN ? EQUAL_SPREAD : ROUND_ROBIN;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
