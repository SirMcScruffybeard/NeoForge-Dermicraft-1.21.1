package net.scruffy.dermicraft.item.custom.module;

/**
 * The Safety Module Frame: the concrete base of the Safety Module branch, the counterpart of
 * {@link EvolutionModuleItem}. On its own it is an inert intermediate (it grants nothing); the kinds of Safety
 * Module extend it, such as {@link HazardSafetyModuleItem}.
 */
public class SafetyModuleItem extends ModuleItem {
    public SafetyModuleItem(Properties properties) {
        super(properties);
    }
}
