## ADDITION SNIPPET — Suit Naming, Tier Scheme, H.E.A.T. Definition, and Mod Numbers

**Target file:** `dermicraft-suit-notes.md`
**Placement:** Add as a new section, suggested placement near the top (after Overview) or as its own section titled "Suit Designations & Naming" before the individual suit breakdowns. Also update the H.E.A.T. references throughout the doc (and in `one-punch-full-breakdown.md`) to reflect the newly confirmed full name.

---

### Suit Full Names (Acronyms Confirmed)

All three suits follow the Gadget naming convention — acronym + full written-out name, last letter matching the final word's first letter. Connector words ("and," "of") do not consume a letter, consistent with standard US military acronym practice.

- **E.X.O. — Evolving Xenograft Operator.** The base suit. "Xenograft" reflects that the suit's living components may or may not be human in origin — deliberately left ambiguous, a narrative flag rather than a mechanical one. "Evolving" refers to player-driven upgrades (tier progression, add-on customization), not autonomous/involuntary change. Grafting to the player begins at Mark I (see Tier Naming, below); Mark 0 is worn but not yet grafted.
- **A.S.S.A.U.L.T. — Armored Shock Sustained Advance Unit of Lethal Temperament.** Armored (Toughness/survivability) + Shock (overwhelming force delivered on impact, regardless of wind-up speed — ties to Heavy Strike and One Punch) + Sustained (endurance under prolonged punishment) + Advance (aggressive forward pressure) + Unit (military framing) + Lethal (combat identity) + Temperament (the volatile accumulate-then-discharge nature of H.E.A.T. — calm until it isn't).
- **R.E.C.O.N. — Rapid Evasive Combatant and Ordnance Nullifier.** Rapid + Evasive describe the suit/wearer (speed, Slipstream's dodge mechanic). Combatant and Ordnance describe what the suit nullifies — enemy fighters and enemy weapons/explosives — rather than naming the wearer's own role, a deliberate grammatical departure from EXO's and Assault's "names the wearer" pattern. "Nullifier" reflects Recon's identity of avoiding/negating harm rather than tanking it.

---

### Suit Tier Naming (Mark / Type / Series)

Modeled on real-world military Mark/Mod designation conventions. Each suit uses its own tier-name word, all starting at 0:

- **EXO → Mark** (Mark 0, Mark I, Mark II, ...)
- **Assault → Type** (Type 0, Type I, Type II, ...)
- **Recon → Series** (Series 0, Series I, Series II, ...)

**Tier 0 (Mark 0 / Type 0 / Series 0)** is a "prototype" state — the suit is worn but, in EXO's case, not yet grafted. All three suits still have valid add-on slots and a valid Mod number at Tier 0.

**Grafting (EXO-specific):** Mark 0 = worn only, no tissue fusion. Mark I onward = grafting begins — a stat state and narrative flag together, not just fluff. The muscle-graft items (Red Muscle, White Muscle, Achilles Graft, Quad Graft) are the actual xenograft tissue that grafts to the player as EXO tiers up — not merely crafting inputs consumed to build the suit.

---

### H.E.A.T. — Full Name Confirmed

**H.E.A.T. = High Energy Accumulation and Transfer.**

Reflects the mechanic's two-part behavior: energy banks from outside-in/direct-contact damage taken (**Accumulation**), and the full bank discharges through One Punch (**Transfer**). Supersedes all prior undefined/placeholder uses of "H.E.A.T." throughout suit and weapon documentation — mechanic itself is unchanged, only the full name is newly confirmed.

---

### Mod Number System

**Purpose:** A secondary designator (paired with Mark/Type/Series) that reflects the *character* of a suit's current add-on loadout — not a literal parts inventory, but a specialization-purity readout: how focused vs. mixed the build is, and toward what.

**Six specialty categories, each with a unique letter:**

| Letter | Specialty |
|---|---|
| M | Mobility |
| D | Defense |
| E | Endurance |
| H | Hazard |
| U | Utility |
| C | Combat/Offense |

Cross-suit bridge add-ons (H.E.A.T. Sink, Overdrive Core) are counted under **Utility** — their main purpose is adapting access to another suit's add-ons, not a combat/mobility/defense function in their own right.

**Weighting:** Each equipped add-on contributes its own tier value to its specialty's running sum. (No separate weight table needed — tier numbers already exist for every add-on.)

**Calculating the Mod value:**
1. Sum tier values within each of the six specialties across all currently equipped add-ons.
2. Identify the dominant specialty (highest sum). Check for ties **before** rounding any percentages.
3. **Pure build** — every equipped add-on falls under one specialty: Mod displays as just that letter. *Example: EXO Mark 0 Mod U.*
4. **Mixed build, single dominant specialty** — Mod displays as that specialty's letter followed by its percentage share of the total weighted sum (dominant specialty's sum ÷ total summed tier-weight across all equipped add-ons × 100, rounded to nearest integer), no separator. *Example: Assault Type 3 Mod C17.*
5. **Two-way tie** for dominant specialty — Mod displays both letters followed by their shared percentage. *Example: Mod MC25.*
6. **Three-or-more-way tie** — Mod displays as **Mod X**.

**Open questions:** Exact tier-value-per-add-on reference table (straightforward once the full add-on roster/tier assignments are finalized). Whether Mod should be visible to the player in a tooltip/UI element, or purely a documentation/lore construct.
