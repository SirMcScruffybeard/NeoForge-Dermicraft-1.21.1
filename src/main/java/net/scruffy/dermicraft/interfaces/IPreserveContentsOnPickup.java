package net.scruffy.dermicraft.interfaces;

/**
 * Marker for a block entity whose contents should travel with it when picked up (e.g. by the
 * Forceps) instead of spilling on the ground. Purely a declaration — the pickup logic lives in
 * {@link ICollectBlocks}, which saves the block entity's data onto the recovered block item and
 * removes the block entity before clearing the block, so nothing drops. Implemented by the Craw.
 */
public interface IPreserveContentsOnPickup {
}
