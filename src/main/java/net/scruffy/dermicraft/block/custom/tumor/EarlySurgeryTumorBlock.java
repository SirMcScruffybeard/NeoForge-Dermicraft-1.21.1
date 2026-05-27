package net.scruffy.dermicraft.block.custom.tumor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.util.ToolUtil;
import org.jetbrains.annotations.Nullable;

public abstract class EarlySurgeryTumorBlock extends BaseEntityBlock {

    protected EarlySurgeryTumorBlock(Properties properties) {
        super(properties.noLootTable());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Override
    public abstract @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState);

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
