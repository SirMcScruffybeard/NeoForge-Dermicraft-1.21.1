# Dermicraft Slurry Notes

Running log of decided design choices for the Slurry family (Dermicraft's fuel fluids) and the reasoning behind them. Add a new entry per slurry as decisions get made — the "why" matters as much as the "what" so later additions stay consistent with earlier logic.

---

## Slurry property model

Each slurry has three tracked properties:

- **Speed modifier** — how much work is done per machine process cycle. Higher = more work per cycle.
- **Use rate modifier** — how much slurry is consumed per machine process cycle. Lower = more efficient (less consumed per cycle).
- **Heal modifier** — how much HP is restored to the machine per cycle while fueled. Higher = faster recovery from health drain.

**Two layers of numbers — read this before touching any value in this doc.** There are two correct-but-different sets of numbers for speed and heal, and mixing them up is the easy way to introduce an error:

- **Raw / data-map value** — what's literally written in `biofuels.json` (and what you'd type when registering a new slurry). Crude Slurry's raw entry is `speed: 0.1`, `heal: 0.1`.
- **Effective / gameplay value** — what actually determines in-machine behavior. `FuelTank.java` divides the raw speed and heal values by `BASE_SPEED_MODIFIER` (`0.1f`) before using them anywhere (`getSpeed()`, `getHeal()`). So **effective = raw ÷ 0.1 = raw × 10**. Crude's raw `0.1` becomes an effective **`1.0`** — this is the number that actually matches "1 progress per cycle" in the health/fuel system (see `dermicraft-machine-notes.md`).
- **Use rate does not get this treatment** — `FuelTank.getRawUseRate()` uses the raw value directly, no division. So for use rate, raw and effective are the same number (Crude: `1.0` either way). It's scaled by `CRAFT_TICKS` (and rounded once, at that point) in `AbstractFueledMachineBlockEntity.setUseRate()`, not in `FuelTank` itself.

**Crude Slurry's baseline — both layers:**

| | Speed | Use Rate | Heal |
|---|---|---|---|
| **Raw (biofuels.json)** | `0.1` | `1.0` | `0.1` |
| **Effective (gameplay)** | `1.0` | `1.0` | `1.0` |

Every other Main Line slurry is derived relative to this baseline — not picked independently per slurry. **When reasoning about design/balance ("how fast is X," "is this a fair curve"), use the Effective numbers below — that's what the player actually experiences.** The Raw numbers only matter when you're the one writing or checking a `biofuels.json` entry.

**Main Line rule:** On the Main Line, heal modifier always matches speed modifier — the same value does double duty. This keeps the property model simple while still leaving room for the experimental branch to decouple them (a branch slurry with high heal but low speed, or vice versa, would create genuinely different strategic choices).

## Fluid-to-Hunger Conversion (mod-wide standard)

**Status:** Formula and baseline confirmed. Applies wherever a fluid restores player hunger — currently the **Fuel Bladder Add-on** and the **Dock's** player-heal bonus (see `dermicraft-suit-notes.md` and `dermicraft-gear-worx-notes.md`). One shared rate, not designed per-mechanic, per the mod's general "one derived formula, reused everywhere" convention.

**Formula:** `mB per hunger point = Base(Crude) mB ÷ effective Heal modifier` — reuses the existing Heal modifier (see Slurry property model, above) as the basis, rather than introducing a new stat. Better fuel grades restore hunger more cheaply, same "better grade = more efficient" pattern as the FL's own heal→fuel-use-rate repurposing.

**Unit:** 1 "hunger point" = 1 raw hunger unit (i.e. half a drumstick icon; vanilla hunger is tracked 0-20, 2 units per icon).

**Baseline:** **50 mB (Crude) per hunger point** — chosen as the middle ground between two tested extremes (10 mB felt negligible, 100 mB felt punishing).

**Resulting table (Main Line):**

| Slurry | Heal modifier | mB per hunger point |
|---|---|---|
| Crude | 1.00 | 50.0 |
| Concentrated | 1.25 | 40.0 |
| Refined | 1.75 | ~28.6 |
| Enriched | 2.50 | 20.0 |
| Superior | 3.50 | ~14.3 |

**Excluded:** the **Feeder Craw Add-on** does not use this formula — it feeds the player from **stored food items directly**, not fluid, so there's no conversion rate to apply.

---

## Main Line — overview

The "Main Line" is the primary slurry progression, in order:

1. Crude
2. Concentrated
3. Refined
4. Enriched
5. Superior

**Progression rule:** Moving down the line, speed increases and use rate decreases at each step. This is **not a trade-off** — each slurry does more work per cycle *while consuming less* per cycle than the one before it. Strictly more efficient at each step, not a "faster but burns more" relationship. Heal rate climbs with speed on the same curve.

**FluidType motion scale curve (confirmed, fixes a bug in Crude's original value).** Crude Slurry's motion scale was originally set to `0.08` as a deliberate "wrongness" trick copying Primitive Catalyst's inverted-motion-scale idea — this was a mistake, not the intent, and has been corrected to `0.0100` (the physically normal value for its viscosity). The corrected design for the whole Main Line: **early slurries behave like a normal thick fluid** (motion scale tracks viscosity the expected way, same relationship vanilla water→lava uses), and **later slurries get progressively more uncanny** — motion scale breaks from the expected downward trend and climbs instead, echoing the mod's general "more refined/potent = more unnatural" pattern. Superior ends up moving almost like water despite being nearly as viscous as lava.

| Slurry | Viscosity | Motion Scale | Character |
|---|---|---|---|
| Crude | 4000 | **0.0100** | normal (fixed from the mistaken `0.08`) |
| Concentrated | 4300 | 0.0090 | normal, still tracks viscosity down |
| Refined | 4600 | 0.0080 | last normal one |
| Enriched | 5000 | 0.0160 | breaks the trend — jumps up, first wrongness |
| Superior | 5500 | 0.0450 | most uncanny — barely resists movement despite near-lava thickness |

Viscosity/density/temperature for Refined/Enriched/Superior are target numbers for when those fluids get built, not locked — only Crude (implemented, fixed) and Concentrated (see its own entry below) have confirmed values so far.

**Progression shape:** Speed follows an **accelerating curve** — each step adds more than the last. Use rate mirrors this as drops that also widen each step (-0.10, -0.15, -0.20, -0.25 — use rate has only one layer, see above). Superior is intentionally a dramatic leap above the Crude→Enriched steady climb — it sits in its own category rather than just being the next rung. All values are provisional and easy to adjust without breaking the underlying pattern.

**Reference table — Effective (gameplay) values.** Use this table for all design/balance reasoning:

| Slurry | Speed | Use Rate | Heal |
|---|---|---|---|
| Crude | 1.00 | 1.00 | 1.00 |
| Concentrated | 1.25 | 0.90 | 1.25 |
| Refined | 1.75 | 0.75 | 1.75 |
| Enriched | 2.50 | 0.55 | 2.50 |
| Superior | 3.50 | 0.30 | 3.50 |

Step deltas (effective speed/heal): +0.25, +0.50, +0.75, +1.00.

**Reference table — Raw (biofuels.json) values.** Use this table only when writing or checking a data-map entry — divide effective speed/heal by 10 to get these:

| Slurry | Speed | Use Rate | Heal |
|---|---|---|---|
| Crude | 0.10 | 1.00 | 0.10 |
| Concentrated | 0.125 | 0.90 | 0.125 |
| Refined | 0.175 | 0.75 | 0.175 |
| Enriched | 0.25 | 0.55 | 0.25 |
| Superior | 0.35 | 0.30 | 0.35 |

**Open question:** "Main Line" naming implies there could be branch/side lines later (a name like that usually leaves room for an offshoot). Worth confirming whether this is the complete slurry roster or just the primary path before treating it as closed.

**Branch flagged — now resolved into the Serum family below.** An **experimental branch** of slurries had been raised as a real possibility, not just a naming coincidence — defined at the time simply as: slurries that don't fit the Main Line's derived-from-baseline progression (i.e. not just another speed-up/use-rate-down step relative to the previous one). That branch has since been scoped and named: see **Serum family**, below. Branch slurries were flagged as the natural place for heal and speed modifiers to diverge — the Main Line keeps them locked together, but nothing prevents a branch entry from having mismatched values; Serums confirm this, with Booster Serum's negative heal modifier being the first real example. Some Main Line content in this doc may still get reworked as Serum recipes come online — current entries are the latest agreed state, not guaranteed final.

---

## Crude Slurry

**Status:** Fully decided — first slurry, baseline properties, true identity clarified, and a growing list of secondary uses.

**What it is:** First slurry made by the player, and also the lowest-tier fuel on the Main Line — these are the same fact, not two separate ones (chronological-first and weakest-tier coincide here). Dark green, originally colored to represent plant life.

**Real identity — clarified:** The plant-life coloring was always more of a *visual* association than the actual mechanism. Crude Slurry's true underlying identity is **nutrient-rich** — its working title during early development was actually **"Nutrient Slurry"** before that name was dropped in favor of "Crude." This nutrient-dense identity is what actually explains *all* of its uses consistently: forcing accelerated plant growth (Flask application, bone-meal-style effects) and forcing accelerated animal growth/healing (Syringe on passive mobs, the Inert Tumor healing recipe — see `dermicraft-tools-notes.md`) are the same mechanism — force accelerated biological growth/repair — just applied to two different categories of living thing. Plant association is the flavor; "nutrient-dense, forces growth" is the actual rule.

**Properties:**
- Speed modifier: **effective `1.00`** (raw `0.10` in `biofuels.json`)
- Use rate modifier: `1.00` (raw and effective are the same for use rate)
- Heal modifier: **effective `1.00`** (raw `0.10` in `biofuels.json`)

**Why these are the baseline:** First slurry in the Main Line — every later slurry's properties are derived relative to these numbers (using the effective values).

**FluidType (confirmed, implemented):** tint `0xFF4FA757` (medium green — "dark green, plant life" flavor). `viscosity(4000)`, `density(3000)`, `temperature(285)`, `motionScale(0.0100)` — the motion scale value was fixed from an earlier mistaken `0.08` (see Main Line — overview, above, for the corrected family-wide curve). `canHydrate(true)`.

**Secondary uses (confirmed, growing list):**
- Base fluid for Primitive Catalyst (see `dermicraft-catalyst-notes.md`).
- Flask of Crude Slurry — converts dirt to grass/mycelium, plants random crops on farmland (see `dermicraft-flask-notes.md`).
- Crude Slurry Syringe — forces instant accelerated effects on passive mobs (sheep wool regrowth, baby-mob maturation, chicken egg-laying, cow milk refresh — roster open-ended). See `dermicraft-tools-notes.md`.
- Marred Tumor healing recipe — raw meat (sutured) + Crude Slurry (injected) heals a Marred Tumor back into a fresh Inert Tumor. See `dermicraft-tools-notes.md`.

**Recipe note:** Can also be produced via the **Masticator** (see `dermicraft-machine-notes.md`) from **water + a plant-based food item** (apple, carrot, bread, etc.) — yield scaled to the food's nutrition/saturation values, the same logic used for Protein Blend and the Drooling Cauldron's food-to-water conversion. Restricted to plant-based items specifically, distinct from Protein Blend's `MEAT_FOOD`-tagged scope — the two food-based recipes split the food-item space cleanly between them (see `dermicraft-crafting-notes.md`).

**Puddle-crafting route (confirmed in code):** Crude Slurry can also be puddle-assembled directly — no machine required — from **4× any `plant_food`-tagged item tossed into a 1×1 puddle of water**, 200 ticks. This is the actual earliest-game bootstrap route (predates having a Masticator at all), sitting alongside the Masticator recipe above as a second confirmed way to produce it.

**Open questions:** None remaining for Crude itself.

---

## Concentrated Slurry

**Status:** Fully implemented — recipe, biofuel properties, and FluidType all live in code.

**What it is:** A direct refinement of Crude Slurry — Crude with a minor catalytic addition of Primitive Catalyst (10:1 ratio, a small dose rather than an equal-parts co-ingredient), reinforcing Primitive Catalyst's identity as a general-purpose processing agent reused across families.

**Recipe (Effluentcer, implemented):** 50 mB Crude Slurry + 5 mB Primitive Catalyst → 50 mB Concentrated Slurry, fixed 50 ticks. Small batch, short cycle by design — see the "steady trickle, not surges" constraint in [[project_concentrated_slurry_recipe]] (Claude Code memory).

**Properties:**
- Speed modifier: effective `1.25` (raw `0.125`)
- Use rate modifier: `0.90`
- Heal modifier: effective `1.25` (raw `0.125`)
- Tier: `1` (Stage 1 fuel, same as Crude)

**Progression note:** First step up from Crude. Speed increase of +0.25 (effective); use rate drop of -0.10. The smallest step on the accelerating curve — intentionally modest, keeping early progression steady rather than dramatic.

**FluidType (confirmed, implemented):** tint `0xFF3D8A47` — a darker, more saturated version of Crude's green (`0xFF4FA757`) rather than a hue shift toward Primitive Catalyst's brown, since "concentrated" should read as more of the same thing, not a different thing. `viscosity(4300)`, `density(3200)`, `temperature(288)`, `motionScale(0.0090)` — the second point on the Main Line's normal→uncanny motion-scale curve (see Main Line — overview, above). `canHydrate(true)`, matches Crude's identity since it's mostly Crude by volume.

**Tags:** `THICK` (Beaker/Glass Flask/Syringe fill-level rendering) and `BIOFUELS` — same tag set as Crude.

**Open questions:** None remaining for Concentrated itself.

---

## Refined Slurry

**Status:** Position and values decided (provisional).

**What it is:** TBD.

**Properties:**
- Speed modifier: effective `1.75` (raw `0.175`)
- Use rate modifier: `0.75`
- Heal modifier: effective `1.75` (raw `0.175`)

**Progression note:** Speed increase of +0.50 (effective) over Concentrated; use rate drop of -0.15. The curve begins to widen noticeably here.

**Open questions:** Recipe/ingredients.

---

## Enriched Slurry

**Status:** Position and values decided (provisional).

**What it is:** TBD.

**Properties:**
- Speed modifier: effective `2.50` (raw `0.25`)
- Use rate modifier: `0.55`
- Heal modifier: effective `2.50` (raw `0.25`)

**Progression note:** Speed increase of +0.75 (effective) over Refined; use rate drop of -0.20. The last step of the steady Crude→Enriched climb before Superior's dramatic leap.

**Open questions:** Recipe/ingredients.

---

## Superior Slurry

**Status:** Position and values decided (provisional). Intended as the Main Line ceiling.

**What it is:** TBD. "Superior" is meant to signal the line's endpoint — worth confirming whether anything comes after it or if this is intentionally the ceiling.

**Properties:**
- Speed modifier: effective `3.50` (raw `0.35`)
- Use rate modifier: `0.30`
- Heal modifier: effective `3.50` (raw `0.35`)

**Progression note:** Speed increase of +1.00 (effective) over Enriched; use rate drop of -0.25. The largest single step on the curve — deliberate. Superior is intended to feel categorically ahead of Enriched rather than just the next rung on the ladder. At 0.30 use rate, a fully fueled Superior machine consumes less than a third of what Crude burns while processing at 3.5x the speed. The challenge of obtaining it is expected to justify this.

**Open questions:** Recipe/ingredients. Confirmation that this is truly the Main Line's endpoint.

---

## Serum family — overview

**Status:** Family identity, core mechanic, and first two members decided. Recipes, ingredients, and the fuel-swap item mechanic are all still open.

**What it is:** A second fuel family, independent of the Main Line — related to Slurries only through recipes (a Main Line Slurry will likely serve as an ingredient in most or all Serum recipes, not yet detailed). Where the Main Line is built around sustained efficiency (more work per cycle, less fuel consumed, the longer you use a better one), Serums are built around a fast, high-impact burst with a real cost attached — burn hot, burn fast, pay for it.

**Relationship to the existing fuel/health system:** Serums use the exact same three-stat model as Slurries — speed modifier, use rate modifier, heal modifier (see Slurry property model above) — just pushed to extremes or inverted, rather than needing any new stat. This keeps the underlying system unified: a machine doesn't need to know whether it's burning a Slurry or a Serum, just what the loaded fuel's stats currently are.

**Defining mechanic — unconditional burn.** This is the property that separates the family from Slurries entirely: once a Serum is loaded into a machine's fuel tank, it burns down at its use-rate **regardless of whether the machine is actively processing, idle, or has no valid recipe queued at all.** Contrast with Slurries, which only factor into the health/drain system while a machine is actively processing (idle Slurry-fueled machines take no damage and, implicitly, don't consume fuel). A Serum is a lit fuse the moment it's in the tank — loading one without immediately putting it to use is a straightforward waste.

**One fuel tank, one fuel, mutually exclusive.** Every machine has a single fuel tank and can hold only one fuel at a time — a Slurry or a Serum, never both, never two Serums. There's no "stacking" a Serum's burst on top of Slurry fueling.

**Swapping fuel mid-burn:** The player can pull a Serum out before it finishes burning and replace it with something else, using any ordinary fluid-handler item (bucket, Beaker, Flask, and similar — not restricted to a specific dedicated tool). This is a normal fill/drain interaction against the fuel tank, the same as topping off or swapping a Slurry — no new mechanic required. Gives the player a genuine escape hatch if a Serum gets loaded with nothing queued to use it on.

**Negative heal modifier — new to the property model.** Serums are the first fuel type to use a heal modifier below zero, repurposing the existing stat as a damage-over-time value instead of a healing one. No new stat was needed to support Booster Serum's drawback — the sign of the existing heal modifier just flips.

**Progression placement:** Targeted for **Stage 2** introduction. This is primarily a pacing/balance decision (players have more machines running by this point, more reason to want a burst tool) rather than a strict thematic requirement — but there's a real possibility Serum recipes end up gated behind thermal hazard (Lava), which would tie the family back into Stage 2's established "forged with heat, not soaked" identity (see Crafting fluids in the primer) without that link being mandatory.

### Healing Serum

**Status:** Core mechanic decided. Recipe not yet defined.

**What it is:** The restorative half of the Serum family — burns fast for a large, immediate boost to machine health, at the total cost of that machine's ability to process anything for the duration.

**Properties:**
- Heal modifier: high — significantly faster healing than any Main Line Slurry offers.
- Speed modifier: **flat 0** while active — not "processing suspended," the modifier itself is forced to zero, so a machine fueled by Healing Serum genuinely cannot make progress on any recipe, valid or not, for as long as it's burning.
- Use rate: burns fast (exact value TBD, family-wide burn speed said to vary per Serum).

**Open questions:** Exact heal modifier value. Exact use-rate/burn-speed value. Recipe/ingredients (a Slurry is expected to be involved).

### Booster Serum

**Status:** Core mechanic decided. Recipe not yet defined.

**What it is:** The aggressive half of the Serum family — burns fast for a large boost to processing speed, at the cost of directly damaging the machine for as long as it's active.

**Properties:**
- Speed modifier: high — significantly faster processing than any Main Line Slurry offers.
- Heal modifier: **negative** — actively damages the machine's HP per cycle while fueled, using the same stat that normally heals.
- Use rate: burns fast (exact value TBD; burns until gone regardless of processing state, per the family-wide unconditional-burn rule).

**Open questions:** Exact speed and negative-heal (damage) modifier values. Whether Booster Serum alone can fully drain a machine to 0 HP mid-burn, forcing it into the starved state the moment the burn ends. Exact use-rate/burn-speed value. Recipe/ingredients.

### Open questions — Serum family, general

- Exact recipes/ingredients for both known Serums — a Main Line Slurry is expected as a component but nothing is locked in.
- Whether Serum recipes end up requiring Lava/thermal hazard, tying the family thematically to Stage 2 rather than just placement-wise.
- Whether more Serums are planned beyond Healing and Booster.
- Exact numeric values across the board (heal, speed, use rate) for both current members.

---

## Template for new entries

```
### [Name]

**Status:**

**What it is:**

**Properties:**
- Speed modifier:
- Use rate modifier:
- Heal modifier:

**Recipe/logic:**

**Alternates considered:**

**Open questions:**
```
