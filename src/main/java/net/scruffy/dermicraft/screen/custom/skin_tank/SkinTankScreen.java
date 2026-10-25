package net.scruffy.dermicraft.screen.custom.skin_tank;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.machine.EvolutionResult;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.AutoDrainToggleClickPayload;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.util.MouseUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pilot for the shared tab-bar helper (dermicraft-progression-notes.md, Decision Point #2 ->
 * sequencing step 4): a Main tab (the existing tank+slots content, unchanged) and a Module tab (one
 * yellow Module slot, {@link SkinTankBlockEntity#installedHazardProfile} reads whatever's in it).
 */
public class SkinTankScreen extends AbstractModScreen<SkinTankMenu> {

    private static final String BACKGROUNDS_DIR = "textures/gui/backgrounds/";
    private static final String TANKS_DIR = "textures/gui/tanks/";
    private static final String SLOTS_DIR = "textures/gui/slots/";
    private static final String ARROWS_DIR = "textures/gui/arrows/";
    private static final String BUTTONS_DIR = "textures/gui/buttons/";

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BACKGROUNDS_DIR + "screen_background.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    private static final ResourceLocation TALL_TANK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, TANKS_DIR + "tall_tank_guage.png");
    private static final int TALL_TANK_WIDTH = 18;
    private static final int TALL_TANK_HEIGHT = 66;

    private static final ResourceLocation ITEM_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "item_slot.png");
    private static final int ITEM_SLOT_SIZE = 18;

    // The dedicated Evolution slot's background (the slot itself is in SkinTankMenu).
    private static final ResourceLocation EVOLUTION_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/gui/slots/evolution_slot.png");

    // The evolution countdown bar, beside the Evolution slot: a dark track with a green fill rising from the bottom.
    private static final int EVOLUTION_BAR_WIDTH = 2;
    private static final int EVOLUTION_BAR_GAP = 2;
    private static final int EVOLUTION_BAR_TRACK_COLOR = 0xFF2B2B2B;
    private static final int EVOLUTION_BAR_FILL_COLOR = 0xFF3CD13C;

    private static final ResourceLocation ARROW_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, ARROWS_DIR + "arrow_background.png");
    private static final int ARROW_WIDTH = 17;
    private static final int ARROW_HEIGHT = 10;

    private FluidTankRenderer fluidRenderer;

    // Auto-drain toggle -- sits right of the OUTPUT slot (x+116, 18px wide -> ends 134), same 4px
    // gap convention as Craw's/Masticator's own auto-push/auto-drain buttons.
    private static final ResourceLocation AUTO_DRAIN_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "output_button.png");
    private static final ResourceLocation AUTO_DRAIN_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "no_use_button.png");
    private static final int AUTO_DRAIN_BUTTON_SIZE = 18;
    private static final int AUTO_DRAIN_BUTTON_X = 98;
    private static final int AUTO_DRAIN_BUTTON_Y = 11;

    public SkinTankScreen(SkinTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // Tab bar -- see AbstractModScreen's "Shared tab-bar state"/Tab record. Only two entries;
    // labels/colors mirror WorkbenchScreen's own tab text convention (a single green, since Skin
    // Tank doesn't need per-tab colors to tell them apart the way Workbench's Mod/Fabrication split
    // might eventually want).
    private static final int TAB_TEXT_COLOR = 0x007F0E;
    private List<Tab> tabs;

    @Override
    protected void init() {
        super.init();
        fluidRenderer = createFluidRenderer16x64(menu.be.getTank().getTankCapacity(0));
        tabs = List.of(
                new Tab(Component.translatable("screen.dermicraft.skin_tank.main_tab"), TAB_TEXT_COLOR),
                new Tab(Component.translatable("screen.dermicraft.skin_tank.module_tab"), TAB_TEXT_COLOR));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (menu.getActiveTab() == SkinTankMenu.MAIN_TAB) {
            renderFluidTooltipArea(guiGraphics, pMouseX, pMouseY, x, y, menu.be.getFluid(), 80, 8, fluidRenderer);

            if (MouseUtil.isMouseOver(pMouseX, pMouseY, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y,
                    AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
                Component label = menu.isAutoDrainEnabled()
                        ? Component.translatable("tooltip.dermicraft.machine.auto_drain_on")
                        : Component.translatable("tooltip.dermicraft.machine.auto_drain_off");
                guiGraphics.renderTooltip(this.font, label, pMouseX - x, pMouseY - y);
            }
        } else if (isEvolutionSlot(hoveredSlot) && !hoveredSlot.hasItem()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                            Component.translatable("tooltip.dermicraft.evolution_slot.empty"),
                            Component.translatable("tooltip.dermicraft.evolution_slot.empty_hint").withStyle(ChatFormatting.GRAY)),
                    pMouseX - x, pMouseY - y);
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
        renderTabs(guiGraphics, x, y, tabs, menu.getActiveTab());

        if (menu.getActiveTab() == SkinTankMenu.MAIN_TAB) {
            renderMainTab(guiGraphics, x, y);
        } else {
            renderModuleTab(guiGraphics, x, y);
        }
    }

    private void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TALL_TANK_TEXTURE, x + 79, y + 7, 0, 0, TALL_TANK_WIDTH, TALL_TANK_HEIGHT,
                TALL_TANK_WIDTH, TALL_TANK_HEIGHT);

        // Slots sit 1px further out than their logical slot position so the slot-to-arrow
        // gap matches the arrow-to-gauge gap (2px on each side), same as the Drooling Cauldron.
        renderItemSlot(guiGraphics, x + 42, y + 33);
        renderItemSlot(guiGraphics, x + 116, y + 33);

        // No crafting/progress concept on this machine, so both arrows are track-only.
        renderArrowBackground(guiGraphics, x + 61, y + 37);
        renderArrowBackground(guiGraphics, x + 98, y + 37);

        fluidRenderer.render(guiGraphics, x + 80, y + 8, menu.be.getFluid());

        renderAutoDrainButton(guiGraphics, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y);
    }

    private void renderAutoDrainButton(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isAutoDrainEnabled()) {
            blitRotated90(guiGraphics, AUTO_DRAIN_ON_TEXTURE, x, y, AUTO_DRAIN_BUTTON_SIZE);
        } else {
            guiGraphics.blit(AUTO_DRAIN_OFF_TEXTURE, x, y, 0, 0, AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE,
                    AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE);
        }
    }

    /** One yellow Module slot -- nothing else on this tab, matching Eater's/Drinker's own bare
     * Module-slot-only look where no gauge/drain slot sits alongside it. */
    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        // The Evolution Module slot sits left of the general Module slots, Module tab only.
        guiGraphics.blit(EVOLUTION_SLOT_TEXTURE, x + SkinTankMenu.EVOLUTION_SLOT_X, y + SkinTankMenu.EVOLUTION_SLOT_Y,
                0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        renderEvolutionBar(guiGraphics, x, y);
        for (int i = 0; i < menu.be.getModuleSlotCount(); i++) {
            guiGraphics.blit(MODULE_SLOT_TEXTURE,
                    x + SkinTankMenu.MODULE_SLOT_X + i * SkinTankMenu.MODULE_SLOT_SPACING,
                    y + SkinTankMenu.MODULE_SLOT_Y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        }
    }

    /** The evolution countdown's progress, a thin bar beside the Evolution slot that fills from the bottom up.
     * Only drawn while a countdown runs, so an idle slot looks the same as before. */
    private void renderEvolutionBar(GuiGraphics guiGraphics, int x, int y) {
        float progress = menu.be.getEvolutionProgress();
        if (progress <= 0f) return;

        int left = x + SkinTankMenu.EVOLUTION_SLOT_X - EVOLUTION_BAR_GAP - EVOLUTION_BAR_WIDTH;
        int top = y + SkinTankMenu.EVOLUTION_SLOT_Y;
        int bottom = top + SLOT_SIZE;
        guiGraphics.fill(left, top, left + EVOLUTION_BAR_WIDTH, bottom, EVOLUTION_BAR_TRACK_COLOR);

        int filled = Math.round(SLOT_SIZE * progress);
        guiGraphics.fill(left, bottom - filled, left + EVOLUTION_BAR_WIDTH, bottom, EVOLUTION_BAR_FILL_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int clickedTab = tabClickedAt(mouseX, mouseY, x, y, tabs.size());
        if (clickedTab >= 0 && clickedTab != menu.getActiveTab()) {
            menu.setActiveTab(clickedTab);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AbstractModMenu.TAB_BUTTON_BASE + clickedTab);
            return true;
        }

        if (menu.getActiveTab() == SkinTankMenu.MAIN_TAB
                && MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + AUTO_DRAIN_BUTTON_X, y + AUTO_DRAIN_BUTTON_Y,
                AUTO_DRAIN_BUTTON_SIZE, AUTO_DRAIN_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new AutoDrainToggleClickPayload(menu.be.getBlockPos()));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderItemSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ITEM_SLOT_TEXTURE, x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE,
                ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }

    private void renderArrowBackground(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        guiGraphics.blit(ARROW_BACKGROUND_TEXTURE, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT,
                ARROW_WIDTH, ARROW_HEIGHT);
    }

    private boolean isEvolutionSlot(@Nullable Slot slot) {
        return slot instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() == menu.be.EVOLUTION_INVENTORY;
    }

    /** The status line(s) under the Evolution slot's tooltip, from {@link SkinTankBlockEntity#getEvolutionStatus()}. */
    private List<Component> getEvolutionStatusLines() {
        Optional<EvolutionResult> status = menu.be.getEvolutionStatus();
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

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = super.getTooltipFromContainerItem(stack);
        if (!isEvolutionSlot(hoveredSlot)) return lines;
        List<Component> withStatus = new ArrayList<>(lines);
        withStatus.addAll(getEvolutionStatusLines());
        return withStatus;
    }
}
