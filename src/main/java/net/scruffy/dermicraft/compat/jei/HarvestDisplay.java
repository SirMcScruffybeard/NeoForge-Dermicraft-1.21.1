package net.scruffy.dermicraft.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One "Scalpel harvests part(s) from tumor" entry for JEI. Tumor harvesting is loot-table driven
 * (see {@code ModBlockLootTableProvider}), but loot tables aren't synced to the client the way
 * recipes are, so this list is hand-authored in {@link DermicraftJeiPlugin} to mirror those pools.
 */
public record HarvestDisplay(ItemStack tumor, List<ItemStack> possibleParts) {
}
