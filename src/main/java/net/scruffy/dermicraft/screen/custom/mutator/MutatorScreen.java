package net.scruffy.dermicraft.screen.custom.mutator;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.block.entity.custom.MutatorBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.MutatorModeClickPayload;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.NotNull;

public class MutatorScreen extends AbstractModScreen<MutatorMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
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

    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

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
    private static final int HP_BAR_INTERIOR_HEIGHT = 64;

    private static final int HEALTH_BAR_X = 8;
    private static final int HEALTH_BAR_Y = 11;

    // Mode toggle -- reuses the Node's existing item/fluid button pair. MUTATE shows the item icon
    // (the machine is producing a different ITEM); FILL shows the fluid icon (the fluid becomes
    // cargo packaged into the container). A single button that flips the mode on click, not a
    // per-leg pair like the Node's -- no "off" state, it's always showing whichever mode is active.
    private static final ResourceLocation MODE_MUTATE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "item_button.png");
    private static final ResourceLocation MODE_FILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "fluid_button.png");
    private static final int MODE_BUTTON_SIZE = 18;

    private static final int REAGENT_X = 30;
    private static final int INPUT_X = 60;
    private static final int ARROW_X = 90;
    private static final int OUTPUT_X = 120;
    private static final int FUEL_X = 150;
    private static final int TANK_Y = 11;
    private static final int SLOT_Y = 34;
    private static final int ARROW_Y = 37;

    private static final int MODE_BUTTON_X = INPUT_X;
    private static final int MODE_BUTTON_Y = SLOT_Y - MODE_BUTTON_SIZE - 2; // directly above the input slot

    private FluidTankRenderer reagentRenderer;
    private FluidTankRenderer fuelRenderer;

    public MutatorScreen(MutatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        reagentRenderer = createFluidRenderer16x40(menu.BE.getReagentTank().getCapacity());
        fuelRenderer = createFluidRenderer16x40(menu.BE.getFuelTank().getCapacity());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_Y,
                MODE_BUTTON_SIZE, MODE_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new MutatorModeClickPayload(menu.BE.getBlockPos()));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getReagentTank().SLOT), REAGENT_X + 1, TANK_Y + 1, reagentRenderer,
                Component.translatable("tooltip.dermicraft.gauge.reagent"));

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, REAGENT_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getReagentTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.reagent_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, FUEL_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, INPUT_X + 1, SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(MutatorBlockEntity.INPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.ingredient"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, OUTPUT_X + 1, SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(MutatorBlockEntity.OUTPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.result"));

        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + HEALTH_BAR_X, y + HEALTH_BAR_Y, HP_BAR_WIDTH, HP_BAR_HEIGHT)) {
            int maxHealth = menu.getMaxHealth();
            int percent = maxHealth <= 0 ? 0 : Math.round(100f * menu.getHealth() / maxHealth);
            guiGraphics.renderTooltip(this.font,
                    Component.literal("HP: " + percent + "%"),
                    pMouseX - x, pMouseY - y);
        }

        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE)) {
            Component label = menu.getMode() == MutatorBlockEntity.Mode.FILL
                    ? Component.translatable("tooltip.dermicraft.mutator.mode_fill")
                    : Component.translatable("tooltip.dermicraft.mutator.mode_mutate");
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

        renderHealthBar(guiGraphics, x, y);

        renderTankAndSlot(guiGraphics, x + REAGENT_X, y + TANK_Y);
        renderItemSlot(guiGraphics, x + INPUT_X, y + SLOT_Y);
        renderProgressArrow(guiGraphics, x + ARROW_X, y + ARROW_Y);
        renderItemSlot(guiGraphics, x + OUTPUT_X, y + SLOT_Y);
        renderTankAndSlot(guiGraphics, x + FUEL_X, y + TANK_Y);
        renderModeButton(guiGraphics, x + MODE_BUTTON_X, y + MODE_BUTTON_Y);

        reagentRenderer.render(guiGraphics, x + REAGENT_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getReagentTank().SLOT));
        fuelRenderer.render(guiGraphics, x + FUEL_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
    }

    private void renderTankAndSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x, y, 0, 0, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
    }

    private void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    private void renderModeButton(GuiGraphics guiGraphics, int x, int y) {
        ResourceLocation texture = menu.getMode() == MutatorBlockEntity.Mode.FILL ? MODE_FILL_TEXTURE : MODE_MUTATE_TEXTURE;
        guiGraphics.blit(texture, x, y, 0, 0, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);

        // No progress arrow makes sense in FILL mode -- fill is a continuous per-cycle operation
        // with no single-shot completion (see MutatorBlockEntity#tickFill), so there's nothing to
        // scale the arrow against. Only render fill in MUTATE mode.
        if (menu.getMode() == MutatorBlockEntity.Mode.MUTATE && menu.isCrafting()) {
            int progressWidth = 1 + menu.getScaledArrowProgress();
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
