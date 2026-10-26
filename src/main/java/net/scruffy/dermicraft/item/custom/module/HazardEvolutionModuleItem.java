package net.scruffy.dermicraft.item.custom.module;

import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.interfaces.IHasHazardModuleData;
import net.scruffy.dermicraft.machine.EvolutionResult;
import net.scruffy.dermicraft.machine.EvolutionRules;

import java.util.Optional;

/** A hazard Evolution Module: permanently grants its hazard family and level when the evolution completes. */
public class HazardEvolutionModuleItem extends EvolutionModuleItem implements IHasHazardModuleData {

    public HazardEvolutionModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public Optional<EvolutionResult> evaluate(EvolutionStateData state, ItemStack stack) {
        HazardModuleData data = getHazardData(stack);
        return data == null ? Optional.empty()
                : Optional.of(EvolutionRules.evaluateHazard(state, data.family(), data.level()));
    }
}