package net.scruffy.dermicraft.screen.custom.mr_shepherd;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractFarmScreen;
import net.scruffy.dermicraft.screen.FarmLayout;

import java.util.List;

/**
 * Mr. Shepherd's screen: the shared farm screen's Home and Field tabs, with the XP gauge as a second horizontal row
 * above the fuel row and the feed row beside the fuel row on Home, and a Population tab with the cap steppers.
 */
public class MrShepherdScreen extends AbstractFarmScreen<MrShepherdMenu> {

    // The XP gauge sits just above the fuel gauge, same size and style, with its container slot to its left.
    private static final int XP_GAUGE_X = FarmLayout.FUEL_GAUGE_X;
    private static final int XP_GAUGE_Y = MrShepherdMenu.XP_SLOT_Y;

    // The cap readout and its two steppers on the Population tab, laid out like a Field row.
    private static final int CAP_ROW_Y = FarmLayout.FIELD_WIDTH_ROW_Y;

    private FluidTankRenderer xpRenderer;

    public MrShepherdScreen(MrShepherdMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        addStepper(MrShepherdMenu.POPULATION_TAB, MrShepherdMenu.BUTTON_CAP_DOWN, false, FarmLayout.FIELD_MINUS_X, CAP_ROW_Y);
        addStepper(MrShepherdMenu.POPULATION_TAB, MrShepherdMenu.BUTTON_CAP_UP, true, FarmLayout.FIELD_PLUS_X, CAP_ROW_Y);
    }

    @Override
    protected void init() {
        super.init();
        xpRenderer = createGaugeRenderer(menu.BE.getXpTank().getCapacity());
    }

    @Override
    protected List<Tab> createExtraTabs() {
        return List.of(new Tab(Component.translatable("screen.dermicraft.farm.population_tab"), TEXT_COLOR));
    }

    @Override
    protected void renderHomeExtras(GuiGraphics guiGraphics, int x, int y) {
        renderSlotBackdrop(guiGraphics, x + MrShepherdMenu.XP_SLOT_X, y + MrShepherdMenu.XP_SLOT_Y);
        renderGauge(guiGraphics, xpRenderer, menu.BE.getXpTank().getFluid(), x + XP_GAUGE_X, y + XP_GAUGE_Y);
        for (int i = 0; i < MrShepherdMenu.FOOD_SLOT_COUNT; i++) {
            renderSlotBackdrop(guiGraphics, x + MrShepherdMenu.FOOD_ROW_X + i * FarmLayout.SLOT_SIZE, y + MrShepherdMenu.FOOD_ROW_Y);
        }
    }

    @Override
    protected void renderHomeLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.BE.getXpTank().getFluid(),
                XP_GAUGE_X + 1, XP_GAUGE_Y + 1, xpRenderer, Component.translatable("tooltip.dermicraft.gauge.xp"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, MrShepherdMenu.XP_SLOT_X + 1, MrShepherdMenu.XP_SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getXpTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.xp_container"));

        // The feed row and the output row are visually identical, so without these labels there's nothing telling the
        // player which is which.
        for (int i = 0; i < MrShepherdMenu.FOOD_SLOT_COUNT; i++) {
            renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                    MrShepherdMenu.FOOD_ROW_X + 1 + i * FarmLayout.SLOT_SIZE, MrShepherdMenu.FOOD_ROW_Y + 1,
                    menu.BE.getItemHandler(null).getStackInSlot(1 + i),
                    Component.translatable("tooltip.dermicraft.slot.animal_feed"));
        }
    }

    @Override
    protected void renderExtraTabLabels(GuiGraphics guiGraphics, int tab) {
        if (tab == MrShepherdMenu.POPULATION_TAB) {
            drawRowLabel(guiGraphics, Component.translatable("gui.dermicraft.mr_shepherd.cap", menu.getPopulationCap()), CAP_ROW_Y);
        }
    }
}
