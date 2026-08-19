package net.scruffy.dermicraft.screen.custom.skin_tank;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.AbstractModScreen;

import java.util.List;

/**
 * Pilot for the shared tab-bar helper (dermicraft-progression-notes.md, Decision Point #2 ->
 * sequencing step 4): a Main tab (the existing tank+slots content, unchanged) and a Module tab (one
 * yellow Module slot, {@link SkinTankBlockEntity#installedHazardProfile} reads whatever's in it).
 */
public class SkinTankScreen extends AbstractModScreen<SkinTankMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String ARROWS_DIR = "textures/gui/arrows/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation TALL_TANK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tall_tank_guage.png");
    private static final int TALL_TANK_WIDTH = 18;
    private static final int TALL_TANK_HEIGHT = 66;

    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

    private static final ResourceLocation ARROW_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_background.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    private FluidTankRenderer fluidRenderer;

    public SkinTankScreen(SkinTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // Tab bar -- see AbstractModScreen's "Shared tab-bar state"/Tab record. Only two entries;
    // labels/colors mirror WorkbenchScreen's own tab text convention (a single green, since Skin
    // Tank doesn't need per-tab colors to tell them apart the way Workbench's Mod/Fabrication split
    // might eventually want).
    private static final int TAB_TEXT_COLOR = 0x007F0E;
    private List<Tab> tabs;

    @Override
    protected void init() {
        super.init();
        fluidRenderer = createFluidRenderer16x64(SkinTankBlockEntity.CAPACITY);
        tabs = List.of(
                new Tab(Component.translatable("screen.dermicraft.skin_tank.main_tab"), TAB_TEXT_COLOR),
                new Tab(Component.translatable("screen.dermicraft.skin_tank.module_tab"), TAB_TEXT_COLOR));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (menu.getActiveTab() == SkinTankMenu.MAIN_TAB) {
            renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, menu.be.getFluid(), 80, 8, fluidRenderer);
        }
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
        renderTabs(guiGraphics, x, y, tabs, menu.getActiveTab());

        if (menu.getActiveTab() == SkinTankMenu.MAIN_TAB) {
            renderMainTab(guiGraphics, x, y);
        } else {
            renderModuleTab(guiGraphics, x, y);
        }
    }

    private void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TALL_TANK_TEXTURE, x + 79, y + 7, 0, 0, TALL_TANK_WIDTH, TALL_TANK_HEIGHT,
                TALL_TANK_WIDTH, TALL_TANK_HEIGHT);

        // Slots sit 1px further out than their logical slot position so the slot-to-arrow
        // gap matches the arrow-to-gauge gap (2px on each side), same as the Drooling Cauldron.
        renderItemSlot(guiGraphics, x + 42, y + 33);
        renderItemSlot(guiGraphics, x + 116, y + 33);

        // No crafting/progress concept on this machine, so both arrows are track-only.
        renderArrowBackground(guiGraphics, x + 61, y + 37);
        renderArrowBackground(guiGraphics, x + 98, y + 37);

        fluidRenderer.render(guiGraphics, x + 80, y + 8, menu.be.getFluid());
    }

    /** One yellow Module slot -- nothing else on this tab, matching Eater's/Drinker's own bare
     * Module-slot-only look where no gauge/drain slot sits alongside it. */
    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(MODULE_SLOT_TEXTURE, x + SkinTankMenu.MODULE_SLOT_X, y + SkinTankMenu.MODULE_SLOT_Y, 0, 0,
                SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
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

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    private void renderArrowBackground(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);
    }
}
