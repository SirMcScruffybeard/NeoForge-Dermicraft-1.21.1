package net.scruffy.dermicraft.block.custom;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.scruffy.dermicraft.block.entity.custom.tumor.TumorBlock;

public class MarredTumorBlock extends TumorBlock {
    public MarredTumorBlock(BlockBehaviour.Properties properties) {
        super(properties
                .strength(.02f)
                .explosionResistance(5f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.8f)
                .ignitedByLava()
        );
    }
}
