package net.scruffy.dermicraft.screen.custom.effluentcer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.NotNull;

public class EffluentcerScreen extends AbstractModScreen<EffluentcerMenu> {

    private static final String PARTS_DIR = "textures/gui/screen_parts/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation TANK_AND_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "tank_and_slot.png");
    private static final int TANK_AND_SLOT_WIDTH = 18;
    private static final int TANK_AND_SLOT_HEIGHT = 66;

    private static final ResourceLocation ARROW_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "arrow_background.png");
    private static final ResourceLocation ARROW_FULL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "arrow_fulll.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    private static final ResourceLocation HP_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "hp_background.png");
    private static final ResourceLocation HP_GREEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "hp_green.png");
    private static final ResourceLocation HP_YELLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "hp_yellow.png");
    private static final ResourceLocation HP_RED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "hp_red.png");
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
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer);

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getInputATank().SLOT), INPUT_A_X + 1, TANK_Y + 1, inputARenderer);

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getInputBTank().SLOT), INPUT_B_X + 1, TANK_Y + 1, inputBRenderer);

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getResultTank().SLOT), RESULT_X + 1, TANK_Y + 1, resultRenderer);

        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + HEALTH_BAR_X, y + HEALTH_BAR_Y, HP_BAR_WIDTH, HP_BAR_HEIGHT)) {
            int maxHealth = menu.getMaxHealth();
            int percent = maxHealth <= 0 ? 0 : Math.round(100f * menu.getHealth() / maxHealth);
            guiGraphics.renderTooltip(this.font,
                    Component.literal("HP: " + percent + "%"),
                    pMouseX - x, pMouseY - y);
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

        renderTankAndSlot(guiGraphics, x + FUEL_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + INPUT_A_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + INPUT_B_X, y + TANK_Y);
        renderTankAndSlot(guiGraphics, x + RESULT_X, y + TANK_Y);

        renderProgressArrow(guiGraphics, x, y);
        renderHealthBar(guiGraphics, x, y);

        fuelRenderer.render(guiGraphics, x + FUEL_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
        inputARenderer.render(guiGraphics, x + INPUT_A_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getInputATank().SLOT));
        inputBRenderer.render(guiGraphics, x + INPUT_B_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getInputBTank().SLOT));
        resultRenderer.render(guiGraphics, x + RESULT_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getResultTank().SLOT));
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
