package net.scruffy.dermicraft.event;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.scruffy.dermicraft.component.BulkItemData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.item.custom.DrinkerItem;
import net.scruffy.dermicraft.item.custom.EaterItem;
import net.scruffy.dermicraft.item.custom.ShatterItem;
import net.scruffy.dermicraft.item.custom.SippingItem;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Config;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.property.KeepOnDeathModuleProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keep-on-Death Modules (Salvage/Anchor -- see the Modules direction note) -- a gadget carrying
 * one is pulled out of the death-drop list entirely and restored directly into the respawned
 * player's inventory instead. No-ops entirely if the {@code keepInventory} gamerule is already on
 * (redundant, not harmful).
 *
 * <p>Two-phase, matching how death/respawn actually works: {@link #onLivingDrops} runs at the
 * moment of death (dying {@link Player} entity still exists, drops haven't spawned yet) -- it
 * intercepts the gadget's drop, applies the trigger cost (Salvage: consumes the Module; Anchor:
 * checks/starts the configurable cooldown), and stashes the stack on the dying player's own
 * {@link Player#getPersistentData()}. {@link #onPlayerClone} runs at respawn, reads that same NBT
 * off {@code event.getOriginal()} (the about-to-be-discarded dying player instance -- persistent
 * data on it is still valid at this point), and hands the stack(s) to the new player instance.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class GadgetDeathPersistenceEvent {

    private static final String SAVED_ITEMS_KEY = "dermicraft_keep_on_death_items";
    private static final String ANCHOR_COOLDOWN_KEY = "dermicraft_anchor_module_cooldown_until";

    /** Which Module data component + slot count a gadget item reads, dispatched by concrete Item
     * class -- one entry per Module-slot-bearing gadget. Extend this when a new gadget gets a
     * Module slot; nothing else in this class needs to change. */
    private record ModuleSlotInfo(DataComponentType<BulkItemData> dataType, int slotCount) {
    }

    private static Optional<ModuleSlotInfo> moduleSlotInfo(ItemStack gadgetStack) {
        if (gadgetStack.getItem() instanceof EaterItem) {
            return Optional.of(new ModuleSlotInfo(ModDataComponentTypes.MODULE_DATA.get(), EaterItem.MODULE_SLOT_COUNT));
        }
        if (gadgetStack.getItem() instanceof DrinkerItem) {
            return Optional.of(new ModuleSlotInfo(ModDataComponentTypes.DRINKER_MODULE_DATA.get(), DrinkerItem.MODULE_SLOT_COUNT));
        }
        if (gadgetStack.getItem() instanceof SippingItem) {
            return Optional.of(new ModuleSlotInfo(ModDataComponentTypes.SIPPING_MODULE_DATA.get(), SippingItem.MODULE_SLOT_COUNT));
        }
        if (gadgetStack.getItem() instanceof SunderItem) {
            return Optional.of(new ModuleSlotInfo(ModDataComponentTypes.SUNDER_MODULE_DATA.get(), SunderItem.MODULE_SLOT_COUNT));
        }
        if (gadgetStack.getItem() instanceof ShatterItem) {
            return Optional.of(new ModuleSlotInfo(ModDataComponentTypes.SHATTER_MODULE_DATA.get(), ShatterItem.MODULE_SLOT_COUNT));
        }
        return Optional.empty();
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        List<ItemStack> saved = new ArrayList<>();

        event.getDrops().removeIf(itemEntity -> {
            ItemStack stack = itemEntity.getItem();
            Optional<ModuleSlotInfo> infoOpt = moduleSlotInfo(stack);
            if (infoOpt.isEmpty()) return false;
            ModuleSlotInfo info = infoOpt.get();

            int slot = IHaveModules.findModuleSlot(stack, info.dataType(), info.slotCount(), ModTags.Items.MODULE_KEEP_ON_DEATH);
            if (slot < 0) return false;

            BulkItemData moduleData = stack.getOrDefault(info.dataType(), BulkItemData.empty(info.slotCount()));
            ItemStack moduleStack = moduleData.slot(slot).asDisplayStack();
            KeepOnDeathModuleProperties props = BuiltInRegistries.ITEM.wrapAsHolder(moduleStack.getItem())
                    .getData(ModDataMaps.KEEP_ON_DEATH_MODULE_PROPERTIES);
            if (props == null) return false;

            if (props.consumed()) {
                IHaveModules.clearModuleSlot(stack, info.dataType(), info.slotCount(), slot);
            } else {
                long cooldownTicks = Config.ANCHOR_MODULE_COOLDOWN_SECONDS.get() * 20L;
                if (cooldownTicks > 0) {
                    CompoundTag data = player.getPersistentData();
                    long now = player.level().getGameTime();
                    if (now < data.getLong(ANCHOR_COOLDOWN_KEY)) return false; // still on cooldown, drop normally
                    data.putLong(ANCHOR_COOLDOWN_KEY, now + cooldownTicks);
                }
            }

            saved.add(stack.copy());
            return true;
        });

        if (saved.isEmpty()) return;

        HolderLookup.Provider registries = player.level().registryAccess();
        ListTag list = new ListTag();
        for (ItemStack stack : saved) {
            list.add(stack.save(registries));
        }
        player.getPersistentData().put(SAVED_ITEMS_KEY, list);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        CompoundTag oldData = event.getOriginal().getPersistentData();
        if (!oldData.contains(SAVED_ITEMS_KEY)) return;

        Player respawned = event.getEntity();
        HolderLookup.Provider registries = respawned.level().registryAccess();
        ListTag list = oldData.getList(SAVED_ITEMS_KEY, CompoundTag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(registries, list.getCompound(i)).ifPresent(stack -> {
                if (stack.isEmpty()) return;
                if (!respawned.getInventory().add(stack)) {
                    respawned.spawnAtLocation(stack);
                }
            });
        }

        oldData.remove(SAVED_ITEMS_KEY);
    }
}
