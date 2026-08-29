package net.scruffy.dermicraft.screen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.screen.custom.charred_effluentcer.CharredEffluentcerMenu;
import net.scruffy.dermicraft.screen.custom.charred_masticator.CharredMasticatorMenu;
import net.scruffy.dermicraft.screen.custom.charred_render_furnace.CharredRenderFurnaceMenu;
import net.scruffy.dermicraft.screen.custom.charred_render_kiln.CharredRenderKilnMenu;
import net.scruffy.dermicraft.screen.custom.charred_tank.CharredTankMenu;
import net.scruffy.dermicraft.screen.custom.charred_craw.CharredCrawMenu;
import net.scruffy.dermicraft.screen.custom.craw.CrawMenu;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronMenu;
import net.scruffy.dermicraft.screen.custom.drooling_crucible.DroolingCrucibleMenu;
import net.scruffy.dermicraft.screen.custom.drooling_geode.DroolingGeodeMenu;
import net.scruffy.dermicraft.screen.custom.effluentcer.EffluentcerMenu;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorMenu;
import net.scruffy.dermicraft.screen.custom.charred_metastasizer.CharredMetastasizerMenu;
import net.scruffy.dermicraft.screen.custom.metastasizer.MetastasizerMenu;
import net.scruffy.dermicraft.screen.custom.mr_farmer.MrFarmerMenu;
import net.scruffy.dermicraft.screen.custom.grafting_table.GraftingTableMenu;
import net.scruffy.dermicraft.screen.custom.charred_mutator.CharredMutatorMenu;
import net.scruffy.dermicraft.screen.custom.mutator.MutatorMenu;
import net.scruffy.dermicraft.screen.custom.node.NodeMenu;
import net.scruffy.dermicraft.screen.custom.render_furnace.RenderFurnaceMenu;
import net.scruffy.dermicraft.screen.custom.render_kiln.RenderKilnMenu;
import net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu;
import net.scruffy.dermicraft.screen.custom.skin_tank.SkinTankMenu;
import net.scruffy.dermicraft.screen.custom.workbench.WorkbenchMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Dermicraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SkinTankMenu>> SKIN_TANK_MENU =
            registerMenuType("skin_tank_menu", SkinTankMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CharredTankMenu>> CHARRED_TANK_MENU =
            registerMenuType("charred_tank_menu", CharredTankMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<DroolingCauldronMenu>> DROOLING_CAULDRON_MENU =
            registerMenuType("drooling_cauldron_menu", DroolingCauldronMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<DroolingCrucibleMenu>> DROOLING_CRUCIBLE_MENU =
            registerMenuType("drooling_crucible_menu", DroolingCrucibleMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<DroolingGeodeMenu>> DROOLING_GEODE_MENU =
            registerMenuType("drooling_geode_menu", DroolingGeodeMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MasticatorMenu>> MASTICATOR_MENU =
            registerMenuType("masticator_menu", MasticatorMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CharredMasticatorMenu>> CHARRED_MASTICATOR_MENU =
            registerMenuType("charred_masticator_menu", CharredMasticatorMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<EffluentcerMenu>> EFFLUENTCER_MENU =
            registerMenuType("effluentcer_menu", EffluentcerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CharredEffluentcerMenu>> CHARRED_EFFLUENTCER_MENU =
            registerMenuType("charred_effluentcer_menu", CharredEffluentcerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MetastasizerMenu>> METASTASIZER_MENU =
            registerMenuType("metastasizer_menu", MetastasizerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CharredMetastasizerMenu>> CHARRED_METASTASIZER_MENU =
            registerMenuType("charred_metastasizer_menu", CharredMetastasizerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CrawMenu>> CRAW_MENU =
            registerMenuType("craw_menu", CrawMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CharredCrawMenu>> CHARRED_CRAW_MENU =
            registerMenuType("charred_craw_menu", CharredCrawMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<NodeMenu>> INNARDS_NODE_MENU =
            registerMenuType("innards_node_menu", NodeMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MrFarmerMenu>> MR_FARMER_MENU =
            registerMenuType("mr_farmer_menu", MrFarmerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<net.scruffy.dermicraft.screen.custom.mr_shepard.MrShepardMenu>> MR_SHEPARD_MENU =
            registerMenuType("mr_shepard_menu", net.scruffy.dermicraft.screen.custom.mr_shepard.MrShepardMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MutatorMenu>> MUTATOR_MENU =
            registerMenuType("mutator_menu", MutatorMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CharredMutatorMenu>> CHARRED_MUTATOR_MENU =
            registerMenuType("charred_mutator_menu", CharredMutatorMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<RenderFurnaceMenu>> RENDER_FURNACE_MENU =
            registerMenuType("render_furnace_menu", RenderFurnaceMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CharredRenderFurnaceMenu>> CHARRED_RENDER_FURNACE_MENU =
            registerMenuType("charred_render_furnace_menu", CharredRenderFurnaceMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<GraftingTableMenu>> GRAFTING_TABLE_MENU =
            registerMenuType("grafting_table_menu", GraftingTableMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<RenderKilnMenu>> RENDER_KILN_MENU =
            registerMenuType("render_kiln_menu", RenderKilnMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CharredRenderKilnMenu>> CHARRED_RENDER_KILN_MENU =
            registerMenuType("charred_render_kiln_menu", CharredRenderKilnMenu::new);

    // First item-triggered menu in the mod, no block entity -- see ScrenchMenu's class javadoc.
    public static final DeferredHolder<MenuType<?>, MenuType<ScrenchMenu>> SCRENCH_MENU =
            registerMenuType("scrench_menu", ScrenchMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<WorkbenchMenu>> WORKBENCH_MENU =
            registerMenuType("workbench_menu", WorkbenchMenu::new);




    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
            String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

}
