package net.scruffy.dermicraft.interfaces;

import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.hazard.HazardProfile;

/**
 * The wiring for a module that grants temporary hazard tolerance while it sits in a general Module slot. A machine
 * or gadget asks each installed module through {@link IHaveModules#installedHazardProfile}, which checks for this
 * interface, so the module states what it is and the reading code does not need to know the concrete item class.
 * It extends {@link IHasHazardModuleData} because the grant comes from the stack's hazard data; a hazard Evolution
 * Module carries the same data but does not implement this interface, so it grants nothing in a general slot.
 */
public interface IGrantTempHazardSafety extends IHasHazardModuleData {
    /**
     * {@code base} plus the tolerance this module grants: its hazard family up to its level (a Severe level
     * covers Mild too). A bare module with no data grants nothing. Never removes tolerance, only adds, the same
     * "union, never reset" rule as every Safety Module reader.
     */
    default HazardProfile addTolerance(HazardProfile base, ItemStack stack) {
        HazardModuleData data = getHazardData(stack);
        return data == null ? base : base.plusFamily(data.family(), data.level());
    }
}
