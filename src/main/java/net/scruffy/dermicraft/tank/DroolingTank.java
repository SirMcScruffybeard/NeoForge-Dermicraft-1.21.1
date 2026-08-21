package net.scruffy.dermicraft.tank;

import net.minecraft.world.level.material.Fluid;

import java.util.function.Supplier;

/**
 * Shared tank for the Drooling machine family (Cauldron, Crucible, and whatever future member
 * shares the same shape) -- accepts only whatever fluid the owning machine currently targets,
 * read fresh from {@code currentTarget} on every fill rather than fixed at construction. Replaces
 * separate fluid-locked tank classes (the old {@code WaterTank}) with one tank whose valid fluid
 * can change over the machine's lifetime -- exactly what the Evolution Module mechanic needs
 * (dermicraft-machine-notes.md, Drooling Cauldron entry): the same tank instance holds water most
 * of the time and lava during/after evolution, with no tank-swap or block-entity-swap involved in
 * that transition.
 *
 * <p>Only gates NEW fills -- existing contents are never invalidated by a target change. That's
 * deliberate: when the target flips (a Module installed or removed), old contents are meant to
 * sit untouched until drained out by some other means, not be dumped or converted (see the
 * "production halts until the old fluid is gone" rule in the same doc entry).
 */
public class DroolingTank extends ModFluidTank {

    public DroolingTank(int capacity, int slot, Supplier<Fluid> currentTarget) {
        super(capacity, slot, fluidStack -> fluidStack.getFluid() == currentTarget.get());
    }
}
