package net.scruffy.dermicraft.screen.custom.skin_tank;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;

/**
 * The Skin Tank's screen on the shared evolution-machine base: it draws only its Main tab, the tall gauge centred
 * between the input and output container slots, each with a track-only arrow (a tank never crafts). The tabs, the
 * Module tab, the Evolution slot and the auto-drain button come from the base; there is no health bar.
 */
public class SkinTankScreen extends AbstractEvolvingMachineScreen<SkinTankMenu> {

    private static final String TANKS_DIR = "textures/gui/tanks/";

    private static final ResourceLocation TALL_TANK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tall_tank_guage.png");
    private static final int TALL_TANK_WIDTH = 18;
    private static final int TALL_TANK_HEIGHT = 66;

    private static final int TANK_X = 79;
    private static final int TANK_Y = 7;

    // Slots sit 1px further out than their logical slot position so the slot-to-arrow gap matches the
    // arrow-to-gauge gap (2px on each side), same as the Drooling Cauldron.
    private static final int INPUT_X = 42;
    private static final int OUTPUT_X = 116;
    private static final int SLOT_Y = 33;
    private static final int INPUT_ARROW_X = 61;
    private static final int OUTPUT_ARROW_X = 98;
    private static final int ARROW_Y = 37;

    // Right of the OUTPUT slot's arrow, above it, as before the migration.
    private static final int AUTO_DRAIN_BUTTON_X = 98;
    private static final int AUTO_DRAIN_BUTTON_Y = 11;

    private FluidTankRenderer fluidRenderer;

    public SkinTankScreen(SkinTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "skin_tank", "auto_drain");
    }

    @Override
    protected void init() {
        super.init();
        fluidRenderer = createFluidRenderer16x64(() -> menu.BE.getTank().getTankCapacity(0));
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
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TALL_TANK_TEXTURE, x + TANK_X, y + TANK_Y, 0, 0, TALL_TANK_WIDTH, TALL_TANK_HEIGHT,
                TALL_TANK_WIDTH, TALL_TANK_HEIGHT);

        renderItemSlot(guiGraphics, x + INPUT_X, y + SLOT_Y);
        renderItemSlot(guiGraphics, x + OUTPUT_X, y + SLOT_Y);

        // No crafting/progress concept on this machine, so both arrows are track-only.
        renderArrowTrack(guiGraphics, x + INPUT_ARROW_X, y + ARROW_Y);
        renderArrowTrack(guiGraphics, x + OUTPUT_ARROW_X, y + ARROW_Y);

        fluidRenderer.render(guiGraphics, x + TANK_X + 1, y + TANK_Y + 1, menu.BE.getFluid());
    }

    @Override
    protected void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.BE.getFluid(),
                TANK_X + 1, TANK_Y + 1, fluidRenderer);
    }
}
