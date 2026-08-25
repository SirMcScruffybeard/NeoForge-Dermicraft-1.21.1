package net.scruffy.dermicraft.screen.custom.charred_craw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.CrawAutoPushToggleClickPayload;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;

import java.util.List;

/** Charred Craw's screen -- identical GUI layout/textures to {@code CrawScreen} (Module tab
 * included; the capability leaps are entirely backend capacity/throughput, not a different GUI),
 * just typed to {@link CharredCrawMenu}. See that class's javadoc for why this is a separate class
 * rather than reusing CrawScreen. */
public class CharredCrawScreen extends AbstractModScreen<CharredCrawMenu> {

    private static final int TAB_TEXT_COLOR = 0x007F0E;
    private List<Tab> tabs;

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String ARROWS_DIR = "textures/gui/arrows/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

    private static final ResourceLocation ARROW_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_background.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    private static final ResourceLocation AUTO_PUSH_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "output_button.png");
    private static final ResourceLocation AUTO_PUSH_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "no_use_button.png");
    private static final int AUTO_PUSH_BUTTON_SIZE = 18;

    private static final int INPUT_SLOT_X = 51;
    private static final int INPUT_SLOT_Y = 35;
    private static final int STORAGE_SLOT_X = 110;
    private static final int STORAGE_SLOT_Y = 35;
    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 39;

    private static final int AUTO_PUSH_BUTTON_X = STORAGE_SLOT_X + ITEM_SLOT_SIZE + 4;
    private static final int AUTO_PUSH_BUTTON_Y = STORAGE_SLOT_Y - 1;

    public CharredCrawScreen(CharredCrawMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        tabs = List.of(
                new Tab(Component.translatable("screen.dermicraft.charred_craw.main_tab"), TAB_TEXT_COLOR),
                new Tab(Component.translatable("screen.dermicraft.charred_craw.module_tab"), TAB_TEXT_COLOR));
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

        if (menu.getActiveTab() == CharredCrawMenu.MAIN_TAB
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + AUTO_PUSH_BUTTON_X, y + AUTO_PUSH_BUTTON_Y,
                AUTO_PUSH_BUTTON_SIZE, AUTO_PUSH_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new CrawAutoPushToggleClickPayload(menu.be.getBlockPos()));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        if (menu.getActiveTab() != CharredCrawMenu.MAIN_TAB) return;

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + AUTO_PUSH_BUTTON_X, y + AUTO_PUSH_BUTTON_Y,
                AUTO_PUSH_BUTTON_SIZE, AUTO_PUSH_BUTTON_SIZE)) {
            Component label = menu.isAutoPushEnabled()
                    ? Component.translatable("tooltip.dermicraft.craw.auto_push_on")
                    : Component.translatable("tooltip.dermicraft.craw.auto_push_off");
            guiGraphics.renderTooltip(this.font, label, pMouseX - x, pMouseY - y);
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

        if (menu.getActiveTab() == CharredCrawMenu.MAIN_TAB) {
            renderMainTab(guiGraphics, x, y);
        } else {
            renderModuleTab(guiGraphics, x, y);
        }
    }

    private void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        renderItemSlot(guiGraphics, x + INPUT_SLOT_X - 1, y + INPUT_SLOT_Y - 1);
        renderItemSlot(guiGraphics, x + STORAGE_SLOT_X - 1, y + STORAGE_SLOT_Y - 1);

        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, x + ARROW_X, y + ARROW_Y, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);

        renderAutoPushButton(guiGraphics, x + AUTO_PUSH_BUTTON_X, y + AUTO_PUSH_BUTTON_Y);
    }

    private void renderAutoPushButton(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isAutoPushEnabled()) {
            blitRotated90(guiGraphics, AUTO_PUSH_ON_TEXTURE, x, y, AUTO_PUSH_BUTTON_SIZE);
        } else {
            guiGraphics.blit(AUTO_PUSH_OFF_TEXTURE, x, y, 0, 0, AUTO_PUSH_BUTTON_SIZE, AUTO_PUSH_BUTTON_SIZE,
                    AUTO_PUSH_BUTTON_SIZE, AUTO_PUSH_BUTTON_SIZE);
        }
    }

    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(MODULE_SLOT_TEXTURE, x + CharredCrawMenu.MODULE_SLOT_X, y + CharredCrawMenu.MODULE_SLOT_Y, 0, 0,
                SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }

    private void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }
}
