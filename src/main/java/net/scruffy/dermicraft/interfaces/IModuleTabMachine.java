package net.scruffy.dermicraft.interfaces;

import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A machine whose screen has a Main tab and a Module tab (the general Module slots plus the Evolution slot and the
 * tolerance panel). The shared menu and screen bases ({@code AbstractEvolvingMachineMenu} and
 * {@code AbstractEvolvingMachineScreen}) read the Module slots and remember the open tab through this.
 */
public interface IModuleTabMachine {

    /** The general Module slots (the Evolution slot is separate, in the engine). */
    ItemStackHandler getModuleInventory();

    /** Whether the Module tab was the last one open, so reopening the screen returns to it. */
    boolean isModuleTabActive();

    void setModuleTabActive(boolean active);
}
