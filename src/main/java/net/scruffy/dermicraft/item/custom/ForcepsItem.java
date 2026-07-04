package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.interfaces.ICollectBlocks;
import net.scruffy.dermicraft.item.custom.base.ToolItem;

public class ForcepsItem extends ToolItem implements ICollectBlocks {
    public ForcepsItem() {
        super(new Item.Properties());
    }

    // onItemUseFirst runs before the clicked block's own useItemOn (and before the crouch/
    // secondary-use check), so the Forceps can pick up an interactive block — a machine, a
    // storage block — without the player having to crouch to bypass that block's right-click.
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null || !canCollect(level, pos)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        collect(level, pos, player);
        return InteractionResult.SUCCESS;
    }
}
