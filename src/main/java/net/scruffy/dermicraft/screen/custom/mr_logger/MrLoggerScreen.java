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
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/buttons/no_use_red_overlay.png");
    private static final ItemStack SHEARS_ICON = new ItemStack(Items.SHEARS);
    private static final ItemStack LEAF_ICON = new ItemStack(Items.OAK_LEAVES);
    private static final ItemStack GROWTH_ICON = new ItemStack(Items.BONE_MEAL);

    public MrLoggerScreen(MrLoggerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected Component getInputSlotTooltip() {
        return Component.translatable("tooltip.dermicraft.slot.sapling");
    }

    @Override
    protected void renderHomeExtras(GuiGraphics guiGraphics, int x, int y) {
        renderToggle(guiGraphics, x + MrLoggerMenu.LEAF_TOGGLE_X, y + MrLoggerMenu.LEAF_TOGGLE_Y, menu.isHarvestingLeaves() ? SHEARS_ICON : LEAF_ICON, true);
        renderToggle(guiGraphics, x + MrLoggerMenu.GROWTH_TOGGLE_X, y + MrLoggerMenu.GROWTH_TOGGLE_Y, GROWTH_ICON,
                menu.isAcceleratingGrowth());
    }

    /** A toggle button: the icon on a blank button, crossed out in red while the setting is off. */
    private void renderToggle(GuiGraphics guiGraphics, int buttonX, int buttonY, ItemStack icon, boolean on) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(BUTTON_TEXTURE, buttonX, buttonY, 0, 0, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE);
        guiGraphics.renderItem(icon, buttonX + 1, buttonY + 1);
        if (!on) {
            guiGraphics.blit(OFF_OVERLAY_TEXTURE, buttonX, buttonY, 0, 0, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE, STEPPER_SIZE);
        }
    }

    @Override
    protected void renderHomeLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        if (MouseUtil.isMouseOver(mouseX, mouseY, x + MrLoggerMenu.LEAF_TOGGLE_X, y + MrLoggerMenu.LEAF_TOGGLE_Y,
                STEPPER_SIZE, STEPPER_SIZE)) {
            guiGraphics.renderTooltip(this.font, Component.translatable(menu.isHarvestingLeaves()
                    ? "tooltip.dermicraft.mr_logger.leaves_on" : "tooltip.dermicraft.mr_logger.leaves_off"), mouseX - x, mouseY - y);
        }
        if (MouseUtil.isMouseOver(mouseX, mouseY, x + MrLoggerMenu.GROWTH_TOGGLE_X, y + MrLoggerMenu.GROWTH_TOGGLE_Y,
                STEPPER_SIZE, STEPPER_SIZE)) {
            guiGraphics.renderTooltip(this.font, Component.translatable(menu.isAcceleratingGrowth()
                    ? "tooltip.dermicraft.mr_logger.growth_on" : "tooltip.dermicraft.mr_logger.growth_off"), mouseX - x, mouseY - y);
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
        if (menu.getActiveTab() == AbstractFarmMenu.HOME_TAB
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + MrLoggerMenu.GROWTH_TOGGLE_X, y + MrLoggerMenu.GROWTH_TOGGLE_Y,
                STEPPER_SIZE, STEPPER_SIZE)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MrLoggerMenu.BUTTON_GROWTH);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
