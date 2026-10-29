package net.scruffy.dermicraft.item.custom.module;

import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.component.LevelModuleData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.item.ModItems;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Where each loaded module item's variants are defined. See {@link ModuleFamily}. */
public final class ModuleFamilies {

    private ModuleFamilies() {}

    /** Tier Evolution Modules: Tier 2 to 4, shown as roman-numeral icons. */
    public static final ModuleFamily<LevelModuleData> TIER = new ModuleFamily<LevelModuleData>(
            "tier_evolution_module",
            () -> ModItems.TIER_EVOLUTION_MODULE.get(),
            () -> ModDataComponentTypes.LEVEL_MODULE_DATA.get(),
            List.of(new ModuleVariant<>("2", "l2", new LevelModuleData(2)),
                    new ModuleVariant<>("3", "l3", new LevelModuleData(3)),
                    new ModuleVariant<>("4", "l4", new LevelModuleData(4))),
            Objects::equals);

    /** Overgrowth Evolution Modules: one shared icon whatever the level, so the one variant matches every level. */
    public static final ModuleFamily<LevelModuleData> OVERGROWTH = new ModuleFamily<LevelModuleData>(
            "overgrowth_evolution_module",
            () -> ModItems.OVERGROWTH_EVOLUTION_MODULE.get(),
            () -> ModDataComponentTypes.LEVEL_MODULE_DATA.get(),
            List.of(new ModuleVariant<>("all", "o", new LevelModuleData(1))),
            (sample, actual) -> true);

    /** Hazard Evolution Modules: the six hazard variants. */
    public static final ModuleFamily<HazardModuleData> HAZARD_EVOLUTION = new ModuleFamily<HazardModuleData>(
            "hazard_evolution_module",
            () -> ModItems.HAZARD_EVOLUTION_MODULE.get(),
            () -> ModDataComponentTypes.HAZARD_MODULE_DATA.get(),
            createHazardVariants(),
            Objects::equals);

    /** Hazard Safety Modules: the same six hazard variants, on the hazard frame. */
    public static final ModuleFamily<HazardModuleData> HAZARD_SAFETY = new ModuleFamily<HazardModuleData>(
            "hazard_safety_module",
            () -> ModItems.HAZARD_SAFETY_MODULE.get(),
            () -> ModDataComponentTypes.HAZARD_MODULE_DATA.get(),
            createHazardVariants(),
            Objects::equals);

    /** The six hazard variants as module variants; shared by every hazard module family. */
    private static List<ModuleVariant<HazardModuleData>> createHazardVariants() {
        return Arrays.stream(HazardModuleVariant.values())
                .map(hazard -> new ModuleVariant<>(hazard.getName(), hazard.getName(), hazard.getData()))
                .toList();
    }
}