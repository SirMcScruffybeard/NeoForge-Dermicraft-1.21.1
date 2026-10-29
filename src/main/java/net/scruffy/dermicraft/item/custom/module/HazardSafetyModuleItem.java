package net.scruffy.dermicraft.item.custom.module;

import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IHasHazardModuleData;

/**
 * A Safety Module: while installed in a general Module slot it grants temporary tolerance to its hazard family at
 * its level (a Severe level covers Mild too), on top of whatever the machine or gadget already tolerates. The
 * hazard data is the same {@link net.scruffy.dermicraft.component.HazardModuleData} the hazard Evolution Module
 * carries; what the grant does comes from this item class, so the reading code checks for it.
 */
public class HazardSafetyModuleItem extends ModuleItem implements IHasHazardModuleData {
    public HazardSafetyModuleItem(Properties properties) {
        super(properties);
    }

    /**
     * {@code base} plus the tolerance this module grants: its hazard family up to its level (a Severe level
     * covers Mild too). A bare module with no data grants nothing. Never removes tolerance, only adds, the same
     * "union, never reset" rule as every Safety Module reader.
     */
    public HazardProfile addTolerance(HazardProfile base, ItemStack stack) {
        HazardModuleData data = getHazardData(stack);
        return data == null ? base : base.plusFamily(data.family(), data.level());
    }
}
