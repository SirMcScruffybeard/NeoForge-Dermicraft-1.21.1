package net.scruffy.dermicraft.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.datagen.tag.ModTags;

public class ToolUtil {

    public static boolean isCollectionTool(ItemStack stack) {
        return stack.is(ModTags.Items.COLLECTION_TOOLS);
    }

    public static boolean isCollectable(BlockState state) {
        return state.is(ModTags.Blocks.COLLECTIBLE);
    }


    public static boolean isExtractionTool(ItemStack stack) {
        return stack.is(ModTags.Items.EXTRACTION_TOOLS);
    }

    public static boolean isExtractable(BlockState state) {
        return state.is(ModTags.Blocks.EXTRACTABLE);
    }


    public static boolean isInjectionTool(ItemStack stack) {
        return stack.is(ModTags.Items.INJECTION_TOOLS);
    }

    public static boolean isInjectable(BlockState state) {
        return state.is(ModTags.Blocks.INJECTABLE);
    }

    public static boolean isSutureTool(ItemStack stack) {
        return stack.is(ModTags.Items.SUTURE_TOOLS);
    }

    public static boolean isSutable(BlockState state) {
        return state.is(ModTags.Blocks.SUTABLE);
    }


}
