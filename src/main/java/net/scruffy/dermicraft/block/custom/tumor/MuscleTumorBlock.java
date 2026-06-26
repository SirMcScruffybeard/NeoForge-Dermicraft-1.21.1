package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.scruffy.dermicraft.item.ModItems;

public class MuscleTumorBlock extends TumorBlock {

    public MuscleTumorBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(.05f)
                .explosionResistance(15f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.6f)
                .ignitedByLava()
        );
    }

    @Override
    protected Item getHarvestItem() {
        return ModItems.DENSE_MUSCLE.get();
    }
}
