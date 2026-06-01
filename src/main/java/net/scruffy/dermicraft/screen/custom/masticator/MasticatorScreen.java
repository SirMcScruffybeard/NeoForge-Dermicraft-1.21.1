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

public class MasticatorScreen extends AbstractModScreen<MasticatorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/masticator/masticator_gui.png");

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/arrows/arrow_progress_green_15x8.png");

    private FluidTankRenderer fuelRenderer;
    private FluidTankRenderer ingredientRenderer;
    private FluidTankRenderer resultRenderer;


    public MasticatorScreen(MasticatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        fuelRenderer = createFluidRenderer16x40(menu.BE.FUEL_CAPACITY);
        fuelRenderer = createFluidRenderer16x40(menu.BE.INGREDIENT_CAPACITY);
        fuelRenderer = createFluidRenderer16x40(menu.BE.RESULT_CAPACITY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.FUEL_SLOT), 27, 11, fuelRenderer);

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.INGREDIENT_SLOT), 80, 11, ingredientRenderer);

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.RESULT_SLOT), 133, 11, resultRenderer);

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

        fuelRenderer.render(guiGraphics, x + 27, y + 11, menu.BE.getFluid(menu.BE.FUEL_SLOT));
        ingredientRenderer.render(guiGraphics, x + 80, y + 11, menu.BE.getFluid(menu.BE.INGREDIENT_SLOT));
        resultRenderer.render(guiGraphics, x + 133, y + 11, menu.BE.getFluid(menu.BE.RESULT_SLOT));
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            guiGraphics.blit(ARROW_TEXTURE, x + 111, y + 39, 0, 0, menu.getScaledArrowProgress(), 8, 15, 8);
        }
    }
}
