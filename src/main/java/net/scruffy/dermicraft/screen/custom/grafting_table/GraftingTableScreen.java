package net.scruffy.dermicraft.screen.custom.grafting_table;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.block.entity.custom.GraftingTableBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;

/** Same fuel-far-right convention as every other machine's screen, with the HP bar on the far left. The 3x3 grid
 * is the one genuinely new layout element in the mod -- nine item_slot tiles instead of the usual 1-2 slots. */
public class GraftingTableScreen extends AbstractEvolvingMachineScreen<GraftingTableMenu> {

    @Override
    protected net.minecraft.client.renderer.Rect2i getRecipeArrowArea() {
        return arrowAt(ARROW_X, ARROW_Y);
    }

    private static final String SLOTS_DIR = "textures/gui/slots/";

    private static final ResourceLocation GHOST_WASH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, SLOTS_DIR + "green_wash_overlay.png");
    private static final int GHOST_WASH_SIZE = 16;

    // Grid origin/step MUST match GraftingTableMenu's GRID_X/GRID_Y/GRID_STEP -- this class only
    // draws the slot art, the menu places the real slots. The grid sits right of the HP bar.
    private static final int GRID_X = 30;
    private static final int GRID_Y = 11;
    private static final int GRID_STEP = 18;

    private static final int ARROW_X = 90;
    private static final int ARROW_Y = 32;
    private static final int OUTPUT_X = 120;
    private static final int OUTPUT_Y = 29;
    private static final int FUEL_X = 150;
    private static final int TANK_Y = 11;

    private FluidTankRenderer fuelRenderer;

    public GraftingTableScreen(GraftingTableMenu menu, Inventory playerInventory, Component title) {
        // Item output, so the button is the "auto dispense" wording of the fluid machines' auto drain.
        super(menu, playerInventory, title, "grafting_table", "auto_dispense");
    }

    @Override
    protected void init() {
        super.init();
        fuelRenderer = createFluidRenderer16x40(() -> menu.BE.getFuelTank().getCapacity());
    }

    // The 3x3 grid ends at x=84, so the column above the arrow is clear.
    @Override
    protected int getAutoDrainButtonX() {
        return ARROW_X;
    }

    @Override
    protected int getAutoDrainButtonY() {
        return TANK_Y;
    }

    @Override
    protected void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, FUEL_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, OUTPUT_X + 1, OUTPUT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(GraftingTableBlockEntity.OUTPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.result"));
    }

    @Override
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
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
}
