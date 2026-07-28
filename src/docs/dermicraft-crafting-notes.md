# Dermicraft Crafting Notes

Running log of decided design choices for the Crafting fluid family and the reasoning behind them. Add a new entry per fluid as decisions get made — the "why" matters as much as the "what" so later additions stay consistent with earlier logic.

---

## Crafting fluids — overview

**Status:** Family identified in the primer; full detail now broken out into this companion doc, six original fluids decided (Carbon, Calcium, Protein, Ferrous, Cuprous, Aurous Blend), plus three newer Sediment Blends (Stone, Silica, Clay) — see their own section below.

**Definition:** Fluids whose main purpose is as recipe ingredients, rather than fuel (Slurries) or a process/transformation step (Catalysts). All produced by the **Masticator** (fluid input + item input → fluid output).

**Base fluid — corrected note:** Earlier shorthand stated "Water is the base fluid for Slurries and Crafting fluids" as a family-wide rule. That was never a real constraint — just a convenient way to avoid repeating "water" three times. It's been retired. The actual facts: **Carbon Blend, Calcium Blend, and Protein Blend each use Water** as their base fluid. **Ferrous, Cuprous, and Aurous Blend use Primitive Catalyst instead** — a deliberate exception, not a violation of anything. Don't assume water as a default for future Crafting fluids without checking case-by-case.

**Stage 2 Crafting Blends — lava-based, the "Molten" family.** All Stage 1 Crafting Blends use Water or Primitive Catalyst as their base. Stage 2 Crafting Blends use **Lava** instead — a deliberate "Stage 2 materials are forged/processed with heat, not soaked" parallel to the Metal Blends' own break from water. **Lava itself is the formal first Stage 2 fluid** — every "Molten X" fluid is a raw material gated behind it. **Molten Redstone** (renamed from "Liquid Redstone" purely to match this naming scheme — mechanic unchanged) is the first confirmed Stage 2 Crafting Blend (see the Stage 2 section below for the full, expanded roster).

**"More efficient alternate route" framing — partial, not universal:** The primer originally framed Crafting fluids as a faster/cheaper path to a fluid that *already* has an established recipe (e.g. Primitive Catalyst via puddle vs. via Blends). This still holds for Carbon/Calcium/Protein Blend. It does **not** hold for the Metal Blends — there's no pre-existing non-Masticator recipe for Ferrous/Cuprous/Aurous Blend to shortcut. They're Masticator-native from day one. It also doesn't hold for the Sediment Blends (Stone/Silica/Clay) — their purpose is a sustainable, repeatable supply loop rather than either an alternate route or a one-off Masticator-native fluid.

**Masticator constraints relevant to this whole family:**
- Tier 1 Masticator's **output buffer is 5000 mB (5 buckets)**.
- **Confirmed behavior:** if a recipe's output would exceed the remaining room in that buffer, the Masticator simply **will not process the recipe at all** — same "won't even attempt it" pattern as Drinker refusing to draw a hazardous fluid, rather than processing and failing/wasting input afterward.
- **Fixed 2026-07-18:** Masticator now consumes a recipe's declared `itemAmount` on craft completion instead of a hardcoded `1` — the recipe schema always had the field, but `onCraftComplete()` ignored it. Unblocks any multi-item-input recipe (e.g. 3 Bone Meal → 990 mB Calcium Blend). Metastasizer needed no equivalent fix — its output count already comes straight from the recipe's result `ItemStack`.
- **Flagged for revisit:** the original six Crafting-fluid yields (Carbon/Calcium/Protein/Metal Blends) may get reworked once the new Sediment Blend yields are set, to keep the whole family internally consistent rather than treating each batch of decisions in isolation.

---

## Carbon Blend

**Status:** Values decided.

**What it is:** Crafting fluid corresponding to Primitive Catalyst's coal/charcoal component.

**Base fluid:** Water.

**Item inputs & yield:**
- **Coal Block** + 1 bucket Water → **1000 mB** Carbon Blend
- **Coal** (loose) → **112 mB** each
- **Charcoal** (loose) → **110 mB** each

**Recipe logic:** Coal and Charcoal are deliberately valued unequally — not a rounding artifact. Charcoal is effectively infinite with a little renewable effort (burn wood) and isn't pure carbon, so it's worth slightly less per unit than mined Coal. Note this flips the usual "loose sub-unit yields less than the whole" pattern seen elsewhere in this doc: 9 loose Coal × 112 mB = 1008 mB, *more* than the Coal Block's 1000 mB — loose Coal is now the more efficient route. Charcoal has no vanilla block form to compare against, so its 990 mB-style shortfall pattern was always a Coal-specific coincidence, not a Carbon Blend-wide rule.

**Open questions — resolved:** Water input for loose Coal and loose Charcoal is **110 mB each** (not a flat 1 bucket) — Coal converts 110 mB Water into 112 mB Carbon Blend (a small net gain), Charcoal converts 110 mB Water into 110 mB Carbon Blend (flat 1:1).

**Metastasizer reverse route — built 2026-07-19.** All three items now duplicate from Carbon Blend, each mirroring its own forward recipe's output amount 1:1 (same convention as the Metal Blends' Ingot/Nugget reverse recipes): Coal Block (1000 mB), Coal (112 mB), Charcoal (110 mB). Previously the one Sediment/Metal-adjacent family with no reverse route at all.

**Secondary use — Torch Dip (implementation in progress):** Holding a Stick and right-clicking any fluid-handler block currently holding Carbon Blend converts Sticks into Torches on the spot, bypassing the crafting table. Uses a custom recipe type. Full mechanic detail — cost, batching, partial-fill behavior, overflow handling — in the Dip Crafting section of the Working Conventions in `dermicraft-project-primer.md`.

---

## Calcium Blend

**Status:** Values decided, expanded with a second recipe.

**What it is:** Crafting fluid corresponding to Primitive Catalyst's bone component.

**Base fluid:** Water.

**Item inputs & yield:**
- **Bone** + 1 bucket Water → **1000 mB** Calcium Blend
- **Bone Meal** → **334 mB** each (revised 2026-07-19, previously 330 mB — see below)

**Recipe logic (revised 2026-07-19):** Bone Meal now yields 334 mB Water in, 334 mB Calcium Blend out — flat 1:1, not the "sub-unit lands just under its parent unit's total" pattern used elsewhere (Charcoal, Nugget). The original 330 mB output (3 × 330 = 990 mB, ten short of a full Bone's 1000) matched that convention deliberately; the 334 mB revision breaks it as a direct call, not an oversight — flagging in case this drifts back out of sync with the rest of the family's "just under" values later.

Because the Masticator currently only accepts one item per cycle, a player can't batch 3 Bone Meal into a single run yet — they'd either run the Bone Meal recipe three separate times, or use a single Bone Meal as a deliberate top-off option when they're short of a full Bone's worth and don't want to go acquire/craft another whole Bone just for the last bit.

**Open questions — resolved:** Water input for Bone Meal is **334 mB**, producing 334 mB Calcium Blend (flat 1:1, both sides revised together 2026-07-19). The reverse Metastasizer duplication recipe (`bone_meal_metastasizing`) was updated to match, keeping the mirrored-pricing convention intact.

**Open questions — still open:** Whether the single-Bone-Meal "top-off" recipe should be kept around even after multi-item batching is added (it has genuine value as a fine-tuning tool, not just a placeholder).

**New, undocumented recipe found — Calcium Glass:** A puddle-crafting recipe not previously logged anywhere: **Bone Meal, puddle-crafted into a pool of Calcium Blend (400 ticks) → `Calcium Glass`** (a new item, `dermicraft:calcium_glass`). This exists in code (`calcium_glass_puddle.json`) but has no design writeup yet — what it's for, whether it's a block or item-only, and where it fits (a glass variant tougher/different from vanilla glass, presumably tied to Calcium Blend's bone/chalk identity) are all open.

---

## Protein Blend

**Status:** Fully decided.

**What it is:** Crafting fluid corresponding to Primitive Catalyst's raw-meat component.

**Base fluid:** Water.

**Item input & yield:** Any item carrying the custom **`MEAT_FOOD`** tag — meat regardless of source, **including Rotten Flesh**. No raw/cooked restriction, no exclusions. Yield is food-based: formula-driven from the input item's nutrition and saturation values, same approach as Drooling Cauldron and Crude Slurry's food-conversion logic.

**Recipe logic:** "Reward" here isn't a bonus rule bolted on — it's a natural consequence of vanilla's own stats. Cooked meat's nutrition/saturation values are dramatically higher than raw (e.g. cooked saturation values run 20–40x raw across most meats), so a player who takes the extra time to cook first gets more Blend out of the same item, purely because the formula is already plugging in real vanilla numbers. No special-casing needed. Rotten Flesh's inclusion self-balances the same way: its terrible saturation (0.1, the worst in the game) means it's allowed in but yields next to nothing — no explicit exclusion required.

**Open questions:** None remaining for the base recipe. See "Blood Nugget" under Metal Blends below for a second, distinct use of Protein Blend.

**Cross-reference:** Crude Slurry's food-based recipe (see `dermicraft-slurry-notes.md`) is restricted to **plant-based** food items specifically — apple, carrot, bread, etc. — not generic "any food," now that Protein Blend's meat-specific scope is locked in. The two recipes split the food-item space cleanly: plant-based → Crude Slurry, meat-tagged → Protein Blend.

---

## Metal Blends — overview (Ferrous, Cuprous, Aurous Blend)

**Status:** Names, base fluid, processability, and recipe-tier structure decided. Most yield values locked; a few open questions remain (see below).

**What they are:** Three Crafting fluids corresponding to Iron, Copper, and Gold.

**Naming:** **Ferrous Blend** (iron), **Cuprous Blend** (copper), **Aurous Blend** (gold) — elemental Latin root (*ferrum*, *cuprum*, *aurum*) + the **-ous** suffix. "Ferrous" already has real-world recognition ("ferrous metal," "ferrous oxide"), which is why **-ous** was chosen across all three instead of mixing in **-ic** forms (Cupric/Auric) — matching the naming *pattern* across the trio mattered more than per-metal chemical precision. (Strict chemistry would pair Ferrous with Cuprous/Aurous anyway by oxidation-state convention, so this also happens to land on the chemically-paired suffix — convenient, not the deciding factor.)

**Base fluid — confirmed in code, split by tier rather than uniform across the family:**
- **Ingot and Nugget tiers use Water**, same as Carbon/Calcium/Protein Blend — these two tiers are framed as **storage/conversion recipes** (turning an already-processed item into its fluid form at a flat, unmultiplied ratio), so they share the family's normal water-balance logic rather than needing anything special.
- **Raw tier uses Primitive Catalyst (250 mB) instead of Water — a deliberate additional cost, not a flavor swap.** Raw is the one tier that **multiplies** value (1 Raw item → 2000 mB, see the tier table below) rather than just converting it, so gating it behind Primitive Catalyst instead of free Water is the intentional price for that multiplication. This is the actual reasoning — not a "metal rusting in water" visual concern.

**No puddle recipe.** Unlike Primitive Catalyst itself, the Metal Blends have **no manual/pre-Masticator production method** — the Masticator is the only way to make them. This is also why the "more efficient alternate route" framing (see family overview above) doesn't apply to this trio.

**Reverse route — confirmed, via the Metastasizer.** The Ingot and Nugget tiers are now mirrored in the opposite direction: Blend + the same Ingot/Nugget item (as the duplication pattern) → a duplicate of that Ingot/Nugget, consuming the exact same fluid amount the Masticator recipe produces from it (1000 mB for Ingots, 110 mB for Nuggets). This closes the loop symmetrically — Masticator turns the metal item into Blend, Metastasizer turns Blend back into the item. No reverse recipe for Raw (it was never a 1:1 conversion to begin with — it multiplies value 8:1 via Primitive Catalyst, so there's no symmetric "cost" to mirror), and no Cuprous Nugget reverse recipe for the same reason there's no forward one (no vanilla Copper Nugget item in this Minecraft version).

**Tier 1 Masticator can process them — confirmed.** The Masticator grinds with calcium-based teeth; grinding a metal item is mechanically the same action regardless of which fluid it's ground into — no smelting-level heat is involved, so nothing in this family trips Tier 1's lava/hazardous-fluid restriction.

### Recipe tiers

Four tiers per metal are designed — **Ore, Ingot, Raw, Nugget** — but only **three are currently implemented**: Ingot, Raw, and Nugget. ("Ore" here means the intact ore block, obtained via Silk Touch, which is a distinct tier from "Raw," not a synonym for it.)

- **Ingot — the baseline, water-based.** 1 Ingot + 1000 mB Water → **1000 mB** Blend. Explicit 1:1 fluid-to-output ratio, a plain storage/conversion recipe; this is the anchor every other tier is built relative to.
- **Raw — 250 mB Primitive Catalyst → 2000 mB Blend, confirmed and correct.** Raw multiplies value (2x Ingot's yield from a cheaper source item) rather than just converting it, so it's gated behind Primitive Catalyst instead of free Water — the deliberate additional cost for that multiplication, not a rusting/visual concern.
- **Nugget — 110 mB Water → 110 mB Blend.** **Iron and Gold only** — Copper has no vanilla nugget item in NeoForge 1.21.1 (Copper Nugget wasn't added until the Copper Age update, Java Edition 1.21.9, past this mod's target version). 110 mB reuses the exact same math as Carbon Blend's Charcoal and Calcium Blend's Bone Meal: 9 Nuggets × 110 mB = 990 mB, ten short of a full Ingot's 1000 — the established "sub-unit lands just under its parent unit's total" pattern, for a third time.
- **Ore — designed, not yet implemented.** Requires Silk Touch. Intended yield = **750 × the maximum number of Raw units obtainable from that ore under Fortune I**, rounded to the nearest hundred. This deliberately makes Ore its own separate effort axis (a specialized enchantment choice, competing with Fortune for that pickaxe slot) rather than a continuation of the Raw → Ingot refinement chain — extraction effort rewarded on its own axis, separate from processing effort. **No recipe file exists yet for this tier on any of the three metals** — the numbers below are the design target, not confirmed in-game values.

| Form | Ferrous (Iron) | Cuprous (Copper) | Aurous (Gold) |
|---|---|---|---|
| **Ore** (Silk Touch, not yet built) | 1500 mB | **7500 mB** | 1500 mB |
| **Ingot** (Water, implemented) | 1000 mB | 1000 mB | 1000 mB |
| **Raw** (Primitive Catalyst, implemented) | 2000 mB | 2000 mB | 2000 mB |
| **Nugget** (Water, implemented) | 110 mB | — (no vanilla item) | 110 mB |

**Ore math, shown (design target, not yet built):** Iron and Gold both cap at **2** Raw units under Fortune I (fixed 1-unit base drop, ×2 multiplier) → 750 × 2 = 1500 mB. Copper's *un-enchanted* base drop is already a 2–5 range (not a flat 1), so Fortune I's ×2 multiplier caps it at **10** → 750 × 10 = 7500 mB. The 5x gap between Cuprous and the other two isn't a design choice — it falls directly out of vanilla's own uneven drop mechanics across these three ores. (This math used the old 750 mB Raw baseline when it was written — worth revisiting now that Raw is confirmed at 2000 mB, since the Ore formula was defined relative to Raw's value.)

### Confirmed issue: Cuprous Ore cannot currently be made on a Tier 1 Masticator

**This describes a design constraint on an unbuilt recipe, not a currently-blocked one.** Tier 1 Masticator's output buffer caps at 5000 mB. Cuprous Ore's *intended* 7500 mB result would exceed that, and per the family's confirmed Masticator behavior (won't process if the output won't fit), this recipe is **planned to be** unreachable on a Tier 1 Masticator once built — not "produces less than expected," but "will not run at all." Whether that's an intentional progression gate or something to revisit once the Ore tier is actually implemented is still open (see below).

### Ferrous Blend (Iron)

| Tier | Item | Yield | Status |
|---|---|---|---|
| Ore | Iron Ore (Silk Touch) | 1500 mB | Not yet implemented |
| Ingot | Iron Ingot | 1000 mB | Implemented |
| Raw | Raw Iron | 2000 mB | Implemented |
| Nugget | Iron Nugget | 110 mB | Implemented |
| Heavy Weighted Pressure Plate | 2 Iron Ingots (built 2026-07-19) | 2000 mB, 60s | Implemented, both directions |

### Ferrous Blend — alternate route (Blood Nugget)

**Status:** Mechanic and chain designed; **not yet implemented** — no Metastasizer machine exists in code at all yet (see `dermicraft-machine-notes.md`), so neither step of this chain can currently run. Exact yields also not yet set.

**What it is:** A second, distinct route into Ferrous Blend, separate from the Ore/Ingot/Raw/Nugget tiers above. Grounded in a simple, factually real claim: Protein Blend (made from raw/meat-tagged items) carries a meaningful blood component, and blood's oxygen-carrying function depends specifically on iron (hemoglobin) — a real, if trace-level, chemical connection.

**Chain:**
1. **Metastasizer:** Protein Blend (consumed) + an **Iron Nugget** (non-consumed pattern) → **Blood Nugget**. The Iron Nugget is **not consumed** — it stays as a reusable pattern indefinitely. This is an intentional, confirmed exception to the Metastasizer's usual "produces a duplicate of the pattern item" behavior; see `dermicraft-machine-notes.md` Metastasizer entry for the reasoning (the duplicate-output behavior is a default, not a hard rule).
2. **Masticator:** Blood Nugget + Primitive Catalyst → Ferrous Blend, consistent with the rest of the Metal Blend family using Primitive Catalyst as base.

**Balance intent — "low but fair":** Because the underlying real-world iron content in blood is trace, not substantial, this route is deliberately intended as a **slow trickle**, not a replacement for the Ore/Ingot/Raw/Nugget effort-gated system. Protein Blend's cheapest source (Rotten Flesh, free mob drops) means the real limiter has to be yield, not availability — same "infinite is fine, just shouldn't be easy" philosophy applied to the Sediment Blends below. Final Ferrous Blend yield from this route should land **well under** a standard Iron Nugget's own 110 mB value, to keep it reading as trace extraction rather than a real iron source.

**Open questions:** Exact Blood Nugget yield from the Metastasizer step. Exact Ferrous Blend yield from Blood Nugget + Primitive Catalyst.

### Cuprous Blend (Copper)

| Tier | Item | Yield | Status |
|---|---|---|---|
| Ore | Copper Ore (Silk Touch) | 7500 mB — **will be unreachable on Tier 1 once built, see issue above** | Not yet implemented |
| Ingot | Copper Ingot | 1000 mB | Implemented |
| Raw | Raw Copper | 2000 mB | Implemented |
| Nugget | — | no vanilla Copper Nugget exists in 1.21.1 | N/A |

**Copper building block family — built 2026-07-19, unaffected (fresh) state only.** Deliberately scoped down from the full vanilla item set: oxidation (Exposed/Weathered/Oxidized) is a passive weathering effect, not a recipe, so a Metastasizer route only ever duplicates a state the player already has a real sample of — it can't skip the wait — but the 4-state × waxed/unwaxed multiplier (up to 8 variants per block type) was cut for scope, fresh copper only. Copper Bulb also excluded — its real recipe (3 Ingot + Blaze Rod + Redstone) doesn't fit the Masticator's one-item-one-fluid shape. Priced by real vanilla crafting ratio relative to Copper Block (9 Ingots = 9000 mB, the anchor) — Cut Copper is a flat 1:1 conversion (no material lost cutting a block into panels, unlike Log → Planks), not a fractional discount:

| Item | Vanilla ratio | Cuprous Blend cost | Craft time |
|---|---|---|---|
| Copper Block | anchor (9 Ingots) | 9000 mB | 90s |
| Cut Copper | 4 Block → 4 Cut Copper (1:1) | 9000 mB | 90s |
| Cut Copper Stairs | 6 Cut Copper → 4 Stairs | 13500 mB | 135s |
| Cut Copper Slab | 3 Cut Copper → 6 Slabs | 4500 mB | 45s |
| Chiseled Copper | 2 Cut Copper → 1 Chiseled | 18000 mB | 180s |
| Copper Grate | 4 Ingot → 4 Grate (1:1) | 1000 mB | 10s |
| Copper Door | 6 Ingot → 3 Doors | 2000 mB | 20s |
| Copper Trapdoor | 4 Ingot → 2 Trapdoors | 2000 mB | 20s |

Craft time uses the Sediment Blend's 5 mB/tick rate (matching the existing Cauldron precedent), not the Metal Blend family's own slower Ingot rate (60s/1000 mB) — this is the "bulk/compound item" half of the Metal Blend family's now-formalized two-tier craft-time convention (see Metal Blends open questions, resolved), not an inconsistency.

### Aurous Blend (Gold)

| Tier | Item | Yield | Status |
|---|---|---|---|
| Ore | Gold Ore (Silk Touch) | 1500 mB | Not yet implemented |
| Ingot | Gold Ingot | 1000 mB | Implemented |
| Raw | Raw Gold | 2000 mB | Implemented |
| Nugget | Gold Nugget | 110 mB | Implemented |
| Light Weighted Pressure Plate | 2 Gold Ingots (built 2026-07-19) | 2000 mB, 60s | Implemented, both directions |

**Weighted Pressure Plates, both metals — built 2026-07-19.** Real vanilla recipe cost (2 Ingots each), so 2000 mB — same yield as Raw, and deliberately kept at Raw's own 60s craft time rather than doubling Ingot's: this family's precedent (Raw already yields 2x Ingot's mB at the same 60s) is "one item processed per cycle costs the same time regardless of its ingot-equivalent value," not a strict mB/tick ratio. Both directions built (Masticator forward + Metastasizer reverse), same as Ingot/Nugget.

**Open questions (Metal Blends) — resolved:**
- Water/Primitive Catalyst input amounts for all implemented tiers, confirmed from the actual recipe files: Ingot uses 1000 mB Water → 1000 mB output (1:1) for all three metals. Nugget uses 110 mB Water → 110 mB output (1:1) for Iron/Gold. Raw uses 250 mB Primitive Catalyst → 2000 mB output (an 8:1 multiplier) for all three metals.
- **Craft-time rate — resolved as a deliberate two-tier convention, not an inconsistency.** The Metal Blend family runs two different mB/tick rates on purpose, split by what kind of item is being processed, not by accident:
  - **Single-unit conversions** (Ingot, Nugget, Raw, Weighted Plate) use the family's own **slow rate, ~60s/1000 mB** — thematically, milling one Ingot-sized item is careful, precision work.
  - **Bulk/compound items** (Cauldron, the Copper building block family) use the **fast Sediment Blend rate, ~10s/1000 mB** — grinding down a large block that's already-aggregated material (9 Ingots' worth, in Copper Block's case) is comparatively quick bulk processing, not a repeat of the same precision work nine times over.
  - This was originally an accidental split (Cauldron and the Copper family both landed on the fast rate for playability, without being reconciled against Ingot's established rate) — formalized as the standing rule 2026-07-19 rather than migrating everything to one rate, since that would mean either making Cauldron/Copper agonizingly slow (7–18 minutes) or making Ingot/Nugget/Raw much faster than their already-tuned values. **Going forward: a new single-item Metal Blend recipe uses the slow rate; a new multi-Ingot compound item uses the fast rate.**

**Open questions (Metal Blends) — still open:**
- Is Cuprous Ore meant to be a deliberate progression gate (unreachable until an evolved Masticator exists), or should the value/approach be revisited so Tier 1 can use it at all? Still unresolved, and now also depends on the Ore tier actually being built first.
- Confirm item form for the Ore tier is the literal intact ore block (Iron Ore/Copper Ore/Gold Ore), obtained via Silk Touch — not Raw with some bonus modifier. Still a design question since Ore isn't implemented yet.
- Add a custom Copper Nugget item to give Cuprous tier parity with Ferrous/Aurous, or accept the missing tier as permanent?
- The Ore-tier yield formula (750 × max Raw units under Fortune I) was defined against the old 750 mB Raw baseline — needs revisiting now that Raw is confirmed at 2000 mB, before Ore gets built.

---

## Sediment Blends — overview (Stone Blend, Silica Blend, Clay Blend)

**Status:** Fully implemented. Rosters, blacklist, cross-feed relationships, recycling rules, hazard-fluid properties, yields, and Metastasizer per-item duplication costs are all decided and live in code.

**What they are:** Three Crafting fluids covering the mod's geological/sediment material families. Each is produced via the **Masticator** (block + base fluid → Blend) and consumed via the **Metastasizer** (Blend + an existing block from that fluid's roster, used as the duplication pattern → a duplicate of that block — set up recipe by recipe, not a generic "any fluid + any item" mechanic) — **the Metastasizer is now fully built and both halves of this loop are playable.** Unlike Carbon/Calcium/Protein Blend, the goal isn't a shortcut to an existing recipe — it's a deliberately repeatable, low-effort (but not zero-effort) supply loop for common building blocks, consciously modeled on the spirit of vanilla's infinite cobblestone generator.

**Metastasizer duplication costs — confirmed, by tier (craft time scales with the metaphorical "density" of the result):**
- **Aggregate tier** (loose gravel/sand) — 750 mB, 6s (120 ticks). Gravel, Sand, Red Sand.
- **Cobble tier** (rough-hewn stone) — 900 mB, 8s (160 ticks). Cobblestone, Cobbled Deepslate.
- **Solid tier** (dense/worked blocks) — 1000 mB, 10s (200 ticks). Stone, Andesite, Diorite, Granite, Deepslate, Calcite, Tuff, Dripstone Block, Sandstone, Red Sandstone, Clay.
- **Light tier** (small/fragile items) — 250 mB, 2.5s (50 ticks). Pointed Dripstone, Clay Ball.

All 17 roster items across the three families have a Metastasizer recipe.

**Base fluid:** Water, for all three — sediment/geological formation in reality involves mineral matter plus water, keeping consistent identity logic across the family.

**The three fluids and why each is separate:**
- **Stone Blend** — "ground stone/gravel sediment." Covers the general overworld stone family.
- **Silica Blend** — split off from Stone Blend because Sand has its own distinct identity: a real path toward glassmaking (silica → glass), and vanilla already treats Sand as its own family (gravity-affected, separate drops) rather than just "more rock."
- **Clay Blend** — split off for a parallel reason: Clay's identity is "moldable-when-wet, fired-when-permanent" (ceramics — bricks, terracotta), a different endpoint than either Stone or Sand, and vanilla already treats Clay as its own distinct family too.

**Rosters (the same list serves as both the Masticator input list and the Metastasizer duplication-pattern list):**

*Stone Blend roster:* Stone, Cobblestone, Andesite, Diorite, Granite, Deepslate, Cobbled Deepslate, Calcite, Tuff, Gravel, Dripstone Block, Pointed Dripstone. Smooth/polished/chiseled/cut variants of any of these fold into their base form rather than getting separate roster entries.

*Silica Blend roster:* Sand, Red Sand, Sandstone, Red Sandstone. Same variant-folding rule applies.

*Clay Blend roster:* Clay. (No natural cousin block the way Sand has Red Sand — Clay's expanded family below is crafted/fired, not natural.)

**Blacklist (excluded from the Stone Blend roster, Tier 1):**
- **Ores** — protects the Ferrous/Cuprous/Aurous Blend Ore-tier economy (which specifically rewards Silk Touch effort); letting Stone Blend duplicate ore blocks would trivialize that system.
- **Dirt, Mud** — not rock-identity (Dirt is already Flask of Crude Slurry's territory), and already trivially renewable.
- **All Obsidian forms** — Stage 2/lava-gated, would bypass the Stage wall.
- **Nether rocks** (Netherrack, Basalt, Blackstone, etc.) — deferred to a later Stage.
- **End rocks** (End Stone, Purpur, etc.) — deferred to a later Stage; first mention anywhere of a possible Stage beyond Stage 2 tied specifically to the End, not otherwise defined yet.

**Amethyst — flagged as a Tier 2 concept, not Tier 1.** Too rare/precious for unlimited Tier 1 duplication, same reasoning as the Ore blacklist. Silica Blend involvement floated as a possible future ingredient angle; mechanic undecided.

**Cross-feed relationships (boosted yield):** Using the sibling fluid instead of plain water as the "feed" produces a higher yield than the water-based base recipe — same "extra processing = extra payoff" logic as the Raw-vs-Ingot tiers and cooked-vs-raw meat above.
- Sand-family block + **Stone Blend** → Silica Blend (boosted)
- Stone-family item + **Silica Blend** → Stone Blend (boosted)
- Sand-family block + **Clay Blend** → Silica Blend (boosted)
- Clay + **Silica Blend** → Clay Blend (boosted)

Silica Blend sits as the hub of this relationship — it cross-feeds with *both* Stone Blend and Clay Blend, while Stone and Clay don't feed each other directly. Confirmed as the intended shape: a glass/silica-adjacent process plausibly touches both rock-grinding and clay-working, where stone and clay don't directly touch each other.

**Repeatable escalation — confirmed intentional.** Alternating the cross-feed recipes (boosted Stone Blend → feed into the Silica recipe → boosted Silica Blend → feed back into the Stone recipe → etc.) lets yields compound across repeated runs. This is allowed by design — comparable to vanilla's infinite cobblestone generator: each individual Masticator run is capped by the Tier 1 output buffer (5000 mB), but nothing stops repeated cycling. Time/effort is the limiter, not a hard resource cap. Cheap, common overworld blocks are exactly the kind of resource this is considered safe for.

**Recycling — crafted/fired derivatives feed back into their source fluid:**
- **Glass + Water → Silica Blend** (Glass is sand's fired form, same logic as the Clay-family recycling below).
- **Clay-family crafted items → Clay Blend:** Clay Ball, Terracotta (all 16 dyed colors collapse into one roster entry), Glazed Terracotta (same — one entry covering all colors). **Mud Bricks excluded** — made from Mud (already blacklisted) + Wheat, a separate material lineage despite the visual similarity.
- **Recycling yield — resolved:** Same as raw, not lossy. `silica_blend_recycling_masticating.json` and `clay_blend_recycling_masticating.json` both use 1000 mB Water → **1000 mB** output — identical to the base raw-material recipe, no penalty for the material having already been fired/crafted once.
- **Brick, Bricks, and Flower Pot pulled out of the flat-rate recycling tag 2026-07-19** and given individually-tiered recipes instead, mirrored in both directions (Masticator forward and Metastasizer reverse use the same amount, same convention as the Metal Blends' Ingot/Nugget): **Brick 250 mB (light tier), Bricks 1000 mB (solid tier), Flower Pot 750 mB (aggregate tier)**. Unlike Clay Ball/Terracotta (flat 1000 mB either direction, no relation between forward and reverse cost), these three now follow the mirrored-pricing model instead — a deliberate per-family choice, not a universal rule change for the rest of the Clay recycling tag.

**Hazard / environmental properties — pools act as traps, achieved via FluidType tuning rather than custom mechanics:**
- **Stone Blend** — heavy/sinking trap (wet-concrete analog). High density + high viscosity as primary levers.
- **Clay Blend** — sticky/immobilizing trap (bog/mud analog). High viscosity as the primary lever.
- **Silica Blend** — loses-support/sinking trap (true quicksand analog). Very low motion scale + high viscosity as primary levers.
- All three: **not Tier-hazardous**, per the project's "hand-in-a-bucket" rule (see Working Conventions in the primer) — none of them harm a hand dipped briefly; the danger is immersion/volume-based, not substance-based.
- A 1×1 pool deep enough to fully submerge a player is a genuine deadly hazard via vanilla's own suffocation/drowning mechanics — no custom danger mechanic needed, just FluidType tuning plus existing game systems.
- Exact numeric FluidType values for all three deferred to the same later yields/properties pass as everything else in this family.

**Open questions — resolved:** Base and boosted yields for all three fluids, confirmed identical across the family: **base recipe (roster item + 1000 mB Water) → 1000 mB** output (1:1); **boosted recipe (roster item + 1000 mB of the sibling Blend) → 1250 mB** output — a flat +250 mB bonus, consistent across all four cross-feed pairs (Stone↔Silica, Silica↔Clay). Recycled-item yield: same as raw (see above). Metastasizer per-item duplication costs (see tier table above).

**Open questions — still open:** Exact FluidType numeric values (density, viscosity, motion scale) for the hazard properties.

---

## Pulp Blend — the wood/tree family, split by living vs. dead

**Status:** Fluid identity, base recipe, and Metastasizer roster ratios all decided. Recipes not yet built in code.

**What it is:** The wood/tree family's Crafting fluid, covering dead/harvested plant matter — logs and everything crafted from them. Deliberately **not** the fluid used for anything living (saplings, leaves) — that stays on **Crude Slurry** instead (see below), splitting the family the same way Stone/Silica/Clay split the sediment family, but by living-vs-dead rather than by material identity.

**Base fluid:** Water, same as the original three Crafting Blends and the Sediment Blends.

**Masticator recipe:** Log (any species) + 1000 mB Water → 1000 mB Pulp Blend. Matches the Sediment Blends' flat 1:1 convention exactly.

**Critical design constraint — one universal fluid, not split by wood species.** This isn't just a naming convenience: Pulp Blend carries no species identity of its own, so pulp made from an abundant species (e.g. Birch) can be spent against a rare species' pattern (e.g. Jungle) on the Metastasizer to duplicate the rare one. Species identity lives entirely in the pattern item. Splitting Pulp Blend per wood type would break this — vanilla's wood types don't carry the kind of identity split Sand/Stone/Clay had (real-world material differences); they're palette variants of the same underlying "wood" concept, so folding them into one fluid follows the same precedent Stone Blend already set for its own variant-folding.

**Real player value:** not "infinite free wood" — a pattern item is still required, so the player must have harvested at least one sample of a species already. The actual payoff is no longer needing to re-travel to a specific biome for a wood species once a single piece of it has been obtained; whatever wood is locally abundant can fund the pulp needed to duplicate whatever species a build actually calls for.

**Metastasizer roster — ratios derived directly from vanilla crafting ratios, not a tier system.** Log's Masticator conversion (1000 mB = 1 bucket) is the anchor; every other item's cost is that item's real vanilla-recipe ratio relative to Planks (250 mB, a quarter of Log's bucket — matching both the real 1-log-to-4-planks ratio and the existing Glass Flask/Beaker quarter-cost precedent). Craft times extrapolated from the existing Metastasizer tiers' rough ~5 mB/tick ratio, since this family doesn't use the tier system.

| Item | Vanilla ratio (relative to Planks) | Pulp Blend cost | Craft time |
|---|---|---|---|
| Log | anchor (1 bucket) | 1000 mB | 200 ticks |
| Planks | anchor (¼ Log) | 250 mB | 50 ticks |
| Slab | 3 Planks → 6 Slabs (½ Plank) | 125 mB | 25 ticks |
| Stick | 2 Planks → 4 Sticks (½ Plank) | 125 mB | 25 ticks |
| Stairs | 6 Planks → 4 Stairs (1.5 Planks) | 375 mB | 75 ticks |
| Door | 6 Planks → 3 Doors (2 Planks) | 500 mB | 100 ticks |
| Trapdoor | 6 Planks → 2 Trapdoors (3 Planks) | 750 mB | 150 ticks |
| Fence Gate | 4 Sticks + 2 Planks → 1 Gate | 1000 mB | 200 ticks |
| Fence | 4 Planks + 2 Sticks → **3** Fence | 1250 mB → 3 Fence | 250 ticks |
| Pressure Plate | 2 Planks → 1 Plate (built 2026-07-19) | 500 mB | 100 ticks |
| Button | 1 Plank → 1 Button (built 2026-07-19) | 250 mB | 50 ticks |

**Fence's 3-per-craft output was flagged as possibly blocked, then confirmed not to be.** Vanilla's Fence recipe yields 3 per craft off a cost (1250 mB) that doesn't divide evenly per single fence (416.67 mB) — initially assumed blocked by the same one-item-per-cycle limitation affecting Calcium Blend's Bone Meal recipe. Checking the actual code showed that limitation only applies to the **Masticator's item-input side** (now fixed, see ToDo list). The **Metastasizer's output side** already supported arbitrary result counts via the recipe's `ItemStack` (whatever count is baked into `result` is exactly what gets inserted) — no engine change was needed. Fence is built exactly as designed: pattern + 1250 mB Pulp Blend → 3 Fence.

**Stale — all 8 overworld species are now covered.** This previously flagged dark variants etc. as not yet built; verified 2026-07-19 that `woodSpecies` in `ModRecipeProvider.java` (oak, spruce, birch, jungle, acacia, dark_oak, mangrove, cherry) already drives Log/Wood/Stripped Log/Stripped Wood/Planks/Slab/Stairs/Door/Trapdoor/Fence Gate/Fence uniformly for every species, on both the Masticator (tag-based, species-agnostic) and Metastasizer (per-species duplication) sides. Only Nether stems/hyphae and Bamboo remain genuinely deferred (see roster note above).

---

## Crude Slurry's expanded role — living plant material

**Status:** Both recipes decided. **Metastasizer half built 2026-07-19** (all 8 overworld species — oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry — via the shared `woodSpecies` loop in `ModRecipeProvider.java`). Mutator half (Sapling + Crude Slurry → Leaves) still not built.

**What it is:** Rather than inventing a second new fluid for the living half of the wood/tree family, Crude Slurry's existing identity ("forces accelerated biological growth" — see `dermicraft-slurry-notes.md`) is reused, since it already fits the living side better than anything new would.

- **Mutator:** Sapling + Crude Slurry → **Leaves** (not Log — a sapling forced to grow produces living green growth, not dead heartwood; Log stays reachable only by harvesting or by Pulp Blend duplication, never synthesized from nothing). **Not yet built.**
- **Metastasizer:** Leaves (pattern, any species) + **100 mB Crude Slurry → Leaves**, 50 ticks. Deliberately priced below the Metastasizer's existing lowest bracket (250 mB) — Leaves are purely decorative and more trivially renewable in vanilla than anything else duplicated so far (no digging required, just natural decay off any tree). Establishes a general working convention: **purely decorative items should undercut the nearest cost tier, not just match it**, rather than defaulting to an existing bracket out of consistency for its own sake. **Built.**

**Open — not yet decided:** Whether Saplings and/or Seeds get their own duplication or synthesis recipe. Mixed precedent across other mods, low power impact either way (wood is a building material — large amounts don't grant a major in-mod advantage), and cross-modpack balance is explicitly the pack builder's problem, not this mod's. No pressure to resolve this before building the rest of the family.

**Stage placement:** Both Pulp Blend and Crude Slurry's expanded role are Stage 1 — all-overworld materials (logs, water), no lava, same Stage-1 fit as the Sediment Blends precedent this design followed.

---

## Mod-wide ToDo list (carried forward from this doc)

- **Build the Metal Blends' Ore tier** (Ferrous/Cuprous/Aurous) — designed, no recipe files exist yet. Revisit the Ore yield formula first since it was defined against the old 750 mB Raw baseline, not the confirmed 2000 mB.
- **Metastasizer machine — built.** Sediment Blend duplication (all 17 roster items), the Metal Blends' reverse Ingot/Nugget route, the Carbon Blend reverse route (Coal/Charcoal/Coal Block, added 2026-07-19), Leaves duplication (all 8 species, added 2026-07-19), the Cauldron/Ferrous Blend route (added 2026-07-19), Brick/Bricks/Flower Pot individually-tiered mirror recipes (added 2026-07-19), MRE/Meat Flavored Meat duplication, and Protein Blend → Inert Tumor/Dense Muscle/Nerve Cluster/Eye duplication are all live. The Blood Nugget → Ferrous Blend chain is the one thing on this list still not implemented (needs the Blood Nugget item + its own Metastasizer recipe) — no longer blocked on the machine itself, just on that specific recipe/item.
- ~~**Masticator:** add support for processing multiples of one item per cycle~~ — **done 2026-07-18.** `onCraftComplete()` now consumes `itemAmount` from the recipe instead of a hardcoded `1`. Clean batch recipes like 3 Bone Meal → 990 mB Calcium Blend are now buildable in a single cycle.
- Resolve Cuprous Ore vs. Tier 1 Masticator's 5000 mB output buffer, once the Ore tier is actually built (see Metal Blends open questions above).
- Decide whether to add a custom Copper Nugget item.
- **Rework existing Crafting-fluid yields (Carbon/Calcium/Protein/Metal Blends) once Sediment Blend yields are set**, to keep the whole family internally consistent. (Sediment Blend yields are now set — see above — so this rework is unblocked.)
- Decide Blood Nugget yield and the resulting Ferrous Blend yield (target: "low but fair") — unblocked now that the Metastasizer exists, just not yet decided.
- Decide exact FluidType values (density, viscosity, motion scale, temperature) for Stone/Silica/Clay Blend hazard properties.
- Decide exact item inputs, yields, and FluidType values for the expanded Molten family: Molten Quartz, Molten Glowstone, Molten Amethyst, Molten Diamond, Molten Obsidian, Molten Lapis, Molten Raw Netherite Scrap/Molten Netherite, Blaze Essence, Ghast Essence, Wither Essence, Molten Soul Silica. (Living Glowstone's creation recipe is now resolved — Molten Glowstone + Living Catalyst via the Gestator — see `dermicraft-catalyst-notes.md`.)
- Decide any uses for Molten Redstone beyond the Redstone Torch Dip.
- Molten Prismarine remains unnamed-in-mechanic on purpose — find a use case before designing it.
- Decide whether the Silica Blend → Molten Soul Silica conversion hook and the Soul Sand hazard-carryover hook (see Molten Soul Silica entry) get built.
- Decide Wither Essence's exact source item(s) and yield. Liquid Nether Star (reserved name for the eventual Wither-boss-tier counterpart) remains fully undefined.
- **Write up Calcium Glass** — a real recipe exists (`calcium_glass_puddle.json`) with no design documentation anywhere.
- **Write up MRE, Meat Flavored Meat, and the Metastasizer's tumor/part duplication recipes** — all implemented in code (see `dermicraft-machine-notes.md` Metastasizer entry) but not yet given a proper design writeup here (ingredient logic, why Protein Blend/F-Stuff, etc.).
- ~~Build the Metastasizer's plain-glass duplication family~~ — **done 2026-07-16.** Glass Block, Beaker (Silica + Calcium routes), Glass Flask (Silica + Calcium routes), Glass Pane, and Calcium Glass duplication are all now live in `ModRecipeProvider.java`, closing the gap discovered while building the Mutator's dyed-glass recipes (the table in `dermicraft-machine-notes.md` → Metastasizer entry had been wrongly marked "Confirmed" without ever actually being datagenned). This also unblocks the OT-native recipes that need a Beaker/Glass Flask sourced without hand-crafting.
- ~~Build the Mutator's Colored/Glazed Terracotta recipes~~ — **done 2026-07-16.** Resolved the dye-keyed collision by changing Glazed's ingredient from the dye to the Colored Terracotta item itself (see `dermicraft-machine-notes.md` → Mutator entry, ceramics family note) rather than splitting across machines like the glass family.

---

## Dragon's Milk

**Status:** Core harvesting mechanic decided. Hazard classification decided. Exact drain rate open.

**What it is:** A Crafting Blend harvested directly from a living mob rather than produced by the Masticator — the only Crafting Blend with **no Masticator recipe at all**, and the only member of this family that isn't "Molten" or an "Essence." Source: the Ender Dragon, tag-based so modded dragon-type entities can be included later; vanilla Ender Dragon confirmed as the baseline.

**Harvest method:** Hold-to-use Drinker interaction directed at the dragon instead of a world fluid block. No cooldown — the dragon is a renewable, repeatable source, not a limited-use drop.

**Tier restriction:** Hazardous (fails the hand-in-a-bucket test). Requires a **Tier 2 Drinker** — Tier 1 Drinker refuses to draw it at all. See `dermicraft-gadget-notes.md` Drinker entry for the Gadget-side change this required.

**Base fluid:** N/A — harvested directly, not synthesized.

**Hazard classification:** `Hazardous → Biohazard`. Second confirmed fluid to use the Biohazard tag (after All Metal — see Stage 4 section below), reinforcing it as a real ongoing category. Fits Dragon's Milk's identity as harvested biological material from a living creature better than a generic Radiation tag would. Still requires a **Tier 2 Drinker** to harvest (Tier 1 Drinker refuses any `Hazardous`-tagged fluid outright, regardless of which child tag).

**Confirmed use:** One of three fluid inputs in the **Living Catalyst** recipe, alongside Molten Quartz and Blaze Essence — see `dermicraft-catalyst-notes.md`. Dragon's Milk's power level fits naturally as a Stage 2/3-boundary ingredient, matching Living Catalyst's own placement.

**Open questions:** Exact per-tick drain rate — deferred to Code. Whether Tier 2 Drinker changes anything besides hazard access.

---

## Stage 2 Crafting Blends — the "Molten" family

All Stage 2 Crafting Blends use **Lava** as their base fluid rather than Water or Primitive Catalyst — a deliberate "Stage 2 materials are forged/processed with heat, not soaked" decision, parallel to the Metal Blends' own break from water. Produced by the Masticator (same as Stage 1 blends), but Lava as a base fluid means these are inherently Tier 2 recipes. **Lava itself counts as the first, foundational Stage 2 fluid** — nothing in this family exists without it.

**Family purpose — confirmed, narrower than "everything gets a lava version."** The Molten family exists specifically to give **rare or otherwise-hard-to-liquefy materials** a fluid form, gated behind Stage 2 lava access — it is not a blanket "reprocess any material with lava" option. This is why the Metal Blends (Ferrous/Cuprous/Aurous — iron, copper, gold) deliberately **don't** get Molten versions: they were never gated materials to begin with, so a Molten Iron/Copper/Gold would add a step without adding a gate. Living Iron/Copper/Gold (once Living Catalyst exists) go straight from the existing Blend, no Molten intermediate.

**Naming split — "Molten" for minerals, "Essence" for mob drops.** Mineral/ore-sourced members use **Molten [material]** (literally liquefied by lava). Creature-drop-sourced members drop the "Molten" prefix entirely and use **[material] Essence** instead (Blaze Essence, Ghast Essence, Wither Essence, Ender Essence) — signaling these are lava-*processed* biological/magical drops, not literally melted rock. Dragon's Milk stays unrenamed since it's harvested directly, with no Masticator/Lava step at all.

**Hazard tagging (confirmed, blanket rule):** every member of this family carries **Extreme Heat** (`hazard/extreme_heat`), inherited directly from Lava as their base fluid — masticating anything with Lava makes the result Extreme-Heat-hazardous, no per-fluid decision needed. This is a rule for the *whole* family, not something to re-confirm fluid-by-fluid (see `dermicraft-project-primer.md` → Hazard tag hierarchy, and [[project_hazard_profile_system]] in Claude Code memory for the code-level tag). Dragon's Milk is the one exception worth double-checking when it's built, since it skips the Masticator/Lava step entirely.

**Reachability gap (flagged, blocking the whole family) — no Tier 2 Masticator exists yet.** The Tier 1 Masticator's tanks reject Lava (`HazardProfile.TIER_1`), so nothing in this family can actually be crafted until an evolved Masticator exists — see `dermicraft-machine-notes.md` → Masticator entry for the full note. Don't implement any Molten-family recipe ahead of that evolution.

### Molten Redstone

**Status:** Fluid registered and implemented (bucket, block, FluidType, tags, creative tab) — no recipe yet, so currently unreachable in survival (matches the family-wide reachability gap, see `dermicraft-machine-notes.md`). (Renamed from "Liquid Redstone" to match the family's naming scheme — mechanic and item-yield values unchanged.)

**FluidType (confirmed, implemented):** tint `0xFFB22222` (firebrick red — glowing-red-lava read). `viscosity(5200)`, `density(3200)` — near Lava's own weight/thickness, since Lava is the literal base fluid. `temperature(1450)` — deliberately *hotter* than plain Lava (~1300); reasoning: redstone is already "vibrating with potential energy," and heat exposure doesn't dissipate through it the way it would through an inert material, it compounds instead. `motionScale(0.035)` — the "energetic" tell: despite being as thick/heavy as Lava, it responds to movement far more than something this viscous should, echoing Primitive Catalyst's own inverted-motion-scale trick but with an in-fluid justification (redstone = literal energy conduction) instead of "wrongness for its own sake." `lightLevel(8)` — emissive, deliberately pitched between a Redstone Torch's light level (7) and lit Redstone Ore's (9) rather than matching either exactly. `canHydrate(false)`.

**Hazard tag:** `hazard/extreme_heat` only, per the family-wide Lava-base rule. Mild Radiation was considered and deliberately rejected — the "energetic" identity is already fully expressed through the FluidType properties above (motion scale, temperature), and stacking a Radiation tag on top would encode the same idea twice. Radiation is reserved for materials that read as unstable/decaying (e.g. Living Glowstone), which a controllable engineering signal like redstone doesn't fit without a distinct justification.

**What it is:** A fluid form of redstone. The mod's first confirmed Stage 2 Crafting Blend and the origin of the lava-based Stage 2 rule above. Supports a Redstone Torch Dip mechanic (see Dip Crafting in `dermicraft-project-primer.md` Working Conventions).

**Base fluid:** Lava.

**Item inputs & yield:**
- **Redstone Block** + 1 bucket Lava → **1000 mB** Molten Redstone
- **Redstone Dust** (loose) → **110 mB** each (9 × 110 = 990 mB — follows the established "sub-unit lands just under its parent unit's total" convention, same math as Iron/Gold Nugget and loose Charcoal)

**Recipe logic:** Same sub-unit convention used across Carbon/Calcium/Metal Blends — 9 Redstone Dust × 110 mB = 990 mB, ten short of a full Block's 1000 mB. Clean, consistent.

**Secondary use — Redstone Torch Dip:** 1 Stick + 75 mB Molten Redstone → 1 Redstone Torch. Same host-block, batching, partial-fill, and overflow rules as Carbon Blend's Torch Dip. Cost comparison: 75 mB vs. 1 Redstone Dust (110 mB-equivalent) in vanilla — roughly 68% of vanilla's dust cost, confirming it as genuinely cheaper. Full mechanic detail in `dermicraft-project-primer.md` Working Conventions (Dip Crafting).

**Open questions:** Any uses for Molten Redstone beyond the Redstone Torch Dip?

### Molten Quartz

**Status:** Fluid registered and implemented (bucket, block, FluidType, tags, creative tab) — no recipe yet, so currently unreachable in survival, same reachability gap as the rest of the family. (Renamed from "Liquid Quartz.")

**What it is:** Nether Quartz in fluid form. Confirmed use as one of Living Catalyst's three fluid inputs (see `dermicraft-catalyst-notes.md`), where it represents time/duration in that recipe's logic.

**Base fluid:** Lava.

**Item inputs & yield:** Not yet decided — presumably Nether Quartz (block/ore) + loose Quartz, following the same block-vs-loose-item split as the rest of the family.

**FluidType (confirmed, implemented):** tint `0xFFE3D6B5` — Nether Quartz's real pale cream, warmed slightly by the Lava base rather than pulled toward Redstone's red or the eventual Amethyst purple, keeping the three visually distinct. `viscosity(5000)`, `density(3200)` — thick like the rest of the family, marginally smoother than Molten Redstone's 5200, reading more like liquid crystal than churning energy. `temperature(1300)` — plain Lava baseline, no inversion trick. `motionScale(0.007)` — matches the physically expected value for its viscosity (same water→lava relationship), deliberately normal rather than uncanny: quartz's "time/duration" identity reads as steady and measured, the opposite of Redstone's energetic wrongness. No light emission — quartz doesn't glow. `canHydrate(false)`.

**Hazard tag:** `hazard/extreme_heat` only, per the family-wide Lava-base rule. Nothing about a "time" identity suggested an additional hazard.

**Open questions:** Exact item inputs and yields.

### Molten Glowstone

**Status:** Fluid registered and implemented (bucket, block, FluidType, tags, creative tab) — no recipe yet, so currently unreachable in survival, same reachability gap as the rest of the family.

**What it is:** Glowstone in fluid form — the plain, non-living Stage 2 precursor to **Living Glowstone** (Stage 3). Molten Glowstone + Living Catalyst → Living Glowstone via the Gestator (see `dermicraft-catalyst-notes.md` and `dermicraft-machine-notes.md`). Not living or self-replicating — those properties are Living Glowstone's alone. **Revised: it is emissive**, just dimmer than the Living form (see FluidType below) — "not itself... emissive" from the original draft was walked back once the fluid actually got built.

**Base fluid:** Lava.

**Item inputs & yield:** Presumably Glowstone (block) + loose Glowstone Dust, following the family's usual block-vs-loose split. Not yet decided.

**FluidType (confirmed, implemented):** tint `0xFFC9A857` — a duller, more muted gold than vanilla Glowstone Dust's bright yellow, reinforcing that this is the "before" state relative to Living Glowstone's eventual activated look. `viscosity(5000)`, `density(3200)`, `temperature(1300)`, `motionScale(0.007)` — family baseline across the board, no tricks; the negative-density/self-replication/uncanny-motion tricks are reserved for the Living Stage 3 version, not earned yet here. `lightLevel(10)` — meaningfully bright, but held back from vanilla Glowstone's full `15` so Living Glowstone still has visible room to feel like the fully-activated upgrade. `canHydrate(false)`.

**Hazard tag (confirmed):** `hazard/radiation_mild`, in addition to the family-wide `hazard/extreme_heat`. Carried down from Living Glowstone's own Mild Radiation classification — the precursor is already radioactive before the Living Catalyst step "activates" it, not a property that switches on only at Stage 3.

**Open questions:** Exact item inputs and yields.

### Molten Amethyst

**Status:** Fluid registered and implemented (bucket, block, FluidType, tags, creative tab) — no recipe yet, so currently unreachable in survival, same reachability gap as the rest of the family.

**What it is:** Amethyst in fluid form — resolves the earlier "Amethyst flagged as a Tier 2 concept" open question from the Sediment Blend section above. Sits adjacent to Molten Quartz (both clear/crystalline Nether-and-cave minerals), but now has two independent points of distinction (see FluidType and Tinted Glass, below) rather than reading as a straight reskin.

**Base fluid:** Lava.

**Item inputs & yield:** Not yet decided.

**FluidType (confirmed, implemented):** tint `0xFF8A5AA8` — amethyst purple, clearly distinct from Quartz's pale cream. `viscosity(5000)`, `density(3200)`, `temperature(1300)` — family baseline, matches Quartz. `motionScale(0.004)` — **lower** than what its viscosity would predict (~0.007), the physical-identity axis that separates it from Quartz: real-world amethyst's etymology is literally "not intoxicated," a calming/sobering stone, so it resists movement *more* than something this thick already should — the mirror opposite of Molten Redstone's "moves more than it should." No light emission. `canHydrate(false)`.

**First confirmed use (new):** **Glass + Molten Amethyst → Tinted Glass**, via a Tier 2 **Mutator** (see `dermicraft-machine-notes.md` → Mutator, Stage 2 cluster) — a reagent-mapping of vanilla's 4-shards-+-glass recipe. This is Molten Amethyst's first consumer and a second, independent point of distinction from Molten Quartz: **light manipulation** (tinted glass blocks light), alongside the "calming" physical identity above.

**Ambient chime (confirmed, implemented — `MoltenAmethystChimeEvent`):** A third, independent point of distinction. Reuses vanilla's own Amethyst Cluster chime (`SoundEvents.AMETHYST_BLOCK_CHIME`) — plays periodically (every 5 seconds, 35% chance per check, server-side) when a player is near any of: a placed world fluid block, a tank block entity holding it (Skin/Chitin Tank, machine input/result tanks — checked via `IFluidHandler` capability, not just placed fluid), or a carried container (bucket, or Beaker/Glass Flask/Syringe via the `FluidData` component) — all within a 5-block radius for the world/tank checks. Reinforces the "calming" identity audibly, not just through the dampened motion scale.

**Hazard tag:** `hazard/extreme_heat` only, per the family-wide Lava-base rule.

**Open questions:** Exact item inputs and yields.

### Molten Diamond

**Status:** Fluid registered and implemented (bucket, block, FluidType, tags, creative tab) — no recipe yet, so currently unreachable in survival, same reachability gap as the rest of the family.

**What it is:** Diamond in fluid form.

**Base fluid:** Lava.

**Item inputs & yield:** Not yet decided.

**FluidType (confirmed, implemented):** tint `0xFF6FCFC4` — diamond's pale cyan-white, clearly distinct from the rest of the family. `viscosity(6200)` — thicker than plain Lava (~6000), the thickest fluid in the mod so far. `density(3800)` — heaviest in the family (baseline elsewhere is 3200). `temperature(1300)` — plain baseline, deliberately no inversion trick; Diamond's identity is physical density/hardness, not heat, keeping it from repeating Molten Redstone's "hotter than Lava" move. `motionScale(0.005)` — low, barely responsive to movement, reinforcing "resists everything, including you." Normal physics taken to an extreme, not an inversion trick like Redstone/Amethyst. No light emission. `canHydrate(false)`.

**Hazard tag:** `hazard/extreme_heat` only.

**Open questions:** Exact item inputs and yields. Any secondary use beyond a recipe ingredient.

### Molten Obsidian

**Status:** Fluid registered and implemented (bucket, block, FluidType, tags, creative tab) — no recipe yet, so currently unreachable in survival, same reachability gap as the rest of the family.

**What it is:** Obsidian in fluid form — a deliberate thematic inversion, since Obsidian is real-world *cooled* lava. Re-melting it back into a fluid is "already-forged, forged again," distinct from every other Molten fluid, which is liquefying a raw material for the first time.

**Base fluid:** Lava.

**Item inputs & yield:** Not yet decided.

**FluidType (confirmed, implemented):** tint `0xFF3D1A2C` — a dark plum-black, pulled much closer to real Obsidian block's own near-black purple than the original lighter draft. `viscosity(6000)`, `density(3000)`, `temperature(1300)`, `motionScale(0.007)` — **exactly Lava's own reference values, no divergence at all.** Deliberate: every other Molten fluid distinguishes itself *from* Lava somehow (Redstone hotter, Diamond thicker, Amethyst calmer); Molten Obsidian's identity is that there's no trick, because re-melting cooled Lava back down honestly is just Lava again. No light emission. `canHydrate(false)`.

**Hazard tag:** `hazard/extreme_heat` only.

**Open questions:** Exact item inputs and yields. Any use beyond a recipe ingredient.

### Molten Lapis

**Status:** Fluid registered and implemented (bucket, block, FluidType, tags, creative tab) — no recipe yet, so currently unreachable in survival, same reachability gap as the rest of the family.

**What it is:** Lapis Lazuli in fluid form.

**Base fluid:** Lava.

**Item inputs & yield:** Not yet decided.

**FluidType (confirmed, implemented):** tint `0xFF1B3F8B` — deep lapis blue, clearly distinct from the rest of the family. `viscosity(5000)`, `density(3200)` — family baseline. `temperature(900)` — noticeably **cooler** than Lava's ~1300 baseline, the fluid's distinguishing axis: leans on Lapis's real identity as the enchanting-table material, reading as carrying arcane/mystical energy rather than raw heat. First Molten fluid to run colder than baseline (Redstone went the opposite direction, hotter). `motionScale(0.007)` — normal for its viscosity, no second trick stacked on top. No light emission. `canHydrate(false)`.

**Hazard tag:** `hazard/extreme_heat` only — still inherited from the family-wide Lava-base rule regardless of the lower temperature value.

**Open questions:** Exact item inputs and yields.

### Molten Raw Netherite Scrap → Molten Netherite → Living Netherite

**Status:** Chain confirmed, supersedes the earlier direct "Netherite Scrap + Living Gold" draft. Exact yields not yet decided.

**What it is:** A three-stage chain giving Netherite a full Molten/Living pipeline, deliberately deeper than the single-step Molten fluids above — fitting Netherite's status as the rarest material in the chain.

**Stage 2 — Molten Raw Netherite Scrap:** Netherite Scrap + Lava, via the Masticator. Base fluid: Lava.

**Stage 2 — Molten Netherite:** Molten Raw Netherite Scrap + **Gold Blend** (Aurous Blend — see Metal Blends above), via the Masticator. Reuses an existing Stage 1 fluid as an ingredient, consistent with the mod's "every ingredient has more than one use" convention.

**Stage 3 — Living Netherite:** Molten Netherite + **Living Catalyst** → Living Netherite, via the **Gestator** (same pattern as Living Glowstone above). Requires an evolved Gestator/machine capable of handling Mild Radiation input, consistent with the rest of the Living Metal family. `Hazardous → Radiation → Mild`, inherited the same way Living Gold's classification worked in the earlier draft.

**Open questions:** Exact yields at each of the three stages. Whether the evolved-machine hazard-handling requirement is a blanket capability or specific to this recipe. Color/FluidType values for Molten Raw Netherite Scrap, Molten Netherite, and Living Netherite.

### Blaze Essence

**Status:** Confirmed as a Stage 2 fluid; item input/yield not yet decided. (No "Molten" prefix — see naming split above.)

**What it is:** Blaze Rod/Powder processed with Lava — a creature-drop "Essence," not a literal melt. Confirmed use as one of Living Catalyst's three fluid inputs (see `dermicraft-catalyst-notes.md`).

**Base fluid:** Lava.

**Item inputs & yield:** Presumably Blaze Rod and/or Blaze Powder. Not yet decided.

**Open questions:** Exact item inputs and yields.

### Ghast Essence

**Status:** Confirmed as a Stage 2 fluid; item input/yield and use case not yet decided. (No "Molten" prefix — see naming split above.)

**What it is:** Ghast Tear processed with Lava — a creature-drop "Essence," matching Blaze Essence's naming and role.

**Base fluid:** Lava.

**Item inputs & yield:** Presumably Ghast Tear. Not yet decided.

**Open questions:** Exact item inputs and yields. What it's actually used for — no confirmed recipe consumes it yet.

### Wither Essence

**Status:** Source confirmed; item input/yield and use case not yet decided. (No "Molten" prefix — see naming split above.)

**What it is:** Sourced from **Wither Skeletons** (not the Wither boss itself) — deliberately pitched at Nether-fortress difficulty, not boss-fight difficulty. A separate, later, harder-tier fluid (tentatively **Liquid Nether Star**) is reserved as the actual Wither-boss-tier counterpart, keeping the two power levels distinct rather than collapsing them into one fluid.

**Base fluid:** Lava.

**Item inputs & yield:** Presumably Wither Skeleton Skull and/or Bone/Coal drops. Not yet decided.

**Open questions:** Exact item input(s) and yields. What it's used for. Liquid Nether Star remains fully undefined beyond being reserved as a name/concept.

### Ender Essence

**Status:** Source, naming, placement, and hazard classification confirmed; item input/yield and use case not yet decided.

**What it is:** Ender Pearl processed with Lava — a creature-drop "Essence" (see naming split above), sourced from Endermen. **Placement note:** Endermen spawn in both the Nether and the Overworld, so despite Ender Pearl's End association, gathering it doesn't actually require Stage 3/End access — this is why Ender Essence sits here in Stage 2, earlier than the rest of the mod's End-tier content (deferred elsewhere — see the Sediment Blend blacklist's "End rocks deferred to a later Stage" note), on the strength of *when the player can acquire the material* rather than its thematic dimension.

**Base fluid:** Lava.

**Item inputs & yield:** Presumably Ender Pearl. Not yet decided.

**Color:** Intended to read as a liquefied Ender Pearl — leaning on the pearl's own color rather than a generic Lava-derived tint. Exact value not yet decided.

**Hazard tags (confirmed):** `hazard/metaphysical_severe` + `hazard/extreme_heat` (the latter inherited from the family-wide Lava-base rule above). Metaphysical Severe fits thematically — its signature effects (hallucination-made-real, or a telegraphed instant dimensional round-trip — see `dermicraft-hazard-effects-notes.md`) mirror Enderman/End teleportation almost exactly. Second confirmed use of the Metaphysical tag family (after Molten Soul Silica's Metaphysical Mild), and the first confirmed use of the Severe half — another dual-hazard-axis fluid, same shape as Molten Soul Silica.

**Recipe use — first confirmed consumer (new):** **Blaze Powder + Ender Essence → 2 Eyes of Ender**, via the Mutator (see `dermicraft-machine-notes.md` → Mutator) — a reagent-mapping of vanilla's Eye recipe (the pearl becomes its fluid form, the blaze component stays physical), at double vanilla's per-powder yield (machine-efficiency payoff). Thematically exact: the fluid whose hazard is justified by Enderman-teleport imagery now makes the teleport-tracking item. **Gating resolved by the Metaphysical Mind Rule** (see `dermicraft-hazard-effects-notes.md` → Metaphysical vs. machines): Metaphysical hazard only affects things with minds, and the Mutator is a dumb (Brain-free) machine — so only Ender Essence's Extreme Heat tag gates, requiring a **Tier 2 Mutator**. The recipe lands at **Stage 2**. Ghast Essence remains the one with no consumer.

**Open questions:** Exact item inputs and yield. Exact color value. (The old "what machine capability handles Metaphysical Severe" question is resolved for *dumb machines* by the Mind Rule — smart structures' protection mechanic remains open, tracked in `dermicraft-hazard-effects-notes.md`.)

### Molten Soul Silica

**Status:** Confirmed as a Stage 2 fluid, standalone (not part of the Sediment Blend cross-feed web); item input/yield not yet decided.

**What it is:** Soul Sand in fluid form — the Stage 2/lava-based parallel to Stage 1's Silica Blend (Sand + Water), giving the Nether's sand variant its own liquid identity. Deliberately **does not** cross-feed with Stone/Silica/Clay Blend — Soul Sand is treated as a different nature of material despite vanilla's shared sand tag, not a fourth member of that web.

**Base fluid:** Lava.

**Item inputs & yield:** Presumably Soul Sand (+ possibly Soul Soil). Not yet decided.

**Possible future hook — Silica Blend conversion:** A possible (not committed) future recipe converting Silica Blend directly into Molten Soul Silica, Create-mod-style, has been floated — no mechanic or machine decided yet.

**Possible future hook — gameplay-effect carryover:** Soul Sand's real block effects (mob-spawn suppression, entity slow) could carry over as a pooled-fluid property, echoing how Stone/Clay/Silica Blend each model a real trapping hazard. Not committed. (Separate from the hazard-*tag* below — this is about mimicking the block's actual gameplay behavior, not the `hazard/*` tag system.)

**Hazard tag — Metaphysical Mild (confirmed, new, distinct from the gameplay-effect hook above).** Molten Soul Silica carries **Metaphysical Mild** (`hazard/metaphysical_mild`) — the first confirmed use of the Metaphysical hazard tag, which had otherwise been added speculatively with no assigned content. Also carries **Extreme Heat** per the family-wide Lava-base rule above, so Molten Soul Silica is hazardous on two independent axes at once — a real test case for the set-based `HazardProfile` model (see `dermicraft-project-primer.md` → Hazard tag hierarchy implementation note) rather than a single-tag fluid.

**Open questions:** Exact item inputs and yields. Whether the Silica Blend conversion hook gets built. Whether the gameplay-effect carryover hook gets built. (The old "what handles Metaphysical Mild" question is resolved for dumb machines by the **Mind Rule** — Metaphysical only affects things with minds, so Brain-free machines process it freely and only the Extreme Heat tag gates; see `dermicraft-hazard-effects-notes.md` → Metaphysical vs. machines. Smart-structure protection remains open there.)

### Molten Prismarine (reserved)

**Status:** Name reserved only — no recipe, use case, or values decided. Deliberately left undesigned until a concrete use is found, rather than filled in speculatively.

---

## Stage 4 — All Metal

**Status:** Recipe and hazard classification decided. Handling method and full purpose scope open. **Confirmed as Stage 4** — the mod's first fluid placed there, since it requires all four Living Metals (themselves a Stage 3 product) to already exist.

### All Metal

**What it is:** The mod's top-tier Living Metal — unifies all four Living Metals into one fluid. Despite the name, it's still a Living Metal by mechanic (self-replicating, same family as Living Iron/Copper/Gold/Netherite), even though "Living" doesn't appear in its name.

**Recipe:** 1000 mB each of **Living Iron + Living Copper + Living Gold + Living Netherite** → 1000 mB All Metal, via a **4-input Effluentcer** — a further evolution of the 3-input Effluentcer built for the Living Catalyst recipe (see `dermicraft-machine-notes.md`), not a separate branch. This makes the Effluentcer's evolution line at least three stages deep: base (2 input) → Living Catalyst tier (3 input) → All Metal tier (4 input).

**Hazard classification:** Dual-tagged — `Hazardous → Radiation → Severe` **and** `Hazardous → Biohazard`. First fluid in the mod carrying two hazard classifications at once, and the **first confirmed use of the Biohazard tag** (Dragon's Milk, above, is the second). Fits combining four already-hazardous living materials into something categorically more dangerous than any one of them — Severe Radiation from the sheer concentration of living-metal material, Biohazard from what "four kinds of living metal fused together" implies biologically.

**Intended purpose:** Both a **recipe ingredient** (Ascendant Catalyst and/or other god-tier gear) and a **material in its own right** (tools/armor crafted from All Metal directly) — dual role, matching the primer's "every ingredient should have more than one use" convention, just built in from the start here rather than discovered later.

**Recipe logic:** Living Metals already establish that living materials handle Mild Radiation via toughness + self-repair. All Metal deliberately breaks that comfort — combining all four doesn't produce a bigger version of the same safe handling, it produces something that outstrips it (Severe Radiation) and adds an entirely new problem (Biohazard) on top. Mirrors Ascendant Catalyst's own "something this powerful shouldn't be casually safe" design principle.

**Open questions:** How Biohazard as a hazard tag actually gets handled — brand new territory, no precedent yet (unlike Severe Radiation, which at least has Ascendant Catalyst's open question to eventually piggyback a solution off of). Whether handling Severe Radiation and Biohazard requires two separate solutions or one unified one. Exact FluidType/color values. Whether the 4-input Effluentcer needs anything beyond Evolution Catalyst to reach (same evolution-trigger open question as the 3-input version).

---

## Template for new entries

```
### [Name]

**Status:**

**What it is:**

**Base fluid:**

**Item inputs & yield:**

**Recipe logic:**

**Alternates considered:**

**Open questions:**
```
