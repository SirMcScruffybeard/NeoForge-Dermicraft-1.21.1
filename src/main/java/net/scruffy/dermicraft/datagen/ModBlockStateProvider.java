package net.scruffy.dermicraft.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.MrFarmerBlock;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Dermicraft.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        String skinTankEnd = "block/skin_tank_end";

        ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
        blockWithItem(ModBlocks.INERT_TUMOR);
        blockWithItem(ModBlocks.MARRED_TUMOR);
        blockWithItem(ModBlocks.STITCHED_TUMOR);

        blockWithItem(ModBlocks.EYE_TUMOR);
        blockWithItem(ModBlocks.MUSCLE_TUMOR);
        blockWithItem(ModBlocks.NERVE_TUMOR);

        blockWithItemWithRenderTypeWithSideAndEnds("skin_tank", ModBlocks.SKIN_TANK, "translucent");

        horizontalBlock(ModBlocks.DROOLING_CAULDRON.get(), models().getExistingFile(ModBlocks.DROOLING_CAULDRON.getId()));


        horizontalBlock(ModBlocks.MASTICATOR.get(), models().cube(
                ModBlocks.MASTICATOR.getId().getPath(),
                modLoc(skinTankEnd),
                modLoc(skinTankEnd),
                modLoc("block/masticator_front"),
                modLoc(skinTankEnd),
                modLoc(skinTankEnd),
                modLoc(skinTankEnd))
                .texture("particle", modLoc("block/masticator_front")));
        itemModels().withExistingParent(ModBlocks.MASTICATOR.getId().getPath(),
                modLoc("block/" + ModBlocks.MASTICATOR.getId().getPath()));

        horizontalBlock(ModBlocks.EFFLUENTCER.get(), models().cube(
                ModBlocks.EFFLUENTCER.getId().getPath(),
                modLoc(skinTankEnd),
                modLoc(skinTankEnd),
                modLoc("block/effluentcer_side"),
                modLoc("block/effluentcer_side"),
                modLoc("block/effluentcer_side"),
                modLoc("block/effluentcer_side"))
                .texture("particle", modLoc("block/effluentcer_side")));
        itemModels().withExistingParent(ModBlocks.EFFLUENTCER.getId().getPath(),
                modLoc("block/" + ModBlocks.EFFLUENTCER.getId().getPath()));

        horizontalBlock(ModBlocks.METASTASIZER.get(), models().cube(
                ModBlocks.METASTASIZER.getId().getPath(),
                modLoc(skinTankEnd),
                modLoc(skinTankEnd),
                modLoc("block/metastasizer_front"),
                modLoc("block/metastasizer_side"),
                modLoc("block/metastasizer_side"),
                modLoc("block/metastasizer_side"))
                .texture("particle", modLoc("block/metastasizer_front")));
        itemModels().withExistingParent(ModBlocks.METASTASIZER.getId().getPath(),
                modLoc("block/" + ModBlocks.METASTASIZER.getId().getPath()));

        simpleBlockWithItem(ModBlocks.CRAW.get(), models().cubeBottomTop(
                ModBlocks.CRAW.getId().getPath(),
                modLoc("block/craw_side"),
                modLoc(skinTankEnd),
                modLoc(skinTankEnd)));

        simpleBlock(ModBlocks.BEAKER.get(), models().cubeBottomTop(
                ModBlocks.BEAKER.getId().getPath(),
                modLoc("block/beaker/beaker_side"),
                modLoc("block/beaker/beaker_bottom"),
                modLoc("block/beaker/beaker_top"))
                .renderType("translucent"));

        mrFarmerBlockState();
        itemModels().getBuilder(ModBlocks.MR_FARMER.getId().getPath())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("item/mr_farmer_icon"));

        ////////////////////Innards Gate\\\\\\\\\\\\\\\\\\\\
        simpleBlockWithItem(ModBlocks.INNARDS_GATE_CONTROLLER.get(), models().cubeAll("innards_gate_controller",
                modLoc("block/innards_gate/innards_gate_controller")));
        simpleBlockWithItem(ModBlocks.INNARDS_GATE_BUFFER.get(), models().cubeAll("innards_gate_buffer",
                modLoc("block/innards_gate/innards_gate_buffer")));
        simpleBlockWithItem(ModBlocks.INNARDS_GATE_PORT.get(), models().cubeAll("innards_gate_port",
                modLoc("block/innards_gate/innards_gate_port")));

    }

    // Explicit 6-way facing so we control rotation exactly (the generic directionalBlock helper uses the
    // vanilla dispenser convention, which doesn't match Mr. Farmer's model: hat authored on +Y, face on north).
    // Intent: the FACE points in the FACING direction (the direction range extends from the mounted surface).
    // Model faces north by default, so the four horizontal mounts stay upright and just spin around Y
    // (same convention as a vanilla furnace); only up/down tip the head to look up or down.
    private void mrFarmerBlockState() {
        ModelFile off = models().getExistingFile(ModBlocks.MR_FARMER.getId());
        ModelFile on = models().getExistingFile(modLoc("block/mr_farmer_on"));
        var builder = getVariantBuilder(ModBlocks.MR_FARMER.get());

        putFarmerVariant(builder, off, on, Direction.UP, 270, 0);     // floor mount: face tips up
        putFarmerVariant(builder, off, on, Direction.DOWN, 90, 0);    // ceiling mount: face tips down
        putFarmerVariant(builder, off, on, Direction.NORTH, 0, 0);    // upright, face north (identity)
        putFarmerVariant(builder, off, on, Direction.SOUTH, 0, 180);  // upright, face south
        putFarmerVariant(builder, off, on, Direction.EAST, 0, 90);    // upright, face east
        putFarmerVariant(builder, off, on, Direction.WEST, 0, 270);   // upright, face west
    }

    private void putFarmerVariant(net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder builder,
                                  ModelFile off, ModelFile on, Direction facing, int rotX, int rotY) {
        builder.partialState().with(MrFarmerBlock.FACING, facing).with(MrFarmerBlock.ACTIVE, false)
                .modelForState().modelFile(off).rotationX(rotX).rotationY(rotY).addModel();
        builder.partialState().with(MrFarmerBlock.FACING, facing).with(MrFarmerBlock.ACTIVE, true)
                .modelForState().modelFile(on).rotationX(rotX).rotationY(rotY).addModel();
    }

    private void blockWithItem(DeferredBlock<Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), models().cubeAll(
                deferredBlock.getId().getPath(),
                modLoc("block/tumor/" + deferredBlock.getId().getPath())));
    }

    private void blockWithItemWithRenderTypeWithSideAndEnds(String name, DeferredBlock<Block> block, String renderType) {
        simpleBlockWithItem(block.get(), models().cubeBottomTop(block.getId().getPath(),
                        modLoc("block/" + name + "_side"),
                        modLoc("block/" + name + "_end"),
                        modLoc("block/" + name + "_end"))
                .renderType(renderType));
    }


}
