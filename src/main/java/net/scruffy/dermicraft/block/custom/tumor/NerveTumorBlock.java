package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.scruffy.dermicraft.item.ModItems;

public class NerveTumorBlock extends TumorBlock {

    public NerveTumorBlock() {
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
        return ModItems.NERVE_CLUSTER.get();
    }
}
