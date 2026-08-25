package net.scruffy.dermicraft.datagen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.item.ModItems;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {

        dropSelf(ModBlocks.CALCIUM_GLASS.get());
        dropSelf(ModBlocks.CHARRED_TUMOR.get());
        dropSelf(ModBlocks.HOT_BONE.get());
        dropSelf(ModBlocks.OUTERFACE.get());
        dropSelf(ModBlocks.BRAIN.get());

        // Ducts and Nodes are the exception to the mod's "destroyed on break" rule -- they drop
        // themselves so players aren't punished for pipe re-fiddling (they keep Forceps pickup too).
        dropSelf(ModBlocks.INNARDS_DUCT.get());
        dropSelf(ModBlocks.CHARRED_INNARDS_DUCT.get());
        dropSelf(ModBlocks.INNARDS_NODE.get());
        dropSelf(ModBlocks.CHARRED_INNARDS_NODE.get());

        // Gate blocks follow the same duct/node exception -- automation infrastructure that gets
        // re-fiddled, so it drops itself on a normal break (and keeps Forceps pickup via COLLECTIBLE).
        dropSelf(ModBlocks.INNARDS_GATE_CONTROLLER.get());
        dropSelf(ModBlocks.INNARDS_GATE_BUFFER.get());
        dropSelf(ModBlocks.INNARDS_GATE_PORT.get());

        // Lab Floor blocks always drop themselves -- they're structural infrastructure, same
        // treatment as ducts/nodes/gates, not a "destroyed on break" machine.
        dropSelf(ModBlocks.STONE_LAB_FLOOR.get());
        dropSelf(ModBlocks.COBBLESTONE_LAB_FLOOR.get());
        dropSelf(ModBlocks.DEEPSLATE_LAB_FLOOR.get());
        dropSelf(ModBlocks.COBBLED_DEEPSLATE_LAB_FLOOR.get());
        dropSelf(ModBlocks.DIORITE_LAB_FLOOR.get());
        dropSelf(ModBlocks.ANDESITE_LAB_FLOOR.get());
        dropSelf(ModBlocks.GRANITE_LAB_FLOOR.get());

        // Tier 1 tumor harvest tables -- 2-4 rolls per harvest, each roll landing on the tumor's
        // real part(s) most of the time (5:1 weight) or Rotten Flesh otherwise (~1 in 6 rolls).
        // Inert splits its 3 possible parts evenly (weight 5 each) against the same ~1-in-6 flesh
        // odds (weight 3 against 15). See TumorBlock.harvest(), which now just pulls this table.
        tumorHarvestTable(ModBlocks.EYE_TUMOR.get(), ModItems.EYE.get());
        tumorHarvestTable(ModBlocks.MUSCLE_TUMOR.get(), ModItems.DENSE_MUSCLE.get());
        tumorHarvestTable(ModBlocks.NERVE_TUMOR.get(), ModItems.NERVE_CLUSTER.get());

        add(ModBlocks.INERT_TUMOR.get(), LootTable.lootTable().withPool(
                LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2, 5))
                        .add(LootItem.lootTableItem(ModItems.DENSE_MUSCLE.get()).setWeight(5))
                        .add(LootItem.lootTableItem(ModItems.NERVE_CLUSTER.get()).setWeight(5))
                        .add(LootItem.lootTableItem(ModItems.EYE.get()).setWeight(5))
                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(3))
        ));
    }

    private void tumorHarvestTable(Block block, Item realPart) {
        add(block, LootTable.lootTable().withPool(
                LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2, 5))
                        .add(LootItem.lootTableItem(realPart).setWeight(5))
                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(1))
        ));
    }

    protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(pBlock, this.applyExplosionDecay(pBlock,
                LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
