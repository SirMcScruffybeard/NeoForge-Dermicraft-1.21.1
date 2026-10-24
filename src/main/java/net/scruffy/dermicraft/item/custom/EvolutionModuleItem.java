package net.scruffy.dermicraft.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.EvolutionModuleData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;

/**
 * The single Evolution Module item. A bare stack is the inert Evolution Module Frame; a stack carrying
 * {@link EvolutionModuleData} is a Tier / Hazard / Overgrowth module and is named from that component
 * ("[type] Evolution Module"), one lang key per variant.
 */
public class EvolutionModuleItem extends Item {
    public EvolutionModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        EvolutionModuleData data = stack.get(ModDataComponentTypes.EVOLUTION_MODULE_DATA);
        if (data == null) return super.getName(stack);

        String base = getDescriptionId();
        return switch (data.type()) {
            case TIER -> Component.translatable(base + ".tier", data.level());
            case OVERGROWTH -> Component.translatable(base + ".overgrowth", data.level());
            case HAZARD -> data.family() == null ? super.getName(stack)
                    : Component.translatable(base + ".hazard." + data.family().getSerializedName() + "." + data.level());
        };
    }
}
