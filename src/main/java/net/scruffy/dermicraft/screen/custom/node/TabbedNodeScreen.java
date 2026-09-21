package net.scruffy.dermicraft.screen.custom.node;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.block.custom.duct.NodeDirectionMode;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.NodeDirectionClickPayload;
import net.scruffy.dermicraft.network.NodeFilterModeClickPayload;
import net.scruffy.dermicraft.network.NodeFilterNbtClickPayload;
import net.scruffy.dermicraft.network.NodeTransferToggleClickPayload;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Framework screen for the tabbed Node GUI rework (see project_node_gui_tab_overhaul memory).
 * Renders a tab per connected leg only (see {@link #connectedLegIndices}), compacted with no gaps
 * for unconnected legs; the active one -- the BE's persisted choice if still connected, otherwise
 * the first connected leg as a visual-only fallback (see {@link #effectiveActiveLeg}) -- shows open
 * art + standard direction icon, the rest stay closed with a pressed icon. The right-side column
 * (item slot, gauge+slot, distribution button) is Node-wide and always shown. Clicking a tab selects
 * it and persists the choice on the BE via {@link TabbedNodeMenu}. The distribution button still
 * only toggles a local, client-only display state -- not wired to the BE yet. What happens if the
 * leg a still-open screen is showing loses its connection mid-session (a dedicated cover/error
 * screen, per project_node_gui_tab_overhaul memory) isn't built yet -- it currently just falls back
 * silently to another connected leg next frame.
 */
public class TabbedNodeScreen extends AbstractModScreen<TabbedNodeMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation TAB_CLOSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "tab_24_closed.png");
    private static final ResourceLocation TAB_OPEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "tab_24_open.png");
    private static final int TAB_SIZE = 24;
    private static final int TAB_ICON_SIZE = 18;
    private static final int TAB_ICON_INSET = (TAB_SIZE - TAB_ICON_SIZE) / 2;
    private static final int TAB_X = -TAB_SIZE + 1;
    private static final int TAB_GAP = 4;
    private static final int TAB_FIRST_Y = 0; // top tab flush with the background texture's top edge
    private static final int TAB_STEP = TAB_SIZE + TAB_GAP;

    // Order agreed on for the tab overhaul: North, South, East, West, Up, Down. Unselected tabs keep
    // the closed-art + pressed-icon combo from the previous step; the selected tab switches to the
    // open-art tab + that direction's standard (unpressed) icon.
    private static final ResourceLocation[] TAB_ICONS_CLOSED = {
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "north_button_pressed.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "south_button_pressed.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "east_button_pressed.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "west_button_pressed.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "up_button_pressed.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "down_button_pressed.png"),
    };
    private static final ResourceLocation[] TAB_ICONS_OPEN = {
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "north_button.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "south_button.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "east_button.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "west_button.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "up_button.png"),
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "down_button.png"),
    };
    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

    private static final ResourceLocation TANK_AND_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tank_and_slot.png");
    private static final int TANK_AND_SLOT_WIDTH = 18;
    private static final int TANK_AND_SLOT_HEIGHT = 66;

    private static final ResourceLocation ROUND_ROBIN_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "round_robin_button.png");
    private static final ResourceLocation SPREAD_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "spread_button.png");
    private static final int DISTRIBUTION_BUTTON_SIZE = 18;

    // Right-side column -- sticks out past imageWidth the same way the tab column sticks out past
    // x=0, so it has the full screen height to work with instead of colliding with the player
    // inventory backdrop. Backed by side_column.png (31 wide x 166 tall, same height as
    // imageHeight), flush with the background's top edge. Order top to bottom within it: item
    // slot, gauge+slot, distribution button, vertically centered as a group.
    private static final ResourceLocation SIDE_COLUMN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "side_column.png");
    private static final int SIDE_COLUMN_WIDTH = 31;
    private static final int SIDE_COLUMN_HEIGHT = 166;
    private static final int SIDE_COLUMN_CONTENT_X_OFFSET = (SIDE_COLUMN_WIDTH - ITEM_SLOT_SIZE) / 2;
    private static final int SIDE_COLUMN_GAP = 4;
    private static final int SIDE_COLUMN_CONTENT_HEIGHT =
            ITEM_SLOT_SIZE + SIDE_COLUMN_GAP + TANK_AND_SLOT_HEIGHT + SIDE_COLUMN_GAP + DISTRIBUTION_BUTTON_SIZE;
    private static final int ITEM_SLOT_Y = (SIDE_COLUMN_HEIGHT - SIDE_COLUMN_CONTENT_HEIGHT) / 2;
    private static final int GAUGE_Y = ITEM_SLOT_Y + ITEM_SLOT_SIZE + SIDE_COLUMN_GAP;
    private static final int DISTRIBUTION_BUTTON_Y = GAUGE_Y + TANK_AND_SLOT_HEIGHT + SIDE_COLUMN_GAP;

    // Per-leg row layout, for whichever leg the active tab represents -- see the sketch agreed on
    // for this step. 5 columns (enable, direction, filter slot, whitelist/blacklist, NBT match),
    // items row on top, fluids row directly below in the same columns. The filter slot column's
    // (x, y) here must match TabbedNodeMenu's item/fluid filter slot positions (53, 21)/(53, 43)
    // exactly -- the +1,+1 nudge vs. this column's own x/y is the same convention every other real
    // slot in this screen uses.
    private static final int ROW_CONTROL_SIZE = 18;
    private static final int ROW_GAP = 4;
    private static final int ROW_COL_STEP = ROW_CONTROL_SIZE + ROW_GAP;
    private static final int ROW_START_X = 8;
    private static final int ENABLE_COL_X = ROW_START_X;
    private static final int DIRECTION_COL_X = ENABLE_COL_X + ROW_COL_STEP;
    private static final int FILTER_COL_X = DIRECTION_COL_X + ROW_COL_STEP;
    private static final int MODE_COL_X = FILTER_COL_X + ROW_COL_STEP;
    private static final int NBT_COL_X = MODE_COL_X + ROW_COL_STEP;
    private static final int ITEMS_ROW_Y = 20;
    private static final int FLUIDS_ROW_Y = ITEMS_ROW_Y + ROW_COL_STEP;

    private static final ResourceLocation INPUT_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "input_button.png");
    private static final ResourceLocation OUTPUT_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "output_button.png");
    private static final ResourceLocation ITEM_TOGGLE_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "item_button.png");
    private static final ResourceLocation ITEM_TOGGLE_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "item_off_button.png");
    private static final ResourceLocation FLUID_TOGGLE_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "fluid_button.png");
    private static final ResourceLocation FLUID_TOGGLE_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "fluid__off_button.png");
    private static final ResourceLocation ALLOW_FILTER_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "allow_filter_button.png");
    private static final ResourceLocation DENY_FILTER_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "deny_filter_button.png");
    private static final ResourceLocation ALLOW_NBT_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "allow_nbt_button.png");
    private static final ResourceLocation DENY_NBT_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "deny_nbt_button.png");

    private FluidTankRenderer tankRenderer;

    // Fluid filter swatch -- same FluidTankRenderer-based render Workbench uses for its own fluid
    // requirement icons (a solid 16x16 fill of the fluid's still texture/tint), not a ghost bucket
    // item. Capacity is arbitrary (1) since a filter has no "amount" -- always rendered full.
    private FluidTankRenderer filterSwatchRenderer;

    // Client-only display state -- not wired to the BE's real distribution mode yet.
    private boolean roundRobin = true;

    public TabbedNodeScreen(TabbedNodeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // The tab column and side column both stick out past imageWidth, but vanilla centers the screen
    // (and JEI positions its item list) using only imageWidth -- so with the screen centered normally,
    // JEI's list overlaps the side column it doesn't know about. Shifting leftPos left after init()
    // moves the whole screen (background, tabs, side column, and every real slot, since vanilla's own
    // slot rendering/hit-testing reads leftPos/topPos directly) out from under it.
    private static final int SCREEN_X_SHIFT = 32;

    @Override
    protected void init() {
        super.init();
        leftPos -= SCREEN_X_SHIFT;
        tankRenderer = createFluidRenderer16x40(menu.BE.getFluidTank().getCapacity());
        filterSwatchRenderer = createFluidRenderer(1, 16, 16);
    }

    /** Which tab indices (into {@link NodeBlockEntity#LEG_ORDER}) have a real duct connection right
     * now -- computed fresh rather than cached, since a connection can change while the screen is
     * open (duct placed/broken nearby). Unconnected legs get no tab slot at all, so the shown tabs
     * compact together instead of leaving gaps -- see project_node_gui_tab_overhaul memory. */
    private List<Integer> connectedLegIndices() {
        List<Integer> connected = new ArrayList<>();
        for (int i = 0; i < NodeBlockEntity.LEG_ORDER.length; i++) {
            if (menu.BE.isConnected(NodeBlockEntity.LEG_ORDER[i])) connected.add(i);
        }
        return connected;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos;
        int y = topPos;

        List<Integer> connected = connectedLegIndices();
        for (int position = 0; position < connected.size(); position++) {
            int legIndex = connected.get(position);
            int tabY = y + TAB_FIRST_Y + position * TAB_STEP;
            if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + TAB_X, tabY, TAB_SIZE, TAB_SIZE)) {
                if (legIndex != menu.getActiveTab()) {
                    menu.setActiveTab(legIndex);
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AbstractModMenu.TAB_BUTTON_BASE + legIndex);
                }
                return true;
            }
        }

        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY,
                x + imageWidth + SIDE_COLUMN_CONTENT_X_OFFSET, y + DISTRIBUTION_BUTTON_Y,
                DISTRIBUTION_BUTTON_SIZE, DISTRIBUTION_BUTTON_SIZE)) {
            roundRobin = !roundRobin;
            return true;
        }

        int effectiveLeg = menu.getEffectiveLeg();
        if (effectiveLeg >= 0) {
            Direction activeDir = NodeBlockEntity.LEG_ORDER[effectiveLeg];
            if (rowClicked(mouseX, mouseY, x, y, ITEMS_ROW_Y, activeDir, false)) return true;
            if (rowClicked(mouseX, mouseY, x, y, FLUIDS_ROW_Y, activeDir, true)) return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Handles the four button columns of one per-leg row (enable, direction, whitelist/blacklist
     * mode, NBT match) -- the filter slot column needs no handling here, since it's a real Slot in
     * TabbedNodeMenu and vanilla's own slot click routing already reaches it. */
    private boolean rowClicked(double mouseX, double mouseY, int x, int y, int rowY, Direction dir, boolean fluid) {
        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + ENABLE_COL_X, y + rowY,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE)) {
            PacketDistributor.sendToServer(new NodeTransferToggleClickPayload(menu.BE.getBlockPos(), dir, fluid));
            return true;
        }
        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + DIRECTION_COL_X, y + rowY,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE)) {
            PacketDistributor.sendToServer(new NodeDirectionClickPayload(menu.BE.getBlockPos(), dir, fluid));
            return true;
        }
        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + MODE_COL_X, y + rowY,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE)) {
            PacketDistributor.sendToServer(new NodeFilterModeClickPayload(menu.BE.getBlockPos(), dir, fluid));
            return true;
        }
        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + NBT_COL_X, y + rowY,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE)) {
            PacketDistributor.sendToServer(new NodeFilterNbtClickPayload(menu.BE.getBlockPos(), dir, fluid));
            return true;
        }
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = leftPos;
        int y = topPos;

        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight,
                BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
        renderPlayerInventoryBackdrop(guiGraphics, x, y);

        // Left-side tab column -- only connected legs get a tab at all, compacted together with no
        // gaps for the missing ones. The active tab (BE's persisted choice, if still connected) shows
        // open art + its standard icon; every other shown tab stays closed with a pressed icon.
        List<Integer> connected = connectedLegIndices();
        int effectiveLeg = menu.getEffectiveLeg();
        for (int position = 0; position < connected.size(); position++) {
            int legIndex = connected.get(position);
            int tabY = y + TAB_FIRST_Y + position * TAB_STEP;
            boolean open = legIndex == effectiveLeg;
            guiGraphics.blit(open ? TAB_OPEN_TEXTURE : TAB_CLOSED_TEXTURE, x + TAB_X, tabY, 0, 0,
                    TAB_SIZE, TAB_SIZE, TAB_SIZE, TAB_SIZE);
            ResourceLocation icon = open ? TAB_ICONS_OPEN[legIndex] : TAB_ICONS_CLOSED[legIndex];
            guiGraphics.blit(icon, x + TAB_X + TAB_ICON_INSET, tabY + TAB_ICON_INSET, 0, 0,
                    TAB_ICON_SIZE, TAB_ICON_SIZE, TAB_ICON_SIZE, TAB_ICON_SIZE);
        }

        // Right-side column -- backdrop texture flush with the background's right edge and top,
        // contents centered within its width.
        int columnX = x + imageWidth;
        guiGraphics.blit(SIDE_COLUMN_TEXTURE, columnX, y, 0, 0,
                SIDE_COLUMN_WIDTH, SIDE_COLUMN_HEIGHT, SIDE_COLUMN_WIDTH, SIDE_COLUMN_HEIGHT);

        int contentX = columnX + SIDE_COLUMN_CONTENT_X_OFFSET;
        guiGraphics.blit(ITEM_SLOT_TEXTURE, contentX, y + ITEM_SLOT_Y, 0, 0,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, contentX, y + GAUGE_Y, 0, 0,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
        tankRenderer.render(guiGraphics, contentX + 1, y + GAUGE_Y + 1, menu.BE.getFluid());

        guiGraphics.blit(roundRobin ? ROUND_ROBIN_BUTTON_TEXTURE : SPREAD_BUTTON_TEXTURE,
                contentX, y + DISTRIBUTION_BUTTON_Y, 0, 0,
                DISTRIBUTION_BUTTON_SIZE, DISTRIBUTION_BUTTON_SIZE, DISTRIBUTION_BUTTON_SIZE, DISTRIBUTION_BUTTON_SIZE);

        // Per-leg rows for whichever leg is effectively active -- nothing renders at all if the Node
        // has no connections yet. The item row's filter slot only draws the backdrop -- the ghost
        // item on top of it renders automatically via the real Slot in TabbedNodeMenu. The fluid
        // row's filter slot instead draws a fluid swatch (see filterSwatchRenderer) since its slot
        // never actually holds an item.
        if (effectiveLeg >= 0) {
            Direction activeDir = NodeBlockEntity.LEG_ORDER[effectiveLeg];
            renderLegRow(guiGraphics, x, y, ITEMS_ROW_Y, null,
                    menu.BE.isItemsEnabled(activeDir) ? ITEM_TOGGLE_BUTTON_TEXTURE : ITEM_TOGGLE_OFF_TEXTURE,
                    directionTexture(menu.BE.getItemDirectionMode(activeDir)),
                    menu.BE.isItemFilterWhitelist(activeDir) ? ALLOW_FILTER_BUTTON_TEXTURE : DENY_FILTER_BUTTON_TEXTURE,
                    menu.BE.isItemFilterNbtMatch(activeDir) ? ALLOW_NBT_BUTTON_TEXTURE : DENY_NBT_BUTTON_TEXTURE);
            renderLegRow(guiGraphics, x, y, FLUIDS_ROW_Y, menu.BE.getFluidFilter(activeDir),
                    menu.BE.isFluidsEnabled(activeDir) ? FLUID_TOGGLE_BUTTON_TEXTURE : FLUID_TOGGLE_OFF_TEXTURE,
                    directionTexture(menu.BE.getFluidDirectionMode(activeDir)),
                    menu.BE.isFluidFilterWhitelist(activeDir) ? ALLOW_FILTER_BUTTON_TEXTURE : DENY_FILTER_BUTTON_TEXTURE,
                    menu.BE.isFluidFilterNbtMatch(activeDir) ? ALLOW_NBT_BUTTON_TEXTURE : DENY_NBT_BUTTON_TEXTURE);
        }
    }

    private void renderLegRow(GuiGraphics guiGraphics, int x, int y, int rowY, Fluid filterFluid,
                               ResourceLocation enableTexture, ResourceLocation directionTexture,
                               ResourceLocation modeTexture, ResourceLocation nbtTexture) {
        guiGraphics.blit(enableTexture, x + ENABLE_COL_X, y + rowY, 0, 0,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE);
        guiGraphics.blit(directionTexture, x + DIRECTION_COL_X, y + rowY, 0, 0,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE);
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x + FILTER_COL_X, y + rowY, 0, 0,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE);
        if (filterFluid != null && filterFluid != Fluids.EMPTY) {
            filterSwatchRenderer.render(guiGraphics, x + FILTER_COL_X + 1, y + rowY + 1, new FluidStack(filterFluid, 1));
        }
        guiGraphics.blit(modeTexture, x + MODE_COL_X, y + rowY, 0, 0,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE);
        guiGraphics.blit(nbtTexture, x + NBT_COL_X, y + rowY, 0, 0,
                ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE, ROW_CONTROL_SIZE);
    }

    private static ResourceLocation directionTexture(NodeDirectionMode mode) {
        return switch (mode) {
            case IN -> INPUT_BUTTON_TEXTURE;
            case OUT -> OUTPUT_BUTTON_TEXTURE;
        };
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = leftPos;
        int y = topPos;
        int contentX = imageWidth + SIDE_COLUMN_CONTENT_X_OFFSET;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(), contentX + 1, GAUGE_Y + 1, tankRenderer);

        int effectiveLeg = menu.getEffectiveLeg();
        if (effectiveLeg < 0) return;
        Direction activeDir = NodeBlockEntity.LEG_ORDER[effectiveLeg];
        Fluid filterFluid = menu.BE.getFluidFilter(activeDir);
        // Fluid name only, not FluidTankRenderer#getTooltip's usual name+amount/capacity lines --
        // a filter has no amount, so that line would just be misleading noise here.
        if (filterFluid != Fluids.EMPTY
                && isMouseAboveArea(pMouseX, pMouseY, x, y, FILTER_COL_X + 1, FLUIDS_ROW_Y + 1, filterSwatchRenderer)) {
            guiGraphics.renderTooltip(this.font, filterFluid.getFluidType().getDescription(), pMouseX - x, pMouseY - y);
        }
    }
}
