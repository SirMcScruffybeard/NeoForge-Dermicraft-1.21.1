package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.world.level.block.Block;

public abstract class TumorBlock extends Block {

    public TumorBlock(Properties properties) {
        super(properties
                .noLootTable());
    }
}
