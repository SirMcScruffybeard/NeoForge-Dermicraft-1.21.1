package net.scruffy.dermicraft.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;

/**
 * The screen shared by the Drooling family (Cauldron, Crucible, Geode): the tall fluid gauge in the middle, the
 * ingredient slot and its progress arrow on the left, the fluid-container slot on the right, and the auto-drain
 * button above the right arrow. No HP bar (the family has none) and no tolerance panel on its Module tab (it handles no
 * hazards). A machine's own screen class only names itself.
 *
 * @param <M> the machine's menu
 */
public abstract class DroolingMachineScreen<M extends DroolingMachineMenu<?>> extends AbstractEvolvingMachineScreen<M> {

    private static final ResourceLocation TALL_TANK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/tanks/tall_tank_guage.png");
    private static final int TALL_TANK_WIDTH = 18;
    private static final int TALL_TANK_HEIGHT = 66;

    private FluidTankRenderer fluidRenderer;

    @Override
    protected net.minecraft.client.renderer.Rect2i getRecipeArrowArea() {
        return arrowAt(61, 37);
    }

    protected DroolingMachineScreen(M menu, Inventory playerInventory, Component title, String screenName) {
        super(menu, playerInventory, title, screenName, "auto_drain");
    }

    @Override
    protected void init() {
        super.init();
        fluidRenderer = createFluidRenderer16x64(() -> menu.BE.getTank(null).getTankCapacity(0));
    }

    @Override
    protected boolean showsTolerances() {
        return false;
    }

    @Override
    protected int getAutoDrainButtonX() {
        return 98;
    }

    @Override
    protected int getAutoDrainButtonY() {
        return 11;
    }

    @Override
    protected void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.BE.getFluid(), 80, 8, fluidRenderer);
    }

    @Override
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TALL_TANK_TEXTURE, x + 79, y + 7, 0, 0, TALL_TANK_WIDTH, TALL_TANK_HEIGHT,
                TALL_TANK_WIDTH, TALL_TANK_HEIGHT);

        // Slots sit 1px further out than before so the slot-to-arrow gap matches the
        // arrow-to-gauge gap (2px on each side).
        renderItemSlot(guiGraphics, x + 42, y + 33);
        renderItemSlot(guiGraphics, x + 116, y + 33);

        // Left arrow sits in the gap between the input slot and the gauge, with the crafting progress; the right arrow
        // mirrors it in the equal-sized gap between the gauge and the container slot (track only, no progress fill).
        renderProgressArrow(guiGraphics, x + 61, y + 37);
        renderArrowTrack(guiGraphics, x + 98, y + 37);

        fluidRenderer.render(guiGraphics, x + 80, y + 8, menu.BE.getFluid());
    }
}
