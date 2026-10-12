package net.scruffy.dermicraft.item.custom;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.custom.tumor.MarredTumorBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The Stitched Tumor item. Marred and Stitched Tumors are one block ({@link MarredTumorBlock}) with a
 * {@code STITCHED} stage property, so this item exists only to pick the stage: it places the same
 * block already stitched, where the plain Marred Tumor item places it open. The stitched stage comes
 * from this item or from stitching an open tumor in the world (a Forceps pickup of a stitched tumor
 * hands this item back, see {@code MarredTumorBlock#getCloneItemStack}).
 *
 * <p>An empty stitched tumor placed this way is decoration: it has no recipe, so the syringe does
 * nothing on it, and cutting its stitches turns it into an ordinary open tumor.
 */
public class StitchedTumorBlockItem extends BlockItem {

    public StitchedTumorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Keeps the base placement checks (including the null "cannot place here" result) and only sets the stage. */
    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(MarredTumorBlock.STITCHED, true);
    }

    @Override
    public String getDescriptionId() {
        return Util.makeDescriptionId("item", BuiltInRegistries.ITEM.getKey(this));
    }

    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {

    }
}
