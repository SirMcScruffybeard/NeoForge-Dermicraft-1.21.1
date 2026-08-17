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

import java.util.List;

/**
 * Skin Tank's Forceps-recovered item (see {@code IPreserveContentsOnPickup}/{@code ICollectBlocks})
 * carries its stored fluid home via the vanilla BLOCK_ENTITY_DATA component -- {@code BlockEntity
 * #saveToItem} writes {@code SkinTankBlockEntity#saveAdditional}'s tag straight onto the item, and
 * placing it back down restores that data automatically (standard {@code BlockItem} behavior, same
 * mechanism vanilla Shulker Boxes use). This class only adds the shift-hidden tooltip showing what's
 * actually inside, reading the same "Fluid" key {@code ModFluidTank}/vanilla {@code FluidTank}
 * writes -- no separate FluidData component needed since there's no fluid to show until a real pickup
 * has happened.
 */
public class SkinTankBlockItem extends BlockItem {

    public SkinTankBlockItem(Block block, Properties properties) {
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
