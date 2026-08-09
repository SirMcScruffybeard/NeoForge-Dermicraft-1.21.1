package net.scruffy.dermicraft.block.custom.floor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A Lab Floor block variant (see dermicraft-machine-notes.md -> Lab Floor). Purely a pass-through
 * connector -- see {@link FloorNetwork} for what actually happens across a network of these. What
 * varies per variant is material (vanilla strength/hardness, copied via the properties passed in)
 * and, from here, {@link #floorTier()}.
 */
public class LabFloorBlock extends Block implements TieredFloor {

    private final int tier;

    public LabFloorBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int floorTier() {
        return tier;
    }
}
