package net.scruffy.dermicraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The screen shared by the farm machines: a Home tab (the "Field: W x L" readout, the fuel slot and gauge, the output
 * buffer, and whatever a machine adds through {@link #renderHomeExtras}) and a Field tab (the width and length steppers
 * and the preview of the field), plus any tabs a machine adds. The tabs are the shared tab bar on the screen's left edge.
 *
 * <p>Plus and minus buttons are registered per tab with {@link #addStepper}; a click fires once at once and, held past a
 * delay, then repeats, so a plain click lands one step and a long hold crosses the whole range. The press shows the
 * pressed art for a few ticks as feedback.
 */
public abstract class AbstractFarmScreen<M extends AbstractFarmMenu<?>> extends AbstractModScreen<M> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";

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

    // Long horizontal gauge that fills left-to-right, sitting to the right of a container slot.
    private static final ResourceLocation LONG_TANK_GAUGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "long_tank_gauge.png");
    protected static final int GAUGE_FRAME_WIDTH = 66;
    protected static final int GAUGE_FRAME_HEIGHT = 18;
    private static final int GAUGE_INTERIOR_INSET = 1;  // 1px frame border around the fluid window
    protected static final int GAUGE_INTERIOR_WIDTH = GAUGE_FRAME_WIDTH - 2 * GAUGE_INTERIOR_INSET;   // 64
    protected static final int GAUGE_INTERIOR_HEIGHT = GAUGE_FRAME_HEIGHT - 2 * GAUGE_INTERIOR_INSET;  // 16

    // Plus and minus steppers: momentary, not toggles -- clicking shows the _pressed art for a few ticks as feedback.
    private static final ResourceLocation PLUS_BUTTON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "plus_button.png");
    private static final ResourceLocation PLUS_BUTTON_PRESSED =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "plus_button_pressed.png");
    private static final ResourceLocation MINUS_BUTTON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "minus_button.png");
    private static final ResourceLocation MINUS_BUTTON_PRESSED =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "minus_button_pressed.png");
    protected static final int STEPPER_SIZE = 18;
    private static final int PRESS_FLASH_TICKS = 4; // how long the _pressed art stays up

    // Click for precision, hold to accelerate. The delay before the value starts running is what preserves precision --
    // a short hold still reads as one deliberate click rather than racing past the target -- while a long hold spares
    // the player many clicks to cross the whole range.
    private static final int HOLD_DELAY_TICKS = 40;  // ~2s before it starts running
    private static final int HOLD_REPEAT_TICKS = 2;  // then one step every 2 ticks

    protected static final int TEXT_COLOR = 0x007F0E; // matches the GUI's green accent

    /** A plus or minus button on one tab, wired to a menu button id. */
    private record Stepper(int tab, int buttonId, boolean plus, int x, int y) {}

    private final List<Stepper> steppers = new ArrayList<>();
    private final Map<Integer, Integer> pressFlash = new HashMap<>(); // button id -> ticks of pressed art left
    private int heldButton = -1; // -1 = nothing held, else the menu button id being held
    private int holdTicks = 0;

    private List<Tab> tabs = List.of();
    protected FluidTankRenderer fuelRenderer;

    protected AbstractFarmScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        addStepper(AbstractFarmMenu.FIELD_TAB, AbstractFarmMenu.BUTTON_WIDTH_DOWN, false,
                FarmLayout.FIELD_MINUS_X, FarmLayout.FIELD_WIDTH_ROW_Y);
        addStepper(AbstractFarmMenu.FIELD_TAB, AbstractFarmMenu.BUTTON_WIDTH_UP, true,
                FarmLayout.FIELD_PLUS_X, FarmLayout.FIELD_WIDTH_ROW_Y);
        addStepper(AbstractFarmMenu.FIELD_TAB, AbstractFarmMenu.BUTTON_LENGTH_DOWN, false,
                FarmLayout.FIELD_MINUS_X, FarmLayout.FIELD_LENGTH_ROW_Y);
        addStepper(AbstractFarmMenu.FIELD_TAB, AbstractFarmMenu.BUTTON_LENGTH_UP, true,
                FarmLayout.FIELD_PLUS_X, FarmLayout.FIELD_LENGTH_ROW_Y);
    }

    // ---- What a machine supplies -------------------------------------------------------------------

    /** The tabs after Home and Field (their content is the machine's own, see {@link #renderExtraTabBg}). */
    protected List<Tab> createExtraTabs() {
        return List.of();
    }

    /** Draws the machine's own Home-tab backdrops (its extra slots, gauges). Runs after the shared Home art. */
    protected void renderHomeExtras(GuiGraphics guiGraphics, int x, int y) {
    }

    /** Draws the machine's Home-tab tooltips and text (labels relative to the panel, tooltip areas to the screen). */
    protected void renderHomeLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
    }

    /** Draws an extra tab's backdrop. {@code tab} is its index in the tab bar. */
    protected void renderExtraTabBg(GuiGraphics guiGraphics, int x, int y, int tab) {
    }

    /** Draws an extra tab's text (relative to the panel). */
    protected void renderExtraTabLabels(GuiGraphics guiGraphics, int tab) {
    }

    // ---- Shared drawing helpers ------------------------------------------------------------------------

    protected void addStepper(int tab, int buttonId, boolean plus, int x, int y) {
        steppers.add(new Stepper(tab, buttonId, plus, x, y));
    }

    protected void renderSlotBackdrop(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x, y, 0, SLOT_CROP_V_OFFSET, SLOT_CROP_SIZE, SLOT_CROP_SIZE,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
    }

    /** A horizontal fluid gauge: the fill first, then the frame on top (the frame's window is transparent, so the fill
     * shows through and an empty tank still shows the frame). {@code x}, {@code y} are the frame's screen position. */
    protected void renderGauge(GuiGraphics guiGraphics, FluidTankRenderer renderer,
                               net.neoforged.neoforge.fluids.FluidStack fluid, int x, int y) {
        renderer.render(guiGraphics, x + GAUGE_INTERIOR_INSET, y + GAUGE_INTERIOR_INSET, fluid);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(LONG_TANK_GAUGE_TEXTURE, x, y, 0, 0,
                GAUGE_FRAME_WIDTH, GAUGE_FRAME_HEIGHT, GAUGE_FRAME_WIDTH, GAUGE_FRAME_HEIGHT);
    }

    protected FluidTankRenderer createGaugeRenderer(int capacity) {
        return new FluidTankRenderer(capacity, true, GAUGE_INTERIOR_WIDTH, GAUGE_INTERIOR_HEIGHT,
                FluidTankRenderer.Orientation.HORIZONTAL);
    }

    // ---- Lifecycle -----------------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        List<Tab> all = new ArrayList<>();
        all.add(new Tab(Component.translatable("screen.dermicraft.farm.home_tab"), TEXT_COLOR));
        all.add(new Tab(Component.translatable("screen.dermicraft.farm.field_tab"), TEXT_COLOR));
        all.addAll(createExtraTabs());
        tabs = all;
        fuelRenderer = createGaugeRenderer(menu.BE.getFuelTank().getCapacity());
    }

    @Override
    public Rect2i getTabStripArea() {
        return tabs.isEmpty() ? null : getStackedTabArea(tabs.size());
    }

    /** Counts down the press flash, and runs the value while a stepper is held past the delay. */
    @Override
    protected void containerTick() {
        super.containerTick();
        pressFlash.replaceAll((id, ticks) -> Math.max(0, ticks - 1));

        if (heldButton < 0) return;
        holdTicks++;
        int elapsedPastDelay = holdTicks - HOLD_DELAY_TICKS;
        if (elapsedPastDelay > 0 && elapsedPastDelay % HOLD_REPEAT_TICKS == 0) {
            press(heldButton);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int clickedTab = tabClickedAt(mouseX, mouseY, x, y, tabs.size());
        if (clickedTab >= 0 && clickedTab != menu.getActiveTab()) {
            menu.setActiveTab(clickedTab);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AbstractModMenu.TAB_BUTTON_BASE + clickedTab);
            return true;
        }

        for (Stepper stepper : steppers) {
            if (stepper.tab() == menu.getActiveTab() && MouseUtil.isMouseOver((int) mouseX, (int) mouseY,
                    x + stepper.x(), y + stepper.y(), STEPPER_SIZE, STEPPER_SIZE)) {
                press(stepper.buttonId()); // fires at once, so a plain click always lands one step
                heldButton = stepper.buttonId();
                holdTicks = 0;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        heldButton = -1;
        holdTicks = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void press(int buttonId) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        pressFlash.put(buttonId, PRESS_FLASH_TICKS);
    }

    // ---- Rendering --------------------------------------------------------------------------------------

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight,
                BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
        renderPlayerInventoryBackdrop(guiGraphics, x, y);
        renderTabs(guiGraphics, x, y, tabs, menu.getActiveTab());

        int tab = menu.getActiveTab();
        if (tab == AbstractFarmMenu.HOME_TAB) {
            renderSlotBackdrop(guiGraphics, x + FarmLayout.FUEL_SLOT_X, y + FarmLayout.FUEL_SLOT_Y);
            for (int i = 0; i < menu.BE.getBufferSlotCount(); i++) {
                renderSlotBackdrop(guiGraphics, x + FarmLayout.BUFFER_ROW_X + i * FarmLayout.SLOT_SIZE, y + FarmLayout.BUFFER_ROW_Y);
            }
            renderGauge(guiGraphics, fuelRenderer, menu.BE.getFuelTank().getFluid(),
                    x + FarmLayout.FUEL_GAUGE_X, y + FarmLayout.FUEL_GAUGE_Y);
            renderHomeExtras(guiGraphics, x, y);
        } else if (tab == AbstractFarmMenu.FIELD_TAB) {
            renderFieldGrid(guiGraphics, x + FarmLayout.FIELD_GRID_X, y + FarmLayout.FIELD_GRID_Y,
                    menu.BE.getFieldWidth(), menu.BE.getFieldLength(),
                    AbstractFarmBlockEntity.MAX_FIELD_SIZE, FarmLayout.FIELD_GRID_CELL);
        } else {
            renderExtraTabBg(guiGraphics, x, y, tab);
        }

        for (Stepper stepper : steppers) {
            if (stepper.tab() != tab) continue;
            boolean pressed = pressFlash.getOrDefault(stepper.buttonId(), 0) > 0;
            ResourceLocation texture = stepper.plus()
                    ? (pressed ? PLUS_BUTTON_PRESSED : PLUS_BUTTON)
                    : (pressed ? MINUS_BUTTON_PRESSED : MINUS_BUTTON);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(texture, x + stepper.x(), y + stepper.y(), 0, 0,
                    STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int tab = menu.getActiveTab();

        if (tab == AbstractFarmMenu.HOME_TAB) {
            guiGraphics.drawString(this.font, Component.translatable("gui.dermicraft.farm.field",
                            menu.BE.getFieldWidth(), menu.BE.getFieldLength()),
                    FarmLayout.READOUT_X, FarmLayout.READOUT_Y, TEXT_COLOR, false);

            renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.BE.getFuelTank().getFluid(),
                    FarmLayout.FUEL_GAUGE_X + GAUGE_INTERIOR_INSET, FarmLayout.FUEL_GAUGE_Y + GAUGE_INTERIOR_INSET, fuelRenderer,
                    Component.translatable("tooltip.dermicraft.gauge.fuel"));

            // Slot role labels. Only shown while a slot is empty; once it holds an item vanilla's own item tooltip covers it.
            renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, FarmLayout.FUEL_SLOT_X + 1, FarmLayout.FUEL_SLOT_Y + 1,
                    menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                    Component.translatable("tooltip.dermicraft.slot.fuel_container"));
            for (int i = 0; i < menu.BE.getBufferSlotCount(); i++) {
                renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                        FarmLayout.BUFFER_ROW_X + 1 + i * FarmLayout.SLOT_SIZE, FarmLayout.BUFFER_ROW_Y + 1,
                        menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getBufferStartSlot() + i),
                        Component.translatable("tooltip.dermicraft.slot.output_buffer"));
            }
            renderHomeLabels(guiGraphics, mouseX, mouseY, x, y);
        } else if (tab == AbstractFarmMenu.FIELD_TAB) {
            drawRowLabel(guiGraphics, Component.translatable("gui.dermicraft.farm.width", menu.BE.getFieldWidth()),
                    FarmLayout.FIELD_WIDTH_ROW_Y);
            drawRowLabel(guiGraphics, Component.translatable("gui.dermicraft.farm.length", menu.BE.getFieldLength()),
                    FarmLayout.FIELD_LENGTH_ROW_Y);
        } else {
            renderExtraTabLabels(guiGraphics, tab);
        }
    }

    /** A text label vertically centered against a stepper row. */
    protected void drawRowLabel(GuiGraphics guiGraphics, Component text, int rowY) {
        guiGraphics.drawString(this.font, text, FarmLayout.FIELD_LABEL_X, rowY + (STEPPER_SIZE - 8) / 2, TEXT_COLOR, false);
    }
}
