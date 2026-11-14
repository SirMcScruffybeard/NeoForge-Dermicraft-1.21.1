package net.scruffy.dermicraft.screen.custom.masticator;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;

public class MasticatorScreen extends AbstractEvolvingMachineScreen<MasticatorMenu> {

    @Override
    protected net.minecraft.client.renderer.Rect2i getRecipeArrowArea() {
        return arrowAt(95, 38);
    }

    private FluidTankRenderer fuelRenderer;
    private FluidTankRenderer ingredientRenderer;
    private FluidTankRenderer resultRenderer;

    // Auto-drain toggle, directly above the crafting-progress arrow (x+95), in the empty column between the
    // ingredient item slot and the result tank.
    private static final int AUTO_DRAIN_BUTTON_X = 95;
    private static final int AUTO_DRAIN_BUTTON_Y = 11;

    public MasticatorScreen(MasticatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "masticator", "auto_drain");
    }

    @Override
    protected void init() {
        super.init();
        fuelRenderer = createFluidRenderer16x40(() -> menu.BE.getFuelTank().getCapacity());
        ingredientRenderer = createFluidRenderer16x40(() -> menu.BE.getIngredientTank().getCapacity());
        resultRenderer = createFluidRenderer16x40(() -> menu.BE.getResultTank().getCapacity());
    }

    @Override
    protected int getAutoDrainButtonX() {
        return AUTO_DRAIN_BUTTON_X;
    }

    @Override
    protected int getAutoDrainButtonY() {
        return AUTO_DRAIN_BUTTON_Y;
    }

    @Override
    protected void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), 151, 12, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getIngredientTank().SLOT), 67, 12, ingredientRenderer,
                Component.translatable("tooltip.dermicraft.gauge.reagent"));

        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getResultTank().SLOT), 123, 12, resultRenderer,
                Component.translatable("tooltip.dermicraft.gauge.result"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, 151, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, 67, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getIngredientTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.reagent_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, 123, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getResultTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.result_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, 38, 35,
                menu.BE.getItemHandler(null).getStackInSlot(MasticatorBlockEntity.INGREDIENT_ITEM_SLOT),
                Component.translatable("tooltip.dermicraft.slot.ingredient"));
    }

    @Override
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        renderTankAndSlot(guiGraphics, x + 150, y + 11);
        renderTankAndSlot(guiGraphics, x + 66, y + 11);
        renderTankAndSlot(guiGraphics, x + 122, y + 11);
        // Solid-ingredient slot backdrop between the HP bar and the reagent gauge, vertically centred on the arrow.
        renderItemSlot(guiGraphics, x + 37, y + 34);

        // The arrow textures carry a 1px transparent margin, so the origin is offset by 1px.
        renderProgressArrow(guiGraphics, x + 95, y + 38);

        fuelRenderer.render(guiGraphics, x + 151, y + 12, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
        ingredientRenderer.render(guiGraphics, x + 67, y + 12, menu.BE.getFluid(menu.BE.getIngredientTank().SLOT));
        resultRenderer.render(guiGraphics, x + 123, y + 12, menu.BE.getFluid(menu.BE.getResultTank().SLOT));
    }
}
