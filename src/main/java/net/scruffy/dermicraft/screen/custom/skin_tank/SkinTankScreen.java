package net.scruffy.dermicraft.screen.custom.skin_tank;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.FluidTankRenderer;

public class SkinTankScreen extends AbstractContainerScreen<SkinTankMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID,"textures/gui/tank_gui.png");
    private FluidTankRenderer fluidRenderer;

    public SkinTankScreen(SkinTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {

    }
}
