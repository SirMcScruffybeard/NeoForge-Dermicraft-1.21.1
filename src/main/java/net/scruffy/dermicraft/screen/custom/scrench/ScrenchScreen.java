package net.scruffy.dermicraft.screen.custom.scrench;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import org.jetbrains.annotations.NotNull;

/**
 * Sunder's field maintenance screen -- chain slot (left) + fuel tank/slot pairing (right), per the
 * GUI layout design notes in {@code dermicraft-gadget-notes.md}. Reuses the same shared
 * screen_background/tank_and_slot/item_slot assets every other screen in the mod uses, no
 * dedicated Scrench art yet.
 */
public class ScrenchScreen extends AbstractModScreen<ScrenchMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";

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

    private FluidTankRenderer fuelRenderer;

    public ScrenchScreen(ScrenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        fuelRenderer = createFluidRenderer16x40(menu.getSunderFuelCapacity());
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, menu.getSunderFluid(),
                ScrenchMenu.fuelTankX() + 1, ScrenchMenu.fuelTankY() + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                ScrenchMenu.chainSlotX() + 1, ScrenchMenu.chainSlotY() + 1,
                menu.getSlot(0).getItem(), Component.translatable("tooltip.dermicraft.slot.chain"));
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

        guiGraphics.blit(ITEM_SLOT_TEXTURE, x + ScrenchMenu.chainSlotX(), y + ScrenchMenu.chainSlotY(), 0, 0,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);

        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + ScrenchMenu.fuelTankX(), y + ScrenchMenu.fuelTankY(), 0, 0,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);

        fuelRenderer.render(guiGraphics, x + ScrenchMenu.fuelTankX() + 1, y + ScrenchMenu.fuelTankY() + 1, menu.getSunderFluid());
    }
}
