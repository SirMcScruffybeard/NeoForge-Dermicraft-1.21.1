package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** A single held ItemStack carried inside another item's data component -- the item-side twin of
 * {@link FluidData}, same shape. Used by the I.D.E.P. tool to hold whatever it extracted from a
 * jammed Gate Buffer. */
public record HeldItemData(ItemStack itemStack) {

    public static final Codec<HeldItemData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ItemStack.OPTIONAL_CODEC.fieldOf("heldItem").forGetter(HeldItemData::itemStack))
                    .apply(instance, HeldItemData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeldItemData> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC, HeldItemData::itemStack,
                    HeldItemData::new);

    public static final HeldItemData EMPTY = new HeldItemData(ItemStack.EMPTY);

    public boolean isEmpty() {
        return itemStack.isEmpty();
    }
}
