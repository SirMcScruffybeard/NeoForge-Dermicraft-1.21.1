package net.scruffy.dermicraft.interfaces;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.LevelModuleData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The wiring for a module that carries {@link LevelModuleData}: reading it off a stack, and naming the stack from
 * it (the item's {@code .named} lang key, which takes the level).
 */
public interface IHasLevelModuleData extends IHasModuleData {
    /** The level data on {@code stack}, or null for a bare module. */
    @Nullable
    default LevelModuleData getLevelData(ItemStack stack) {
        return stack.get(ModDataComponentTypes.LEVEL_MODULE_DATA);
    }

    @Override
    default Optional<Component> getDataName(ItemStack stack, String descriptionId) {
        LevelModuleData data = getLevelData(stack);
        if (data == null) return Optional.empty();
        return Optional.of(Component.translatable(descriptionId + ".named", data.level()));
    }

}
