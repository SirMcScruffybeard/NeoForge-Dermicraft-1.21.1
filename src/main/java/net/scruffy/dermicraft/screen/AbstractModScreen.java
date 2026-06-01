package net.scruffy.dermicraft.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.util.MouseUtil;

import java.util.Optional;

public abstract class AbstractModScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public AbstractModScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // Gets rid of title and inventory title
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    /************************************************************************************
     *
     * @param capacity
     * @param width
     * @param height
     * @return
     ************************************************************************************/
    protected FluidTankRenderer createFluidRenderer(int capacity, int width, int height) {
        return new FluidTankRenderer(capacity, true, width, height);
        //width and height are measured from the inside of the "fluid gauge"
    }

    /************************************************************************************
     *
     * @param capacity
     * @return fluid renderer that is 16px X 64px.
     *      Used by DroolingCauldronScreen, SkinTankScreen
     *************************************************************************************/
    protected FluidTankRenderer createFluidRenderer16x64(int capacity) {
        return createFluidRenderer(capacity, 16, 64);
    }

    /************************************************************************************
     *
     * @param capacity
     * @return fluid renderer that is 16px X 40px.
     *      Used by MasticatorScreen
     *************************************************************************************/
    protected FluidTankRenderer createFluidRenderer16x40(int capacity) {
        return createFluidRenderer(capacity, 16, 40);
    }

    protected void renderFluidTooltipArea(GuiGraphics guiGraphics, int pMouseX, int pMouseY, int x, int y,
                                          FluidStack stack, int offsetX, int offsetY, FluidTankRenderer renderer) {
        if (isMouseAboveArea(pMouseX, pMouseY, x, y, offsetX, offsetY, renderer)) {
            guiGraphics.renderTooltip(this.font, renderer.getTooltip(stack, TooltipFlag.Default.NORMAL),
                    Optional.empty(), pMouseX - x, pMouseY - y);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    public static boolean isMouseAboveArea(int pMouseX, int pMouseY, int x, int y, int offsetX, int offsetY, FluidTankRenderer renderer) {
        return MouseUtil.isMouseOver(pMouseX, pMouseY, x + offsetX, y + offsetY, renderer.getWidth(), renderer.getHeight());
    }
}
