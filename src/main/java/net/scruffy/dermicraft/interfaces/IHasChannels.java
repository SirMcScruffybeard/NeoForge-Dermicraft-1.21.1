package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;

/**
 * Implemented by machine block entities that want to be reachable by the Gate multiblock without
 * going through the normal direction-locked {@code getItemHandler(Direction)}/
 * {@code getTank(Direction)} routing -- the routing every machine keeps today so a player can still
 * zone automation by face when they have the room for it (see the duct-system design notes for the
 * "cramped machine" problem this exists to solve).
 *
 * <p>Each implementing machine self-describes its own automatable slots/tanks as a flat list of
 * {@link Channel}s. Anything generic against this interface -- the Gate, or any future consumer --
 * never needs to know the machine's concrete type: adding Gate support to a new machine is a
 * one-time addition of this one method, nothing else changes.
 *
 * <p><b>Self-filtering (no GUI needed on the Gate side):</b> {@code getChannels()} is expected to be
 * dynamic, not a static list -- a machine omits a channel entirely once something is already reaching
 * it directly on its native face(s), via {@link #isFaceServiced}. This is what lets the Gate Controller
 * skip already-handled channels and only ever step in for genuine gaps, with zero player configuration.
 */
public interface IHasChannels {

    List<Channel> getChannels();

    /**
     * Whether ANY of {@code nativeFaces} already has a neighbour exposing a matching-kind capability
     * -- i.e. this channel is already reachable directly, without the Gate's help. A Gate block
     * ({@link IGateBlock}) on a native face never counts: otherwise a Gate Controller plugged into
     * the very face it's meant to service would make the machine omit that channel, and the Gate
     * would never be able to claim it (self-poisoning). "Any" rather than "all" faces, matching each
     * machine's own routing: if even one native face already works, the player doesn't need the Gate
     * for this channel at all.
     */
    default boolean isFaceServiced(Level level, BlockPos pos, Channel.Kind kind, Direction... nativeFaces) {
        for (Direction dir : nativeFaces) {
            BlockPos neighborPos = pos.relative(dir);
            if (level.getBlockState(neighborPos).getBlock() instanceof IGateBlock) continue;

            Direction opposite = dir.getOpposite();
            boolean serviced = switch (kind) {
                case ITEM -> level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, opposite) != null;
                case FLUID -> level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, opposite) != null;
            };
            if (serviced) return true;
        }
        return false;
    }

    /**
     * Human-readable description of what {@code face} actually reaches on this machine -- the raw
     * {@code getTank(Direction)}/{@code getItemHandler(Direction)} routing, mirrored directly (a
     * third small per-face switch alongside those two, same pattern each machine already repeats,
     * not a shared abstraction). Deliberately independent of {@link #getChannels}'s self-filtering:
     * this must stay accurate even on a face that's currently occupied (that's the case a player is
     * most likely to be pointing the I.D.E.P. at), so it can't be derived from the filtered list.
     */
    Component describeFace(Direction face);
}
