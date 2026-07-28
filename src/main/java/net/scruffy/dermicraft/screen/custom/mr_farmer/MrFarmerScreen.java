package net.scruffy.dermicraft.screen.custom.mr_farmer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import org.jetbrains.annotations.NotNull;

public class MrFarmerScreen extends AbstractModScreen<MrFarmerMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    // Reuses the bottom 18x18 "slot" portion of the shared tank_and_slot asset as a generic slot
    // backdrop (no dedicated single-slot texture exists yet).
    private static final ResourceLocation TANK_AND_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tank_and_slot.png");
    private static final int TANK_AND_SLOT_WIDTH = 18;
    private static final int TANK_AND_SLOT_HEIGHT = 66;
    private static final int SLOT_CROP_SIZE = 18;
    private static final int SLOT_CROP_V_OFFSET = TANK_AND_SLOT_HEIGHT - SLOT_CROP_SIZE;

    // Long horizontal fuel gauge that fills left-to-right, sitting to the right of the fuel slot.
    private static final ResourceLocation LONG_TANK_GAUGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "long_tank_gauge.png");
    private static final int GAUGE_FRAME_WIDTH = 66;
    private static final int GAUGE_FRAME_HEIGHT = 18;
    private static final int GAUGE_INTERIOR_INSET = 1;  // 1px frame border around the fluid window
    private static final int GAUGE_INTERIOR_WIDTH = GAUGE_FRAME_WIDTH - 2 * GAUGE_INTERIOR_INSET;   // 64
    private static final int GAUGE_INTERIOR_HEIGHT = GAUGE_FRAME_HEIGHT - 2 * GAUGE_INTERIOR_INSET;  // 16

    // Visual backdrop position only -- the clickable slot hitbox lives in MrFarmerMenu and is
    // offset from this by 1px so the item sits centered in the backdrop art.
    private static final int FUEL_SLOT_X = 7;
    private static final int FUEL_SLOT_Y = 44;

    // Gauge sits just right of the fuel slot backdrop (ends at x=25) with a 5px gap.
    private static final int GAUGE_FRAME_X = 30;
    private static final int GAUGE_FRAME_Y = 44;
    private static final int GAUGE_INTERIOR_X = GAUGE_FRAME_X + GAUGE_INTERIOR_INSET;
    private static final int GAUGE_INTERIOR_Y = GAUGE_FRAME_Y + GAUGE_INTERIOR_INSET;

    private static final int BUFFER_SLOT_COUNT = 9;
    private static final int BUFFER_ROW_X = 7;
    private static final int BUFFER_ROW_Y = 64;

    // Top-left anchor of the range grid (grid_square tiled n x n, grows down-right with fuel-tier range).
    private static final int RANGE_GRID_X = 105;
    private static final int RANGE_GRID_Y = 15;

    // "Range: NxN" text readout, left edge aligned with the fuel gauge's left edge (GAUGE_FRAME_X).
    private static final int RANGE_TEXT_X = GAUGE_FRAME_X;
    private static final int RANGE_TEXT_Y = 18;
    private static final int RANGE_TEXT_COLOR = 0x007F0E; // matches the GUI's green accent

    private FluidTankRenderer fuelRenderer;

    public MrFarmerScreen(MrFarmerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        fuelRenderer = new FluidTankRenderer(menu.BE.getFuelTank().getCapacity(), true,
                GAUGE_INTERIOR_WIDTH, GAUGE_INTERIOR_HEIGHT, FluidTankRenderer.Orientation.HORIZONTAL);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFuelTank().getFluid(), GAUGE_INTERIOR_X, GAUGE_INTERIOR_Y, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        // Slot role labels, matching Mr. Shepard. Only shown while a slot is empty; once it holds an
        // item vanilla's own item tooltip covers it.
        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, FUEL_SLOT_X + 1, FUEL_SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                    BUFFER_ROW_X + 1 + i * 18, BUFFER_ROW_Y + 1,
                    menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT + 1 + i),
                    Component.translatable("tooltip.dermicraft.slot.output_buffer"));
        }

        guiGraphics.drawString(this.font, Component.translatable("gui.dermicraft.mr_farmer.range", menu.getRange()),
                RANGE_TEXT_X, RANGE_TEXT_Y, RANGE_TEXT_COLOR, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight,
                BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
        renderPlayerInventoryBackdrop(guiGraphics, x, y);

        renderSlotBackdrop(guiGraphics, x + FUEL_SLOT_X, y + FUEL_SLOT_Y);
        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            renderSlotBackdrop(guiGraphics, x + BUFFER_ROW_X + i * 18, y + BUFFER_ROW_Y);
        }

        // Fluid fill first, then the frame on top -- the frame's window is transparent so the
        // fill shows through, and an empty tank still shows the frame (no "hole" in the layout).
        fuelRenderer.render(guiGraphics, x + GAUGE_INTERIOR_X, y + GAUGE_INTERIOR_Y, menu.BE.getFuelTank().getFluid());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(LONG_TANK_GAUGE_TEXTURE, x + GAUGE_FRAME_X, y + GAUGE_FRAME_Y, 0, 0,
                GAUGE_FRAME_WIDTH, GAUGE_FRAME_HEIGHT, GAUGE_FRAME_WIDTH, GAUGE_FRAME_HEIGHT);

        renderRangeGrid(guiGraphics, x + RANGE_GRID_X, y + RANGE_GRID_Y, menu.getRange());
    }

    private void renderSlotBackdrop(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x, y, 0, SLOT_CROP_V_OFFSET, SLOT_CROP_SIZE, SLOT_CROP_SIZE,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
    }
}
