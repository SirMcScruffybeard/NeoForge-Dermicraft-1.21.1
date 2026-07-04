package net.scruffy.dermicraft.screen.custom.metastasizer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MetastasizerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class MetastasizerMenu extends AbstractModMenu {

    public final MetastasizerBlockEntity BE;
    private final Level level;

    public MetastasizerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MetastasizerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.METASTASIZER_MENU.get(), containerId, 4);
        checkContainerSize(inv, 2);
        this.BE = (MetastasizerBlockEntity) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getReagentTank().SLOT, 31, 60));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), MetastasizerBlockEntity.PATTERN_SLOT, 61, 35));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), MetastasizerBlockEntity.OUTPUT_SLOT, 121, 35));
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.METASTASIZER, BE);
    }

    public boolean isCrafting() {
        return BE.isStillCrafting();
    }

    public int getScaledArrowProgress() {
        int arrowPixels = 15;
        return BE.getScaledProgress(arrowPixels);
    }

    public int getHealth() {
        return BE.getHealth();
    }

    public int getMaxHealth() {
        return BE.getMaxHealth();
    }
}
