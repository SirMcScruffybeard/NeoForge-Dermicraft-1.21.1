package net.scruffy.dermicraft.main;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.fluids.FluidType;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.fluid.BaseFluidType;
import net.scruffy.dermicraft.fluid.ModFluidTypes;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.property.ModItemProperties;
import net.scruffy.dermicraft.renderer.BeakerBlockEntityRenderer;
import net.scruffy.dermicraft.renderer.DroolingCauldronBlockEntityRenderer;
import net.scruffy.dermicraft.renderer.KnowledgeVatBlockEntityRenderer;
import net.scruffy.dermicraft.renderer.SkinTankBlockEntityRenderer;
import net.scruffy.dermicraft.screen.ModMenuTypes;
import net.scruffy.dermicraft.screen.custom.craw.CrawScreen;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronScreen;
import net.scruffy.dermicraft.screen.custom.effluentcer.EffluentcerScreen;
import net.scruffy.dermicraft.screen.custom.charred_masticator.CharredMasticatorScreen;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorScreen;
import net.scruffy.dermicraft.screen.custom.charred_metastasizer.CharredMetastasizerScreen;
import net.scruffy.dermicraft.screen.custom.metastasizer.MetastasizerScreen;
import net.scruffy.dermicraft.screen.custom.render_kiln.RenderKilnScreen;
import net.scruffy.dermicraft.screen.custom.charred_render_kiln.CharredRenderKilnScreen;
import net.scruffy.dermicraft.screen.custom.grafting_table.GraftingTableScreen;
import net.scruffy.dermicraft.screen.custom.mutator.MutatorScreen;
import net.scruffy.dermicraft.screen.custom.render_furnace.RenderFurnaceScreen;
import net.scruffy.dermicraft.screen.custom.mr_farmer.MrFarmerScreen;
import net.scruffy.dermicraft.screen.custom.node.NodeScreen;
import net.scruffy.dermicraft.screen.custom.skin_tank.SkinTankScreen;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Dermicraft.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Dermicraft.MOD_ID, value = Dist.CLIENT)
public class DermicraftClient {
    public DermicraftClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModItemProperties.addCustomItemProperties();

            // Brain's model has transparent-cutout regions (see brain.json) -- cutout, not solid,
            // so those pixels render as fully see-through rather than opaque black/magenta.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BRAIN.get(), RenderType.cutout());

            renderTranslucentFluid(ModFluids.SOURCE_CALCIUM_BLEND.get(), ModFluids.FLOWING_CALCIUM_BLEND.get());
            renderTranslucentFluid(ModFluids.SOURCE_CARBON_BLEND.get(), ModFluids.FLOWING_CARBON_BLEND.get());
            renderTranslucentFluid(ModFluids.SOURCE_PROTEIN_BLEND.get(), ModFluids.FLOWING_PROTEIN_BLEND.get());

            renderTranslucentFluid(ModFluids.SOURCE_CRUDE_SLURRY.get(), ModFluids.FLOWING_CRUDE_SLURRY.get());
            renderTranslucentFluid(ModFluids.SOURCE_CONCENTRATED_SLURRY.get(), ModFluids.FLOWING_CONCENTRATED_SLURRY.get());

            renderTranslucentFluid(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), ModFluids.FLOWING_PRIMITIVE_CATALYST.get());
            renderTranslucentFluid(ModFluids.SOURCE_REINFORCING_CATALYST.get(), ModFluids.FLOWING_REINFORCING_CATALYST.get());
            renderTranslucentFluid(ModFluids.SOURCE_SYNAPSE_CATALYST.get(), ModFluids.FLOWING_SYNAPSE_CATALYST.get());
            renderTranslucentFluid(ModFluids.SOURCE_KNOWLEDGE_ESSENCE.get(), ModFluids.FLOWING_KNOWLEDGE_ESSENCE.get());
            renderTranslucentFluid(ModFluids.SOURCE_KINETIC_CATALYST.get(), ModFluids.FLOWING_KINETIC_CATALYST.get());
            renderTranslucentFluid(ModFluids.SOURCE_EVOLUTION_CATALYST.get(), ModFluids.FLOWING_EVOLUTION_CATALYST.get());

            renderTranslucentFluid(ModFluids.SOURCE_STONE_BLEND.get(), ModFluids.FLOWING_STONE_BLEND.get());
            renderTranslucentFluid(ModFluids.SOURCE_SILICA_BLEND.get(), ModFluids.FLOWING_SILICA_BLEND.get());
            renderTranslucentFluid(ModFluids.SOURCE_CLAY_BLEND.get(), ModFluids.FLOWING_CLAY_BLEND.get());

            renderTranslucentFluid(ModFluids.SOURCE_FERROUS_BLEND.get(), ModFluids.FLOWING_FERROUS_BLEND.get());
            renderTranslucentFluid(ModFluids.SOURCE_CUPROUS_BLEND.get(), ModFluids.FLOWING_CUPROUS_BLEND.get());
            renderTranslucentFluid(ModFluids.SOURCE_AUROUS_BLEND.get(), ModFluids.FLOWING_AUROUS_BLEND.get());

            renderTranslucentFluid(ModFluids.SOURCE_F_STUFF.get(), ModFluids.FLOWING_F_STUFF.get());
            renderTranslucentFluid(ModFluids.SOURCE_C_STUFF.get(), ModFluids.FLOWING_C_STUFF.get());

            renderTranslucentFluid(ModFluids.SOURCE_PULP_BLEND.get(), ModFluids.FLOWING_PULP_BLEND.get());

            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_REDSTONE.get(), ModFluids.FLOWING_MOLTEN_REDSTONE.get());
            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_QUARTZ.get(), ModFluids.FLOWING_MOLTEN_QUARTZ.get());
            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_GLOWSTONE.get(), ModFluids.FLOWING_MOLTEN_GLOWSTONE.get());
            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_AMETHYST.get(), ModFluids.FLOWING_MOLTEN_AMETHYST.get());
            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_DIAMOND.get(), ModFluids.FLOWING_MOLTEN_DIAMOND.get());            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_LAPIS.get(), ModFluids.FLOWING_MOLTEN_LAPIS.get());
            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_EMERALD.get(), ModFluids.FLOWING_MOLTEN_EMERALD.get());
            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_RAW_NETHERITE_SCRAP.get(), ModFluids.FLOWING_MOLTEN_RAW_NETHERITE_SCRAP.get());
            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_NETHERITE.get(), ModFluids.FLOWING_MOLTEN_NETHERITE.get());
            renderTranslucentFluid(ModFluids.SOURCE_BLAZE_ESSENCE.get(), ModFluids.FLOWING_BLAZE_ESSENCE.get());
            renderTranslucentFluid(ModFluids.SOURCE_GHAST_ESSENCE.get(), ModFluids.FLOWING_GHAST_ESSENCE.get());
            renderTranslucentFluid(ModFluids.SOURCE_WITHER_ESSENCE.get(), ModFluids.FLOWING_WITHER_ESSENCE.get());
            renderTranslucentFluid(ModFluids.SOURCE_ENDER_ESSENCE.get(), ModFluids.FLOWING_ENDER_ESSENCE.get());
            renderTranslucentFluid(ModFluids.SOURCE_MOLTEN_SOUL_SILICA.get(), ModFluids.FLOWING_MOLTEN_SOUL_SILICA.get());
        });
    }

    @SubscribeEvent
    public static void onClientExtensions(RegisterClientExtensionsEvent event) {
        registerFluidType(event, ModFluidTypes.CALCIUM_BLEND_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.CARBON_BLEND_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.PROTEIN_BLEND_FLUID_TYPE.get());

        registerFluidType(event, ModFluidTypes.CRUDE_SLURRY_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.CONCENTRATED_SLURRY_FLUID_TYPE.get());

        registerFluidType(event, ModFluidTypes.PRIMITIVE_CATALYST_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.REINFORCING_CATALYST_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.SYNAPSE_CATALYST_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.KNOWLEDGE_ESSENCE_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.KINETIC_CATALYST_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.EVOLUTION_CATALYST_FLUID_TYPE.get());

        registerFluidType(event, ModFluidTypes.STONE_BLEND_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.SILICA_BLEND_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.CLAY_BLEND_FLUID_TYPE.get());

        registerFluidType(event, ModFluidTypes.FERROUS_BLEND_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.CUPROUS_BLEND_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.AUROUS_BLEND_FLUID_TYPE.get());

        registerFluidType(event, ModFluidTypes.F_STUFF_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.C_STUFF_FLUID_TYPE.get());

        registerFluidType(event, ModFluidTypes.PULP_BLEND_FLUID_TYPE.get());

        registerFluidType(event, ModFluidTypes.MOLTEN_REDSTONE_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.MOLTEN_QUARTZ_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.MOLTEN_GLOWSTONE_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.MOLTEN_AMETHYST_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.MOLTEN_DIAMOND_FLUID_TYPE.get());        registerFluidType(event, ModFluidTypes.MOLTEN_LAPIS_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.MOLTEN_EMERALD_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.MOLTEN_RAW_NETHERITE_SCRAP_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.MOLTEN_NETHERITE_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.BLAZE_ESSENCE_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.GHAST_ESSENCE_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.WITHER_ESSENCE_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.ENDER_ESSENCE_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.MOLTEN_SOUL_SILICA_FLUID_TYPE.get());

        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private final net.scruffy.dermicraft.item.custom.SippingItemRenderer renderer =
                    new net.scruffy.dermicraft.item.custom.SippingItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, net.scruffy.dermicraft.item.ModItems.SIPPING.get());

        // Bare stand-up renderer only -- see AidItem's class javadoc. No mode/animation logic yet.
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private final net.scruffy.dermicraft.item.custom.AidItemRenderer renderer =
                    new net.scruffy.dermicraft.item.custom.AidItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, net.scruffy.dermicraft.item.ModItems.AID.get());

        // Not an inline anonymous class like the others -- DRINKER also overrides the hand
        // transform to stay still while siphoning. See DrinkerClientExtensions.
        event.registerItem(new net.scruffy.dermicraft.item.custom.DrinkerClientExtensions(),
                net.scruffy.dermicraft.item.ModItems.DRINKER.get());

        // Same steady-hand shape as DRINKER -- see EaterClientExtensions.
        event.registerItem(new net.scruffy.dermicraft.item.custom.EaterClientExtensions(),
                net.scruffy.dermicraft.item.ModItems.EATER.get());

        // Placeholder renderer wiring -- see SunderItem's class javadoc. In-game only for model preview.
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private final net.scruffy.dermicraft.item.custom.SunderItemRenderer renderer =
                    new net.scruffy.dermicraft.item.custom.SunderItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, net.scruffy.dermicraft.item.ModItems.SUNDER.get());

        // Registration/visibility pass only -- see ShatterItem's class javadoc.
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private final net.scruffy.dermicraft.item.custom.ShatterItemRenderer renderer =
                    new net.scruffy.dermicraft.item.custom.ShatterItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, net.scruffy.dermicraft.item.ModItems.SHATTER.get());

        // See WorkbenchBottomItemRenderer's own javadoc -- a plain BlockItem isn't a GeoItem, so
        // GeckoLib's own automatic item-render hook never fires for it without this.
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private final net.scruffy.dermicraft.renderer.WorkbenchBottomItemRenderer renderer =
                    new net.scruffy.dermicraft.renderer.WorkbenchBottomItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, net.scruffy.dermicraft.block.ModBlocks.WORKBENCH.asItem());
    }

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SKIN_TANK_BE.get(), SkinTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CHARRED_TANK_BE.get(), SkinTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.KNOWLEDGE_VAT_BE.get(), KnowledgeVatBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DROOLING_CAULDRON_BE.get(), DroolingCauldronBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DROOLING_CRUCIBLE_BE.get(), DroolingCauldronBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DROOLING_GEODE_BE.get(), DroolingCauldronBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BEAKER_BE.get(), BeakerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WORKBENCH_TOP_BE.get(),
                net.scruffy.dermicraft.renderer.WorkbenchTopBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WORKBENCH_BE.get(),
                net.scruffy.dermicraft.renderer.WorkbenchBottomBlockEntityRenderer::new);
        // Base tier only -- Charred variants' canEvolve() is hardwired false, so their progress
        // fraction is always 0 and the overlay would never actually draw anything.
        event.registerBlockEntityRenderer(ModBlockEntities.MASTICATOR_BE.get(),
                net.scruffy.dermicraft.renderer.EvolutionOverlayBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.METASTASIZER_BE.get(),
                net.scruffy.dermicraft.renderer.EvolutionOverlayBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.EFFLUENTCER_BE.get(),
                net.scruffy.dermicraft.renderer.EvolutionOverlayBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MUTATOR_BE.get(),
                net.scruffy.dermicraft.renderer.EvolutionOverlayBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RENDER_KILN_BE.get(),
                net.scruffy.dermicraft.renderer.EvolutionOverlayBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RENDER_FURNACE_BE.get(),
                net.scruffy.dermicraft.renderer.EvolutionOverlayBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SKIN_TANK_MENU.get(), SkinTankScreen::new);
        event.register(ModMenuTypes.CHARRED_TANK_MENU.get(), net.scruffy.dermicraft.screen.custom.charred_tank.CharredTankScreen::new);
        event.register(ModMenuTypes.DROOLING_CAULDRON_MENU.get(), DroolingCauldronScreen::new);
        event.register(ModMenuTypes.DROOLING_CRUCIBLE_MENU.get(), net.scruffy.dermicraft.screen.custom.drooling_crucible.DroolingCrucibleScreen::new);
        event.register(ModMenuTypes.DROOLING_GEODE_MENU.get(), net.scruffy.dermicraft.screen.custom.drooling_geode.DroolingGeodeScreen::new);
        event.register(ModMenuTypes.MASTICATOR_MENU.get(), MasticatorScreen::new);
        event.register(ModMenuTypes.CHARRED_MASTICATOR_MENU.get(), CharredMasticatorScreen::new);
        event.register(ModMenuTypes.EFFLUENTCER_MENU.get(), EffluentcerScreen::new);
        event.register(ModMenuTypes.CHARRED_EFFLUENTCER_MENU.get(), net.scruffy.dermicraft.screen.custom.charred_effluentcer.CharredEffluentcerScreen::new);
        event.register(ModMenuTypes.METASTASIZER_MENU.get(), MetastasizerScreen::new);
        event.register(ModMenuTypes.CHARRED_METASTASIZER_MENU.get(), CharredMetastasizerScreen::new);
        event.register(ModMenuTypes.CRAW_MENU.get(), CrawScreen::new);
        event.register(ModMenuTypes.CHARRED_CRAW_MENU.get(), net.scruffy.dermicraft.screen.custom.charred_craw.CharredCrawScreen::new);
        event.register(ModMenuTypes.INNARDS_NODE_MENU.get(), NodeScreen::new);
        event.register(ModMenuTypes.MR_FARMER_MENU.get(), MrFarmerScreen::new);
        event.register(ModMenuTypes.MR_SHEPARD_MENU.get(), net.scruffy.dermicraft.screen.custom.mr_shepard.MrShepardScreen::new);
        event.register(ModMenuTypes.MUTATOR_MENU.get(), MutatorScreen::new);
        event.register(ModMenuTypes.CHARRED_MUTATOR_MENU.get(), net.scruffy.dermicraft.screen.custom.charred_mutator.CharredMutatorScreen::new);
        event.register(ModMenuTypes.RENDER_FURNACE_MENU.get(), RenderFurnaceScreen::new);
        event.register(ModMenuTypes.CHARRED_RENDER_FURNACE_MENU.get(), net.scruffy.dermicraft.screen.custom.charred_render_furnace.CharredRenderFurnaceScreen::new);
        event.register(ModMenuTypes.GRAFTING_TABLE_MENU.get(), GraftingTableScreen::new);
        event.register(ModMenuTypes.RENDER_KILN_MENU.get(), RenderKilnScreen::new);
        event.register(ModMenuTypes.CHARRED_RENDER_KILN_MENU.get(), CharredRenderKilnScreen::new);
        event.register(ModMenuTypes.SCRENCH_MENU.get(), net.scruffy.dermicraft.screen.custom.scrench.ScrenchScreen::new);
        event.register(ModMenuTypes.AID_MENU.get(), net.scruffy.dermicraft.screen.custom.aid.AidScreen::new);
        event.register(ModMenuTypes.WORKBENCH_MENU.get(), net.scruffy.dermicraft.screen.custom.workbench.WorkbenchScreen::new);
    }

    private static void renderTranslucentFluid(Fluid source, Fluid flow) {
        ItemBlockRenderTypes.setRenderLayer(source, RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(flow, RenderType.translucent());

    }

    private static void registerFluidType(RegisterClientExtensionsEvent event, FluidType fluidType) {

        event.registerFluidType(((BaseFluidType) fluidType).getClientFluidTypeExtensions(),
                fluidType);

    }
}
