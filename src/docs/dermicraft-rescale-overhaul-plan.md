# Fluid Rescale and Rename Overhaul: Implementation Plan

Written 2026-11-05. Design is SETTLED; this document is the build order for the code pass (done in a separate session, user writes the code). Source of truth for the reasoning is the memory file `project_idea_backlog.md` (items 5, 6, 7 and the DECIDED sections). If this file and a later user ruling disagree, the ruling wins.

No old-world compatibility: no migration, aliasing or legacy ids. The user starts a new world after this.

---

## Phase 0: Prep (before touching anything)

1. Commit or stash current work so the rename diff is isolated.
2. Grep inventory, saved to scratch, so nothing is missed:
   - `Blend` / `BLEND` (ids, constants, classes, lang, textures, tags)
   - `Essence` / `ESSENCE`
   - `RAW_NETHERITE_SCRAP` / `raw_netherite_scrap`
   - `SOURCE_<FLUID>` for each renamed fluid (catches extras like `GlassFlaskItem` per-fluid dispatch)
   - `PULP_BLEND` / `pulp_blend` (the wood fluid)
3. Read ModRecipeProvider top to bottom once and list every literal that is a multiple of 1000 or a known old value (1000, 9000, 4000, 3000, 750, 900, 500, 375, 250, 330, 334). These are the rescale targets.

---

## Phase 1: Constants

Add named constants in one shared place (suggest the recipe-builder or a new `MbScale` class):

| Constant | Value |
|---|---|
| `INGOT` | 90 |
| `NUGGET` | 10 |
| `BLOCK` | 810 |

Rules to encode (document in the class comment):
- Rounding is flat round-UP everywhere, costs AND yields. The Clay Ball/Block style 2 mB loop is accepted; do not guard it.
- Item with a storage block: item = 90, block = 9:1 -> 810, 4:1 -> 360.
- Plain (non-storage) blocks: NO flat rate. Per family (Phase 3): stone family 360, cobble 270, sand / gravel / netherrack 90. Wood is decided separately and is still pending.
- Slab = half of the block it is made from. Stairs = 1:1 with the block they are made from (stonecutter rule; wood stairs follow the same rule).
- Functional crafted blocks stay ingot-priced (Hopper 450, Cauldron 630, Heavy Weighted Pressure Plate 180, Iron Bars 34 each), not flat 810.

---

## Phase 2: Renames (code, not only lang)

Do this BEFORE the rescale so every later edit uses the new names. Rename registry ids, Java constants/fields (`SOURCE_*`, `*_BUCKET`, `FLOWING_*`, `*_BLOCK`), class and variable names, texture and model file names, datagen providers, tags, recipe ids, data maps. Then `./gradlew runData` and check the `src/generated/resources` diff; delete orphaned old generated files.

| Old | New |
|---|---|
| X Blend (all 9: Calcium, Carbon, Protein, Stone, Silica, Clay, Ferrous, Cuprous, Aurous) | X Pulp |
| Pulp Blend (dead-wood dupe fluid) | Cellulose Pulp |
| Knowledge / Blaze / Ghast / Wither / Ender Essence | X Colloid |
| Molten Raw Netherite Scrap (`molten_raw_netherite_scrap`) | Dirty Molten Netherite (`dirty_molten_netherite`) |

Follow-on name changes:
- Item names that embed the fluid DROP the word entirely: "Blaze Essence Shatter Head" -> "Blaze Shatter Head", likewise the Sunder Chain and any similar item.
- "Knowledge Essence" -> "Knowledge Colloid" everywhere: XP gauge, slot tooltip ("Knowledge Colloid Container"), knowledge vat tooltip.
- Update docs that name the old fluids (see Phase 9).
- Gotchas: lang keys (`item.dermicraft.*_bucket`, `fluid_type.dermicraft.*`), item-model textures for buckets (shared overlay, so only the tint registration and lang change), `ModClientEvents.registerBucketTint`, `DermicraftClient` registrations, creative tab entries, `ModDataMapProvider` (BIOFUELS / EDIBLE_FLUID), both fluid-tag and item-tag mirrors.
- Dirty Molten Netherite does NOT use the contamination rule; it simply never mixes with Molten Netherite.

Checkpoint: `./gradlew build` compiles, `runData` clean, game launches, all buckets render with the right tint.

---

## Phase 3: Direct conversions (item <-> mB)

Every Masticator, Metastasizer (duplicate) and Render Kiln recipe for these materials is re-denominated. Craft times are unchanged unless noted.

**Metals (Ferrous, Cuprous, Aurous Pulp):** ingot 90, nugget 10, block 810. Nugget 9 -> 1 ingot becomes exactly 90 (drops the old 110 / 1000 mismatch). Iron Bars 34 each (6 ingots / 16, rounded up). Hopper 450, Cauldron 630, Bucket 270, etc.: re-derive every ingot-priced crafted item from its ingot count.

**Netherite:** Netherite Ingot 90, Netherite Block 810 (new masticate / duplicate / Render Kiln recipes needed; none exist today). Netherite Scrap -> Dirty Molten Netherite 90, lava input on the scrap masticate 90 (1:1).

**Other metal-denominated recipes:** REMOVE `mutating_gilded_blackstone` (550 Aurous, ~line 1533) for now; the user will revisit it later.

**Coal / Charcoal (Carbon Pulp):** 90 each, Coal Block 810.

**Bone (Calcium Pulp):** Bone Meal 90, Bone Block 810, Bone 270 (3x bone meal), Skeleton Skull 810. The 330-vs-334 hack and the batch-of-3 recipe go away (3 x 90 = 270 = Bone). Add Bone Block recipes (masticate, metastasize, Render Kiln); none exist today. The Calcium Pulp puddle recipe is REMOVED (see Phase 5, Puddle Craft recipes).

**Molten gems (all get the ratio):** Diamond, Emerald, Lapis, Redstone, Quartz, Glowstone, Amethyst, Soul Silica.
- 9:1 materials (Diamond, Emerald, Lapis, Redstone): item 90, block 810.
- 4:1 materials (Quartz, Glowstone Dust, Amethyst Shard): item 90, block 360.
- Soul Sand -> Molten Soul Silica: 810 (a non-storage block).
- Amethyst shard and block masticate / Metastasizer dupes: 1000 / 4000 -> 90 / 360.

**Stone / Silica / Clay families (Stone Pulp, Silica Pulp, Clay Pulp):**
- REVISED 2026-11-05 (supersedes the 810 / 630 / 720 gradient). 360 (4 units, because Pointed Dripstone : Dripstone Block is 4:1): Stone, Smooth Stone, Andesite, Diorite, Granite, Deepslate, Calcite, Tuff, Dripstone Block, Sandstone, Red Sandstone (4 sand), Terracotta (all colors, plain and glazed). Walls of these: 360.
- 270 (3 units): Cobblestone, Cobbled Deepslate, and their walls.
- 90 (1 unit): Sand, Red Sand, Gravel, Pointed Dripstone (4 per Dripstone Block), Glass (1 sand smelted). Glass Pane 30 (deliberately a little under the 33.75 parity, decorative-cheap).
- Slabs = half their own block (180 for the 360 family, 135 for cobble / cobbled deepslate). Stairs = 1:1 with their block (360 / 270); Stairs currently have masticate recipes only.
- Clay: Clay Ball and Brick 90; Clay Block and Bricks 360 (treated as storage blocks, confirmed); Flower Pot 270.
- Tank check: a 5000 mB tank holds about 13 stone blocks, a Charred tank about 27 (user wanted "more than a few", not a full stack).

**Netherrack family (new, feeds Nether Ichor), REVISED 2026-11-05:** Netherrack = 90 (1 unit). It smelts to the Nether Brick item = 90; 4 bricks = Nether Bricks block = 360; Nether Brick Slab = 180 (half). Netherrack gives 90 mB Nether Ichor (the earlier 810 block rate is superseded). Needs the Masticator recipe (Phase 5), a Render Kiln reverse (90 Nether Ichor -> 1 Netherrack) and a Metastasizer duplicate (pattern Netherrack, 90 Nether Ichor). Nether Brick Slab 180, Stairs 360, Wall 360 and Fence 180 (2 Nether Bricks + 4 Nether Brick items make 6 fences = 1080 / 6) all get recipes (user confirmed). The supply is Nether-only: every mB of Nether Ichor originates from mined netherrack, and conversions consume it, so the gate is ongoing, not first-trip.

**Wood (Cellulose Pulp), DECIDED 2026-11-06: Log = 360, Plank = 90.** A log is 4 planks (the same 4:1 shape as Dripstone Block / Pointed Dripstone), so every value is a multiple of 45 and nothing rounds; 4 planks cost exactly one log. Slab = half a plank; Stairs = 1 plank (the 1:1 stair rule). All four log variants (Log, Wood, Stripped Log, Stripped Wood) cost the same (vanilla's 4 logs -> 3 wood is deliberately ignored). Times are UNCHANGED from the current tiers (log 10 s, plank 2.5 s, etc.), except Stairs use the plank's 2.5 s instead of 3.75 s; Bow also moves off `stairsTicks` (it used the 375 mB stairs tier) to 3 sticks' worth. Nothing consumes wood pulp except the duplicate and recycling recipes (verified by grep: no reagent, fuel or data-map use).

| Item | Basis | Old | New |
|---|---|---|---|
| Log / Wood / Stripped Log / Stripped Wood | 4 planks | 1000 | 360 |
| Planks | 1 unit | 250 | 90 |
| Slab | 1/2 plank | 125 | 45 |
| Stairs | 1 plank | 375 | 90 |
| Stick | 1/2 plank | 125 | 45 |
| Button | 1 plank | 250 | 90 |
| Door | 2 planks | 500 | 180 |
| Pressure Plate | 2 planks | 500 | 180 |
| Trapdoor | 3 planks | 750 | 270 |
| Fence Gate | 2 planks + 4 sticks | 1000 | 360 |
| Fence (batch of 3) | 4 planks + 2 sticks | 1250 | 450 |
| Fence (single) | just under 1/3 of the batch (150) | 400 | 140 |
| Bow | 3 sticks | 375 | 135 |
| Crafting Table | 4 planks | 1000 | 360 |
| Chest (and the other 2000 mB chest-family line) | 8 planks | 2000 | 720 |
| Paper (NEW, no recipe exists today) | decorative-cheap; 3 paper = 1 plank | none | 30 |

Paper (added 2026-11-06): joins the wood family even though sugar cane is not wood (other mods make paper from wood, so there is precedent). New Metastasizer duplicate (`metastasizing_paper`, pattern Items.PAPER) and a Masticator recycle (`pulp_blend_recycling_paper`), both 30 mB Cellulose Pulp. Craft time `halfPlankTicks` (1.25 s), user accepted.

**Glass family (DECIDED 2026-11-06), Silica Pulp unless noted, craft times unchanged:**

| Item | Old | New | Note |
|---|---|---|---|
| Glass Block | 1000 | 90 | duplicate + Masticator recycle |
| Glass Pane | 500 | 30 | duplicate + recycle |
| Stained Glass Block (16 colors) | 1000 | 90 | duplicate; `mutating_stained_glass_*` takes dye + 90 Silica |
| Stained Glass Pane (16 colors) | 500 | 30 | duplicate |
| Calcium Glass | 1000 | 90 | duplicate with Calcium Pulp |
| Beaker (Silica and Calcium recipes) | 1000 | 90 | matched to Glass Block |
| Glass Flask (Silica and Calcium recipes) | 250 | 20 | sized from the Beaker's volume |
| Glass Bottle (vanilla, NEW) | none | 30 | new duplicate; recycle also assumed (same 30 tier as Pane) |

- REMOVE `calcium_glass_puddle` (bone meal + Calcium Pulp puddle, ~line 1195).
- ADD a Metastasizer recipe that copies a plain Glass Block into Calcium Glass with Calcium Pulp: pattern `Items.GLASS` (non-consumed), 90 Calcium Pulp, result Calcium Glass; use `RecipeBuilders.buildMetastasizing(...)` with an `ItemStack` result (the fence recipe is the precedent for a result differing from a plain duplicate). Cost matches regular glass (90). Craft time solidTicks (10 s). Keep the existing `metastasizing_calcium_glass` duplicate at 90.
- Bottle craft time is unspecified; suggest the Pane's `aggregateTicks` (placeholder).

Recipes to touch (ModRecipeProvider ~1669-1781): `pulp_blend_masticating` (logs), the `pulp_blend_recycling_*` family (planks, slabs, stairs, doors, trapdoors, fence gates, stick, bow, pressure plates, buttons, fence, single fence), the per-species `metastasizing_<species>_*` loop, `metastasizing_stick`, chest and crafting table (both directions). Remember the fence batch recipes use `itemAmount = 3`.

**NOT rescaled:**
- Protein Pulp (values come from the food).
- Knowledge Colloid / XP values (vanilla XP mechanics).
- F-Stuff.
- Crude Slurry and the later slurries.
- Dyes use their own rule (see Phase 5).

**Metastasizer sediment duplication costs** (see memory `project_metastasizer_sediment_costs`) must be recomputed in the new units, not hand-scaled.

Checkpoint: spot-check a round trip per family in-game (masticate -> duplicate cost -> Render Kiln) and confirm no mB value in a recipe is a stale 1000-scale literal.

---

## Phase 4: New fluids (full CLAUDE.md checklist for EACH)

Five new registered fluids (Flux, Luminous Ichor, Nether Ichor, and the three Dirty Pulps counted as one family). Per fluid: `ModFluidTypes`, `ModFluids` (source, flowing, LiquidBlock, bucket, Properties), `ModItemModelProvider` (chunky or thin bucket), `ModClientEvents.registerBucketTint`, `DermicraftClient` (`renderTranslucentFluid` AND `registerFluidType`), `ModFluidTagProvider` (THICK/THIN only; hazards are no longer tags), `ModDataMapProvider` FLUID_HAZARD entry for Radiation / Biohazard / Metaphysical intensity (Thermal comes from the FluidType's temperature, so set it honestly), `ModItemTagProvider` mirrors, `ModDataMapProvider` if relevant, a survival recipe, lang keys, `ModCreativeModeTabs` (bucket + filled Beaker). Then `runData`.

| Fluid | Look | Notes |
|---|---|---|
| **Flux** | thin water-style texture (`thinBucketItem`), tint 0xFFB5E4EE (pale ice cyan), color vector (181, 228, 238) | FINAL 2026-11-06. viscosity 1500, density 1500 (inert), temperature 295 (keep near ambient; Thermal reads it), motion scale 0.025. THIN tag. No hazards. The earlier pale white 0xFFE4EEF0 was dropped: it sat within 13 per channel of Ghast Essence (0xFFEDEDE3). |
| **Dirty Ferrous Pulp** | chunky bucket, SHARED speckle-overlay dirty still/flowing textures (new art, the user will make them), tint 0xFFB5B3B1 | FINAL 2026-11-06 (25% slag mix, shared slag 0x4A423C). viscosity 3000, density 2600 (inert), temperature 290, motion scale 0.012, fog = tint. THICK tag, no hazards. |
| **Dirty Cuprous Pulp** | tint 0xFF9D6735 | same FluidType values and shared dirty textures as Dirty Ferrous |
| **Dirty Aurous Pulp** | tint 0xFFB29438 | same FluidType values and shared dirty textures. The old 50% slag tints (0x918D8A, 0x815A37, 0x8F783A) are superseded: Dirty Ferrous fell between Stone and Clay Pulp and Dirty Cuprous sat on F-Stuff, which the speckle overlay now resolves. |
| **Luminous Ichor** | chunky bucket, tint 0xFFF0B46A (amber-peach), darker fog of same hue | emissive: light 10 on BOTH the FluidType and the LiquidBlock; viscosity ~2500, density ~1500, temp 310, normal motion scale. Fluid tag THICK. Hazard is a FLUID_HAZARD data-map entry, `FluidHazardData.radiation(~50)` (Mild band 1-99), NOT a tag. Not Thermal (temp 310), not BIOFUELS. Adding Biohazard too is an open question (see the backlog). |
| **Nether Ichor** (`nether_ichor`) | chunky bucket; look/tint TBD (flesh-like, reddish-brown; to choose) | Made from Netherrack + Protein Pulp. Not Thermal (ambient temperature; Soul Silica is made without lava). Hazard OPEN: possibly `FluidHazardData.biohazard(intensity)` in the data map (user is considering Biohazard; see the backlog), otherwise none. Consumed in conversions. Not a formal family with Luminous Ichor (user unsure), but "Ichor" = flesh-like. |

Dirty Pulps have no other uses than the refining forks; their survival reachability is the Masticator route in Phase 5.

---

## Phase 5: Recipes (mixed fluids and the rest)

All Effluentcer ("effluencing") recipes take exactly two input fluids. Negative tick values mean ticks per 100 mB of output; do not use them in new recipes.

**C-Stuff (3 recipes, all 200 ticks):**
- Effluencing: 90 Carbon Pulp + 90 Calcium Pulp -> 180 C-Stuff.
- Masticator: Coal OR Charcoal + 90 Calcium Pulp -> 180 C-Stuff.
- Masticator: Bone Meal + 90 Carbon Pulp -> 180 C-Stuff.

**Primitive Catalyst:** 180 F-Stuff + 180 C-Stuff -> 360, 15 s (300 ticks).

**Reinforcing Catalyst:** 180 C-Stuff + 18 Primitive Catalyst -> 180, 180 ticks. Chassis cost unchanged (Iron Bars + 1000 mB Reinforcing Catalyst).

**Synapse Catalyst:** 45 Cuprous Pulp + 45 Primitive Catalyst -> 750, 45 s. Consumers unchanged.

**Kinetic Catalyst:** 45 Molten Redstone + 45 Molten Quartz -> 750, 45 s. Its only remaining consumer is the Work Speed Module bucket.

**Luminous Ichor (new):**
- Masticator: 1 Glowstone Dust + 90 Protein Pulp -> 135 Ichor, 15 s.
- Effluencing: 90 Molten Glowstone + 90 Protein Pulp -> 180 Ichor, 15 s.
- Times are placeholders; tune in playtest.

**Evolution Catalyst (rewritten):** 500 Knowledge Colloid + 500 Luminous Ichor -> 1000 Evolution Catalyst. The old Kinetic-based recipe is removed. Consumers (Anchor / Heat Evolution / Safety Modules, Evolution Module Frame, all 1000 mB) are unchanged. The Knowledge+EC recipe near line ~1190 must be re-checked.

**Molten Amethyst (rewritten):**
- Effluencing: 90 Molten Quartz + 10 Luminous Ichor -> 90 Molten Amethyst, 15 s. Delete the stale "deliberately NOT cheap / Evolution Catalyst pays for rarity" comment.
- Masticator alternate: 1 Nether Quartz + 10 Ichor -> 68, same time.
- Molten Amethyst gets Thermal only (no Radiation Mild); in-fiction the radiation is used up. The Effluencing route has Thermal + Radiation Mild inputs, which is fine now that the one-hazard rule is dropped.

**Molten Netherite:** 360 Dirty Molten Netherite + 360 Aurous Pulp -> 90 Molten Netherite, 60 s.

**Concentrated Slurry:** 50 Crude Slurry + 25 Lava -> 50 Concentrated Slurry, 50 ticks. No Primitive Catalyst; lava is consumed. Replace the stale 10:1 Primitive Catalyst comment. Output stays untagged.

**Nether Ichor and the Silica -> Quartz route (REDESIGNED 2026-11-05, replaces the old single `molten_quartz_effluencing`).** Purpose of the redesign: remove the Nether-free quartz route. Nether Ichor is the Nether-gated converter for turning overworld fluids into Nether-gated ones; Soul Silica is its first use. No Silica -> Soul Silica recipe exists in code today; the only Soul Silica source is Soul Sand (`molten_soul_silica_masticating`), and Molten Soul Silica has no consumer yet.
- Nether Ichor (Masticator): 1 Netherrack + 90 Protein Pulp (solvent = yield, the masticateWithWater convention) -> 90 Nether Ichor. Time 15 s is a placeholder. Protein is the solvent per the user ("change later if it becomes a problem"). Protein is not rescaled (food-derived).
- Silica -> Soul Silica (Effluentcer, NO lava): Silica Pulp + Nether Ichor -> Molten Soul Silica. ACCEPTED 2026-11-05 ("change later if necessary"): 180 Silica + 10 Nether Ichor -> 180 Molten Soul Silica, 45 s (time placeholder). Ichor is consumed; output mirrors the Silica amount (Molten Amethyst precedent).
- Soul Silica -> Quartz (Effluentcer): Molten Soul Silica + Lava -> Molten Quartz. ACCEPTED 2026-11-05: 180 Soul Silica + 90 Lava -> 90 Molten Quartz, 45 s (time placeholder). Lava consumed. Cost per quartz = 2 sand (sand is now 90) + 1/9 netherrack + 90 lava.
- Rescaled from the earlier 1260 / 14-sand version because sand dropped from 630 to 90.
- Hazards: multiple hazards are allowed (see Phase 7). Molten Soul Silica is Thermal + Metaphysical Mild and Molten Quartz is Thermal; no special handling.
- Knock-on: every Molten Quartz route is now Nether-gated (Kinetic Catalyst, Molten Amethyst Effluencing). Nether Quartz itself is still masticatable for the Amethyst alternate route.

**Refining chain (the ore -> ingot work):**
1. Flux (Effluentcer): 180 C-Stuff + 180 Silica Pulp -> 360 Flux, 15 s (300 ticks).
2. Masticator: 1 Raw Iron/Copper/Gold + 25 Flux -> 90 Dirty [metal] Pulp, 45 s. This replaces the old one-step Primitive Catalyst recipe.
3. Quick fork (Render Kiln): 68 Dirty Pulp -> 1 Ingot, 200 ticks.
4. Patient fork (Effluentcer): 90 Dirty Pulp + 45 Flux -> 180 clean Pulp, 900 ticks fixed.
- Flux cost per Raw: 25 (dirty route) or 70 (full clean).
- Ingot-to-pulp (clean) recipes stay as direct conversions from Phase 3.

**Ferrous Blood (iron from protein):** Effluentcer, 900 Protein Pulp + 25 Flux -> 90 Dirty Ferrous Pulp, 45 s (900 ticks). Iron only; no copper/gold analog. Reuses the downstream forks above unchanged. Delete `ferrous_blend_masticating_blood_nugget` and `ferrous_blend_effluencing_blood_nugget`.

**Blood Nugget (kept, now a food):**
- Food values: nutrition 3, saturation 1.7 (masticates back to exactly 250 mB Protein).
- Add to the MEAT_FOOD item tag in `ModItemTagProvider`.
- `metastasizing_blood_nugget` becomes a plain duplicate with the nugget itself as the pattern: 250 Protein Pulp, 96 ticks (`RecipeBuilders.duplicate(...)`).
- `render_kiln_blood_nugget` kept, 250 Protein, set to 96 ticks.

**Dyes:** all 16 dye Metastasizer duplications flat 10 mB (White Calcium, Black Carbon, Gray Stone, Light Gray C-Stuff, the other 12 Crude Slurry). Craft times unchanged.

**Reagent audit rulings (DECIDED 2026-11-06).** Mechanical rescales by the ingot rule: Golden Apple 540, Golden Carrot and Glistering Melon Slice 60 each, Heavy and Light Weighted Pressure Plate 180, Cauldron 630, Hopper 450, Colored Terracotta (Mutator) 360, Glazed Terracotta (Mutator) 180. Remaining items:
- **Copper family, fixed to the vanilla ratios** (the old "cut copper = block" price came from missing information). Copper Block 810; Cut Copper 203 (4 blocks make 16, so 202.5 rounded up); Cut Copper Stairs 304 (6 cut make 4, 1.5 cut each); Cut Copper Slab 102 (3 cut make 6); Chiseled Copper 203 (2 cut copper slabs, about 1 cut copper); Copper Grate 810 (4 blocks make 4); Copper Door 180 (6 ingots make 3); Copper Trapdoor 360 (4 ingots make 1; the old 2000 was wrong). Do both directions (Masticator and Metastasizer mirror each other). Craft times: scale by the same ratio to the Copper Block's 90 s, i.e. amount / 9 seconds (user agreed): Cut Copper 22.5 s, Stairs 33.75 s, Slab 11.25 s, Chiseled 22.5 s, Grate 90 s, Door 20 s, Trapdoor 40 s.
- **Primitive -> Iron tool upgrades (Mutator, ~1465-1477):** REMOVE `mutating_suture_kit` (the kit needs string in the crafting grid; a new Flesh Lab recipe will replace it, to be designed later). Forceps cost matches the crafting-table recipe (4 Iron Ingots + 1 Nugget = 370 mB Ferrous Pulp; forceps do not break, so the higher cost is fine). Scalpel Primitive upgrade: 10 mB Ferrous (crafting from scratch is 3 nuggets = 30, and an upgrade should be cheaper). Syringe Primitive upgrade: 45 mB Ferrous (stays alongside the new Glass Block route below).
- **New Syringe Mutator route (feasible, to add):** Glass Block + Ferrous Pulp -> Syringe, using `RecipeBuilders.buildMutating` with `Ingredient.of(Tags.Items.GLASS_BLOCKS_CHEAP)` (same tag the crafting recipe uses; the magma block recipe at ~1161 is the tag-ingredient precedent). Cost = the crafting recipe, 1 Iron Ingot + 1 Nugget = 100 mB Ferrous. No {item, fluid} collision with the existing Mutator recipes (stained glass uses dye + Silica).
- **Fuel Bladder and Charred Fuel Bladder:** 75 Cuprous Pulp each (`mutating_fuel_bladder`, `charred_fuel_bladder_from_charred_bladder_mutating`). The Feeder Bladder (750 Protein) is unchanged.
- **Leather:** 135 Calcium Pulp with Rotten Flesh (`mutating_leather`), preserving the old 1.5 Bone Meal cost.

**Puddle Craft recipes (DECIDED 2026-11-06).** A puddle recipe converts a whole 1000 mB source block, so its output cannot be rescaled and the output stays as is.
- REMOVE `calcium_blend_puddle` (4 Bone Meal -> 1000 mB Calcium Pulp, ~line 1201). No early implant recipe consumes Calcium Pulp (grep), so reachability is unaffected; the Masticator makes it. (`calcium_glass_puddle` already removed under the glass family.)
- KEEP `primitive_catalyst_puddle_coal` / `_charcoal` unchanged. Totals compared per 1000 mB: puddle = 1 coal + 1 Bone + 1 raw meat + a 1000 mB Crude Slurry puddle (about 8 item-equivalents counting Bone as 3 Bone Meal and the extra Crude as ~3 plant food); machine = 2.8 coal + 2.8 Bone Meal + ~1 meat + 250 Crude (about 7.6). Roughly even overall, so no change.
- KEEP `evolution_catalyst_puddle` (roughly even vs the Ichor route), `bone_puddle` (4 Bone Meal -> 1 Bone = 3 meal, premium kept), `crude_slurry_puddle` and `inert_tumor_puddle` (slurry is not rescaled).

**Stone / Silica / Clay roster recipes (DECIDED 2026-11-05):**
- The flat roster Masticator recipes (`stone_blend_masticating` ~804, and the silica / clay roster and recycling ones) are DONE AWAY WITH (user: the flat rate was a misunderstanding and is moot after the rescale). Replace with per-tier recipes whose yield mirrors the item's duplicate cost. Suggested tier tags: 360 (Stone, Smooth Stone, Andesite, Diorite, Granite, Deepslate, Calcite, Tuff, Dripstone Block; Sandstone, Red Sandstone; Clay Block; all Terracotta and Glazed), 270 (Cobblestone, Cobbled Deepslate), 90 (Gravel, Pointed Dripstone, Sand, Red Sand, Glass, Clay Ball), 30 (Glass Pane). Brick / Bricks / Flower Pot keep their individual recipes. Masticate times were not discussed; keep the current ones unless changed.
- The four cross-feed "boosted" recipes (~869-887) are KEPT. Their purpose is pulp-to-pulp conversion between stone, sand and clay; the +25% is a deliberate small gain (low gain = hard to exploit). Needs the per-tier split: solvent (sibling pulp) = the item's own yield, output = yield x 1.25 rounded up (sand 90 -> 113, cobble 270 -> 338, stone 360 -> 450). FYI, not a problem: because the item is consumed and must be re-duplicated at its own yield to recycle it, the real exchange rate is about 4 sibling : 1 own pulp, so there is no loop.
- Slabs: half their block. Stairs: 1:1 with their block (stone 360, cobble 270); masticate time follows the wall times (8 s cobble, 10 s solid) instead of the old 11.25 s (user agreed).
- Gravel 90. Glass 90, Glass Pane 30 (recycling).

**Torches (both the Dipping and the Mutator copies of each):**
- Torch: 1 wooden rod + 45 mB Carbon Pulp -> 4 Torches (`torch_dipping` ~1142, `mutating_torch` ~1154).
- Redstone Torch: 1 wooden rod + 45 mB Molten Redstone -> 1 (`redstone_torch_dipping` ~1148, `mutating_redstone_torch` ~1156).
- Times unchanged.

**Left ALONE by decision (do not touch):**
- All Sunder chain / Shatter head upgrade costs: the 3000 mB material transitions and the 1000/2000/3000 durability ladder (`buildPartUpgradeTiers`).
- Gadget fabrication recipes (Shatter 3000 Ferrous, Sunder ~2500).
- The user accepts that these become ~11x pricier in real materials until the gadget overhaul.
- Refined / Enriched / Superior slurry recipes (those fluids are not in the game yet).

---

## Phase 6: Contamination logic (player container path only)

Rule: dirty and clean of the SAME metal may mix in storage; the result turns dirty.
- Applies only when a PLAYER moves fluid with a container (bucket, Beaker, Glass Flask etc.).
- Automation (ducts, Nodes, machine auto-push, recipe outputs) never contaminates; it simply refuses to mix, like any mismatched fluid. A machine outputting Dirty Pulp into a tank holding clean Pulp stalls.
- Gadgets (Sipping, Drinker, etc.) can NOT contaminate.
- One-directional: dirty poured into clean turns the tank dirty; clean poured into dirty just joins the dirty tank and the source container stays clean. A dirty bucket placed and re-picked stays dirty (no laundering).
- Implementation point: the player container-interaction path (`ModFluidUtil` / `FluidUtil.interactWithFluidHandler` handling on machines), converting the tank's contents to the dirty fluid. Applies to the 3 Dirty Pulps only (not Dirty Molten Netherite).
- Per CLAUDE.md, any new variant block entity needs its own `RegisterCapabilitiesEvent` entries; irrelevant here unless new machines appear, but re-check Effluentcer variants.

---

## Phase 7: Loose ends to audit during the pass

- Carbon / Calcium Pulp used as a REAGENT (not an item proxy) anywhere else, e.g. Protein + catalyst recipes: audit like Synapse's Cuprous.
- Any remaining reference to the removed Primitive Catalyst volumes (Bone puddle, etc.).
- Open caveat, not blocking: what "difficult without a guard" does to a Tier 1 machine holding lava (Concentrated Slurry, Quartz route). Revisit when hazard effects are designed. Hazards are soft (no hard blocks).
- OPEN, possible Biohazard values on the Ichors (2026-11-06): the user is considering giving Nether Ichor (my lean: yes) and/or Luminous Ichor (taste call; already Radiation Mild) a Biohazard intensity via a `FLUID_HAZARD` data-map entry (`FluidHazardData.biohazard(...)`, band 1-100). Not decided. If adopted, the machine effect is a speed and healing slowdown only (x1 to x0.5), and the Silica -> Soul Silica step would involve three families (Biohazard + Thermal + Metaphysical).
- DEPENDENCY, Biohazard Safety Module: it must be craftable BEFORE the first Biohazard fluid is needed. Its recipe must not use an Ichor-derived item or Evolution Catalyst (Evolution Catalyst needs Luminous Ichor), or the chain is circular. The Safety Module variant recipes are part of the current machine overhaul (only the Safety Module Frame exists in `ModRecipeProvider` today).
- Hazard rule: the old "at most one hazard per recipe" rule is DROPPED (2026-11-05). Multiple hazards in a recipe are valid unless that recipe says otherwise. Hazards stay soft (no hard blocks).
- Survival reachability: confirm a real crafting chain for every new tagged or gated fluid (Flux, Dirty Pulps, both Ichors, Amethyst, Soul Silica, Quartz). The Nether Ichor chain needs a Nether trip but is otherwise Tier 1; verify in code.

---

## Phase 8: Data generation and verification

1. `./gradlew runData`; review the full diff under `src/generated/resources` (orphan files from the renames, new bucket models, tags, recipes, lang).
2. `./gradlew build`.
3. `./gradlew runClient` checklist:
   - Every renamed bucket and Beaker shows correct name and tint.
   - Each new fluid places, flows, renders translucent, and glows if emissive (Luminous Ichor).
   - One full ore chain: Raw Iron -> Dirty Ferrous -> Ingot (quick) and -> clean Pulp (patient).
   - Protein + Flux -> Dirty Ferrous, both Blood Nugget loops.
   - Contamination: dirty bucket into clean tank (turns dirty), clean into dirty (source stays clean), Node / duct refuses to mix.
   - Evolution Catalyst via Ichor, Molten Amethyst both routes, Concentrated Slurry with lava, Silica -> Molten Quartz.
   - JEI shows no zero or out-of-range amounts.
4. `./gradlew runGameTestServer`.

---

## Phase 9: Docs to update (these are mine, not code)

- `dermicraft-crafting-notes.md` ~142-154: old Blood Nugget documentation (now a food; iron route is fluid-only).
- `dermicraft-machine-notes.md`: Metastasizer "unimplemented / Blood Nugget" mentions.
- `dermicraft-project-primer.md` ~260: Blood Nugget.
- `dermicraft-catalyst-notes.md`, `dermicraft-slurry-notes.md`: new Evolution / Primitive / Concentrated recipes and rename.
- Design note for the old Pulp Blend (`project_pulp_blend_tree_fluid_system` memory): becomes Cellulose Pulp.
- `dermicraft-hazard-effects-notes.md`: Luminous Ichor in the Radiation Mild list, hard blocks dropped.
- Any doc using the words Blend, Essence, Raw Netherite Scrap.

---

## Suggested session split

Each is independently buildable and testable; stop at the checkpoint after each.

1. Phase 0 + 1 + 2 (constants and renames).
2. Phase 3 (direct rescale).
3. Phase 4 (new fluids).
4. Phase 5 (all recipes).
5. Phase 6 (contamination).
6. Phase 7 + 8 + 9 (audit, datagen, verification, docs).

Phases 3 and 5 are the largest; each can be split by material family or by recipe group if a single session runs long.
