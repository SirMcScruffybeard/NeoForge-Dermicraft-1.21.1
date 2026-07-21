package net.scruffy.dermicraft.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Shared JEI drawables built from the same GUI art the real machine screens use (slots/tanks/
 * arrows under {@code textures/gui/}), so recipe category pages look like miniature versions of
 * the actual machine screens without needing any new artwork.
 */
public final class JeiTextures {

    private JeiTextures() {
    }

    private static final ResourceLocation ITEM_SLOT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/slots/item_slot.png");
    private static final ResourceLocation TANK_AND_SLOT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/tanks/tank_and_slot.png");
    private static final ResourceLocation TALL_TANK =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/tanks/tall_tank_guage.png");
    private static final ResourceLocation ARROW_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/arrows/arrow_background.png");
    private static final ResourceLocation ARROW_FULL =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/arrows/arrow_fulll.png");

    public static final int ITEM_SLOT_SIZE = 18;
    public static final int TANK_WIDTH = 18;
    public static final int TANK_HEIGHT = 66;
    public static final int ARROW_WIDTH = 17;
    public static final int ARROW_HEIGHT = 10;

    public static IDrawable itemSlot(IGuiHelper gui) {
        return gui.drawableBuilder(ITEM_SLOT, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)
                .setTextureSize(ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)
                .build();
    }

    public static IDrawable tankAndSlot(IGuiHelper gui) {
        return gui.drawableBuilder(TANK_AND_SLOT, 0, 0, TANK_WIDTH, TANK_HEIGHT)
                .setTextureSize(TANK_WIDTH, TANK_HEIGHT)
                .build();
    }

    public static IDrawable tallTank(IGuiHelper gui) {
        return gui.drawableBuilder(TALL_TANK, 0, 0, TANK_WIDTH, TANK_HEIGHT)
                .setTextureSize(TANK_WIDTH, TANK_HEIGHT)
                .build();
    }

    public static IDrawable arrowBackground(IGuiHelper gui) {
        return gui.drawableBuilder(ARROW_BACKGROUND, 0, 0, ARROW_WIDTH, ARROW_HEIGHT)
                .setTextureSize(ARROW_WIDTH, ARROW_HEIGHT)
                .build();
    }

    public static IDrawable arrowFull(IGuiHelper gui) {
        return gui.drawableBuilder(ARROW_FULL, 0, 0, ARROW_WIDTH, ARROW_HEIGHT)
                .setTextureSize(ARROW_WIDTH, ARROW_HEIGHT)
                .build();
    }
}
