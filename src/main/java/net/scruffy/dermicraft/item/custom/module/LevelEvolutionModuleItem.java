package net.scruffy.dermicraft.item.custom.module;

import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.component.LevelModuleData;
import net.scruffy.dermicraft.interfaces.IHasLevelModuleData;
import net.scruffy.dermicraft.machine.EvolutionLadder;
import net.scruffy.dermicraft.machine.EvolutionResult;

import java.util.Optional;

/** A Tier or Overgrowth Evolution Module: which ladder it climbs is given at registration, the level is on the stack. */
public class LevelEvolutionModuleItem extends EvolutionModuleItem implements IHasLevelModuleData {

    private final EvolutionLadder LADDER;

    public LevelEvolutionModuleItem(Properties properties, EvolutionLadder ladder) {
        super(properties);
        LADDER = ladder;
    }

    @Override
    public Optional<EvolutionResult> evaluate(EvolutionStateData state, ItemStack stack) {
        LevelModuleData data = getLevelData(stack);
        return data == null ? Optional.empty() : Optional.of(LADDER.evaluate(state, data.level()));
    }
}
