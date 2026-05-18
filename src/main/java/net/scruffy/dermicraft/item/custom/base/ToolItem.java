package net.scruffy.dermicraft.item.custom.base;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class ToolItem extends Item {
    public ToolItem(Properties properties) {
        super(properties
                .stacksTo(1));
    }

    protected int getTotalWear(float modifier) {
        return (int) (this.getMaxDamage(new ItemStack(this)) * modifier);

    }

}
