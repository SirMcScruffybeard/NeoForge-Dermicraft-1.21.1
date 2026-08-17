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

    public MasticatorMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MasticatorMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MASTICATOR_MENU.get(), containerId, 4);
        checkContainerSize(inv, 2);
        this.BE = ((MasticatorBlockEntity) blockEntity);
        this.level = inv.player.level();


        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getIngredientTank().SLOT, 67, 60));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getResultTank().SLOT, 123, 60));
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), MasticatorBlockEntity.INGREDIENT_ITEM_SLOT, 38, 35));

        setQuickMoveInputSlots(3, 1); // INGREDIENT_ITEM_SLOT only -- skip fuel/ingredient/result tank slots
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

    public int getHealth() {
        return BE.getHealth();
    }

    public int getMaxHealth() {
        return BE.getMaxHealth();
    }
}
