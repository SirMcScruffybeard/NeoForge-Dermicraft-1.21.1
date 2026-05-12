package net.scruffy.dermicraft.block.entity.custom.tumor;

import net.minecraft.world.level.block.SoundType;

public class InertTumorBlock extends TumorBlock{
    public InertTumorBlock(Properties properties) {
        super(properties
                .strength(.05f)
                .explosionResistance(15f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.6f)
                .ignitedByLava()
        );
    }
}
