package net.scruffy.dermicraft.block.custom;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * What a machine is doing and how healthy it is, as one block-state property shared by every machine with health
 * (the Masticator, Metastasizer, Mutator, Effluentcer, Render Kiln, Render Furnace and Grafting Table). It tells the
 * player at a glance, through the status lamp and any moving parts, what state the machine is in. The Drooling family
 * has no health and no lamp, so it never carries this property.
 *
 * <p>Two things feed it: whether the machine is working, and its health. Health has three bands: full, damaged (below
 * max, but the machine still functions) and broken down. A machine breaks down when its health reaches 0 and stays
 * broken until it has healed back to full, so BROKEN_DOWN is a latched state, not just "health is low" (the latch
 * lives in {@code MachineBaseBlockEntity}). A broken-down machine does not work.
 *
 * <table>
 *   <caption>Statuses and what they show</caption>
 *   <tr><th>Status</th><th>Meaning</th><th>Lamp</th></tr>
 *   <tr><td>IDLE</td><td>not working, full health</td><td>off</td></tr>
 *   <tr><td>RUNNING</td><td>working, full health</td><td>on (green)</td></tr>
 *   <tr><td>STRAINED_IDLE</td><td>not working, damaged but able to function</td><td>warning (yellow)</td></tr>
 *   <tr><td>STRAINED_RUNNING</td><td>working, damaged</td><td>warning (yellow)</td></tr>
 *   <tr><td>BROKEN_DOWN</td><td>broken down, healing back to full</td><td>error (red)</td></tr>
 * </table>
 */
public enum MachineStatus implements StringRepresentable {
    IDLE("idle"),
    RUNNING("running"),
    STRAINED_IDLE("strained_idle"),
    STRAINED_RUNNING("strained_running"),
    BROKEN_DOWN("broken_down");

    /** The block-state property every machine with health shares. */
    public static final EnumProperty<MachineStatus> PROPERTY = EnumProperty.create("status", MachineStatus.class);

    private final String name;

    MachineStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Whether the machine is doing its work in this status, which is what a moving part (the Masticator's mouth)
     * follows. */
    public boolean isWorking() {
        return this == RUNNING || this == STRAINED_RUNNING;
    }

    /**
     * The one place the rules live. {@code brokenDown} is the machine's latched flag (see the class comment), which
     * wins over everything; a broken-down machine is never reported as working.
     *
     * @param working whether the machine is actually working right now
     * @param health current health
     * @param maxHealth maximum health; 0 means the machine has no health system, so it is never strained
     */
    public static MachineStatus of(boolean working, boolean brokenDown, int health, int maxHealth) {
        if (brokenDown) return BROKEN_DOWN;
        boolean damaged = maxHealth > 0 && health < maxHealth;
        if (working) return damaged ? STRAINED_RUNNING : RUNNING;
        return damaged ? STRAINED_IDLE : IDLE;
    }
}
