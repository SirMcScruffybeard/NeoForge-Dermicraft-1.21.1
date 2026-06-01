package net.scruffy.dermicraft.block.custom.tumor;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.custom.ModBaseEntityBlock;
import net.scruffy.dermicraft.util.ToolUtil;

public abstract class EarlySurgeryTumorBlock extends ModBaseEntityBlock {

    protected EarlySurgeryTumorBlock(Properties properties) {
        super(properties.noLootTable());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

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
