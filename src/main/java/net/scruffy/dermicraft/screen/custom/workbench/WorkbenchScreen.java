package net.scruffy.dermicraft.screen.custom.workbench;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;

import java.util.List;

/**
 * Workbench's Storage strip -- see dermicraft-gear-stations-notes.md -> Workbench -> Storage
 * strip. Only the strip is built this pass; the "main content" area above it (Fabrication/Swap
 * pages) is separate future work, so this screen is otherwise a blank shell for now.
 */
public class WorkbenchScreen extends AbstractModScreen<WorkbenchMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation OPEN_TAB_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "open_tab.png");
    private static final ResourceLocation CLOSED_TAB_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "closed_tab.png");
    private static final int TAB_WIDTH = 20;
    private static final int TAB_HEIGHT = 56;
    // Stacked vertically on the left edge -- a small prototype of the eventual right-strip tab bar
    // (Storage/Fabrication/Mod pages) from the unified-GUI plan. The left-strip station selector
    // (FL Core/Dock/Growth Chamber/Workbench) is still separate future work, not this.
    private static final int MOD_TAB_X = -TAB_WIDTH + 2;
    private static final int MOD_TAB_TEXT_COLOR = 0x007F0E;
    private static final int MOD_TAB_Y = 8;
    private static final float TAB_TEXT_SCALE = 0.8F;
    private static final int FABRICATION_TAB_X = MOD_TAB_X;
    private static final int FABRICATION_TAB_Y = MOD_TAB_Y + TAB_HEIGHT + 4;
    private static final int FABRICATION_TAB_TEXT_COLOR = MOD_TAB_TEXT_COLOR;

    private enum Page { MOD, FABRICATION }
    private Page currentPage = Page.MOD;

    // Fabrication display roster (2026-08-08, display only -- no recipes/availability/tier-lock
    // yet, see Workbench -> Fabrication page design notes). Scrench isn't a gadget (no IGadget),
    // but it's the tool every physical-part-swap gadget depends on, so it's included here too.
    private static final List<Item> FABRICATION_ROSTER = List.of(
            ModItems.SIPPING.get(), ModItems.DRINKER.get(), ModItems.EATER.get(),
            ModItems.SUNDER.get(), ModItems.SCRENCH.get());
    private static final int FAB_ICON_X = 8;
    private static final int FAB_ICON_Y = 8;
    private static final int FAB_ICON_SPACING = 20;

    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

    private static final ResourceLocation NO_USE_OVERLAY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "no_use_overlay.png");

    private static final ResourceLocation TANK_AND_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tank_and_slot.png");
    private static final int TANK_AND_SLOT_WIDTH = 18;

    private static final ResourceLocation FILL_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "fill_button.png");
    private static final ResourceLocation FILL_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "fill_button_pressed.png");
    private static final int FILL_BUTTON_SIZE = 18;
    // Beside the fill-slot (bottom of the combined tank+slot asset), not below the whole tank --
    // "below" (FUEL_TANK_Y + FUEL_TANK_HEIGHT + a few px = ~80) collides with the player's own
    // inventory grid, which always starts at absolute y=83 (AbstractModScreen.PLAYER_INVENTORY_Y).
    // Whatever's in that inventory slot draws its icon on top afterward, hiding the button.
    private static final int FILL_BUTTON_X = WorkbenchMenu.FUEL_TANK_X + TANK_AND_SLOT_WIDTH + 6;
    private static final int FILL_BUTTON_Y = WorkbenchMenu.FUEL_FILL_SLOT_Y;

    private static final ResourceLocation STRIP_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "single_bar_background.png");
    private static final int STRIP_BACKGROUND_WIDTH = 176;
    private static final int STRIP_BACKGROUND_HEIGHT = 31;

    private static final ResourceLocation ITEM_SLOT_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot_bar.png");
    private static final int ITEM_SLOT_BAR_WIDTH = 162;
    private static final int ITEM_SLOT_BAR_HEIGHT = 18;

    private static final ResourceLocation SCROLL_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "scroll_bar.png");
    private static final int SCROLL_BAR_WIDTH = 8;
    private static final int SCROLL_BAR_HEIGHT = 31;

    private static final ResourceLocation SCROLLER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "scroller.png");
    private static final int SCROLLER_WIDTH = 6;
    private static final int SCROLLER_NATIVE_HEIGHT = 2;
    private static final int SCROLLER_MIN_HEIGHT = 4;

    // Background bar sits a few px above/below the slot row it frames (31 tall vs. the row's 18).
    private static final int BACKGROUND_Y_OFFSET = -7;
    private static final int SCROLL_BAR_X = WorkbenchMenu.STRIP_X + ITEM_SLOT_BAR_WIDTH + 1 + 6;
    private static final int SCROLL_TRACK_MARGIN = 2;

    // The strip is its own zone below the main panel and player hotbar, not embedded inside the
    // main background -- screen grows to fit both. MAIN_PANEL_HEIGHT/STRIP_GAP/BOTTOM_MARGIN here
    // must derive the same STRIP_Y as WorkbenchMenu's own (duplicated, not shared -- the menu is
    // common-side code and can't reference this client-only screen class).
    private static final int MAIN_PANEL_HEIGHT = 166;
    private static final int STRIP_GAP = 4;
    private static final int STRIP_BOTTOM_MARGIN = 7;

    // Thumb-drag state -- grab offset keeps the thumb anchored under the cursor at the point it
    // was clicked, rather than snapping its top edge to the cursor.
    private boolean draggingThumb = false;
    private int dragGrabOffsetY = 0;
    private int lastDraggedRow = -1;
    private boolean fillButtonPressedFlash = false;

    private FluidTankRenderer fuelRenderer;

    public WorkbenchScreen(WorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = MAIN_PANEL_HEIGHT + STRIP_GAP + STRIP_BACKGROUND_HEIGHT + STRIP_BOTTOM_MARGIN;
    }

    @Override
    protected void init() {
        super.init();
        fuelRenderer = createFluidRenderer16x40(menu.getSunderFuelCapacity());
        setPage(currentPage);
    }

    private void setPage(Page page) {
        currentPage = page;
        menu.setModPageActive(page == Page.MOD);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (currentPage == Page.MOD) {
            if (menu.isWorkItemSunder()) {
                renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.getSunderFluid(),
                        WorkbenchMenu.FUEL_TANK_X + 1, WorkbenchMenu.FUEL_TANK_Y + 1, fuelRenderer,
                        Component.translatable("tooltip.dermicraft.gauge.fuel"));

                renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                        WorkbenchMenu.CHAIN_SLOT_X + 1, WorkbenchMenu.CHAIN_SLOT_Y + 1,
                        menu.getSlot(WorkbenchMenu.CHAIN_SLOT_INDEX).getItem(), Component.translatable("tooltip.dermicraft.slot.chain"));
            }

            renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                    WorkbenchMenu.WORK_SLOT_X + 1, WorkbenchMenu.WORK_SLOT_Y + 1,
                    menu.getWorkItemStack(), Component.translatable("tooltip.dermicraft.slot.workbench_work_item"));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight,
                BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
        renderPlayerInventoryBackdrop(guiGraphics, x, y);

        blitFlippedX(guiGraphics, currentPage == Page.MOD ? OPEN_TAB_TEXTURE : CLOSED_TAB_TEXTURE,
                x + MOD_TAB_X, y + MOD_TAB_Y, TAB_WIDTH, TAB_HEIGHT);
        drawScaledString(guiGraphics, Component.translatable("screen.dermicraft.workbench.mod_tab"),
                x + MOD_TAB_X + 3, y + MOD_TAB_Y + TAB_HEIGHT / 2 - 4, MOD_TAB_TEXT_COLOR, TAB_TEXT_SCALE);

        blitFlippedX(guiGraphics, currentPage == Page.FABRICATION ? OPEN_TAB_TEXTURE : CLOSED_TAB_TEXTURE,
                x + FABRICATION_TAB_X, y + FABRICATION_TAB_Y, TAB_WIDTH, TAB_HEIGHT);
        drawScaledString(guiGraphics, Component.translatable("screen.dermicraft.workbench.fabrication_tab"),
                x + FABRICATION_TAB_X + 3, y + FABRICATION_TAB_Y + TAB_HEIGHT / 2 - 4,
                FABRICATION_TAB_TEXT_COLOR, TAB_TEXT_SCALE);

        if (currentPage == Page.MOD) {
            guiGraphics.blit(ITEM_SLOT_TEXTURE, x + WorkbenchMenu.WORK_SLOT_X, y + WorkbenchMenu.WORK_SLOT_Y, 0, 0,
                    ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

            // Sunder's panel (chain slot + fuel gauge + fill button) only makes sense once a Sunder
            // is actually sitting in the work slot -- an empty/non-Sunder work slot is the page's
            // "home" state, per the Swap page design notes (occupancy of the working slot drives
            // dispatch).
            if (menu.isWorkItemSunder()) {
                guiGraphics.blit(ITEM_SLOT_TEXTURE, x + WorkbenchMenu.CHAIN_SLOT_X, y + WorkbenchMenu.CHAIN_SLOT_Y, 0, 0,
                        ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

                guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + WorkbenchMenu.FUEL_TANK_X, y + WorkbenchMenu.FUEL_TANK_Y, 0, 0,
                        TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT, TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT);
                fuelRenderer.render(guiGraphics, x + WorkbenchMenu.FUEL_TANK_X + 1, y + WorkbenchMenu.FUEL_TANK_Y + 1, menu.getSunderFluid());

                ResourceLocation fillTexture = fillButtonPressedFlash ? FILL_BUTTON_PRESSED_TEXTURE : FILL_BUTTON_TEXTURE;
                guiGraphics.blit(fillTexture, x + FILL_BUTTON_X, y + FILL_BUTTON_Y, 0, 0,
                        FILL_BUTTON_SIZE, FILL_BUTTON_SIZE, FILL_BUTTON_SIZE, FILL_BUTTON_SIZE);
            }
        } else {
            renderFabricationGrid(guiGraphics, x, y);
        }

        int stripBgY = y + WorkbenchMenu.STRIP_Y + BACKGROUND_Y_OFFSET;
        guiGraphics.blit(STRIP_BACKGROUND_TEXTURE, x, stripBgY, 0, 0,
                STRIP_BACKGROUND_WIDTH, STRIP_BACKGROUND_HEIGHT, STRIP_BACKGROUND_WIDTH, STRIP_BACKGROUND_HEIGHT);
        guiGraphics.blit(ITEM_SLOT_BAR_TEXTURE, x + WorkbenchMenu.STRIP_X, y + WorkbenchMenu.STRIP_Y, 0, 0,
                ITEM_SLOT_BAR_WIDTH, ITEM_SLOT_BAR_HEIGHT, ITEM_SLOT_BAR_WIDTH, ITEM_SLOT_BAR_HEIGHT);

        renderScrollbar(guiGraphics, x + SCROLL_BAR_X, stripBgY);
    }

    /** Display only, 2026-08-08 -- item_slot.png backdrop + icon per roster entry, no recipe
     * lookup or tier check yet. See Workbench -> Fabrication page. */
    private void renderFabricationGrid(GuiGraphics guiGraphics, int x, int y) {
        for (int i = 0; i < FABRICATION_ROSTER.size(); i++) {
            int slotX = x + FAB_ICON_X + i * FAB_ICON_SPACING;
            int slotY = y + FAB_ICON_Y;
            guiGraphics.blit(ITEM_SLOT_TEXTURE, slotX, slotY, 0, 0,
                    ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
            guiGraphics.renderItem(new ItemStack(FABRICATION_ROSTER.get(i)), slotX + 1, slotY + 1);

            // TODO: replace with real "no GadgetFabricatingRecipe available / below station tier"
            // checks once Fabrication actually looks up recipes. Every roster item is unconditionally
            // overlaid right now because none of them have a real recipe wired up yet -- none are
            // actually craftable, so this is accurate, not a placeholder value.
            //
            // Same state dance GraftingTableScreen#renderGhostItem already uses for an overlay drawn
            // after renderItem: depth test off so the item's leftover depth can't hide a later flat
            // blit, blend explicitly enabled (with the standard alpha blend function) so the overlay's
            // translucent pixels actually blend instead of writing fully opaque.
            guiGraphics.flush();
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(NO_USE_OVERLAY_TEXTURE, slotX, slotY, 0, 0,
                    ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
            guiGraphics.flush();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    /** Draws text at a smaller size than the font's native scale -- same pose-stack approach as
     * {@link #blitFlippedX}, just scaling uniformly instead of mirroring. x/y are the position the
     * text should visually appear at; dividing by scale before drawing compensates for the
     * scaled-up coordinate space so it lands in the right place rather than drifting toward the
     * corner as scale shrinks. */
    private void drawScaledString(GuiGraphics guiGraphics, Component text, int x, int y, int color, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1);
        guiGraphics.drawString(font, text, (int) (x / scale), (int) (y / scale), color);
        guiGraphics.pose().popPose();
    }

    /** Mirrors a texture horizontally at render time (180deg around the Y axis) -- no second,
     * flipped copy of the art needed. Standard pose-stack trick: translate the origin to the far
     * edge of the target rect, then scale X by -1 so the blit draws back across it reversed. */
    private void blitFlippedX(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height) {
        // A negative X scale reverses the quad's winding order -- without this, backface culling
        // (enabled for this render pass in 1.21's GuiGraphics) discards the flipped quad entirely
        // instead of drawing it mirrored, which is why the tab vanished rather than just flipping.
        RenderSystem.disableCull();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + width, y, 0);
        guiGraphics.pose().scale(-1, 1, 1);
        guiGraphics.blit(texture, 0, 0, 0, 0, width, height, width, height);
        guiGraphics.pose().popPose();
        RenderSystem.enableCull();
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int trackX, int trackY) {
        guiGraphics.blit(SCROLL_BAR_TEXTURE, trackX, trackY, 0, 0,
                SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT);

        int thumbHeight = thumbHeight();
        int thumbY = thumbY(trackY, thumbHeight);
        int thumbX = thumbX(trackX);
        guiGraphics.blit(SCROLLER_TEXTURE, thumbX, thumbY, 0, 0,
                SCROLLER_WIDTH, thumbHeight, SCROLLER_WIDTH, SCROLLER_NATIVE_HEIGHT);
    }

    private int travel() {
        return SCROLL_BAR_HEIGHT - 2 * SCROLL_TRACK_MARGIN;
    }

    private int thumbHeight() {
        int totalRows = menu.getMaxScrollRow() + 1;
        int travel = travel();
        return Math.min(travel, Math.max(SCROLLER_MIN_HEIGHT, travel / totalRows));
    }

    private int thumbY(int trackY, int thumbHeight) {
        int maxRow = menu.getMaxScrollRow();
        int thumbY = trackY + SCROLL_TRACK_MARGIN;
        if (maxRow > 0) {
            thumbY += (int) (((float) menu.getScrollRow() / maxRow) * (travel() - thumbHeight));
        }
        return thumbY;
    }

    private int thumbX(int trackX) {
        return trackX + (SCROLL_BAR_WIDTH - SCROLLER_WIDTH) / 2 - 1;
    }

    private int trackX() {
        return (width - imageWidth) / 2 + SCROLL_BAR_X;
    }

    private int trackY() {
        return (height - imageHeight) / 2 + WorkbenchMenu.STRIP_Y + BACKGROUND_Y_OFFSET;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.getMaxScrollRow() > 0 && isOverStrip((int) mouseX, (int) mouseY)) {
            int steps = (int) Math.signum(scrollY);
            if (steps != 0) {
                pressScrollButton(steps < 0 ? WorkbenchMenu.BUTTON_SCROLL_DOWN : WorkbenchMenu.BUTTON_SCROLL_UP);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static final int FILL_PRESS_FLASH_TICKS = 4;
    private int fillPressTicks = 0;

    @Override
    protected void containerTick() {
        super.containerTick();
        if (fillPressTicks > 0) {
            fillPressTicks--;
            if (fillPressTicks == 0) fillButtonPressedFlash = false;
        }
    }

    // Grab the thumb to drag it; otherwise a classic scrollbar-track click (above the thumb pages
    // up, below pages down).
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + MOD_TAB_X, y + MOD_TAB_Y, TAB_WIDTH, TAB_HEIGHT)) {
            setPage(Page.MOD);
            return true;
        }
        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + FABRICATION_TAB_X, y + FABRICATION_TAB_Y, TAB_WIDTH, TAB_HEIGHT)) {
            setPage(Page.FABRICATION);
            return true;
        }

        if (menu.isWorkItemSunder() && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + FILL_BUTTON_X, y + FILL_BUTTON_Y,
                FILL_BUTTON_SIZE, FILL_BUTTON_SIZE)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.BUTTON_FILL_FROM_POOL);
            fillButtonPressedFlash = true;
            fillPressTicks = FILL_PRESS_FLASH_TICKS;
            return true;
        }

        if (menu.getMaxScrollRow() > 0) {
            int trackX = trackX();
            int trackY = trackY();
            int thumbHeight = thumbHeight();
            int thumbY = thumbY(trackY, thumbHeight);
            int thumbX = thumbX(trackX);

            if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, thumbX, thumbY, SCROLLER_WIDTH, thumbHeight)) {
                draggingThumb = true;
                dragGrabOffsetY = (int) mouseY - thumbY;
                lastDraggedRow = menu.getScrollRow();
                return true;
            }

            if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, trackX, trackY, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT)) {
                int trackMid = trackY + SCROLL_BAR_HEIGHT / 2;
                pressScrollButton(mouseY < trackMid ? WorkbenchMenu.BUTTON_SCROLL_UP : WorkbenchMenu.BUTTON_SCROLL_DOWN);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingThumb) {
            int maxRow = menu.getMaxScrollRow();
            int thumbHeight = thumbHeight();
            int usableTravel = travel() - thumbHeight;
            int trackTop = trackY() + SCROLL_TRACK_MARGIN;

            int desiredThumbY = (int) mouseY - dragGrabOffsetY;
            int clampedOffset = Math.max(0, Math.min(usableTravel, desiredThumbY - trackTop));

            int row = maxRow > 0 && usableTravel > 0
                    ? Math.round(((float) clampedOffset / usableTravel) * maxRow)
                    : 0;
            row = Math.max(0, Math.min(maxRow, row));

            if (row != lastDraggedRow) {
                lastDraggedRow = row;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.setRowButtonId(row));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingThumb) {
            draggingThumb = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverStrip(int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int stripY = trackY();
        return MouseUtil.isMouseOver(mouseX, mouseY, x, stripY, STRIP_BACKGROUND_WIDTH, STRIP_BACKGROUND_HEIGHT);
    }

    private void pressScrollButton(int buttonId) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }
}
