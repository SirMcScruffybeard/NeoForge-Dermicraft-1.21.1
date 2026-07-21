package net.scruffy.dermicraft.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.property.EdibleFluidProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Shared drink logic for any fluid-holding item that lets the player drink an edible fluid --
 * looks up {@code dermicraft:edible_fluid} data map entries (see dermicraft-liquid-foods-notes.md).
 * Not Feeder Bladder-specific; other drinkable containers can reuse this.
 */
public class FluidFoodUtil {

    @Nullable
    private static EdibleFluidProperties getEdibleData(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) return null;
        return BuiltInRegistries.FLUID.wrapAsHolder(fluidStack.getFluid()).getData(ModDataMaps.EDIBLE_FLUID);
    }

    public static boolean isEdible(FluidStack fluidStack) {
        return getEdibleData(fluidStack) != null;
    }

    /**
     * Whether a drink can be started right now -- fluid must be edible, hold at least one full
     * drink's worth (no partial sips), and the player must actually be able to eat.
     */
    public static boolean canDrink(Player player, FluidStack fluidStack) {
        EdibleFluidProperties data = getEdibleData(fluidStack);
        if (data == null || fluidStack.getAmount() < data.mbPerDrink()) return false;
        return player.canEat(false);
    }

    /**
     * Applies one drink's hunger/saturation and returns the fluid data with that drink's mB drained.
     * Call only after {@link #canDrink} has already confirmed the drink is valid.
     */
    public static FluidData drink(Player player, FluidData data) {
        EdibleFluidProperties props = getEdibleData(data.getFluidStack());
        if (props == null) return data;

        player.getFoodData().eat(props.hunger(), props.saturation());

        FluidStack remaining = data.getFluidStack().copy();
        remaining.shrink(props.mbPerDrink());
        return FluidData.createData(remaining);
    }
}
