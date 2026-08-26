package net.scruffy.dermicraft.datagen.datamaps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.property.BiofuelProperties;
import net.scruffy.dermicraft.property.ChainProperties;
import net.scruffy.dermicraft.property.EdibleFluidProperties;
import net.scruffy.dermicraft.property.SafetyModuleProperties;
import net.scruffy.dermicraft.property.ShatterHeadProperties;
import net.scruffy.dermicraft.property.WorkSpeedModuleProperties;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ModDataMapProvider extends DataMapProvider {
    /**
     * Create a new provider.
     *
     * @param packOutput     the output location
     * @param lookupProvider a {@linkplain CompletableFuture} supplying the registries
     */
    public ModDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {

        this.builder(ModDataMaps.BIOFUELS)
                .add(getResourceLocation(ModFluids.SOURCE_CRUDE_SLURRY),
                        new BiofuelProperties(.1f, 1f, .1f, 1), false)
                .add(getResourceLocation(ModFluids.SOURCE_CONCENTRATED_SLURRY),
                        new BiofuelProperties(.125f, .90f, .125f, 1), false)

                ;

        this.builder(ModDataMaps.EDIBLE_FLUID)
                .add(getResourceLocation(ModFluids.SOURCE_CRUDE_SLURRY),
                        new EdibleFluidProperties(250, 3, 0.1f), false)
                .add(getResourceLocation(ModFluids.SOURCE_PROTEIN_BLEND),
                        new EdibleFluidProperties(250, 5, 0.3f), false)
                .add(getResourceLocation(ModFluids.SOURCE_F_STUFF),
                        new EdibleFluidProperties(250, 4, 0.5f), false)

                ;

        // Iron is the BASELINE material every other chain is defined relative to, not merely one
        // entry among equals -- its numbers are the reference point, so a later material reads as
        // "+15% damage over iron" rather than needing its own from-scratch justification.
        //
        // - damage_multiplier 1.0: baseline by definition.
        // - bleed_chance 0.50 / decap_chance 0.20: deliberately NOT equal. Standard-hit Bleed was
        //   specified as a "high chance," standard-hit decapitation as ~20-25% -- two different
        //   bars, so equal values would collapse a real distinction. These carry the WHOLE chance
        //   rather than a bonus over some Sunder-side base, which is what makes the decided
        //   "broken chain = flat 0%" behavior fall out for free instead of needing an override.
        // - durability 250: matches IRON_SUNDER_CHAIN's own standalone-item durability, itself
        //   anchored to vanilla's iron-tool durability.
        // - tint #D8D8D8: Ferrous Blend's own tint color (see ModFluidTypes). Tint is deliberately
        //   NOT part of the iron-is-baseline idea -- it's independent per chain, each taking the
        //   tint of its equivalent fluid material, so there's no "base tint" other chains deviate
        //   from. Adjustable toward a more iron-like shade later.
        // - no status effect: signature traits are reserved for special-case materials.
        //
        // Numbers are first-pass and untuned -- see the design notes' open questions.
        // Copper/Gold/Diamond (2026-08-03, first pass, untuned) -- all defined relative to Iron's
        // baseline above, tint reused directly from each material's own equivalent fluid (Cuprous/
        // Aurous/Molten Diamond Blend, see ModFluidTypes) per the same "no independent base tint"
        // rule Iron's own comment explains.
        // - Copper: a below-Iron budget option -- lower damage and durability (softer metal than
        //   iron), but a slight Bleed edge (thin, easily-sharpened, wears fast) rather than being
        //   strictly worse across every stat.
        // - Gold: a side-grade, not a strict upgrade -- low durability (matches vanilla gold tools'
        //   own fragility), and a higher decapitation chance to compensate (soft metal takes a razor
        //   edge easily). Signature trait (2026-08-03): a 20% per-drop "weak Fortune/Looting" bonus
        //   on kill -- see ChainProperties' own javadoc and SunderEvents#onLivingDrops. No MobEffect
        //   status effect (that slot stays reserved) -- this material's signature trait lives in the
        //   dedicated loot_bonus_chance field instead, since bonus loot isn't a status effect.
        // - Diamond: a clear step up on all four base stats over Iron, per the design discussion.
        this.builder(ModDataMaps.SUNDER_CHAIN_PROPERTIES)
                .add(getResourceLocation(ModItems.IRON_SUNDER_CHAIN),
                        new ChainProperties(1.0f, 0.50f, 0.20f, 0.0f, 250,
                                TextColor.fromRgb(0xD8D8D8), Optional.empty(), 0.0f, 0, false), false)
                .add(getResourceLocation(ModItems.COPPER_SUNDER_CHAIN),
                        new ChainProperties(0.85f, 0.60f, 0.15f, 0.0f, 150,
                                TextColor.fromRgb(0xB87333), Optional.empty(), 0.0f, 0, false), false)
                .add(getResourceLocation(ModItems.GOLD_SUNDER_CHAIN),
                        new ChainProperties(0.90f, 0.45f, 0.30f, 0.20f, 100,
                                TextColor.fromRgb(0xD4AF37), Optional.empty(), 0.0f, 0, false), false)
                .add(getResourceLocation(ModItems.DIAMOND_SUNDER_CHAIN),
                        new ChainProperties(1.25f, 0.65f, 0.30f, 0.0f, 400,
                                TextColor.fromRgb(0x6FCFC4), Optional.empty(), 0.0f, 0, false), false)
                // Bone (2026-08-03) -- the renewable budget option (mob drops, no mining required),
                // distinct from Copper's own budget niche rather than a duplicate of it: lowest
                // damage and durability in the roster (brittle), but the highest Bleed chance of any
                // material (jagged shards, not a clean edge) -- pushes the same "fragile but bleeds
                // well" idea further than Copper does rather than sitting redundantly next to it.
                // Lowest decap chance too (jagged bone doesn't take a clean edge for decapitation the
                // way a honed blade does). No independent tint source (no equivalent fluid -- Bone
                // never gets a Mutator recipe of its own, see the recipe provider) -- a standalone
                // bone-ivory pick instead.
                .add(getResourceLocation(ModItems.BONE_SUNDER_CHAIN),
                        new ChainProperties(0.75f, 0.75f, 0.10f, 0.0f, 75,
                                TextColor.fromRgb(0xE3DAC9), Optional.empty(), 0.0f, 0, false), false)
                // Netherite -- the capstone above Diamond: a clear step up on all four base stats
                // over Diamond, same pattern Diamond itself used over Iron. No loot-bonus (that
                // stays Gold's signature); tint 0x4A3F4A (dark purplish gray) matches the tint
                // baked into netherite_sunder_chain.png's own icon.
                .add(getResourceLocation(ModItems.NETHERITE_SUNDER_CHAIN),
                        new ChainProperties(1.40f, 0.70f, 0.35f, 0.0f, 520,
                                TextColor.fromRgb(0x4A3F4A), Optional.empty(), 0.0f, 0, false), false)
                // Emerald -- a toned-down Diamond (all four base stats partway between Iron's and
                // Diamond's own), but with a stronger loot-bonus than Gold's 0.20 -- "valuable"
                // fits Emerald's identity too, deliberately sharing Gold's signature axis rather
                // than inventing a separate one. Tint 0x50C878 (real-world emerald green) matches
                // the tint baked into emerald_sunder_chain.png's own icon.
                .add(getResourceLocation(ModItems.EMERALD_SUNDER_CHAIN),
                        new ChainProperties(1.10f, 0.55f, 0.22f, 0.25f, 300,
                                TextColor.fromRgb(0x50C878), Optional.empty(), 0.0f, 0, false), false)
                // Blaze Essence -- ignite on hit, same 50%/4s as Shatter's identical trait on
                // standard hits; SAWING's pulses instead guarantee it every ~15-tick pulse (see
                // ChainProperties' own javadoc for why that alone gives "burns through the attack
                // plus 4s after"). smeltsLogs is this material's other trait -- SAWING's tree-felling
                // drops Charcoal (real SmeltingRecipe lookup, XP included) instead of raw Logs.
                // Baseline stats match Iron exactly -- mid-tier flavor material, not a power tier.
                // Tint 0xFFCC55 -- brightened from the Blaze Essence fluid's own 0xFFB13D, which
                // read too dark on the chain/head.
                .add(getResourceLocation(ModItems.BLAZE_ESSENCE_SUNDER_CHAIN),
                        new ChainProperties(1.0f, 0.50f, 0.20f, 0.0f, 250,
                                TextColor.fromRgb(0xFFCC55), Optional.empty(), 0.5f, 4, true), false)

                ;

        // Iron is the baseline material -- explicitly untinted (0xFFFFFF, a no-op vertex-colour
        // multiply) rather than the light-gray Sunder gives its own Iron chain, per the decision that
        // Iron's face/tail bones should show the bare grayscale texture as-authored.
        // Bone -- also untinted for now (its own item icon is hand-painted, not tinted at all -- see
        // ModItems.BONE_SHATTER_HEAD -- but the mounted weapon's face/tail bones still read this
        // entry, so it needs a real value too, not a null/missing one).
        //
        // Mining tiers (2026-08-12, decided) match vanilla's own pickaxe tier numbering
        // (Tiers#getLevel(): 0 = Wood/Gold, 1 = Stone, 2 = Iron, 3 = Diamond, 4 = Netherite) -- Bone
        // stands in for the Stone tier (no dedicated Wood-tier head exists), the rest match their
        // real-world vanilla counterpart directly. See ShatterItem#meetsMiningTier for the actual gate.
        //
        // Damage shifts (2026-08-12, decided) -- see ShatterHeadProperties#damageShift's own javadoc
        // for the reasoning; the actual target numbers (a flat +2 total damage margin kept over each
        // material's own vanilla pickaxe total) live on ShatterItem#BASE_ATTACK_DAMAGE. Iron = 0.0f
        // (the baseline, no modifier emitted -- see ShatterItem's own "skip the no-op" comment there).
        this.builder(ModDataMaps.SHATTER_HEAD_PROPERTIES)
                .add(getResourceLocation(ModItems.IRON_SHATTER_HEAD),
                        new ShatterHeadProperties(TextColor.fromRgb(0xFFFFFF), 2, 0.0f, 0.0f, 0.0f, 0, false), false)
                // Copper -- same tint as Sunder's own Copper chain, for visual consistency across the
                // two weapons' copper materials. Mining tier 2 and damage shift 0.0f both match Iron
                // exactly (2026-08-17, decided) -- Copper's only distinction from Iron is durability
                // (see ModItems.COPPER_SHATTER_HEAD), not tier or damage.
                .add(getResourceLocation(ModItems.COPPER_SHATTER_HEAD),
                        new ShatterHeadProperties(TextColor.fromRgb(0xB87333), 2, 0.0f, 0.0f, 0.0f, 0, false), false)
                // Stone Pickaxe totals 3 damage -- Bone's shift (-1.0f) brings Shatter to 5, a +2
                // margin, same margin every material keeps.
                .add(getResourceLocation(ModItems.BONE_SHATTER_HEAD),
                        new ShatterHeadProperties(TextColor.fromRgb(0xFFFFFF), 1, -1.0f, 0.0f, 0.0f, 0, false), false)
                // Same gold tint Sunder's own Gold chain uses, for visual consistency across the two
                // weapons' gold materials. Mining tier 0, same as vanilla's Gold Pickaxe (gold is a
                // soft metal -- fast digging, but no better than bare hands against tool-gated blocks).
                // Gold Pickaxe totals 2 damage -- shift (-2.0f) brings Shatter to 4, the same +2 margin.
                // lootBonusChance 0.20f -- Gold's signature trait, matching Sunder's own Gold chain
                // value exactly; see ShatterHeadProperties' own javadoc for why it's ore-restricted.
                .add(getResourceLocation(ModItems.GOLD_SHATTER_HEAD),
                        new ShatterHeadProperties(TextColor.fromRgb(0xD4AF37), 0, -2.0f, 0.20f, 0.0f, 0, false), false)
                // Also hand-painted, untinted -- same reasoning as Bone. Diamond Pickaxe totals 5
                // damage -- shift (+1.0f) brings Shatter to 7, the same +2 margin.
                .add(getResourceLocation(ModItems.DIAMOND_SHATTER_HEAD),
                        new ShatterHeadProperties(TextColor.fromRgb(0xFFFFFF), 3, 1.0f, 0.0f, 0.0f, 0, false), false)
                // Netherite -- treated as a metal (tinted shared texture, not its own hand-painted
                // one like Bone/Diamond), same tint as the Sunder chain's icon for cross-weapon
                // consistency. Mining tier 4 (above Diamond's 3). Netherite Pickaxe totals 6 damage
                // -- shift (+2.0f) brings Shatter to 8, the same +2 margin every material keeps.
                .add(getResourceLocation(ModItems.NETHERITE_SHATTER_HEAD),
                        new ShatterHeadProperties(TextColor.fromRgb(0x4A3F4A), 4, 2.0f, 0.0f, 0.0f, 0, false), false)
                // Emerald -- shares Diamond's own mining tier (3, per the design call) despite being
                // a toned-down Diamond otherwise. Shift +0.5f lands between Iron's 6 total and
                // Diamond's 7 total (6.5). Loot bonus 0.25, matching the chain's own value -- a
                // stronger version of Gold's identical 0.20 trait, not a separate specialty.
                .add(getResourceLocation(ModItems.EMERALD_SHATTER_HEAD),
                        new ShatterHeadProperties(TextColor.fromRgb(0x50C878), 3, 0.5f, 0.25f, 0.0f, 0, false), false)
                // Blaze Essence -- ignite on hit (50% chance, 4s burn) plus universal auto-smelt (any
                // block with a real SmeltingRecipe drops its smelted result instead, with matching
                // XP) -- treated as a mid-tier flavor material, not a power tier: baseline stats
                // match Iron exactly (tier 2, shift 0.0f), the value is entirely in the two special
                // traits. Tint 0xFFCC55 -- brightened from the Blaze Essence fluid's own 0xFFB13D,
                // which read too dark on the chain/head.
                .add(getResourceLocation(ModItems.BLAZE_ESSENCE_SHATTER_HEAD),
                        new ShatterHeadProperties(TextColor.fromRgb(0xFFCC55), 2, 0.0f, 0.0f, 0.5f, 4, true), false);

        // Thermal Safety Module grants exactly THERMAL (lava) -- the tag-vs-data split means
        // adding a Radiation/Biohazard Safety Module later is just another entry here, no new tag.
        this.builder(ModDataMaps.SAFETY_MODULE_PROPERTIES)
                .add(getResourceLocation(ModItems.HEAT_SAFETY_MODULE),
                        new SafetyModuleProperties(List.of(ModTags.Fluids.THERMAL)), false)
                .add(getResourceLocation(ModItems.METAPHYSICAL_SAFETY_MODULE),
                        new SafetyModuleProperties(List.of(ModTags.Fluids.METAPHYSICAL_MILD, ModTags.Fluids.METAPHYSICAL_SEVERE)), false)

                ;

        // Thermal Evolution Module: targets lava for a selector consumer (Drooling Cauldron) AND
        // grants THERMAL for a hazard-gated consumer (Masticator, eventually) -- same physical
        // item serves either shape depending on which machine's Module slot it sits in. Threshold
        // is an explicit placeholder (one Minecraft day of continuous active production), not a
        // tuned number -- see dermicraft-machine-notes.md.
        this.builder(ModDataMaps.EVOLUTION_MODULE_PROPERTIES)
                .add(getResourceLocation(ModItems.HEAT_EVOLUTION_MODULE),
                        new net.scruffy.dermicraft.property.EvolutionModuleProperties(
                                java.util.Optional.of(net.minecraft.world.level.material.Fluids.LAVA),
                                List.of(ModTags.Fluids.THERMAL),
                                24000), false)
                ;

        // Scoped to mobs with a real vanilla head/skull item already -- see the design notes.
        // Player excluded deliberately: a real player head needs profile NBT, not just an Item
        // reference, so it doesn't fit this data map's shape -- a separate mechanism if ever wanted.
        // Ender Dragon included despite being a one-time boss, not regular combat fodder -- it's
        // re-fightable, other mods already use Dragon Head for farming setups, and there may be a
        // future use for it here too.
        this.builder(ModDataMaps.DECAPITATION_HEADS)
                .add(getResourceLocation(EntityType.SKELETON), Items.SKELETON_SKULL, false)
                .add(getResourceLocation(EntityType.WITHER_SKELETON), Items.WITHER_SKELETON_SKULL, false)
                .add(getResourceLocation(EntityType.ZOMBIE), Items.ZOMBIE_HEAD, false)
                .add(getResourceLocation(EntityType.CREEPER), Items.CREEPER_HEAD, false)
                .add(getResourceLocation(EntityType.PIGLIN), Items.PIGLIN_HEAD, false)
                .add(getResourceLocation(EntityType.ENDER_DRAGON), Items.DRAGON_HEAD, false)

                ;

        // Flat 25% craft-speed bonus -- see WorkSpeedModuleProperties for why this also nets a real
        // fuel-per-craft savings, not just a time savings.
        this.builder(ModDataMaps.WORK_SPEED_MODULE_PROPERTIES)
                .add(getResourceLocation(ModItems.WORK_SPEED_MODULE), new WorkSpeedModuleProperties(1.25f), false)
                ;

    }

    private static ResourceLocation getResourceLocation(Supplier<FlowingFluid> fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid.get());
    }

    private static ResourceLocation getResourceLocation(net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> item) {
        return BuiltInRegistries.ITEM.getKey(item.get());
    }

    private static ResourceLocation getResourceLocation(EntityType<?> entityType) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
    }
}
