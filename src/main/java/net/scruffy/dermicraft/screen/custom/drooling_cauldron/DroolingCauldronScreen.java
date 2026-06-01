package net.scruffy.dermicraft.screen.custom.drooling_cauldron;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.block.entity.custom.DroolingCauldronBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;

public class DroolingCauldronScreen extends AbstractModScreen<DroolingCauldronMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/drooling_cauldron/drooling_cauldron_gui.png");

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID,  "textures/gui/arrows/arrow_progress_green_15x8.png");

    private FluidTankRenderer fluidRenderer;

    public DroolingCauldronScreen(DroolingCauldronMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
    @Override
    protected void init() {
        super.init();

         fluidRenderer = createFluidRenderer16x64(DroolingCauldronBlockEntity.CAPACITY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, menu.be.getFluid(), 80, 8, fluidRenderer);
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
        fluidRenderer.render(guiGraphics, x + 80, y + 8, menu.be.getFluid());

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if(menu.isCrafting()) {
            guiGraphics.blit(ARROW_TEXTURE,x + 62, y + 38, 0, 0, menu.getScaledArrowProgress(), 8, 15, 8);
        }
    }
}
