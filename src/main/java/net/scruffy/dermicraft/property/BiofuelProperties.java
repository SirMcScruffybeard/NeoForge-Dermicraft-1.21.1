package net.scruffy.dermicraft.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BiofuelProperties(float speed, float useRate, float heal) {

    public static final Codec<BiofuelProperties> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("speed").forGetter(BiofuelProperties::speed),
                    Codec.FLOAT.fieldOf("use_rate").forGetter(BiofuelProperties::useRate),
                    Codec.FLOAT.fieldOf("heal").forGetter(BiofuelProperties::heal)
            ).apply(instance, BiofuelProperties::new)
    );
}
