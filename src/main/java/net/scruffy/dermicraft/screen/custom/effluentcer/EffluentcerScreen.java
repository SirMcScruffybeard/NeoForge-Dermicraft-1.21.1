package net.scruffy.dermicraft.screen.custom.effluentcer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;

public class EffluentcerScreen extends AbstractEvolvingMachineScreen<EffluentcerMenu> {

    // "Inline pair" layout: HP far-left, fuel far-right, the two ingredient tanks together
    // left of center, a single progress arrow right of center, result beyond it.
    private static final int INPUT_A_X = 40;
    private static final int INPUT_B_X = 60;
    private static final int ARROW_X = 86;
    private static final int ARROW_Y = 38;
    private static final int RESULT_X = 112;
    private static final int FUEL_X = 150;
    private static final int TANK_Y = 11;

    private FluidTankRenderer fuelRenderer;
    private FluidTankRenderer inputARenderer;
    private FluidTankRenderer inputBRenderer;
    private FluidTankRenderer resultRenderer;

    public EffluentcerScreen(EffluentcerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "effluentcer", "auto_drain");
    }

    @Override
    protected void init() {
        super.init();

        fuelRenderer = createFluidRenderer16x40(() -> menu.BE.getFuelTank().getCapacity());
        inputARenderer = createFluidRenderer16x40(() -> menu.BE.getInputATank().getCapacity());
        inputBRenderer = createFluidRenderer16x40(() -> menu.BE.getInputBTank().getCapacity());
        resultRenderer = createFluidRenderer16x40(() -> menu.BE.getResultTank().getCapacity());
    }

    // Auto-drain toggle -- sits directly above the crafting-progress arrow, in the empty column between the input
    // tanks and the result tank.
    @Override
    protected int getAutoDrainButtonX() {
        return ARROW_X;
    }

    @Override
    protected int getAutoDrainButtonY() {
        return TANK_Y;
    }

    @Override
    protected void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getInputATank().SLOT), INPUT_A_X + 1, TANK_Y + 1, inputARenderer,
                Component.translatable("tooltip.dermicraft.gauge.input_a"));

        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getInputBTank().SLOT), INPUT_B_X + 1, TANK_Y + 1, inputBRenderer,
                Component.translatable("tooltip.dermicraft.gauge.input_b"));

        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getResultTank().SLOT), RESULT_X + 1, TANK_Y + 1, resultRenderer,
                Component.translatable("tooltip.dermicraft.gauge.result"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, FUEL_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, INPUT_A_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getInputATank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.input_a_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, INPUT_B_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getInputBTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.input_b_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, RESULT_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getResultTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.result_container"));
    }

    @Override
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        renderTankAndSlot(guiGraphics, x + FUEL_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + INPUT_A_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + INPUT_B_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + RESULT_X, y + TANK_Y);

        // The arrow textures carry a 1px transparent margin, so the origin is offset by 1px to keep it aligned.
        renderProgressArrow(guiGraphics, x + ARROW_X, y + ARROW_Y);

        fuelRenderer.render(guiGraphics, x + FUEL_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
        inputARenderer.render(guiGraphics, x + INPUT_A_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getInputATank().SLOT));
        inputBRenderer.render(guiGraphics, x + INPUT_B_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getInputBTank().SLOT));
        resultRenderer.render(guiGraphics, x + RESULT_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getResultTank().SLOT));
    }
}
