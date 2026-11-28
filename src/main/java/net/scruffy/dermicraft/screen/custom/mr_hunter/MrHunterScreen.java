package net.scruffy.dermicraft.screen.custom.mr_hunter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractFarmScreen;
import net.scruffy.dermicraft.screen.FarmLayout;

/** Mr. Hunter's screen: the shared farm screen's tabs, with the XP gauge as a row above the fuel row on Home, as Mr. Shepherd's. */
public class MrHunterScreen extends AbstractFarmScreen<MrHunterMenu> {

    // The XP gauge sits just above the fuel gauge, same size and style, with its container slot to its left.
    private static final int XP_GAUGE_X = FarmLayout.FUEL_GAUGE_X;
    private static final int XP_GAUGE_Y = MrHunterMenu.XP_SLOT_Y;

    private FluidTankRenderer xpRenderer;

    public MrHunterScreen(MrHunterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        xpRenderer = createGaugeRenderer(menu.BE.getXpTank().getCapacity());
    }

    @Override
    protected void renderHomeExtras(GuiGraphics guiGraphics, int x, int y) {
        renderSlotBackdrop(guiGraphics, x + MrHunterMenu.XP_SLOT_X, y + MrHunterMenu.XP_SLOT_Y);
        renderGauge(guiGraphics, xpRenderer, menu.BE.getXpTank().getFluid(), x + XP_GAUGE_X, y + XP_GAUGE_Y);
    }

    @Override
    protected void renderHomeLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.BE.getXpTank().getFluid(),
                XP_GAUGE_X + 1, XP_GAUGE_Y + 1, xpRenderer, Component.translatable("tooltip.dermicraft.gauge.xp"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, MrHunterMenu.XP_SLOT_X + 1, MrHunterMenu.XP_SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getXpTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.xp_container"));
    }
}
