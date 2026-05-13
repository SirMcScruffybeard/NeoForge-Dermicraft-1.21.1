package net.scruffy.dermicraft.block.entity.custom.tumor;

import net.minecraft.world.level.block.Block;
import net.scruffy.dermicraft.interfaces.IHarvestable;

public abstract class TumorBlock extends Block {
    public TumorBlock(Properties properties) {
        super(properties
                .noLootTable());
    }
    

}
