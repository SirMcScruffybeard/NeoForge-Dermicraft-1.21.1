package net.scruffy.dermicraft.screen.custom.effluentcer;

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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EffluentcerScreen extends AbstractModScreen<EffluentcerMenu> {

    private static final int TAB_TEXT_COLOR = 0x007F0E;
    private List<Tab> tabs;

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
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

    // "Inline pair" layout: HP far-left, fuel far-right, the two ingredient tanks together
    // left of center, a single progress arrow right of center, result beyond it.
    private static final int HEALTH_BAR_X = 8;
    private static final int HEALTH_BAR_Y = 11;
    private static final int INPUT_A_X = 40;
    private static final int INPUT_B_X = 60;
    private static final int ARROW_X = 86;
    private static final int ARROW_Y = 38;
    private static final int RESULT_X = 112;
    private static final int FUEL_X = 150;
    private static final int TANK_Y = 11;

    private FluidTankRenderer fuelRenderer;
    private FluidTankRenderer inputARenderer;
    private FluidTankRenderer inputBRenderer;
    private FluidTankRenderer resultRenderer;

    // Auto-drain toggle -- the only free gap on this row is between the result tank (RESULT_X=112,
    // 18px wide -> ends 130) and the fuel tank (FUEL_X=150), a 20px gap that exactly fits an 18px
    // icon with a 1px margin either side.
    private static final ResourceLocation AUTO_DRAIN_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "output_button.png");
    private static final ResourceLocation AUTO_DRAIN_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "no_use_button.png");
    private static final int AUTO_DRAIN_BUTTON_SIZE = 18;
    private static final int AUTO_DRAIN_BUTTON_X = 131;
    private static final int AUTO_DRAIN_BUTTON_Y = TANK_Y;

    public EffluentcerScreen(EffluentcerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        fuelRenderer = createFluidRenderer16x40(menu.BE.getFuelTank().getCapacity());
        inputARenderer = createFluidRenderer16x40(menu.BE.getInputATank().getCapacity());
        inputBRenderer = createFluidRenderer16x40(menu.BE.getInputBTank().getCapacity());
        resultRenderer = createFluidRenderer16x40(menu.BE.getResultTank().getCapacity());
        tabs = List.of(
                new Tab(Component.translatable("screen.dermicraft.effluentcer.main_tab"), TAB_TEXT_COLOR),
                new Tab(Component.translatable("screen.dermicraft.effluentcer.module_tab"), TAB_TEXT_COLOR));
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (menu.getActiveTab() != EffluentcerMenu.MAIN_TAB) return;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getInputATank().SLOT), INPUT_A_X + 1, TANK_Y + 1, inputARenderer,
                Component.translatable("tooltip.dermicraft.gauge.input_a"));

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getInputBTank().SLOT), INPUT_B_X + 1, TANK_Y + 1, inputBRenderer,
                Component.translatable("tooltip.dermicraft.gauge.input_b"));

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getResultTank().SLOT), RESULT_X + 1, TANK_Y + 1, resultRenderer,
                Component.translatable("tooltip.dermicraft.gauge.result"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, FUEL_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, INPUT_A_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getInputATank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.input_a_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, INPUT_B_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getInputBTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.input_b_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, RESULT_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getResultTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.result_container"));

        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + HEALTH_BAR_X, y + HEALTH_BAR_Y, HP_BAR_WIDTH, HP_BAR_HEIGHT)) {
            int maxHealth = menu.getMaxHealth();
            int percent = maxHealth <= 0 ? 0 : Math.round(100f * menu.getHealth() / maxHealth);
            guiGraphics.renderTooltip(this.font,
                    Component.literal("HP: " + percent + "%"),
                    pMouseX - x, pMouseY - y);
        }

        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y,
                AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
            Component label = menu.isAutoDrainEnabled()
                    ? Component.translatable("tooltip.dermicraft.machine.auto_drain_on")
                    : Component.translatable("tooltip.dermicraft.machine.auto_drain_off");
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

        renderHealthBar(guiGraphics, x, y); // ambient status, visible on both tabs

        if (menu.getActiveTab() == EffluentcerMenu.MAIN_TAB) {
            renderMainTab(guiGraphics, x, y);
        } else {
            renderModuleTab(guiGraphics, x, y);
        }
    }

    private void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        renderTankAndSlot(guiGraphics, x + FUEL_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + INPUT_A_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + INPUT_B_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + RESULT_X, y + TANK_Y);

        renderProgressArrow(guiGraphics, x, y);

        fuelRenderer.render(guiGraphics, x + FUEL_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
        inputARenderer.render(guiGraphics, x + INPUT_A_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getInputATank().SLOT));
        inputBRenderer.render(guiGraphics, x + INPUT_B_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getInputBTank().SLOT));
        resultRenderer.render(guiGraphics, x + RESULT_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getResultTank().SLOT));

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

    /** One yellow Module slot -- nothing else on this tab, matching every other machine's own bare
     * Module-slot-only look. */
    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(MODULE_SLOT_TEXTURE, x + EffluentcerMenu.MODULE_SLOT_X, y + EffluentcerMenu.MODULE_SLOT_Y, 0, 0,
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

        if (menu.getActiveTab() == EffluentcerMenu.MAIN_TAB
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y,
                AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new AutoDrainToggleClickPayload(menu.BE.getBlockPos()));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTankAndSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x, y, 0, 0, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        // Textures carry a 1px transparent margin around a 15x8 opaque arrow, so the draw
        // origin is offset by 1px to keep the arrow visually aligned.
        int arrowX = x + ARROW_X;
        int arrowY = y + ARROW_Y;

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
