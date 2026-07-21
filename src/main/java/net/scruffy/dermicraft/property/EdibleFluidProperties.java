package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EdibleFluidProperties(int mbPerDrink, int hunger, float saturation) {

    public static final Codec<EdibleFluidProperties> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("mb_per_drink").forGetter(EdibleFluidProperties::mbPerDrink),
                    Codec.INT.fieldOf("hunger").forGetter(EdibleFluidProperties::hunger),
                    Codec.FLOAT.fieldOf("saturation").forGetter(EdibleFluidProperties::saturation)
            ).apply(instance, EdibleFluidProperties::new)
    );
}
