package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-item data for a Keep-on-Death Module (Salvage/Anchor), looked up via
 * {@code ModDataMaps.KEEP_ON_DEATH_MODULE_PROPERTIES} (keyed on the Module item) -- same
 * kind-vs-data split as {@link SafetyModuleProperties}: tag membership in
 * {@code ModTags.Items.MODULE_KEEP_ON_DEATH} marks an item as one of these Modules at all, this
 * data map says WHICH tier a specific one is.
 *
 * <p>{@code consumed}: true for the cheap Salvage tier (a single save, then the Module itself is
 * destroyed -- Totem of Undying shape); false for the expensive Anchor tier (never consumed, gated
 * instead by an optional cooldown -- see {@code Config#ANCHOR_MODULE_COOLDOWN_SECONDS}).
 */
public record KeepOnDeathModuleProperties(boolean consumed) {

    public static final Codec<KeepOnDeathModuleProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("consumed").forGetter(KeepOnDeathModuleProperties::consumed))
            .apply(instance, KeepOnDeathModuleProperties::new));
}
