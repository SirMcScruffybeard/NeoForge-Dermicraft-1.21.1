package net.scruffy.dermicraft.screen.custom.craw;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;

/**
 * The Craw's screen on the shared evolution-machine base: it draws only its Main tab, the staging input feeding the
 * storage slot through a track-only arrow (the transfer is instant). The tabs, the Module tab, the Evolution slot and
 * the auto-push button (the base's auto-dispense toggle) come from the base. There is no health bar, and the Module tab
 * shows only the tier, since a Craw holds items and has no hazard tolerances.
 */
public class CrawScreen extends AbstractEvolvingMachineScreen<CrawMenu> {

    // Logical slot positions in CrawMenu -- input feeds into storage, matching the input/arrow/
    // result layout used by Masticator, just without a fill.
    private static final int INPUT_SLOT_X = 51;
    private static final int INPUT_SLOT_Y = 35;
    private static final int STORAGE_SLOT_X = 110;
    private static final int STORAGE_SLOT_Y = 35;
    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 39;

    // Sits right of the storage slot, same row -- clear of the arrow/slots, no layout collision.
    private static final int AUTO_PUSH_BUTTON_X = STORAGE_SLOT_X + 18 + 4;
    private static final int AUTO_PUSH_BUTTON_Y = STORAGE_SLOT_Y - 1;

    public CrawScreen(CrawMenu menu, Inventory playerInventory, Component title) {
        // Item output, so the button is the "auto dispense" wording of the fluid machines' auto drain.
        super(menu, playerInventory, title, "craw", "auto_dispense");
    }

    @Override
    protected boolean showsTolerances() {
        return false;
    }

    @Override
    protected int getAutoDrainButtonX() {
        return AUTO_PUSH_BUTTON_X;
    }

    @Override
    protected int getAutoDrainButtonY() {
        return AUTO_PUSH_BUTTON_Y;
    }

    @Override
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        renderItemSlot(guiGraphics, x + INPUT_SLOT_X - 1, y + INPUT_SLOT_Y - 1);
        renderItemSlot(guiGraphics, x + STORAGE_SLOT_X - 1, y + STORAGE_SLOT_Y - 1);
        renderArrowTrack(guiGraphics, x + ARROW_X, y + ARROW_Y);
    }

    @Override
    protected void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        // No gauges or named slots to describe.
    }
}
