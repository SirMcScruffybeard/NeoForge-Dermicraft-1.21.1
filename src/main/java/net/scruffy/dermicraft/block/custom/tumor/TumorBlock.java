package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.scruffy.dermicraft.util.ToolUtil;

public abstract class TumorBlock extends Block {

    public TumorBlock(Properties properties) {
        super(properties
                .noLootTable());
    }

    public boolean isCollectionTool(ItemStack stack) {
        return ToolUtil.isCollectionTool(stack);
    }

    public boolean isExtractionTool(ItemStack stack) {
        return ToolUtil.isExtractionTool(stack);
    }

    public boolean isInjectionTool(ItemStack stack) {
        return ToolUtil.isInjectionTool(stack);
    }

    public boolean isSutureTool(ItemStack stack) {
        return ToolUtil.isSutureTool(stack);
    }
}
