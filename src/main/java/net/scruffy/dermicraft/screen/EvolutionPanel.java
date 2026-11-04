package net.scruffy.dermicraft.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.component.LevelModuleData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.interfaces.IHasHazardModuleData;
import net.scruffy.dermicraft.interfaces.IGrantTempHazardSafety;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.module.EvolutionModuleItem;
import net.scruffy.dermicraft.item.custom.module.HazardModuleVariant;
import net.scruffy.dermicraft.item.custom.module.ModuleFamilies;
import net.scruffy.dermicraft.machine.EvolutionResult;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    /** Where the hazard icons start when there is room: under the first Module slot (menu x 79, item 1px in). */
    public static final int HAZARD_ANCHOR_X = 80;
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
     * Where the parts of a machine's panel go, in panel coordinates. The Evolution slot is at {@code slotX}, the tier
     * icon under it, then the "Tolerances:" label and the hazard icons. On a machine with room, the hazard icons start
     * under the first Module slot with the label right before them; where the slot has to sit further right (the
     * Masticator's health bar takes the left edge) everything after it shifts right so nothing overlaps.
     */
    public record Layout(int slotX, int tierX, int labelX, int hazardX) {

        /** The layout for an Evolution slot at {@code slotX}; needs the font to measure the label. */
        public static Layout of(Font font, int slotX) {
            int tierX = slotX + 1;
            int labelWidth = Math.round(font.width(Component.translatable("screen.dermicraft.evolution_panel.tolerances")) * TEXT_SCALE);
            int labelX = Math.max(tierX + ICON_SIZE + LABEL_GAP, HAZARD_ANCHOR_X - LABEL_GAP - labelWidth);
            int hazardX = Math.max(HAZARD_ANCHOR_X, labelX + labelWidth + LABEL_GAP);
            return new Layout(slotX, tierX, labelX, hazardX);
        }
    }

    /**
     * The cells for a machine laid out as {@code layout}, and a hazard Evolution Module in {@code evolution}
     * that is evolving the machine right now counts as a temporary tolerance too (it becomes permanent when the
     * evolution completes).
     *
     * @param modules   the machine's general Module slots, read for temporary Safety Module tolerance
     * @param evolution the machine's Evolution slot, or null for a machine without one
     */
    public static List<Cell> getCells(Layout layout, EvolutionStateData state, IItemHandler modules, @Nullable IItemHandler evolution) {
        List<Cell> cells = new ArrayList<>();

        if (state.overgrowth() > 0) {
            ItemStack stack = new ItemStack(ModuleFamilies.OVERGROWTH.getItem());
            stack.set(ModDataComponentTypes.LEVEL_MODULE_DATA, new LevelModuleData(state.overgrowth()));
            cells.add(new Cell(layout.tierX(), ROW_Y, stack, String.valueOf(state.overgrowth()),
                    Component.translatable("tooltip.dermicraft.evolution_panel.overgrowth", state.overgrowth())));
        } else {
            ItemStack stack = new ItemStack(ModuleFamilies.TIER.getItem());
            stack.set(ModDataComponentTypes.LEVEL_MODULE_DATA, new LevelModuleData(state.tier()));
            cells.add(new Cell(layout.tierX(), ROW_Y, stack, null,
                    Component.translatable("tooltip.dermicraft.evolution_panel.tier", state.tier())));
        }

        int column = 0;
        for (HazardFamily family : HazardFamily.values()) {
            int permanent = state.level(family);
            int temporary = Math.max(getTemporaryLevel(modules, family), getEvolvingLevel(state, evolution, family));
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
            cells.add(new Cell(layout.hazardX() + column * ICON_STEP, ROW_Y, stack, null, tooltip));
            column++;
        }
        return cells;
    }

    /** The level of {@code family} the hazard Evolution Module in {@code evolution} would grant, if its upgrade is live
     * on this machine (a blocked module grants nothing), or 0. */
    private static int getEvolvingLevel(EvolutionStateData state, @Nullable IItemHandler evolution, HazardFamily family) {
        if (evolution == null || evolution.getSlots() == 0) return 0;
        ItemStack stack = evolution.getStackInSlot(0);
        if (!(stack.getItem() instanceof EvolutionModuleItem module) || !(stack.getItem() instanceof IHasHazardModuleData hazard)) {
            return 0;
        }
        if (!(module.evaluate(null, state, stack).orElse(null) instanceof EvolutionResult.Upgrade)) return 0;
        HazardModuleData data = hazard.getHazardData(stack);
        return data != null && data.family() == family ? data.level() : 0;
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
    public static void render(GuiGraphics guiGraphics, Font font, int originX, int originY, Layout layout, List<Cell> cells) {
        drawScaledText(guiGraphics, font, Component.translatable("screen.dermicraft.evolution_panel.tolerances"),
                originX + layout.labelX(), originY + ROW_Y, LABEL_COLOR);

        if (cells.size() <= 1) {
            drawScaledText(guiGraphics, font, Component.translatable("screen.dermicraft.evolution_panel.none"),
                    originX + layout.hazardX(), originY + ROW_Y, NONE_COLOR);
        }

        for (Cell cell : cells) {
            guiGraphics.renderItem(cell.stack(), originX + cell.x(), originY + cell.y());
            if (cell.count() != null) {
                guiGraphics.renderItemDecorations(font, cell.stack(), originX + cell.x(), originY + cell.y(), cell.count());
            }
        }
    }

    // ---- The Evolution slot's own background, progress bar and tooltips (shared by every machine screen) ------------

    /** Row of the Evolution slot and the Module slots on a Module tab, in menu coordinates. */
    public static final int SLOT_Y = 34;
    private static final int SLOT_SIZE = 18;
    private static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/slots/evolution_slot.png");
    private static final int BAR_WIDTH = 2;
    private static final int BAR_GAP = 2;
    private static final int BAR_TRACK_COLOR = 0xFF2B2B2B;
    private static final int BAR_FILL_COLOR = 0xFF3CD13C;

    /** Draws the Evolution slot's background, and to its right a thin bar filling from the bottom up while an evolution
     * runs ({@code progress} 0 to 1; nothing is drawn at 0, so an idle slot looks plain). */
    public static void renderSlot(GuiGraphics guiGraphics, int originX, int originY, Layout layout, float progress) {
        guiGraphics.blit(SLOT_TEXTURE, originX + layout.slotX(), originY + SLOT_Y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        if (progress <= 0f) return;

        int left = originX + layout.slotX() + SLOT_SIZE + BAR_GAP;
        int top = originY + SLOT_Y;
        int bottom = top + SLOT_SIZE;
        guiGraphics.fill(left, top, left + BAR_WIDTH, bottom, BAR_TRACK_COLOR);
        int filled = Math.round(SLOT_SIZE * progress);
        guiGraphics.fill(left, bottom - filled, left + BAR_WIDTH, bottom, BAR_FILL_COLOR);
    }

    /** Whether {@code slot} is the machine's Evolution slot. */
    public static boolean isEvolutionSlot(@Nullable Slot slot, IItemHandler evolution) {
        return slot instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() == evolution;
    }

    /** The tooltip of an empty Evolution slot. */
    public static List<Component> getEmptySlotTooltip() {
        return List.of(
                Component.translatable("tooltip.dermicraft.evolution_slot.empty"),
                Component.translatable("tooltip.dermicraft.evolution_slot.empty_hint").withStyle(ChatFormatting.GRAY));
    }

    /** The status line(s) added under a held module's tooltip in the Evolution slot: inert, applying, or why it is blocked. */
    public static List<Component> getStatusLines(Optional<EvolutionResult> status) {
        if (status.isEmpty()) {
            return List.of(Component.translatable("tooltip.dermicraft.evolution_slot.inert").withStyle(ChatFormatting.GRAY));
        }
        return switch (status.get()) {
            case EvolutionResult.Upgrade upgrade ->
                    List.of(Component.translatable("tooltip.dermicraft.evolution_slot.applying").withStyle(ChatFormatting.GREEN));
            case EvolutionResult.Blocked blocked ->
                    List.of(Component.translatable("tooltip.dermicraft.evolution_slot.blocked." + blocked.reasonKey())
                            .withStyle(ChatFormatting.RED));
        };
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
