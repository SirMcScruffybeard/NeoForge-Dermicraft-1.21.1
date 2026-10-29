package net.scruffy.dermicraft.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.component.LevelModuleData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.interfaces.IGrantTempHazardSafety;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.module.HazardModuleVariant;
import net.scruffy.dermicraft.item.custom.module.ModuleFamilies;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The row of icons on a machine's Module tab that shows what the machine has become: the current tier (or Overgrowth
 * level, with a counter) under the Evolution slot, then one icon per hazard family the machine tolerates under the
 * Module slots. The icons are the module items' own models, so they match the items a player knows.
 *
 * <p>A hazard's icon shows its strongest level from any source. A tolerance earned by evolving is shown with the
 * hazard Evolution Module's model (permanent); one that only comes from an installed Safety Module uses the
 * Safety Module's model (temporary); a tie goes to the permanent one. Positions are relative to the panel's
 * top-left, so every machine screen draws it with one call.
 */
public final class EvolutionPanel {

    private EvolutionPanel() {}

    public static final int ICON_SIZE = 16;
    /** Distance between hazard icons: a little tighter than the Module slots, so the row does not read as one icon per slot. */
    public static final int ICON_STEP = 18;
    public static final int ROW_Y = 58;
    /** Under the Evolution slot (menu x 8, the slot is 18 wide and its item sits 1px in). */
    public static final int TIER_X = 9;
    /** Under the first Module slot (menu x 79). */
    public static final int HAZARD_X = 80;
    /** The "Tolerances:" label and the "None" beside it are drawn smaller than normal text so the label fits between
     * the tier icon and the first hazard icon. */
    private static final float TEXT_SCALE = 0.75F;
    private static final int LABEL_COLOR = 0x007F0E;
    private static final int NONE_COLOR = 0x808080;
    /** Gap between the label and the first hazard icon, and between the label and the tier icon at the least. */
    private static final int LABEL_GAP = 3;

    /** One icon: where it goes, what it draws, a counter over it if any, and its hover text. */
    public record Cell(int x, int y, ItemStack stack, @Nullable String count, Component tooltip) {}

    /**
     * The cells for a machine: the tier (or Overgrowth) cell, then one per tolerated hazard family in a fixed order.
     *
     * @param modules the machine's general Module slots, read for temporary Safety Module tolerance
     */
    public static List<Cell> getCells(EvolutionStateData state, IItemHandler modules) {
        List<Cell> cells = new ArrayList<>();

        if (state.overgrowth() > 0) {
            ItemStack stack = new ItemStack(ModuleFamilies.OVERGROWTH.getItem());
            stack.set(ModDataComponentTypes.LEVEL_MODULE_DATA, new LevelModuleData(state.overgrowth()));
            cells.add(new Cell(TIER_X, ROW_Y, stack, String.valueOf(state.overgrowth()),
                    Component.translatable("tooltip.dermicraft.evolution_panel.overgrowth", state.overgrowth())));
        } else {
            ItemStack stack = new ItemStack(ModuleFamilies.TIER.getItem());
            stack.set(ModDataComponentTypes.LEVEL_MODULE_DATA, new LevelModuleData(state.tier()));
            cells.add(new Cell(TIER_X, ROW_Y, stack, null,
                    Component.translatable("tooltip.dermicraft.evolution_panel.tier", state.tier())));
        }

        int column = 0;
        for (HazardFamily family : HazardFamily.values()) {
            int permanent = state.level(family);
            int temporary = getTemporaryLevel(modules, family);
            int level = Math.max(permanent, temporary);
            if (level == 0) continue;

            HazardModuleVariant variant = HazardModuleVariant.of(family, level);
            if (variant == null) continue;

            boolean isPermanent = permanent >= temporary;
            ItemStack stack = new ItemStack(isPermanent
                    ? ModItems.getHazardEvolutionModule(variant) : ModItems.getHazardSafetyModule(variant));
            Component tooltip = Component.translatable(
                    isPermanent ? "tooltip.dermicraft.evolution_panel.permanent" : "tooltip.dermicraft.evolution_panel.temporary",
                    Component.translatable(variant.getTranslationKey()));
            cells.add(new Cell(HAZARD_X + column * ICON_STEP, ROW_Y, stack, null, tooltip));
            column++;
        }
        return cells;
    }

    /** The strongest level of {@code family} granted by a Safety Module in {@code modules}, or 0. */
    private static int getTemporaryLevel(IItemHandler modules, HazardFamily family) {
        int strongest = 0;
        for (int slot = 0; slot < modules.getSlots(); slot++) {
            ItemStack stack = modules.getStackInSlot(slot);
            if (!(stack.getItem() instanceof IGrantTempHazardSafety safety)) continue;
            HazardModuleData data = safety.getHazardData(stack);
            if (data != null && data.family() == family) strongest = Math.max(strongest, data.level());
        }
        return strongest;
    }

    /**
     * Draws the cells at {@code originX/originY}, and the "Tolerances:" label just before the hazard icons. When the
     * only cell is the tier cell (no hazard is tolerated), "None" is drawn after the label instead.
     */
    public static void render(GuiGraphics guiGraphics, Font font, int originX, int originY, List<Cell> cells) {
        Component label = Component.translatable("screen.dermicraft.evolution_panel.tolerances");
        int labelWidth = Math.round(font.width(label) * TEXT_SCALE);
        int labelX = Math.max(TIER_X + ICON_SIZE + LABEL_GAP, HAZARD_X - LABEL_GAP - labelWidth);
        drawScaledText(guiGraphics, font, label, originX + labelX, originY + ROW_Y, LABEL_COLOR);

        if (cells.size() <= 1) {
            drawScaledText(guiGraphics, font, Component.translatable("screen.dermicraft.evolution_panel.none"),
                    originX + HAZARD_X, originY + ROW_Y, NONE_COLOR);
        }

        for (Cell cell : cells) {
            guiGraphics.renderItem(cell.stack(), originX + cell.x(), originY + cell.y());
            if (cell.count() != null) {
                guiGraphics.renderItemDecorations(font, cell.stack(), originX + cell.x(), originY + cell.y(), cell.count());
            }
        }
    }

    /** Text at {@link #TEXT_SCALE}, vertically centred on an icon-height row whose top is {@code rowY}. */
    private static void drawScaledText(GuiGraphics guiGraphics, Font font, Component text, int x, int rowY, int color) {
        float textHeight = font.lineHeight * TEXT_SCALE;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, rowY + (ICON_SIZE - textHeight) / 2F, 0);
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    /** Draws the hover text of the cell under the mouse (panel-relative coordinates), if any. */
    public static void renderTooltip(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY, List<Cell> cells) {
        for (Cell cell : cells) {
            if (mouseX >= cell.x() && mouseX < cell.x() + ICON_SIZE && mouseY >= cell.y() && mouseY < cell.y() + ICON_SIZE) {
                guiGraphics.renderTooltip(font, cell.tooltip(), mouseX, mouseY);
                return;
            }
        }
    }
}
