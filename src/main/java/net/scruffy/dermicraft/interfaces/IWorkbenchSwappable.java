package net.scruffy.dermicraft.interfaces;

/**
 * Marks a gadget item as having its own physical-part-swap sub-panel on the Workbench's Mod page
 * (see dermicraft-gear-stations-notes.md -> Workbench -> Swap page). Placing an item into the Mod
 * page's working-item slot dispatches to whichever gadget type it is -- this is the marker that
 * gate keeps the slot (only swappable gadgets may be placed there) and, as more gadgets grow their
 * own sub-panels, the thing each one implements to opt in, rather than a hardcoded if/else chain.
 *
 * <p>No methods yet -- Sunder's panel (chain slot + fuel gauge) is currently wired directly into
 * {@code WorkbenchMenu}/{@code WorkbenchScreen} rather than through a callback on this interface,
 * since it's the only implementation that exists. Once a second gadget (e.g. Drill Hammer) needs
 * its own different panel, this is the extension point to grow real dispatch methods on.
 */
public interface IWorkbenchSwappable {
}
