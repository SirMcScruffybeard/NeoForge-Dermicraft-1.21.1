package net.scruffy.dermicraft.compat.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.scruffy.dermicraft.screen.AbstractEvolvingMachineScreen;
import net.scruffy.dermicraft.screen.AbstractModScreen;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronScreen;
import net.scruffy.dermicraft.screen.custom.drooling_crucible.DroolingCrucibleScreen;
import net.scruffy.dermicraft.screen.custom.drooling_geode.DroolingGeodeScreen;
import net.scruffy.dermicraft.screen.custom.effluentcer.EffluentcerScreen;
import net.scruffy.dermicraft.screen.custom.grafting_table.GraftingTableScreen;
import net.scruffy.dermicraft.screen.custom.masticator.MasticatorScreen;
import net.scruffy.dermicraft.screen.custom.metastasizer.MetastasizerScreen;
import net.scruffy.dermicraft.screen.custom.mutator.MutatorScreen;
import net.scruffy.dermicraft.screen.custom.render_furnace.RenderFurnaceScreen;
import net.scruffy.dermicraft.screen.custom.render_kiln.RenderKilnScreen;

import java.util.Collection;
import java.util.List;

/**
 * How JEI treats the mod's machine screens: the tab strip on the left edge is kept clear of JEI's own lists, and clicking
 * a machine's progress arrow opens that machine's recipes.
 */
final class ScreenAreas {

    private ScreenAreas() {}

    static void register(IGuiHandlerRegistration registration) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<AbstractModScreen<?>> modScreen = (Class) AbstractModScreen.class;
        registration.addGuiContainerHandler(modScreen, new IGuiContainerHandler<AbstractModScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AbstractModScreen<?> screen) {
                Rect2i tabs = screen.getTabStripArea();
                return tabs == null ? List.of() : List.of(tabs);
            }
        });

        addArrow(registration, MasticatorScreen.class, DermicraftRecipeTypes.MASTICATING);
        addArrow(registration, EffluentcerScreen.class, DermicraftRecipeTypes.EFFLUENCING);
        addArrow(registration, MetastasizerScreen.class, DermicraftRecipeTypes.METASTASIZING);
        addArrow(registration, MutatorScreen.class, DermicraftRecipeTypes.MUTATING);
        addArrow(registration, RenderKilnScreen.class, DermicraftRecipeTypes.RENDERING);
        addArrow(registration, DroolingCauldronScreen.class, DermicraftRecipeTypes.DROOLING_CAULDRON);
        addArrow(registration, DroolingCrucibleScreen.class, DermicraftRecipeTypes.DROOLING_CRUCIBLE);
        addArrow(registration, DroolingGeodeScreen.class, DermicraftRecipeTypes.DROOLING_GEODE);
        addArrow(registration, RenderFurnaceScreen.class, RecipeTypes.SMELTING);
        addArrow(registration, GraftingTableScreen.class, RecipeTypes.CRAFTING);
    }

    /** Makes the screen's progress arrow open {@code type}'s recipes, only while the Main tab (where the arrow is) shows. */
    private static <S extends AbstractEvolvingMachineScreen<?>> void addArrow(IGuiHandlerRegistration registration,
                                                                              Class<S> screenClass, RecipeType<?> type) {
        registration.addGuiContainerHandler(screenClass, new IGuiContainerHandler<S>() {
            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(S screen, double mouseX, double mouseY) {
                Rect2i area = screen.getRecipeClickArea();
                if (area == null) return List.of();
                // JEI wants the area relative to the screen's top-left.
                return List.of(IGuiClickableArea.createBasic(area.getX() - screen.getGuiLeft(),
                        area.getY() - screen.getGuiTop(), area.getWidth(), area.getHeight(), type));
            }
        });
    }
}
