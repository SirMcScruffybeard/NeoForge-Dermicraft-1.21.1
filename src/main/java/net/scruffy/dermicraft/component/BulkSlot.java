package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One slot of a {@link BulkItemData} buffer: an item identity (kept at count 1, components and
 * all) plus a real count tracked separately as a plain int.
 *
 * <p>Exists because a single {@link ItemStack}'s count is capped at 99 by the vanilla codec used to
 * save it to NBT ({@code ExtraCodecs.intRange(1, 99)}) -- a stack renders and syncs fine above that,
 * but crashes with {@code IllegalStateException} the moment it's saved. {@code CrawBlockEntity} hit
 * this and fixed it the same way: keep the item's identity as a count-1 template, track the real
 * quantity as its own field. This is that pattern, applied to a data component instead of a block
 * entity's saved NBT, so it can back gadgets that hold bulk stacks in hand (Eater, Portable Mass
 * Storage) rather than only in a placed block.
 */
public record BulkSlot(ItemStack template, int count) {

    public static final BulkSlot EMPTY = new BulkSlot(ItemStack.EMPTY, 0);

    public BulkSlot {
        // The template is an identity + component carrier only -- never a real quantity. Forcing
        // count 1 here means every other piece of code can copyWithCount freely without re-deriving
        // this invariant, and a template can never itself be the thing that hits the 99 cap.
        if (template.getCount() != 1 && !template.isEmpty()) {
            template = template.copyWithCount(1);
        }
    }

    public static final Codec<BulkSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC.fieldOf("template").forGetter(BulkSlot::template),
                    Codec.INT.fieldOf("count").forGetter(BulkSlot::count))
            .apply(instance, BulkSlot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BulkSlot> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, BulkSlot::template,
            ByteBufCodecs.VAR_INT, BulkSlot::count,
            BulkSlot::new);

    public boolean isEmpty() {
        return template.isEmpty() || count <= 0;
    }

    /**
     * A full {@link ItemStack} reflecting this slot's real count -- fine to hand out for display,
     * capability reads, or anything else that stays in memory. Never save this particular stack
     * directly via NBT if {@link #count} exceeds 99; go through this record's own codec instead.
     */
    public ItemStack asDisplayStack() {
        return isEmpty() ? ItemStack.EMPTY : template.copyWithCount(count);
    }

    public BulkSlot withCount(int newCount) {
        return newCount <= 0 ? EMPTY : new BulkSlot(template, newCount);
    }
}
