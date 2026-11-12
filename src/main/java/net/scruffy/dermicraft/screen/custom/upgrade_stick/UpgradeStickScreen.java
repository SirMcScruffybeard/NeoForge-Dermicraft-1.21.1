package net.scruffy.dermicraft.screen.custom.upgrade_stick;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.component.LevelModuleData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.module.HazardModuleVariant;
import net.scruffy.dermicraft.item.custom.module.ModuleFamilies;
import net.scruffy.dermicraft.machine.EvolutionStats;
import net.scruffy.dermicraft.machine.UpgradeStickRules;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Upgrade Stick's selection screen: a row of level buttons (L1 to L4 and O), an Overgrowth stepper that shows when O
 * is selected, and a row of hazard buttons that cycle off, Mild, Severe. The buttons use the module items' own icons,
 * like the machines' Module tab. What is allowed comes from {@link UpgradeStickRules}; the server enforces it too.
 */
public class UpgradeStickScreen extends AbstractModScreen<UpgradeStickMenu> {

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/backgrounds/screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final int TEXT_COLOR = 0x007F0E; // the green the other screens use for their text
    private static final int BORDER = 0xFF373737;
    private static final int FILL = 0xFF8B8B8B;
    private static final int FILL_HOVER = 0xFFA5A5A5;
    private static final int FILL_SELECTED = 0xFFD7B34A;
    private static final int FILL_DISABLED = 0xFF5A5A5A;

    private static final int TIER_Y = 24;
    private static final int TIER_W = 30;
    private static final int TIER_H = 28;
    private static final int TIER_GAP = 2;
    private static final int TIER_X = 8;

    private static final int STEP_Y = 58;
    private static final int STEP_H = 14;

    private static final int HAZARD_Y = 84;
    private static final int HAZARD_W = 36;
    private static final int HAZARD_H = 34;
    private static final int HAZARD_GAP = 4;
    private static final int HAZARD_X = 10;

    /** One button: where, what it does, how it looks, and its hover text. */
    private record Button(int x, int y, int w, int h, int id, @Nullable ItemStack icon, Component label,
                          boolean selected, boolean enabled, List<Component> tooltip) {

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public UpgradeStickScreen(UpgradeStickMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private List<Button> buildButtons() {
        EvolutionStateData state = menu.getState();
        List<Button> buttons = new ArrayList<>();

        for (int tier = 1; tier <= EvolutionStateData.MAX_TIER; tier++) {
            boolean allowed = UpgradeStickRules.canSetTier(state, tier);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("tooltip.dermicraft.evolution_panel.tier", tier));
            if (!allowed) tooltip.add(Component.translatable("tooltip.dermicraft.upgrade_stick.too_many_tolerances"));
            buttons.add(new Button(TIER_X + (tier - 1) * (TIER_W + TIER_GAP), TIER_Y, TIER_W, TIER_H,
                    UpgradeStickMenu.BUTTON_TIER_BASE + tier - 1, getLevelStack(ModuleFamilies.TIER.getItem(), tier),
                    Component.literal("L" + tier), state.overgrowth() == 0 && state.tier() == tier, allowed, tooltip));
        }
        buttons.add(new Button(TIER_X + EvolutionStateData.MAX_TIER * (TIER_W + TIER_GAP), TIER_Y, TIER_W, TIER_H,
                UpgradeStickMenu.BUTTON_OVERGROWTH,
                getLevelStack(ModuleFamilies.OVERGROWTH.getItem(), Math.max(1, state.overgrowth())),
                Component.literal(state.overgrowth() > 0 ? "O" + state.overgrowth() : "O"),
                state.overgrowth() > 0, true,
                List.of(Component.translatable("screen.dermicraft.upgrade_stick.overgrowth"))));

        if (state.overgrowth() > 0) {
            int max = Math.max(1, EvolutionStats.getOvergrowthMaxLevel());
            buttons.add(stepButton(8, 24, UpgradeStickMenu.BUTTON_OVERGROWTH_MINUS_5, "-5", state.overgrowth() > 1));
            buttons.add(stepButton(34, 18, UpgradeStickMenu.BUTTON_OVERGROWTH_MINUS_1, "-", state.overgrowth() > 1));
            buttons.add(stepButton(124, 18, UpgradeStickMenu.BUTTON_OVERGROWTH_PLUS_1, "+", state.overgrowth() < max));
            buttons.add(stepButton(144, 24, UpgradeStickMenu.BUTTON_OVERGROWTH_PLUS_5, "+5", state.overgrowth() < max));
        }

        HazardFamily[] families = HazardFamily.values();
        for (HazardFamily family : families) {
            int level = state.level(family);
            boolean allowed = level > 0 || UpgradeStickRules.canAddFamily(state);
            HazardModuleVariant variant = HazardModuleVariant.of(family, Math.max(1, level));
            ItemStack icon = variant == null ? null : new ItemStack(ModItems.getHazardEvolutionModule(variant));

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("hazard_family.dermicraft." + family.getSerializedName()));
            tooltip.add(getLevelName(family, level));
            if (!allowed) tooltip.add(Component.translatable("tooltip.dermicraft.upgrade_stick.cap_reached"));

            buttons.add(new Button(HAZARD_X + family.ordinal() * (HAZARD_W + HAZARD_GAP), HAZARD_Y, HAZARD_W, HAZARD_H,
                    UpgradeStickMenu.BUTTON_HAZARD_BASE + family.ordinal(), icon, getLevelName(family, level),
                    level > 0, allowed, tooltip));
        }
        return buttons;
    }

    private static Button stepButton(int x, int w, int id, String text, boolean enabled) {
        return new Button(x, STEP_Y, w, STEP_H, id, null, Component.literal(text), false, enabled, List.of());
    }

    private static ItemStack getLevelStack(net.minecraft.world.item.Item item, int level) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponentTypes.LEVEL_MODULE_DATA, new LevelModuleData(level));
        return stack;
    }

    /** "Off", "On" (a one-level family) or "Mild" / "Severe". */
    private static Component getLevelName(HazardFamily family, int level) {
        if (level == 0) return Component.translatable("screen.dermicraft.upgrade_stick.off");
        if (family.getMaxLevel() == 1) return Component.translatable("screen.dermicraft.upgrade_stick.on");
        return Component.translatable(level == 1 ? "screen.dermicraft.upgrade_stick.mild" : "screen.dermicraft.upgrade_stick.severe");
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, imageWidth, imageHeight,
                BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);

        EvolutionStateData state = menu.getState();
        guiGraphics.drawString(font, Component.translatable("screen.dermicraft.upgrade_stick"), x + 8, y + 8, TEXT_COLOR, false);
        guiGraphics.drawString(font, Component.translatable("screen.dermicraft.upgrade_stick.level"), x + 8, y + 15, TEXT_COLOR, false);

        for (Button button : buildButtons()) {
            drawButton(guiGraphics, button, x, y, button.contains(mouseX - x, mouseY - y));
        }

        if (state.overgrowth() > 0) {
            Component text = Component.translatable("screen.dermicraft.upgrade_stick.overgrowth_level", state.overgrowth());
            guiGraphics.drawString(font, text, x + 54 + (70 - font.width(text)) / 2, y + STEP_Y + 3, TEXT_COLOR, false);
        }

        guiGraphics.drawString(font, Component.translatable("screen.dermicraft.upgrade_stick.tolerances",
                state.getHeldFamilyCount(), state.tier()), x + 8, y + 74, TEXT_COLOR, false);
        guiGraphics.drawString(font, Component.translatable("screen.dermicraft.upgrade_stick.selected", state.label()),
                x + 8, y + 126, TEXT_COLOR, false);
        guiGraphics.drawString(font, Component.translatable("screen.dermicraft.upgrade_stick.hint"),
                x + 8, y + 142, TEXT_COLOR, false);
    }

    private void drawButton(GuiGraphics guiGraphics, Button button, int x, int y, boolean hovered) {
        int bx = x + button.x();
        int by = y + button.y();
        int fill = !button.enabled() ? FILL_DISABLED : button.selected() ? FILL_SELECTED : hovered ? FILL_HOVER : FILL;
        guiGraphics.fill(bx, by, bx + button.w(), by + button.h(), BORDER);
        guiGraphics.fill(bx + 1, by + 1, bx + button.w() - 1, by + button.h() - 1, fill);

        int labelY;
        if (button.icon() != null) {
            guiGraphics.renderItem(button.icon(), bx + (button.w() - 16) / 2, by + 2);
            if (!button.enabled()) {
                guiGraphics.fill(bx + 1, by + 1, bx + button.w() - 1, by + button.h() - 1, 0x88333333);
            }
            labelY = by + button.h() - 10;
        } else {
            labelY = by + (button.h() - 8) / 2;
        }
        int color = button.enabled() ? 0x202020 : 0x909090;
        guiGraphics.drawString(font, button.label(), bx + (button.w() - font.width(button.label())) / 2, labelY, color, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        for (Button button : buildButtons()) {
            if (!button.tooltip().isEmpty() && button.contains(mouseX - leftPos, mouseY - topPos)) {
                guiGraphics.renderComponentTooltip(font, button.tooltip(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Button b : buildButtons()) {
                if (b.contains(mouseX - leftPos, mouseY - topPos)) {
                    if (b.enabled()) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b.id());
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
