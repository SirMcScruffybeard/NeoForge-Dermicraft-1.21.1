package net.scruffy.dermicraft.screen.custom.workbench;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.custom.floor.GearStationPool;
import net.scruffy.dermicraft.item.custom.DrinkerItem;
import net.scruffy.dermicraft.item.custom.EaterItem;
import net.scruffy.dermicraft.item.custom.ShatterItem;
import net.scruffy.dermicraft.item.custom.SippingItem;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.gadget_fabricating.GadgetFabricatingRecipe;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Workbench's screen -- see dermicraft-gear-stations-notes.md -> Workbench. Builds the Mod page
 * (Sunder/Shatter/Eater swap sub-panel), the Fabrication page (recipe grid + ingredient/craft detail panel), and
 * the persistent Storage strip along the bottom shared by both. Point-Spend (the design's eventual
 * 3rd right-strip page) remains deferred, not built here.
 */
public class WorkbenchScreen extends AbstractModScreen<WorkbenchMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";
    private static final String ARROWS_DIR = "textures/gui/arrows/";

    // Fabrication's job-progress indicator -- same asset/pattern EffluentcerScreen and others
    // already use for a timed-crafting arrow.
    private static final ResourceLocation ARROW_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_background.png");
    private static final ResourceLocation ARROW_FULL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_fulll.png");

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

    // Restored from the BE on open (see WorkbenchBlockEntity#isFabricationPageActive/
    // #getSelectedFabricationRecipeId) rather than defaulted, so reopening THIS Workbench's screen
    // returns to whichever page/recipe was last selected instead of always resetting -- per-block,
    // persisted, and synced the same way scrollRow/open/active already are. Tab clicks and recipe
    // selection below write back to the BE (via clickMenuButton, same trust boundary as every other
    // button on this screen) in addition to updating these local fields for instant feedback.
    private Page currentPage = menu.isFabricationPageActive() ? Page.FABRICATION : Page.MOD;

    // Fabrication recipe grid (2026-08-09) -- every registered GadgetFabricatingRecipe, queried
    // live off the client's own synced RecipeManager rather than a hardcoded roster. Scrench is no
    // longer listed here: it was only ever a placeholder for this grid before Fabrication read real
    // recipes, and Scrench has its own ordinary crafting-table recipe, not a GadgetFabricatingRecipe
    // -- it was never governed by this page's tier/pool mechanics to begin with.
    private static final int FAB_ICON_X = 8;
    private static final int FAB_ICON_Y = 8;
    private static final int FAB_ICON_SPACING = 20;

    // Detail panel (ingredient icons + Craft button/progress) for whichever recipe icon is
    // currently selected -- see #renderFabricationDetail. Items and fluids sit in two side-by-side
    // icon columns (tooltip-only, no inline text) rather than a stacked text list. Each column shows
    // exactly ONE row of FAB_ICONS_PER_ROW icons at a time now (2026-09-06, revised) -- past that, an
    // independent per-column scrollbar (#fabItemScrollRow/#fabFluidScrollRow) pages through the rest
    // instead of wrapping to a 2nd row, which used to silently overlap the fixed action row below
    // once a recipe needed more than 4 of something (a real gadget hit this: 6 items). The action
    // row's own Y is unchanged from before this revision -- it was always sized for exactly one row.
    private static final int FAB_DETAIL_X = FAB_ICON_X;
    private static final int FAB_DETAIL_Y = FAB_ICON_Y + WorkbenchScreen.ITEM_SLOT_SIZE + 4;
    private static final int FAB_ICONS_PER_ROW = 4;
    // Each column's scrollbar sits directly right of its own 4 icons. Width mirrors SCROLL_BAR_WIDTH
    // (8, declared further down alongside the Storage strip's own scrollbar constants) as a literal
    // rather than a reference -- Java disallows forward-referencing a later field's name from an
    // earlier one's initializer within the same class.
    private static final int FAB_ITEM_SCROLL_X = FAB_DETAIL_X + FAB_ICONS_PER_ROW * WorkbenchScreen.ITEM_SLOT_SIZE + 1;
    private static final int FLUID_COLUMN_X = FAB_ITEM_SCROLL_X + 8 + 3;
    private static final int FAB_FLUID_SCROLL_X = FLUID_COLUMN_X + FAB_ICONS_PER_ROW * WorkbenchScreen.ITEM_SLOT_SIZE + 1;
    private static final int FAB_ACTION_Y = FAB_DETAIL_Y + WorkbenchScreen.ITEM_SLOT_SIZE + 6;
    private static final int FAB_ACTION_WIDTH = 18;
    private static final int FAB_ACTION_HEIGHT = 18;
    private static final int FAB_ARROW_WIDTH = 17;
    private static final int FAB_ARROW_HEIGHT = 10;

    private static final ResourceLocation CRAFT_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "craft_button.png");
    private static final ResourceLocation CRAFT_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "craft_button_pressed.png");
    // Dims the button when the recipe isn't actually craftable yet -- no separate disabled art
    // exists, so this multiplies the same texture's color instead (same trick fluid tinting
    // already uses via RenderSystem.setShaderColor).
    private static final float CRAFT_BUTTON_DISABLED_TINT = 0.5f;

    // Translucent red overlay marking an ingredient slot short of what the recipe needs -- same
    // at-a-glance role NO_USE_OVERLAY_TEXTURE plays on the recipe grid, just a plain tinted fill
    // rather than a texture (no bespoke art exists for this panel yet).
    private static final int SHORTFALL_OVERLAY_COLOR = 0x80FF0000;

    // Translucent green overlay marking the recipe grid's currently-selected icon -- same plain-fill
    // approach as SHORTFALL_OVERLAY_COLOR above, so the player can tell what they're building at a
    // glance without having to track the detail panel separately.
    private static final int SELECTED_OVERLAY_COLOR = 0x8000FF00;

    // Opacity for an empty fluid slot's "ghost" preview -- see #renderFluidColumn.
    private static final float GHOST_ALPHA = 0.35f;

    private int selectedFabricationIndex = menu.getSelectedFabricationIndex();

    // Which row of the selected recipe's item/fluid list is currently showing, independent per
    // column. Deliberately pure client state, NOT menu/BE-backed like the Storage strip's own
    // scrollRow -- the ingredient columns are decorative tooltip-only icons, not real Slots (see
    // #renderIngredientColumn's own javadoc), so there's nothing server-authoritative to keep in
    // sync; no button-click round-trip needed to change these. Reset to 0 whenever a different
    // recipe is selected (see #handleFabricationClick) so scrolling into one recipe's fluid list
    // doesn't carry over to the next one picked.
    private int fabItemScrollRow = 0;
    private int fabFluidScrollRow = 0;

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
    private static final int FILL_BUTTON_X = SunderItem.FUEL_TANK_X + TANK_AND_SLOT_WIDTH + 6;
    private static final int FILL_BUTTON_Y = WorkbenchMenu.FUEL_FILL_SLOT_Y;
    // Eater's own tank sits at a different X than Sunder/Shatter's shared position (see
    // EaterItem.FUEL_TANK_X's own placeholder-layout note), so its Fill button gets its own position
    // rather than reusing the Sunder/Shatter one above.
    private static final int EATER_FILL_BUTTON_X = EaterItem.FUEL_TANK_X + TANK_AND_SLOT_WIDTH + 6;
    private static final int EATER_FILL_BUTTON_Y = EaterItem.FUEL_SLOT_Y;

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

    // Purpose-built 8x18 track for the Fabrication panel's two ingredient scrollbars -- stretching
    // SCROLL_BAR_TEXTURE (native 31 tall, made for the Storage strip) down to their 18px row height
    // squashed/cut off its art, so this is a separate texture at the exact height instead.
    private static final ResourceLocation FAB_SCROLL_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "scrollbar_18.png");
    private static final int FAB_SCROLL_BAR_HEIGHT = 18;

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

    // Which of the (now three) scrollbars is currently being thumb-dragged, if any -- generalized
    // 2026-09-06 alongside #renderScrollbar so the Storage strip and the two independent Fabrication
    // ingredient-column scrollbars share one drag implementation. Grab offset keeps the thumb
    // anchored under the cursor at the point it was clicked, rather than snapping its top edge to it.
    private enum ScrollTarget { STORAGE, FAB_ITEMS, FAB_FLUIDS }

    @Nullable
    private ScrollTarget draggingScrollbar = null;
    private int dragGrabOffsetY = 0;
    private int lastDraggedRow = -1;
    private boolean fillButtonPressedFlash = false;
    private boolean craftButtonPressedFlash = false;

    private FluidTankRenderer fuelRenderer;

    // Last known RAW (GLFW window-space, not GUI-scaled) cursor position -- static, not an instance
    // field, so it survives across WorkbenchMenu#broadcastChanges' auto-reopen (a fresh WorkbenchScreen
    // instance is constructed each time the menu recreates itself). Without restoring this, that
    // reopen visibly snapped the cursor to the screen's default position every time a gadget landed
    // in the work slot. -1 means "nothing captured yet" (first-ever open), skip the warp.
    private static double lastCursorX = -1;
    private static double lastCursorY = -1;

    public WorkbenchScreen(WorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = MAIN_PANEL_HEIGHT + STRIP_GAP + STRIP_BACKGROUND_HEIGHT + STRIP_BOTTOM_MARGIN;
    }

    @Override
    protected void init() {
        super.init();
        // Read whichever gadget is actually present rather than assuming a fixed capacity -- same
        // reasoning as every other per-gadget branch on this screen.
        int fuelCapacity;
        if (menu.isWorkItemShatter()) {
            fuelCapacity = menu.getShatterFuelCapacity();
        } else if (menu.isWorkItemDrinker()) {
            fuelCapacity = menu.getDrinkerCapacity();
        } else if (menu.isWorkItemSipping()) {
            fuelCapacity = menu.getSippingCapacity();
        } else if (menu.isWorkItemEater()) {
            fuelCapacity = menu.getEaterFuelCapacity();
        } else {
            fuelCapacity = menu.getSunderFuelCapacity();
        }
        fuelRenderer = createFluidRenderer16x40(fuelCapacity);
        setPage(currentPage);

        // Restores the cursor to wherever it was on the PREVIOUS screen instance -- see
        // lastCursorX/Y's own javadoc. GLFW's own cursor-position call, not anything Screen exposes,
        // since MouseHandler has no public warp method.
        if (lastCursorX >= 0 && lastCursorY >= 0) {
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(this.minecraft.getWindow().getWindow(), lastCursorX, lastCursorY);
        }
    }

    /** Continuously tracks the RAW cursor position (not the GUI-scaled {@code mouseX}/{@code mouseY}
     * this method is actually handed) so {@link #init} always has an up-to-date value to restore on
     * the next reopen, however that reopen happens to be triggered. */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        lastCursorX = this.minecraft.mouseHandler.xpos();
        lastCursorY = this.minecraft.mouseHandler.ypos();
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
                        SunderItem.FUEL_TANK_X + 1, SunderItem.FUEL_TANK_Y + 1, fuelRenderer,
                        Component.translatable("tooltip.dermicraft.gauge.fuel"));

                renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                        SunderItem.CHAIN_SLOT_X + 1, SunderItem.CHAIN_SLOT_Y + 1,
                        menu.getSlot(WorkbenchMenu.CHAIN_SLOT_INDEX).getItem(), Component.translatable("tooltip.dermicraft.slot.chain"));
            } else if (menu.isWorkItemShatter()) {
                renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.getShatterFluid(),
                        ShatterItem.FUEL_TANK_X + 1, ShatterItem.FUEL_TANK_Y + 1, fuelRenderer,
                        Component.translatable("tooltip.dermicraft.gauge.fuel"));

                renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                        ShatterItem.HEAD_SLOT_X + 1, ShatterItem.HEAD_SLOT_Y + 1,
                        menu.getSlot(WorkbenchMenu.HEAD_SLOT_INDEX).getItem(), Component.translatable("tooltip.dermicraft.slot.shatter_head"));
            } else if (menu.isWorkItemDrinker()) {
                renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.getDrinkerFluid(),
                        DrinkerItem.TANK_X + 1, DrinkerItem.TANK_Y + 1, fuelRenderer,
                        Component.translatable("tooltip.dermicraft.gauge.fuel"));
            } else if (menu.isWorkItemSipping()) {
                renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.getSippingFluid(),
                        SippingItem.TANK_X + 1, SippingItem.TANK_Y + 1, fuelRenderer,
                        Component.translatable("tooltip.dermicraft.gauge.fuel"));
            } else if (menu.isWorkItemEater()) {
                renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y, menu.getEaterFluid(),
                        EaterItem.FUEL_TANK_X + 1, EaterItem.FUEL_TANK_Y + 1, fuelRenderer,
                        Component.translatable("tooltip.dermicraft.gauge.fuel"));
            }

            renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                    WorkbenchMenu.WORK_SLOT_X + 1, WorkbenchMenu.WORK_SLOT_Y + 1,
                    menu.getWorkItemStack(), Component.translatable("tooltip.dermicraft.slot.workbench_work_item"));
        } else {
            renderFabricationTooltips(guiGraphics, mouseX, mouseY, x, y);
        }
    }

    /** Hover tooltips for the detail panel's ingredient icons -- exact "available/required" numbers
     * live here rather than as inline text, see #renderFabricationDetail. Both item and fluid
     * tooltips are hand-built rather than reusing vanilla's automatic item tooltip or
     * FluidTankRenderer#getTooltip: these are decorative icons, not real Slots, so the former never
     * fires on them, and the latter reads its fluid identity off the AVAILABLE amount, which breaks
     * for an empty ingredient (see the fluid loop's own comment below). */
    private void renderFabricationTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        List<RecipeHolder<GadgetFabricatingRecipe>> recipes = fabricationRecipes();
        if (selectedFabricationIndex < 0 || selectedFabricationIndex >= recipes.size()) return;

        GadgetFabricatingRecipe recipe = recipes.get(selectedFabricationIndex).value();
        GearStationPool.Snapshot pool = menu.getPoolSnapshot();

        List<ItemStack> items = recipe.items();
        int itemStart = fabItemScrollRow * FAB_ICONS_PER_ROW;
        for (int i = itemStart; i < Math.min(items.size(), itemStart + FAB_ICONS_PER_ROW); i++) {
            int slotX = FAB_DETAIL_X + (i - itemStart) * ITEM_SLOT_SIZE;
            int slotY = FAB_DETAIL_Y;
            if (!MouseUtil.isMouseOver(mouseX, mouseY, x + slotX, y + slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) continue;

            ItemStack requirement = items.get(i);
            int available = pool.itemCount(requirement);
            List<Component> tooltip = List.of(requirement.getHoverName(),
                    Component.literal(available + "/" + requirement.getCount())
                            .withStyle(available >= requirement.getCount() ? ChatFormatting.GREEN : ChatFormatting.RED));
            guiGraphics.renderTooltip(font, tooltip, Optional.empty(), mouseX - x, mouseY - y);
            return;
        }

        List<FluidStack> fluids = recipe.fluids();
        int fluidStart = fabFluidScrollRow * FAB_ICONS_PER_ROW;
        for (int i = fluidStart; i < Math.min(fluids.size(), fluidStart + FAB_ICONS_PER_ROW); i++) {
            int slotX = FLUID_COLUMN_X + (i - fluidStart) * ITEM_SLOT_SIZE;
            int slotY = FAB_DETAIL_Y;
            if (!MouseUtil.isMouseOver(mouseX, mouseY, x + slotX, y + slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) continue;

            FluidStack requirement = fluids.get(i);
            int available = pool.fluidAmount(requirement);
            // Built by hand rather than routed through FluidTankRenderer#getTooltip: that method
            // reads fluidStack.getFluid(), which NeoForge's FluidStack silently rewrites to
            // Fluids.EMPTY whenever amount <= 0 (isEmpty()'s definition) -- passing the AVAILABLE
            // amount (0 when the pool has none) made an empty slot's tooltip say "Empty" instead of
            // naming the fluid the recipe actually needs. Reading the name off requirement (whose
            // amount is the recipe's cost, never 0) sidesteps that landmine entirely.
            Component fluidName = requirement.getFluid().getFluidType().getDescription();
            List<Component> tooltip = List.of(fluidName,
                    Component.literal(available + "/" + requirement.getAmount() + " mB")
                            .withStyle(available >= requirement.getAmount() ? ChatFormatting.GREEN : ChatFormatting.RED));
            guiGraphics.renderTooltip(font, tooltip, Optional.empty(), mouseX - x, mouseY - y);
            return;
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

            // A gadget's own panel (Sunder's chain+fuel, Eater's Modules+buffer) only makes sense
            // once one is actually sitting in the work slot -- an empty/unswappable work slot is the
            // page's "home" state, per the Swap page design notes (occupancy of the working slot
            // drives dispatch). Sunder gets its Fill-from-pool button (fuel is a consumable resource,
            // the button restocks it from the shared pool); Eater doesn't -- Modules aren't
            // fluid-fillable and the buffer holds arbitrary harvested items, not fuel.
            if (menu.isWorkItemSunder()) {
                guiGraphics.blit(MODULE_SLOT_TEXTURE, x + SunderItem.MODULE_SLOT_X, y + SunderItem.MODULE_SLOT_Y, 0, 0,
                        ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

                guiGraphics.blit(ITEM_SLOT_TEXTURE, x + SunderItem.CHAIN_SLOT_X, y + SunderItem.CHAIN_SLOT_Y, 0, 0,
                        ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

                guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + SunderItem.FUEL_TANK_X, y + SunderItem.FUEL_TANK_Y, 0, 0,
                        TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT, TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT);
                fuelRenderer.render(guiGraphics, x + SunderItem.FUEL_TANK_X + 1, y + SunderItem.FUEL_TANK_Y + 1, menu.getSunderFluid());

                ResourceLocation fillTexture = fillButtonPressedFlash ? FILL_BUTTON_PRESSED_TEXTURE : FILL_BUTTON_TEXTURE;
                guiGraphics.blit(fillTexture, x + FILL_BUTTON_X, y + FILL_BUTTON_Y, 0, 0,
                        FILL_BUTTON_SIZE, FILL_BUTTON_SIZE, FILL_BUTTON_SIZE, FILL_BUTTON_SIZE);
            } else if (menu.isWorkItemShatter()) {
                // Same layout as Sunder's own branch above -- head slot standing in for the chain
                // slot, same fuel tank/slot coordinates (ShatterItem.FUEL_TANK_X/Y equal Sunder's own,
                // see ShatterItem's own field javadoc), same Fill-from-pool button (Shatter's fuel is
                // just as much a consumable resource as Sunder's).
                guiGraphics.blit(MODULE_SLOT_TEXTURE, x + ShatterItem.MODULE_SLOT_X, y + ShatterItem.MODULE_SLOT_Y, 0, 0,
                        ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

                guiGraphics.blit(ITEM_SLOT_TEXTURE, x + ShatterItem.HEAD_SLOT_X, y + ShatterItem.HEAD_SLOT_Y, 0, 0,
                        ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

                guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + ShatterItem.FUEL_TANK_X, y + ShatterItem.FUEL_TANK_Y, 0, 0,
                        TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT, TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT);
                fuelRenderer.render(guiGraphics, x + ShatterItem.FUEL_TANK_X + 1, y + ShatterItem.FUEL_TANK_Y + 1, menu.getShatterFluid());

                ResourceLocation shatterFillTexture = fillButtonPressedFlash ? FILL_BUTTON_PRESSED_TEXTURE : FILL_BUTTON_TEXTURE;
                guiGraphics.blit(shatterFillTexture, x + FILL_BUTTON_X, y + FILL_BUTTON_Y, 0, 0,
                        FILL_BUTTON_SIZE, FILL_BUTTON_SIZE, FILL_BUTTON_SIZE, FILL_BUTTON_SIZE);
            } else if (menu.isWorkItemDrinker()) {
                // Module slot + buffer-gauge/drain-slot pairing, same layout ScrenchScreen's own
                // renderDrinkerBg uses -- this page was simply missing a Drinker branch entirely
                // (only Sunder/Shatter/Eater were dispatched), so Drinker's real, functional slots
                // (built generically via IWorkbenchSwappable, same as every other gadget) had no
                // background art drawn under them at all.
                guiGraphics.blit(MODULE_SLOT_TEXTURE, x + DrinkerItem.MODULE_SLOT_X, y + DrinkerItem.MODULE_SLOT_Y, 0, 0,
                        ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

                guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + DrinkerItem.TANK_X, y + DrinkerItem.TANK_Y, 0, 0,
                        TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT, TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT);
                fuelRenderer.render(guiGraphics, x + DrinkerItem.TANK_X + 1, y + DrinkerItem.TANK_Y + 1, menu.getDrinkerFluid());
            } else if (menu.isWorkItemSipping()) {
                // Same layout as Drinker's own branch above -- Sipping's Module slot/tank coordinates
                // are identical (SippingItem.MODULE_SLOT_X/Y and TANK_X/Y equal Drinker's own).
                guiGraphics.blit(MODULE_SLOT_TEXTURE, x + SippingItem.MODULE_SLOT_X, y + SippingItem.MODULE_SLOT_Y, 0, 0,
                        ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

                guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + SippingItem.TANK_X, y + SippingItem.TANK_Y, 0, 0,
                        TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT, TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT);
                fuelRenderer.render(guiGraphics, x + SippingItem.TANK_X + 1, y + SippingItem.TANK_Y + 1, menu.getSippingFluid());
            } else if (menu.isWorkItemEater()) {
                renderSlotBackgrounds(guiGraphics, MODULE_SLOT_TEXTURE, x, y,
                        EaterItem.MODULE_SLOT_X, EaterItem.MODULE_SLOT_Y,
                        EaterItem.MODULE_SLOT_COUNT, EaterItem.MODULE_SLOT_SPACING, 0);

                renderSlotBackgrounds(guiGraphics, ITEM_SLOT_TEXTURE, x, y,
                        EaterItem.BUFFER_SLOT_X, EaterItem.BUFFER_SLOT_Y,
                        EaterItem.SLOT_COUNT, EaterItem.BUFFER_SLOT_SPACING, 0);

                // Fuel tank + fill slot + Fill-from-pool button (2026-08-27) -- Aggregate/Beam Module
                // mining now actually costs fuel, see EaterItem#aggregateTick.
                guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + EaterItem.FUEL_TANK_X, y + EaterItem.FUEL_TANK_Y, 0, 0,
                        TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT, TANK_AND_SLOT_WIDTH, WorkbenchMenu.FUEL_TANK_HEIGHT);
                fuelRenderer.render(guiGraphics, x + EaterItem.FUEL_TANK_X + 1, y + EaterItem.FUEL_TANK_Y + 1, menu.getEaterFluid());

                ResourceLocation eaterFillTexture = fillButtonPressedFlash ? FILL_BUTTON_PRESSED_TEXTURE : FILL_BUTTON_TEXTURE;
                guiGraphics.blit(eaterFillTexture, x + EATER_FILL_BUTTON_X, y + EATER_FILL_BUTTON_Y, 0, 0,
                        FILL_BUTTON_SIZE, FILL_BUTTON_SIZE, FILL_BUTTON_SIZE, FILL_BUTTON_SIZE);
            }
        } else {
            guiGraphics.blit(ITEM_SLOT_TEXTURE, x + WorkbenchMenu.OUTPUT_SLOT_X, y + WorkbenchMenu.OUTPUT_SLOT_Y, 0, 0,
                    ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
            renderFabricationGrid(guiGraphics, x, y);
            renderFabricationDetail(guiGraphics, x, y);
        }

        int stripBgY = y + WorkbenchMenu.STRIP_Y + BACKGROUND_Y_OFFSET;
        guiGraphics.blit(STRIP_BACKGROUND_TEXTURE, x, stripBgY, 0, 0,
                STRIP_BACKGROUND_WIDTH, STRIP_BACKGROUND_HEIGHT, STRIP_BACKGROUND_WIDTH, STRIP_BACKGROUND_HEIGHT);
        guiGraphics.blit(ITEM_SLOT_BAR_TEXTURE, x + WorkbenchMenu.STRIP_X, y + WorkbenchMenu.STRIP_Y, 0, 0,
                ITEM_SLOT_BAR_WIDTH, ITEM_SLOT_BAR_HEIGHT, ITEM_SLOT_BAR_WIDTH, ITEM_SLOT_BAR_HEIGHT);

        renderScrollbar(guiGraphics, x + SCROLL_BAR_X, stripBgY, SCROLL_BAR_HEIGHT, menu.getScrollRow(), menu.getMaxScrollRow());
    }

    /** Every registered GadgetFabricatingRecipe, read live off the client's own synced
     * RecipeManager -- see GadgetFabricatingRecipe#allRecipes for why the sort order is safe to
     * rely on for indexing without transmitting it separately. */
    private List<RecipeHolder<GadgetFabricatingRecipe>> fabricationRecipes() {
        return GadgetFabricatingRecipe.allRecipes(minecraft.level);
    }

    /** Icon grid, one per known recipe (2026-08-09, now backed by real recipes) -- every recipe is
     * always shown (never hidden), greyed out with NO_USE_OVERLAY only when the Workbench's own
     * station tier can't reach it yet, per the design's "no surprises, plan ahead" pattern. Ingredient
     * shortfall is NOT what greys an icon here -- that's the detail panel's red/green counts, see
     * #renderFabricationDetail. */
    private void renderFabricationGrid(GuiGraphics guiGraphics, int x, int y) {
        List<RecipeHolder<GadgetFabricatingRecipe>> recipes = fabricationRecipes();

        for (int i = 0; i < recipes.size(); i++) {
            GadgetFabricatingRecipe recipe = recipes.get(i).value();
            int slotX = x + FAB_ICON_X + i * FAB_ICON_SPACING;
            int slotY = y + FAB_ICON_Y;

            guiGraphics.blit(ITEM_SLOT_TEXTURE, slotX, slotY, 0, 0,
                    ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
            guiGraphics.renderItem(recipe.getResult(), slotX + 1, slotY + 1);

            if (i == selectedFabricationIndex) {
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + ITEM_SLOT_SIZE - 1, slotY + ITEM_SLOT_SIZE - 1, SELECTED_OVERLAY_COLOR);
            }

            if (!recipe.meetsTier(menu.getStationTier())) {
                // Same overlay-after-renderItem state dance GraftingTableScreen#renderGhostItem uses:
                // depth test off so the item's leftover depth can't hide this later flat blit, blend
                // explicitly enabled so the overlay's translucent pixels actually blend.
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
    }

    /**
     * Ingredient icons (items left column, fluids right) + Craft button/progress for whichever
     * recipe is currently selected. Icons only -- no inline text -- with a translucent red overlay
     * on anything short of what's needed; exact numbers live in the hover tooltip (see
     * #renderFabricationTooltips). The Craft button itself is a flat fill, no bespoke art exists
     * for this panel yet.
     */
    private void renderFabricationDetail(GuiGraphics guiGraphics, int x, int y) {
        List<RecipeHolder<GadgetFabricatingRecipe>> recipes = fabricationRecipes();
        if (selectedFabricationIndex < 0 || selectedFabricationIndex >= recipes.size()) return;

        GadgetFabricatingRecipe recipe = recipes.get(selectedFabricationIndex).value();
        GearStationPool.Snapshot pool = menu.getPoolSnapshot();

        int itemMaxRow = maxIngredientScrollRow(recipe.items().size());
        int fluidMaxRow = maxIngredientScrollRow(recipe.fluids().size());
        fabItemScrollRow = Math.min(fabItemScrollRow, itemMaxRow);
        fabFluidScrollRow = Math.min(fabFluidScrollRow, fluidMaxRow);

        renderIngredientColumn(guiGraphics, x + FAB_DETAIL_X, y + FAB_DETAIL_Y, recipe.items(), pool, fabItemScrollRow);
        renderFluidColumn(guiGraphics, x + FLUID_COLUMN_X, y + FAB_DETAIL_Y, recipe.fluids(), pool, fabFluidScrollRow);

        if (itemMaxRow > 0) {
            renderScrollbar(guiGraphics, x + FAB_ITEM_SCROLL_X, y + FAB_DETAIL_Y, ITEM_SLOT_SIZE, fabItemScrollRow, itemMaxRow);
        }
        if (fluidMaxRow > 0) {
            renderScrollbar(guiGraphics, x + FAB_FLUID_SCROLL_X, y + FAB_DETAIL_Y, ITEM_SLOT_SIZE, fabFluidScrollRow, fluidMaxRow);
        }

        int actionX = x + FAB_DETAIL_X;
        int actionY = y + FAB_ACTION_Y;

        boolean isActiveJob = menu.isFabricating(recipes.get(selectedFabricationIndex));
        if (isActiveJob) {
            guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, actionX, actionY, 0, 0, FAB_ARROW_WIDTH, FAB_ARROW_HEIGHT,
                    FAB_ARROW_WIDTH, FAB_ARROW_HEIGHT);
            int progressWidth = menu.getScaledFabricationProgress(FAB_ARROW_WIDTH);
            if (progressWidth > 0) {
                guiGraphics.blit(ARROW_FULL_TEXTURE, actionX, actionY, 0, 0, progressWidth, FAB_ARROW_HEIGHT,
                        FAB_ARROW_WIDTH, FAB_ARROW_HEIGHT);
            }
        } else {
            boolean available = !menu.isFabricating()
                    && recipe.meetsTier(menu.getStationTier())
                    && recipe.testItems(pool.items()) && recipe.testFluids(pool.fluids());

            ResourceLocation craftTexture = craftButtonPressedFlash ? CRAFT_BUTTON_PRESSED_TEXTURE : CRAFT_BUTTON_TEXTURE;
            if (!available) {
                RenderSystem.setShaderColor(CRAFT_BUTTON_DISABLED_TINT, CRAFT_BUTTON_DISABLED_TINT, CRAFT_BUTTON_DISABLED_TINT, 1.0F);
            }
            guiGraphics.blit(craftTexture, actionX, actionY, 0, 0, FAB_ACTION_WIDTH, FAB_ACTION_HEIGHT,
                    FAB_ACTION_WIDTH, FAB_ACTION_HEIGHT);
            if (!available) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    /** How many extra rows past the first exist for a {@code count}-long ingredient list -- 0 means
     * everything already fits in the one visible row, no scrollbar needed. */
    private static int maxIngredientScrollRow(int count) {
        return Math.max(0, (count - 1) / FAB_ICONS_PER_ROW);
    }

    /** Item ingredients, one row of {@link #FAB_ICONS_PER_ROW} at a time starting at
     * {@code scrollRow * FAB_ICONS_PER_ROW} -- past-4 ingredients page via the column's own
     * scrollbar (see #renderFabricationDetail) rather than wrapping to a 2nd row, which used to
     * silently overlap the fixed action row below once a recipe needed more than 4 of something. */
    private void renderIngredientColumn(GuiGraphics guiGraphics, int columnX, int columnY, List<ItemStack> required, GearStationPool.Snapshot pool, int scrollRow) {
        int start = scrollRow * FAB_ICONS_PER_ROW;
        for (int i = start; i < Math.min(required.size(), start + FAB_ICONS_PER_ROW); i++) {
            ItemStack requirement = required.get(i);
            int slotX = columnX + (i - start) * ITEM_SLOT_SIZE;
            int slotY = columnY;

            guiGraphics.blit(ITEM_SLOT_TEXTURE, slotX, slotY, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
            guiGraphics.renderItem(requirement, slotX + 1, slotY + 1);

            if (pool.itemCount(requirement) < requirement.getCount()) {
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + ITEM_SLOT_SIZE - 1, slotY + ITEM_SLOT_SIZE - 1, SHORTFALL_OVERLAY_COLOR);
            }
        }
    }

    /** Fluid counterpart to {@link #renderIngredientColumn} -- same one-row-at-a-time/scrollbar
     * shape, same reasoning. Each slot's fill level is the available amount rendered against a tank
     * whose capacity IS the required amount, so a full swatch means "enough," not an absolute
     * reading. Reuses FluidTankRenderer, the same gauge machinery Sunder's fuel tank already uses on
     * this screen's Mod page. */
    private void renderFluidColumn(GuiGraphics guiGraphics, int columnX, int columnY, List<FluidStack> required, GearStationPool.Snapshot pool, int scrollRow) {
        int start = scrollRow * FAB_ICONS_PER_ROW;
        for (int i = start; i < Math.min(required.size(), start + FAB_ICONS_PER_ROW); i++) {
            FluidStack requirement = required.get(i);
            int slotX = columnX + (i - start) * ITEM_SLOT_SIZE;
            int slotY = columnY;

            guiGraphics.blit(ITEM_SLOT_TEXTURE, slotX, slotY, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

            int available = pool.fluidAmount(requirement);
            FluidTankRenderer swatch = fluidSwatchRenderer(requirement);

            if (available <= 0) {
                // Zero available draws nothing at all otherwise (drawFluid's scaledAmount stays 0)
                // -- an empty slot would give no clue what fluid even belongs there. A dim, full-
                // height "ghost" of the required fluid at reduced alpha reads as "not present" while
                // still identifying it.
                FluidStack ghost = new FluidStack(requirement.getFluid(), requirement.getAmount());
                swatch.render(guiGraphics, slotX + 1, slotY + 1, ghost, GHOST_ALPHA);
            } else {
                // Capped to the requirement for display -- "over 100%" just reads as a full swatch,
                // same as the fuel gauge elsewhere never overflows its own bar.
                FluidStack display = new FluidStack(requirement.getFluid(), Math.min(available, requirement.getAmount()));
                swatch.render(guiGraphics, slotX + 1, slotY + 1, display);
            }

            if (available < requirement.getAmount()) {
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + ITEM_SLOT_SIZE - 1, slotY + ITEM_SLOT_SIZE - 1, SHORTFALL_OVERLAY_COLOR);
            }
        }
    }

    /** A 16x16 gauge sized to one fluid requirement -- capacity is the REQUIRED amount (not some
     * fixed tank size), which is what makes a full swatch mean "you have enough" rather than an
     * arbitrary fraction. Built fresh per call since the capacity differs per ingredient. */
    private static FluidTankRenderer fluidSwatchRenderer(FluidStack requirement) {
        return new FluidTankRenderer(Math.max(1, requirement.getAmount()), true, 16, 16);
    }

    /** Icon-grid selection and the Craft button, both scoped to the Fabrication page. Returns
     * whether the click was consumed. */
    private boolean handleFabricationClick(int mouseX, int mouseY, int x, int y) {
        List<RecipeHolder<GadgetFabricatingRecipe>> recipes = fabricationRecipes();

        for (int i = 0; i < recipes.size(); i++) {
            int slotX = x + FAB_ICON_X + i * FAB_ICON_SPACING;
            int slotY = y + FAB_ICON_Y;
            if (MouseUtil.isMouseOver(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) {
                selectedFabricationIndex = i;
                fabItemScrollRow = 0;
                fabFluidScrollRow = 0;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.selectRecipeButtonId(i));
                return true;
            }
        }

        if (selectedFabricationIndex >= 0 && selectedFabricationIndex < recipes.size()
                && !menu.isFabricating(recipes.get(selectedFabricationIndex))) {
            int actionX = x + FAB_DETAIL_X;
            int actionY = y + FAB_ACTION_Y;
            if (MouseUtil.isMouseOver(mouseX, mouseY, actionX, actionY, FAB_ACTION_WIDTH, FAB_ACTION_HEIGHT)) {
                // No client-side gate on the click itself -- startFabrication re-validates
                // everything server-side and simply no-ops if it's not actually available, same
                // trust boundary every other menu button in this class already follows.
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        WorkbenchMenu.startFabricationButtonId(selectedFabricationIndex));
                craftButtonPressedFlash = true;
                craftPressTicks = FILL_PRESS_FLASH_TICKS;
                return true;
            }
        }

        return false;
    }

    // drawScaledString/blitFlippedX moved to AbstractModScreen (generalized for the shared tab-bar
    // helper, dermicraft-progression-notes.md Decision Point #2) -- inherited from there now.

    /** Generalized 2026-09-06 so the Storage strip's scrollbar and the Fabrication detail panel's
     * two independent per-column ones (see #renderFabricationDetail) all share one implementation --
     * {@code trackHeight}/{@code currentRow}/{@code maxRow} are passed in rather than pulled from
     * {@code menu} (which only the Storage strip's scroll is actually backed by; the Fabrication
     * columns are pure client-side state, see #fabItemScrollRow's own javadoc). The Storage strip's
     * 31-tall track uses SCROLL_BAR_TEXTURE at its native size; the Fabrication columns' 18-tall
     * track uses the separate FAB_SCROLL_BAR_TEXTURE (stretching the 31-tall art down to 18 cut off
     * its border), picked by matching trackHeight rather than a caller-passed flag. */
    private void renderScrollbar(GuiGraphics guiGraphics, int trackX, int trackY, int trackHeight, int currentRow, int maxRow) {
        ResourceLocation trackTexture = trackHeight == FAB_SCROLL_BAR_HEIGHT ? FAB_SCROLL_BAR_TEXTURE : SCROLL_BAR_TEXTURE;
        guiGraphics.blit(trackTexture, trackX, trackY, 0, 0,
                SCROLL_BAR_WIDTH, trackHeight, SCROLL_BAR_WIDTH, trackHeight);

        int thumbHeight = thumbHeight(trackHeight, maxRow);
        int thumbY = thumbY(trackY, trackHeight, currentRow, maxRow, thumbHeight);
        int thumbX = thumbX(trackX, trackHeight);
        guiGraphics.blit(SCROLLER_TEXTURE, thumbX, thumbY, 0, 0,
                SCROLLER_WIDTH, thumbHeight, SCROLLER_WIDTH, SCROLLER_NATIVE_HEIGHT);
    }

    // The Storage strip's 31-tall track was tuned with a 2px margin; the Fabrication columns' 18-tall
    // track sat 1px short of both ends at that same margin (a real gap above the thumb when scrolled
    // to the top, and below it when scrolled to the bottom), so it gets its own tighter margin instead.
    private static int trackMargin(int trackHeight) {
        return trackHeight == FAB_SCROLL_BAR_HEIGHT ? 1 : SCROLL_TRACK_MARGIN;
    }

    private static int travel(int trackHeight) {
        return trackHeight - 2 * trackMargin(trackHeight);
    }

    private static int thumbHeight(int trackHeight, int maxRow) {
        int totalRows = maxRow + 1;
        int travel = travel(trackHeight);
        return Math.min(travel, Math.max(SCROLLER_MIN_HEIGHT, travel / totalRows));
    }

    private static int thumbY(int trackY, int trackHeight, int currentRow, int maxRow, int thumbHeight) {
        int thumbY = trackY + trackMargin(trackHeight);
        if (maxRow > 0) {
            thumbY += (int) (((float) currentRow / maxRow) * (travel(trackHeight) - thumbHeight));
        }
        return thumbY;
    }

    private static int thumbX(int trackX, int trackHeight) {
        int offset = trackHeight == FAB_SCROLL_BAR_HEIGHT ? 0 : -1;
        return trackX + (SCROLL_BAR_WIDTH - SCROLLER_WIDTH) / 2 + offset;
    }

    private int trackX() {
        return (width - imageWidth) / 2 + SCROLL_BAR_X;
    }

    private int trackY() {
        return (height - imageHeight) / 2 + WorkbenchMenu.STRIP_Y + BACKGROUND_Y_OFFSET;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int steps = (int) Math.signum(scrollY);
        if (steps == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        boolean up = steps > 0;

        if (menu.getMaxScrollRow() > 0 && isOverStrip((int) mouseX, (int) mouseY)) {
            pressScrollButton(up ? WorkbenchMenu.BUTTON_SCROLL_UP : WorkbenchMenu.BUTTON_SCROLL_DOWN);
            return true;
        }

        if (currentPage == Page.FABRICATION) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;
            GadgetFabricatingRecipe recipe = selectedFabricationRecipe();
            if (recipe != null) {
                int itemMaxRow = maxIngredientScrollRow(recipe.items().size());
                int itemZoneWidth = FAB_ITEM_SCROLL_X - FAB_DETAIL_X + SCROLL_BAR_WIDTH;
                if (itemMaxRow > 0 && MouseUtil.isMouseOver((int) mouseX, (int) mouseY,
                        x + FAB_DETAIL_X, y + FAB_DETAIL_Y, itemZoneWidth, ITEM_SLOT_SIZE)) {
                    fabItemScrollRow = clampRow(fabItemScrollRow + (up ? -1 : 1), itemMaxRow);
                    return true;
                }

                int fluidMaxRow = maxIngredientScrollRow(recipe.fluids().size());
                int fluidZoneWidth = FAB_FLUID_SCROLL_X - FLUID_COLUMN_X + SCROLL_BAR_WIDTH;
                if (fluidMaxRow > 0 && MouseUtil.isMouseOver((int) mouseX, (int) mouseY,
                        x + FLUID_COLUMN_X, y + FAB_DETAIL_Y, fluidZoneWidth, ITEM_SLOT_SIZE)) {
                    fabFluidScrollRow = clampRow(fabFluidScrollRow + (up ? -1 : 1), fluidMaxRow);
                    return true;
                }
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** The recipe the Fabrication grid currently has selected, or null if the selection index is
     * stale/unset -- shared by every scroll/click handler that needs to know the current item/fluid
     * counts without re-deriving the bounds-check each time. */
    @Nullable
    private GadgetFabricatingRecipe selectedFabricationRecipe() {
        List<RecipeHolder<GadgetFabricatingRecipe>> recipes = fabricationRecipes();
        return selectedFabricationIndex >= 0 && selectedFabricationIndex < recipes.size()
                ? recipes.get(selectedFabricationIndex).value()
                : null;
    }

    private static int clampRow(int row, int maxRow) {
        return Math.max(0, Math.min(maxRow, row));
    }

    private static final int FILL_PRESS_FLASH_TICKS = 4;
    private int fillPressTicks = 0;
    private int craftPressTicks = 0;

    @Override
    protected void containerTick() {
        super.containerTick();
        if (fillPressTicks > 0) {
            fillPressTicks--;
            if (fillPressTicks == 0) fillButtonPressedFlash = false;
        }
        if (craftPressTicks > 0) {
            craftPressTicks--;
            if (craftPressTicks == 0) craftButtonPressedFlash = false;
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
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.BUTTON_SET_MOD_PAGE);
            return true;
        }
        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + FABRICATION_TAB_X, y + FABRICATION_TAB_Y, TAB_WIDTH, TAB_HEIGHT)) {
            setPage(Page.FABRICATION);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.BUTTON_SET_FABRICATION_PAGE);
            return true;
        }

        if ((menu.isWorkItemSunder() || menu.isWorkItemShatter())
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + FILL_BUTTON_X, y + FILL_BUTTON_Y,
                FILL_BUTTON_SIZE, FILL_BUTTON_SIZE)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.BUTTON_FILL_FROM_POOL);
            fillButtonPressedFlash = true;
            fillPressTicks = FILL_PRESS_FLASH_TICKS;
            return true;
        }

        if (menu.isWorkItemEater()
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + EATER_FILL_BUTTON_X, y + EATER_FILL_BUTTON_Y,
                FILL_BUTTON_SIZE, FILL_BUTTON_SIZE)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.BUTTON_FILL_FROM_POOL);
            fillButtonPressedFlash = true;
            fillPressTicks = FILL_PRESS_FLASH_TICKS;
            return true;
        }

        if (currentPage == Page.FABRICATION && handleFabricationClick((int) mouseX, (int) mouseY, x, y)) {
            return true;
        }

        if (tryScrollbarClick(ScrollTarget.STORAGE, trackX(), trackY(), SCROLL_BAR_HEIGHT,
                menu.getScrollRow(), menu.getMaxScrollRow(), mouseX, mouseY)) {
            return true;
        }

        if (currentPage == Page.FABRICATION) {
            GadgetFabricatingRecipe recipe = selectedFabricationRecipe();
            if (recipe != null) {
                if (tryScrollbarClick(ScrollTarget.FAB_ITEMS, x + FAB_ITEM_SCROLL_X, y + FAB_DETAIL_Y, ITEM_SLOT_SIZE,
                        fabItemScrollRow, maxIngredientScrollRow(recipe.items().size()), mouseX, mouseY)) {
                    return true;
                }
                if (tryScrollbarClick(ScrollTarget.FAB_FLUIDS, x + FAB_FLUID_SCROLL_X, y + FAB_DETAIL_Y, ITEM_SLOT_SIZE,
                        fabFluidScrollRow, maxIngredientScrollRow(recipe.fluids().size()), mouseX, mouseY)) {
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Shared thumb-grab/track-click detection for whichever scrollbar {@code target} identifies --
     * generalized 2026-09-06 (see {@link ScrollTarget}'s own javadoc). Starts a drag on a thumb hit,
     * or pages one row on a track hit (above the thumb pages up, below pages down); returns whether
     * either happened, so callers can early-return the same way the old Storage-only code did. */
    private boolean tryScrollbarClick(ScrollTarget target, int trackX, int trackY, int trackHeight,
                                       int currentRow, int maxRow, double mouseX, double mouseY) {
        if (maxRow <= 0) return false;

        int thumbHeight = thumbHeight(trackHeight, maxRow);
        int thumbY = thumbY(trackY, trackHeight, currentRow, maxRow, thumbHeight);
        int thumbX = thumbX(trackX, trackHeight);

        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, thumbX, thumbY, SCROLLER_WIDTH, thumbHeight)) {
            draggingScrollbar = target;
            dragGrabOffsetY = (int) mouseY - thumbY;
            lastDraggedRow = currentRow;
            return true;
        }

        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, trackX, trackY, SCROLL_BAR_WIDTH, trackHeight)) {
            boolean up = mouseY < trackY + trackHeight / 2.0;
            applyScrollStep(target, up, currentRow, maxRow);
            return true;
        }

        return false;
    }

    private void applyScrollStep(ScrollTarget target, boolean up, int currentRow, int maxRow) {
        switch (target) {
            case STORAGE -> pressScrollButton(up ? WorkbenchMenu.BUTTON_SCROLL_UP : WorkbenchMenu.BUTTON_SCROLL_DOWN);
            case FAB_ITEMS -> fabItemScrollRow = clampRow(currentRow + (up ? -1 : 1), maxRow);
            case FAB_FLUIDS -> fabFluidScrollRow = clampRow(currentRow + (up ? -1 : 1), maxRow);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar == null) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        int trackHeight;
        int trackTop;
        int maxRow;
        switch (draggingScrollbar) {
            case STORAGE -> {
                trackHeight = SCROLL_BAR_HEIGHT;
                trackTop = trackY();
                maxRow = menu.getMaxScrollRow();
            }
            case FAB_ITEMS -> {
                GadgetFabricatingRecipe recipe = selectedFabricationRecipe();
                trackHeight = ITEM_SLOT_SIZE;
                trackTop = (height - imageHeight) / 2 + FAB_DETAIL_Y;
                maxRow = recipe == null ? 0 : maxIngredientScrollRow(recipe.items().size());
            }
            default -> {
                GadgetFabricatingRecipe recipe = selectedFabricationRecipe();
                trackHeight = ITEM_SLOT_SIZE;
                trackTop = (height - imageHeight) / 2 + FAB_DETAIL_Y;
                maxRow = recipe == null ? 0 : maxIngredientScrollRow(recipe.fluids().size());
            }
        }

        int thumbHeight = thumbHeight(trackHeight, maxRow);
        int usableTravel = travel(trackHeight) - thumbHeight;
        int trackContentTop = trackTop + SCROLL_TRACK_MARGIN;

        int desiredThumbY = (int) mouseY - dragGrabOffsetY;
        int clampedOffset = Math.max(0, Math.min(usableTravel, desiredThumbY - trackContentTop));

        int row = maxRow > 0 && usableTravel > 0
                ? Math.round(((float) clampedOffset / usableTravel) * maxRow)
                : 0;
        row = clampRow(row, maxRow);

        if (row != lastDraggedRow) {
            lastDraggedRow = row;
            switch (draggingScrollbar) {
                case STORAGE -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.setRowButtonId(row));
                case FAB_ITEMS -> fabItemScrollRow = row;
                case FAB_FLUIDS -> fabFluidScrollRow = row;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar != null) {
            draggingScrollbar = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverStrip(int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int stripY = trackY();
        // Widened past STRIP_BACKGROUND_WIDTH to also cover the scrollbar itself, which sits just to
        // the right of the background art (SCROLL_BAR_X = STRIP_X + ITEM_SLOT_BAR_WIDTH + 1 + 6 lands
        // right at the background's own right edge) -- otherwise scrolling while hovering the bar
        // itself, rather than the icon row, did nothing.
        int stripWidth = SCROLL_BAR_X + SCROLL_BAR_WIDTH;
        return MouseUtil.isMouseOver(mouseX, mouseY, x, stripY, stripWidth, STRIP_BACKGROUND_HEIGHT);
    }

    private void pressScrollButton(int buttonId) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }
}
