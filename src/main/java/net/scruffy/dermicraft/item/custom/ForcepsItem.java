package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.interfaces.ICollectBlocks;
import net.scruffy.dermicraft.item.custom.base.ToolItem;

public class ForcepsItem extends ToolItem implements ICollectBlocks {
    public ForcepsItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (canCollect(level, context.getClickedPos())) {
            collect(level, context.getClickedPos(), context.getPlayer());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void collect(Level level, BlockPos pos, Player player) {
        grabItem(player, getBlockItem(level, pos));
        changeToAir(level, pos);
        playDefaultPickupSound(level, pos);
    }
}
