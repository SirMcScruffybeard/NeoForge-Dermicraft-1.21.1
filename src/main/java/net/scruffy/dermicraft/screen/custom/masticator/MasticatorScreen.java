package net.scruffy.dermicraft.screen.custom.masticator;

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

public class MasticatorScreen extends AbstractModScreen<MasticatorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/masticator/masticator_gui.png");

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/arrows/arrow_progress_green_15x8.png");

    private FluidTankRenderer fuelRenderer;
    private FluidTankRenderer ingredientRenderer;
    private FluidTankRenderer resultRenderer;

    // Placeholder placement/size -- easy to move once real art exists.
    private static final int HEALTH_BAR_X = 8; // aligned with the player inventory's left edge
    private static final int HEALTH_BAR_Y = 11; // top of the fluid tank renderers
    private static final int HEALTH_BAR_WIDTH = 5;
    private static final int HEALTH_BAR_HEIGHT = 64; // bottom of the item slots (y=59, 16px tall) minus HEALTH_BAR_Y


    public MasticatorScreen(MasticatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        fuelRenderer = createFluidRenderer16x40(menu.BE.getFuelTank().getCapacity());
        ingredientRenderer = createFluidRenderer16x40(menu.BE.getIngredientTank().getCapacity());
        resultRenderer = createFluidRenderer16x40(menu.BE.getResultTank().getCapacity());
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), 27, 11, fuelRenderer);

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getIngredientTank().SLOT), 80, 11, ingredientRenderer);

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getResultTank().SLOT), 133, 11, resultRenderer);

        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + HEALTH_BAR_X, y + HEALTH_BAR_Y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT)) {
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
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, x, y);
        renderHealthBar(guiGraphics, x, y);

        fuelRenderer.render(guiGraphics, x + 27, y + 11, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
        ingredientRenderer.render(guiGraphics, x + 80, y + 11, menu.BE.getFluid(menu.BE.getIngredientTank().SLOT));
        resultRenderer.render(guiGraphics, x + 133, y + 11, menu.BE.getFluid(menu.BE.getResultTank().SLOT));
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            guiGraphics.blit(ARROW_TEXTURE, x + 111, y + 39, 0, 0, menu.getScaledArrowProgress(), 8, 15, 8);
        }
    }

    private void renderHealthBar(GuiGraphics guiGraphics, int x, int y) {
        int maxHealth = menu.getMaxHealth();
        if (maxHealth <= 0) return;

        float ratio = menu.getHealth() / (float) maxHealth;
        int filledHeight = Math.round(HEALTH_BAR_HEIGHT * ratio);

        guiGraphics.fill(x + HEALTH_BAR_X, y + HEALTH_BAR_Y,
                x + HEALTH_BAR_X + HEALTH_BAR_WIDTH, y + HEALTH_BAR_Y + HEALTH_BAR_HEIGHT, 0xFF404040);
        // Fills from the bottom up, like a thermometer, so it empties downward as HP drops.
        guiGraphics.fill(x + HEALTH_BAR_X, y + HEALTH_BAR_Y + HEALTH_BAR_HEIGHT - filledHeight,
                x + HEALTH_BAR_X + HEALTH_BAR_WIDTH, y + HEALTH_BAR_Y + HEALTH_BAR_HEIGHT, getHealthBarColor(ratio));
    }

    private int getHealthBarColor(float ratio) {
        if (ratio > 0.5f) return 0xFF2E6B30;
        if (ratio > 0.2f) return 0xFFE0C020;
        return 0xFFCC3333;
    }
}
