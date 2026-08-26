package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-item speed grant for a Work Speed Module, looked up via
 * {@code ModDataMaps.WORK_SPEED_MODULE_PROPERTIES} (keyed on the Module item) -- same kind-vs-data
 * split as {@link SafetyModuleProperties}: tag membership in {@code ModTags.Items.MODULE_WORK_SPEED}
 * marks an item as a Work Speed Module at all, this data map says WHAT multiplier a specific one
 * grants. Deliberately multiplicative onto the existing fuel-grade/tier speed calculation
 * ({@code AbstractFueledMachineBlockEntity#setSpeed()}), not additive -- and since fuel use rate is
 * computed independently of speed, a faster machine also finishes each craft using less total fuel,
 * not just less time. That's an intentional part of the payoff, not an oversight -- keep the
 * multiplier and the item's own crafting cost tuned together, not the mechanic patched to remove it.
 */
public record WorkSpeedModuleProperties(float speedMultiplier) {

    public static final Codec<WorkSpeedModuleProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf("speed_multiplier").forGetter(WorkSpeedModuleProperties::speedMultiplier))
            .apply(instance, WorkSpeedModuleProperties::new));
}
