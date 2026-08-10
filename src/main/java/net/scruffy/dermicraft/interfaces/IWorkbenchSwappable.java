package net.scruffy.dermicraft.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Marks a gadget item as having its own physical-part-swap sub-panel, reachable both from the
 * Scrench (field, see {@code ScrenchMenu}) and the Workbench's Swap page (station, see
 * {@code WorkbenchMenu}) -- see dermicraft-gear-stations-notes.md -> Workbench -> Swap page, and
 * the Scrench design notes in dermicraft-gadget-notes.md.
 *
 * <p>Generalized 2026-08-09 (Eater becomes the second implementation alongside Sunder) so the panel
 * itself lives on the gadget, not duplicated per host -- both hosts just ask whichever gadget is
 * present for its {@link SwapPanel} and add whatever comes back, rather than each host hand-writing
 * its own copy of gadget-specific slot logic.
 */
public interface IWorkbenchSwappable {

    /**
     * Opens a fresh, session-scoped panel. Called once when the hosting menu (Scrench or Workbench)
     * is constructed -- {@code Item} instances are singletons, so any per-session state has to live
     * on the returned {@link SwapPanel} object, never on the gadget item itself.
     *
     * <p>{@code gadgetStackSupplier} is a live accessor, not a captured {@link ItemStack} -- the
     * Workbench's own working-item slot can have its contents swapped out entirely while its menu
     * stays open (a long-lived session), so every read/write must go through this supplier rather
     * than holding one reference from open time. The Scrench's shorter hand-held session has no such
     * risk in practice, but uses the same supplier shape for one consistent contract both hosts
     * share -- see {@code SunderChainSlot} (the pre-existing Workbench-only live-view slot this
     * generalizes) for the pattern this was modeled on.
     *
     * @param fieldHosted true when opened via the Scrench (field), false via the Workbench
     *                    (station) -- the "safer/costless at the station" distinction some gadgets'
     *                    field costs (movement penalty, recalibration delay, ...) key off of.
     */
    SwapPanel openSwapPanel(Supplier<ItemStack> gadgetStackSupplier, Player player, boolean fieldHosted);

    /**
     * A gadget's own swap-panel session. {@link #slots} is called once to build the panel's slots
     * against {@code panelX}/{@code panelY} -- the origin the host wants the panel drawn at, so the
     * same gadget panel can be positioned differently by Scrench's screen vs. the Workbench's Swap
     * page without either host needing to know the gadget's own internal slot layout.
     */
    interface SwapPanel {
        /**
         * @param active whether the returned slots should currently be interactable/visible-as-live
         *               -- the Workbench hosts multiple pages sharing one menu (Mod vs. Fabrication),
         *               so its panel needs to go inert while a different page is showing; the Scrench
         *               is single-purpose and always passes a constantly-true supplier. Each
         *               implementation's own {@code Slot}s should return {@code active.getAsBoolean()}
         *               from their {@code isActive()} override.
         */
        List<Slot> slots(int panelX, int panelY, BooleanSupplier active);

        /** Called when the hosting menu closes. A copy-out/copy-back gadget (Sunder) commits its
         * materialized data and applies any completed-swap costs here; a live-view gadget (whose
         * slots already write straight through, e.g. Eater) can leave this as a no-op or use it
         * purely for a field-cost check. */
        default void onClosed(Player player) {}
    }
}
