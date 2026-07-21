package net.scruffy.dermicraft.screen.custom.render_furnace;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.RenderFurnaceBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class RenderFurnaceMenu extends AbstractModMenu {

    public final RenderFurnaceBlockEntity BE;
    private final Level level;

    public RenderFurnaceMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public RenderFurnaceMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.RENDER_FURNACE_MENU.get(), containerId, 3);
        checkContainerSize(inv, 2);
        this.BE = (RenderFurnaceBlockEntity) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), RenderFurnaceBlockEntity.INPUT_SLOT, 61, 35));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), RenderFurnaceBlockEntity.OUTPUT_SLOT, 121, 35));
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.RENDER_FURNACE, BE);
    }

    public boolean isCrafting() {
        return BE.isStillCrafting();
    }

    public int getScaledArrowProgress() {
        int arrowPixels = 15;
        return BE.getScaledProgress(arrowPixels);
    }
}
