package net.scruffy.dermicraft.screen.custom.render_furnace;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.block.entity.custom.RenderFurnaceBlockEntity;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;

/** Same layout convention as every other machine's screen -- input/arrow/output in the middle,
 * fuel tank (paired with its own bucket-fill slot) on the far right, HP bar on the far left. */
public class RenderFurnaceScreen extends AbstractEvolvingMachineScreen<RenderFurnaceMenu> {

    @Override
    protected net.minecraft.client.renderer.Rect2i getRecipeArrowArea() {
        return arrowAt(ARROW_X, ARROW_Y);
    }

    private static final int INPUT_X = 60;
    private static final int ARROW_X = 90;
    private static final int OUTPUT_X = 120;
    private static final int FUEL_X = 150;
    private static final int TANK_Y = 11;
    private static final int SLOT_Y = 34;
    private static final int ARROW_Y = 37;

    private FluidTankRenderer fuelRenderer;

    public RenderFurnaceScreen(RenderFurnaceMenu menu, Inventory playerInventory, Component title) {
        // Item output, so the button is the "auto dispense" wording of the fluid machines' auto drain.
        super(menu, playerInventory, title, "render_furnace", "auto_dispense");
    }

    @Override
    protected void init() {
        super.init();
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
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, FUEL_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, INPUT_X + 1, SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(RenderFurnaceBlockEntity.INPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.ingredient"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, OUTPUT_X + 1, SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(RenderFurnaceBlockEntity.OUTPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.result"));
    }

    @Override
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        renderItemSlot(guiGraphics, x + INPUT_X, y + SLOT_Y);
        renderProgressArrow(guiGraphics, x + ARROW_X, y + ARROW_Y);
        renderItemSlot(guiGraphics, x + OUTPUT_X, y + SLOT_Y);
        renderTankAndSlot(guiGraphics, x + FUEL_X, y + TANK_Y);

        fuelRenderer.render(guiGraphics, x + FUEL_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
    }
}
