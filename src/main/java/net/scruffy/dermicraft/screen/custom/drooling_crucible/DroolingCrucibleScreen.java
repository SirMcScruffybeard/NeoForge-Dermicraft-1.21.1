package net.scruffy.dermicraft.screen.custom.drooling_crucible;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.AutoDrainToggleClickPayload;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;

import java.util.List;

public class DroolingCrucibleScreen extends AbstractModScreen<DroolingCrucibleMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String ARROWS_DIR = "textures/gui/arrows/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";

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
    private static final ResourceLocation ARROW_FULL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_fulll.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    private static final int TAB_TEXT_COLOR = 0x007F0E;

    private FluidTankRenderer fluidRenderer;
    private List<Tab> tabs;

    // Auto-drain toggle -- see SkinTankScreen's own identical constants/comment.
    private static final ResourceLocation AUTO_DRAIN_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "output_button.png");
    private static final ResourceLocation AUTO_DRAIN_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "no_use_button.png");
    private static final int AUTO_DRAIN_BUTTON_SIZE = 18;
    private static final int AUTO_DRAIN_BUTTON_X = 98;
    private static final int AUTO_DRAIN_BUTTON_Y = 11;

    public DroolingCrucibleScreen(DroolingCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        fluidRenderer = createFluidRenderer16x64(menu.BE.getTank(null).getTankCapacity(0));
        tabs = List.of(
                new Tab(Component.translatable("screen.dermicraft.drooling_crucible.main_tab"), TAB_TEXT_COLOR),
                new Tab(Component.translatable("screen.dermicraft.drooling_crucible.module_tab"), TAB_TEXT_COLOR));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (menu.getActiveTab() == DroolingCrucibleMenu.MAIN_TAB) {
            renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, menu.BE.getFluid(), 80, 8, fluidRenderer);

            if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y,
                    AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
                Component label = menu.isAutoDrainEnabled()
                        ? Component.translatable("tooltip.dermicraft.machine.auto_drain_on")
                        : Component.translatable("tooltip.dermicraft.machine.auto_drain_off");
                guiGraphics.renderTooltip(this.font, label, pMouseX - x, pMouseY - y);
            }
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

        if (menu.getActiveTab() == DroolingCrucibleMenu.MAIN_TAB) {
            renderMainTab(guiGraphics, x, y);
        } else {
            renderModuleTab(guiGraphics, x, y);
        }
    }

    private void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TALL_TANK_TEXTURE, x + 79, y + 7, 0, 0, TALL_TANK_WIDTH, TALL_TANK_HEIGHT,
                TALL_TANK_WIDTH, TALL_TANK_HEIGHT);

        // Slots sit 1px further out than before so the slot-to-arrow gap matches the
        // arrow-to-gauge gap (2px on each side).
        renderItemSlot(guiGraphics, x + 42, y + 33);
        renderItemSlot(guiGraphics, x + 116, y + 33);

        // Left arrow sits in the gap between the input slot and the gauge; the right arrow
        // mirrors it in the equal-sized gap between the gauge and the output slot.
        renderArrowBackground(guiGraphics, x + 61, y + 37);
        renderArrowWithProgress(guiGraphics, x + 61, y + 37);
        renderArrowBackground(guiGraphics, x + 98, y + 37); // output arrow: track only, no progress fill

        fluidRenderer.render(guiGraphics, x + 80, y + 8, menu.BE.getFluid());

        renderAutoDrainButton(guiGraphics, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y);
    }

    private void renderAutoDrainButton(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isAutoDrainEnabled()) {
            blitRotated90(guiGraphics, AUTO_DRAIN_ON_TEXTURE, x, y, AUTO_DRAIN_BUTTON_SIZE);
        } else {
            guiGraphics.blit(AUTO_DRAIN_OFF_TEXTURE, x, y, 0, 0, AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE,
                    AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE);
        }
    }

    /** One yellow Module slot -- nothing else on this tab, matching Eater's/Drinker's/Skin Tank's
     * own bare Module-slot-only look. */
    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(MODULE_SLOT_TEXTURE, x + DroolingCrucibleMenu.MODULE_SLOT_X, y + DroolingCrucibleMenu.MODULE_SLOT_Y, 0, 0,
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

        if (menu.getActiveTab() == DroolingCrucibleMenu.MAIN_TAB
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y,
                AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new AutoDrainToggleClickPayload(menu.BE.getBlockPos()));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    private void renderArrowBackground(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        // Texture carries a 1px transparent margin around a 15x8 opaque arrow, so the draw
        // origin is offset by 1px from the arrow's visually intended position.
        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);
    }

    private void renderArrowWithProgress(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        if (menu.isCrafting()) {
            int progressWidth = 1 + menu.getScaledArrowProgress(); // include the 1px left margin
            guiGraphics.blit(ARROW_FULL_TEXTURE, arrowX, arrowY, 0, 0, progressWidth, ARROW_HEIGHT,
                    ARROW_WIDTH, ARROW_HEIGHT);
        }
    }
}
