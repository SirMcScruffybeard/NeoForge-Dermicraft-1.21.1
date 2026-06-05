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
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.fluid.BaseFluidType;
import net.scruffy.dermicraft.fluid.ModFluidTypes;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.property.ModItemProperties;
import net.scruffy.dermicraft.renderer.DroolingCauldronBlockEntityRenderer;
import net.scruffy.dermicraft.renderer.SkinTankBlockEntityRenderer;
import net.scruffy.dermicraft.screen.ModMenuTypes;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronScreen;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorScreen;
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

            renderTranslucentFluid(ModFluids.SOURCE_CALCIUM_BLEND.get(), ModFluids.FLOWING_CALCIUM_BLEND.get());
            renderTranslucentFluid(ModFluids.SOURCE_CARBON_BLEND.get(), ModFluids.FLOWING_CARBON_BLEND.get());

            renderTranslucentFluid(ModFluids.SOURCE_CRUDE_SLURRY.get(), ModFluids.FLOWING_CRUDE_SLURRY.get());
        });
    }

    @SubscribeEvent
    public static void onClientExtensions(RegisterClientExtensionsEvent event) {
        registerFluidType(event, ModFluidTypes.CALCIUM_BLEND_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.CARBON_BLEND_FLUID_TYPE.get());
        registerFluidType(event, ModFluidTypes.CRUDE_SLURRY_FLUID_TYPE.get());

    }

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SKIN_TANK_BE.get(), SkinTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DROOLING_CAULDRON_BE.get(), DroolingCauldronBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SKIN_TANK_MENU.get(), SkinTankScreen::new);
        event.register(ModMenuTypes.DROOLING_CAULDRON_MENU.get(), DroolingCauldronScreen::new);
        event.register(ModMenuTypes.MASTICATOR_MENU.get(), MasticatorScreen::new);
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
