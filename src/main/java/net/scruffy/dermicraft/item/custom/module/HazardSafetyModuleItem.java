package net.scruffy.dermicraft.item.custom.module;

import net.scruffy.dermicraft.interfaces.IGrantTempHazardSafety;

/**
 * A Safety Module: while installed in a general Module slot it grants temporary tolerance to its hazard family at
 * its level (a Severe level covers Mild too), on top of whatever the machine or gadget already tolerates. The
 * hazard data is the same {@link net.scruffy.dermicraft.component.HazardModuleData} the hazard Evolution Module
 * is given (one data map entry per item); the grant itself is {@link IGrantTempHazardSafety}, which the Evolution Module does not implement.
 */
public class HazardSafetyModuleItem extends SafetyModuleItem implements IGrantTempHazardSafety {
    public HazardSafetyModuleItem(Properties properties) {
        super(properties);
    }
}
