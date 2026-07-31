package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Collections;
import java.util.List;

/**
 * A fixed-size list of {@link BulkSlot}s carried inside another item's data component -- the
 * multi-slot, over-99-capable sibling of {@link HeldItemData}. Backs any gadget that stores
 * several bulk item stacks in hand rather than one (Eater, Portable Mass Storage); see
 * {@code IHaveItemData} for the handler wrapping this into an {@code IItemHandlerModifiable}.
 *
 * <p>Slot count is a property of the gadget/tier reading this data, not of the data itself -- this
 * record only ever stores however many entries {@link #withSize} was given. Resizing (e.g. a tier
 * upgrade granting more slots) means re-wrapping via {@link #withSize}, which pads with
 * {@link BulkSlot#EMPTY} or truncates as needed.
 */
public record BulkItemData(List<BulkSlot> slots) {

    public static final Codec<BulkItemData> CODEC =
            BulkSlot.CODEC.listOf().xmap(BulkItemData::new, BulkItemData::slots);

    public static final StreamCodec<RegistryFriendlyByteBuf, BulkItemData> STREAM_CODEC =
            BulkSlot.STREAM_CODEC.apply(ByteBufCodecs.list()).map(BulkItemData::new, BulkItemData::slots);

    public static BulkItemData empty(int size) {
        return new BulkItemData(Collections.nCopies(size, BulkSlot.EMPTY));
    }

    public BulkSlot slot(int index) {
        return index >= 0 && index < slots.size() ? slots.get(index) : BulkSlot.EMPTY;
    }

    public BulkItemData withSlot(int index, BulkSlot value) {
        List<BulkSlot> updated = new java.util.ArrayList<>(slots);
        updated.set(index, value);
        return new BulkItemData(updated);
    }

    /** Pads with {@link BulkSlot#EMPTY} or truncates to match a new slot count. */
    public BulkItemData withSize(int newSize) {
        if (newSize == slots.size()) return this;

        List<BulkSlot> resized = new java.util.ArrayList<>(newSize);
        for (int i = 0; i < newSize; i++) {
            resized.add(i < slots.size() ? slots.get(i) : BulkSlot.EMPTY);
        }
        return new BulkItemData(resized);
    }
}
