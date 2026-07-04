package net.scruffy.dermicraft.screen.custom.craw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.screen.AbstractModScreen;

public class CrawScreen extends AbstractModScreen<CrawMenu> {

    private static final String PARTS_DIR = "textures/gui/screen_parts/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

    private static final ResourceLocation ARROW_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, PARTS_DIR + "arrow_background.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    // Logical slot positions in CrawMenu -- input feeds into storage, matching the input/arrow/
    // result layout used by Masticator, just without a fill (the transfer here is instant).
    private static final int INPUT_SLOT_X = 51;
    private static final int INPUT_SLOT_Y = 35;
    private static final int STORAGE_SLOT_X = 110;
    private static final int STORAGE_SLOT_Y = 35;
    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 39;

    public CrawScreen(CrawMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        // Draw the true stored count next to the storage slot -- a vanilla slot only shows up
        // to the item's max stack size, but the Craw can hold up to 640 of one item.
        int count = menu.getStoredCount();
        if (count > 0) {
            String text = Integer.toString(count);
            guiGraphics.drawString(this.font, text, STORAGE_SLOT_X + 22, STORAGE_SLOT_Y + 5, 0x404040, false);
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

        renderItemSlot(guiGraphics, x + INPUT_SLOT_X - 1, y + INPUT_SLOT_Y - 1);
        renderItemSlot(guiGraphics, x + STORAGE_SLOT_X - 1, y + STORAGE_SLOT_Y - 1);

        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, x + ARROW_X, y + ARROW_Y, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);
    }

    private void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }
}
