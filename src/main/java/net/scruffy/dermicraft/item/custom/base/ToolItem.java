package net.scruffy.dermicraft.item.custom.base;

import net.minecraft.world.item.Item;

public abstract class ToolItem extends Item {
    public ToolItem(Properties properties) {
        super(properties
                .stacksTo(1));
    }
}
