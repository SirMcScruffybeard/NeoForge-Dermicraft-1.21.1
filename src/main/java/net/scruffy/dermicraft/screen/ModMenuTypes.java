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
import net.scruffy.dermicraft.screen.custom.craw.CrawMenu;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronMenu;
import net.scruffy.dermicraft.screen.custom.effluentcer.EffluentcerMenu;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorMenu;
import net.scruffy.dermicraft.screen.custom.metastasizer.MetastasizerMenu;
import net.scruffy.dermicraft.screen.custom.mr_farmer.MrFarmerMenu;
import net.scruffy.dermicraft.screen.custom.grafting_table.GraftingTableMenu;
import net.scruffy.dermicraft.screen.custom.mutator.MutatorMenu;
import net.scruffy.dermicraft.screen.custom.node.NodeMenu;
import net.scruffy.dermicraft.screen.custom.render_furnace.RenderFurnaceMenu;
import net.scruffy.dermicraft.screen.custom.skin_tank.SkinTankMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Dermicraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SkinTankMenu>> SKIN_TANK_MENU =
            registerMenuType("skin_tank_menu", SkinTankMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<DroolingCauldronMenu>> DROOLING_CAULDRON_MENU =
            registerMenuType("drooling_cauldron_menu", DroolingCauldronMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MasticatorMenu>> MASTICATOR_MENU =
            registerMenuType("masticator_menu", MasticatorMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<EffluentcerMenu>> EFFLUENTCER_MENU =
            registerMenuType("effluentcer_menu", EffluentcerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MetastasizerMenu>> METASTASIZER_MENU =
            registerMenuType("metastasizer_menu", MetastasizerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CrawMenu>> CRAW_MENU =
            registerMenuType("craw_menu", CrawMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<NodeMenu>> INNARDS_NODE_MENU =
            registerMenuType("innards_node_menu", NodeMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MrFarmerMenu>> MR_FARMER_MENU =
            registerMenuType("mr_farmer_menu", MrFarmerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<MutatorMenu>> MUTATOR_MENU =
            registerMenuType("mutator_menu", MutatorMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<RenderFurnaceMenu>> RENDER_FURNACE_MENU =
            registerMenuType("render_furnace_menu", RenderFurnaceMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<GraftingTableMenu>> GRAFTING_TABLE_MENU =
            registerMenuType("grafting_table_menu", GraftingTableMenu::new);




    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
            String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

}
