package net.scruffy.dermicraft.block.custom.gate;

import net.minecraft.world.level.block.Block;
import net.scruffy.dermicraft.interfaces.IGateBlock;

/**
 * Gate Port -- the dumb, stateless edge of the Gate multiblock. Has no block entity and no config:
 * its whole job is to expose, on its outward faces, the capability of whichever Gate Buffer it is
 * physically touching (same direct-adjacency idea a duct uses to reach a machine's END face). That
 * capability wiring is registered externally (see {@code ModBusEvents}); the block itself is just a
 * marker + geometry. Bonds into a Controller's structure via the adjacency chain like any other
 * {@link IGateBlock}.
 */
public class GatePortBlock extends Block implements IGateBlock {

    public GatePortBlock(Properties properties) {
        super(properties);
    }
}
