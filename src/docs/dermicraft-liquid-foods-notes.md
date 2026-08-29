# Dermicraft Liquid Foods Notes

Running log of decided design choices for the liquid-foods system — edible fluids the player drinks from the Feeder Bladder — and the reasoning behind them. Add a new entry per edible fluid as decisions get made; the "why" matters as much as the "what" so later additions stay consistent with earlier logic.

---

## Liquid foods — overview

**Status:** System model and the first three edible fluids decided. Per-fluid secondary effects deferred (hook exists, unapplied). Suit-integrated Feeder Bladder module and grade/Heal scaling explicitly out of scope for v1.

**What it is:** A system for treating certain fluids as drinkable food. The player drinks an edible fluid out of the **Feeder Bladder** (a `BladderItem` variant, already registered as `FEEDER_BLADDER`); each drink drains a fixed amount of fluid and restores hunger + saturation, like vanilla food.

**Mental model:** *vanilla food + a flat mB tax per bite.* An edible fluid is essentially a `FoodProperties`-style entry (hunger + saturation) with an added "mB drained per drink" cost. Hunger and mB cost are **decoupled** — deliberately, so a fluid can be "expensive to drink but very filling" or "cheap sips of weak filler" independently. There is no fluid-to-hunger conversion constant tying them together (an earlier draft used one; dropped).

### Data model — `edible_fluid` data map

The single source of truth for "is this fluid drinkable, and what does it do." A NeoForge data map under `datagen/datamaps/`, keyed by **source fluid id**, following the same pattern as the hazard-profile data map. Registered in `DataGenerators.java`, populated by a new provider in `datagen/datamaps/`.

Per-entry shape:

```
dermicraft:edible_fluid  ->  {
    mbPerDrink:   int,      // fluid drained per drink action
    hunger:       int,      // hunger points restored per drink (vanilla nutrition)
    saturation:   float,    // vanilla saturationModifier (finalSat = hunger x mod x 2)
    effect:       optional  // MobEffectInstance — parsed and stored, NOT applied for now
}
```

- **A fluid absent from the map is not edible.** No tag, no flag elsewhere — presence in this map is the whole test.
- `saturation` uses vanilla's `saturationModifier` convention (`finalSat = hunger x mod x 2`), to read the same as every other food in the mod (`property/ModFoodProperties.java`) rather than a bespoke flat number.
- `effect` is the **deferred hook.** Parsed and stored now but never applied. When per-fluid effects get designed later, fill in the map entries and flip on one line in the drink handler — no structural change.

### Drink mechanic (Feeder Bladder)

- `use()` returns the vanilla **DRINK** `UseAnim` with the standard drink duration — hold-to-drink, feels like food.
- On `finishUsingItem`: read the held fluid from the `FluidData` component -> look up its `edible_fluid` entry -> drain `mbPerDrink`, restore `hunger` + `saturation`.
- **Not `alwaysEdible`** — drinkable only when the player isn't at max hunger, exactly like real food.
- **Partial sip at the bottom of the bladder is disallowed:** if less than `mbPerDrink` remains, you can't drink (behaves like an almost-empty bottle — no scaled partial swig).
- **Fallthrough:** an empty bladder, or one holding a non-edible fluid, falls through to the existing `BladderItem` bucket-style pickup/place behavior. The same item stays a general fluid container when it isn't feeding.

### Out of scope for v1 (by design)

- **Per-fluid secondary effects** — hook exists in the data map, unapplied. "Treating these fluids like basic food for now."
- **Healing** — not a property; these are food, not medicine.
- **Suit-integrated Feeder Bladder module** (the Chest-slot version in `dermicraft-suit-notes.md` that auto-feeds from suit fuel) — this covers the *handheld* item only; the suit module can reuse the same data map later.
- **Grade / Heal-modifier scaling** (the mB-per-hunger table in `dermicraft-slurry-notes.md`) — the flat per-fluid map is v1; grade scaling could layer on later without restructuring.

**Open questions:**
- Which fluids beyond the first three become edible? (All food-derived Slurries/Blends, or a curated subset?)
- Does the handheld Feeder Bladder and the suit module share one data map cleanly, or does the suit version need its own cost model?
- Should `mbPerDrink` ever vary per fluid, or stay uniform? (Uniform 250 for now — see below.)

---

## Edible fluids

**Current roster:** Crude Slurry, Protein Blend, F-Stuff. All three drain **250 mB/drink** to start — Feeder Bladder holds 2000 mB, so 250 mB/sip = **8 drinks per full bladder**, a readable ratio. Uniform sip size is a starting choice, not a rule; the whole point of decoupling mB from hunger is that a fluid can later take a bigger/smaller sip independently.

| Fluid | mB/drink | Hunger | Saturation | Identity |
|---|---|---|---|---|
| Crude Slurry | 250 | 3 | 0.1 | Thin plant filler — keeps you alive, burns off fast |
| Protein Blend | 250 | 5 | 0.3 | Meat-rich — filling now, less staying power |
| F-Stuff | 250 | 4 | 0.5 | Processed field rations — efficient, long-lasting |

Numbers are starting points to tune in testing; the *structure* is the decision, not the exact ints.

### Crude Slurry

**Status:** Values decided.

**What it is:** The mod's baseline plant-derived fluid — made from a puddle-crafting recipe consuming `dermicraft:plant_food`-tagged items in water (see `dermicraft-slurry-notes.md`).

**Values:** 250 mB/drink, 3 hunger, 0.1 saturation.

**Why:** Thin plant filler — the cheapest, weakest edible fluid. Low saturation means it doesn't stick with you; it's the "keeps you from starving" tier.

### Protein Blend

**Status:** Values decided.

**What it is:** The meat-derived fluid — masticated from `dermicraft:meat_food`-tagged items in water.

**Values:** 250 mB/drink, 5 hunger, 0.3 saturation.

**Why:** Highest raw hunger of the three, standing in for fresh meat. Good saturation but not the best — it fills you now more than it lasts.

### F-Stuff

**Status:** Values decided.

**What it is:** A processed plant+meat byproduct blend — the effluencing recipe combines **250 mB Crude Slurry + 250 mB Protein Blend -> 500 mB F-Stuff** (1:1 volume, no loss).

**Values:** 250 mB/drink, 4 hunger, 0.5 saturation.

**Why — the processing must pay off:** Because F-Stuff is made from equal parts of the other two, setting its hunger at the plain average (4) makes it a **break-even** — 500 mB F-Stuff would carry the exact same total hunger as drinking its 500 mB of raw inputs, so processing would gain you nothing but logistics. That's a dead recipe.

The fix rewards the tech investment through **saturation, not raw hunger.** F-Stuff is deliberately *not* pushed above Protein Blend on hunger — it's a diluted byproduct, and out-filling fresh meat on raw hunger reads wrong. Instead it gets the **best saturation of the three (0.5)**, making it the **liquid field-rations** tier: modest peak hunger, but it stays with you the longest. This mirrors how the MRE item already works (nutrition 6, sat 0.6, "on par with cooked chicken"). Now processing is worth it — you trade a little peak hunger for markedly better staying power plus single-fluid logistics, and F-Stuff becomes the thing you make once you've built the Effluencer, not a pointless sidegrade.

**Open questions:** None blocking. Exact hunger/saturation split is tunable; if raw-hunger reward is preferred later, F-Stuff at 5 hunger (beating the input average) is a one-int change — but the saturation version is the starting call.

---

## Template for new entries (per edible fluid)

```
### [Fluid Name]

**Status:**

**What it is:**

**Values:** [mB/drink], [hunger], [saturation]

**Why:**

**Open questions:**
```
