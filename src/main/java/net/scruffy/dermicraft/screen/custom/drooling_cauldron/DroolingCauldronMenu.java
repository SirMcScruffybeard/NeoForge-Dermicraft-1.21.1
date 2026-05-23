package net.scruffy.dermicraft.screen.custom.drooling_cauldron;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.DroolingCauldronBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class DroolingCauldronMenu extends AbstractModMenu {

    public final DroolingCauldronBlockEntity be;
    private final Level level;
    private final ContainerData data;

    public DroolingCauldronMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public DroolingCauldronMenu(int pContainerId, Inventory inv, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.DROOLING_CAULDRON_MENU.get(), pContainerId, 2);
        checkContainerSize(inv, 2);
        this.be = ((DroolingCauldronBlockEntity) blockEntity);
        this.level = inv.player.level();
        this.data = data;

        super.addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.be.INVENTORY, 0, 44, 34));
        this.addSlot(new SlotItemHandler(this.be.INVENTORY, 1, 116, 34) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.DROOLING_CAULDRON, be);
    }

    public boolean isCrafting() {
        return be.isStillCrafting();
    }

    public int getScaledArrowProgress() {
        int arrowPixels = 15;
        return be.getScaledProgress(arrowPixels);
    }
}
