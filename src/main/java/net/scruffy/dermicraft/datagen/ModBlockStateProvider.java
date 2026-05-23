package net.scruffy.dermicraft.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Dermicraft.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
        blockWithItem(ModBlocks.INERT_TUMOR);
        blockWithItem(ModBlocks.MARRED_TUMOR);
        blockWithItem(ModBlocks.STITCHED_TUMOR);

        blockWithItem(ModBlocks.EYE_TUMOR);
        blockWithItem(ModBlocks.MUSCLE_TUMOR);
        blockWithItem(ModBlocks.NERVE_TUMOR);

        blockWithItemWithRenderTypeWithSideAndEnds("skin_tank", ModBlocks.SKIN_TANK, "translucent");

        horizontalBlock(ModBlocks.DROOLING_CAULDRON.get(), models().getExistingFile(ModBlocks.DROOLING_CAULDRON.getId()));
    }

    private void blockWithItem(DeferredBlock<Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockWithItemWithRenderTypeWithSideAndEnds(String name, DeferredBlock<Block> block, String renderType) {
        simpleBlockWithItem(block.get(), models().cubeBottomTop(block.getId().getPath(),
                        modLoc("block/" + name + "_side"),
                        modLoc("block/" + name + "_end"),
                        modLoc("block/" + name + "_end"))
                .renderType(renderType));
    }


}
