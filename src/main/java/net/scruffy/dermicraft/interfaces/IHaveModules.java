package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.component.BulkItemData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.property.SafetyModuleProperties;
import net.scruffy.dermicraft.property.WorkSpeedModuleProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Shared Module-loadout plumbing, generalized out of {@code EaterItem} (the first and, so far,
 * only implementation) per {@code dermicraft-progression-notes.md} -- see "Decision Points" ->
 * step 2 of the sequencing plan. See {@code dermicraft-gadget-notes.md} -> Gadget upgrade points
 * -> "Modules — direction note" for the design this backs: a fixed-size, item-tag-identified
 * loadout on a gadget's own {@link ItemStack}, field-swappable via the Scrench/Workbench, where a
 * Safety Module ({@link ModTags.Items#MODULE_SAFETY}) grants temporary hazard tolerance on top of
 * whatever the gadget's own permanent tier already tolerates.
 *
 * <p>Pure static-utility shape (no abstract methods to implement), matching {@link IHaveItemData}'s
 * own convention -- every method below takes its consumer's {@link DataComponentType} and slot
 * count explicitly rather than reading them off an instance, since several of Eater's own Module
 * checks run from {@code private static} contexts (per-tick scan loops) where there's no
 * {@code this} to dispatch through. <b>Each consumer must register its own distinct
 * {@code DataComponentType<BulkItemData>}</b> for its Module loadout -- never share one across two
 * gadgets, same "separate registration per consumer" rule {@code ModDataComponentTypes} already
 * follows for e.g. {@code EATER_MODE_DATA}/{@code DRINKER_MODE_DATA}.
 */
public interface IHaveModules {

    /** Exactly one Module per slot by default -- Modules aren't stackable resources. Not currently
     * overridden by any consumer; kept as a named constant rather than a bare {@code 1} at every
     * call site so a future stackable Module kind (if one is ever designed) has one place to widen. */
    int DEFAULT_MODULE_SLOT_CAPACITY = 1;

    /**
     * Whether {@code stack} currently has a Module tagged {@code moduleTag} installed in any of its
     * Module slots -- the generic capability-query dispatch the Modules direction note describes
     * (e.g. {@code hasModule(stack, ModTags.Items.MODULE_BEAM)}).
     */
    static boolean hasModule(ItemStack stack, DataComponentType<BulkItemData> moduleDataType,
                              int moduleSlotCount, TagKey<Item> moduleTag) {
        BulkItemData data = stack.getOrDefault(moduleDataType, BulkItemData.empty(moduleSlotCount));
        for (int i = 0; i < moduleSlotCount; i++) {
            ItemStack module = data.slot(i).asDisplayStack();
            if (!module.isEmpty() && module.is(moduleTag)) return true;
        }
        return false;
    }

    /**
     * Union of {@code base} with every hazard kind a currently-installed Safety Module grants --
     * see the Modules direction note's "derived capability" section. {@code base} is the
     * consumer's own PERMANENT hazard profile (e.g. {@link HazardProfile#TIER_1} for a consumer
     * with no real tier concept yet, or a gadget's actual current tier profile once one exists) --
     * Safety Modules are a LOCAL, temporary addition on top of whatever the consumer would already
     * tolerate, never a replacement for it. Recomputed on demand, not cached -- see the Modules
     * direction note's "derived capability is cached" backend-shape entry for why a future consumer
     * whose targeting runs every tick may want to cache this against its own slot-change hook
     * instead of calling this fresh each time, the way Eater currently does.
     */
    static HazardProfile installedHazardProfile(ItemStack stack, DataComponentType<BulkItemData> moduleDataType,
                                                 int moduleSlotCount, HazardProfile base) {
        BulkItemData data = stack.getOrDefault(moduleDataType, BulkItemData.empty(moduleSlotCount));
        HazardProfile profile = base;
        for (int i = 0; i < moduleSlotCount; i++) {
            profile = installedHazardProfile(profile, data.slot(i).asDisplayStack());
        }
        return profile;
    }

    /**
     * Single-slot core of the hazard union above, factored out so a consumer whose Module slot
     * isn't {@link BulkItemData}-backed at all can still get the identical rule. Machines are the
     * case this exists for: a machine's Module slot is one ordinary slot in its own
     * {@code ItemStackHandler} (persisted the same way every other machine slot already is), not a
     * gadget-style component on an {@link ItemStack} -- there's no {@link ItemStack} to hold a
     * {@link BulkItemData} component in the first place. {@code base.plus(hazard)}-per-tag, unioning
     * onto whatever {@code base} already tolerates -- same "never resets, only adds" rule as the
     * multi-slot overload.
     */
    static HazardProfile installedHazardProfile(HazardProfile base, ItemStack module) {
        if (module.isEmpty() || !module.is(ModTags.Items.MODULE_SAFETY)) return base;

        SafetyModuleProperties properties = BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                .getData(ModDataMaps.SAFETY_MODULE_PROPERTIES);
        if (properties == null) return base;

        HazardProfile profile = base;
        for (TagKey<Fluid> hazard : properties.hazards()) {
            profile = profile.plus(hazard);
        }
        return profile;
    }

    /**
     * Combined multiplier from every Work Speed Module currently present in {@code modules} (any
     * slot count -- deliberately list-based so this needs no changes when a machine gets more
     * Module slots later, e.g. a Charred machine's planned second slot). Diminishing-return
     * stacking: ranked strongest-to-weakest by each module's own bonus, the strongest contributes
     * its full value, and each next-strongest contributes at HALF the weight of the rank before it
     * (1, 1/2, 1/4, ...) -- half of its own POSITIONAL weight, not half of the previous module's
     * raw value, so the same formula produces the right answer for any mix of current or future
     * module tiers without special-casing. Returns 1.0f (no bonus at all) if none of the given
     * stacks are a real Work Speed Module.
     */
    static float workSpeedMultiplier(List<ItemStack> modules) {
        List<Float> bonuses = new ArrayList<>();
        for (ItemStack module : modules) {
            if (module.isEmpty() || !module.is(ModTags.Items.MODULE_WORK_SPEED)) continue;

            WorkSpeedModuleProperties properties = BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                    .getData(ModDataMaps.WORK_SPEED_MODULE_PROPERTIES);
            if (properties == null) continue;

            bonuses.add(properties.speedMultiplier() - 1f);
        }
        bonuses.sort(Collections.reverseOrder());

        float totalBonus = 0f;
        float weight = 1f;
        for (float bonus : bonuses) {
            totalBonus += bonus * weight;
            weight /= 2f;
        }
        return 1f + totalBonus;
    }

    /**
     * Index of the first slot in {@code gadgetStack}'s Module loadout holding an item tagged
     * {@code moduleTag}, or -1 if none. Generic capability-query dispatch for a consumer that needs
     * to know WHICH slot, not just whether one exists (e.g. to consume it afterward) -- see
     * {@link #clearModuleSlot}.
     */
    static int findModuleSlot(ItemStack gadgetStack, DataComponentType<BulkItemData> moduleDataType,
                               int moduleSlotCount, TagKey<Item> moduleTag) {
        BulkItemData data = gadgetStack.getOrDefault(moduleDataType, BulkItemData.empty(moduleSlotCount));
        for (int i = 0; i < moduleSlotCount; i++) {
            if (data.slot(i).asDisplayStack().is(moduleTag)) return i;
        }
        return -1;
    }

    /** Empties one Module slot on {@code gadgetStack} in place -- the consumed-on-trigger half of
     * a Keep-on-Death Module (Salvage), found via {@link #findModuleSlot} first. */
    static void clearModuleSlot(ItemStack gadgetStack, DataComponentType<BulkItemData> moduleDataType,
                                 int moduleSlotCount, int slotIndex) {
        BulkItemData data = gadgetStack.getOrDefault(moduleDataType, BulkItemData.empty(moduleSlotCount));
        gadgetStack.set(moduleDataType, data.withSlot(slotIndex, net.scruffy.dermicraft.component.BulkSlot.EMPTY));
    }

    /**
     * Builds a Module-slot row for a gadget's {@link IWorkbenchSwappable.SwapPanel}: one
     * {@link Slot} per Module slot, left-to-right from {@code (x, y)} at {@code spacing} pixels
     * apart, backed by the already-constructed {@code moduleHandler} (typically an
     * {@link IHaveItemData.BulkItemHandler} wrapped in {@link IHaveItemData#liveHandler}, filtered
     * to {@link ModTags.Items#MODULES} so the slot only accepts tagged Module items). Calls
     * {@code onSlotChanged} whenever a slot's contents actually change, so the caller can apply
     * whatever field-swap cost it uses (e.g. Eater's recalibration cooldown) without this method
     * needing to know what that cost is.
     */
    static List<Slot> buildModuleSlots(IItemHandlerModifiable moduleHandler, int moduleSlotCount,
                                        int x, int y, int spacing, BooleanSupplier active, Runnable onSlotChanged) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < moduleSlotCount; i++) {
            int slotX = x + i * spacing;
            slots.add(new SlotItemHandler(moduleHandler, i, slotX, y) {
                @Override
                public boolean isActive() {
                    return active.getAsBoolean();
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    onSlotChanged.run();
                }
            });
        }
        return slots;
    }
}
