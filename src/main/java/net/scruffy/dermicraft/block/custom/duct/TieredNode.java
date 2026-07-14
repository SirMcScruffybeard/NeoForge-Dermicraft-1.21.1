package net.scruffy.dermicraft.block.custom.duct;

/**
 * Implemented by a Node's {@code Block} to declare which {@link NodeTier} it is. The Node block
 * entity reads the tier off its block state at runtime, so the tier never has to be stored in NBT
 * and one block-entity class can serve every Node tier.
 */
public interface TieredNode {
    NodeTier getTier();
}
