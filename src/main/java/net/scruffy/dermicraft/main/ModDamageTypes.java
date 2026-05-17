package net.scruffy.dermicraft.main;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public class ModDamageTypes {

    public static final ResourceKey<DamageType> BLOOD_LET = registerDamage("blood_let");

    public static final ResourceKey<DamageType> RIP_STITCHES = registerDamage("rip_stitches");


    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\
    public static ResourceKey<DamageType> registerDamage(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name));
    }

    public static DamageSource getSource(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
    }
}
