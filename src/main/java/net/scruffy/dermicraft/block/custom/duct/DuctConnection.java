package net.scruffy.dermicraft.block.custom.duct;

import net.minecraft.util.StringRepresentable;

/**
 * Per-face connection state for an Innards Duct, driving which sub-model that face renders.
 * <ul>
 *     <li>{@link #NONE} - nothing on that face (air/solid); render nothing.</li>
 *     <li>{@link #PIPE} - another duct or Node; render the arm.</li>
 *     <li>{@link #END} - a machine/inventory (capability block); render the end flange.</li>
 * </ul>
 * This is purely visual. Actual flow is gated by a Node per the design; a PIPE/END connection
 * here only means the pieces are physically touching, not that anything is routed.
 */
public enum DuctConnection implements StringRepresentable {
    NONE("none"),
    PIPE("pipe"),
    END("end");

    private final String name;

    DuctConnection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
