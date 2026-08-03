package net.scruffy.dermicraft.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Dermicraft.MOD_ID);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    public static final Holder<MobEffect> BLOOD_LET = MOB_EFFECTS.register("blood_let",
            BloodLet::new);

    public static final Holder<MobEffect> SUTURED = MOB_EFFECTS.register("sutured",
            SuturedEffect::new);

    public static final Holder<MobEffect> SPICY_REGRET = MOB_EFFECTS.register("spicy_regret",
            SpicyRegretEffect::new);

    public static final Holder<MobEffect> BLEED = MOB_EFFECTS.register("bleed",
            Bleed::new);

    public static final Holder<MobEffect> SCRENCH_OFF_BALANCE = MOB_EFFECTS.register("scrench_off_balance",
            ScrenchOffBalanceEffect::new);
}
