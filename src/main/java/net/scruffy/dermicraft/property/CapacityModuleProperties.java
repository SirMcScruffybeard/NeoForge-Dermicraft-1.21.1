package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-item fluid-capacity bonus for a Capacity Module, looked up via
 * {@code ModDataMaps.CAPACITY_MODULE_PROPERTIES} (keyed on the Module item) -- same kind-vs-data
 * split as every other Module property record: tag membership in
 * {@code ModTags.Items.MODULE_CAPACITY} marks an item as a Capacity Module at all, this data map
 * says how much mB a specific one grants. Deliberately additive, not multiplicative, and applied
 * to EVERY tank on the consumer (not one specific tank) -- see the design discussion this came
 * from. Fluid-only for now; item-slot capacity is an explicitly deferred future extension, not
 * covered by this record.
 */
public record CapacityModuleProperties(int bonusAmount) {

    public static final Codec<CapacityModuleProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("bonus_amount").forGetter(CapacityModuleProperties::bonusAmount))
            .apply(instance, CapacityModuleProperties::new));
}
