package net.scruffy.dermicraft.block.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * The block-state mirror of a machine's evolution state, so models can read it: multipart blockstates pick the tier look
 * of the parts from {@link #TIER} and the plate from {@link #OVERGROWTH} (see "Visuals" in the machine notes). Hazards
 * are deliberately not mirrored (decided 2026-11-08): no hazard accents are drawn, so a hazard never changes how a
 * machine looks, and leaving the four hazard properties out keeps a face machine at a couple of hundred block states
 * instead of several thousand. The machine's block entity is the source of truth; these properties are written from it by
 * {@code MachineBaseBlockEntity} whenever the evolution state changes or a machine loads, so nothing else ever sets them.
 *
 * <p>A block opts in from its {@code createBlockStateDefinition} with {@link #add} and gives its default state the
 * defaults with {@link #withDefaults}; a block without the properties is simply never written to.
 */
public final class EvolutionBlockProperties {

    private EvolutionBlockProperties() {}

    /** The machine's tier, 1 to 4. */
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 4);
    /** A small stage derived from the Overgrowth level (0 = none), see {@link #getOvergrowthStage}. It changes the
     * plate, and the first stage starts at O1, since reaching Overgrowth is meant to be a big visible moment. */
    public static final IntegerProperty OVERGROWTH = IntegerProperty.create("overgrowth", 0, 3);

    /** The item-model predicates that mirror {@link #TIER} and {@link #OVERGROWTH} for a machine's item (read from its
     * EVOLUTION_STATE component, registered client-side in {@code ModItemProperties}), so an inventory icon can show
     * the same look as the placed block. */
    public static final ResourceLocation ITEM_TIER = ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "tier");
    public static final ResourceLocation ITEM_OVERGROWTH = ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "overgrowth");

    /** The Overgrowth level where each further stage begins: stage 1 from O1, stage 2 from O10, stage 3 from O20
     * (set 2026-11-14; the 45 cap and the cost curve still wait for the rescaling overhaul). */
    private static final int[] OVERGROWTH_STAGE_STARTS = {1, 10, 20};

    /** The Overgrowth stage (0 to 3) a level falls in. */
    public static int getOvergrowthStage(int overgrowthLevel) {
        int stage = 0;
        for (int start : OVERGROWTH_STAGE_STARTS) {
            if (overgrowthLevel >= start) stage++;
        }
        return stage;
    }

    /** Adds the properties to a block's state definition. */
    public static void add(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER, OVERGROWTH);
    }

    /** {@code state} with the properties this block has set to a plain L1 machine's values. */
    public static BlockState withDefaults(BlockState state) {
        return apply(state, EvolutionStateData.DEFAULT);
    }

    /** {@code state} with each evolution property the block has set from {@code evolution}; a property the block does
     * not have is skipped. Returns the same instance when nothing changes. */
    public static BlockState apply(BlockState state, EvolutionStateData evolution) {
        state = set(state, TIER, evolution.tier());
        state = set(state, OVERGROWTH, getOvergrowthStage(evolution.overgrowth()));
        return state;
    }

    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> property, T value) {
        return state.hasProperty(property) && !state.getValue(property).equals(value) ? state.setValue(property, value) : state;
    }
}
