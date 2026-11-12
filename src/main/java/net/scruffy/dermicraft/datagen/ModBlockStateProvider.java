package net.scruffy.dermicraft.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.*;
import net.scruffy.dermicraft.block.custom.tumor.MarredTumorBlock;
import net.scruffy.dermicraft.datagen.machine.MachineBlockStates;
import net.scruffy.dermicraft.datagen.machine.CrawBlockStates;
import net.scruffy.dermicraft.datagen.machine.MachineLayouts;
import net.scruffy.dermicraft.datagen.machine.TankBlockStates;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Dermicraft.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {


        horizontalBlock(ModBlocks.BRAIN.get(), models().getExistingFile(modLoc("block/brain")));
        itemModels().withExistingParent(ModBlocks.BRAIN.getId().getPath(),
                modLoc("block/" + ModBlocks.BRAIN.getId().getPath()));

        // GeckoLib-rendered (RenderShape.ENTITYBLOCK_ANIMATED) -- no baked mesh of their own, the
        // block entity's own GeoBlockRenderer draws everything every frame. "builtin/entity" is
        // vanilla's own special marker model for exactly this (same one chests/banners/skulls use).
        //
        // Bottom has its own hand-authored model/block/workbench_bottom.json (parent builtin/entity,
        // real display transforms tuned to the actual geometry, plus a particle texture) -- same
        // getExistingFile pattern as BRAIN just above, not the generic vanilla-block-default
        // transforms this used to build inline via the item-model transforms() builder.
        simpleBlockWithItem(ModBlocks.WORKBENCH.get(), models().getExistingFile(modLoc("block/workbench_bottom")));
        // Top only needs a particle texture (its item is never actually handed to players, see
        // ModCreativeModeTabs), but the raw vanilla "builtin/entity" model carries no textures block
        // at all -- without its own hand-authored model/block/workbench_top.json, break particles
        // have nothing to sample and just don't render.
        simpleBlockWithItem(ModBlocks.WORKBENCH_TOP.get(), models().getExistingFile(modLoc("block/workbench_top")));

        ////////////////////Flesh Lab Floor\\\\\\\\\\\\\\\\\\\\
        simpleBlockWithItem(ModBlocks.STONE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/stone_lab_floor")));
        simpleBlockWithItem(ModBlocks.COBBLESTONE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/cobblestone_lab_floor")));
        simpleBlockWithItem(ModBlocks.DEEPSLATE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/deepslate_lab_floor")));
        simpleBlockWithItem(ModBlocks.COBBLED_DEEPSLATE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/cobbled_deepslate_lab_floor")));
        simpleBlockWithItem(ModBlocks.DIORITE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/diorite_lab_floor")));
        simpleBlockWithItem(ModBlocks.ANDESITE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/andesite_lab_floor")));
        simpleBlockWithItem(ModBlocks.GRANITE_LAB_FLOOR.get(), models().getExistingFile(modLoc("block/flesh_lab/granite_lab_floor")));

        ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
        // Tumor overhaul: Inert/Eye/Muscle/Nerve are the shared tumor_base plus a transparent overlay.
        final String TUMOR_DIR = "block/tumor/";
        ResourceLocation base = modLoc(TUMOR_DIR + "tumor_base");
        // Inert is the only tumor with different art per face (concentric ring below, patch on top).
        tumorOverlayCube(ModBlocks.INERT_TUMOR,
                base,
                modLoc(TUMOR_DIR + "inert_top_overlay"),
                modLoc(TUMOR_DIR + "inert_bottom_overlay"),
                modLoc(TUMOR_DIR + "inert_side_overlay"));

        // Eye/Muscle/Nerve use one (animated) overlay on all six faces.
        tumorOverlayCube(ModBlocks.EYE_TUMOR, base, modLoc(TUMOR_DIR + "eye_overlay"));
        tumorOverlayCube(ModBlocks.MUSCLE_TUMOR, base, modLoc(TUMOR_DIR + "muscle_overlay"));
        tumorOverlayCube(ModBlocks.NERVE_TUMOR, base, modLoc(TUMOR_DIR + "nerve_overlay"));

        ResourceLocation marred = modLoc(TUMOR_DIR + "marred_overlay");
        ResourceLocation stitch = modLoc(TUMOR_DIR + "stitch_overlay");

        BlockModelBuilder openModel = tumorLayeredModel("marred_tumor", base, marred);
        BlockModelBuilder stitchedModel = tumorLayeredModel("stitched_tumor", base, marred, stitch);

        getVariantBuilder(ModBlocks.MARRED_TUMOR.get())
                .partialState().with(MarredTumorBlock.STITCHED, false)
                .modelForState().modelFile(openModel).addModel()
                .partialState().with(MarredTumorBlock.STITCHED, true)
                .modelForState().modelFile(stitchedModel).addModel();

        itemModels().withExistingParent("marred_tumor", modLoc("block/marred_tumor"));
        itemModels().withExistingParent("stitched_tumor", modLoc("block/stitched_tumor"));

        // Charred Tumor -- plain Block, not part of the TumorBlock family, so its texture lives at
        // the top-level block/ path rather than block/tumor/.
        simpleBlockWithItem(ModBlocks.CHARRED_TUMOR.get(), models().cubeAll("charred_tumor",
                modLoc("block/charred_tumor")));

        simpleBlockWithItem(ModBlocks.HOT_BONE.get(), models().cubeAll("hot_bone",
                modLoc("block/hot_bone")));

        // The Skin Tank is a multipart: windowed plate, accent and glass on the sides, ports on the top and bottom, and
        // an interior backing; its item model is built alongside (see TankBlockStates).
        new TankBlockStates(this).register(ModBlocks.SKIN_TANK.get(), "skin_tank");

        // Knowledge Vat -- real dedicated art (2026-08-29): front/back share one texture,
        // left/right another, top/bottom a third -- no tintindex any more (that was only ever
        // there to recolor the borrowed Skin Tank placeholder; real per-face art renders as
        // authored, not multiplied by Knowledge Essence's green).
        simpleBlockWithItem(ModBlocks.KNOWLEDGE_VAT.get(),
                models().getBuilder(ModBlocks.KNOWLEDGE_VAT.getId().getPath())
                        .texture("front_back", modLoc("block/knowledge_vat/knowledge_vat_front_back"))
                        .texture("left_right", modLoc("block/knowledge_vat/knowledge_vat_left_right"))
                        .texture("end", modLoc("block/knowledge_vat/knowledge_vat_end"))
                        .texture("particle", modLoc("block/knowledge_vat/knowledge_vat_front_back"))
                        .element()
                                .from(0, 0, 0).to(16, 16, 16)
                                .face(Direction.DOWN).texture("#end").end()
                                .face(Direction.UP).texture("#end").end()
                                .face(Direction.NORTH).texture("#front_back").end()
                                .face(Direction.SOUTH).texture("#front_back").end()
                                .face(Direction.WEST).texture("#left_right").end()
                                .face(Direction.EAST).texture("#left_right").end()
                        .end()
                        .renderType("translucent"));

        droolingBlock(ModBlocks.DROOLING_CAULDRON.get(), models().getExistingFile(ModBlocks.DROOLING_CAULDRON.getId()));
        // New model carries its own display transforms (2026-08-20) -- item model now parents
        // straight to the block model, same convention every other machine already uses (see
        // Masticator below), rather than the old hand-authored flat item/generated icon.
        itemModels().withExistingParent(ModBlocks.DROOLING_CAULDRON.getId().getPath(),
                modLoc("block/" + ModBlocks.DROOLING_CAULDRON.getId().getPath()));

        // Shares Cauldron's geometry via model-level parenting (models/block/drooling_crucible.json
        // parents drooling_cauldron.json, only the texture differs) -- same convention as the item
        // model below.
        droolingBlock(ModBlocks.DROOLING_CRUCIBLE.get(), models().getExistingFile(ModBlocks.DROOLING_CRUCIBLE.getId()));
        itemModels().withExistingParent(ModBlocks.DROOLING_CRUCIBLE.getId().getPath(),
                modLoc("block/" + ModBlocks.DROOLING_CRUCIBLE.getId().getPath()));

        // Same geometry-sharing convention as Crucible above (models/block/drooling_geode.json
        // parents drooling_cauldron.json, only the texture differs).
        droolingBlock(ModBlocks.DROOLING_GEODE.get(), models().getExistingFile(ModBlocks.DROOLING_GEODE.getId()));
        itemModels().withExistingParent(ModBlocks.DROOLING_GEODE.getId().getPath(),
                modLoc("block/" + ModBlocks.DROOLING_GEODE.getId().getPath()));


        masticatorBlockState();

        new MachineBlockStates(this).register(ModBlocks.EFFLUENTCER.get(), "effluentcer",
                MachineLayouts.EFFLUENTCER, EffluentcerBlock.FACING);


        new MachineBlockStates(this).register(ModBlocks.METASTASIZER.get(), "metastasizer",
                MachineLayouts.METASTASIZER, MetastasizerBlock.FACING);


        new MachineBlockStates(this).register(ModBlocks.MUTATOR.get(), "mutator",
                MachineLayouts.MUTATOR, MutatorBlock.FACING);


        new MachineBlockStates(this).register(ModBlocks.RENDER_FURNACE.get(), "render_furnace",
                MachineLayouts.RENDER_FURNACE, RenderFurnaceBlock.FACING);

        // The grid is on the top face; the facing only turns it (and the light mount) about the vertical axis.
        new MachineBlockStates(this).register(ModBlocks.GRAFTING_TABLE.get(), "grafting_table",
                MachineLayouts.GRAFTING_TABLE, GraftingTableBlock.FACING);


        new CrawBlockStates(this).register(ModBlocks.CRAW.get(), "craw");

        simpleBlock(ModBlocks.BEAKER.get(), models().cubeBottomTop(
                ModBlocks.BEAKER.getId().getPath(),
                modLoc("block/beaker/beaker_side"),
                modLoc("block/beaker/beaker_bottom"),
                modLoc("block/beaker/beaker_top"))
                .renderType("translucent"));

        new MachineBlockStates(this).register(ModBlocks.RENDER_KILN.get(), "render_kiln",
                MachineLayouts.RENDER_KILN, RenderKilnBlock.FACING);


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

    // The Masticator is a multipart: a plate per Overgrowth stage plus the light, mouth and port layers, and its
    // item model is built alongside (see MachineBlockStates). Nothing about it is hand-authored any more.
    private void masticatorBlockState() {
        new MachineBlockStates(this).register(ModBlocks.MASTICATOR.get(), "masticator",
                MachineLayouts.MASTICATOR, MasticatorBlock.FACING);
    }


    /** The Drooling family's one model per facing, with the light level and the evolution properties left out of the
     * generated keys so the blockstate file stays at four entries however many states the block has. */
    private void droolingBlock(Block block, ModelFile model) {
        getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                        .modelFile(model)
                        .rotationY(((int) state.getValue(net.scruffy.dermicraft.block.custom.DroolingMachineBlock.FACING).toYRot() + 180) % 360)
                        .build(),
                net.scruffy.dermicraft.block.custom.DroolingMachineBlock.LIGHT_LEVEL,
                net.scruffy.dermicraft.block.custom.EvolutionBlockProperties.TIER,
                net.scruffy.dermicraft.block.custom.EvolutionBlockProperties.OVERGROWTH);
    }


    /**
     * Full cube made of an opaque base layer with a transparent overlay on top, with the overlay
     * chosen per face group. Both layers are same-bounds elements (the vanilla grass-block approach),
     * so the overlay needs no z-offset; the cutout render type lets the base show through the
     * overlay's transparent pixels. The item model parents to this block model.
     */
    private void tumorOverlayCube(DeferredBlock<Block> block, ResourceLocation base, ResourceLocation top,
                                  ResourceLocation bottom, ResourceLocation side) {
        BlockModelBuilder model = models().getBuilder(block.getId().getPath())
                // Required for the GUI/hand display transforms and gui_light; without a parent the
                // inventory icon renders as a flat face-on square.
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", base)
                .texture("base", base)
                .texture("top", top)
                .texture("side", side)
                .texture("bottom", bottom)
                .renderType("cutout");

        // element 1: opaque base on all six faces
        model.element().from(0, 0, 0).to(16, 16, 16).allFaces((dir, face) ->
                face.texture("#base").cullface(dir)).end();

        // element 2: same bounds, per-face overlay
        model.element().from(0, 0, 0).to(16, 16, 16).allFaces((dir, face) ->
                face.texture(dir == Direction.UP ? "#top" : dir == Direction.DOWN ? "#bottom" : "#side")
                        .cullface(dir)).end();

        simpleBlockWithItem(block.get(), model);
    }

    /** Same overlay on all six faces. */
    private void tumorOverlayCube(DeferredBlock<Block> block, ResourceLocation base, ResourceLocation overlay) {
        tumorOverlayCube(block, base, overlay, overlay, overlay);
    }

    private BlockModelBuilder tumorLayeredModel(String name, ResourceLocation base, ResourceLocation... overlays) {
        BlockModelBuilder model = models().getBuilder(name)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", base)
                .texture("base", base)
                .renderType("cutout");

        model.element().from(0, 0, 0).to(16, 16, 16).allFaces((dir, face) ->
                face.texture("#base").cullface(dir)).end();

        for (int i = 0; i < overlays.length; i++) {
            String key = "layer" + i;
            model.texture(key, overlays[i]);
            model.element().from(0, 0, 0).to(16, 16, 16).allFaces((dir, face) ->
                    face.texture("#" + key).cullface(dir)).end();
        }
        return model;
    }

}
