package net.scruffy.dermicraft.screen.custom.masticator;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.NotNull;

public class MasticatorScreen extends AbstractModScreen<MasticatorMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String ARROWS_DIR = "textures/gui/arrows/";
    private static final String HEALTH_DIR = "textures/gui/health/";

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

    // Solid-ingredient slot backdrop between the HP bar and the reagent gauge -- backs
    // MasticatorBlockEntity.INGREDIENT_ITEM_SLOT (added at MasticatorMenu (38,35), 1px inset from
    // this (37,34) backdrop, matching the rest of this screen's slot-backdrop convention).
    // Vertically centered on the arrow (arrow spans y+38 to y+47, center 43; an 18-tall slot at
    // y+34 centers at 43 too).
    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

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

    private FluidTankRenderer fuelRenderer;
    private FluidTankRenderer ingredientRenderer;
    private FluidTankRenderer resultRenderer;

    private static final int HEALTH_BAR_X = 8; // evenly spaced with fuel/ingredient/arrow/result
    private static final int HEALTH_BAR_Y = 11; // top of the fluid tank renderers


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
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), 151, 12, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getIngredientTank().SLOT), 67, 12, ingredientRenderer,
                Component.translatable("tooltip.dermicraft.gauge.reagent"));

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getResultTank().SLOT), 123, 12, resultRenderer,
                Component.translatable("tooltip.dermicraft.gauge.result"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, 151, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, 67, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getIngredientTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.reagent_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, 123, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getResultTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.result_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, 38, 35,
                menu.BE.getItemHandler(null).getStackInSlot(MasticatorBlockEntity.INGREDIENT_ITEM_SLOT),
                Component.translatable("tooltip.dermicraft.slot.ingredient"));

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
        renderPlayerInventoryBackdrop(guiGraphics, x, y);

        renderTankAndSlot(guiGraphics, x + 150, y + 11);
        renderTankAndSlot(guiGraphics, x + 66, y + 11);
        renderTankAndSlot(guiGraphics, x + 122, y + 11);
        renderItemSlot(guiGraphics, x + 37, y + 34);

        renderProgressArrow(guiGraphics, x, y);
        renderHealthBar(guiGraphics, x, y);

        fuelRenderer.render(guiGraphics, x + 151, y + 12, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
        ingredientRenderer.render(guiGraphics, x + 67, y + 12, menu.BE.getFluid(menu.BE.getIngredientTank().SLOT));
        resultRenderer.render(guiGraphics, x + 123, y + 12, menu.BE.getFluid(menu.BE.getResultTank().SLOT));
    }

    private void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    private void renderTankAndSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x, y, 0, 0, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        // Textures carry a 1px transparent margin around a 15x8 opaque arrow, so the draw
        // origin is offset by 1px to keep the arrow aligned with its old (111,39) position.
        int arrowX = x + 95;
        int arrowY = y + 38;

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
