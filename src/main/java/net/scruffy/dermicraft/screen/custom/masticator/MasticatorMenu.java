package net.scruffy.dermicraft.screen.custom.masticator;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class MasticatorMenu extends AbstractModMenu {

    public final MasticatorBlockEntity BE;
    private Level level;
    private final ContainerData data;

    public MasticatorMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public MasticatorMenu(int containerId, Inventory inv, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MASTICATOR_MENU.get(), containerId, 3);
        checkContainerSize(inv, 2);
        this.BE = ((MasticatorBlockEntity) blockEntity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.FUEL_SLOT, 27, 59));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.INGREDIENT_SLOT, 80, 59));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.RESULT_SLOT, 133, 59));

        addDataSlots(data);
    }

    @Override

    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.MASTICATOR, BE);
    }

    public boolean isCrafting() {
        return BE.isStillCrafting();
    }

    public int getScaledArrowProgress() {
        int arrowPixels = 15;
        return BE.getScaledProgress(arrowPixels);
    }
}
