package net.scruffy.dermicraft.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.scruffy.dermicraft.block.entity.custom.MrHunterBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes the loot and XP of a kill Mr. Hunter made, so they go to the machine instead of the ground.
 *
 * <p>Mr. Hunter's kill is a player's attack (a fake player's), so the mob dies the ordinary way, with the ordinary loot table,
 * the player-only drops and the XP. A death fires its events synchronously inside the damage call, so the machine marks the mob
 * it is striking ({@link #begin}) just before the hit and clears it ({@link #end}) just after; an event for that mob and no
 * other is the machine's. The events run last, so anything another mod adds to the drops is included.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class HunterKillEvents {

    @Nullable
    private static MrHunterBlockEntity currentHunter = null;
    @Nullable
    private static LivingEntity currentTarget = null;

    private HunterKillEvents() {}

    /** Marks {@code target} as being struck by {@code hunter}, until {@link #end}. */
    public static void begin(MrHunterBlockEntity hunter, LivingEntity target) {
        currentHunter = hunter;
        currentTarget = target;
    }

    public static void end() {
        currentHunter = null;
        currentTarget = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        MrHunterBlockEntity hunter = currentHunter;
        if (hunter == null || event.getEntity() != currentTarget) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            if (!drop.getItem().isEmpty()) stacks.add(drop.getItem().copy());
        }
        event.getDrops().clear(); // nothing spawns in the world: the machine has it all
        hunter.acceptKillLoot(level, event.getEntity().blockPosition(), stacks);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingExperience(LivingExperienceDropEvent event) {
        MrHunterBlockEntity hunter = currentHunter;
        if (hunter == null || event.getEntity() != currentTarget) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        int xp = event.getDroppedExperience();
        event.setDroppedExperience(0); // no orbs: the machine takes it as fluid
        hunter.acceptKillXp(level, event.getEntity().position(), xp);
    }
}
