package net.scruffy.dermicraft.screen.custom.aid;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scruffy.dermicraft.item.custom.AidItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModScreen;

/**
 * A.I.D.'s GUI -- a filtered string slot (Suture ammo) and a tank+fill/drain-slot pairing for the
 * Syringe tank, same {@code tank_and_slot} layout Drinker/Sipping/Sunder/Shatter/Eater already use.
 * Reuses the mod's existing generic panel/gauge/slot textures rather than new art -- see the design
 * notes' still-open "new gauge textures" item; this is a functional-first pass.
 */
public class AidScreen extends AbstractModScreen<AidMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation TANK_AND_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tank_and_slot.png");
    private static final int TANK_AND_SLOT_WIDTH = 18;
    private static final int TANK_AND_SLOT_HEIGHT = 66;

    // The tank art's own top -- 48px above its bottom-anchored fill/drain slot, same relationship
    // Sunder's FUEL_TANK_Y/FUEL_SLOT_Y use. Derived from AidMenu's slot position (not duplicated as
    // its own independent constant) so the two can never drift apart.
    public static final int TANK_X = AidMenu.TANK_FILL_SLOT_X;
    public static final int TANK_Y = AidMenu.TANK_FILL_SLOT_Y - 48;

    private FluidTankRenderer fluidRenderer;

    public AidScreen(AidMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // 16x40, not 16x64 -- the tank_and_slot texture reserves its bottom 26px for the fill/drain
        // slot, same sizing every other tank_and_slot user (Drinker/Sipping/Sunder/Shatter/Eater)
        // pairs it with. The old 16x64 renderer was sized for the plain tall_tank_guage art this
        // screen used before the fill slot was added, and overdrew into the slot's own art.
        fluidRenderer = createFluidRenderer16x40(AidItem.FLUID_CAPACITY);
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

        guiGraphics.blit(ITEM_SLOT_TEXTURE, x + AidMenu.STRING_SLOT_X - 1, y + AidMenu.STRING_SLOT_Y - 1, 0, 0,
                SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        guiGraphics.blit(TANK_AND_SLOT_TEXTURE, x + TANK_X, y + TANK_Y, 0, 0,
                TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT, TANK_AND_SLOT_WIDTH, TANK_AND_SLOT_HEIGHT);

        if (fluidRenderer != null) {
            fluidRenderer.render(guiGraphics, x + TANK_X + 1, y + TANK_Y + 1, menu.getFluid());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderItemSlotTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, AidMenu.STRING_SLOT_X, AidMenu.STRING_SLOT_Y,
                menu.getSlot(AidMenu.STRING_SLOT_INDEX).getItem(),
                Component.translatable("tooltip.dermicraft.aid.string_ingredient"));

        if (fluidRenderer != null) {
            renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, menu.getFluid(),
                    TANK_X + 1, TANK_Y + 1, fluidRenderer,
                    Component.translatable("tooltip.dermicraft.aid.mode.syringe"));
        }
    }
}
