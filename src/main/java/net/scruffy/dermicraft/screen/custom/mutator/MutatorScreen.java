package net.scruffy.dermicraft.screen.custom.mutator;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scruffy.dermicraft.block.entity.custom.MutatorBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.network.MutatorModeClickPayload;
import net.scruffy.dermicraft.renderer.gui.FluidTankRenderer;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;
import net.scruffy.dermicraft.util.MouseUtil;

public class MutatorScreen extends AbstractEvolvingMachineScreen<MutatorMenu> {

    private static final String BUTTONS_DIR = "textures/gui/buttons/";

    // Mode toggle -- reuses the Node's existing item/fluid button pair. MUTATE shows the item icon
    // (the machine is producing a different ITEM); FILL shows the fluid icon (the fluid becomes
    // cargo packaged into the container). A single button that flips the mode on click, not a
    // per-leg pair like the Node's -- no "off" state, it's always showing whichever mode is active.
    private static final ResourceLocation MODE_MUTATE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "item_button.png");
    private static final ResourceLocation MODE_FILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, BUTTONS_DIR + "fluid_button.png");
    private static final int MODE_BUTTON_SIZE = 18;

    private static final int REAGENT_X = 30;
    private static final int INPUT_X = 60;
    private static final int ARROW_X = 90;
    private static final int OUTPUT_X = 120;
    private static final int FUEL_X = 150;
    private static final int TANK_Y = 11;
    private static final int SLOT_Y = 34;
    private static final int ARROW_Y = 37;

    private static final int MODE_BUTTON_X = INPUT_X;
    private static final int MODE_BUTTON_Y = SLOT_Y - MODE_BUTTON_SIZE - 2; // directly above the input slot

    private FluidTankRenderer reagentRenderer;
    private FluidTankRenderer fuelRenderer;

    public MutatorScreen(MutatorMenu menu, Inventory playerInventory, Component title) {
        // Item output, so the button is the "auto dispense" wording of the fluid machines' auto drain.
        super(menu, playerInventory, title, "mutator", "auto_dispense");
    }

    @Override
    protected void init() {
        super.init();
        reagentRenderer = createFluidRenderer16x40(() -> menu.BE.getReagentTank().getCapacity());
        fuelRenderer = createFluidRenderer16x40(() -> menu.BE.getFuelTank().getCapacity());
    }

    // Directly above the crafting-progress arrow, one column right of the Mode button so the two don't collide.
    @Override
    protected int getAutoDrainButtonX() {
        return ARROW_X;
    }

    @Override
    protected int getAutoDrainButtonY() {
        return TANK_Y;
    }

    @Override
    protected boolean handleMainTabClick(double mouseX, double mouseY, int x, int y) {
        if (MouseUtil.isMouseOver((int) mouseX, (int) mouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_Y,
                MODE_BUTTON_SIZE, MODE_BUTTON_SIZE)) {
            PacketDistributor.sendToServer(new MutatorModeClickPayload(menu.BE.getBlockPos()));
            return true;
        }
        return false;
    }

    @Override
    protected void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getReagentTank().SLOT), REAGENT_X + 1, TANK_Y + 1, reagentRenderer,
                Component.translatable("tooltip.dermicraft.gauge.reagent"));

        renderFluidTooltipArea(guiGraphics, mouseX, mouseY, x, y,
                menu.BE.getFluid(menu.BE.getFuelTank().SLOT), FUEL_X + 1, TANK_Y + 1, fuelRenderer,
                Component.translatable("tooltip.dermicraft.gauge.fuel"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, REAGENT_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getReagentTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.reagent_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, FUEL_X + 1, 60,
                menu.BE.getItemHandler(null).getStackInSlot(menu.BE.getFuelTank().SLOT),
                Component.translatable("tooltip.dermicraft.slot.fuel_container"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, INPUT_X + 1, SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(MutatorBlockEntity.INPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.ingredient"));

        renderItemSlotTooltipArea(guiGraphics, mouseX, mouseY, x, y, OUTPUT_X + 1, SLOT_Y + 1,
                menu.BE.getItemHandler(null).getStackInSlot(MutatorBlockEntity.OUTPUT_SLOT),
                Component.translatable("tooltip.dermicraft.slot.result"));

        if (MouseUtil.isMouseOver(mouseX, mouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE)) {
            Component label = menu.getMode() == MutatorBlockEntity.Mode.FILL
                    ? Component.translatable("tooltip.dermicraft.mutator.mode_fill")
                    : Component.translatable("tooltip.dermicraft.mutator.mode_mutate");
            guiGraphics.renderTooltip(this.font, label, mouseX - x, mouseY - y);
        }
    }

    @Override
    protected void renderMainTab(GuiGraphics guiGraphics, int x, int y) {
        renderTankAndSlot(guiGraphics, x + REAGENT_X, y + TANK_Y);
        renderItemSlot(guiGraphics, x + INPUT_X, y + SLOT_Y);
        renderProgressArrow(guiGraphics, x + ARROW_X, y + ARROW_Y);
        renderItemSlot(guiGraphics, x + OUTPUT_X, y + SLOT_Y);
        renderTankAndSlot(guiGraphics, x + FUEL_X, y + TANK_Y);
        renderModeButton(guiGraphics, x + MODE_BUTTON_X, y + MODE_BUTTON_Y);

        reagentRenderer.render(guiGraphics, x + REAGENT_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getReagentTank().SLOT));
        fuelRenderer.render(guiGraphics, x + FUEL_X + 1, y + TANK_Y + 1, menu.BE.getFluid(menu.BE.getFuelTank().SLOT));
    }

    private void renderModeButton(GuiGraphics guiGraphics, int x, int y) {
        ResourceLocation texture = menu.getMode() == MutatorBlockEntity.Mode.FILL ? MODE_FILL_TEXTURE : MODE_MUTATE_TEXTURE;
        guiGraphics.blit(texture, x, y, 0, 0, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE);
    }
}
