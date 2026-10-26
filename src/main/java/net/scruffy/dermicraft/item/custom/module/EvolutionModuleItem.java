package net.scruffy.dermicraft.item.custom.module;

import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.machine.EvolutionResult;

import java.util.Optional;

/**
 * An item that fits a machine's Evolution slot. As itself it is the Evolution Module Frame: it fits the slot
 * and does nothing. The loaded modules extend it and override {@link #evaluate}.
 */
public class EvolutionModuleItem extends ModuleItem {
    public EvolutionModuleItem(Properties properties) {
        super(properties);
    }

    /**
     * What this module would do on a machine in {@code state}: an upgrade that will apply, or why it will not.
     * Empty means inert, which is all the Frame does, and what a loaded module without its component does.
     * Override this when adding a new kind of Evolution Module; forgetting to makes it silently inert.
     */
    public Optional<EvolutionResult> evaluate(EvolutionStateData state, ItemStack stack) {
        return Optional.empty();
    }
}