# Dermicraft Progression System — Overhaul Notes

Running log for the progression-system overhaul: stretching the Module system's flexibility (see `dermicraft-gadget-notes.md` → "Modules — direction note") past hazard tolerance and past Eater, to a consistent mod-wide framework for how the player unlocks/customizes capability. Companion to `dermicraft-gadget-notes.md`, `dermicraft-suit-notes.md`, `dermicraft-machine-notes.md`, and `dermicraft-hazard-effects-notes.md`.

**Why this doc exists, separate from the others:** every other progression-adjacent doc describes its own consumer's numbers. This one is the cross-cutting pass — the taxonomy that decides *which mechanism* a given capability should use, audited against everything that already exists, so future capabilities (and reworks of existing ones) have a rule to check against instead of an ad hoc call each time.

---

## The three mechanism shapes (proposed framework, not yet ratified)

Everything in the mod that gates or grows player/gadget/suit/machine capability already reduces to one of three shapes. None of these are new — each already exists somewhere in the codebase or design corpus — the overhaul is naming them explicitly and auditing every consumer against the set, per the "build the taxonomy first" starting point already logged in the Modules direction note.

### 1. Tier ladder
Permanent, sequential, forced or semi-forced progression. Advancing consumes the previous tier's item/recipe cost; there's no "undo." Grants **capacity and access**, not fine-grained choice.

- Code: `HazardProfile.TIER_1..TIER_4`, `MachineTier`, `NodeTier`, `TieredFloor`, gadget tiers (Eater/Drinker/Sprayer), suit Mark/Type/Series tiers.
- Mechanism: forced-evolution (Evolution Catalyst injection, dumb machines) or willing/button-press evolution (smart structures) or Growth Chamber (suits/gadgets, station not yet built — `dermicraft-project-primer.md` lines 159-174).
- What it answers: "What is this consumer *permanently* capable of right now."

### 2. Points
Permanent currency, spent once, never reclaimed. Buys either a smooth incremental stat rank or a discrete ability unlock. Each consumer (a suit, a gadget) holds its **own separate pool** — shared mechanic, not a shared currency (`dermicraft-gadget-notes.md` line 37).

- Code: none yet — design-only for Eater's material categories, M.A.W.S.'s features; suits' own point-spend menu at the Dock is further along but numbers aren't locked either.
- Earned via: fixed grant per tier-up, plus an escalating-cost buy-in once at max tier (same rule for suits and gadgets, not invented twice).
- What it answers: "How much / which permanent abilities has the player chosen to buy."

### 3. Modules
Destructible, physical, slot-limited items. Field-swappable (Scrench for gadgets; the suit's own swap path). Can **locally exceed** what tier/points alone would currently allow, and — where that's their purpose — are **self-resolving**: the same physical item gets consumed as a tier-up ingredient once the permanent tier catches up to what it was covering.

- Code: `MODULE_DATA`/`SAFETY_MODULE_PROPERTIES` on `EaterItem` only. Kind identified by item tag (`module/safety_heat`, `module/ore`, etc.), not a Java enum, so third-party items can join a category by datapack tag alone.
- **Resolved (2026-08-19):** the suit Add-on → "Module" rename is naming-only. Suit Modules (formerly Add-ons) keep their exact current behavior — permanent equipment, chosen from a roster, docked in the Add-on Frame, stays equipped until the player unequips it. They do **not** become destructible or self-resolving. Only the gadget-side Safety Modules/mouthpieces (and anything designed after them with the same destructible-and-self-resolving shape) carry that behavior.
- **Residual naming note:** "Module" is now a shared label over two genuinely different behavioral patterns — destructible/self-resolving (gadget Safety Modules, mouthpieces, Fluid Bypass) vs. permanent/equipped (suit Modules, ex-Add-ons). This is intentional (both are "an item in a scarce slot that defines a capability"), but it means the word alone doesn't tell you which behavior applies — always check the specific consumer's own doc rather than assuming destructibility (or permanence) from the name.
- What it answers: "What can this consumer do *right now, locally*, beyond its permanent tier/points state" — for the destructible sub-shape specifically, at the cost of the item's own survival; for the suit sub-shape, at the cost of a scarce slot only.

---

## Audit — existing/designed systems against the taxonomy

| System | Current shape | Taxonomy fit | Notes |
|---|---|---|---|
| Machine hazard access (Masticator, Skin Tank, Drooling Cauldron, Effluencer, FL Floor) | Tier ladder only | **Tier ladder** — correct fit, no change needed | Already follows the exact cumulative rule as everything else (`dermicraft-machine-notes.md` lines 133, 413-421: "each tier handles its own hazard class and all below it"). **The gap isn't the tier system — it's that machines have no Module-equivalent slot at all**, so there's no way to locally exceed a machine's current hazard tier the way Eater can via a Safety Module. |
| Eater's material categories (Aggregate, Plant, Animal, Fireball catching) | Points (designed, not built) | **Points** — correct fit | Smooth/discrete permanent unlocks, "rule of fun over strict logical progression" — explicitly not tier-gated or sequenced. No hazard-flexibility angle at all; stays Points. |
| Eater's Safety Modules / mouthpieces | Modules (designed, not built) | **Modules** — correct fit, template case | The one place all three shapes' interaction is actually worked out: Safety Module = local hazard-tier exception, self-resolving into a Growth Chamber ingredient. |
| Eater's Fluid Bypass | Modules (mundane, non-hazard) | **Modules**, but worth flagging | Confirms Modules aren't only a hazard-flexibility mechanism — a Module can just be "a cheap toggle for a targeting rule." Relevant if a future audit tries to define Modules as "the hazard-flex slot" too narrowly. |
| Drinker's hazard restriction | Hardcoded `HazardProfile.TIER_1` constant, no tier/points/module axis at all | **Should be Modules** (named intent, not built) | `dermicraft-gadget-notes.md` line 77 already confirms Drinker should accept the same Safety Module items as Eater. Currently the single clearest "designed but not wired" gap. |
| Sunder chain material / Shatter head | Swappable physical part, own datamap-driven stats (`SUNDER_CHAIN_PROPERTIES`/`SHATTER_HEAD_PROPERTIES`), plus a separate `PART_UPGRADE_TIER` Mutator-upgrade ladder | **Physical part, not Modules** | Not destructible in the Module sense (an equipped chain/head doesn't get "lost," it's a durability object with its own upgrade path). Correctly stays outside all three shapes above — it's a fourth, narrower pattern ("swappable identity part with its own tier"), scoped to exactly two consumers. No reason to force it into Points or Modules. |
| Suit Modules, formerly Add-ons (Bio Vision Goggles, Red/White Muscle, Achilles/Quad Graft, Grapple Module, etc.) | Physical equipment, docked into a universal Module Frame slot, slots grow with suit tier, magnitude tuned via Points | **Rename-only Module, not a Module in the destructible sense** — hybrid of Tier ladder (slots) + Points (magnitude) + a socketed-equipment pick that behaves like Sunder/Shatter's swappable part | **Resolved (2026-08-19):** naming-only rename, no behavioral change — see the residual naming note under "3. Modules" above. Suit Modules stay permanent/equipped, not destructible, despite sharing the "Module" word with Eater's destructible Safety Modules. The Monster Hunter "decoration socket" framing (`dermicraft-suit-notes.md` line 163) still matches Modules' "scarce slot, swappable" spirit — it just doesn't carry destructibility along with it. |
| Player-facing Hazard Resistance / Hazard Defense (suit stats) | Not a capability gate at all — modifies effect severity/timing of hazards the player is already exposed to | **None of the three — a fourth, separate axis (mitigation, not access)** | Correctly out of scope for this taxonomy; flagged only because of the doc's own noted naming collision with `HazardProfile` (`dermicraft-hazard-effects-notes.md` line 131). Worth a rename pass in this overhaul since "stretching hazard tolerance mod-wide" is exactly the moment two same-named-but-different "hazard" systems will get confused if left alone. |
| Metaphysical hazard's Mind Rule (machines) | A hazard-tag carve-out (Metaphysical bypasses `HazardProfile` for "dumb" machines/ducts entirely) | **Modifies Tier ladder's scope, not a new shape** | Doesn't need its own taxonomy slot — it's a rule about which hazards a Tier ladder even applies to for a given consumer class, orthogonal to Points/Modules. |

---

## Decision Points

1. **✅ Resolved (2026-08-19) — Suit Add-ons → Modules is rename-only.** "Add-on" becomes "Module" mod-wide in naming (suit terminology, the Add-on Frame → Module Frame, etc.), with zero behavioral change: suit Modules stay permanent equipment, chosen and kept, never destructible or self-resolving. Only the gadget-side Safety Modules/mouthpieces (and future designs sharing their purpose) carry the destructible/self-resolving behavior. Actual rename pass (code identifiers, item names, lang keys, this doc's own terminology, `dermicraft-suit-notes.md`) is a separate follow-up task, not done as part of this planning pass.
2. **Does every machine get a Module slot, or only ones with a plausible "local hazard exception" story?** The farming machine family is explicitly hazard-free by design (`dermicraft-machine-notes.md` line 883) — giving it a Safety Module slot would be a slot with nothing to plug into it. Suggests Module slots should be opt-in per machine family, not a blanket `MachineBaseBlockEntity` addition.
3. **Sequencing** (unchanged from the conversational pass, restated here for the written record):
   1. ~~Lock decision #1 above~~ — done.
   2. Generalize the Module *infrastructure* (slot component, item-tag-kind dispatch, data-map-for-structured-modules, swap-item flow) out of `EaterItem` into shared code any consumer can adopt.
   3. Wire Drinker onto it — cheapest second-consumer proof, already named as intended.
   4. Design machine Module slots last, opt-in per family per Decision Point #2, since it's a genuinely new concept (no block-entity precedent yet) rather than an extension of something already proven.
4. **Hazard-naming collision** (`HazardProfile` vs. Hazard Resistance/Defense) — resolve as part of this overhaul or explicitly defer again. Given the overhaul is about stretching hazard-flexibility mod-wide, deferring it a third time risks the confusion compounding rather than staying static.

---

## Open questions (not yet even at "decision point" stage)

- Whether Points, as a mechanism, ever needs its own flexibility story (e.g. respec), or whether Modules is deliberately the *only* flexible/reversible axis and Points staying permanent is the intended contrast.
- Whether a taxonomy-driven audit like this should also cover Sunder/Shatter's swappable-part pattern as a fourth named shape (worth naming if a third consumer besides Sunder/Shatter ever wants it) or stays a two-consumer special case indefinitely.
- Whether machine Points (a magnitude-upgrade currency for machines, mirroring gadgets/suits) is in scope for this overhaul at all, or whether machines are meant to stay Tier-ladder-only by design (unlike every other consumer class) — not discussed anywhere yet, including in `dermicraft-machine-notes.md`.
