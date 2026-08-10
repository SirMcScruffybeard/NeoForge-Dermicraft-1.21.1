package net.scruffy.dermicraft.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.util.MouseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractModScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    // Self-tiling range-grid cell shared by the auto-farmer machines' GUIs. The tile carries a blank
    // margin so butting copies together forms a grid; tiled top-left anchored to depict an NxN workspace.
    private static final ResourceLocation GRID_SQUARE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/patterns/grid_square.png");
    private static final int GRID_CELL = 3;

    // screen_background is now a blank panel (no baked-in inventory art), so every screen needs
    // these drawn on top, near the coordinates AbstractModMenu.addPlayerInventory/addPlayerHotbar
    // place their slots (x=8,y=84 for the 3x9 grid; x=8,y=142 for the hotbar row) -- shifted 1px
    // up and left of the logical slot origin so the art centers correctly on the item icons.
    private static final ResourceLocation PLAYER_INVENTORY_SLOTS =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/slots/player_inventory_slots.png");
    private static final int PLAYER_INVENTORY_WIDTH = 162;
    private static final int PLAYER_INVENTORY_HEIGHT = 54;
    private static final int PLAYER_INVENTORY_X = 7;
    private static final int PLAYER_INVENTORY_Y = 83;

    private static final ResourceLocation ITEM_SLOT_BAR =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/slots/item_slot_bar.png");
    private static final int ITEM_SLOT_BAR_WIDTH = 162;
    private static final int ITEM_SLOT_BAR_HEIGHT = 18;
    private static final int ITEM_SLOT_BAR_X = 7;
    private static final int ITEM_SLOT_BAR_Y = 141;

    // Shared plain item-slot background -- every screen that needs a bare 18x18 slot backdrop
    // (Scrench's chain slot, Eater's buffer column, ...) reuses this one texture/constant instead of
    // each screen declaring its own private copy.
    protected static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/slots/item_slot.png");

    // Module-slot background (see dermicraft-gadget-notes.md -> Gadget upgrade points -> Modules
    // direction note) -- visually distinct (yellow) from a plain item slot so a Module slot reads as
    // its own category at a glance, same role ITEM_SLOT_TEXTURE plays for ordinary item slots.
    protected static final ResourceLocation MODULE_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/slots/yellow_slot.png");

    protected static final int SLOT_SIZE = 18;

    // Subtle translucent divider -- separates a swap panel's own slot rows from the player's own
    // inventory grid below it (e.g. Eater's buffer row) without needing a dedicated texture asset.
    private static final int DIVIDER_COLOR = 0x80000000;
    private static final int DIVIDER_HEIGHT = 1;

    public AbstractModScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** Draws an n x n grid of the shared grid_square tile, top-left anchored at (x, y). Range display for farmers. */
    protected void renderRangeGrid(GuiGraphics guiGraphics, int x, int y, int n) {
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                guiGraphics.blit(GRID_SQUARE, x + col * GRID_CELL, y + row * GRID_CELL, 0, 0,
                        GRID_CELL, GRID_CELL, GRID_CELL, GRID_CELL);
            }
        }
    }

    /**
     * Draws the player inventory grid and hotbar backdrop. Every screen calls this right after
     * blitting its (now-blank) background, since that art no longer includes the player's own slots.
     */
    protected void renderPlayerInventoryBackdrop(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(PLAYER_INVENTORY_SLOTS, x + PLAYER_INVENTORY_X, y + PLAYER_INVENTORY_Y, 0, 0,
                PLAYER_INVENTORY_WIDTH, PLAYER_INVENTORY_HEIGHT, PLAYER_INVENTORY_WIDTH, PLAYER_INVENTORY_HEIGHT);
        guiGraphics.blit(ITEM_SLOT_BAR, x + ITEM_SLOT_BAR_X, y + ITEM_SLOT_BAR_Y, 0, 0,
                ITEM_SLOT_BAR_WIDTH, ITEM_SLOT_BAR_HEIGHT, ITEM_SLOT_BAR_WIDTH, ITEM_SLOT_BAR_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        // Gets rid of title and inventory title
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    /**
     * Draws {@code count} copies of an 18x18 slot background in a straight line -- a row
     * ({@code spacingY = 0}) or a column ({@code spacingX = 0}) -- starting at
     * {@code (x + startX, y + startY)}. Shared by any screen hosting an {@code IWorkbenchSwappable}
     * panel (Scrench, Workbench's Swap page) so the same gadget panel's slot backgrounds don't need
     * re-drawing per host; only the panel's own origin/texture choice varies.
     */
    protected void renderSlotBackgrounds(GuiGraphics guiGraphics, ResourceLocation texture,
                                          int x, int y, int startX, int startY,
                                          int count, int spacingX, int spacingY) {
        for (int i = 0; i < count; i++) {
            guiGraphics.blit(texture, x + startX + i * spacingX, y + startY + i * spacingY, 0, 0,
                    SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        }
    }

    /** A thin horizontal rule at {@code (x + startX, y + startY)}, {@code width} wide -- see
     * {@link #DIVIDER_COLOR}. */
    protected void renderDivider(GuiGraphics guiGraphics, int x, int y, int startX, int startY, int width) {
        guiGraphics.fill(x + startX, y + startY, x + startX + width, y + startY + DIVIDER_HEIGHT, DIVIDER_COLOR);
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
        //width and height are measured from the inside of the "puddle gauge"
    }

    /************************************************************************************
     *
     * @param capacity
     * @return puddle renderer that is 16px X 64px.
     *      Used by DroolingCauldronScreen, SkinTankScreen
     *************************************************************************************/
    protected FluidTankRenderer createFluidRenderer16x64(int capacity) {
        return createFluidRenderer(capacity, 16, 64);
    }

    /************************************************************************************
     *
     * @param capacity
     * @return puddle renderer that is 16px X 40px.
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

    /**
     * Same as {@link #renderFluidTooltipArea}, but prepends a purpose label (e.g. "Fuel", "Reagent",
     * "Result") above the fluid name/amount lines -- for screens with multiple tanks where it's not
     * obvious from the gauge alone which role each one plays.
     */
    protected void renderFluidTooltipArea(GuiGraphics guiGraphics, int pMouseX, int pMouseY, int x, int y,
                                          FluidStack stack, int offsetX, int offsetY, FluidTankRenderer renderer,
                                          Component purpose) {
        if (isMouseAboveArea(pMouseX, pMouseY, x, y, offsetX, offsetY, renderer)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(purpose.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            tooltip.addAll(renderer.getTooltip(stack, TooltipFlag.Default.NORMAL));
            guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pMouseX - x, pMouseY - y);
        }
    }

    /**
     * Shows a purpose label (e.g. "Fuel Container", "Ingredient") when hovering an empty item slot
     * -- the slot's coordinates here are its logical position (same as the SlotItemHandler added to
     * the menu), an 18x18 area. Only fires while the slot is empty: once it holds an item, vanilla's
     * own item tooltip already covers it, and stacking a second tooltip on top would be redundant.
     */
    protected void renderItemSlotTooltipArea(GuiGraphics guiGraphics, int pMouseX, int pMouseY, int x, int y,
                                             int offsetX, int offsetY, ItemStack stack, Component purpose) {
        if (!stack.isEmpty()) return;
        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + offsetX, y + offsetY, 18, 18)) {
            guiGraphics.renderTooltip(this.font, purpose, pMouseX - x, pMouseY - y);
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
