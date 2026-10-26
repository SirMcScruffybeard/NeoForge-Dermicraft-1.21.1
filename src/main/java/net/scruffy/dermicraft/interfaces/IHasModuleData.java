package net.scruffy.dermicraft.interfaces;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * A module item whose stacks carry component data that names them. {@code ModuleItem#getName} asks this, since an
 * interface default cannot override a class method; {@link IHasHazardModuleData} and {@link IHasLevelModuleData} supply it for their
 * component.
 */
public interface IHasModuleData {

    /** The stack's name from its component data, or empty for a stack that carries none (a bare module). */
    Optional<Component> getDataName(ItemStack stack, String descriptionId);
}