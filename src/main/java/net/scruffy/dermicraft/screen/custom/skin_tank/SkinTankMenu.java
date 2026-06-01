package net.scruffy.dermicraft.screen.custom.skin_tank;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;


public class SkinTankMenu extends AbstractModMenu {

    public final SkinTankBlockEntity be;
    private Level level;

    public SkinTankMenu(int containerId, Inventory inventory, FriendlyByteBuf extendData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extendData.readBlockPos()));
    }

    public SkinTankMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenuTypes.SKIN_TANK_MENU.get(), containerId, 2);

        checkContainerSize(inventory, 2);
        this.be = (SkinTankBlockEntity) blockEntity;
        this.level = inventory.player.level();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.addSlot(new SlotItemHandler(be.INVENTORY, 0, 44, 34));
        this.addSlot(new SlotItemHandler(be.INVENTORY, 1, 116, 34) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.SKIN_TANK, be);
    }
}
