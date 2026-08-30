# Dermicraft Suit Notes

Running log of decided design choices for the three wearable suit lines — Exo-skeleton, Assault, and Recon — and the shared systems (modules, tiers, points, removal mechanics) that govern them. New companion doc; ties closely into `dermicraft-gadget-notes.md` since suits use the same customization philosophy as Gadgets.

---

## Overview

Three suits exist, all originating from the same FL-born base:

- **Exo-skeleton** — the base suit. No defense, ever (aside from a possible late-game module exception). Free utility layer with no fuel requirement and no drawback when unfueled.
- **Assault** — heavy combat specialization. Forked from the Exo-skeleton at the first upgrade choice.
- **Recon** — light, high-mobility specialization. Forked from the Exo-skeleton at the same first upgrade choice.

All three suits occupy the full four vanilla armor slots as a single locked-in unit — pieces cannot be mixed and matched between suits or with vanilla armor.

**Fork point:** The Assault/Recon split happens at the player's first upgrade choice (light vs. heavy), not at initial construction. From that point forward, each suit is easier to discuss independently.

**Construction:** The base Exo-skeleton is FL-born, following the FL's established role as the standard construction path for Gadget-tier equipment. **No longer FL-exclusive** — the Dock (a Gear Worx Station) can now also build the Tier 0 suit itself, using "build" rather than "craft" deliberately (living power armor, not a mechanical assembly) — see `dermicraft-gear-worx-notes.md` → Dock, Duty 7. The FL retains the same capability; nothing was removed from it.

---

## Suit Designations & Naming

All three suits follow the Gadget naming convention — acronym + full written-out name, last letter matching the final word's first letter. Connector words ("and," "of") do not consume a letter, consistent with standard US military acronym practice.

- **E.X.O. — Evolving Xenograft Operator.** The base suit. "Xenograft" reflects that the suit's living components may or may not be human in origin — deliberately left ambiguous, a narrative flag rather than a mechanical one. "Evolving" refers to player-driven upgrades (tier progression, module customization), not autonomous/involuntary change.
- **A.S.S.A.U.L.T. — Armored Shock Sustained Advance Unit of Lethal Temperament.** Armored (Toughness/survivability) + Shock (overwhelming force delivered on impact, regardless of wind-up speed — ties to Heavy Strike and One Punch) + Sustained (endurance under prolonged punishment) + Advance (aggressive forward pressure) + Unit (military framing) + Lethal (combat identity) + Temperament (the volatile accumulate-then-discharge nature of H.E.A.T. — calm until it isn't).
- **R.E.C.O.N. — Rapid Evasive Combatant and Ordnance Nullifier.** Rapid + Evasive describe the suit/wearer (speed, Slipstream's dodge mechanic). Combatant and Ordnance describe what the suit nullifies — enemy fighters and enemy weapons/explosives — rather than naming the wearer's own role, a deliberate grammatical departure from EXO's and Assault's "names the wearer" pattern. "Nullifier" reflects Recon's identity of avoiding/negating harm rather than tanking it.

### Suit Tier Naming (Mark / Type / Series)

Modeled on real-world military Mark/Mod designation conventions. Each suit uses its own tier-name word, all starting at 0:

- **EXO → Mark** (Mark 0, Mark I, Mark II, ...)
- **Assault → Type** (Type 0, Type I, Type II, ...)
- **Recon → Series** (Series 0, Series I, Series II, ...)

**Tier 0** (Mark 0 / Type 0 / Series 0) is a "prototype" state — the suit is worn but, in EXO's case, not yet grafted. All three suits still have valid module slots and a valid Mod number (see below) at Tier 0.

**Grafting (EXO-specific):** Mark 0 = worn only, no tissue fusion. Mark I onward = grafting begins — a stat state and narrative flag together, not just fluff. The muscle-graft items (Red Muscle, White Muscle, Achilles Graft, Quad Graft) are the actual xenograft tissue that grafts to the player as EXO tiers up — not merely crafting inputs consumed to build the suit.

### H.E.A.T. — Full Name Confirmed

**H.E.A.T. = High Energy Accumulation and Transfer.** Reflects the mechanic's two-part behavior: energy banks from outside-in/direct-contact damage taken (**Accumulation**), and the full bank discharges through One Punch (**Transfer**). The mechanic itself is unchanged from its description throughout this doc and `one-punch-full-breakdown.md` — only the full name is newly confirmed.

### Mod Number System

**Purpose:** A secondary designator (paired with Mark/Type/Series) reflecting the *character* of a suit's current module loadout — not a literal parts inventory, but a specialization-purity readout: how focused vs. mixed the build is, and toward what.

**Six specialty categories, each with a unique letter:**

| Letter | Specialty |
|---|---|
| M | Mobility |
| D | Defense |
| E | Endurance |
| H | Hazard |
| U | Utility |
| C | Combat/Offense |

Cross-suit bridge modules (H.E.A.T. Sink, Overdrive Core) are counted under **Utility** — their main purpose is adapting access to another suit's modules, not a combat/mobility/defense function in their own right.

**Weighting:** Each equipped module contributes its own tier value to its specialty's running sum — no separate weight table needed, since tier numbers already exist for every module.

**Calculating the Mod value:**
1. Sum tier values within each of the six specialties across all currently equipped modules.
2. Identify the dominant specialty (highest sum). Check for ties **before** rounding any percentages.
3. **Pure build** — every equipped module falls under one specialty: Mod displays as just that letter. *Example: EXO Mark 0 Mod U.*
4. **Mixed build, single dominant specialty** — Mod displays as that specialty's letter followed by its percentage share of the total weighted sum (dominant specialty's sum ÷ total summed tier-weight across all equipped modules × 100, rounded to nearest integer), no separator. *Example: Assault Type 3 Mod C17.*
5. **Two-way tie** for dominant specialty — Mod displays both letters followed by their shared percentage. *Example: Mod MC25.*
6. **Three-or-more-way tie** — Mod displays as **Mod X**.

**Open questions:** Exact tier-value-per-module reference table (straightforward once the full module roster/tier assignments are finalized). Whether Mod should be visible to the player in a tooltip/UI element, or purely a documentation/lore construct.

---

## The Exo-Skeleton (Base Suit)

**Defense:** None. Permanent trait — the Exo-skeleton never gains defense through normal progression. A late-game module exception is a possibility, not a commitment.

**Hazard handling (confirmed) — Hazard Resistance and Hazard Defense both sit at 0, module-only.** Every suit carries these two stats (see `dermicraft-hazard-effects-notes.md` for the full mechanic); consistent with having no baseline defense stat at all, Exo has **neither by default** — hazard mitigation is meant to come entirely from **equippable modules** the player chooses for the job at hand (Lava Waders is the existing example — see Universal Modules, Legs), not designed further yet. This fits Exo's "versatility, freedom over ceiling" identity: hazard mitigation is a *choice you slot in*, not a trait the suit has.

**Passive stats (unfueled, no drawback):** Modest boosts to movement speed, jump height, fall distance before damage, reduced fall damage, increased swing and mining speed, and reduced hunger use. The Exo-skeleton functions fully and safely with no fuel at all — fuel is optional here, not required.

**Fuel's role:** Fuel does not power the base suit's passive stats directly at Crude grade — it exists to power **modules**. From Concentrated grade upward, better fuel additionally boosts the suit's own passive stats (movement/jump/fall/swing-mining speed/hunger reduction), scaling with grade. **Crude Slurry provides no stat boost to the suit itself** — it only powers modules.

**Fuel sourcing:** Draws first from its own internal tank, then from any other fluid-handling item in the player's inventory (**except Syringe**, which does not count as a standard fuel handler), and finally falls back to draining the player's hunger bar once all fuel sources are exhausted. This fallback order is the **standard rule for all fueled equipment** in the mod unless otherwise stated.

**Slot layout (Tier 0):**
| Slot | Count |
|---|---|
| Head | 1 |
| Chest | 1 |
| Arm (each) | 1 (2 total) |
| Leg (each) | 1 (2 total) |

**Total: 6 slots**, evenly distributed — one per body part.

**Material upgrade branch:** Improving the Exo-skeleton's base materials (vanilla-inspired tier scale, exact numbers not yet set) improves its core passive capabilities. This is a separate track from module upgrades; most module upgrades scale via living fluids (Slurries/Serums), while material upgrades govern the suit's foundational stats.

**Exclusive module:**
- **Landing Brace Module** — Exo's first genuine exclusive module, resolved from the former universal Landing Brace (see history below). **No upgrades — deliberately flat**, unlike almost everything else in the mod.
  - **Equipment origin: Landing Brace** — real, standalone player equipment usable by *anyone*, any suit or none. Spends fuel to soften a hard landing, **capable of negating fall damage entirely**. Only **docking it into a suit's Module Frame is Exo-exclusive** — Assault/Recon players can still carry and use the standalone item, they just can't slot it into their own suit's Frame.
  - **Risk on extreme falls:** for a fall great enough, Landing Brace can **drain every drop of fuel the player is carrying and still not fully prevent damage** — genuine stakes that scale naturally with fall height, without needing a tier system to create tension.
  - **Docked behavior is identical to standalone** — no scope expansion the way some other docked equipment gets (e.g. Auto-Healer); it does exactly the same thing worn loose or slotted into Exo's suit.
  - **History:** formerly the universal "Landing Brace" module (standard Leg placement all three suits, Assault-exclusive Arm placement) — retired from that universal role once Hero Landing, Hulk Landing, and Hell Dive made it redundant everywhere except Exo, which had no suit-specific fall option of its own. Rather than being discarded, it's **the primary ingredient for both Hero Landing's and Hulk Landing's (still equipment-less) mechanics** — a basic fall-cushioning device refined differently by Recon's and Assault's engineers into their own suit-signature techniques.

---

## Universal Modules

Available across all three suits (behavior may vary slightly by suit — noted where applicable). Chest doubles as the slot representing the torso *and* arms, since vanilla has no dedicated arm slot — however, Dermicraft's custom upgrade system is not bound by vanilla slot limits, so **Arm-specific modules exist as their own functional category** even though only three physical pieces exist.

### Head
- **Bio Vision Goggles Module** *(renamed to match naming convention: [equipment] + Module)* — outlines nearby entities. **Equipment origin: Bio Vision Goggles**, separate from Night Vision's.
- **Night Vision Goggles Module** *(renamed, same convention)* — standard night vision effect. **Equipment origin: Night Vision Goggles**, separate from Bio Vision's.
- **Ore Vision Goggles Module** *(new)* — reveals ore through terrain, same vision-family pattern as Bio Vision/Night Vision. **Equipment origin: Ore Vision Goggles**, its own separate item.
- **Fluid Vision Goggles Module** *(new)* — reveals fluid, same vision-family pattern as Bio Vision/Night Vision. **Equipment origin: Fluid Vision Goggles**, its own separate item.
- **Goggles, equipment-origin rule (all four):** standalone use occupies the **vanilla head/helmet slot** — deliberately, following the established genre convention for vision-enhancement gear across other mods (trading real helmet defense for the vision effect, an expected and legible tradeoff). This differs from Grapple Anchor's inventory-carry model, which is fine — different equipment types can use different origin mechanics. When integrated into a suit instead, each occupies the suit's own **Head module slot** — a separate resource from vanilla armor, no conflict there.
- **Goggle Frame (Stage 3/4) — replaces the earlier "combined goggles" idea.** A late-game modular system, not a single fixed fusion item: the **Goggle Frame** is a head-slot item that holds swappable **vision modules**, starting with **one slot** and upgradeable (via "upgrade the base item, then re-slot," same rule as the Rack) up to **four slots** — eventually holding all four vision effects active at once. **The four existing goggles (Bio/Night/Ore/Fluid Vision) double as the modules** — same standalone-or-inserted duality the Grapple already has (handheld/docked); no new item category invented, just a second way to use what already exists. Worn in the vanilla head slot, same tradeoff as the base goggles.

### Chest
- **Fuel Bladder Module** *(renamed from "Fuel Tank"/"Fuel Bladder" to match naming convention)* — the player modifies a base Bladder into this variant, which holds fuel (Crude Slurry confirmed, other grades likely). **Priority draw order:** among the pool of "other fluid-handling items in the player's inventory" the standard fuel-sourcing fallback checks (after the suit's own internal tank), the Fuel Bladder is **drawn from first**, ahead of any other fluid-handling item the player happens to be carrying — a designated overflow tank rather than one random option among many. **Refill sources:** tops itself off either **when docked** (into the universal Module Frame, slotted into a suit) **or via existing fluid-filling equipment that already targets fluid handlers generally** — e.g. the **Drinker** gadget's Transfer mode already fills "a fluid-handling item in the player's inventory if one is available," so the Fuel Bladder is simply a valid target for that existing mechanic, no new fill behavior needed.
- **Feeder Bladder — no longer a module (confirmed 2026-08-29).** Stays a real, standalone item exactly as already implemented (`BladderItem`/`FEEDER_BLADDER`, hand-drunk via its own `edible_fluid` data map — see `dermicraft-liquid-foods-notes.md`), but is dropped from the Equipment-origin dual-mode pattern entirely: it no longer docks into a suit's Module Frame, and the "may carry additional effects depending on which fluid is fed" idea that used to live here goes with it. Its old suit-side job (passive Chest-slot hunger feeding) is superseded by the new **Feeder Module** below.
- **Squid o' War Module** *(renamed from "Flight" to match naming convention)* — full flight capability. Can also be mounted in Legs (same effect, alternate slot). **Equipment origin: Squid o' War** — a living, symbiotic organ combining two real biological mechanisms: a **gas-filled buoyant sac** (inspired by the Portuguese Man o' War's gas float) for lift, and **jet propulsion** (inspired by squid/cephalopods forcefully expelling fluid) for directional thrust — deliberately not a "jetpack" reskin, a genuinely different underlying mechanism. **Worn in the vanilla Chest/chestplate slot** (not inventory-carry) — given how powerful full flight is, this is the most significant armor-slot tradeoff of any equipment origin so far (Chest being arguably the highest-value defense slot), appropriately scaled to the ability's outsized power.
  - **Fuel model:** fuel is only consumed for **active movement** — hovering in place is **free**, but causes a **slow altitude decay** (can't sustain hover forever for free, without an outright fuel drain either). Creates a real choice between drifting/planning for free vs. spending fuel to actually travel.
  - **Sprint + jump** triggers an accelerated climb — an active use of the jet-propulsion half specifically for fast vertical ascent, not just "hold up to go up."
  - **Upgrades, two axes:** movement speed, and reduced hover-drop rate.
  - **Max tier reward:** hover-drop is fully eliminated at max investment — **perfect free hover**, a genuine "earned it" payoff rather than something available from the start, consistent with the mod's broader philosophy of real hassle building toward real reward.
- **Rack Module** *(renamed from "Air Tank" to match naming convention)* — underwater breathing. **Equipment origin: a Rack loaded with Glass Flasks**, not a bare single Flask — the Rack is what docks into the universal Module Frame and slots into a suit, aggregating multiple Flasks' air-restoring capacity into one item used like a single higher-capacity Flask. Reuses Glass Flask's existing air-source mechanic (`dermicraft-flask-notes.md`) rather than inventing a new one. **The Rack itself is tiered** — higher tiers hold more Flask slots — following the general "upgrade the base item, then re-slot" rule: the player upgrades to a bigger-slotted Rack and re-docks it, rather than the suit having its own internal upgrade menu. (Calcium Glass is *not* a tier distinction here — it's a tag-equivalent alternate material to regular glass, same as Silica/Calcium Blend both routing to the same Beaker/Flask items at the same cost.) A multi-Flask Rack likely has general use as flask-carrying storage beyond Air Tank specifically, fitting the mod's "every item has more than one use" convention.
- **Feeder Craw — no longer a module (confirmed 2026-08-29).** The Craw-derived standalone item (portable, item-based bulk food storage) stays real, usable equipment — likely renamed (TBD, not yet picked) since "Feeder Craw" was named for its module role — but it no longer docks into a suit's Module Frame. Its old suit-side job is superseded by the new **Feeder Module** below.
- **Feeder Module (new, confirmed 2026-08-29 — replaces both Feeder Bladder and Feeder Craw as modules).** Passively manages player hunger by pulling directly from the player's **general inventory**, not from one dedicated Bladder/Craw item docked in the slot — a real mechanism change, not just a rename/merge of the two old entries.
  - **Primary source — food items anywhere in inventory.** Item-based, generalizing Feeder Craw's old mechanic off a single dedicated container to the whole inventory. Stays outside the Fluid-to-Hunger Conversion rate, same as Feeder Craw's rule did — item-based feeding was never part of that formula.
  - **Secondary source — any carried Feeder Bladder(s).** The module can drink from one exactly like the handheld item already does on its own, reusing its existing `edible_fluid` data map (mbPerDrink/hunger/saturation) passively rather than routing it through a separate suit-side cost model — no new mechanic needed here, just triggering the Bladder's own existing drink logic automatically. This resolves `dermicraft-liquid-foods-notes.md`'s own open question about whether a suit-integrated Feeder Bladder needs its own cost model: it doesn't, because there is no suit-integrated Feeder Bladder anymore — Feeder Module just calls the same item-side mechanic.
  - **Possibly configurable (open, not decided):** letting the player restrict which inventory items/sources the module may draw from (a whitelist/filter, an on/off toggle, or something else) — raised but not designed; needs its own pass before this is buildable.
  - **Equipment-origin rule tension (open, flagged):** every module is supposed to originate from a real standalone piece of equipment (see the Equipment-origin rule below) — Feeder Module has no obvious handheld form of its own, since its whole point is pulling from general inventory rather than a dedicated container. Either it needs its own standalone equipment concept invented, or it joins the short list of confirmed Equipment-origin-rule exemptions (Hulk Landing, Juggernaut, Hero Landing, Hell Dive, Reactive Defense Module, Slipstream Module) — not decided either way yet.
  - **Slot:** carried over from Feeder Craw's old Chest/Arm dual-mount as a starting assumption, not re-confirmed as part of this change.
- **Auto Shield Module** *(renamed to match naming convention)* — passively auto-blocks incoming hits exactly like vanilla shield-blocking, but automated. **Equipment origin: a modified Shield** — the player modifies a vanilla Shield into this dedicated equipment (same "modify a base item" pattern as the Bladders and Feeder Craw), so there's no more separate loose Shield tracked in inventory. **Cost model changed:** no longer consumes the item's own durability as it blocks — instead costs **fuel per hit blocked**. Can also be mounted in an Arm slot.
- **Auto Heal Module** *(renamed to match naming convention; merged from the former Self-Healing + Player Regen + Safe Removal — one module now covers all three)* — draws fuel to do all three jobs at once: heals the suit's own durability, heals the player's HP directly, and reduces/prevents the damage cost of unassisted suit removal (or reduces assisted-removal time at the Dock). **All three suits, no exclusion** — the old Assault/Recon-only restriction on suit-durability healing is lifted: it existed to compensate for Exo's permanent lack of defense, and that gap is now covered by Exo's newer defining feature (more slots + cross-suit module access via H.E.A.T. Sink/Overdrive Core), so the restriction is no longer needed.
  - **Equipment origin: Auto-Healer.** Standalone form auto-heals the player's HP via fuel whenever damage occurs — a genuine, independently useful item on its own (satisfying the Equipment-origin rule honestly). Once **docked into a suit**, its scope naturally **expands** to also cover suit durability and removal-safety, since both only exist in a suit context.
  - **Consolidation, no extra balancing needed:** this merges three previously separate Chest-slot modules into one slot. Fine as-is — running all three healing jobs off a **single shared fuel budget** is itself the natural balancing mechanic (no need for a steeper cost or weaker per-effect potency on top of that).

### Arms
- **Red Muscle Module** *(renamed from "Swing Speed" to match naming convention)* — swing speed upgrade. **Equipment origin: Red Muscle** — a modified Dense Muscle graft tuned for rapid contraction (anime-inspired naming, paired with Swing Strength's White Muscle below — both derived from the same base organic material, differently tuned).
- **White Muscle Module** *(renamed from "Swing Strength" to match naming convention; new, paired with Red Muscle Module)* — swing strength upgrade. **Equipment origin: White Muscle** — the power-oriented counterpart graft to Red Muscle, same base material (Dense Muscle), tuned for force over speed.
- **Glider Module** *(renamed from "Glide" to match naming convention)* — a third flight-family option. Must be held in a hand to work (distinct from Squid o' War Module's Chest-slot requirement). Early on, cannot be combined with the other Flight modules (Chest/Legs).
- **Spider Gloves Module** *(renamed from "Climbing (Ladder/Vine style)" to match naming convention)* — behaves exactly like climbing a vanilla ladder or vine: slow, steady, no failure state. **Equipment origin: Spider Gloves** — spiked/gripping gauntlets, **inspired by spider biology lore-wise** (not literally spider-derived tissue, keeping it consistent with Red/White Muscle's "inspired by" rather than "sourced from" approach) — explains the generic, works-on-almost-any-surface grip (mirroring how a spider adheres broadly rather than needing selective placement, unlike real pitons). **String is the major crafting component**, tying the lore together using an item already vanilla-associated with spiders.
- **Auto Shield Module** — see Chest entry; same module, alternate mount point.
- *(Landing Brace formerly listed here — resolved and moved to Exo's own Exclusive module section; see "The Exo-Skeleton (Base Suit)" above.)*

### Legs
- **Achilles Graft Module** *(renamed from "Movement Speed" to match naming convention)* — movement speed upgrade. **Equipment origin: Achilles Graft** — a modified Dense Muscle/tendon graft, fast-twitch analog paralleling Red Muscle, grounded in real leg anatomy (the Achilles tendon enabling explosive running push-off) rather than reusing the color-coded Arms naming. Same inventory-carry standalone model as the rest of the equipment-origin project.
- **Quad Graft Module** *(renamed from "Jump/Landing" to match naming convention; split from the former combined "Movement/Jump/Landing," same split pattern as Red/White Muscle)* — jump/landing upgrade. **Equipment origin: Quad Graft** — the power analog to Achilles Graft, grounded in the quadriceps' role in jumping and landing/impact absorption.
- **Exo ingredient use (all four muscle grafts):** Red Muscle, White Muscle, Achilles Graft, and Quad Graft **all double as ingredient items for Exo** (its own construction/tier-upgrade recipes), not just standalone equipment/modules — fits the mod's "every item needs more than one use" convention. Exact recipe details deferred, consistent with Exo's own construction recipe not yet being fully specified.
- **Squid o' War Module** — see Chest entry; same module, alternate mount point.
- **Gecko Cleats Module** *(renamed from "Climbing (Wall-run style)" to match naming convention)* — faster than Ladder/Vine climbing in both vertical and horizontal movement, has an active "running/standing" feel, but **starts slipping** after a time — a burst-mobility tool rather than a sustainable climb. Early on, exclusive against the Arms climbing type. **Equipment origin: Gecko Cleats** — worn leg/footwear, inspired by gecko van der Waals-force adhesion (distinct bio-inspiration from Spider Gloves, keeping the two Climbing types visually/thematically separate). Gecko adhesion requiring a specific peel-and-reattach motion to sustain maps naturally onto "starts slipping after a time" — the mechanism inherently can't hold indefinitely, not an arbitrary timer. Same inventory-carry standalone model as the rest of the equipment-origin project.
- *(Landing Brace formerly listed here — resolved and moved to Exo's own Exclusive module section; see "The Exo-Skeleton (Base Suit)" above.)*
- **Grapple Anchor Module** *(renamed from "Grappling Brace"/"Grapple Anchor" to match naming convention)* — universal (all three suits). A Grapple-augment module (see the Grapple entry in `dermicraft-gadget-notes.md`), now with **its own standalone equipment origin** (resolving the earlier ambiguity of riding entirely on the Grapple's own equipment). **Design principle:** the module grants the **suit** the capability to use the Grapple this way — it isn't the Grapple item itself being empowered — so it works identically whether the Grapple is docked (Grapple Module) or simply carried on the hotbar, as long as the suit + this module are worn.
  - **Equipment origin — inventory-carry, not a vanilla armor slot.** Works as long as the physical Grapple Anchor item is **carried anywhere in the player's inventory** — same model as Auto Shield ("requires the player to be carrying an actual Shield item in inventory," no special slot consumed). **Explicitly not worn in the vanilla leg-armor slot** — that was considered and rejected: occupying that slot would cost the player their entire leg armor defense when not using a suit, a disproportionate tax compared to every other Gadget in the mod (all hotbar/handheld, none compete with armor). The "leg-mounted brace" framing survives purely as **cosmetic/lore flavor**, not a mechanical slot requirement.
  - **Slot:** alternate mount points, **Leg or Arm, player's choice**, open to all three suits (same pattern as Flight's Chest/Legs or Auto Shield's Chest/Arm). **Lore:** crouch-activation represents anchoring/grounding the character during the hold, which is also why Leg is a natural home for it.
  - **Effect 1 — hold a mob:** crouch **before firing** at a mob to **brace and hold it in place** (immobilized for as long as the brace is active; neither party is pulled — this is a hold, not a pull). Same **range** as the Grapple's normal fire range. **Fuel drain scales with the held mob's mass** — heavier mob costs more per tick to restrain. **No separate duration cap** — limited purely by available fuel, same as the suit's other continuous-drain modules (Self-Healing, Player Regen, Auto-Feeder). Pulling a *heavier* mob to the player (rather than just holding it) remains a possible future **companion module**, not part of Grapple Anchor.
  - **Effect 2 — PvP:** enables grappling **other players** and pulling them in **harmlessly** (no crouch needed for player targets) — the confirmed mechanism for PvP grappling, which the base Grapple otherwise disallows.
  - **Effect 3 — anchor self on a block:** crouch **before firing** at a **block** (rather than a mob) braces the *player* in place instead of pulling them toward the anchor — the player stays stationary until they stand up (release crouch), at which point normal movement resumes. **No added fuel cost** — unlike the mob-hold (Effect 1), self-anchoring is free beyond the normal shot cost, and its duration is bound directly to crouch state rather than fuel. Completes the module's core identity: crouch-before-fire always converts the Grapple's pull into a **brace/restrain** effect, whether that's restraining a mob (Effect 1) or restraining the player's own position (this effect) — only the PvP pull (Effect 2) doesn't need crouch. This overrides the base Grapple's pull-to-block behavior (and interactions like Momentum Retention/Controlled Drop) specifically under the crouch-before-fire condition; normal (non-crouch) block firing is unaffected.
- **Water Striders Module** *(renamed from "Water Striding" to match naming convention)* — walk/run on the surface of water. **Suit availability:** Exo-skeleton and Recon only — **not available to Assault**, at least not early on. Assault's mass is too great to be supported on water's surface. May open up later for Assault via lighter/advanced materials — left as an open possibility, not a permanent exclusion like Assault's Toughness-only or Exo's zero-defense rules. **Equipment origin: Water Striders** — named directly after the real insect that walks on water via surface tension on hydrophobic legs, about as literal a match as the naming gets. **Worn in the vanilla boots slot, not inventory-carry** (see Lava Waders below for the shared reasoning across all three foot-slot items).
- **Lava Waders Module** *(renamed from "Lava Wading" to match naming convention)* — walk *through* lava (submerged/waist-deep) without taking damage. An immunity effect, not a locomotion trick. **Suit availability:** all three suits. Tier restriction is not hardcoded — it emerges naturally from the cost of whatever Thermal-capable materials the module requires, consistent with the mod's existing Tier 2 = lava-capability pattern. **Equipment origin: Lava Waders** — modeled on real fisherman's waders (waterproof boots for wading in high water), reflavored for heat/lava immunity. **Worn in the vanilla boots slot, required to function** — a deliberate choice applied to all three foot-slot items (Water Striders, Salamander Striders, Lava Waders), grounded in vanilla Minecraft's own precedent: Frost Walker and Depth Strider are both boots-slot enchantments granting special terrain/movement abilities, so foot-slot-for-movement-magic is baked into the base game already, not just a genre convention borrowed from elsewhere. Same tradeoff as the goggles (Bio Vision/Night Vision) — real boots defense/enchants traded for the ability.
- **Salamander Striders Module** *(renamed from "Lava Striding" to match naming convention)* — the lava equivalent of Water Striders Module: walking on top of lava's surface. Available to **all three suits, including Assault** — lava is dense enough to support even Assault's mass, unlike water. Same natural material-cost tier gate as Lava Wading. **Compatibility:** co-compatible with Lava Wading — both can be equipped and used together, unlike some other module pairs (Flight/Glide, Climbing types) which carry early exclusivity restrictions. **Equipment origin: Salamander Striders** — no real animal walks on lava, so this leans mythological instead of biological: the fire salamander, the classic folklore creature said to live in and be immune to flame. "Striders" rather than "Cleats," since the mechanic is surface-walking, not gripping/traction. Kept as a **separate item** from Water Striders despite the identical mechanic, since the two modules already have different suit-availability rules — avoids repeating the same "one item, context-dependent behavior" problem flagged for Red/White Muscle. **Worn in the vanilla boots slot, not inventory-carry** (see Lava Waders below for the shared reasoning across all three foot-slot items).

**Design note:** The universal module/slot system as a whole is confirmed to be a simplified version of the decoration-socket philosophy used in modern Monster Hunter — scarce, valuable slots; specialization through what's equipped rather than a fixed build; some slots (Chest) deliberately contested by many strong options.

**Naming convention (confirmed):** every module is named **"[Equipment name] Module"** — matching the equipment it originates from, with "Module" tacked on. A retroactive rename pass has been applied throughout this doc to bring existing modules in line (see individual entries for old→new names).

**Deliberate exemption from the Equipment-origin rule:** **Hulk Landing, Juggernaut, Hero Landing, Hell Dive, Reactive Defense Module, and Slipstream Module** do **not** get a standalone-equipment origin, and this is a considered exception, not a gap to fill later. Unlike everything else on this list (which all read naturally as discrete wearable/carriable objects with a real-world analog — Bladders, muscle grafts, Gecko Cleats, goggles, Squid o' War), these six are **suit-scale kinetic events or tuned modifiers on an existing inherent mechanic**, not objects — forcing a physical-item origin onto them would be contrived rather than natural. **This exemption only skips the standalone-equipment requirement — it does not change their status as modules:** all six still cost a slot, still dock via the universal Module Frame, and (Hulk Landing/Juggernaut/Hero Landing/Hell Dive specifically) stay reachable by Exo through H.E.A.T. Sink/Overdrive Core exactly as before. (Increased Dash Speed and Landing Brace, formerly in this same "still needs resolving" category, are now both resolved — see Running Gear Module under Recon's exclusives, and Landing Brace Module under Exo's own Exclusive module section.)

**Equipment-origin rule (system-wide, confirmed) — every module, existing and future, is generalized from the Grapple's own model:** each module must originate as a **real, standalone piece of equipment** the player can use on its own (a Gadget), which then docks into a **universal Module Frame** — one single, generic Frame type for the whole system, not a bespoke frame per module ("no need to complicate this part"). This turns the module/slot system from "abstract upgrades you unlock" into "real gadgets you choose to hardwire into your suit instead of carrying loose," and it carries the same design principle already set for Grappling Brace: **the module grants the suit the capability to use the equipment — the equipment itself isn't what's being empowered.**
- **Retroactive scope, confirmed:** this applies to every module already listed below (Bio Vision, Flight, Auto Shield, Landing Brace, all of them), not just new ones — each needs its own standalone-equipment origin designed. This is a **large standing project**, tracked here rather than solved in one pass; modules below remain documented as abstract effects until their equipment origin is designed.
- **Resolves the Hero Landing fork** (see Recon's exclusive modules): Hero Landing is confirmed to be its **own equipment + the universal Frame**, not a reskin of Landing Brace's effect — a single physical item behaving in fundamentally different damage-classes per suit would have been a bigger asymmetry than the Frame system is meant to carry.

### Cross-Suit Exclusivity Notes
- **Flight vs. Glide** and **Ladder/Vine Climbing vs. Wall-run Climbing** are each mutually exclusive pairs **early on** — implies this restriction loosens at later tiers or via points (exact unlock mechanism not yet decided).
- **Recon is the exception:** can run both Flight types and both Climbing types simultaneously, even early — part of its dedicated mobility identity.

### Grapple Module

Suit-integrated version of the **Grapple** (G.R.A.P.P.L.E) gadget — see `dermicraft-gadget-notes.md`. Reuses the gadget's grapple logic **identically** (Deep Rock Galactic-style direct pull, Slurry fuel charged at reload, mass-based mob interaction, normal fall-damage rules on vertical pulls, full tier upgrade ladder). The module changes only *how it is triggered and carried*, not what it does.

**Not a separate item — the universal Module Frame:** the module is the **same generic Frame type used system-wide** (see Equipment-origin rule, above) occupying a suit slot; the actual Grapple (with its tiers, loaded cell, and fuel grade) docks into it — this was the original model the system-wide rule was generalized from, not a bespoke Grapple-specific frame. Single source of truth — no duplicate upgrade path between handheld and docked form. Full nesting/removal details (Player → Suit → Frame → Grapple, Dock's role, specialized station dependency on the Gadget Upgrade System) are in the Grapple's own entry in `dermicraft-gadget-notes.md`.

**Tradeoff:** costs a suit module slot in exchange for freeing an inventory slot and never needing to switch to the held gadget — always-ready traversal at the price of a scarce slot. No power difference versus the gadget.

**Slot:** undecided — natural fit is an **Arm** slot (hand-fired traversal tool), not yet locked in.

**Controls:** Fire, Release, and Lock are each independent, player-**customizable keybinds** (defaults still TBD; Fire and Release are discussed here as unarmed right-click / a separate release input by default, but nothing is hardcoded). Because vanilla does not sync empty-hand right-clicks to the server, Fire requires the mod's **first client→server custom packet** (`CustomPacketPayload`): the client catches the input and tells the server to fire. This packet channel is reusable plumbing for future input-driven modules, and also carries the Lock toggle and manual Release (see the Grapple's Momentum Retention tier).

**Lock mechanic (accident prevention):**
- The dedicated **Lock keybind** toggles the lock. Locked = Fire does nothing; unlocked/armed = Fire fires the grapple — keeps a stray input from flinging the player across the map.
- **Locked by default** on equip; the player must deliberately unlock to arm it.
- Giving Lock (and Release) their own binds, separate from Fire, avoids any ambiguity about what a given input means mid-pull — each action has exactly one meaning.

**State indicator (two non-removable status effects):**
- Two mutually-exclusive custom effects — *Grapple: Locked* and *Grapple: Armed* — provide a persistent HUD icon for the current state. Toggling removes one and applies the other; exactly one is present while equipped.
- Both are **no-tick, particle-less, and non-removable by external effect-clearing (e.g. milk)** so the icon cannot desync from the true lock state. Server cost is negligible — synced only on change, no per-tick work.
- The module's own logic manages them: apply *Locked* on equip, swap on toggle, and remove whichever is present on unequip/suit removal so no icon lingers without the suit.

**Feedback:** a button-click sound plays on each lock toggle, pairing with the icon swap — the click confirms the input landed, the icon confirms the resulting state.

---

## Assault

**Fork identity:** Heavy combat specialist. Cumbersome compared to Recon, but fuel improves its mobility somewhat (never fully closing the gap).

**Hazard handling (confirmed) — Hazard Resistance dampens effect strength, Hazard Defense handles damage.** Every suit carries both stats (see `dermicraft-hazard-effects-notes.md` for the full mechanic). For **non-damaging** hazards, Assault's **Hazard Resistance** dampens **effect severity** (weaker Nausea/Wither/Slowness-style stacking, by a percentage) while exposure timing stays standard — the mirror image of Recon's exposure-*time* expression of the same stat. For **damaging** hazards, Assault's **Hazard Defense** reduces the damage taken; only genuinely *direct/outside-in* damage (Thermal Hazard) additionally feeds the **general H.E.A.T. system** — deterioration-type damage (Radiation, Biohazard) never generates H.E.A.T. even though it hurts, per the general rule in `dermicraft-hazard-effects-notes.md`. See `one-punch-full-breakdown.md` → H.E.A.T. sections for the full mechanic (generation, the capacity-cap-doubles-as-damage-soak-cap rule, and Hazard Defense's pre-H.E.A.T. damage discount). In short: Assault doesn't avoid Thermal Hazard, it **eats it for fuel**, up to a cap — everything else it just has to tank or resist.

**Slot layout (Tier 0):**
| Slot | Count |
|---|---|
| Head | 0 |
| Chest | 1 |
| Arm (each) | 2 (4 total) |
| Leg | 1 (shared/total) |

**Total: 6 slots**, heavily concentrated in Arms; no Head slot at all.

**Slot growth — first tier-based slot (confirmed):** Assault's very first tier-based slot increase (per the standard "one new module slot every second tier" rule under Tier & Point System) fills in a **Head slot** — closing what was previously a total gap in its Tier 0 layout. This is not an exception to the standard slot-growth schedule, just where Assault's first scheduled slot happens to land; both Assault and Recon continue gaining further slots on the same schedule at later tiers.

**Baseline stats (Tier 0):** Movement/speed just above unarmored — deliberately near the bottom, reinforcing Assault's cumbersome identity even before modules are applied. Some defense is present at Tier 0, but the suit is still squishy — no Toughness yet.

**Baseline traits (standard, not modules or points):**
- **Heavy Strike** — increased attack damage for both combat and mining, in exchange for a smaller swing-speed boost than other speed-focused options give.
- **Reduced knockback taken** — standard, present from the start.
- **Knockback resistance** increases further with each tier upgrade (Dermicraft's own numbers, not vanilla's).
- **Toughness** — begins at **Tier 1**, not Tier 0. This is the moment Assault's tanky identity fully kicks in. Scales upward in small amounts with further tier upgrades. **Assault-exclusive** — Recon may get a small amount at high tiers (nowhere near Assault's level); Exo never gets it through normal progression.
- **Reactive Defense** *(moved from module to inherent trait)* — Assault's defensive complement to One Punch's offense, pairing the same way Recon's Slipstream complements Scout Blade. Increases defense; costs a one-time burst **at the moment of being hit** (not a continuous drain) — a **H.E.A.T. consumer** (per the Assault-exclusives-use-H.E.A.T. unification): **process order is cost first, then H.E.A.T. gain** — the incoming hit's Reactive Defense cost is paid from **currently-stored H.E.A.T.** (banked from *previous* hits) first; if that's insufficient, the **shortfall is paid from regular Slurry fuel**. *Only after* the cost is resolved does the hit's own H.E.A.T. gain get banked (per the normal gain rule) — so this hit's own gain never counts toward paying for its own trigger. **Balance guarantee:** the cost is calibrated to always be *less* than what this hit will bank, so triggering Reactive Defense never nets an overall H.E.A.T. loss from that hit, even though the cost draws from the pre-existing pool rather than the new gain.
  - **Net tradeoff of making this inherent:** more power (free, no slot cost, plus a stackable module exists now) *and* more cost (the H.E.A.T./fuel draw now fires **unconditionally on every hit** — no more opting out by simply not equipping it, the way a module could be skipped). Both sides of the ledger grew, not just the upside.
  - **Planned rebalance — inherent weakened, module strengthened:** both this trait and Slipstream (Recon's mirror) are candidates to have their **inherent-trait strength reduced**, with the **new module version tuned stronger**, so slotting the module is a real, felt choice rather than a marginal bonus. Applies symmetrically to both suits, not just Assault. Not yet numerically decided.

**Movement-boost modules:** Individually weaker than on other suits, but **stackable** — Assault can approach competitive mobility only by investing multiple slots, rather than getting it cheaply.

**Flight restriction:** No Flight-family modules at all at Tier 0 — a hard gate, not an exclusivity rule. Flight becomes available on upgraded versions only.

**Exclusive modules:**
- **Reactive Defense Module** *(new — stacks with the inherent Reactive Defense trait above)* — same pairing pattern as Slipstream's new module version (see Recon). This "inherent trait + stackable module" pattern is **scoped specifically to these two defensive-complement traits, a one-off symmetry fix — not a general template** extended to other inherent traits (Toughness, Heavy Strike, One Punch, Scout Blade don't get this treatment). **Purpose beyond the symmetry fix:** since this is a true module (not inherent), it's reachable by **Exo via H.E.A.T. Sink** — giving Exo genuine survivability tooling (H.E.A.T.-cost damage mitigation) **without breaking its permanent zero-Defense rule**, since this mechanic never touches the Defense/Toughness stat itself, it's a separate system. Exact stacking mechanic (does it add flat defense, reduce H.E.A.T. cost, increase the balance-guarantee margin, etc.) not yet decided — deferred alongside the rest of this module's numbers.
- *(The former Assault-exclusive "Landing Brace, Arm placement" privilege is gone — Landing Brace's suit-docking is now Exo-exclusive instead. See Exo's own Exclusive module section.)*
- **Hulk Landing** *(in active discussion — several pieces settled, more to come)* — Assault-exclusive fall-damage module. Reduces fall damage **before armor/Toughness is applied** — a separate reduction layer on top of whatever the suit's own defense already mitigates, not a replacement for it — and produces a **crater sized relative to the damage absorbed** (bigger fall → bigger crater). The Assault counterpart to Recon's Hero Landing — impact-heavy where Hero Landing is clean.
  - **Open gap, flagged:** what happens if the landing point is **directly on a mob** rather than terrain — not yet defined at all (Hulk Landing's mechanics so far only cover the block/crater case). Needs its own rule, possibly mirroring Hell Dive's mob interaction.
  - **Its own equipment + the universal Module Frame** (per the Equipment-origin rule) — tier/upgradability discussion deferred to when that equipment is designed.
  - **Cost:** now a **H.E.A.T. consumer**, same draw order as Reactive Defense — cost is paid from **currently-stored H.E.A.T. first**, with any **shortfall paid from regular Slurry fuel**. Unlike Reactive Defense, there's **no balance guarantee against this trigger's own gain**, since a fall impact doesn't generate H.E.A.T. the way taking a hit does. **Tentative:** higher tiers of Hulk Landing might start **generating H.E.A.T. from the landing impact itself** (mirroring how taking a hit generates it) — not decided, deferred to the tier/equipment discussion.
  - **Trigger:** **automatic** — no crouch or input requirement, unlike Hero Landing. Fits Assault's always-on, raw-power identity (Heavy Strike, Toughness) rather than Recon's timing/skill-based mechanics.
  - **Damage reduction expressed as a percentage, not a flat number** — same decision applies to Hero Landing (see that entry). Exact percentage deferred to the tier/equipment discussion.
  - **Crater size:** scales via a **formula** relative to damage absorbed (not stepped size bands) — exact formula still open.
  - **Impact zone:** a **2×2 area centered on the player** at the landing point (the crater/shockwave's point of origin), rather than a single block.
  - **Shockwave, tiered:** the block-break/crater effect is the base behavior from early tiers; **knockback to nearby entities is added at higher tiers** — so Hulk Landing scales in two dimensions as it upgrades (damage-reduction percentage *and* added shockwave effects), not just bigger numbers on the same behavior. Exact tier threshold for knockback deferred to the equipment/tier discussion.
- **Juggernaut** *(in active discussion — core mechanics settled, numbers deferred)* — Assault-exclusive module named after the Marvel character. Lets the player **run through walls**, carving a **2×2 tunnel** while sprinting — in theory, sustained use could bore straight through a mountain. Its own equipment + the universal Module Frame, per the standing rule.
  - **Trigger:** **automatic** — sprinting into a wall starts tunneling, no special input, matching Assault's no-finesse identity. **Guarded by a short activation delay (a few ticks)** of sustained wall-contact-while-sprinting before it engages, specifically to prevent accidental triggering from a stray bump or corner clip.
  - **Block removal:** follows **Hell Dive's precedent** — standard vanilla break pattern from the player's POV (normal break event, real drops), not a silent deletion or hollowing effect.
  - **Footprint:** fixed **2×2**, matching Hulk Landing's destructive-effect language.
  - **Tunneling speed** scales against **block hardness** (soft stone = fast, tough material = slower) — and this scaling is itself **influenced by upgrading Juggernaut's own equipment**, i.e. a two-variable formula (hardness × equipment tier), not a fixed rate. Exact formula deferred to the equipment/tier discussion.
  - **Hardness ceiling:** Juggernaut has **hard caps** on what it can break through at all (not everything is eventually breachable) — and **those caps rise with equipment level**, letting higher tiers punch through progressively tougher material. Exact cap values per tier deferred to the equipment/tier discussion.
  - **Cost — two-resource model:** primary cost is **H.E.A.T.** (see `one-punch-full-breakdown.md`), drawn **continuously and partially** while tunneling (Juggernaut's own spending pattern — contrast with One Punch's all-or-nothing full-bank spend) — **plus a modest secondary Slurry-fuel draw** running alongside it. If H.E.A.T. is depleted mid-tunnel, Juggernaut **does not hard-stop** (which could strand the player inside solid terrain) — instead it **falls back to a much steeper fuel-only draw** to keep going. Exact rates for both draws deferred to the equipment/tier discussion.
  - **Sibling mechanic, not a conflict:** thematically close to the **Drill Hammer** gadget (3×3 face AoE mining) — a wielded-weapon-swing version of "clear a lot of blocks at once," where Juggernaut is the suit-driven, sustained-run version. Worth cross-referencing if a shared "mass block destruction" costing logic is ever useful.
  - **Doubles as an attack — mobs, not just blocks.** Confirmed after discussion: an ability named Juggernaut needs to be person-proof as well as wall-proof (the character's whole defining trait), it resolves an otherwise-awkward gap (a mob standing in the tunnel path shouldn't harmlessly block or be ignored by something that bores through granite), and it matches the sibling precedent already set by Hell Dive (works on mobs *and* blocks).
    - **Contact:** hits **every mob in the 2×2 path** (not single-target like Hell Dive's precision pick) — reinforcing Juggernaut as the bulldozer counterpart to Hell Dive's precision dive.
    - **Push vs. grind:** mobs in the path are **pushed forward**, using the **same mass-comparison framework as the Grapple** (lighter → displaced; too massive → not moved). If a mob's mass is too great to push, the player instead **grinds damage against it in place** rather than displacing it. The mass ceiling for "pushable" **rises with Juggernaut's own equipment tier**, same as its block-hardness ceiling — both scale together on the same upgrade path.
    - **Toughness slows the charge like block hardness does** — with one exception: **if the hit kills the mob outright, the speed cost is negated.** Weak mobs melt through without breaking stride; tough survivors cost real momentum.
    - **Damage model — burst-then-grind, not speed-based** (speed-scaled damage stays Recon's exclusive identity, per the Scout Blade). A **damage spike on initial impact**, then **reduced continuous damage** for as long as contact/grinding continues. Distinct combat texture from both One Punch (single decisive blow) and Hell Dive (single precise strike) — Juggernaut keeps hitting while it keeps moving.
    - Exact burst/grind damage values, and the mass/hardness ceiling numbers per tier, deferred to the equipment/tier discussion like the rest of Juggernaut.

---

## Recon

**Fork identity:** Light, high-mobility specialist. Trades defense for mobility, but still carries a real (reduced) defense stat — unlike Exo's permanent zero.

**Slot layout (Tier 0):**
| Slot | Count |
|---|---|
| Head | 2 |
| Chest | 1 |
| Arm (shared/total) | 1 |
| Leg (each) | 2 |

**Total: 6 slots**, concentrated in Head and Legs — light on hands-on/combat utility (only 1 shared Arm slot), heavy on perception and mobility.

**Baseline stats (Tier 0):** Movement/mobility stats similar to the Exo-skeleton's, toned down to balance out having actual defense (unlike Exo).

**Defense:** Present, reduced compared to Assault — the core tradeoff for its mobility focus.

**Hazard handling (confirmed) — Hazard Resistance is the lead stat, expressed as exposure time.** Every suit carries both Hazard Resistance and Hazard Defense (see `dermicraft-hazard-effects-notes.md` for the full mechanic); Recon leans on **Hazard Resistance**, and expresses it specifically as **exposure time rather than effect severity** — a high-Resistance Recon must be in/near a hazard **longer before effects start stacking**, and the stack **tapers off faster** once they leave, while effect severity itself follows the default curve. Fits Recon's mobility-over-endurance identity: it rewards moving through danger rather than tanking it, the mirror image of Assault's severity-dampening expression of the same stat. **Broad but partial, with natural affinities:** Recon is meant to gain real but incomplete capability as its materials improve, eventually reaching *some* coverage against most/all hazard tags, not capped at a fixed subset forever — **Radiation, Biohazard, and Metaphysical are its natural affinities** (strongest without extra investment), while Thermal Hazard is reachable too, just not where the suit leads.

**Fuel relationship:** Unlike Exo, Recon **requires fuel to function normally** — there is no "fine unfueled" state. Better fuel grades improve performance. Fuel does **not** affect defense in either direction. When fuel runs out (after exhausting all available sources per the standard fallback order), Recon draws from the player's hunger bar instead of functioning normally.

**Module flexibility:** Can run **both Flight types** and **both Climbing types** simultaneously — no early exclusivity restriction, unlike Exo and Assault.

**Exclusive modules:**
- **Running Gear Module** *(renamed from "Increased Dash Speed" to match naming convention)* — Recon-exclusive sprint speed boost, distinct from the universal Achilles Graft Module (Movement Speed). **Equipment origin: Running Gear** — worn in **both the vanilla leggings slot and the suit's Leg module slot** (same real-armor tradeoff pattern as Water Striders/Salamander Striders/Lava Waders). **Base multiplier: 1.5× sprint speed at Tier 0.**
- **Slipstream Module** *(new — stacks with the inherent Slipstream trait)* — mirrors Assault's new Reactive Defense Module, added in exchange for that change so both suits' inherent defensive trait gets the same "stackable module version" treatment (scoped to just these two — see that entry for the full reasoning, not a general template). **Purpose beyond the symmetry fix:** since this is a true module (not inherent), it's reachable by **Exo via Overdrive Core** — giving Exo a genuine evasion-based survivability option (dodge chance) **without breaking its permanent zero-Defense rule**, since dodge chance is a separate system from the Defense/Toughness stat. Exact stacking mechanic (e.g. raising the dodge-chance-per-speed curve, lowering the speed threshold, etc.) not yet decided — deferred alongside the rest of this module's numbers.
- **Hero Landing** *(in active discussion — several pieces settled, more to come)* — Recon-exclusive fall-damage module. Negates **most if not all** fall damage when the fall distance is **≤ the world's full vertical range (build-height cap down to bedrock)** — i.e. effectively any survivable-height fall in the world. Thematically the superhero three-point landing.
  - **Its own equipment + the universal Module Frame** (resolved fork — see Equipment-origin rule under Universal Modules) — not a reskin of Landing Brace.
  - **Tier:** undecided — possibly **upgradable**, with damage reduction scaling per tier/upgrade level rather than unlocking at full strength immediately, specifically to avoid being overpowered too soon.
  - **Cost:** one-time **fuel draw on impact** (matches Reactive Defense's burst-at-trigger-moment pattern).
  - **Trigger:** crouch must be held **at the instant of impact** to activate — the player can crouch any time during the fall, but only the state at landing matters.
  - **Damage reduction expressed as a percentage, not a flat number** (same decision applies to Hulk Landing). Exact percentage still open, pending the upgradable-tier decision.
  - **Stacking:** governed by the general mod-wide rule (see `dermicraft-project-primer.md` → damage-reduction stacking) — stacks with other fall-damage-reducing sources unless one fully negates the damage, in which case that one handles it.
  - **Feedback:** sound + landing dust particles; animation later.
- **Hell Dive** *(in active discussion — core mechanics settled, more to come)* — a new Recon-exclusive fall module, arrived at after considering whether Recon could just reuse Hulk Landing (decided against — see Hulk Landing's note on keeping distinct behaviors as separate equipment). **Converts fall distance into pinpoint damage** rather than only negating/redirecting it like Hero Landing or Hulk Landing — an offensive dive-strike identity layered on top of the same defensive base those two provide. **Works on both mobs and blocks.** Its own equipment + the universal Module Frame, per the standing rule.
  - **Self-protection:** same fall-damage reduction as Hulk Landing (percentage-based, pre-armor/Toughness). Hell Dive is a defense-plus-offense hybrid, not purely offensive.
  - **Area of effect — fixed 2×2**, same footprint as Hulk Landing, but **does not expand with tier/damage** the way Hulk Landing's crater does — only **depth** scales with the formula, never width. This is the core differentiator: Hulk Landing grows wide-and-shallow; Hell Dive stays narrow-and-deep.
  - **Block behavior:** a **depth-scaled drill** — every block in the shaft is actually broken (not hollowed/left intact), following the **standard vanilla break pattern from the player's point of view** (normal break event, normal drops/particles/sound) rather than a special non-standard removal.
  - **Mob behavior:** **single target, once per fall** — Hell Dive can only trigger **one time per fall**, against **one target** at most. Whichever mob is **closest to the exact impact point** takes the damage. **Ground-mob interaction:** the player's own fall-damage-reduction self-protection **still applies normally** even when the single trigger is spent on a ground-based mob — hitting a target doesn't disable the player's own protection for that landing. **Flying-mob interaction:** if the target is airborne, the player **passes through it after dealing damage** and continues the dive downward — but since the trigger is already spent for that fall, **no second effect (mob hit or block drill) fires on the subsequent ground impact**; the rest of the descent is a normal fall (subject to whatever other stacking protections apply). This is a real tradeoff: using Hell Dive on a flying mob mid-fall forfeits its protection/drill for the landing that follows, if there's still distance left to fall. **Fall-damage counter reset (important):** for that remaining unprotected descent, the fall-distance counter **resets to the flying mob's height, not the player's original peak** — so only the distance from the mob down to the landing counts toward damage, not the full original fall from where the player started. Without this, "using Hell Dive on a flying mob" would risk the full original fall's damage on top of already spending the ability, turning a tactical choice into a hidden trap.
  - **Trigger:** **crouch** — same "held at the moment of impact" pattern as Hero Landing, not automatic like Hulk Landing. Fits Hell Dive's more deliberate, aimed identity.
  - **Cost:** same one-time **fuel draw on impact** as Hero Landing and Hulk Landing.
  - **Exclusive with Hero Landing — permanent.** The two cannot be equipped simultaneously (Recon picks one fall-identity: clean negation, or protected-plus-offensive dive), and this exclusivity **never loosens** — unlike the mod's early-on Flight/Glide and Climbing-type exclusivity pattern. Comparable to the mod's other permanent rules: Exo's permanent zero-defense, and Toughness being permanently Assault-exclusive.
  - **Formula** for distance→depth/damage conversion — deferred to the equipment/tier discussion, same as Hero Landing and Hulk Landing.
  - All of the above is explicitly still up for discussion, not locked.

---

## Suit Balance Summary

**At Tier 0 specifically**, all three suits total **6 slots**, distributed differently to reflect specialization — no suit has strictly more total customization capacity than another *at that starting tier*. **This changes with progression** — see Exo's defining feature, below.

| | Exo-skeleton | Recon | Assault |
|---|---|---|---|
| Defense | None (permanent) | Reduced | Higher (full Toughness from Tier 1) |
| Baseline mobility | High, free | Moderate (toned down from Exo) | Low (near-unarmored) |
| Fuel requirement | Optional — no drawback unfueled | Required — hunger drain when dry | Required — hunger drain when dry |
| Identity cost | Zero defense ceiling | Fragility | Mobility / hands-on utility (limited Arm/Leg slots at low tiers, no Flight until upgraded) |

**Design intent (confirmed):** This is a three-way trade-off, not a power ladder — Exo trades ceiling for freedom, Recon trades defense for mobility, Assault trades mobility for raw power, and each pays its fuel-maintenance cost differently (or not at all, in Exo's case).

**Exo's defining feature, confirmed — horizontal breadth as the payoff for a permanent zero-defense ceiling.** As Exo upgrades through tiers, it gains **more total module slots than Assault or Recon** — the Tier 0 "all suits equal at 6" rule is a starting-point snapshot, not a permanent cap; Exo's slot count deliberately outpaces the other two at higher tiers, specifically to balance out never having the vertical specialization (unique inherent traits, deeper exclusive combat/mobility ceilings) Assault and Recon get instead. This is paired with Exo's second defining trait: **cross-suit module compatibility**, via bridge items like **H.E.A.T. Sink** (Assault) and **Overdrive Core** (Recon) — see their entries below — which grant Exo access to **other suits' exclusive modules** specifically (not their inherent suit-signature traits, which stay permanently exclusive to their origin suit regardless). So Exo's answer to "trades ceiling for freedom" is now concrete: freedom = more slots, plus eventually the ability to equip *any* module in the game, at the cost of never having Assault's Toughness/One Punch or Recon's Scout Blade/Slipstream itself.

**Exo — Cross-Suit Compatibility Modules** *(in active discussion — core mechanics settled, numbers deferred)*: the concrete mechanism behind Exo's defining feature above. Each bridge item (a) boosts its origin suit's own resource further, and (b) grants Exo access to that suit's exclusive **modules only** — never its inherent, suit-signature traits, which stay permanently exclusive regardless of infrastructure access.
- **H.E.A.T. Sink Module** *(renamed to match naming convention)* — the Assault bridge. Its equipment base may also double as an actual component of Assault's own construction. **On Assault:** adds more H.E.A.T. capacity — **the H.E.A.T. cap is now confirmed to exist and to double as Assault's damage-soak cap** (see `one-punch-full-breakdown.md` → H.E.A.T. — Storage & Loss for the full mechanic; exact numbers still deferred to the equipment/tier discussion). **On Exo:** *is* Exo's H.E.A.T. bank outright (Exo has no baseline H.E.A.T. generation of its own to build on) — grants access to Assault's H.E.A.T.-consuming modules (Juggernaut, and others as they're built), **not** One Punch itself (suit-signature, stays Assault-exclusive).
- **Overdrive Core Module** *(renamed to match naming convention)* — the Recon bridge, same shape mirrored for speed instead of H.E.A.T. **On Recon:** raises achievable peak speed further, directly boosting Scout Blade's damage multiplier and Slipstream's dodge chance. **On Exo:** raises Exo's speed ceiling high enough for Recon's speed-gated modules (Increased Dash Speed, Hero Landing, Hell Dive) to be meaningful once unlocked — **not** Scout Blade or Slipstream themselves (suit-signature, stay Recon-exclusive).
- **Open questions:** exact slot-count-per-tier curve for Exo (deferred to the tier/equipment pass). Whether H.E.A.T. Sink's equipment base literally shares a component with Assault's construction recipe, or is merely thematically similar (same open question noted when H.E.A.T. Sink was first proposed). Whether Overdrive Core gets a parallel "may double as a Recon component" property, matching H.E.A.T. Sink's, for symmetry.

---

## Tier & Point System

**Layering order (critical for implementation):** Base stats → tier adjustments applied on top → the result becomes the new base → point-spend upgrades are calculated after that. Each layer's output becomes the next layer's starting point.

**Tier ladder reference:** Loosely modeled on vanilla armor material progression (Leather → Chainmail → Iron → Gold → Diamond → Netherite). Stats vanilla armor actually has (Defense, Durability) borrow vanilla's proportional per-tier increases directly. Stats unique to Dermicraft's suits (movement speed, fuel efficiency, swing speed, etc.) extrapolate a similarly-sized proportional jump relative to the suit's own Tier 0 baseline, using vanilla's average per-tier percentage growth as the reference curve.

**Tier 0** = the base/starter version of each suit, as detailed above. Deliberately weak across the board — even Assault is still squishy here, with no Toughness yet. **No unassisted-removal damage cost applies at Tier 0** for any suit.

**Tier 1** = first upgrade tier. Assault gains Toughness here. Unassisted removal begins costing damage starting at this tier: **1 point of damage at Tier 1.**

**Points, awarded:** A fixed number of points are granted per tier-up. At the **final tier**, an additional system allows purchasing further points at an **ever-increasing cost**, paid in **large volumes of difficult-to-produce materials** (specifics not yet decided) — allows continued progression without a hard ceiling, while naturally decelerating.

**Point-spend menu (1 point per rank, uniform cost across categories):**
1. Slot relocation — move an existing slot to a different body part.
2. Durability
3. Player max health while worn
4. Movement speed
5. Jump height
6. Safe landing distance / fall damage reduction
7. Damage dealt
8. Swing speed
9. Reduced fuel cost
10. Suit healing speed — independent of the Auto Heal module's suit-durability effect, stacks with it, but takes many ranks to match what the module alone provides.
11. Player healing while worn — same relationship to Auto Heal's player-heal effect as #10 has to its suit-heal effect.
12. Defense — **Assault & Recon only**, not Exo.
13. Reduced removal damage / reduced assisted-removal time — a slower, point-based path toward the same effect Auto Heal's removal-safety effect provides directly.

**Slots are not point-purchased.** Each suit gains a new module slot **every second tier**, as a fixed structural unlock — same schedule for all three suits. **Exo receives a larger grant per unlock** than Assault/Recon (more than one slot at each of those milestones), which is how Exo ends up with more total slots at higher tiers (see Exo's defining feature, above) without needing a different cadence. Points can only **relocate** an existing slot to a different body part (1 point per relocation) — they cannot create new slots.

### Unassisted vs. Assisted Removal

- **Unassisted (rushed) removal:** costs the player direct damage, scaling with suit development. Confirmed ratio at Tier 1: 1 point of damage. Scaling for higher tiers not yet fully defined, but confirmed to be **linear**.
- **Assisted removal (via a dedicated dock/station, "safe removal"):** costs **time** instead of damage, at a ratio of **1 second per point of damage** the unassisted method would have inflicted. Fully safe regardless of tier — always an option, at the cost of standing still for the equivalent time.
- **Mitigations exist on both fronts:** Auto Heal's removal-safety effect and point category #13 both reduce this cost, meaning the raw numbers can scale aggressively at high tiers without being unfair, since players have chosen, investable ways to blunt it.
- **Max tier, no mitigation:** ripping off a fully-developed suit with zero preparation is calibrated to deal damage **equal to or exceeding the player's effective max health** — functionally equivalent to dying (normal death sequence, respawn, etc.), not a separate scripted instant-kill. This means external survivability tools (extra health sources, damage reduction, absorption effects) that exist independently of the suit could still save a careless player, since the mechanic works through the normal damage/health system rather than bypassing it. This is intentional: the threat is severe specifically *because* players have multiple legitimate ways to plan around it (the dock, Auto Heal, point investment) — a well-prepared player is never caught off guard.

**Open questions:**
- Full scaling curve for unassisted-removal damage across tiers (confirmed linear, exact per-tier values not yet set).
- Exact currency and cost curve for the final-tier point-purchase system.
- Whether the Flight/Glide and Climbing exclusivity restrictions ever lift for Exo/Assault, and if so, at what tier or via which point/module.
- Whether Exo ever gets any defense via a late-game module.
