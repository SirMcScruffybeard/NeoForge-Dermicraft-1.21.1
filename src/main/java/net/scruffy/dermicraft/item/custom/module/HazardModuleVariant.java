package net.scruffy.dermicraft.item.custom.module;

import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.hazard.HazardFamily;

public enum HazardModuleVariant {
    THERMAL("thermal", HazardFamily.THERMAL, 1),
    RADIATION_MILD("radiation_mild", HazardFamily.RADIATION, 1),
    RADIATION_SEVERE("radiation_severe", HazardFamily.RADIATION, 2),
    BIOHAZARD("biohazard", HazardFamily.BIOHAZARD, 1),
    METAPHYSICAL_MILD("metaphysical_mild", HazardFamily.METAPHYSICAL, 1),
    METAPHYSICAL_SEVERE("metaphysical_severe", HazardFamily.METAPHYSICAL, 2);

    private final String NAME;
    private final HazardModuleData DATA;

    HazardModuleVariant(String name, HazardFamily family, int level) {
        NAME = name;
        DATA = new HazardModuleData(family, level);
    }

    public String getName() {
        return NAME;
    }

    public HazardModuleData getData() {
        return DATA;
    }
}
