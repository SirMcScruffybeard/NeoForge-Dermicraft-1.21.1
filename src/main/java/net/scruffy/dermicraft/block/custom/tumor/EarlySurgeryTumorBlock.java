package net.scruffy.dermicraft.block.custom.tumor;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.scruffy.dermicraft.block.custom.ModBaseEntityBlock;

public abstract class EarlySurgeryTumorBlock extends ModBaseEntityBlock {

    protected EarlySurgeryTumorBlock(Properties properties) {
        super(properties.noLootTable());
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();
}
