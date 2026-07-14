package net.scruffy.dermicraft.interfaces;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * One self-described automatable slot/tank on a machine that implements {@link IHasChannels} --
 * the unit the (planned) Gate multiblock relays against instead of going through the normal
 * direction-locked {@code getItemHandler(Direction)}/{@code getTank(Direction)} routing.
 *
 * <p>{@code io} is intrinsic to the channel, not a player choice (unlike a Node leg's In/Out): a
 * channel already knows whether it's fed or drained, so whatever relays it just pulls from every
 * {@link IO#OUT} channel and pushes into every {@link IO#IN} channel, no separate direction config
 * needed. {@link IO#BOTH} exists for the handful of channels that are genuinely bidirectional
 * general-purpose storage rather than a machine's distinct crafting input/output (e.g. Skin Tank's
 * fluid tank, Craw's item storage) -- those already accept both directions today with no face-lock
 * at all, so a BOTH channel is simply relayed whichever way the moment calls for.
 *
 * <p>Labels are plain {@link Component#literal} for now, not translatable -- nothing renders them
 * yet (no GUI on this system by design so far); switch to {@code Component.translatable} with real
 * lang entries once/if something actually displays a channel's label.
 */
public sealed interface Channel {

    String id();

    Component label();

    IO io();

    /** Whether this channel is item- or fluid-shaped -- lets a Gate Buffer pick which universal store
     * to use without knowing the concrete channel type. */
    Kind kind();

    enum IO {IN, OUT, BOTH}

    enum Kind {ITEM, FLUID}

    record ItemChannel(String id, Component label, IO io, IItemHandler handler) implements Channel {
        @Override
        public Kind kind() {
            return Kind.ITEM;
        }
    }

    record FluidChannel(String id, Component label, IO io, IFluidHandler handler) implements Channel {
        @Override
        public Kind kind() {
            return Kind.FLUID;
        }
    }
}
