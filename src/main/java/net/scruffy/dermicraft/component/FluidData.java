package net.scruffy.dermicraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public record FluidData(FluidStack fluidStack) {

    ////////////////CODECS\\\\\\\\\\\\\\\\
    public static final Codec<FluidData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FluidStack.CODEC.fieldOf("fluid").forGetter(FluidData::fluidStack))
            .apply(instance, FluidData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidData> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStack.STREAM_CODEC, FluidData::fluidStack,
                    FluidData::new);

    public static FluidData EMPTY = createData(FluidStack.EMPTY);

    public static FluidData createData(FluidStack fluidStack) {
        return new FluidData(fluidStack);
    }

    public static FluidData createData(Fluid fluid, int amount) {
        return new FluidData(new FluidStack(fluid, amount));
    }

    public FluidStack getFluidStack() {
        return fluidStack;
    }

    public Fluid getFluid() {
        return getFluidStack().getFluid();
    }

    public FluidType getFluidType() {
        return getFluid().getFluidType();
    }

    public boolean isSameAsContained(Fluid fluid){
        return fluid.isSame(getFluid());
    }

    public int getFluidAmount(){
        return fluidStack.getAmount();
    }

    public boolean isFluidEmpty(){
        return fluidStack.isEmpty();
    }

    public Component getFluidComponent() {
        return Component.translatable(this.getFluidType().getDescriptionId());
    }

    public String getFluidString() {
        return getFluidComponent().getString();
    }
}
