package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BiofuelProperties(float speed, float useRate) {

    public static final Codec<BiofuelProperties> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("speed").forGetter(BiofuelProperties::speed),
                    Codec.FLOAT.fieldOf("use_rate").forGetter(BiofuelProperties::useRate)
            ).apply(instance, BiofuelProperties::new)
    );
}
