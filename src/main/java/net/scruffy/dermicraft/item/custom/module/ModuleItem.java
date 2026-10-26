package net.scruffy.dermicraft.item.custom.module;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.interfaces.IHasModuleData;

/**
 * Base of every module item. For now it only names a stack from its component data when the item has one
 * ({@link IHasModuleData}); a bare stack keeps the item's own name. Shared module behavior goes here as other
 * modules need it.
 */
public abstract class ModuleItem extends Item {
    public ModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (this instanceof IHasModuleData named) {
            return named.getDataName(stack, getDescriptionId()).orElseGet(() -> super.getName(stack));
        }
        return super.getName(stack);
    }
}
