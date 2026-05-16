package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class MarredTumorBlock extends TumorBlock {

    public MarredTumorBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(.02f)
                .explosionResistance(5f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.8f)
                .ignitedByLava());
    }

}
