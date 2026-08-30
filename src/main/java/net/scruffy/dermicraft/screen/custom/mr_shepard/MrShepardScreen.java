package net.scruffy.dermicraft.screen.custom.mr_shepard;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.NotNull;

public class MrShepardScreen extends AbstractModScreen<MrShepardMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation TANK_AND_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tank_and_slot.png");
    private static final int TANK_AND_SLOT_WIDTH = 18;
    private static final int TANK_AND_SLOT_HEIGHT = 66;
    private static final int SLOT_CROP_SIZE = 18;
    private static final int SLOT_CROP_V_OFFSET = TANK_AND_SLOT_HEIGHT - SLOT_CROP_SIZE;

    private static final ResourceLocation LONG_TANK_GAUGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "long_tank_gauge.png");
    private static final int GAUGE_FRAME_WIDTH = 66;
    private static final int GAUGE_FRAME_HEIGHT = 18;
    private static final int GAUGE_INTERIOR_INSET = 1;
    private static final int GAUGE_INTERIOR_WIDTH = GAUGE_FRAME_WIDTH - 2 * GAUGE_INTERIOR_INSET;
    private static final int GAUGE_INTERIOR_HEIGHT = GAUGE_FRAME_HEIGHT - 2 * GAUGE_INTERIOR_INSET;

    // XP-gathering strip -- a dedicated vertical panel (side_column.png, already authored 31x166,
    // flush with the main panel's own height), placed alongside it rather than below (screen grows
    // wider, not taller -- same "grow imageWidth to fit an extra panel" trick WorkbenchScreen's own
    // imageHeight override established, just the other axis).
    private static final ResourceLocation XP_STRIP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "side_column.png");
    private static final int XP_STRIP_WIDTH = 31;
    private static final int XP_STRIP_HEIGHT = 166;

    private static final int MAIN_PANEL_WIDTH = 176;
    private static final int MAIN_PANEL_HEIGHT = 166;
    private static final int XP_STRIP_GAP = 4;
    private static final int XP_STRIP_X = MAIN_PANEL_WIDTH + XP_STRIP_GAP;
    private static final int XP_STRIP_Y = 0;

    // XP tank+slot column -- reuses the same tank_and_slot art every single-tank machine screen
    // uses (Masticator's ingredient/result columns, etc.), centered horizontally in the 31-wide
    // strip ((31-18)/2 = 6). Positioned so the SLOT half (the bottom 18px of the 66-tall art, see
    // SLOT_CROP_V_OFFSET) sits flush with the buffer row's own top -- the slot is the part that
    // visually reads as "a row", so that's what should line up, not the tank half above it.
    // Gauge/slot offsets mirror Masticator's own "+1 for the gauge" pattern.
    private static final int XP_COLUMN_X = XP_STRIP_X + 6;
    private static final int XP_COLUMN_Y = 64 - SLOT_CROP_V_OFFSET; // 64 == BUFFER_ROW_Y (declared below, can't forward-ref it)
    private static final int XP_GAUGE_X = XP_COLUMN_X + 1;
    private static final int XP_GAUGE_Y = XP_COLUMN_Y + 1;
    private static final int XP_SLOT_BACKDROP_X = XP_COLUMN_X + 1;
    private static final int XP_SLOT_BACKDROP_Y = XP_COLUMN_Y + SLOT_CROP_V_OFFSET;

    // Fuel slot + gauge sit on the same row as the 4-slot food row (y=44). Gauge butts up close
    // to the slot backdrop (which ends at x=25) and ends at x=92, just left of the food row.
    private static final int FUEL_SLOT_X = 7;
    private static final int FUEL_SLOT_Y = 44;
    private static final int GAUGE_FRAME_X = 26;
    private static final int GAUGE_FRAME_Y = 44;
    private static final int GAUGE_INTERIOR_X = GAUGE_FRAME_X + GAUGE_INTERIOR_INSET;
    private static final int GAUGE_INTERIOR_Y = GAUGE_FRAME_Y + GAUGE_INTERIOR_INSET;

    private static final int FOOD_SLOT_COUNT = 4;
    // Right-aligned with the 9-slot buffer row below it: buffer spans x=7..169, so a 4-slot
    // (72px) row starts at 169 - 72 = 97.
    private static final int FOOD_ROW_X = 97;
    private static final int FOOD_ROW_Y = 44;

    private static final int BUFFER_SLOT_COUNT = 9;
    private static final int BUFFER_ROW_X = 7;
    private static final int BUFFER_ROW_Y = 64;

    private static final int RANGE_GRID_X = 105;
    private static final int RANGE_GRID_Y = 15;
    private static final int RANGE_TEXT_X = 7;
    private static final int RANGE_TEXT_Y = 6;
    private static final int RANGE_TEXT_COLOR = 0x007F0E;

    // Cap readout sits on its own row below the range line, vertically centered against the
    // 18px stepper buttons that share that row.
    private static final int CAP_TEXT_X = 7;
    private static final int CAP_TEXT_Y = 20;

    // Population-cap steppers. Momentary, not toggles: clicking shows the _pressed art for a few
    // ticks as click feedback, then it reverts to the normal texture (see capPressTicks below).
    private static final ResourceLocation PLUS_BUTTON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "plus_button.png");
    private static final ResourceLocation PLUS_BUTTON_PRESSED =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "plus_button_pressed.png");
    private static final ResourceLocation MINUS_BUTTON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "minus_button.png");
    private static final ResourceLocation MINUS_BUTTON_PRESSED =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "minus_button_pressed.png");
    private static final int CAP_BUTTON_SIZE = 18;
    // "Cap: 64" at the widest is ~36px from CAP_TEXT_X=7, ending at x=43; +10px gap = 53.
    private static final int CAP_BUTTON_UP_X = 53;
    private static final int CAP_BUTTON_DOWN_X = 72;
    private static final int CAP_BUTTON_Y = CAP_TEXT_Y; // tops flush with the cap readout
    private static final int CAP_PRESS_FLASH_TICKS = 4; // how long the _pressed art stays up

    // Click for precision, hold to accelerate. The delay before the value starts running is what
    // preserves precision -- a short hold still reads as one deliberate click rather than racing
    // past the target -- while a long hold spares the player 30 clicks to cross the whole range.
    private static final int HOLD_DELAY_TICKS = 40;  // ~2s before it starts running
    private static final int HOLD_REPEAT_TICKS = 2;  // then one step every 2 ticks

    private int plusPressTicks = 0;
    private int minusPressTicks = 0;
    private int heldButton = -1; // -1 = nothing held, else the BUTTON_CAP_* id being held
    private int holdTicks = 0;

    private FluidTankRenderer fuelRenderer;
    private FluidTankRenderer xpRenderer;

    public MrShepardScreen(MrShepardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = XP_STRIP_X + XP_STRIP_WIDTH;
        this.imageHeight = Math.max(MAIN_PANEL_HEIGHT, XP_STRIP_Y + XP_STRIP_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        fuelRenderer = new FluidTankRenderer(menu.BE.getFuelTank().getCapacity(), true,
                GAUGE_INTERIOR_WIDTH, GAUGE_INTERIOR_HEIGHT, FluidTankRenderer.Orientation.HORIZONTAL);
        xpRenderer = createFluidRenderer16x40(menu.BE.getXpTank().getCapacity());
    }

    /** Counts down the press flash, and runs the value while a stepper is held past the delay. */
    @Override
    protected void containerTick() {
        super.containerTick();
        if (plusPressTicks > 0) plusPressTicks--;
        if (minusPressTicks > 0) minusPressTicks--;

        if (heldButton < 0) return;
        holdTicks++;
        int elapsedPastDelay = holdTicks - HOLD_DELAY_TICKS;
        if (elapsedPastDelay > 0 && elapsedPastDelay % HOLD_REPEAT_TICKS == 0) {
            pressCapButton(heldButton);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + CAP_BUTTON_UP_X, y + CAP_BUTTON_Y,
                CAP_BUTTON_SIZE, CAP_BUTTON_SIZE)) {
            beginHold(MrShepardMenu.BUTTON_CAP_UP);
            return true;
        }
        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + CAP_BUTTON_DOWN_X, y + CAP_BUTTON_Y,
                CAP_BUTTON_SIZE, CAP_BUTTON_SIZE)) {
            beginHold(MrShepardMenu.BUTTON_CAP_DOWN);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        heldButton = -1;
        holdTicks = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Fires the step immediately (so a plain click always lands one), then arms the hold timer. */
    private void beginHold(int buttonId) {
        pressCapButton(buttonId);
        heldButton = buttonId;
        holdTicks = 0;
    }

    private void pressCapButton(int buttonId) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        if (buttonId == MrShepardMenu.BUTTON_CAP_UP) {
            plusPressTicks = CAP_PRESS_FLASH_TICKS;
        } else {
            minusPressTicks = CAP_PRESS_FLASH_TICKS;
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFuelTank().getFluid(), GAUGE_INTERIOR_X, GAUGE_INTERIOR_Y, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getXpTank().getFluid(), XP_GAUGE_X, XP_GAUGE_Y, xpRenderer,
                Component.translatable("tooltip.dermicraft.gauge.xp"));

        // Slot role labels -- the feed row and the output row are visually identical, so without
        // these there's nothing telling the player which is which. Only shown while a slot is empty;
        // once it holds an item vanilla's own item tooltip covers it.
        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, FUEL_SLOT_X + 1, FUEL_SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, XP_SLOT_BACKDROP_X, XP_SLOT_BACKDROP_Y,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getXpTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.xp_container"));

        for (int i = 0; i < FOOD_SLOT_COUNT; i++) {
            renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                    FOOD_ROW_X + 1 + i * 18, FOOD_ROW_Y + 1,
                    menu.BE.getItemHandler(null).getStackInSlot(1 + i),
                    Component.translatable("tooltip.dermicraft.slot.animal_feed"));
        }

        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                    BUFFER_ROW_X + 1 + i * 18, BUFFER_ROW_Y + 1,
                    menu.BE.getItemHandler(null).getStackInSlot(1 + FOOD_SLOT_COUNT + i),
                    Component.translatable("tooltip.dermicraft.slot.output_buffer"));
        }

        guiGraphics.drawString(this.font, Component.translatable("gui.dermicraft.mr_shepard.range", menu.getRange()),
                RANGE_TEXT_X, RANGE_TEXT_Y, RANGE_TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.dermicraft.mr_shepard.cap", menu.getPopulationCap()),
                CAP_TEXT_X, CAP_TEXT_Y, RANGE_TEXT_COLOR, false);
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

        guiGraphics.blit(XP_STRIP_TEXTURE, x + XP_STRIP_X, y + XP_STRIP_Y, 0, 0,
                XP_STRIP_WIDTH, XP_STRIP_HEIGHT, XP_STRIP_WIDTH, XP_STRIP_HEIGHT);
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + XP_COLUMN_X, y + XP_COLUMN_Y, 0, 0,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
        xpRenderer.render(guiGraphics, x + XP_GAUGE_X, y + XP_GAUGE_Y, menu.BE.getXpTank().getFluid());

        renderSlotBackdrop(guiGraphics, x + FUEL_SLOT_X, y + FUEL_SLOT_Y);
        for (int i = 0; i < FOOD_SLOT_COUNT; i++) {
            renderSlotBackdrop(guiGraphics, x + FOOD_ROW_X + i * 18, y + FOOD_ROW_Y);
        }
        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            renderSlotBackdrop(guiGraphics, x + BUFFER_ROW_X + i * 18, y + BUFFER_ROW_Y);
        }

        fuelRenderer.render(guiGraphics, x + GAUGE_INTERIOR_X, y + GAUGE_INTERIOR_Y, menu.BE.getFuelTank().getFluid());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(LONG_TANK_GAUGE_TEXTURE, x + GAUGE_FRAME_X, y + GAUGE_FRAME_Y, 0, 0,
                GAUGE_FRAME_WIDTH, GAUGE_FRAME_HEIGHT, GAUGE_FRAME_WIDTH, GAUGE_FRAME_HEIGHT);

        renderRangeGrid(guiGraphics, x + RANGE_GRID_X, y + RANGE_GRID_Y, menu.getRange());

        renderCapButton(guiGraphics, x + CAP_BUTTON_UP_X, y + CAP_BUTTON_Y,
                plusPressTicks > 0 ? PLUS_BUTTON_PRESSED : PLUS_BUTTON);
        renderCapButton(guiGraphics, x + CAP_BUTTON_DOWN_X, y + CAP_BUTTON_Y,
                minusPressTicks > 0 ? MINUS_BUTTON_PRESSED : MINUS_BUTTON);
    }

    private void renderCapButton(GuiGraphics guiGraphics, int x, int y, ResourceLocation texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(texture, x, y, 0, 0, CAP_BUTTON_SIZE, CAP_BUTTON_SIZE, CAP_BUTTON_SIZE, CAP_BUTTON_SIZE);
    }

    private void renderSlotBackdrop(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x, y, 0, SLOT_CROP_V_OFFSET, SLOT_CROP_SIZE, SLOT_CROP_SIZE,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
    }
}
