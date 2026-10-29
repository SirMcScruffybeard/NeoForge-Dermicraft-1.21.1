package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.HazardModuleData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import org.jetbrains.annotations.Nullable;

/**
 * The wiring for a module that grants a hazard family and level: reading what the item grants from the
 * {@code hazard_module} item data map. Written once for every hazard module, Evolution and Safety. The grant lives
 * on the item, not the stack, so a datapack can change it and every stack of the item agrees.
 */
public interface IHasHazardModuleData {
    /** What {@code stack}'s item grants, or null if it has no {@code hazard_module} entry (it then grants nothing). */
    @Nullable
    default HazardModuleData getHazardData(ItemStack stack) {
        return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).getData(ModDataMaps.HAZARD_MODULE);
    }
}
