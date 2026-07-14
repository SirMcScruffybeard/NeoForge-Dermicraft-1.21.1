package net.scruffy.dermicraft.interfaces;

/**
 * Marker for every block that is part of the Gate multiblock (Controller, Buffer, Port). Two uses:
 * <ul>
 *   <li>The Controller's adjacency-chain bonding flood-fills over neighbours that are Gate blocks.</li>
 *   <li>A machine's {@link IHasChannels#getChannels()} self-filter must NOT count a Gate block sitting
 *       on a native face as "already serviced" -- otherwise the Controller's own presence would make
 *       the machine omit the very channel the Controller is there to service (self-poisoning). The
 *       self-filter skips any neighbour that is an {@code IGateBlock}.</li>
 * </ul>
 */
public interface IGateBlock {
}
