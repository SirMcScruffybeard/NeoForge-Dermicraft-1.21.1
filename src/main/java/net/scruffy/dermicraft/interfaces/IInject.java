package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;


public interface IInject {
    default DataComponentType<FluidData> getFluidDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }

    default void emptyDataFluid(ItemStack stack) {
        stack.set(getFluidDataType(), FluidData.EMPTY);
    }

    /**
     * Empties the injected fluid unless the player is in creative mode,
     * letting creative players reuse the same loaded syringe indefinitely.
     */
    default void emptyDataFluidIfSurvival(ItemStack stack, Player player) {
        if (!player.isCreative()) {
            emptyDataFluid(stack);
        }
    }

    /**
     * Drains exactly {@code doseSize} mB off the held fluid rather than wiping it entirely -- for a
     * multi-dose tank (A.I.D.'s Syringe mode) where one injection should only cost one dose, not
     * everything currently loaded. Clamps to emptying if less than a full dose remains, same as any
     * other partial drain.
     */
    default void useDose(ItemStack stack, int doseSize) {
        FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
        if (data.isFluidEmpty()) return;

        FluidStack held = data.getFluidStack();
        int remaining = held.getAmount() - doseSize;
        if (remaining <= 0) {
            emptyDataFluid(stack);
            return;
        }

        FluidStack leftover = held.copy();
        leftover.setAmount(remaining);
        stack.set(getFluidDataType(), FluidData.createData(leftover));
    }

    /** Same creative-mode exemption as {@link #emptyDataFluidIfSurvival}, for {@link #useDose}. */
    default void useDoseIfSurvival(ItemStack stack, Player player, int doseSize) {
        if (!player.isCreative()) {
            useDose(stack, doseSize);
        }
    }
}
