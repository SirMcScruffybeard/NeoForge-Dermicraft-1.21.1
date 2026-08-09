package net.scruffy.dermicraft.screen.custom.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.custom.floor.GearStationPool;

import java.util.List;

/**
 * Client-side cache of the latest {@link net.scruffy.dermicraft.network.WorkbenchPoolSyncPayload}
 * received -- see that payload's own javadoc for why this exists at all (a vanilla container like a
 * connected chest won't proactively sync its contents to a client that hasn't opened it, so the
 * client's own live capability query would otherwise show it as empty/unavailable regardless of the
 * real, server-side contents).
 *
 * <p>One entry only: only one Workbench screen can be open on a given client at a time, so there's
 * no need to key this by position beyond guarding against a stray/late packet for a Workbench that
 * isn't the one currently open.
 */
public final class WorkbenchClientPool {

    private static BlockPos pos = null;
    private static GearStationPool.Snapshot snapshot = new GearStationPool.Snapshot(List.of(), List.of());

    private WorkbenchClientPool() {
    }

    public static void update(BlockPos syncedPos, List<ItemStack> items, List<FluidStack> fluids) {
        pos = syncedPos;
        snapshot = new GearStationPool.Snapshot(items, fluids);
    }

    /** The last synced snapshot for {@code wantedPos}, or an empty one if nothing's been synced for
     * it yet (e.g. the very first frame before the server's first broadcast arrives). */
    public static GearStationPool.Snapshot get(BlockPos wantedPos) {
        return wantedPos.equals(pos) ? snapshot : new GearStationPool.Snapshot(List.of(), List.of());
    }
}
