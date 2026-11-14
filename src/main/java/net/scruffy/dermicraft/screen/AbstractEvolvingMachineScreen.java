package net.scruffy.dermicraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.AutoDrainToggleClickPayload;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The screen shared by every machine that has moved onto the evolution engine. It owns what they all draw the same
 * way: the background and tabs, the health bar (visible on both tabs), the auto-drain button and its tooltip, the
 * Module tab with the Evolution slot, the tolerance panel and its tooltips, and the small drawing helpers for tank
 * and item-slot backdrops and the progress arrow. A machine's screen supplies only its Main tab: what to draw, which
 * tooltips to show, and where the auto-drain button sits.
 *
 * @param <M> the machine's menu
 */
public abstract class AbstractEvolvingMachineScreen<M extends AbstractEvolvingMachineMenu<?>> extends AbstractModScreen<M> {

    private static final int TAB_TEXT_COLOR = 0x007F0E;

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String ARROWS_DIR = "textures/gui/arrows/";
    private static final String HEALTH_DIR = "textures/gui/health/";
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

    private static final ResourceLocation ARROW_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_background.png");
    private static final ResourceLocation ARROW_FULL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_fulll.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    private static final ResourceLocation HP_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, HEALTH_DIR + "hp_background.png");
    private static final ResourceLocation HP_GREEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, HEALTH_DIR + "hp_green.png");
    private static final ResourceLocation HP_YELLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, HEALTH_DIR + "hp_yellow.png");
    private static final ResourceLocation HP_RED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, HEALTH_DIR + "hp_red.png");
    private static final int HP_BAR_WIDTH = 18;
    private static final int HP_BAR_HEIGHT = 66;
    private static final int HP_BAR_INTERIOR_HEIGHT = 64; // opaque interior height used for the fill crop
    private static final int HEALTH_BAR_X = 8;
    private static final int HEALTH_BAR_Y = 11; // top of the fluid tank renderers

    // Fluid machines call this "auto drain", item-output machines "auto dispense"; same shared icons either way
    // (output, rotated 90 degrees, for ON; no-use for OFF).
    private static final ResourceLocation AUTO_DRAIN_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "output_button.png");
    private static final ResourceLocation AUTO_DRAIN_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "no_use_button.png");
    protected static final int AUTO_DRAIN_BUTTON_SIZE = 18;

    private final String screenName;
    private final String autoDrainWording;

    private List<Tab> tabs;
    /** Where the Evolution slot, tier icon, label and hazard icons go; set in init() once the font is known. */
    private EvolutionPanel.Layout panelLayout;

    /**
     * @param screenName       the machine's name in the lang keys {@code screen.dermicraft.<name>.main_tab} and
     *                         {@code .module_tab}
     * @param autoDrainWording {@code auto_drain} for a machine that pushes fluid out, {@code auto_dispense} for one
     *                         that pushes items out; picks the button's tooltip
     */
    protected AbstractEvolvingMachineScreen(M menu, Inventory playerInventory, Component title,
                                            String screenName, String autoDrainWording) {
        super(menu, playerInventory, title);
        this.screenName = screenName;
        this.autoDrainWording = autoDrainWording;
    }

    @Override
    protected void init() {
        super.init();
        panelLayout = EvolutionPanel.Layout.of(font, AbstractEvolvingMachineMenu.EVOLUTION_SLOT_X, showsTolerances());
        tabs = List.of(
                new Tab(Component.translatable("screen.dermicraft." + screenName + ".main_tab"), TAB_TEXT_COLOR),
                new Tab(Component.translatable("screen.dermicraft." + screenName + ".module_tab"), TAB_TEXT_COLOR));
    }

    @Override
    public Rect2i getTabStripArea() {
        return getStackedTabArea(2);
    }

    /** Where the Main tab's progress arrow sits, relative to the screen's top-left, so JEI can offer the machine's recipes
     * when the arrow is clicked. Null for a machine with no recipe arrow of its own. */
    protected @Nullable Rect2i getRecipeArrowArea() {
        return null;
    }

    /** {@link #getRecipeArrowArea} made absolute, and null on the Module tab where the arrow is not drawn. */
    public @Nullable Rect2i getRecipeClickArea() {
        Rect2i arrow = getRecipeArrowArea();
        if (arrow == null || menu.getActiveTab() != AbstractEvolvingMachineMenu.MAIN_TAB) return null;
        return new Rect2i(leftPos + arrow.getX(), topPos + arrow.getY(), arrow.getWidth(), arrow.getHeight());
    }

    /** The arrow's own footprint at a Main-tab position, for a machine's {@link #getRecipeArrowArea}. */
    protected static Rect2i arrowAt(int arrowX, int arrowY) {
        return new Rect2i(arrowX, arrowY, ARROW_WIDTH, ARROW_HEIGHT);
    }

    // ---- What a machine supplies -------------------------------------------------------------

    /** Draws the Main tab's tanks, slots, arrow and fluids; {@code x/y} is the screen's top-left. */
    protected abstract void renderMainTab(GuiGraphics guiGraphics, int x, int y);

    /** Draws the Main tab's hover tooltips (gauges and slots); {@code x/y} is the screen's top-left. */
    protected abstract void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y);

    /** Where the auto-drain button sits on the Main tab, relative to the screen's top-left. */
    protected abstract int getAutoDrainButtonX();

    protected abstract int getAutoDrainButtonY();

    /** Whether the Module tab shows the hazard tolerances (the "Tolerances:" label and icons). A machine with no hazard
     * concept, such as the Drooling family, returns false and shows only its tier. */
    protected boolean showsTolerances() {
        return true;
    }

    /** A machine's own extra Main-tab buttons (the Mutator's mode toggle): handle a click and return true if it was
     * on one. {@code x/y} is the screen's top-left. Nothing by default. */
    protected boolean handleMainTabClick(double mouseX, double mouseY, int x, int y) {
        return false;
    }

    // ---- Shared drawing ----------------------------------------------------------------------

    @Override
    protected final void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight,
                BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
        renderPlayerInventoryBackdrop(guiGraphics, x, y);
        renderTabs(guiGraphics, x, y, tabs, menu.getActiveTab());

        renderHealthBar(guiGraphics, x, y); // ambient status, visible on both tabs

        if (menu.getActiveTab() == AbstractEvolvingMachineMenu.MAIN_TAB) {
            renderMainTab(guiGraphics, x, y);
            renderAutoDrainButton(guiGraphics, x + getAutoDrainButtonX(), y + getAutoDrainButtonY());
        } else {
            renderModuleTab(guiGraphics, x, y);
        }
    }

    @Override
    protected final void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (menu.getActiveTab() == AbstractEvolvingMachineMenu.MAIN_TAB) {
            renderMainTabTooltips(guiGraphics, mouseX, mouseY, x, y);

            if (MouseUtil.isMouseOver(mouseX, mouseY, x + getAutoDrainButtonX(), y + getAutoDrainButtonY(),
                    AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
                Component label = Component.translatable("tooltip.dermicraft.machine." + autoDrainWording
                        + (menu.isAutoDrainEnabled() ? "_on" : "_off"));
                guiGraphics.renderTooltip(this.font, label, mouseX - x, mouseY - y);
            }
        }

        // The health bar stays interactive on both tabs: ambient status, not tied to either tab's content.
        if (menu.getMaxHealth() > 0
                && MouseUtil.isMouseOver(mouseX, mouseY, x + HEALTH_BAR_X, y + HEALTH_BAR_Y, HP_BAR_WIDTH, HP_BAR_HEIGHT)) {
            int maxHealth = menu.getMaxHealth();
            int percent = maxHealth <= 0 ? 0 : Math.round(100f * menu.getHealth() / maxHealth);
            guiGraphics.renderTooltip(this.font, Component.literal("HP: " + percent + "%"), mouseX - x, mouseY - y);
        }

        if (menu.getActiveTab() == AbstractEvolvingMachineMenu.MODULE_TAB) {
            if (EvolutionPanel.isEvolutionSlot(hoveredSlot, menu.BE.EVOLUTION_INVENTORY) && !hoveredSlot.hasItem()) {
                guiGraphics.renderComponentTooltip(this.font, EvolutionPanel.getEmptySlotTooltip(), mouseX - x, mouseY - y);
            }
            EvolutionPanel.renderTooltip(guiGraphics, font, mouseX - x, mouseY - y, getEvolutionPanelCells());
        }
    }

    /** The icons under the slots on the Module tab: this machine's tier and tolerances, see {@link EvolutionPanel}. */
    private List<EvolutionPanel.Cell> getEvolutionPanelCells() {
        return EvolutionPanel.getCells(panelLayout, menu.BE.getEvolutionState(), menu.BE.getModuleInventory(),
                menu.BE.EVOLUTION_INVENTORY, menu.BE::getHazardIntensity);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = super.getTooltipFromContainerItem(stack);
        if (!EvolutionPanel.isEvolutionSlot(hoveredSlot, menu.BE.EVOLUTION_INVENTORY)) return lines;
        List<Component> withStatus = new ArrayList<>(lines);
        withStatus.addAll(EvolutionPanel.getStatusLines(menu.BE.getEvolutionStatus()));
        return withStatus;
    }

    /** The Module slots' backdrops, the Evolution slot with its progress bar, and the tolerance panel. */
    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        EvolutionPanel.renderSlot(guiGraphics, x, y, panelLayout, menu.BE.getGradualEvolutionFraction());
        for (int i = 0; i < menu.BE.getModuleSlotCount(); i++) {
            guiGraphics.blit(MODULE_SLOT_TEXTURE,
                    x + AbstractEvolvingMachineMenu.MODULE_SLOT_X + i * AbstractEvolvingMachineMenu.MODULE_SLOT_SPACING,
                    y + AbstractEvolvingMachineMenu.MODULE_SLOT_Y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        }
        EvolutionPanel.render(guiGraphics, font, x, y, panelLayout, getEvolutionPanelCells());
    }

    private void renderAutoDrainButton(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isAutoDrainEnabled()) {
            blitRotated90(guiGraphics, AUTO_DRAIN_ON_TEXTURE, x, y, AUTO_DRAIN_BUTTON_SIZE);
        } else {
            guiGraphics.blit(AUTO_DRAIN_OFF_TEXTURE, x, y, 0, 0, AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE,
                    AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE);
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

        if (menu.getActiveTab() == AbstractEvolvingMachineMenu.MAIN_TAB && handleMainTabClick(mouseX, mouseY, x, y)) {
            return true;
        }

        if (menu.getActiveTab() == AbstractEvolvingMachineMenu.MAIN_TAB
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + getAutoDrainButtonX(), y + getAutoDrainButtonY(),
                AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new AutoDrainToggleClickPayload(menu.BE.getBlockPos()));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---- Helpers for a machine's Main tab ----------------------------------------------------

    /** A tank's backdrop with its container slot below it; {@code x/y} is the backdrop's top-left. */
    protected void renderTankAndSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x, y, 0, 0, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
    }

    /** An item slot's backdrop; {@code x/y} is the backdrop's top-left (the slot itself sits 1px inside). */
    protected void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    /** The empty arrow track alone, with no progress fill. */
    protected void renderArrowTrack(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);
    }

    /** The crafting-progress arrow at the given absolute position; its textures carry a 1px transparent margin. */
    protected void renderProgressArrow(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);

        if (menu.isCrafting()) {
            int progressWidth = 1 + menu.getScaledArrowProgress(); // include the 1px left margin
            guiGraphics.blit(ARROW_FULL_TEXTURE, arrowX, arrowY, 0, 0, progressWidth, ARROW_HEIGHT,
                    ARROW_WIDTH, ARROW_HEIGHT);
        }
    }

    private void renderHealthBar(GuiGraphics guiGraphics, int x, int y) {
        int maxHealth = menu.getMaxHealth();
        if (maxHealth <= 0) return;

        int barX = x + HEALTH_BAR_X;
        int barY = y + HEALTH_BAR_Y;

        guiGraphics.blit(HP_BACKGROUND_TEXTURE, barX, barY, 0, 0, HP_BAR_WIDTH, HP_BAR_HEIGHT,
                HP_BAR_WIDTH, HP_BAR_HEIGHT);

        float ratio = menu.getHealth() / (float) maxHealth;
        int filledHeight = Math.round(HP_BAR_INTERIOR_HEIGHT * ratio);
        if (filledHeight <= 0) return;

        // Fills from the bottom up, like a thermometer, so it empties downward as HP drops.
        int vOffset = HP_BAR_HEIGHT - 1 - filledHeight;
        guiGraphics.blit(getHealthBarTexture(ratio), barX, barY + vOffset, 0, vOffset,
                HP_BAR_WIDTH, filledHeight, HP_BAR_WIDTH, HP_BAR_HEIGHT);
    }

    private ResourceLocation getHealthBarTexture(float ratio) {
        if (ratio > 0.5f) return HP_GREEN_TEXTURE;
        if (ratio > 0.2f) return HP_YELLOW_TEXTURE;
        return HP_RED_TEXTURE;
    }
}
