package net.scruffy.dermicraft.screen.custom.drooling_crucible;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;

public class DroolingCrucibleScreen extends AbstractModScreen<DroolingCrucibleMenu> {

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
    private static final ResourceLocation ARROW_FULL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_fulll.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    private FluidTankRenderer fluidRenderer;

    public DroolingCrucibleScreen(DroolingCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        fluidRenderer = createFluidRenderer16x64(menu.BE.getTank(null).getTankCapacity(0));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, menu.BE.getFluid(), 80, 8, fluidRenderer);
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
