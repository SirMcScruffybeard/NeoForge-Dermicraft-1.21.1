package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.entity.custom.KnowledgeVatBlockEntity;

import java.util.List;

/**
 * Knowledge Vat's Forceps-recovered item -- same {@code BLOCK_ENTITY_DATA}-carries-the-tank-home
 * mechanism as {@link SkinTankBlockItem} (see that class's own javadoc), but with its own tooltip
 * since Knowledge Vat's contents are more meaningfully read as levels (its actual player-facing
 * unit) than raw mB alone -- shows both.
 */
public class KnowledgeVatBlockItem extends BlockItem {

    public KnowledgeVatBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        FluidStack fluid = readStoredFluid(stack, context.registries());
        if (fluid.isEmpty()) return;

        if (Screen.hasShiftDown()) {
            tooltip.add(fluid.getHoverName());
            tooltip.add(Component.translatable("tooltip.dermicraft.liquid.amount", fluid.getAmount()));
            tooltip.add(Component.translatable("tooltip.dermicraft.knowledge_vat.levels",
                    fluid.getAmount() / KnowledgeVatBlockEntity.MB_PER_LEVEL));
        } else {
            tooltip.add(Component.translatable("tooltip.dermicraft.hold_shift_for_amount"));
        }
    }

    private FluidStack readStoredFluid(ItemStack stack, HolderLookup.Provider registries) {
        if (registries == null) return FluidStack.EMPTY;

        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return FluidStack.EMPTY;

        CompoundTag tag = data.copyTag();
        if (!tag.contains("Fluid")) return FluidStack.EMPTY;

        return FluidStack.parseOptional(registries, tag.getCompound("Fluid"));
    }
}
