package net.scruffy.dermicraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.List;

/**
 * Server -> client: a Workbench's current Gear Station pool contents (items + fluids), for the
 * Fabrication detail panel's live availability display. Needed because the pool can include vanilla
 * containers (a chest connected via the Lab Floor network) that don't proactively sync their
 * inventory to nearby clients the way this mod's own machines do -- the client's own capability
 * query would otherwise see an empty/stale chest until a player actually opens it, even though the
 * real server-side contents (and therefore actual crafting) are correct. See
 * WorkbenchBlockEntity#tickPoolSync/#sendPoolSyncTo for the sender side.
 */
public record WorkbenchPoolSyncPayload(BlockPos pos, List<ItemStack> items, List<FluidStack> fluids) implements CustomPacketPayload {

    public static final Type<WorkbenchPoolSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "workbench_pool_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchPoolSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WorkbenchPoolSyncPayload::pos,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), WorkbenchPoolSyncPayload::items,
            FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()), WorkbenchPoolSyncPayload::fluids,
            WorkbenchPoolSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
