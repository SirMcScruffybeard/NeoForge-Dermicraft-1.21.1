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
import net.scruffy.dermicraft.screen.custom.skin_tank.SkinTankMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Dermicraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SkinTankMenu>> SKIN_TANK_MENU =
            registerMenuType("skin_tank_menu", SkinTankMenu::new);




    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
            String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

}
