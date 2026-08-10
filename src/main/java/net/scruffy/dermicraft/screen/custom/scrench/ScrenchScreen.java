package net.scruffy.dermicraft.screen.custom.scrench;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.item.custom.EaterItem;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic field maintenance screen, opened by the Scrench. Generalized 2026-08-09 alongside
 * {@link ScrenchMenu} -- the menu's own slot-building is fully gadget-agnostic now (see that class),
 * but the actual background art still branches per gadget here, since Sunder's chain+fuel-gauge look
 * and Eater's Module-row+buffer-column look are genuinely different panels, not two skins of the
 * same layout. This is the deliberate line: {@code IWorkbenchSwappable} shares slot *behavior*
 * across every host, while each host still owns how it *draws* whichever gadget is present.
 */
public class ScrenchScreen extends AbstractModScreen<ScrenchMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation TANK_AND_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tank_and_slot.png");
    private static final int TANK_AND_SLOT_WIDTH = 18;
    private static final int TANK_AND_SLOT_HEIGHT = 66;

    @Nullable
    private FluidTankRenderer fuelRenderer;

    public ScrenchScreen(ScrenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        if (isSunder()) {
            fuelRenderer = createFluidRenderer16x40(menu.getSunderFuelCapacity());
        }
    }

    private boolean isSunder() {
        return menu.getGadgetStack().getItem() instanceof SunderItem;
    }

    private boolean isEater() {
        return menu.getGadgetStack().getItem() instanceof EaterItem;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isSunder() && fuelRenderer != null) {
            renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, menu.getSunderFluid(),
                    ScrenchMenu.fuelTankX() + 1, ScrenchMenu.fuelTankY() + 1, fuelRenderer,
                    Component.translatable("tooltip.dermicraft.gauge.fuel"));

            renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                    ScrenchMenu.chainSlotX() + 1, ScrenchMenu.chainSlotY() + 1,
                    menu.getSlot(0).getItem(), Component.translatable("tooltip.dermicraft.slot.chain"));
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

        if (isSunder()) {
            renderSunderBg(guiGraphics, x, y);
        } else if (isEater()) {
            renderEaterBg(guiGraphics, x, y);
        }
    }

    private void renderSunderBg(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x + ScrenchMenu.chainSlotX(), y + ScrenchMenu.chainSlotY(), 0, 0,
                SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + ScrenchMenu.fuelTankX(), y + ScrenchMenu.fuelTankY(), 0, 0,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);

        if (fuelRenderer != null) {
            fuelRenderer.render(guiGraphics, x + ScrenchMenu.fuelTankX() + 1, y + ScrenchMenu.fuelTankY() + 1, menu.getSunderFluid());
        }
    }

    /** Module row (yellow) + buffer row (plain, directly above the player's own inventory grid,
     * divider between them), at exactly the coordinates {@code EaterItem.EaterSwapPanel} builds its
     * slots against -- see those constants' own javadoc for why they're public. */
    private void renderEaterBg(GuiGraphics guiGraphics, int x, int y) {
        renderSlotBackgrounds(guiGraphics, MODULE_SLOT_TEXTURE, x, y,
                EaterItem.MODULE_SLOT_X, EaterItem.MODULE_SLOT_Y,
                EaterItem.MODULE_SLOT_COUNT, EaterItem.MODULE_SLOT_SPACING, 0);

        renderSlotBackgrounds(guiGraphics, ITEM_SLOT_TEXTURE, x, y,
                EaterItem.BUFFER_SLOT_X, EaterItem.BUFFER_SLOT_Y,
                EaterItem.SLOT_COUNT, EaterItem.BUFFER_SLOT_SPACING, 0);

        // Separates the buffer row from the player's own inventory grid just below it -- spans the
        // same width as that grid (PLAYER_INVENTORY_X/WIDTH) so it reads as one continuous rule.
        renderDivider(guiGraphics, x, y, 7, EaterItem.BUFFER_SLOT_Y + SLOT_SIZE + 2, 162);
    }
}
