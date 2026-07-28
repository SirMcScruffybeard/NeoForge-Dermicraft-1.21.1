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
import net.scruffy.dermicraft.block.custom.EffluentcerBlock;
import net.scruffy.dermicraft.block.custom.EffluentcerVisualState;
import net.scruffy.dermicraft.block.custom.MasticatorBlock;
import net.scruffy.dermicraft.block.custom.MasticatorVisualState;
import net.scruffy.dermicraft.block.custom.MrFarmerBlock;
import net.scruffy.dermicraft.block.custom.MrShepardBlock;
import net.scruffy.dermicraft.block.custom.MutatorBlock;
import net.scruffy.dermicraft.block.custom.MutatorVisualState;
import net.scruffy.dermicraft.block.custom.RenderFurnaceBlock;
import net.scruffy.dermicraft.block.custom.RenderKilnBlock;
import net.scruffy.dermicraft.block.custom.RenderKilnVisualState;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Dermicraft.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        String skinTankEnd = "block/skin_tank_end";

        ////////////////////Flesh Lab Floor\\\\\\\\\\\\\\\\\\\\
        simpleBlockWithItem(ModBlocks.STONE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/stone_lab_floor")));
        simpleBlockWithItem(ModBlocks.COBBLESTONE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/cobblestone_lab_floor")));
        simpleBlockWithItem(ModBlocks.DEEPSLATE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/deepslate_lab_floor")));
        simpleBlockWithItem(ModBlocks.COBBLED_DEEPSLATE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/cobbled_deepslate_lab_floor")));
        simpleBlockWithItem(ModBlocks.DIORITE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/diorite_lab_floor")));
        simpleBlockWithItem(ModBlocks.ANDESITE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/andesite_lab_floor")));
        simpleBlockWithItem(ModBlocks.GRANITE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/granite_lab_floor")));

        ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
        blockWithItem(ModBlocks.INERT_TUMOR);
        blockWithItem(ModBlocks.MARRED_TUMOR);
        blockWithItem(ModBlocks.STITCHED_TUMOR);

        blockWithItem(ModBlocks.EYE_TUMOR);
        blockWithItem(ModBlocks.MUSCLE_TUMOR);
        blockWithItem(ModBlocks.NERVE_TUMOR);

        blockWithItemWithRenderTypeWithSideAndEnds("skin_tank", ModBlocks.SKIN_TANK, "translucent");

        horizontalBlock(ModBlocks.DROOLING_CAULDRON.get(), models().getExistingFile(ModBlocks.DROOLING_CAULDRON.getId()));


        masticatorBlockState(skinTankEnd);
        itemModels().withExistingParent(ModBlocks.MASTICATOR.getId().getPath(),
                modLoc("block/" + ModBlocks.MASTICATOR.getId().getPath()));

        effluentcerBlockState(skinTankEnd);
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

        mutatorBlockState(skinTankEnd);
        itemModels().withExistingParent(ModBlocks.MUTATOR.getId().getPath(),
                modLoc("block/" + ModBlocks.MUTATOR.getId().getPath()));

        renderFurnaceBlockState(skinTankEnd);
        itemModels().withExistingParent(ModBlocks.RENDER_FURNACE.getId().getPath(),
                modLoc("block/" + ModBlocks.RENDER_FURNACE.getId().getPath()));

        // No FACING, no ACTIVE state -- only a top texture exists for this one so far, sides/bottom
        // reuse skin_tank_end like every other machine's placeholder faces.
        simpleBlockWithItem(ModBlocks.GRAFTING_TABLE.get(), models().cubeBottomTop(
                ModBlocks.GRAFTING_TABLE.getId().getPath(),
                modLoc(skinTankEnd),
                modLoc(skinTankEnd),
                modLoc("block/grafting_table_top")));

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

        renderKilnBlockState(skinTankEnd);
        itemModels().withExistingParent(ModBlocks.RENDER_KILN.getId().getPath(),
                modLoc("block/" + ModBlocks.RENDER_KILN.getId().getPath()));

        mrFarmerBlockState();
        itemModels().getBuilder(ModBlocks.MR_FARMER.getId().getPath())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("item/mr_farmer_icon"));

        mrShepardBlockState();
        itemModels().withExistingParent(ModBlocks.MR_SHEPARD.getId().getPath(),
                modLoc("block/" + ModBlocks.MR_SHEPARD.getId().getPath()));

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

    // Mirrors mrFarmerBlockState() exactly -- same 6-way facing convention, same on/off swap.
    private void mrShepardBlockState() {
        ModelFile off = models().getExistingFile(ModBlocks.MR_SHEPARD.getId());
        ModelFile on = models().getExistingFile(modLoc("block/mr_shepard_on"));
        var builder = getVariantBuilder(ModBlocks.MR_SHEPARD.get());

        putShepardVariant(builder, off, on, Direction.UP, 270, 0);
        putShepardVariant(builder, off, on, Direction.DOWN, 90, 0);
        putShepardVariant(builder, off, on, Direction.NORTH, 0, 0);
        putShepardVariant(builder, off, on, Direction.SOUTH, 0, 180);
        putShepardVariant(builder, off, on, Direction.EAST, 0, 90);
        putShepardVariant(builder, off, on, Direction.WEST, 0, 270);
    }

    private void putShepardVariant(net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder builder,
                                   ModelFile off, ModelFile on, Direction facing, int rotX, int rotY) {
        builder.partialState().with(MrShepardBlock.FACING, facing).with(MrShepardBlock.ACTIVE, false)
                .modelForState().modelFile(off).rotationX(rotX).rotationY(rotY).addModel();
        builder.partialState().with(MrShepardBlock.FACING, facing).with(MrShepardBlock.ACTIVE, true)
                .modelForState().modelFile(on).rotationX(rotX).rotationY(rotY).addModel();
    }

    // 4 horizontal facings x 3 MutatorVisualState values -- only the front (north-authored) face
    // texture differs between the three models; skin_tank_end covers every other face in all three.
    // Y-rotation per facing matches the standard horizontal-facing convention (same values horizontalBlock
    // itself would generate): North=identity, East=90, South=180, West=270.
    private void mutatorBlockState(String skinTankEnd) {
        ModelFile idle = models().cube(ModBlocks.MUTATOR.getId().getPath(),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/mutator_face"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/mutator_face"));
        ModelFile running = models().cube("mutator_on",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/mutator_face_on"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/mutator_face_on"));
        ModelFile recovering = models().cube("mutator_error",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/mutator_face_error"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/mutator_face_error"));

        var builder = getVariantBuilder(ModBlocks.MUTATOR.get());
        putMutatorVariant(builder, idle, running, recovering, Direction.NORTH, 0);
        putMutatorVariant(builder, idle, running, recovering, Direction.EAST, 90);
        putMutatorVariant(builder, idle, running, recovering, Direction.SOUTH, 180);
        putMutatorVariant(builder, idle, running, recovering, Direction.WEST, 270);
    }

    private void putMutatorVariant(net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder builder,
                                   ModelFile idle, ModelFile running, ModelFile recovering, Direction facing, int rotY) {
        builder.partialState().with(MutatorBlock.FACING, facing).with(MutatorBlock.STATE, MutatorVisualState.IDLE)
                .modelForState().modelFile(idle).rotationY(rotY).addModel();
        builder.partialState().with(MutatorBlock.FACING, facing).with(MutatorBlock.STATE, MutatorVisualState.RUNNING)
                .modelForState().modelFile(running).rotationY(rotY).addModel();
        builder.partialState().with(MutatorBlock.FACING, facing).with(MutatorBlock.STATE, MutatorVisualState.RECOVERING)
                .modelForState().modelFile(recovering).rotationY(rotY).addModel();
    }

    // Same shape as mutatorBlockState, but only 2 models (no HP mechanic here -- see MachineTier.NO_HEALTH
    // -- so no "recovering" state) using a plain BooleanProperty ACTIVE, same idiom as Mr. Farmer's.
    private void renderFurnaceBlockState(String skinTankEnd) {
        ModelFile off = models().cube(ModBlocks.RENDER_FURNACE.getId().getPath(),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/render_furnace_face"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/render_furnace_face"));
        ModelFile on = models().cube("render_furnace_on",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/render_furnace_face_on"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/render_furnace_face_on"));

        var builder = getVariantBuilder(ModBlocks.RENDER_FURNACE.get());
        putRenderFurnaceVariant(builder, off, on, Direction.NORTH, 0);
        putRenderFurnaceVariant(builder, off, on, Direction.EAST, 90);
        putRenderFurnaceVariant(builder, off, on, Direction.SOUTH, 180);
        putRenderFurnaceVariant(builder, off, on, Direction.WEST, 270);
    }

    private void putRenderFurnaceVariant(net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder builder,
                                         ModelFile off, ModelFile on, Direction facing, int rotY) {
        builder.partialState().with(RenderFurnaceBlock.FACING, facing).with(RenderFurnaceBlock.ACTIVE, false)
                .modelForState().modelFile(off).rotationY(rotY).addModel();
        builder.partialState().with(RenderFurnaceBlock.FACING, facing).with(RenderFurnaceBlock.ACTIVE, true)
                .modelForState().modelFile(on).rotationY(rotY).addModel();
    }

    // Same shape as mutatorBlockState -- 4 horizontal facings x 3 RenderKilnVisualState values, only
    // the front (north-authored) face texture differs between the three models; skin_tank_end covers
    // every other face in all three.
    private void renderKilnBlockState(String skinTankEnd) {
        ModelFile idle = models().cube(ModBlocks.RENDER_KILN.getId().getPath(),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/render_kiln_face"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/render_kiln_face"));
        ModelFile running = models().cube("render_kiln_on",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/render_kiln_face_on"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/render_kiln_face_on"));
        ModelFile recovering = models().cube("render_kiln_error",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/render_kiln_face_error"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/render_kiln_face_error"));

        var builder = getVariantBuilder(ModBlocks.RENDER_KILN.get());
        putRenderKilnVariant(builder, idle, running, recovering, Direction.NORTH, 0);
        putRenderKilnVariant(builder, idle, running, recovering, Direction.EAST, 90);
        putRenderKilnVariant(builder, idle, running, recovering, Direction.SOUTH, 180);
        putRenderKilnVariant(builder, idle, running, recovering, Direction.WEST, 270);
    }

    private void putRenderKilnVariant(net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder builder,
                                      ModelFile idle, ModelFile running, ModelFile recovering, Direction facing, int rotY) {
        builder.partialState().with(RenderKilnBlock.FACING, facing).with(RenderKilnBlock.STATE, RenderKilnVisualState.IDLE)
                .modelForState().modelFile(idle).rotationY(rotY).addModel();
        builder.partialState().with(RenderKilnBlock.FACING, facing).with(RenderKilnBlock.STATE, RenderKilnVisualState.RUNNING)
                .modelForState().modelFile(running).rotationY(rotY).addModel();
        builder.partialState().with(RenderKilnBlock.FACING, facing).with(RenderKilnBlock.STATE, RenderKilnVisualState.RECOVERING)
                .modelForState().modelFile(recovering).rotationY(rotY).addModel();
    }

    // Mirrors mutatorBlockState -- 4 horizontal facings x 3 MasticatorVisualState values, only the
    // front (north-authored) face texture differs between the three models; skin_tank_end covers
    // every other face in all three. masticator_face_on is a 2-frame animated texture (see its
    // .png.mcmeta) so the RUNNING model visibly pulses while the machine is actively crafting.
    private void masticatorBlockState(String skinTankEnd) {
        ModelFile idle = models().cube(ModBlocks.MASTICATOR.getId().getPath(),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/masticator_face"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/masticator_face"));
        ModelFile running = models().cube("masticator_on",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/masticator_face_on"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/masticator_face_on"));
        ModelFile recovering = models().cube("masticator_error",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/masticator_face_error"),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc(skinTankEnd))
                .texture("particle", modLoc("block/masticator_face_error"));

        var builder = getVariantBuilder(ModBlocks.MASTICATOR.get());
        putMasticatorVariant(builder, idle, running, recovering, Direction.NORTH, 0);
        putMasticatorVariant(builder, idle, running, recovering, Direction.EAST, 90);
        putMasticatorVariant(builder, idle, running, recovering, Direction.SOUTH, 180);
        putMasticatorVariant(builder, idle, running, recovering, Direction.WEST, 270);
    }

    private void putMasticatorVariant(net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder builder,
                                   ModelFile idle, ModelFile running, ModelFile recovering, Direction facing, int rotY) {
        builder.partialState().with(MasticatorBlock.FACING, facing).with(MasticatorBlock.STATE, MasticatorVisualState.IDLE)
                .modelForState().modelFile(idle).rotationY(rotY).addModel();
        builder.partialState().with(MasticatorBlock.FACING, facing).with(MasticatorBlock.STATE, MasticatorVisualState.RUNNING)
                .modelForState().modelFile(running).rotationY(rotY).addModel();
        builder.partialState().with(MasticatorBlock.FACING, facing).with(MasticatorBlock.STATE, MasticatorVisualState.RECOVERING)
                .modelForState().modelFile(recovering).rotationY(rotY).addModel();
    }

    // Mirrors masticatorBlockState/mutatorBlockState -- 4 horizontal facings x 3
    // EffluentcerVisualState values, only the front (north-authored) face texture differs between
    // the three models; effluentcer_side covers the other 3 horizontal faces and skinTankEnd covers
    // top/bottom in all three.
    private void effluentcerBlockState(String skinTankEnd) {
        ModelFile idle = models().cube(ModBlocks.EFFLUENTCER.getId().getPath(),
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/effluentcer_face"),
                        modLoc("block/effluentcer_side"), modLoc("block/effluentcer_side"), modLoc("block/effluentcer_side"))
                .texture("particle", modLoc("block/effluentcer_face"));
        ModelFile running = models().cube("effluentcer_on",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/effluentcer_face_on"),
                        modLoc("block/effluentcer_side"), modLoc("block/effluentcer_side"), modLoc("block/effluentcer_side"))
                .texture("particle", modLoc("block/effluentcer_face_on"));
        ModelFile recovering = models().cube("effluentcer_error",
                        modLoc(skinTankEnd), modLoc(skinTankEnd), modLoc("block/effluentcer_face_error"),
                        modLoc("block/effluentcer_side"), modLoc("block/effluentcer_side"), modLoc("block/effluentcer_side"))
                .texture("particle", modLoc("block/effluentcer_face_error"));

        var builder = getVariantBuilder(ModBlocks.EFFLUENTCER.get());
        putEffluentcerVariant(builder, idle, running, recovering, Direction.NORTH, 0);
        putEffluentcerVariant(builder, idle, running, recovering, Direction.EAST, 90);
        putEffluentcerVariant(builder, idle, running, recovering, Direction.SOUTH, 180);
        putEffluentcerVariant(builder, idle, running, recovering, Direction.WEST, 270);
    }

    private void putEffluentcerVariant(net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder builder,
                                   ModelFile idle, ModelFile running, ModelFile recovering, Direction facing, int rotY) {
        builder.partialState().with(EffluentcerBlock.FACING, facing).with(EffluentcerBlock.STATE, EffluentcerVisualState.IDLE)
                .modelForState().modelFile(idle).rotationY(rotY).addModel();
        builder.partialState().with(EffluentcerBlock.FACING, facing).with(EffluentcerBlock.STATE, EffluentcerVisualState.RUNNING)
                .modelForState().modelFile(running).rotationY(rotY).addModel();
        builder.partialState().with(EffluentcerBlock.FACING, facing).with(EffluentcerBlock.STATE, EffluentcerVisualState.RECOVERING)
                .modelForState().modelFile(recovering).rotationY(rotY).addModel();
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
