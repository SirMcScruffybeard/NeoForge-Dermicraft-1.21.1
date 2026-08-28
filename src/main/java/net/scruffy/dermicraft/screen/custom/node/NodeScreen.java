package net.scruffy.dermicraft.screen.custom.node;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.block.custom.duct.NodeDirectionMode;
import net.scruffy.dermicraft.block.custom.duct.NodeDistributionMode;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.NodeDirectionClickPayload;
import net.scruffy.dermicraft.network.NodeDistributionClickPayload;
import net.scruffy.dermicraft.network.NodeTransferToggleClickPayload;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Node GUI. Interaction model: click a connected compass direction to select it (highlighted),
 * then click either the Item or the Fluid direction button to cycle THAT type's mode on the
 * selected leg (In <-> Out) independently of the other -- e.g. items In while fluid is Out on the
 * same face. Each direction button's own texture reflects the selected leg's current mode for that
 * type. The Item/Fluid enable toggle buttons act on the same selected leg, independently of
 * direction and of each other (both default off) -- direction is meaningless for a type until it's
 * enabled. Round-Robin/Spread is a separate global toggle, no selection needed, positioned beside
 * East. Unconnected directions simply don't render a button.
 */
public class NodeScreen extends AbstractModScreen<NodeMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation TANK_AND_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tank_and_slot.png");
    private static final int TANK_AND_SLOT_WIDTH = 18;
    private static final int TANK_AND_SLOT_HEIGHT = 66;

    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

    private static final ResourceLocation NORTH_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "north_button.png");
    private static final ResourceLocation NORTH_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "north_button_pressed.png");
    private static final ResourceLocation SOUTH_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "south_button.png");
    private static final ResourceLocation SOUTH_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "south_button_pressed.png");
    private static final ResourceLocation EAST_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "east_button.png");
    private static final ResourceLocation EAST_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "east_button_pressed.png");
    private static final ResourceLocation WEST_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "west_button.png");
    private static final ResourceLocation WEST_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "west_button_pressed.png");
    private static final ResourceLocation UP_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "up_button.png");
    private static final ResourceLocation UP_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "up_button_pressed.png");
    private static final ResourceLocation DOWN_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "down_button.png");
    private static final ResourceLocation DOWN_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "down_button_pressed.png");
    private static final ResourceLocation NULL_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "null_button.png");
    private static final int DIRECTION_BUTTON_SIZE = 18;

    // Shared mode-toggle icons. Input/Output reflects whichever direction is currently selected;
    // Round-Robin/Spread always reflects the Node's global distribution mode.
    private static final ResourceLocation INPUT_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "input_button.png");
    private static final ResourceLocation OUTPUT_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "output_button.png");
    private static final ResourceLocation ROUND_ROBIN_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "round_robin_button.png");
    private static final ResourceLocation SPREAD_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "spread_button.png");

    // Per-leg item/fluid toggles, acting on the currently selected leg (same interaction model as
    // the shared In/Out button). ON uses the dedicated icon; OFF now has its own dedicated icon per
    // type too (originally reused the shared "no use" icon, but that made it unclear which toggle
    // was off -- both off states looked identical).
    private static final ResourceLocation ITEM_TOGGLE_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "item_button.png");
    private static final ResourceLocation ITEM_TOGGLE_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "item_off_button.png");
    private static final ResourceLocation FLUID_TOGGLE_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "fluid_button.png");
    private static final ResourceLocation FLUID_TOGGLE_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "fluid__off_button.png");

    // tank_and_slot origin (tank gauge on top; the GUI fluid-handler slot sits at its base).
    // Right edge aligned with the player inventory's right edge (rightmost slot at x=152,
    // +18 wide -> 170), so tank origin = 170 - 18.
    private static final int TANK_AND_SLOT_X = 152;
    private static final int TANK_AND_SLOT_Y = 11;
    // buffer slot background origin (menu slot is +1,+1 from here) -- kept tight (4px) to the tank
    // gauge's left edge so the two read as one grouped unit, in contrast to the more open direction
    // grid (TANK_AND_SLOT_X - ITEM_SLOT_SIZE - 4).
    private static final int BUFFER_SLOT_X = TANK_AND_SLOT_X - ITEM_SLOT_SIZE - 4;
    private static final int BUFFER_SLOT_Y = 32;

    // Full direction-button grid: 4 columns (up/down, west, center/north-south, east) x 3 rows
    // (top, middle, bottom), each gap 4px. Left column stays aligned with the player inventory's
    // left edge (x=8). The whole grid is shifted vertically so the middle row (West/East) lines
    // up with the item buffer slot's row (BUFFER_SLOT_Y).
    private static final int DIRECTION_BUTTON_GAP = 4;
    private static final int DIRECTION_COL_STEP = DIRECTION_BUTTON_SIZE + DIRECTION_BUTTON_GAP;

    private static final int DIRECTION_COL0_X = 8; // up/down column, matches player inventory left edge
    private static final int DIRECTION_COL1_X = DIRECTION_COL0_X + DIRECTION_COL_STEP; // west
    private static final int DIRECTION_COL2_X = DIRECTION_COL1_X + DIRECTION_COL_STEP; // north/south (cross center)
    private static final int DIRECTION_COL3_X = DIRECTION_COL2_X + DIRECTION_COL_STEP; // east

    private static final int DIRECTION_ROW_MID_Y = BUFFER_SLOT_Y; // west/east inline with the buffer slot
    private static final int DIRECTION_ROW_TOP_Y = DIRECTION_ROW_MID_Y - DIRECTION_COL_STEP;    // up/north
    private static final int DIRECTION_ROW_BOTTOM_Y = DIRECTION_ROW_MID_Y + DIRECTION_COL_STEP; // down/south

    // Mode-toggle column: one grid step past East, using all three rows now (2026-08-27 rework,
    // item/fluid-independent direction) -- Item direction (top, mirrors Up/North's row), Round-Robin
    // (middle, directly beside East -- moved here from the old bottom slot to make room), Fluid
    // direction (bottom, the slot Round-Robin vacated, mirrors Down/South's row).
    private static final int MODE_TOGGLE_X = DIRECTION_COL3_X + DIRECTION_COL_STEP;
    private static final int ITEM_DIRECTION_BUTTON_X = MODE_TOGGLE_X;
    private static final int ITEM_DIRECTION_BUTTON_Y = DIRECTION_ROW_TOP_Y; // mirrors Up
    private static final int ROUND_ROBIN_BUTTON_X = MODE_TOGGLE_X;
    private static final int ROUND_ROBIN_BUTTON_Y = DIRECTION_ROW_MID_Y; // beside East
    private static final int FLUID_DIRECTION_BUTTON_X = MODE_TOGGLE_X;
    private static final int FLUID_DIRECTION_BUTTON_Y = DIRECTION_ROW_BOTTOM_Y; // mirrors Down

    // Item/Fluid toggle column: one more grid step past the In/Out/Round-Robin column, same
    // spacing pattern. Item lines up with North's row (top row), Fluid with South's row (bottom row).
    private static final int TRANSFER_TOGGLE_X = MODE_TOGGLE_X + DIRECTION_COL_STEP;
    private static final int ITEM_TOGGLE_BUTTON_X = TRANSFER_TOGGLE_X;
    private static final int ITEM_TOGGLE_BUTTON_Y = DIRECTION_ROW_TOP_Y; // mirrors North
    private static final int FLUID_TOGGLE_BUTTON_X = TRANSFER_TOGGLE_X;
    private static final int FLUID_TOGGLE_BUTTON_Y = DIRECTION_ROW_BOTTOM_Y; // mirrors South

    private static final Map<Direction, int[]> DIRECTION_POS = new EnumMap<>(Direction.class);
    private static final Map<Direction, ResourceLocation> DIRECTION_TEXTURE = new EnumMap<>(Direction.class);
    private static final Map<Direction, ResourceLocation> DIRECTION_PRESSED_TEXTURE = new EnumMap<>(Direction.class);

    static {
        DIRECTION_POS.put(Direction.UP, new int[]{DIRECTION_COL0_X, DIRECTION_ROW_TOP_Y});
        DIRECTION_POS.put(Direction.DOWN, new int[]{DIRECTION_COL0_X, DIRECTION_ROW_BOTTOM_Y});
        DIRECTION_POS.put(Direction.WEST, new int[]{DIRECTION_COL1_X, DIRECTION_ROW_MID_Y});
        DIRECTION_POS.put(Direction.NORTH, new int[]{DIRECTION_COL2_X, DIRECTION_ROW_TOP_Y});
        DIRECTION_POS.put(Direction.SOUTH, new int[]{DIRECTION_COL2_X, DIRECTION_ROW_BOTTOM_Y});
        DIRECTION_POS.put(Direction.EAST, new int[]{DIRECTION_COL3_X, DIRECTION_ROW_MID_Y});

        DIRECTION_TEXTURE.put(Direction.UP, UP_BUTTON_TEXTURE);
        DIRECTION_TEXTURE.put(Direction.DOWN, DOWN_BUTTON_TEXTURE);
        DIRECTION_TEXTURE.put(Direction.WEST, WEST_BUTTON_TEXTURE);
        DIRECTION_TEXTURE.put(Direction.NORTH, NORTH_BUTTON_TEXTURE);
        DIRECTION_TEXTURE.put(Direction.SOUTH, SOUTH_BUTTON_TEXTURE);
        DIRECTION_TEXTURE.put(Direction.EAST, EAST_BUTTON_TEXTURE);

        DIRECTION_PRESSED_TEXTURE.put(Direction.UP, UP_BUTTON_PRESSED_TEXTURE);
        DIRECTION_PRESSED_TEXTURE.put(Direction.DOWN, DOWN_BUTTON_PRESSED_TEXTURE);
        DIRECTION_PRESSED_TEXTURE.put(Direction.WEST, WEST_BUTTON_PRESSED_TEXTURE);
        DIRECTION_PRESSED_TEXTURE.put(Direction.NORTH, NORTH_BUTTON_PRESSED_TEXTURE);
        DIRECTION_PRESSED_TEXTURE.put(Direction.SOUTH, SOUTH_BUTTON_PRESSED_TEXTURE);
        DIRECTION_PRESSED_TEXTURE.put(Direction.EAST, EAST_BUTTON_PRESSED_TEXTURE);
    }

    private FluidTankRenderer tankRenderer;

    @Nullable
    private Direction selectedDirection = null;

    public NodeScreen(NodeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        tankRenderer = createFluidRenderer16x40(menu.BE.getFluidTank().getCapacity());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        for (Direction dir : Direction.values()) {
            if (!menu.BE.isConnected(dir)) continue;
            int[] pos = DIRECTION_POS.get(dir);
            if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + pos[0], y + pos[1],
                    DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE)) {
                selectedDirection = dir;
                return true;
            }
        }

        if (selectedDirection != null && menu.BE.isConnected(selectedDirection)
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + ITEM_DIRECTION_BUTTON_X, y + ITEM_DIRECTION_BUTTON_Y,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new NodeDirectionClickPayload(menu.BE.getBlockPos(), selectedDirection, false));
            return true;
        }

        if (selectedDirection != null && menu.BE.isConnected(selectedDirection)
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + FLUID_DIRECTION_BUTTON_X, y + FLUID_DIRECTION_BUTTON_Y,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new NodeDirectionClickPayload(menu.BE.getBlockPos(), selectedDirection, true));
            return true;
        }

        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + ROUND_ROBIN_BUTTON_X, y + ROUND_ROBIN_BUTTON_Y,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new NodeDistributionClickPayload(menu.BE.getBlockPos()));
            return true;
        }

        if (selectedDirection != null && menu.BE.isConnected(selectedDirection)
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + ITEM_TOGGLE_BUTTON_X, y + ITEM_TOGGLE_BUTTON_Y,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new NodeTransferToggleClickPayload(menu.BE.getBlockPos(), selectedDirection, false));
            return true;
        }

        if (selectedDirection != null && menu.BE.isConnected(selectedDirection)
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + FLUID_TOGGLE_BUTTON_X, y + FLUID_TOGGLE_BUTTON_Y,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new NodeTransferToggleClickPayload(menu.BE.getBlockPos(), selectedDirection, true));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
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

        guiGraphics.blit(ITEM_SLOT_TEXTURE, x + BUFFER_SLOT_X, y + BUFFER_SLOT_Y, 0, 0,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + TANK_AND_SLOT_X, y + TANK_AND_SLOT_Y, 0, 0,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);

        tankRenderer.render(guiGraphics, x + TANK_AND_SLOT_X + 1, y + TANK_AND_SLOT_Y + 1, menu.BE.getFluid());

        // Every direction always shows a button: null_button where there's no connection,
        // the compass texture (pressed if selected, regular otherwise) where there is one.
        for (Direction dir : Direction.values()) {
            int[] pos = DIRECTION_POS.get(dir);
            ResourceLocation texture = !menu.BE.isConnected(dir) ? NULL_BUTTON_TEXTURE
                    : dir == selectedDirection ? DIRECTION_PRESSED_TEXTURE.get(dir)
                    : DIRECTION_TEXTURE.get(dir);

            guiGraphics.blit(texture, x + pos[0], y + pos[1], 0, 0,
                    DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE);
        }

        guiGraphics.blit(getItemDirectionTexture(), x + ITEM_DIRECTION_BUTTON_X, y + ITEM_DIRECTION_BUTTON_Y, 0, 0,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE);

        guiGraphics.blit(getFluidDirectionTexture(), x + FLUID_DIRECTION_BUTTON_X, y + FLUID_DIRECTION_BUTTON_Y, 0, 0,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE);

        guiGraphics.blit(getDistributionTexture(), x + ROUND_ROBIN_BUTTON_X, y + ROUND_ROBIN_BUTTON_Y, 0, 0,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE);

        guiGraphics.blit(getItemToggleTexture(), x + ITEM_TOGGLE_BUTTON_X, y + ITEM_TOGGLE_BUTTON_Y, 0, 0,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE);

        guiGraphics.blit(getFluidToggleTexture(), x + FLUID_TOGGLE_BUTTON_X, y + FLUID_TOGGLE_BUTTON_Y, 0, 0,
                DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE, DIRECTION_BUTTON_SIZE);
    }

    private ResourceLocation getItemDirectionTexture() {
        if (selectedDirection == null || !menu.BE.isConnected(selectedDirection)) {
            return NULL_BUTTON_TEXTURE; // nothing selected -- same null state the direction grid uses
        }
        return getModeTexture(menu.BE.getItemDirectionMode(selectedDirection));
    }

    private ResourceLocation getFluidDirectionTexture() {
        if (selectedDirection == null || !menu.BE.isConnected(selectedDirection)) {
            return NULL_BUTTON_TEXTURE;
        }
        return getModeTexture(menu.BE.getFluidDirectionMode(selectedDirection));
    }

    private ResourceLocation getModeTexture(NodeDirectionMode mode) {
        return switch (mode) {
            case IN -> INPUT_BUTTON_TEXTURE;
            case OUT -> OUTPUT_BUTTON_TEXTURE;
        };
    }

    private ResourceLocation getDistributionTexture() {
        return menu.BE.getDistributionMode() == NodeDistributionMode.ROUND_ROBIN
                ? ROUND_ROBIN_BUTTON_TEXTURE : SPREAD_BUTTON_TEXTURE;
    }

    private ResourceLocation getItemToggleTexture() {
        if (selectedDirection == null || !menu.BE.isConnected(selectedDirection)) return NULL_BUTTON_TEXTURE;
        return menu.BE.isItemsEnabled(selectedDirection) ? ITEM_TOGGLE_BUTTON_TEXTURE : ITEM_TOGGLE_OFF_TEXTURE;
    }

    private ResourceLocation getFluidToggleTexture() {
        if (selectedDirection == null || !menu.BE.isConnected(selectedDirection)) return NULL_BUTTON_TEXTURE;
        return menu.BE.isFluidsEnabled(selectedDirection) ? FLUID_TOGGLE_BUTTON_TEXTURE : FLUID_TOGGLE_OFF_TEXTURE;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(), TANK_AND_SLOT_X + 1, TANK_AND_SLOT_Y + 1, tankRenderer);
    }
}
