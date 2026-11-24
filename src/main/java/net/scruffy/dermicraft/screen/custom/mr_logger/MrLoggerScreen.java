package net.scruffy.dermicraft.screen.custom.mr_logger;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.screen.AbstractFarmMenu;
import net.scruffy.dermicraft.screen.AbstractFarmScreen;
import net.scruffy.dermicraft.util.MouseUtil;

/**
 * Mr. Logger's screen: the shared farm screen's tabs, with the leaf toggle on Home. The toggle is a button showing a leaf block;
 * a "not allowed" overlay crosses it out while leaves are left alone.
 */
public class MrLoggerScreen extends AbstractFarmScreen<MrLoggerMenu> {

    private static final ResourceLocation BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/buttons/blank_button.png");
    private static final ResourceLocation OFF_OVERLAY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/buttons/no_use_overlay.png");
    private static final ItemStack LEAF_ICON = new ItemStack(Items.OAK_LEAVES);

    public MrLoggerScreen(MrLoggerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected Component getInputSlotTooltip() {
        return Component.translatable("tooltip.dermicraft.slot.sapling");
    }

    @Override
    protected void renderHomeExtras(GuiGraphics guiGraphics, int x, int y) {
        int buttonX = x + MrLoggerMenu.LEAF_TOGGLE_X;
        int buttonY = y + MrLoggerMenu.LEAF_TOGGLE_Y;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(BUTTON_TEXTURE, buttonX, buttonY, 0, 0, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE);
        guiGraphics.renderItem(LEAF_ICON, buttonX + 1, buttonY + 1);
        if (!menu.isBreakingLeaves()) {
            guiGraphics.blit(OFF_OVERLAY_TEXTURE, buttonX, buttonY, 0, 0, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE);
        }
    }

    @Override
    protected void renderHomeLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        if (MouseUtil.isMouseOver(mouseX, mouseY, x + MrLoggerMenu.LEAF_TOGGLE_X, y + MrLoggerMenu.LEAF_TOGGLE_Y,
                STEPPER_SIZE, STEPPER_SIZE)) {
            guiGraphics.renderTooltip(this.font, Component.translatable(menu.isBreakingLeaves()
                    ? "tooltip.dermicraft.mr_logger.leaves_on" : "tooltip.dermicraft.mr_logger.leaves_off"), mouseX - x, mouseY - y);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        if (menu.getActiveTab() == AbstractFarmMenu.HOME_TAB
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + MrLoggerMenu.LEAF_TOGGLE_X, y + MrLoggerMenu.LEAF_TOGGLE_Y,
                STEPPER_SIZE, STEPPER_SIZE)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MrLoggerMenu.BUTTON_LEAVES);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
