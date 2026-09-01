package net.scruffy.dermicraft.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.scruffy.dermicraft.block.custom.ModLiquidBlock;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Hot fluids ignite anything directly touching them -- an item falling in, a mob wading through --
 * the same rule Lava already follows, not a separate fire mechanic of our own. {@link
 * Entity#lavaHurt()} is the exact method vanilla's own {@code isInLava()} check calls internally, so
 * reusing it here gets every one of vanilla's own protections against it for free: Fire Resistance,
 * fire-immune entity types (Blazes, Striders, etc. -- see {@link Entity#fireImmune()}), and the
 * standard fire damage/ignite numbers, all without duplicating any of that logic ourselves.
 *
 * <p>Scoped to only THIS mod's own hot fluids via {@link ModLiquidBlock#isHotDermicraftFluid} --
 * vanilla Lava (a real temperature of 1300) would otherwise also match a bare temperature check,
 * which would double up with vanilla's own separate lava-contact handling and hurt/ignite an entity
 * standing in ordinary Lava twice over. Every other mod's fluids are excluded the same way.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class FluidHazardEvents {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.isInFluidType((type, height) -> ModLiquidBlock.isHotDermicraftFluid(type), false)) {
            entity.lavaHurt();
        }
    }
}
