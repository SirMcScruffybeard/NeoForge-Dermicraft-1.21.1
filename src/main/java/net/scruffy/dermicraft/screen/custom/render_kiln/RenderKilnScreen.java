package net.scruffy.dermicraft.screen.custom.render_kiln;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.block.entity.custom.RenderKilnBlockEntity;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;

public class RenderKilnScreen extends AbstractEvolvingMachineScreen<RenderKilnMenu> {

    // No pattern slot on the Kiln -- HP (far left) and Fuel (far right) stay pinned to the edges
    // (same "inline pair" convention as the Effluentcer screen), while input/arrow/output cluster
    // together near center instead of spreading across the Metastasizer's original 4-column spacing.
    private static final int INPUT_X = 54;
    private static final int ARROW_X = 78;
    private static final int OUTPUT_X = 100;
    private static final int FUEL_X = 150;
    private static final int TANK_Y = 11;
    private static final int SLOT_Y = 34;
    private static final int ARROW_Y = 37;

    private FluidTankRenderer inputRenderer;
    private FluidTankRenderer fuelRenderer;

    public RenderKilnScreen(RenderKilnMenu menu, Inventory playerInventory, Component title) {
        // Item output, so the button is the "auto dispense" wording of the fluid machines' auto drain.
        super(menu, playerInventory, title, "render_kiln", "auto_dispense");
    }

    @Override
    protected void init() {
        super.init();
        inputRenderer = createFluidRenderer16x40(() -> menu.BE.getInputTank().getCapacity());
        fuelRenderer = createFluidRenderer16x40(() -> menu.BE.getFuelTank().getCapacity());
    }

    // Directly above the crafting-progress arrow.
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
                menu.BE.getFluid(menu.BE.getInputTank().SLOT), INPUT_X + 1, TANK_Y + 1, inputRenderer,
                Component.translatable("tooltip.dermicraft.gauge.input"));

        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, INPUT_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getInputTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.input_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, FUEL_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, OUTPUT_X + 1, SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(RenderKilnBlockEntity.OUTPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.result"));
    }

    @Override
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        renderTankAndSlot(guiGraphics, x + INPUT_X, y + TANK_Y);
        renderProgressArrow(guiGraphics, x + ARROW_X, y + ARROW_Y);
        renderItemSlot(guiGraphics, x + OUTPUT_X, y + SLOT_Y);
        renderTankAndSlot(guiGraphics, x + FUEL_X, y + TANK_Y);

        inputRenderer.render(guiGraphics, x + INPUT_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getInputTank().SLOT));
        fuelRenderer.render(guiGraphics, x + FUEL_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
    }
}
