package net.scruffy.dermicraft.screen.custom.grafting_table;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.block.entity.custom.GraftingTableBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.AutoDrainToggleClickPayload;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.NotNull;

/** Same fuel-far-right convention as every other machine's screen. The 3x3 grid is the one genuinely
 * new layout element in the mod -- nine item_slot tiles instead of the usual 1-2 slots. No HP bar
 * (MachineTier.NO_HEALTH, same as the Render Furnace). */
public class GraftingTableScreen extends AbstractModScreen<GraftingTableMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String ARROWS_DIR = "textures/gui/arrows/";
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

    private static final ResourceLocation GHOST_WASH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "green_wash_overlay.png");
    private static final int GHOST_WASH_SIZE = 16;

    private static final ResourceLocation ARROW_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_background.png");
    private static final ResourceLocation ARROW_FULL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_fulll.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    // Grid origin/step MUST match GraftingTableMenu's GRID_X/GRID_Y/GRID_STEP -- this class only
    // draws the slot art, the menu places the real slots.
    private static final int GRID_X = 8;
    private static final int GRID_Y = 11;
    private static final int GRID_STEP = 18;

    private static final int ARROW_X = 70;
    private static final int ARROW_Y = 32;
    private static final int OUTPUT_X = 100;
    private static final int OUTPUT_Y = 29;
    private static final int FUEL_X = 150;
    private static final int TANK_Y = 11;

    // Auto-dispense toggle -- see RenderKilnScreen's own identical constants/comment. The 3x3 grid
    // ends at x=62, so the column above the arrow (x=70) is clear.
    private static final ResourceLocation AUTO_DRAIN_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "output_button.png");
    private static final ResourceLocation AUTO_DRAIN_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "no_use_button.png");
    private static final int AUTO_DRAIN_BUTTON_SIZE = 18;
    private static final int AUTO_DRAIN_BUTTON_X = ARROW_X;
    private static final int AUTO_DRAIN_BUTTON_Y = TANK_Y;

    private FluidTankRenderer fuelRenderer;

    public GraftingTableScreen(GraftingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        fuelRenderer = createFluidRenderer16x40(menu.BE.getFuelTank().getCapacity());
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, FUEL_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, OUTPUT_X + 1, OUTPUT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(GraftingTableBlockEntity.OUTPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.result"));

        if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y,
                AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
            Component label = menu.isAutoDrainEnabled()
                    ? Component.translatable("tooltip.dermicraft.machine.auto_dispense_on")
                    : Component.translatable("tooltip.dermicraft.machine.auto_dispense_off");
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

        for (int row = 0; row < GraftingTableBlockEntity.GRID_HEIGHT; row++) {
            for (int col = 0; col < GraftingTableBlockEntity.GRID_WIDTH; col++) {
                int gridIndex = row * GraftingTableBlockEntity.GRID_WIDTH + col;
                int slotX = x + GRID_X + col * GRID_STEP;
                int slotY = y + GRID_Y + row * GRID_STEP;
                renderItemSlot(guiGraphics, slotX, slotY);

                // Ghost only shows while there's no real stock -- vanilla's own slot-content pass
                // (which runs right after renderBg) draws the real item on top once stock arrives,
                // so nothing extra is needed for that case.
                if (menu.BE.getRealStock(gridIndex).isEmpty()) {
                    ItemStack ghost = menu.BE.getGhost(gridIndex);
                    if (!ghost.isEmpty()) renderGhostItem(guiGraphics, slotX + 1, slotY + 1, ghost);
                }
            }
        }

        renderProgressArrow(guiGraphics, x + ARROW_X, y + ARROW_Y);
        renderItemSlot(guiGraphics, x + OUTPUT_X, y + OUTPUT_Y);

        // Result ghost preview -- only while the real output slot is empty (a real result sitting
        // there always takes visual priority, drawn by vanilla's own slot-content pass afterward).
        if (menu.BE.getItemHandler(null).getStackInSlot(GraftingTableBlockEntity.OUTPUT_SLOT).isEmpty()) {
            ItemStack ghostResult = menu.BE.getGhostResult();
            if (!ghostResult.isEmpty()) renderGhostItem(guiGraphics, x + OUTPUT_X + 1, y + OUTPUT_Y + 1, ghostResult);
        }

        renderTankAndSlot(guiGraphics, x + FUEL_X, y + TANK_Y);

        fuelRenderer.render(guiGraphics, x + FUEL_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));

        renderAutoDrainButton(guiGraphics, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y,
                AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new AutoDrainToggleClickPayload(menu.BE.getBlockPos()));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderAutoDrainButton(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isAutoDrainEnabled()) {
            blitRotated90(guiGraphics, AUTO_DRAIN_ON_TEXTURE, x, y, AUTO_DRAIN_BUTTON_SIZE);
        } else {
            guiGraphics.blit(AUTO_DRAIN_OFF_TEXTURE, x, y, 0, 0, AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE,
                    AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE);
        }
    }

    /** Washed-out icon, no stack-count decoration -- ghosts aren't real items. renderItem() leaves
     * depth testing enabled (needed for its own 3D-ish item model rendering) and writes real depth
     * values at the item's pixels -- any later draw at those same pixels gets occluded by that
     * depth data regardless of render type (fill() and blit() both hit this identically). Forcing
     * depth test off for the overlay draw makes it ignore that leftover depth entirely. */
    private void renderGhostItem(GuiGraphics guiGraphics, int x, int y, ItemStack stack) {
        guiGraphics.renderItem(stack, x, y);
        guiGraphics.flush();

        RenderSystem.disableDepthTest();
        // Blending isn't guaranteed on just because the texture has an alpha channel -- without
        // explicitly enabling it (and setting the standard alpha blend function), the wash's
        // translucent pixels get written as fully opaque instead of blending with what's beneath.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(GHOST_WASH_TEXTURE, x, y, 0, 0, GHOST_WASH_SIZE, GHOST_WASH_SIZE,
                GHOST_WASH_SIZE, GHOST_WASH_SIZE);
        guiGraphics.flush();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private void renderTankAndSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x, y, 0, 0, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);
    }

    private void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);

        if (menu.isCrafting()) {
            int progressWidth = 1 + menu.getScaledArrowProgress();
            guiGraphics.blit(ARROW_FULL_TEXTURE, arrowX, arrowY, 0, 0, progressWidth, ARROW_HEIGHT,
                    ARROW_WIDTH, ARROW_HEIGHT);
        }
    }
}
