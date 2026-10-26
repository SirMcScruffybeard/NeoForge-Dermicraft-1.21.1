package net.scruffy.dermicraft.interfaces;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The wiring for a module that carries {@link HazardModuleData}: reading it off a stack, and naming the stack from
 * it ({@code <item>.hazard.<family>.<level>}). Written once for every hazard module, Evolution and Safety.
 */
public interface IHasHazardModuleData extends IHasModuleData {
    /** The hazard data on {@code stack}, or null for a bare module. */
    @Nullable
    default HazardModuleData getHazardData(ItemStack stack) {
        return stack.get(ModDataComponentTypes.HAZARD_MODULE_DATA);
    }

    @Override
    default Optional<Component> getDataName(ItemStack stack, String descriptionId) {
        HazardModuleData data = getHazardData(stack);
        if (data == null) return Optional.empty();
        return Optional.of(Component.translatable(
                descriptionId + ".hazard." + data.family().getSerializedName() + "." + data.level()));
    }
}
