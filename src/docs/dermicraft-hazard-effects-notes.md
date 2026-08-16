# Dermicraft Hazard Effects — Player-Facing Design

How the mod's hazard tag system (`hazard/*` — see `dermicraft-project-primer.md` → Hazard tag hierarchy, and [[project_hazard_profile_system]] in Claude Code memory for the code-level model) affects the **player directly**, not just machines/ducts/tanks. New companion doc — ties into `dermicraft-suit-notes.md` (suit-side stats) and `one-punch-full-breakdown.md` (Assault's H.E.A.T. interaction).

**Why this exists, separate from the machine-side hazard system:** machines/tanks/ducts use a hard accept/reject gate (`HazardProfile`) — a fluid either qualifies or the container refuses it, silently. That model doesn't fit the player: standing near/in a hazardous fluid needs to actually feel like something (an effect), not just a wall. This doc covers that feel — per-hazard gameplay effects, and how each suit's stats modify them.

---

## The two suit-wide stats (confirmed)

Every suit (Exo, Recon, Assault) carries both of the following. Same two stats mod-wide — each suit just expresses/grows them differently, per its own identity (see Per-Suit Expression, below).

- **Hazard Resistance** — governs **non-damaging** hazard effects (status-effect-style hazards, e.g. Radiation Mild's Nausea/Hunger/Slowness) — covering both the **timing** side (how long before effects kick in / how fast they taper off) and the **strength** side (how severe the effects are once active). Both are facets of this one stat; which facet actually moves is a per-suit expression choice (see Per-Suit Expression, below), not two separate stats.
- **Hazard Defense** — governs **damaging** hazard effects, uniformly across all three suits. For Assault specifically, this stat also discounts the raw hazard-damage baseline *before* it's banked as H.E.A.T. (see `one-punch-full-breakdown.md`) — mirroring how the now-folded-in "Heat Resistance" concept worked, generalized from Extreme-Heat-only to all damaging hazards.

**Exo baseline: both stats are 0.** Consistent with Exo's permanent zero-defense identity — neither stat grows through normal suit progression. Any Hazard Resistance/Defense Exo ever has would have to come from add-ons (not yet designed — deferred to a future session, see Open Questions).

---

## Per-Suit Expression (confirmed)

Same two stats, different mechanism per suit — this is deliberate, not an oversight (each suit's hazard-handling identity should extend its existing combat/mobility identity, not be a generic fourth stat pasted on):

- **Assault — Hazard Resistance expressed as strength; Hazard Defense scaled off Toughness for damage.** For non-damaging hazards, Assault's Hazard Resistance reduces effect **severity** (weaker Nausea/Hunger/Slowness stacking, by a percentage) — **exposure timing stays standard**, unlike Recon. For damaging hazards, Hazard Defense (uniform stat, see above) reduces the damage taken and also discounts the raw baseline before H.E.A.T. banking (see `one-punch-full-breakdown.md`) — the deliberate diminishing-returns lever for hazard-fueling: better Hazard Defense = safer *and* worse fuel farming, on purpose.
- **Recon — Hazard Resistance expressed as timing.** A high-Resistance Recon needs to be in/near a hazard **longer before effects start stacking**, and the stack **tapers off faster** once they leave — effect severity itself follows the default/unmodified curve, the mirror image of Assault's strength-based expression of the same stat. Fits Recon's mobility identity: it rewards moving through danger rather than tanking it. Likely tied to Recon's existing speed stat as the underlying driver (not fully locked — see Open Questions).
- **Exo — both stats 0, add-on-only.** Not yet designed which add-ons (if any) grant Hazard Resistance/Defense, or how much. Deferred.

---

## H.E.A.T. Generation Rule (confirmed, general — supersedes per-hazard-tag response table)

Which hazards feed Assault's H.E.A.T. (see `one-punch-full-breakdown.md`) is decided by **one general rule based on the mechanism of harm**, not a per-hazard-tag whitelist:

- **Outside-in / direct-contact damage generates H.E.A.T.** The harm comes from an external source actively hurting the player right now, tied to ongoing contact — Extreme Heat (fire/lava) is the confirmed example: **generates H.E.A.T. continuously, per tick, up to the capacity cap**, for as long as contact continues.
- **Inside-out / deterioration damage does NOT generate H.E.A.T.**, even when it deals real HP damage. The harm is the player's own body breaking down internally, independent of whether the source is still nearby — Radiation and Biohazard are both this category. This is a **feel-based distinction, not a strict engine rule** (vanilla Fire is technically tick-based under the hood too) — the actual test is "is something hurting you from outside right now" vs. "is something now wrong inside you regardless of the source."
- **This retroactively explains Biohazard's "no H.E.A.T." note** (see Per-Hazard Effect Design, below) — it was never a special-cased exception, it's simply another deterioration-type hazard, same bucket as Radiation. Biohazard's "fully resisted at high Assault tier" is just Hazard Resistance/Defense scaling toward zero-effect, unrelated to H.E.A.T. logic.
- **Litmus test for future hazards (including Metaphysical, not yet designed):** ask whether the hazard hurts the player from outside (→ H.E.A.T.-generating) or breaks something down inside them (→ not H.E.A.T.-generating), rather than deciding per-tag from scratch each time.

---

## Per-Hazard Effect Design

### Extreme Heat — confirmed, no new mechanic needed
Reuses vanilla lava/fire behavior directly (burning). No custom effect required — every Molten-family fluid inherits this via the family-wide hazard tag rule (see `dermicraft-crafting-notes.md` → Stage 2 Crafting Blends).

### Radiation (Mild) — confirmed direction
**Real-world grounding:** survivable exposure — nausea, fatigue, disorientation, a lingering "something's wrong with me" that doesn't kill but wears the player down over time. Not sharp pain, a creeping wrongness.

**Mechanical translation (confirmed):**
- **No HP damage** — deliberately excluded. This is a debuff hazard, not a damage hazard (and notably, this means Assault gets **no H.E.A.T.** from Radiation Mild exposure under the current H.E.A.T. rule, since H.E.A.T. only banks from damage — a deliberate consequence, not an oversight).
- **Effects:** Nausea + Hunger drain + a touch of Slowness.
- **Behavior:** lingers a short time after the player leaves the hazard (doesn't clear instantly); **stacks while exposed, tapers off** after leaving (not a flat refresh-only effect).
- **Implementation shape (performance-conscious, confirmed direction):** don't track three independently-stacking potion effects. Instead keep **one hidden numeric "exposure level" per player** that rises while in contact with the hazard and decays on a timer once they leave. The three visible effects (Nausea/Hunger-drain/Slowness) derive their amplifier/strength from that single number, recalculated periodically rather than every tick — cheap regardless of exposed-player count. This same scalar is the mechanism both suit stats hook into: Hazard Defense-driven suits dampen the *effect amplitude* derived from the scalar; Hazard Resistance-driven (Recon-style) suits dampen the scalar's *gain/decay rate* directly.

### Radiation (Severe) — confirmed direction
**Real-world grounding, deliberately NOT followed too closely (confirmed):** most real acute-radiation stories involve a single strong flash exposure (e.g. criticality accidents), not gradual days-long illness — that flash-then-delay shape is the inspiration, not literal medical realism.

**Mechanical translation (confirmed):**
- **Delayed hit, not on-contact damage.** Exposure banks a hidden "dose" (does not deal damage immediately). After a **fixed, moderate delay** — long enough that the player relaxes and stops expecting danger, not so long they forget it happened — the dose converts into a real hit.
- **Effects on trigger:** Wither + Nausea + Slowness, severity scaled to accumulated dose. Wither specifically (real damage + caps max HP while active) is what makes Severe read as a genuinely different, nastier register than Mild's zero-HP-damage debuff.
- **Stacks, no damage cap.** Multiple exposures before the first dose resolves compound the eventual hit — deliberately nasty, no ceiling. The only mercy: each additional stack still carries its own full delay, so a player who keeps re-exposing themselves gets repeated warning windows rather than one unavoidable pile-up — "the game gave you every chance" fairness, consistent with the suit-removal-damage rule elsewhere in the mod.
- **Cure: actual vanilla items, confirmed literal, not a Dermicraft-native equivalent (at least initially)** — standard Golden Apple, Enchanted Golden Apple, and possibly Milk, used as the existing vanilla curative toolkit rather than inventing a new item first. Dermicraft-native add-ons/equipment that also cure it are possible later, not required.
- **No H.E.A.T. generated**, per the general H.E.A.T. Generation Rule above (deterioration/inside-out, despite dealing real damage via Wither) — this is Radiation Severe's specific case of that rule, not a standalone exception.
- **Suit-stat interaction:** Assault's Hazard Resistance reduces the eventual effect severity by a percentage (Wither/Nausea/Slowness amplifier scaled down); Assault's Hazard Defense specifically reduces Wither's damage — implemented the same way Radiation Mild's amplifier-selection pattern works (Wither's per-tick damage is driven by its amplifier level in vanilla, so "less damage" = picking a lower amplifier at trigger time based on Hazard Defense, no custom damage-interception needed). Recon's Hazard Resistance (exposure-time expression) is a natural fit for extending the pre-trigger delay, giving a mobile player more time to reach a cure — not yet locked as a confirmed interaction, but flagged as the obvious payoff of the shared-stat architecture.

### Biohazard — confirmed direction
**Real-world grounding:** infection/contamination — something living or reproducing inside you that isn't you. The unsettling part isn't the damage, it's that it *changes* you and runs its own course once it's taken hold, not just a scary flourish on top of damage. Thematically the best fit for the mod's body-horror identity of any hazard so far.

**Mechanical translation (confirmed):**
- **Progressive angle, chosen deliberately** over the other two candidate framings considered (contagion/spread, transformation) — spread specifically **means nothing in singleplayer**, but is flagged as a possible addition anyway (not designed).
- **Trigger — a rolling percentage chance, not a deterministic threshold.** The longer the exposure, the higher the chance of actually catching it per interval — distinct from both Radiation tiers, which are deterministic (guaranteed effect scaled by exposure amount, not a coin flip). A brief exposure might roll clean.
- **Once sick, you're sick.** Further exposure while already infected doesn't matter — no re-triggering, no re-worsening from more contact. Simplifies the state to "infected or not," no exposure-tracking needed once triggered.
- **Progression — rise, peak, fall (a fever arc, not a straight escalation or a single delayed hit).** Genuinely dangerous at the peak, not necessarily fatal — **base vanilla Poison cannot kill on its own** (stops at very low health), which is the built-in mechanism behind "potentially deadly but not necessarily": a patient, careful player survives on time alone.
  - **Rising:** Weakness + Hunger drain + mild Poison.
  - **Peak:** Weakness + Poison + a brief window of **Wither** (the one effect in this phase that actually *can* kill) + **Mining Fatigue** (can't fight, can't work, the incapacitation moment). Uses the vanilla particles Wither/Poison already render — no custom VFX needed for now.
  - **Falling:** Weakness + faint Poison, tapering to nothing.
- **Recovery — active self-care shortens the arc; passive waiting is the fallback, not the only path.** Staying fed, sleeping, avoiding damage, and not over-exerting (e.g. prolonged sprinting) all speed recovery. Fully passive/idle recovery still works ("careful and patient" gets you through it), but costs real wasted in-game time — the cost is paid in time, not materials, unless a cure is used.
- **Cure — a tiered mix (confirmed):** vanilla Soups (Mushroom Stew, etc.) as a folk-remedy/home-remedy tier — softens the arc (weaker peak, faster taper) rather than an outright cure — alongside the possibility of a proper Dermicraft-native antidote item that cuts the illness short entirely. Both tiers intended, not an either/or.
- **No H.E.A.T. generated** (deterioration/inside-out, per the general H.E.A.T. Generation Rule above), and **fully resisted at high Assault tier** rather than merely dampened — the one hazard confirmed to reach zero-effect via Hazard Resistance/Defense rather than just reduced-effect.

### Metaphysical — no real-world referent, leaning fully into supernatural/cosmic horror (confirmed tone)
**Split confirmed:** **Mild messes with the character's head** (illusory — nothing that happens is actually real); **Severe is "more real"** (genuine consequence, not just a scare — not yet designed, see below). This mirrors the debuff/damage split every other hazard's Mild/Severe pair already has, just translated into "is this even really happening to you."

#### Metaphysical (Mild) — confirmed direction
- **No HP damage, no real mechanical harm** — everything the player perceives during this hazard is **a lie**: no real mob, no real damage, no real direction change. This is the one hazard category so far that isn't honest about what it's doing to the player, which is the whole point.
- **Baseline effect:** vanilla **Darkness** (unused by any other hazard so far — deliberately not reusing Nausea a third time, since Darkness reads as "something's blotting out your senses" rather than physical disorientation).
- **Hallucination Table (confirmed architecture — this hazard needs its own system, not just a fixed effect).** A **weighted random pool of flavor-events**, sampled per trigger, so no two exposures look the same. Two-layer design: the baseline (Darkness) applies consistently every time; which specific unsettling thing happens on top is randomly rolled from the table. Confirmed pool entries so far:
  - Fake distant mob silhouettes that vanish on approach.
  - Sound cues from the wrong direction / ambient noise that isn't coming from anything real.
  - Rare whisper/ambient audio sting tied to effect duration.
  - Compass spinning erratically (client-side only, self-corrects when the effect ends).
  - **Doppelganger — a fake copy of the player (or, technical constraint: a player they've actually encountered before, since rendering a phantom needs cached skin data — doesn't apply in singleplayer, which just never rolls this variant).** Behavior is itself randomized per instance across three confirmed patterns: **idle-and-staring** (motionless, faces the real player, vanishes on approach), **delayed mimicry** (copies the player's recent actions a beat behind), or **malfunctioning** (does something wrong — attacks nothing, walks into a wall, stands over empty space).
  - **The Scientist (flagged, deferred — needs its own design pass, more work than the rest of the table).** A hallucination of the mad scientist who wrote the Notebook (see `dermicraft-misc-notes.md` → In-game Guide System, "the unsettled scientist"; full motivation/cosmic-horror framing in `dermicraft-scientist-lore-notes.md`). Deliberately **NOT** meant to reuse the doppelganger's idle/mimic/malfunctioning behaviors — needs bespoke, more deliberate behavior specifically so it reads differently from the rest of the table. **Narrative purpose (confirmed):** this is the concrete mechanism for gradually hinting that the player may actually be encountering the real scientist, well before the confirmed final-boss reveal — resolves the open narrative question already on record in `dermicraft-misc-notes.md` about whether early hints of wrongness should surface gradually or stay hidden until the reveal. **Absorbs the former standalone "Herobrine" entry** — the classic Steve-with-glowing-white-eyes-in-a-labcoat look is now one of the Scientist's own forms, not a separate generic entry, per `dermicraft-scientist-lore-notes.md`'s nonlinear-timeline framing (each form is a different, non-chronological point along a self that no longer holds together as one continuous person). That form keeps its prior confirmed trait — **persists for the player's full exposure duration** once rolled, rather than a brief flicker, unlike the rest of the table. Not designed further yet (how often overall, what other forms exist, what it does/says, whether behavior shifts as the story progresses — all open).
- **Table is a living list — confirmed to grow later**, not something that needs to be complete now.

#### Metaphysical (Severe) — confirmed direction
**"More real" than Mild (confirmed framing):** whatever was illusory in Mild stops being fake. Rolls between two confirmed outcomes per exposure instance:

- **Hallucination-made-real.** Rolls from (an expansion of) Mild's Hallucination Table, but the outcome now has genuine physical consequence instead of being a lie: a doppelganger's "malfunctioning" attack actually connects and deals damage; a fake mob silhouette isn't fake this time, a real hostile mob is there; Herobrine doesn't just linger, he actually breaks/places a block. Deliberately weaponizes the trust Mild trains into the player ("none of this is real, just wait it out") — same table, same flavor, escalated stakes. Exact expanded-table entries and how "real" each one becomes not yet itemized.
- **Telegraphed instant dimensional round-trip.** Unlike every other Severe hazard so far (all delayed/accumulating), this one triggers **immediately** on exposure — but with a **visual warning** the instant it starts, giving the player a real (if brief) window to react before the roll resolves. If it resolves: short-range displacement to **any registered dimension** (Nether, End, another mod's dimension, or even just another spot in the Overworld — not always the "exciting" outcome, deliberately unpredictable). **Guaranteed non-clipping landing** (never spawns the player inside solid terrain — no suffocation-by-placement), but **not guaranteed non-dangerous otherwise** (real fall damage, proximity to lava/void/mobs at the destination is still fair game) — the line is "never an unfun, zero-counterplay death," not "never risky." **Auto-returns the player near their origin point once the exposure effect's duration ends** — a round-trip, not one way, specifically to avoid a bad-luck loop (teleported somewhere dangerous, then dumped somewhere else dangerous, repeat). Reframes "be quick" as **capitalizing on a timed opportunity** (loot, scouting, placing a waystone-equivalent marker) rather than escaping — and since the teleport is just "move the player to a valid spot in another dimension," this creates emergent value with any mod that lets a player mark/return to a location (Waystones, etc.) for free, no Dermicraft-side compatibility code needed.

#### Metaphysical vs. machines — the Mind Rule (confirmed, mod-wide)

**Core rule: Metaphysical hazard only affects things with minds.** Grounded in the genre convention (cosmic horror attacks the mind first — the body later or never; things without minds are inert to it, the way the cultist goes mad while the stone idol just sits there), and mapped onto a boundary the mod *already formally defines*: the **Smart vs. Dumb machine rule** ("smart" = the Brain, or a Brain-derived ingredient, is required in the recipe).

- **Dumb machines are natively immune.** Ordinary machine families (Masticator, Mutator, Metastasizer, Effluentcer, etc. — permanently Brain-free by rule) can contain and process Metaphysical-tagged fluids **with no special protection and at no tier requirement for the Metaphysical tag itself.** Any *other* hazard tags on the fluid still gate normally (e.g. Ender Essence's Extreme Heat still requires a Tier 2 machine — the Metaphysical Severe tag just passes through the mindless machine like wind through an empty house).
- **Ducts/Nodes are mindless connective tissue** — Metaphysical tags are **exempt from the duct hazard-tier weakest-link filter** (their physical hazard tags still filter normally). Needs the explicit carve-out in the `HazardProfile` check.
- **Smart structures ARE affected** — the FL (Brain/Core), Gear Worx Stations, and the Imago Engine all have minds and need protection from Metaphysical exposure.
- **The player is a mind** — nothing changes player-side: hand-in-a-bucket classification, Drinker tier gating, and the two suit Hazard stats all still apply to Metaphysical fluids exactly as before.

**Design consequence — the hazard hierarchy inverts:** every other hazard says "better machine = safer." Metaphysical says **intelligence is the vulnerability** — the dumbest grinder handles Ender Essence without noticing; the factory's crowning brain needs shielding from it. This is the genre's thesis expressed as a mechanic, and it's what un-blocked the Eye of Ender recipe (see machine notes → Mutator).

**Exposure definition (confirmed): containment, not proximity.** A smart structure is exposed when a Metaphysical-tagged fluid is **contained within the structure or its network** — e.g. a tank attached to the FL's floor network holding Ender Essence exposes the FL. Cheap to compute (the FL already knows its network contents via Knitting/direct access — no proximity scanning), and fair (the player chose to pipe it in).

**Effect on an exposed, unprotected smart structure (confirmed direction): the GUI glitches out until the hazard is removed.** Madness, not damage — non-destructive, self-resolving on removal of the fluid, instantly legible as "something is wrong with this machine's *mind*," and still a real gate (an unreadable GUI can't be operated). Severity ladder falls out of the existing tags for free: **Mild = visual noise/flicker but usable; Severe = GUI fully illegible until purged.**

**Open questions:** What "protection" for a smart structure actually is (a shielding block? an upgrade? — the relocated remainder of the old "what handles Metaphysical" question). Exact GUI-glitch presentation per severity. Whether exposure has any effect beyond the GUI (e.g. does a Severe-exposed FL also misbehave — misroute crafts, Knit wrong — or is the GUI the whole effect for now?).

---

## Open Questions

- Metaphysical Severe — the split/weighting between its two outcomes (hallucination-made-real vs. teleport) not set; exactly which Hallucination Table entries can become real and how; exact "short time" duration before auto-return not numerically set.
- Metaphysical Mild's Hallucination Table — confirmed to keep growing (living list); The Scientist entry specifically needs its own full design pass (behavior, frequency, whether it evolves as the story progresses, what forms besides labcoat-Herobrine exist); its roll-weight/rarity not numerically set.
- Biohazard: exact infection-chance-per-exposure-second curve, exact rise/peak/fall durations, Dermicraft-native antidote item (not designed — recipe, ingredients, name all open), and whether the spread/contagion angle ever gets added on top of the progressive core.
- Whether Recon's Hazard Resistance (exposure-time) extending Radiation Severe's pre-trigger delay is a confirmed interaction or just a flagged natural fit — not locked.
- Exo's hazard add-ons — which ones exist, what they grant, not designed.
- Whether Recon's exposure-time expression is literally driven by its speed stat or a separate formula.
- Exact Hazard Resistance/Defense growth curves (how much per suit tier) for Assault and Recon — not numerically set, same deferred-to-tier/equipment-discussion status as the rest of the suits' numbers.
- Radiation Severe's exact delay duration, stacking-delay length, and dose→severity curve — not numerically set.
- Naming: "Hazard Defense"/"Hazard Resistance" vs. the unrelated `HazardProfile`/hazard-tag system used by machines risks some reader confusion since both use the word "hazard" for different mechanisms (player mitigation stats vs. machine accept/reject gate) — flagged, not renamed.
