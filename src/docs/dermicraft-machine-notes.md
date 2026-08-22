# Dermicraft Machine Notes

Running log of decided design choices for Machines and the reasoning behind them. Add a new entry per machine as decisions get made.

---

## Table of contents

This doc is large — use this to jump to a section instead of reading the whole file. Regenerate by hand (or ask Claude to) whenever a top-level machine entry is added, removed, or renamed; a stale entry here just means falling back to a header grep, not a wrong answer.

- Machine health and fuel system
- Machine Evolution — Smart vs. Dumb (mod-wide rule)
- Known machines
  - Skin Tank
  - Chitin Tank
  - Drooling Cauldron
  - Masticator
  - Effluentcer
  - Upgraded Effluentcer (name not yet decided)
  - Metastasizer
  - Gestator
  - Drooling Crucible
  - Craw
  - Flesh Lab ("FL") — component blocks
    - Lab Floor
    - Autonomous Structure Growth
    - Core
    - Brain — Tier 1 Control (limitations)
    - Core — Recursive Crafting & Network Behavior
  - Imago Engine (working name was "Network Evolver")
  - Filling Station — folded into the Mutator (see below)
  - Mutator
- Farming automation concepts (early planning)
  - Shared mechanics (starting point, not locked)
  - Mr. Farmer
  - Mr. Shepard
  - Mr. Logger
  - Mr. Hunter
- Template for new entries

---

## Machine health and fuel system

**Machines have an HP pool** that drains only while actively processing — idle machines take no damage. When HP hits zero the machine stops entirely and will not process until health is fully restored.

**Fuel (Slurry) serves two roles simultaneously:** speed multiplier and healing source. Heal rate is derived directly from the fuel's heal modifier — faster slurries heal faster on the same scale (see `dermicraft-slurry-notes.md` for the full property model).

**Three operational states:**
- **Fueled and full health** — processes at full speed for the introduced fuel.
- **Unfueled while processing** — health drains at a rate determined by the machine. No processing occurs.
- **Starved (0 HP) with fuel reintroduced** — healing only, no processing until full health is restored.
- **Fuel introduced before hitting zero** — machine processes at **10% of what the fuel's normal speed would be**, while simultaneously healing. Catching a starving machine before it hits zero is meaningfully better than letting it starve.

**Hunger rate** varies by machine and Stage — later/higher-Stage machines drain faster, proportionally raising the pressure to maintain proper fuel infrastructure. Early machines are forgiving while the player is learning the fuel system; Stage 2+ machines punish neglect noticeably faster.

**Unfueled baseline:** Machines run without Slurry but at a drastically reduced rate — **1/10th the progress of Crude Slurry** (i.e. 0.1 progress/tick vs. Crude's 1 progress/tick). Combined with health drain while processing, unfueled operation is a viable temporary or emergency state, not a real alternative to proper fuel infrastructure. A player who builds many unfueled machines to compensate faces proportionally more health drain across all of them simultaneously, scaling the fuel demand with machine count rather than making it a front-loaded fixed cost.

**Crude Slurry as the practical floor:** Crude has a by-hand recipe with readily available ingredients, so a player is never truly forced into the unfueled state — they just have to not bother making Crude. The unfueled mode is therefore a genuine fallback rather than an intended playstyle.

**Design note — cycle structure:** Machines operate on a 10-tick (0.5 second) cycle. This is a server performance decision rather than a design one and isn't surfaced to the player — the numbers players interact with are effective progress/tick rates, not the underlying cycle mechanic.

---

## Machine Evolution — Smart vs. Dumb (mod-wide rule)

**Status:** Confirmed. Governs every future Machine and structure's evolution/tier-up path.

**Two categories, split by a thematic rule — whether the thing evolving is "smart" or "dumb":**

- **"Dumb" Machines — forced evolution, standard mechanic (unchanged).** Ordinary single-block Machines don't want to evolve on their own — they have to be **forced** into it via **Evolution Catalyst injection**, the original mechanic (already confirmed for Drooling Cauldron → Drooling Crucible; Skin Tank → Chitin Tank follows the exact same pattern). This is the **standard, default path for every Tier 1→2 Machine evolution**, including the Masticator, Effluentcer, and Metastasizer as they reach their own Tier 2 forms.
- **"Smart" structures — willing evolution, the new Gear Worx Stations model.** The **FL's control block** (Brain → Core) and the **Gear Worx Stations** (Dock/Workbench/Growth Chamber) are framed as intelligent/living enough to **want** to evolve on their own — no forced injection. Instead: the screen shows the fluid requirement, the shared pool fills it, the player presses a button, a **timed** process runs, and **if interrupted the fluids are lost but the structure itself is never at risk**. This is the exception, not the new default — it applies only to these "smart" structures, not to ordinary Machines.

**Why this matters going forward:** when a new Machine or structure gets a Tier 2+ form, default to the **forced/Evolution Catalyst path** unless there's a specific thematic reason (the thing is framed as intelligent/living, like the FL or Gear Worx Stations) to give it the willing/button-press path instead.

**Hazard rejection is instinctive, not intelligent (confirmed flavor, ties "dumb" to the hazard-hard-stop rule).** A Tier 1 "dumb" Machine still hard-refuses to process a fluid above its hazard tolerance (see `dermicraft-project-primer.md` → Hazard tag hierarchy) even with no Brain anywhere in its recipe. In-fiction framing: the machine **literally feels the fluid and recoils from it** — a reflexive, sensory response, not a decision. This is consistent with (not a new mechanic layered on) every Machine implant recipe already requiring **Nerve Cluster** tissue — the sensory capacity was always built in, it just doesn't require intelligence (Brain) to act on. A future upgrade tier tolerating more hazards isn't "getting smarter about it," it's growing a *thicker hide*, not a *bigger brain*.

**Concrete "smart" test (confirmed) — the Brain as ingredient.** Rather than a judgment call per new addition, "smart" status is defined by a checkable recipe rule: **a structure only qualifies as smart if the Brain (or a Brain-derived ingredient) is required in its recipe.**
- **FL's control block:** obviously qualifies — the Brain **is** the thing being built/evolved (Brain Block → Core).
- **Gear Worx Stations (Dock/Workbench/Growth Chamber):** qualify via their **initial construction recipe** — the Brain is a required ingredient when a Gear Worx Station is first built, not necessarily in its later tier-up/evolution recipes. Building with the Brain from birth is what earns willing-evolution status going forward; exact quantities/other ingredients still TBD (see Gear Worx Stations notes).
- **Ordinary Machine families** (Masticator, Effluentcer, Metastasizer, Skin Tank/Chitin Tank, and small machines generally) **never** include the Brain in any recipe, at any tier — this is what keeps them permanently "dumb," forced-evolution only, for their entire family lineage.

**Families (new concept, mod-wide):** Machines group into **families** — a family shares the same shape and function across all its tiers, gaining only better performance and higher hazard-tolerance as it climbs (e.g. every Masticator tier is still recognizably a Masticator, just faster/tougher). A family's smart/dumb status is fixed for its entire lineage — a dumb family (which is all of them, by design, aside from the FL/Gear Worx Stations) stays dumb at every tier, not just Tier 1→2.

**Extends beyond evolution — operational behavior at fuel-empty (confirmed).** The same smart/dumb split governs how a structure behaves when it **runs out of fuel mid-operation**, not just how it evolves:
- **"Dumb" Machines** run the **standard Machine health/fuel system** (see Machine health and fuel system, above) — they don't know when to stop, so they **keep going unfueled** at a drastically reduced rate (1/10th normal progress) while their HP drains, until they hit 0 HP and stall outright.
- **"Smart" structures** (FL, Gear Worx Stations) **know when to stop** — no HP pool at all (same as the FL's own confirmed "no HP, fuel-required" model). When fuel runs out mid-process, a smart structure **halts immediately and preserves progress** (the FL's confirmed Stop-on-fuel-out behavior — see Core, Fuel/HP, above), surfacing the standard error/warning rather than grinding on. Applies to every fuel-driven Gear Worx Station process (Dock's refuel/repair/player-heal, Growth Chamber's tier-ups, Workbench's recharge/durability-repair) — all halt cleanly and resume once refueled, never degrade-and-drain like a dumb Machine.

**Standard Tier 2 evolution recipe template (dumb Machines, confirmed default):** most Tier 1→2 evolutions follow one consistent shape, rather than being invented per-machine:
1. A **Lava Bucket** occupies one of the machine's item slots.
2. One of the machine's tanks is **completely filled with Water**.
3. The assembly is **injected with Evolution Catalyst**.
4. **Everything is consumed** (bucket empties, water is used up, Catalyst is consumed) and the **evolved block spawns into the world** in the original's place.

This is a deliberate thematic echo of vanilla obsidian generation (lava + water) and lines up with the primer's existing **Tier 2 "obsidian carapace"** visual theme — not a coincidence, worth keeping in mind as future Tier 2 evolutions get designed, since it reinforces a theme that was already established independently. This is a **default template, not an absolute rule** (per the mod's existing "signature mechanic is a default, not a hard rule" convention) — an individual machine's evolution can deviate if it has a specific reason to.

**Tier 2 FL-native construction (new):** starting at **Tier 2**, the FL gains **build-from-scratch recipes** for these Machines too — extending the existing Tier 1 FL-native recipe formula (see FL-native machine recipes, below: keep the defining physical item, convert flesh ingredients to a discounted Protein Blend cost, keep the binding agent, drop the suture requirement) to Tier 2 Machines as they're designed. This is separate from a Tier 1 Machine's own forced-evolution path (Cauldron → Crucible via Catalyst) — the FL can now also **print a Tier 2 Machine directly**, not just evolve an existing Tier 1 one.

**Deferred discussion (not yet resolved) — a Tier 2 Inert Tumor variant?** Should there also be a **hand-crafted** route to a Tier 2 Machine — a Tier 2 Inert Tumor + Tier 2 implant recipes — as the hand-crafted counterpart to the FL-native construction above, the same way Tier 1 Machines have both a hand-crafted implant recipe and an FL-native one? Not designed yet; full note logged in `dermicraft-tools-notes.md` → Inert Tumor.

---

## Known machines

Every Machine here is physically born from an Inert Tumor block — recipe items sutured in, the tumor stitched closed, then injected (most likely with Primitive Catalyst) via Syringe. Full mechanic in `dermicraft-tools-notes.md`. **Drooling Cauldron, Masticator, Skin Tank, Effluentcer, Craw, and Metastasizer all have their tumor-genesis (implant) recipes written and live in code** — the Metastasizer's (the last outstanding one) is now implemented per the spec in its entry below. Machines still in concept stage (Gestator, Drooling Crucible, Filling Station) don't have one yet, for the obvious reason.

### Skin Tank

**Status:** Function and capacity defined.

**What it is:** A simple fluid storage tank — no processing function, just bulk storage.

**Capacity:** `10 buckets` (10,000 mB) — by far the largest fluid capacity logged so far (10x a Beaker; 10x Drinker/Sipper's internal tank).

**Implant recipe (live in code, `skin_tank_implant.json`):** 1 **Beaker** + 2 **Dense Muscle** + 2 **Nerve Cluster**, injected with **100 mB Primitive Catalyst** (sutured) — the standard implant shape.

**FL-native recipe:** 1 Beaker + 1500 mB Protein Blend + 100 mB Primitive Catalyst, FL-assembled (no suture) — derived via the standard FL-native machine recipe formula (includes the universal Inert Tumor cost, not just the listed flesh ingredients); full worked-out math lives with the other worked examples (see FL-native machine recipes, under the Core section).

**Transport (new, confirmed) — Forceps pickup with contents preserved.** By the mod's general "living block" rule, breaking a block the vanilla way destroys it and drops only its contents, and a plain Forceps pickup recovers the block itself but *still* drops its contents (see Forceps, `dermicraft-tools-notes.md`) — the **Craw** is the sole existing exception, opting into the `IPreserveContentsOnPickup` marker interface (see Craw's own entry below) that keeps its contents inside the recovered block item so it relocates fully loaded. **Skin Tank now opts into that same `IPreserveContentsOnPickup` behavior** — a Forceps pickup recovers the tank with its fluid contents intact, letting a player physically relocate a full tank (e.g. seeding a new base, moving Slurry reserves) without needing floor-network infrastructure at the destination. This makes Skin Tank the fluid-side counterpart to the Craw's item-side "relocate fully loaded" precedent — the same symmetry already implied by Skin Tank and Craw being described as fluid/item counterparts elsewhere in this doc. **Inherited by Chitin Tank** (see its own entry below) as part of the same family/evolution lineage — no separate design needed.

**Evolution:** Forced-evolves into the **Chitin Tank** (Tier 2 — see its own entry below) via the standard "dumb" Machines forced-evolution mechanic (Evolution Catalyst injection) — see Machine Evolution — Smart vs. Dumb, above. Same pattern as Drooling Cauldron → Drooling Crucible.

**Automation access (confirmed) — fuel only, any face.** Automation (hoppers/pipes/capability-based access) can interact with the tank from **any face**, but **only for fuel-type fluids** — Skin Tank's automated role is specifically as a bulk **fuel** reservoir/distribution buffer, not a generic any-fluid bulk tank for automation purposes. **Applies to the whole family** (Chitin Tank and future evolutions inherit this rule automatically).

### Chitin Tank

**Status:** Core identity, hazard capability, evolution recipe, and capacity all decided.

**What it is:** The **Tier 2 evolved form of the Skin Tank** — same family, same simple bulk-fluid-storage function, no processing. Name leans on chitin, the organic shell/carapace material, fitting the primer's Tier 2 "obsidian carapace" visual theme (a fit reinforced further by its own evolution recipe — see below). **Not** a single-use vessel specific to any one recipe — it's a general-purpose Tier 2 tank, usable anywhere a Tier 2 tank is called for. Its first confirmed mention was as the tank required for the FL's own Brain → Core evolution (holding Molten Redstone), but that's simply its first appearance, not its defining purpose.

**Capacity:** **20,000 mB** — a clean double of Skin Tank's 10,000 mB, per the mod's general multiplicative-growth convention (see `dermicraft-project-primer.md` → Working conventions). **Growth rule confirmed for this family:** capacity **doubles at every subsequent tier upgrade** beyond this one (i.e. a future Tier 3 tank in this lineage would be 40,000 mB, Tier 4 would be 80,000 mB, and so on) — a fixed ×2-per-tier rule specific to the Skin Tank/Chitin Tank family, not just a one-off starting multiplier.

**Hazard capability:** Follows the same **cumulative Tier hazard rule** as the rest of the mod (Floor tiers, etc.) — Tier 2 means it handles **Tier 1 safe fluids + Tier 2 Extreme Heat (lava)** together, not a narrow "just Molten Redstone" restriction.

**Evolution recipe (Skin Tank → Chitin Tank):** follows the **standard Tier 2 evolution template** (see above) exactly — Lava Bucket in an item slot, the tank completely filled with Water, injected with Evolution Catalyst, everything consumed, Chitin Tank spawns in its place.

**Transport:** Inherits Skin Tank's Forceps-pickup-with-contents-preserved behavior (see Skin Tank entry, above) — no separate design needed, same family/lineage.

**Automation access:** Inherits Skin Tank's family-wide rule — **fuel only, any face** (see Skin Tank entry, above). Note this only restricts *automated* (hopper/pipe) access — **manual player interaction is unrestricted**, so a Chitin Tank can still be hand-filled with non-fuel fluids like Molten Redstone (its confirmed use for the FL's Brain → Core evolution) via Beaker/bucket; only automation is limited to fuel-type fluids.

**Open questions:** None remaining for Chitin Tank itself.

### Drooling Cauldron

**Status:** Core function defined.

**What it is:** A water-generating machine with two production modes:
- **Passive generation:** continuously creates water at a fixed rate of `4 mB/second`, with no input required.
- **Food-boosted generation:** accepts food items as input to create additional water, with the amount produced scaled to that food's nutrition and saturation values (vanilla hunger/saturation stats) via the shared `IVagueRecipe` formula (`amount = round(25 × nutrition × (saturation + 1))`, time = `round(12 × nutrition × (saturation + 1))` ticks) — same nutrition/saturation-driven approach as the Masticator's food recipes.

**Balance note:** passive + food-boosted output (≈2.28 mB/tick while actively fed) is tuned to slightly exceed one Masticator's Water draw (≈2.17 mB/tick, 1:1 with its Crude Slurry output) when both are fed the same food, so a Cauldron can keep a Masticator supplied with Water with a small margin to spare.

**Evolution:** Forced-evolves into the **Drooling Crucible** (Tier 2, produces lava instead of water — see below) via the standard Machines evolution process. **As of 2026-08-19, gains a SECOND route to the same end state** -- see "Production Module" below.

**FL-native recipe:** 1 Cauldron + 1500 mB Protein Blend + 100 mB Primitive Catalyst, FL-assembled (no suture) — full worked-out math with the other worked examples (see FL-native machine recipes, under the Core section).

**Open questions:** Does passive generation require anything (placement conditions, etc.) or is it unconditional? Internal tank capacity? Tier restriction?

#### Evolution Module family (design decided 2026-08-19/20; core mechanic ✅ built 2026-08-21 for Drooling Cauldron, visual overlay still pending)

**Renamed from "Production Module" (2026-08-20) -- the fluid-switching behavior is Drooling Cauldron's own local expression of a broader mechanism, not what the mechanism fundamentally is.** What it actually is: a family of Modules, each **upgraded from an existing Hazard Module** (crafted using one as an ingredient, not the same item wearing two hats -- see `dermicraft-progression-notes.md`'s taxonomy for why sharing the base item was rejected), that drives **gradual, tick-driven, self-resolving evolution** toward a consumer's permanent Tier 2 -- machines for now, gadgets flagged as a likely future extension of the same pattern (not detailed yet).

**Additive to Safety Modules, not a replacement.** A player who just wants today's hazard tolerance keeps using a plain Safety Module exactly as already built -- nothing about that changes. An Evolution Module costs more (it's crafted from one) and does more: same immediate hazard union PLUS a path to making that tolerance permanent.

**Family, not one polymorphic item -- "gives each consumer what it needs," decided per consumer as each is designed:**
- **Drooling Cauldron:** no hazard concept to grant, so its Evolution Module is a **selector** -- e.g. a Lava Module decides which fluid the Cauldron currently produces, both passively and via food-boosted generation, switching output outright the moment it's installed (not gated behind any progress -- see below for what IS gated).
- **Masticator (and likely Effluentcer/Metastasizer -- not yet individually designed):** these already have Safety Modules doing real work, so their Evolution Module variant grants hazard tolerance the same way a Safety Module does, ADDITIONALLY accumulating toward a permanent Tier 2. **This is confirmed (2026-08-20) as the actual resolution to the Masticator's already-flagged blocking reachability gap** -- see that entry below: the entire Stage 2 "Molten" fluid family needs Lava/Extreme-Heat tolerance no Tier 1 Masticator has, and "lava tolerance via a gradual Evolution Module, resolving into a real Tier 2" is a complete answer to it, using a mechanism this doc already has to design for Cauldron anyway rather than inventing a second one.

**The rest of this subsection is Drooling Cauldron's own instantiation of the family** -- the general mechanism (upgraded-from-Hazard-Module, gradual, self-resolving) is shared, but these specific numbers/behaviors are the Cauldron's, not necessarily identical for Masticator's own eventual Evolution Module.

**Gradual evolution, not instant:** while a selector Module is installed, the Cauldron accumulates evolution progress. **Confirmed (2026-08-19): for every OTHER machine this pattern might extend to, progress only accumulates during genuinely active production -- it does NOT advance while idle.** The Cauldron is the one exception, and for a specific reason: it has no idle state at all, since passive generation runs unconditionally the moment it's placed (per the existing Cauldron entry above). So "as long as a Module is installed" and "for every tick it's actively producing" are the same condition for the Cauldron specifically, but the rule that actually generalizes to a future machine using this pattern is the active-production one, not "installed = ticking" -- a machine with a real idle state (no fuel, no recipe running) must NOT progress while idle, matching Masticator's `progress` already only advancing during a recipe. On reaching the threshold, the block transforms into an actual **Drooling Crucible** -- the identical end state Evolution Catalyst injection produces -- and the Module is **consumed**, matching the existing self-resolving convention Safety Modules already follow (a Module that locally covers a gap the permanent tier hasn't reached yet, consumed once the tier catches up).

**Removing the Module: production stops, then purges, then reverts -- NOT an instant revert.** Corrected 2026-08-19 from an earlier, wrong draft of this note that said output reverts to water immediately; it does not. Pulling the Module halts NEW production outright (no more lava generated), but whatever's already in the tank stays exactly as it is -- the Cauldron does not dump or convert it. Only once the tank is fully emptied (drained out by the player, piped out, however) does passive generation resume, now making the original fluid (water) again. So there's a real "dead" period where a Module-less Cauldron with lava still sitting in its tank produces nothing at all, by design -- this is presumably what prevents an instant mid-tank fluid swap/exploit, and reads as a real cost for pulling the Module rather than a free undo. Accumulated evolution progress is still wiped on removal (unchanged from the original design) -- reinstalling starts the tick counter over from zero regardless of tank state.

**Placeholder pacing (explicitly not tuned, pick a real number in-game before shipping):** 24000 ticks (one full Minecraft day of continuous operation) as the evolution threshold -- chosen only because it's a clean, legible unit, not because the real value has been worked out. Needs an actual playtested number before this ships.

**Scope -- corrected 2026-08-20, this reverses the previous note.** An earlier draft of this doc said Masticator/Effluentcer/Metastasizer explicitly would NOT get this pattern, Safety Modules alone being sufficient. That's now wrong: Evolution Modules DO extend to them, specifically because Lava/Extreme-Heat tolerance is what most Stage 2 fluids actually need (per the Masticator's own flagged gap above), and Safety Modules alone have no path to making that tolerance *permanent*. What's still true from the old note: Safety Modules remain the correct, sufficient, and unchanged mechanism for a player who isn't trying to evolve anything -- Evolution Modules are the pricier option layered on top for a player who is.

**✅ Core mechanic built (2026-08-21), Drooling Cauldron only:**
- **`EvolutionModuleProperties`** data map (`ModDataMaps.EVOLUTION_MODULE_PROPERTIES`), mirroring `SafetyModuleProperties`'s own kind-vs-data split: `targetFluid` (for a selector consumer), `hazards` (for a hazard-gated consumer, additive to that consumer's own Safety Module), `evolutionThreshold`. One record covers both consumer shapes -- which fields a given item populates is what decides its behavior, not a Java subtype. Heat Evolution Module populates both (`lava` / `EXTREME_HEAT`), since the same physical item is meant to serve either shape depending on which machine's Module slot it sits in -- per direction, this is also the intended mechanism for Masticator's own eventual variant, not something to invent twice.
- **`DroolingCauldronBlockEntity`**: `currentTargetFluid()`/`passiveYieldAmount()` now read the Module slot live each call (50% of normal rate while evolving, confirmed number, not a placeholder). `onPassiveFillResult` (a new hook on the shared base, called once per passive-fill cycle with how much actually got added) advances `evolutionProgress` only when real production happened that cycle -- confirmed: NOT during the halt-while-old-fluid-drains dead period, matching direction. `onModuleChanged` (another new shared-base hook) resets progress on any change to the slot.
- **The block-transform itself** -- first of its kind in this codebase, no Evolution Catalyst precedent existed to reuse. `onTickStart` (new shared-base hook, checked first thing every tick) performs the swap at the START of the tick AFTER threshold is reached, deliberately not synchronously from inside the fill-cycle logic, since replacing the block mid-tick would leave the rest of that same tick running against a now-stale instance. The transform clears this instance's own slots/tank BEFORE calling `level.setBlock` (`DroolingMachineBlock#onRemove` drops whatever's still there when the block changes, which would otherwise duplicate everything about to be handed to the new instance), then writes the captured ingredient/output items and tank contents into the freshly-constructed `DroolingCrucibleBlockEntity`. **Deliberately does NOT carry over recipe-in-progress bookkeeping** (active recipe/item, craft progress) -- that state is keyed to Cauldron's own `VAGUE_DROOLING_TYPE`, and blindly copying it into a Crucible whose recipe type differs would leave stale, type-mismatched data sitting in the new instance for no benefit. A half-finished food-boost cycle just restarts cleanly on the new block instead. This is a correction to the original assumption that unifying NBT tag keys would let a transform "read state and write it straight into the new instance" wholesale -- true for the tank/item slots, not true for recipe bookkeeping.
- Module slot filtering was deliberately left at the generic `MODULES` tag, not narrowed -- a non-Evolution Module (Aggregate Module, etc.) left in a Cauldron's slot just finds no data map entry and is inert, same as it would be doing nothing useful in any machine's Module slot it doesn't apply to.

**Still open:**
- **✅ Visual overlay built (2026-08-21).** A second render pass in `DroolingCauldronBlockEntityRenderer`, drawing a translucent red/orange (packed ARGB, alpha 90) tint over the Cauldron's own outer surface (0-1 bounds plus a tiny outset to avoid z-fighting), height-scaled by `DroolingCauldronBlockEntity#getEvolutionProgressFraction` (0 = not evolving at all, 1 = about to transform). Reuses lava's own still texture (animated, for a simmering look) rather than a flat color, and copies the exact pushPose/mulPose/translate transforms `drawDefaultTop`/`drawDefaultSides` already use for the fluid-pool cavity -- unchanged, only the quad's own bound literals differ (full 0-1 shell instead of the 0.1-0.9 inset cavity). Renders regardless of whether the tank currently has fluid (the halt-while-draining dead period still counts as "evolving"); Crucible never shows it, since it's already the end state.
- Masticator's own Evolution Module is confirmed in direction and now has a real mechanism to read from (the same data map), but isn't wired up yet -- what it's upgraded from, its threshold, whether Tier 2 Masticator gets a name/model/anything beyond "the Molten fluids now work." Effluentcer's and Metastasizer's are even further out -- "likely," not designed at all.
- No Evolution Catalyst (instant, non-Module) forced-evolution path exists -- only the gradual Module-driven one described here. Whether an instant path is still wanted alongside this, or whether the gradual path fully replaces that idea, isn't decided.
- Exact recipe/cost for any Evolution Module -- "crafted from a Hazard Module, priced higher" is the shape, not a number.

### Masticator

**Status:** Core function defined; recipe-yield logic established; output buffer capacity and a confirmed refusal behavior now set. Full per-fluid recipe detail lives in the companion doc: `dermicraft-crafting-notes.md`.

**What it is:** A blending machine — takes one fluid input and one item input, combines them into a different output fluid. Confirmed as the machine behind all current Crafting fluids: the original six (Carbon Blend, Calcium Blend, Protein Blend, Ferrous Blend, Cuprous Blend, Aurous Blend) plus the newer Sediment Blends (Stone Blend, Silica Blend, Clay Blend) and the Blood Nugget → Ferrous Blend alternate route — see `dermicraft-crafting-notes.md` for full detail on all of these.

**Recipe yield logic:**
- Most recipes produce a **set/fixed amount** of output fluid.
- **Food-based recipes** are the exception — confirmed so far for **Crude Slurry** (plant-based food items only) and **Protein Blend** (any `MEAT_FOOD`-tagged item) — where the output amount is instead derived from the input food item's nutrition and saturation values, the same formula-driven approach as the Drooling Cauldron's food-to-water conversion.

**Base fluid pattern — corrected:** Water is the fluid input for Crude Slurry, Carbon Blend, Calcium Blend, and Protein Blend, but this is **not a universal rule** — Ferrous/Cuprous/Aurous Blend use Primitive Catalyst as their base instead, and the Sediment Blends (Stone/Silica/Clay) use Water (see `dermicraft-crafting-notes.md`). Don't assume water as a default for future Masticator recipes without checking case by case.

**Output buffer (Tier 1):** `5000 mB` (5 buckets).

**Confirmed behavior — output buffer check:** If a recipe's output would exceed the room currently available in the output buffer, the Masticator **will not process the recipe at all** — it simply refuses to start, the same "won't even attempt it" pattern Drinker uses for hazardous fluids, rather than processing and failing or wasting the input partway through. This is currently blocking one real recipe (Cuprous Ore, 7500 mB — see `dermicraft-crafting-notes.md`) until an evolved Masticator with a larger buffer exists.

**FL-native recipe:** 2 Bone + 1310 mB Protein Blend + 100 mB Primitive Catalyst, FL-assembled (no suture) — full worked-out math with the other worked examples (see FL-native machine recipes, under the Core section).

**Reachability gap (flagged, blocking) — no Tier 2 Masticator exists yet, name undecided.** The Tier 1 Masticator's fluid tanks are `VulnerableTank` (`HazardProfile.TIER_1` — rejects Lava/Extreme-Heat), so **the entire Stage 2 "Molten" fluid family is currently unreachable** — none of Molten Redstone, Molten Quartz, Molten Glowstone, etc., or Molten Soul Silica can actually be made in-game until an evolved Masticator with at least `TIER_2` hazard tolerance exists (presumably via the same forced-evolution/Evolution Catalyst pattern as Skin Tank → Chitin Tank — see that entry above for the template). Not a bug, just sequencing: don't build/register any Molten-family fluid's recipe before this evolution exists, or a survival player will have a fluid with no way to produce it. See [[feedback_survival_reachability_check]] in Claude Code memory. **Confirmed resolution path (2026-08-20, not designed in detail):** Masticator's own Evolution Module — see the Drooling Cauldron entry above's "Evolution Module family" subsection — grants Lava/Extreme-Heat tolerance the same way its existing Safety Module would, additionally accumulating toward a permanent Tier 2 that resolves this gap for good. Not yet designed beyond that direction (no threshold, no crafting cost, no confirmation of what "Tier 2 Masticator" is named or looks like) — still blocking until it's actually built.

**Open questions:** Exact item lists are now set for Carbon/Calcium/Protein/Metal Blends and the Sediment Blends (see companion doc) — remaining open items live there, including a flagged intent to rework the original six fluids' yields once the Sediment Blends' numbers are set. Internal fluid-input capacity (separate from the output buffer)? Tier restriction beyond the confirmed Tier 1 metal-grinding capability? **Tier 2 Masticator** — name, evolution recipe, and buffer size all undecided (also the confirmed unblock for the Cuprous Ore 7500 mB output-buffer issue noted above).

**ToDo:** Add support for processing **multiples of one item per cycle** — currently strictly one item per process. Blocked on this: a clean single-cycle "3 Bone Meal → 990 mB Calcium Blend" batch recipe (see `dermicraft-crafting-notes.md`), and likely other future batch-style recipes.

### Effluentcer

**Status:** Operational. Built exactly as specified below — block (`EffluentcerBlock`), block entity (`EffluentcerBlockEntity`), menu (`EffluentcerMenu`), and screen (`EffluentcerScreen`) all implemented, F-Stuff and C-Stuff exist as registered fluids with working recipes, all three Effluencing recipes are live, and the `effluentcer_implant` tumor-genesis recipe is written (see Known machines intro / primer Machines → Origin). The "Finalized design" section below is now an as-built description, not a forward-looking spec. C-Stuff's standalone Syringe use on Creepers is also live — see `dermicraft-tools-notes.md`.

**What it is:** A machine that mixes two fluids into one output fluid — distinct from the Masticator (fluid + item), this one takes two fluid inputs.

**Known recipes (all three implemented and confirmed in-game):**
- **F-Stuff** — Crude Slurry (250 mB) + Protein Blend (250 mB) → F-Stuff (500 mB). The "F" stands for food, fitting given the plant-life (Crude Slurry) + meat (Protein Blend) combination. Confirmed secondary use: a Filled F-Stuff Bucket smelts (200 ticks) or smokes (100 ticks) into a food item called **MRE** (real-world ration reference; nutrition 6 / saturation modifier 0.6, matched to cooked chicken), both at 0.35 XP — mirroring vanilla's raw beef/chicken cook times. The Metastasizer can also produce MRE directly from F-Stuff (900 mB), using an existing MRE as the non-consumed duplication pattern — furnace/smoker cooking is the required bootstrap step to get the player's first MRE before the Metastasizer route becomes available (see the Metastasizer entry below, and the primer's "prefer Dermicraft-native processes" convention). **Meat Flavored Meat** is a simpler parallel food item (nutrition 5 / saturation modifier 0.5) using the exact same smelt/smoke/Metastasizer trio, but built from **Protein Blend** directly instead of F-Stuff.
- **C-Stuff** — Carbon Blend (250 mB) + Calcium Blend (250 mB) → C-Stuff (500 mB). Follows the same "[Letter]-Stuff" naming pattern as F-Stuff. **Now has its own standalone secondary use beyond being a Primitive Catalyst ingredient:** a C-Stuff Syringe injected into a Creeper disrupts its explosion-trigger process for a few seconds (it still swells/hisses normally, just can't detonate), repeatable with a cooldown — see `dermicraft-tools-notes.md` Syringe entry for the full mechanic and reasoning (Calcium as binding agent + Carbon as purifying agent, without Primitive Catalyst's unrelated life-coded ingredients). This resolves the "every fluid should have a use beyond a single recipe" convention for C-Stuff, which previously only fed the Primitive Catalyst shortcut below.
- **F-Stuff (500 mB) + C-Stuff (500 mB) → Primitive Catalyst (750 mB).** A third, confirmed Effluentcer recipe, fixed at 900 ticks — together, F-Stuff and C-Stuff cover all four of Primitive Catalyst's original puddle ingredients (plant base, meat, bone, coal), just pre-combined into two Blends instead of four raw items. A fully fluid-based alternate route to Primitive Catalyst, parallel to (not replacing) the original puddle-crafting recipe — see `dermicraft-catalyst-notes.md`.

**Craft-time note:** F-Stuff and C-Stuff both use the `EffluencingRecipe` dynamic-timing mode (`ticks: -120` in their recipe JSON) — a negative value means "ticks per 100 mB of output" rather than a fixed duration, since the Effluentcer has no item ingredient to scale timing off (unlike the Masticator's food-based recipes). At their current 500 mB yield that works out to 600 ticks each. The Primitive Catalyst recipe uses a fixed positive value (900 ticks) instead.

**As-built design:** A structural clone of the Masticator, with the item-ingredient slot (and its food/`IVagueRecipe` logic) removed and a second fluid input added. Standard 4-part machine pattern: Block (`EffluentcerBlock`, keeps a `FACING` property purely for automation-face mapping — side texture is uniform) + Block entity (`EffluentcerBlockEntity` extends `MachineBaseBlockEntity`) + Menu (`EffluentcerMenu`) + Screen (`EffluentcerScreen`).

- **Tanks (4):** `FUEL` (`FuelTank` — required, since only `FuelTank` carries the biofuel validator + speed/heal/use-rate getters the health system reads), plus `INPUT_A`, `INPUT_B`, and `RESULT` (all `VulnerableTank`, which blocks `HAZARDOUS`-tagged fluids = the Tier-1 lava guard). All 5000 mB. `RESULT` is the Tier-1 output buffer and uses the Masticator's refuse-if-output-won't-fit check (won't start a recipe whose output can't fit).
- **Face mapping** (`getTank(direction)` for pipe/automation access): `UP` → fuel, `DOWN` → result, front + back → input A, left + right → input B.
- **Health/fuel:** identical to the Masticator (200 max HP, hunger rate 1 HP/cycle while unfueled+processing, unfueled 0.1× flat speed, recovery 0.1× while healing). Consistent with the fuel-machine-health standard.
- **Recipe type:** new `EffluencingRecipe` (+ nested `Serializer`) driven by a new `TwoFluidRecipeInput` record. Fields: `fluidA, amountA, fluidB, amountB, result, resultAmount, ticks`. All amounts/yields come from the recipe (no food-based scaling — there's no food item). **`ticks` negative ⇒ dynamic, scaled on the result fluid amount** (bigger batch → longer); positive ⇒ used directly. **Matching is order-independent** — A+B matches regardless of which input tank holds which fluid.
- **Item slots (4, one per tank):** main purpose is inserting/removing fluids via fluid-handler items (Beaker, Flask, Syringe, bucket). No item-type restriction (no `isItemValid` filter); a non-fluid-handler item is inert automatically because every transfer helper no-ops when the item has no fluid-handler capability. Direction is auto-chosen: a filled container drains into the tank, an empty container fills from the tank.
  - **Automation** (directional capability wrapper) caps each fluid-handler slot at 1 item, so it can never pile a stack there — it feeds one container at a time and pulls the filled/emptied result out. Effectively inert to stacks.
  - **GUI stacks** (>1 items — by definition player-placed, since automation can't form them): **fill-one-and-eject**. Fill a single container, keep the remaining empties in the slot, and eject the filled one; `onContentsChanged` re-fires until one filled container remains. **Eject target: into the interacting player's inventory if there's room, else drop at the player's feet** (`player.getInventory().add(...)`, fallback `player.drop(...)`). The BE gets the player reference from the open `EffluentcerMenu`. NOTE: ejection is only needed in the fill-empties-from-tank direction (result slot normally; also a fuel/input slot if the player pulls fluid back out of it) — draining a stack of *filled* containers restacks the resulting empties for free, no eject.
- **Textures:** `effluentcer_side` on all four sides, `skin_tank_end` on top/bottom (block model like the Masticator's).
- **Screen (GUI) layout:** `EffluentcerScreen extends AbstractModScreen<EffluentcerMenu>`, composited from the shared `textures/gui/screen_parts/` textures (no bespoke per-machine GUI PNG) — follows the Masticator screen pattern (see [[feedback_machine_gui_composited_layout]]). Base = `screen_background.png` (256-canvas). Four `tank_and_slot` columns (fuel, input A, input B, result), each with a `createFluidRenderer16x40` gauge and a menu `Slot` whose x tracks the column's blit-x (slot y stays 59, tank blit y = 11). HP bar (`hp_background` + green/yellow/red bottom-up crop) with a % tooltip. **Arrangement = "inline pair":** HP pinned to the far-left edge (`x+8`) and fuel mirrored to the far-right edge (`x+150`); the two **ingredient tanks (A + B) sit together left of center**, a **single progress arrow (`arrow_background`/`arrow_fulll`) sits right of center and shows progress**, with the result tank beyond it. Approximate starting coords (tune live in `runClient`): HP `x+8`, input A `x+40`, input B `x+58`, arrow `x+84 y+38`, result `x+112`, fuel `x+150` (all tanks blit at `y+11`). Four `renderFluidTooltipArea` calls + the HP hover tooltip in `renderLabels`. Watch the blit-256 gotcha: `screen_background` uses the implicit-256 overload; every tight part texture uses the explicit-size overload.
- **Registration/datagen touchpoints** (all following existing patterns): `ModRecipes` (serializer + type), `ModBlocks`, `ModBlockEntities`, `ModMenuTypes`, `ModBusEvents` (ItemHandler + FluidHandler caps), `DermicraftClient.registerScreens`, `ModCreativeModeTabs`, `ModBlockStateProvider`, `ModBlockTagProvider`; block uses `noLootTable()` (drops via BE); run `runData` and commit `src/generated/resources`.

**Formerly deferred, now resolved:**
- **F-Stuff** and **C-Stuff** fluids + the **MRE** item are all implemented; all three Effluencing recipes are wired and working (see "Known recipes" above for exact mB amounts and tick times).
- **`effluentcer_implant`** tumor-genesis recipe is written: 3× Glass Flask + 2× Dense Muscle + 2× Nerve Cluster (sutured), stitched, then injected with 100 mB Primitive Catalyst.
- GUI art was never a dependency — the screen composites from the existing shared `screen_parts/` textures, and the inline-pair layout needed no new/mirrored arrow texture.

**FL-native recipe:** 3 Glass Flask + 1500 mB Protein Blend + 100 mB Primitive Catalyst, FL-assembled (no suture) — derived via the standard FL-native machine recipe formula; full worked-out math lives with the other worked examples (see FL-native machine recipes, under the Core section).

**Still open:** A dedicated balance pass on the real recipe numbers (mB in/out per recipe) hasn't happened yet — current amounts are functional, not confirmed final.

**Open questions:** Are more result fluids planned beyond these three? A proposed reverse machine — one fluid splitting back into two — has been flagged as a concept only, not detailed; likely scoped to reversing Effluentcer's own outputs specifically (F-Stuff/C-Stuff) rather than a generic "unmixer," and likely lossy rather than a perfect 1:1 reverse if it's ever built.

### Upgraded Effluentcer (name not yet decided)

**Status:** Concept stage — required by a specific recipe, not yet designed as a machine in its own right.

**What it is:** A Tier/evolution step up from the base Effluentcer, adding a **third fluid input tank** (base Effluentcer has only `INPUT_A`/`INPUT_B` plus fuel and result). Directly required by the **Living Catalyst** recipe (Dragon's Milk + Molten Quartz + Blaze Essence — see `dermicraft-catalyst-notes.md`), which needs three simultaneous fluid inputs the base machine can't support.

**Likely path:** Reached via the Effluentcer's own forced-evolution process (matches the established Drooling Cauldron → Drooling Crucible precedent) rather than being a separately-crafted machine, though this isn't confirmed yet.

**Open questions:** Name. Evolution trigger/recipe (what items/fluids stored over time, plus Evolution Catalyst injection, per the standard evolution pattern). Whether it gains any other capability beyond the third input tank. Tank capacity/face-mapping changes, if any, relative to base Effluentcer.

### Metastasizer

**Status:** **Operational.** Block (`MetastasizerBlock`), block entity (`MetastasizerBlockEntity`), menu, and screen are all implemented, and a wide roster of duplication recipes is live across several ingredient families. Works on a recipe-by-recipe basis. Now fully distinct from **Gestator**, which has been reassigned as the name of a new, separate machine (see its own entry below) — "Metastasizer" is the sole name for this machine going forward, no dual-naming ambiguity remaining.

**What it is:** A machine that takes a fluid + an item and produces a copy of that item. The item acts as a non-consumed **pattern** — only the fluid is spent. Used for making simple items (duplication-based, rather than an ingredient-based recipe).

**Conceptual framing:** essentially a **small 3D printer** — it scans a pattern item and prints copies from fluid "ink." This framing is also the root of the FL's "printing vs combining" recipe method (see FL — Brain FL-only recipes), and it's why the FL can run it **pattern-free** (it conjures a transient pattern to scan; see the FL's Metastasizer invocation rule).

**Recipe (implant) — now live in code:** 1× **Beaker** + 2× **Dense Muscle** + 4× **Nerve Cluster** + 1× **Eye**, sutured with the **Suture Kit** → injected with **100 mB Primitive Catalyst**. Deliberately pricier than the other machine implants (8 items vs. ~5) to reflect its powerful duplication ability. The Eye fits the 3D-printer framing (the scanner that reads the pattern). This was the last built machine without an implant recipe; the gap is now closed.

**Confirmed — works on a recipe-by-recipe basis.** Not a generic "any fluid + any item" machine — each valid fluid+item combination needs its own defined recipe, the same granularity the Masticator already uses for its fluid+item pairs. This resolves the earlier open question of whether any fluid would work; it doesn't.

**Confirmed — "produces a duplicate of the pattern item" is the primary/default behavior, not a hard rule.** It's the Metastasizer's main, intended use case, and every recipe implemented so far follows it (the once-planned Blood Nugget recipe, whose output would *not* be a duplicate, remains an unimplemented concept — see below).

**Confirmed — Sediment Blend duplication, costs now set.** Stone Blend, Silica Blend, and Clay Blend each have their own per-item Metastasizer recipes duplicating every block on that fluid's roster (17 items total across all three families — see `dermicraft-crafting-notes.md` Sediment Blends section for the full tier/cost table: 750/900/1000/250 mB by tier, 6s/8s/10s/2.5s craft time).

**Confirmed — Metal Blends reverse route.** Ferrous/Cuprous/Aurous Blend can now be turned back into their Ingot/Nugget-tier item via the Metastasizer, mirroring the Masticator's forward recipes 1:1 on fluid amount (1000 mB per Ingot, 110 mB per Nugget). No reverse recipe for Raw (never a 1:1 conversion) or Cuprous Nugget (no vanilla item). See `dermicraft-crafting-notes.md` Metal Blends section.

**Confirmed — MRE and Meat Flavored Meat duplication.** F-Stuff (900 mB, consumed) + an existing **MRE** (non-consumed pattern) → more MRE, and the same setup for **Meat Flavored Meat** using **Protein Blend** (900 mB) instead of F-Stuff — a new, simpler food item (nutrition 5 / saturation modifier 0.5) added alongside MRE (nutrition 6 / saturation modifier 0.6, matched to cooked chicken). Both foods also have a furnace/smoker bootstrap recipe (1 Filled Bucket of the relevant fluid → 1 food item, 200/100 ticks, 0.35 XP — matching vanilla raw beef/chicken cook times) to get the player's first copy before the Metastasizer loop takes over.

**Confirmed — Protein Blend → tumor/part duplication.** Protein Blend also duplicates **Inert Tumor** (1000 mB, solid tier), **Marred Tumor** (900 mB), and the three tumor-drop parts **Dense Muscle, Nerve Cluster, Eye** (250 mB each, light tier) — reusing the same tier/craft-time convention as the Sediment Blends. (As with all Metastasizer recipes, the FL can run these **pattern-free** via a transient conjured pattern — see the FL's Metastasizer invocation rule.)

**Built 2026-07-19 — meat duplication, one recipe per `MEAT_FOOD`-tag item.** The Masticator's own Protein Blend recipe is vague/tag-based (nutrition-scaled, covers the whole `MEAT_FOOD` tag generically — see `dermicraft-crafting-notes.md`), so unlike the Sediment/Metal/Carbon Blends there was no per-item forward cost to mirror on the Metastasizer side. Instead, each of the 11 `MEAT_FOOD` items (Beef, Porkchop, Chicken, Rabbit, Mutton, their 5 cooked forms, and Rotten Flesh) got its own duplication recipe costed by plugging that item's real vanilla nutrition/saturation into the same vague formula the Masticator uses (`IVagueRecipe`: mB = round(25 × 2.6 × nutrition × (saturation + 1))), rather than a flat or tiered guess — so duplicating a specific meat costs exactly what masticating it would have produced. Cross-checked against the existing `metastasizing_bread` recipe (Crude Slurry family, same formula shape), which the calculation reproduces exactly (520 mB). Yields: Beef/Porkchop/Rabbit 254 mB, Chicken/Mutton 169 mB, Cooked Beef/Porkchop 936 mB, Cooked Chicken 624 mB, Cooked Rabbit 520 mB, Cooked Mutton 702 mB, Rotten Flesh 286 mB.

**Confirmed — Leather duplication via Protein Blend.** Protein Blend (consumed) + Leather (non-consumed pattern) → Leather — leather is processed hide, i.e. structural protein (collagen), same reagent logic as the Cobweb/fibroin recipe below. Steady-state half of the bootstrap → duplication loop with the Mutator's Rotten Flesh + Calcium Blend → Leather recipe (see the Mutator entry). **Cost TBD, needs a deliberate pricing pass** — leather is mid-value (books, armor, frames), so it should land above the light tier (250 mB) parts; somewhere near the MRE/solid range (900–1000 mB) is the working guess.

**Confirmed — Cobweb duplication via Protein Blend.** Protein Blend (consumed) + Cobweb (non-consumed pattern) → Cobweb, at the **light tier** (250 mB, 50 ticks) — spider silk is literally protein (fibroin), same reagent logic as the Mutator's String + Protein Blend → Cobweb bootstrap recipe (see the Mutator entry). The two form the established **bootstrap → duplication loop** pattern (like MRE): Mutator spins the first cobweb from String (or shear one from a mineshaft), Metastasizer duplicates it thereafter. **Pricing note:** both routes are ultimately "Protein Blend → silk mass" and inherit the vanilla 1 Cobweb → 9 String decomposition, so set the two costs together — duplication as the cheaper steady-state, the Mutator route as the bootstrap.

**Confirmed — Bone/Bone Meal duplication.** Calcium Blend duplicates **Bone** (1000 mB, mirroring the Masticator's Bone → Calcium Blend recipe) and **Bone Meal** (330 mB, mirroring the Masticator's Bone Meal recipe) — same "reverse the Masticator recipe that consumes this item" pattern as the Metal Blends above.

**Confirmed and now actually implemented — glass family via Silica Blend and Calcium Blend, sized by volume.** Was wrongly marked "Confirmed" for a long time while never actually being datagenned — caught 2026-07-16 while building the Mutator's dyed-glass recipes, and built in the same session (`metastasizing_glass_block`, `metastasizing_beaker_silica/_calcium`, `metastasizing_glass_flask_silica/_calcium`, `metastasizing_glass_pane`, `metastasizing_calcium_glass`, all in `ModRecipeProvider.java`). Real glass is made from both silica (sand) and lime (calcium) — so glass items get **parallel duplication routes** through either fluid, not just one:

| Item | Silica Blend route | Calcium Blend route |
|---|---|---|
| **Glass Block** | 1000 mB (the reference unit) | — |
| **Beaker** | 1000 mB | **1000 mB (new)** |
| **Glass Flask** | 250 mB — a quarter of the Beaker's cost, matching its **quarter volume relative to the Beaker** | **250 mB (new)** |
| **Glass Pane** | **500 mB (new)** | — |
| **Calcium Glass** | — | **1000 mB (new)** — Calcium Glass's own duplication route, fitting since it's already a calcium-based glass item (see its existing puddle recipe) |

All routes are **additive** — the existing vanilla-style crafting-table recipes (glass block smelting, Beaker's 3-glass recipe, Flask's 4-glass chalice-pattern recipe, Calcium Glass's puddle recipe) all stay, and Beaker/Flask having *two* valid fluid routes each is intentional flexibility, not a conflict (Metastasizer already supports multiple recipes per output item). Load-bearing for the FL: the Beaker and Flask were previously crafting-table-only (which the FL can't do), so this makes both **FL-sourceable** (Metastasizer, pattern-free) — unblocking FL-native machine recipes that need one (Metastasizer's own recipe and Skin Tank need a Beaker; Effluentcer needs Glass Flasks).

**Confirmed — plain Terracotta via Clay Blend.** Clay Blend duplicates plain (uncolored) **Terracotta** (Clay Blend + Terracotta pattern → Terracotta), the ceramics parallel to the glass family above — Clay Blend's identity is ceramics, and Terracotta already has a recycling recipe back into Clay Blend, so this closes that loop the same way. Sized at the Sediment solid tier: **~1000 mB, 200 ticks (10s)**, mirroring the Terracotta → Clay Blend recycling rate. This is the plain-form counterpart to the Mutator's Clay Blend glazing/coloring family (Dye + Clay Blend → Colored/Glazed Terracotta — see the Mutator entry): **Metastasizer duplicates plain Terracotta, Mutator transforms it into colored/glazed.** (Note: unlike glass, this recipe is *new* — the Clay Blend roster was previously just Clay.)

**Confirmed and built 2026-07-19 — Cauldron via Ferrous Blend.** Silica Blend duplicates the **vanilla Cauldron**'s glass-based cousins above, but the Cauldron itself is metal — Ferrous Blend duplicates it instead, at **7000 mB / 1400 ticks (70s)**, following the existing Ferrous Blend↔Ingot reverse rate (1000 mB per Ingot) literally against the Cauldron's real vanilla recipe cost of 7 Iron Ingots. Steep, but deliberately consistent with the established rate rather than a discounted one-off. Unblocks the Drooling Cauldron's FL-native recipe. (This entry had been marked "Confirmed" for a while without ever actually being datagenned — same gap pattern as the glass family above; caught and fixed in the same pass as the Chest correction below.)

**Stale — Chest does have a Metastasizer recipe.** This entry previously claimed Chest was deliberately excluded from the Metastasizer, citing "no obvious fluid identity." That's wrong: Chest is species-agnostic wood furniture, same as Crafting Table, and has a real `metastasizing_chest` recipe (Pulp Blend, 2000 mB — 8 Planks' worth, mirroring the vanilla 8-plank Chest recipe — see `dermicraft-crafting-notes.md` Pulp Blend section). Corrected 2026-07-19; the Craw's FL-native recipe note elsewhere in this doc that assumed Chest was hand-supplied-only needs re-checking against this.

**New family, built 2026-07-19 — Dye duplication, split across fluids by real source material.** Unlike every other Metastasizer family, the 16 vanilla dyes don't share one material identity, so this deliberately doesn't force them onto a single fluid:
- **White** → Calcium Blend, mirroring the vanilla Bone Meal → White Dye craft (Calcium Blend already duplicates Bone Meal itself).
- **Black** → Carbon Blend (soot/charcoal identity).
- **Gray** → Stone Blend (literal stone-gray).
- **Light Gray** → **C-Stuff** (Carbon Blend + Calcium Blend, via the Effluentcer) — reads as "Gray diluted by White," matching the fluid's own composition.
- **Every other color, including Purple** → Crude Slurry (living plant material) — the flower-derived majority; Purple has no single vanilla flower source, but purple flowers are common enough in reality (lilac, violet, lavender) to fit the same bucket.

All 16 share flat pricing — **100 mB / 50 ticks**, the same cheap/decorative bracket as Leaves — regardless of which fluid, since every route here is just as trivially renewable as a flower or a bonemeal application. Metastasizer-only (one-directional, like Leaves) — no Masticator forward recipe, since none of these fluids are literally "made from" a dye the way Bone→Calcium Blend or Log→Pulp Blend are.

**New, built 2026-07-19 — Wooden Pressure Plate and Button, added to the Pulp Blend wood family.** Same per-species mirrored treatment as the rest of the family (`woodSpecies` loop, all 8 species) plus the matching Masticator recycling recipes (`ItemTags.WOODEN_PRESSURE_PLATES`/`WOODEN_BUTTONS`, tag-based like Planks/Slabs/etc.): Pressure Plate costs Door's own rate (500 mB / 100 ticks — both are a 2-Plank item), Button costs exactly one Plank's worth (250 mB / 50 ticks, no further processing beyond the Plank itself).

**Still a concept, not yet implemented — Blood Nugget:** Protein Blend (consumed) + an **Iron Nugget** (non-consumed pattern) → **Blood Nugget**, feeding into a Masticator recipe (Blood Nugget + Primitive Catalyst → Ferrous Blend). This remains the one designed-but-unimplemented recipe on this machine, and would be the first real example of the Metastasizer's output *not* being a duplicate of its pattern item — no longer blocked on the machine existing, just on the Blood Nugget item/recipe itself being built. See `dermicraft-crafting-notes.md` for the full chain and "low but fair" yield reasoning.

**Open questions:** What counts as a valid pattern item beyond "simple item" now that Blood Nugget (if/when built) would show the output doesn't have to be a duplicate at all — is there still any restriction on pattern items, or is it purely whatever a defined recipe specifies?

### Gestator

**Status:** Core mechanic decided, including fuel model. A new, distinct machine — not a rename of the Metastasizer above (which previously carried this name before it was reassigned here).

**What it is:** Takes **Living Catalyst** (renamed from "Life Catalyst" — see `dermicraft-catalyst-notes.md`) and one other fluid as inputs. Behavior branches on whether that second fluid is living. **Confirmed live examples:** Molten Glowstone → Living Glowstone, Molten Netherite → Living Netherite (see `dermicraft-crafting-notes.md`).
- **Non-living second fluid, with a living counterpart that exists:** the two mix to produce the **living version** of that fluid.
- **Living second fluid:** produces a **large batch of that same living fluid** — framed as a visual/functional illusion of a greatly accelerated replication rate, not an actual permanent change to that fluid's own inherent replication speed.

**Invalid input handling — deliberate deviation from the usual fail-safe refusal pattern.** If the second fluid has no living counterpart, the Gestator does **not** refuse outright the way Drinker/Masticator do. Instead the fluid simply **pools inside the machine** and must be manually extracted — either via a dedicated Gadget built specifically to make this easy, or via ordinary automation (pumping it back out). This is a softer version of the mod's fail-safe philosophy: bad input doesn't process, but it's recoverable rather than instantly rejected.

**Fuel — revised (supersedes the tube-driven/fuel-optional draft below).** The **mixing step** (Living Catalyst + fluid → Living fluid) is fueled the same way as every other fuel/HP machine — standard Slurry fuel + machine-health model (see Machine health and fuel system, and [[feedback_fuel_machine_health_standard]]), no special-case tube mechanic. Deliberately not made "extra painful" unfueled beyond the normal baseline — the player already has a separate management burden from the containment/decay rule above (deciding whether to keep mixing more vs. losing what they have to decay), so the mixing fuel cost doesn't need to carry additional friction on top of that. If the Gestator ends up over- or under-tuned in practice, adjust via raw fuel cost, not via a bespoke mechanic.
- **Passive replication fuel bonus:** while fueled, the Gestator's contained Living fluid gets a **growth-speed bonus** to its passive self-replication (see above) on top of the baseline rate. This is a pure bonus, not a requirement — passive replication happens with or without fuel, fuel just speeds it up. No downside/tradeoff attached to keeping it fueled long-term; if this ends up too strong once in-game, the fix is raising fuel cost, not inventing an offsetting cost.

**Superseded — old tube-driven-cycle draft, kept for history:** ~~The Gestator runs without Slurry fuel, operating instead on an internal tube-driven timing cycle — a slow passive pulse rather than continuous fuel draw. Introducing Slurry speeds the process up, but isn't required.~~ Replaced by the standard fuel/HP model above; the "tube mechanic" concept is dropped along with it (see removed open question).

**Open questions:** Exact passive-replication growth curve (linear vs. diminishing returns) and its fueled-vs-unfueled rate values. Full roster of which fluids have a living counterpart (currently assumed universal — "every fluid can have one" — with the pooling behavior above as the safety net for any that don't). Whether the Gestator itself will ever get a tumor-genesis recipe like other Machines, or whether it's built differently given its unusual mechanic.

**Living-fluid containment rule (confirmed — new, changes the Gestator's role from "producer" to "life support"):** A Living fluid is not a stable, stockpile-and-ship resource like the rest of the mod's fluids. It only stays living while held in a container that actively **supports** it — currently the Gestator itself, or a **partner tank**, defined as **any tank already capable of holding Living fluids** adjacent to the Gestator's output face. This is a capability check, not a positional/"any tank in that slot" rule — an ordinary tank that isn't rated to hold Living fluid at all wouldn't count just by being adjacent. **Confirmed:** the gate is Biohazard tolerance via the existing Hazard Profile system (`dermicraft-hazard-effects-notes.md`) — a tank qualifies as a partner tank if and only if its Hazard Profile already tolerates Biohazard, no new capability system needed. (Other specialized containers may be added later as dedicated life-support options, but nothing beyond the Gestator/partner tank exists yet.) Every other container — buckets, other machines' tanks, Innards Duct/Node/Gate infrastructure while fluid is stationary in it — does **not** support life; once a Living fluid enters one, a decay timer starts, and on expiry it reverts to its non-living counterpart in place.
- **Buckets are on the timer too, not an instant revert.** A player can carry a bucket of Living fluid for a while before it dies — it's not a hard ban on manual transport, just a race against the clock, consistent with buckets being "just another non-supporting container" rather than a special case.
- **Ducts, Gates, and Nodes pause the timer** while fluid is actively moving through the transport network — for now. (Decay starting *inside* a Node specifically is flagged as a good future addition — thematically a Node idling a batch of Living fluid mid-route should probably cost something — but no clean implementation exists yet, so transport network transit is fully timer-safe for the time being.)
- **Creative-mode player inventory is exempt** — a Living fluid held in a creative inventory does not decay. (This is a testing/design-mode carve-out, not a survival mechanic.)
- **Poured on the ground, Living fluid decays faster** than the container timer, but not instantly — slow enough that a player can wander into a puddle of it before it dies, which matters given the Biohazard tag below.
- **All Living fluids carry the Biohazard hazard tag**, universally, regardless of what their non-living counterpart's hazard profile is. This is intrinsic to being "alive," not inherited per-recipe. See `dermicraft-hazard-effects-notes.md` for Biohazard's effect design (infection-style, no H.E.A.T., deterioration-based).
- **Living Catalyst is a recipe ingredient only, not fuel.** The Gestator does not run *on* Living Catalyst — it consumes it once per creation recipe (Molten Glowstone + Living Catalyst → Living Glowstone, etc.), same as any other machine's recipe input. It is not spent to power the machine's fuel/HP cycle itself.
- **Passive self-replication while contained (confirmed, new mechanic — distinct from the recipe-time "large batch" behavior above, though it can also happen inside a Gestator):** any Living fluid sitting in a valid vessel (Gestator or partner tank) **with spare room** self-replicates over time on its own, growing its own volume for free without further Living Catalyst input. **Scales with volume** — a larger existing quantity replicates faster/produces more per tick than a small one (exact curve not yet set: linear vs. diminishing-returns not decided). This is a standing passive property of any contained Living fluid, separate from — and not the explanation for — the recipe-time "Living second fluid → large batch" effect described earlier, which remains its own one-off processing-step behavior.
- **Open design tension (not yet resolved):** because almost nothing except the Gestator/partner tank stops the clock, Living fluids function as a *perishable, use-near-the-source* resource rather than something you stockpile and ship across the base like other fluids — recipes and Gadgets that consume Living fluid should probably be designed to sit near the Gestator, or accept that cross-base transport is a timed gamble. Exact timer lengths (Gestator/partner tank baseline "safe," bucket decay, ground-puddle decay) not yet numerically set.

### Drooling Crucible

**Status:** ✅ Built (2026-08-20) as a standalone Tier 2 machine — Block/BlockEntity/Menu/Screen, registration, and datagen all live in code. Not yet reachable in survival (no craft recipe on purpose, see below) and not yet actually connected to Cauldron by any evolution mechanic — both are separate, deliberately deferred follow-up work, not gaps in this build.

**What it is:** Produces lava instead of water, otherwise identical in shape to Drooling Cauldron — passive generation (4 mB/s, same rate as Cauldron, per direction) plus food-boosted generation (same ingredient list/modifier as Cauldron's own, a straight copy under its own recipe type — see below). Fits the Tier 2 pattern established elsewhere (lava capability is the defining Tier 2 upgrade — see `dermicraft-project-primer.md` Stage structure).

**Implementation note:** built via `DroolingMachineBlockEntity<R>`/`DroolingMachineBlock`, a shared base extracted from what used to be Cauldron-only code — see the Drooling Cauldron entry's "Evolution Module family" subsection for the hooks this exposes (`currentTargetFluid`, `passiveYieldAmount`) and why they're shaped the way they are (built specifically so a future evolving Cauldron can share the exact same tank/block-entity machinery). Its own food-boost recipe (`VagueDroolingCrucibleRecipe`) is a separate `RecipeType` from Cauldron's, not a shared one — two recipes matching the same ingredient under one type would be ambiguous to Minecraft's own recipe lookup.

**Not yet built, still real work:** reachability (no implant/FL-native recipe — visible in creative only), and the evolution path connecting it to Drooling Cauldron at all (Evolution Catalyst injection, and/or the gradual Evolution Module — see Cauldron's own entry). Both are confirmed direction, not built.

**Confirmed:** Drooling Cauldron → Drooling Crucible is a real example of the Machines' forced-evolution mechanic — the Cauldron is forced into evolving into the Crucible via that process (Evolution Catalyst injection completes the transformation, per `dermicraft-project-primer.md` Machines). **As of 2026-08-19, a second route to this exact same end state is designed (not built)** -- a gradual, Module-driven path, see the Drooling Cauldron entry's "Evolution Module family" subsection above.

**Open questions:** ✅ Resolved (2026-08-20) — keeps food-boosted generation, same ingredients as Cauldron; passive rate is 4 mB/s, same as Cauldron.

### Craw

**Status:** Implemented — block, block entity, menu, and screen all live in code, along with the implant recipe and datagen (model/blockstate/tags/lang). Capacity-scaling is the one deferred piece.

**What it is:** A single-item-type storage block — holds multiple stacks of one item, functioning as bulk storage for automation and player alike. Distinct from Skin Tank (fluid-only bulk storage) as the item-side equivalent.

**Capacity:** 10 stacks (640 items of one type).

**Item-type locking:** Locks to whichever item type is first inserted, same pattern as fluid tanks locking to a fluid type. Unlocks back to "any item" once fully emptied — not a permanent restriction. Confirmed working: a mismatched item is silently refused while the Craw is locked.

**Access:** Automation and player can interact from any face — no directional restriction (matches Beaker's current-build access rules, not the old pre-rebuild top-only pattern). Automation goes through the block's item-handler capability straight to the storage slot.

**Interaction — two ways, both live:**
- **Drawer-style (right-click the block directly):** regular right-click with an item deposits **one**; crouch + right-click deposits the **whole held stack**. Empty-handed regular right-click withdraws **one**; crouch + right-click withdraws a **full stack** (or the remainder if less). This is the quick, no-GUI path.
- **GUI (opened via an attached Outerface, same as every other machine):** a dedicated **input slot** feeds into the **storage slot** — anything placed/shift-clicked/dragged into the input drains into storage on the next tick, so a whole stack transfers in one action instead of being capped at the vanilla one-stack-per-click limit. A static arrow sits centered between the two slots pointing input → storage. The storage slot shows a live total count next to it (since a vanilla slot only visually caps at the item's max stack size).

**Forceps pickup preserves contents.** Unlike most other collectible blocks — which drop their contents on the ground when picked up, same as a break — the Craw keeps its stored items *inside* the recovered block item, so it can be relocated fully loaded. Implemented via a marker interface (`IPreserveContentsOnPickup`) the Craw's block entity carries; the pickup path saves its data onto the item and removes the block entity before clearing the block, so nothing spills. A vanilla break still destroys it and drops the contents normally — only the Forceps pickup preserves. **No longer Craw-exclusive** — Skin Tank (and by inheritance, Chitin Tank) now opts into the same interface for its fluid contents; see Skin Tank's entry, above.

**Recipe (implant):** 1× Chest + 2× Dense Muscle + 2× Nerve Cluster, sutured with the **Suture Kit** → stitched closed → injected with Primitive Catalyst (100 mB).

**Note:** Same tumor-drop-item count as Skin Tank (2× Dense Muscle + 2× Nerve Cluster), with a Chest standing in for Skin Tank's Beaker — fitting, since the two are the item-side and fluid-side bulk-storage counterparts. Lighter than Effluentcer's recipe (which adds 3× Glass Flask on top of the same 2/2 organic base).

**FL-native recipe:** 1 Chest + 1500 mB Protein Blend + 100 mB Primitive Catalyst, FL-assembled (no suture; Chest stays physical the same way Beaker/Cauldron/Glass Flask do for their own machines — it has its own Metastasizer route, 2000 mB Pulp Blend, `metastasizing_chest`, not a hand-supply requirement, corrected 2026-07-19) — full worked-out math with the other worked examples (see FL-native machine recipes, under the Core section).

**Textures:** New `craw_side` for the block sides; top and bottom reuse Skin Tank's existing end texture. The GUI reuses the shared `screen_parts` (no bespoke GUI texture).

**Technical caveat (bulk storage vs. vanilla's 99-count cap):** the storage slot holds a single stack of up to 640, but Minecraft's ItemStack NBT codec rejects any saved count above 99. So the Craw serializes its storage manually (item type from a count-1 copy + the real count as a separate int) and splits its drops into ≤64 pieces on break. This is the reference pattern for every future bulk-storage block — see the working-conventions note in `dermicraft-project-primer.md`.

**Open questions:** Scaling mechanic (capacity is expected to scale somehow, not yet designed).

### Flesh Lab ("FL") — component blocks

**Status:** Core purpose, structure, and design role established; confirmed as the standard construction path for Gadgets and, more broadly, as a universal crafting front-end. **No longer the sole path for Gadgets** — the Gear Worx Stations' Workbench can now also fabricate any Gadget/gadget-adjacent item using these same recipes (see `dermicraft-gear-worx-notes.md` → Workbench, Duty 6), giving the player a self-contained alternative that doesn't require a full FL; the FL retains every recipe and its own exclusive scope elsewhere (Machines, etc.). Three physical component blocks now detailed below: **Lab Floor**, **Craw** (see above; doubles as FL output storage), and **Core**.

**What it is:** A **multiblock structure** — the first of its kind logged here, distinct from every other Machine so far, which are single blocks. Built around a **control block** — which advances through tiers (Tier 1 **Brain** → Tier 2 **Core**; see Control Tiers below) — plus dedicated **Floor** blocks. Any Dermicraft machines and storage devices attached to the floor, within range of the control block, feed into/provide for the FL.

**Confirmed — primary purpose is crafting Gadgets, but scope is broader.** The FL is the **standard, intended way Gadgets get built**, distinct from both the Tool category (no dedicated construction chain established yet) and Machines (hand-crafted via the Inert Tumor chain — see `dermicraft-tools-notes.md`). This gives the mod three parallel construction paths at three different scales/complexity levels:
- **Tools** — no dedicated construction mechanic yet.
- **Machines** — hand-crafted, one at a time, via the Inert Tumor chain (Syringe injection completes it).
- **Gadgets** — FL-crafted, the automation hub's signature output.

**Confirmed — craftable scope is universal, not Gadget-limited.** If the FL itself or any attached Machine on the floor network has a recipe for something, the FL can craft it. This makes the FL a universal front-end over the whole connected Machine network's combined recipe set, not a Gadget-specific device.

The three-way Tools/Machines/Gadgets construction split above is now **confirmed deliberate** — framed as a dependency ladder (Tools build Machines; the apex Machine, the FL, builds Gadgets) with a **living-matter through-line** as its real law (everything past simple Tools uses living matter, shifting from organic flesh to living metals over the Stages). Full framing, rules, and the Nose-on-a-Stick exception in `dermicraft-project-primer.md` → Construction philosophy.

**First confirmed FL-crafted Gadget — the Tablet.** See `dermicraft-gadget-notes.md` for full detail. Notable as the first Gadget built from **living materials**, raising an open question about whether the FL has its own distinct "how it builds living things" logic, separate from the hand-crafted tumor chain, or whether it's secretly still running some version of that same chain internally, just automated.

**Design role:** Envisioned as the mod's central automation hub — in the player's own words, "big daddy in automation." **Completable within Stage 1** as its Tier 1 milestone — the player builds it to a functional (if severely limited) state run by the Tier 1 **Brain** control, with no Stage 2 dependency required to *finish* or *use* it. Later stages **upgrade** it rather than complete it. Not strictly necessary to play or finish the mod.

**Control Tiers (open-ended — only Tier 1 and Tier 2 defined so far):** the FL's control block advances through tiers, each a bigger, more capable brain; more are expected beyond the two below.
- **Tier 1 — Brain:** completes the FL in Stage 1. Functional but severely limited (see Brain — Tier 1 Control, below) — this is what makes the FL a genuine Stage 1 milestone rather than a Stage 2 one.
- **Tier 2 — Core:** the Brain **evolved** in Stage 2 via the Evolution Catalyst step (see recipe below), lifting the Brain's limits. The Core sections below document the FL's control behavior in general; the Brain runs the same systems under tighter limits.

**Evolution — two senses, at two scales (the word deliberately does double duty):**
- **Component (forced) evolution** — the biological sense the mod already uses: individual machines and the control block are *made* to transform to improve, via the tumor-genesis / Evolution Catalyst mechanic — a discrete, triggered event. The control block advances through its Control Tiers (above) this way; **Brain → Core (Stage 2) is the first confirmed step**, more tiers expected beyond the Core (how many, and what each unlocks, not yet set).
- **Structural (whole-FL) evolution** — the general/dictionary sense: the FL as a whole is never "finished." It evolves gradually as the player iteratively **swaps parts out for better ones** — better Floor variants, upgraded storage, a higher control tier, adding or rearranging machines. No single trigger; it's the aggregate of the player's ongoing improvements. This is exactly the **FL-wide modularity** rule below, viewed as a progression mechanic — modular part-swapping *is* the mechanism of structural evolution, and **Knitting** is the FL re-settling after each such step.

**Relationship to Syringe (resolved):** The two split cleanly — **Syringe** crafts Machines **by hand**; **FL** is the **automated** counterpart, and now confirmed as a universal crafting front-end rather than Gadget-only. Not overlapping roles.

**FL-wide modularity (new, general rule):** All FL component parts — Floor, Core, flanking output storage — can be **upgraded, changed, or moved around** after initial construction; the FL is not a fixed one-time build. Structural changes trigger **Knitting** (see Core — Recursive Crafting & Network Behavior, below) — the network's event-driven recompute/reconnection pass — rather than an always-on continuous check.

**Open questions:** New concept or carried over from the old version? Exact range of the Core's influence (Core-level property, mechanics deferred to Code). What counts as "attached to the floor" — resolved as **touching a Floor piece on any face** (in the floor plane, under, or on top); face doesn't matter, only contact (see Lab Floor below). How many evolution stages does the FL have, and what does each one unlock? Whether the FL's living-material construction (Tablet) implies a general rule for all future living Gadgets, or is a one-off.

#### Lab Floor (formerly "Floor Block")

**Status:** Core purpose, connection behavior, and a two-material bootstrap recipe pair decided. Material progression roster open-ended. Naming decided 2026-07-21: block name is `<material> Lab Floor` (e.g. "Stone Lab Floor"); JSON assets live under a `flesh_lab` subdirectory.

**What it is:** A pure pass-through connector — conducts both **items and fluids** between the Core and attached Machines/storage, but never stores either itself.

**Connection — any-face contact (revised, supersedes the old "on top only" rule):** a machine or storage block connects to the network if it is **touching a required Floor piece on any face** — sitting in the floor plane ("part of the floor"), directly under a Floor piece, or on top. **Face doesn't matter, only contact.** This flexibility is *why* the FL accesses Dermicraft machines directly rather than through capabilities (see Machine access, Core section) — with no single predictable connection face, face-based capability access would get in the way. The only place faces still matter is **foreign (non-Dermicraft) blocks**, which the FL reaches through the capability path where sided access applies.

**Connection requirement — within-reach connectivity:** Floor blocks must form a connected network, but "connected" is now **within-reach**, not strictly adjacent — each floor bridges up to its own tier's reach (see Floor Tiers below: Tier 1–2 adjacent-only, Tier 3–4 one-block gap), and that reach applies to both floor-to-floor spacing and machine/storage connection. The **control block's range** is still the outer boundary of the whole structure (Brain's cube, Core's larger range); floor reach is the *local* connectivity within it, and floors must be present to propagate connections through the structure at all.

**Fluid-output-face conflict — resolved.** Previously flagged: existing machines output fluid from the bottom face, which conflicts with sitting a machine on top of a Floor block. This is now resolved by the FL's **direct machine access** (see Core — Recursive Crafting & Network Behavior): because the FL reads/writes Dermicraft machines' tanks directly rather than through face-based capabilities, block faces are irrelevant to FL transfer entirely — no output-face override needed.

**Material progression (variants tied to tiers):** Lab Floor blocks come in multiple variants tied to different structural materials, mirroring a mini progression rather than a single fixed recipe:
- **Stone Lab Floor**, **Cobblestone Lab Floor**, **Deepslate Lab Floor**, **Cobbled Deepslate Lab Floor**, **Diorite Lab Floor**, **Andesite Lab Floor**, and **Granite Lab Floor** — seven separate crafting-table recipes, each hand-craftable as an early (Tier 1) bootstrap variant (see recipe below). **Implemented 2026-07-21** — blocks, recipes, and a composited overlay-model template (opaque base texture + separate top/bottom and side overlay textures, no rotation) are live in code. Each variant keeps its base material's vanilla strength/hardness, and drops itself on break (same treatment as ducts/nodes/gates — structural infrastructure, not a "destroyed on break" machine).
- **All other variants** — most likely **FL-crafted only**, no crafting-table recipe of their own. Once enough Lab Floor blocks exist to build a working Core and initial structure, the FL can craft further/better Lab Floor blocks itself — a self-replication loop, consistent with the "prefer Dermicraft-native processes" convention.
- **Metal variants confirmed planned as Tier 2:** one Floor variant per main metal (Ferrous, Cuprous, Aurous), matching the existing Metal Blend trio. Exact recipes deliberately deferred until more Stage 2 material exists to design them against.
- **Variants are functional, not cosmetic** — a variant's material sets its **tier**, which carries the functional properties below. (Supersedes the earlier "currently cosmetic only" note.) Roster still open-ended.

**Floor Tiers — function (fluid-type, reach):** a floor's tier bundles **two escalating properties** — one tier number, advancing both at once. (Originally three — **throughput was scrapped 2026-08-09**, dropped from the design rather than deferred; nothing in the mod ever varies transfer speed by floor tier.)

1. **Fluid-type capability** — what hazard classes the floor can conduct, following the mod's Stage-hazard ladder (see `dermicraft-project-primer.md` → Stage structure / Hazard tag hierarchy). Cumulative — each tier handles its own hazard class **and all below it**, via the tier-appropriate material (it's not "free"; higher tiers incorporate the materials that actually solve each hazard, consistent with the primer's "heat-handling ≠ radiation-handling" principle):
   - **Tier 1** — safe fluids only (no hazardous). *(Stage 1)*
   - **Tier 2** — + Extreme Heat (lava). *(Stage 1→2 gate)*
   - **Tier 3** — + Mild Radiation. *(late Stage 2 / early Stage 3)*
   - **Tier 4** — + Severe Radiation and Biohazard. *(late Stage 3 / Stage 4)*
2. **Reach** — the floor's *local* connection span (see Connection requirement above): **Tier 1–2 = adjacent only** (0 gap); **Tier 3–4 = one-block gap**, applied to both floor-to-floor spacing and machine/storage connection.

**Path resolution:** a "path" is the chain of floors a fluid/item traverses between a machine/storage block and the control block.
- **Fluid-type capability = the lowest-tier floor in the path** (weakest link) — a single Tier 1 floor anywhere along a route caps that route at "no hazards," regardless of the other floors.
- **Reach = per-floor, piece by piece** (local, *not* weakest-link) — each floor bridges its own gap by its own tier.

**Unsupported fluid = hard block:** if a recipe needs a fluid the relevant path can't conduct, it's blocked with the standard Core-GUI error (sound + message) until the floor network is upgraded to carry it.

**Implemented for the Gear Worx Stations' Floor network, 2026-08-09 (hazard-capability first, then reach — throughput scrapped, see above).** `TieredFloor` (new interface, `block/custom/floor/`) is what a Lab Floor block implements to declare `floorTier()`, deriving both `hazardProfile()` (reusing `HazardProfile`'s existing TIER_1-4 ladder presets verbatim) and `reach()` (0 for Tier 1-2, 1 for Tier 3-4) as defaults. `LabFloorBlock` is the new base class all seven current Lab Floor variants now use (still all Tier 1 — no functional change to today's game, this is groundwork for when Tier 2+ material variants land). `FloorNetwork`'s walk reads both off each visited tile's blockstate, no block entity added:
- **Hazard capability is network-wide weakest-link, not true per-path.** `FloorNetwork` intersects every visited floor tile's `hazardProfile()` into one profile for the whole connected region, then wraps every returned `IFluidHandler` in a new `HazardGatedFluidHandler` so a fluid the network can't conduct is invisible/undrainable through the pool (not just excluded by some separate check a caller has to remember). This is a deliberate simplification: the Gear Worx Station pool is genuinely flat/shared (no per-consumer routing concept exists anywhere in that system), unlike the FL Core's own path concept above, which doesn't exist in code yet and can implement the real per-path version when it does.
- **Reach is genuinely per-floor**, matching the design exactly — each visited tile's own `reach()` decides how many blocks a connection may skip past it, both when walking floor-to-floor and when finding containers adjacent to a floor tile. No obstruction check on what occupies a skipped block (consistent with this mod's ducts/Nodes not doing line-of-sight checks either). Since every current Lab Floor variant is Tier 1 (reach 0), this doesn't change today's connectivity — it's live and correct, just not yet exercisable until a Tier 3+ variant exists.

**Recipe (crafting table) — Tier 1 Lab Floor variants, revised 2026-07-21:** a 3×3 shaped recipe, not an implant — structural block in the center, **Nerve Cluster** on the four edge-middles (plumbing/wiring), **Dense Muscle** on the four corners (connective casing):

```
D N D
N S N
D N D
```

The structural block is the only thing that changes per variant:

| Variant | Structural block |
|---|---|
| Stone Lab Floor | 1 Stone |
| Cobblestone Lab Floor | 1 Cobblestone |
| Deepslate Lab Floor | 1 Deepslate |
| Cobbled Deepslate Lab Floor | 1 Cobbled Deepslate |
| Diorite Lab Floor | 1 Diorite |
| Andesite Lab Floor | 1 Andesite |
| Granite Lab Floor | 1 Granite |

**Open questions:** Exact ingredient quantities. Recipes/materials for each tier's variants (metal and beyond). Exact throughput and reach numbers per tier. How many tiers/variants total (open-ended, but at least the four hazard tiers above).

#### Autonomous Structure Growth

The FL builds itself out through **three related-but-distinct behaviors** — the first already noted (request-crafting), the latter two new. All three make the FL the mod's one visibly "living," self-extending structure, and reinforce the **Structural (whole-FL) evolution** concept above.

1. **Request-crafting (existing):** on demand, the FL crafts more Floor pieces *into* network storage (the self-replication loop already noted under Lab Floor). Produces stock; does not place anything.
2. **Self-Build (new, toggleable):** when enabled via a GUI toggle, roughly **every ~30 seconds** the FL takes one eligible piece from network storage and **places it into the structure**, if one is available (if not, it simply does nothing that tick).
   - Eligible pieces: **Floor blocks, machines, and storage devices** held in storage.
   - Placement extends the existing network — a Floor block is copied adjacent to an existing Floor block; machines/storage are installed into valid connected spots (touching the Floor network, per the any-face rule), within the control block's range.
   - Player-controlled: feed the FL materials and it assembles itself. Consumes the stored piece.
3. **Growth (new, always-on, not player-controlled):** a small chance rolled **~every half hour**, **free** (nothing consumed) — the FL picks a **random existing Floor block and places an identical copy adjacent to it**, so it grows using whatever variants are already in the structure.
   - **Floor blocks only** — never machines or storage.
   - **No toggle** — an inherent living behavior, unlike Self-Build.
   - **Fuel grade modulates the chance** (better grade → slightly more likely); fuel is only *read*, never consumed for growth, so this doesn't conflict with the "idle machines don't consume fuel" rule.
   - Placement is contiguous and into valid empty space only, within the control block's range — so growth scope scales automatically with control tier (a Brain's small cube vs. a Core's larger range).

**Open questions:** exact Self-Build interval and Growth chance/cadence numbers. How machines/storage devices choose their placement spot during Self-Build (Floor placement is defined; machine/storage placement logic is not). Whether Self-Build should ever be smart about *what* it pulls (prioritizing floors vs. machines) or stay simple.

#### Core

**Status:** GUI/crafting behavior, output flanking, fuel/HP model, and the full Brain-build + Core-evolution recipe decided. Core-tier range and recursion depth deferred to Code.

**What it is:** The FL's **Tier 2** control block — the Brain evolved (see Control Tiers above). Functions like a brain (thematically, not necessarily in shape), tying back to the resurrected **Brain Block** it grows from. Everything in this Core section — GUI, crafting scope, recursion, network behavior — describes the FL's control behavior generally; the **Brain (Tier 1)** runs the same systems under the tighter limits in its own subsection below.

**Player interaction:** Right-click opens a GUI displaying available crafting options. Selecting an option displays its ingredient list. A button starts crafting.

**Craftable scope:** Universal front-end over the combined recipe set of the FL itself and every attached Machine on the floor network — not limited to Gadgets. If the FL or any connected Machine has a recipe for something, the FL can craft it.

**Recursive crafting:** If direct ingredients aren't available but the ingredients-to-make-those-ingredients are, crafting still begins — the Core actively routes sub-crafts through connected Machines (e.g. a Masticator produces a needed Blend on demand), and crafts some outputs itself directly. **Recursion depth limit is an open question for Code**, flagged specifically as a lag/performance risk on the server.

**Queueing:** Single job at a time. All production runs one batch at a time until the request's amount-remaining counter is satisfied (see Universal batch processing under Core — Recursive Crafting & Network Behavior). Explicitly provisional — may expand later.

**Output:** Flanked by a Skin Tank and Craw (the same blocks documented elsewhere — no bespoke FL-specific storage) where the player collects finished results. These flanking pieces are **separate from and unrelated to** the tank used in the Core's own construction (see the Tier 2 evolution step below), and can be swapped for different/upgraded storage as part of the FL's general modularity (see above).

**Fuel/HP — fuel-required, no HP, heal repurposed into efficiency:**
- **No HP mechanic.** The FL is the first confirmed Machine to use fuel while opting out of the standard HP/health-drain system entirely. With no HP pool, it has no damage-grace to spend, so it is **fuel-*required*** — unlike the fuel-*optional* machines that limp along on HP (the Masticator template). Since Slurry is only burned while actively processing, an idle FL never stalls.
- **Speed:** fuel grade drives FL processing speed as with other machines, and also modulates the autonomous Growth chance.
- **Heal → efficiency (repurposed):** because there is no HP for the fuel's heal modifier to act on, the FL **folds the heal factor into the use-rate factor to its own benefit** — heal *reduces* the FL's effective fuel use rate, so fuel lasts longer. On the Main Line (where heal scales with grade) this makes **better fuel disproportionately more fuel-efficient in the FL specifically** — premium fuel runs both fast *and* long here. Exact formula (e.g. use ÷ heal) deferred to Code; design intent is "heal lowers use for the FL." Thematically reframes "fuel heals the FL" into "fuel sustains it longer," fitting the living-structure theme. (Serum-family fuels, with their inverted/zeroed heal values, would interact oddly here — flagged as open if Serums are ever FL-usable; Main Line is the clean case.)

**Stop-on-fuel-out behavior:** when fuel runs out mid-craft, the FL **halts immediately** and **preserves** the in-progress batch's progress (not lost), surfaces the standard error/warning in the Core GUI, and **the player decides the next action** — add fuel and Continue (resume from preserved progress) or Cancel. Same pattern as the ingredient-exhaustion recovery.

**Recipe — build the Brain (Stage 1), then evolve it into the Core (Stage 2):**

1. **Tier 1 build (Stage 1) — Brain Block:** a **Mutator** recipe (`mutating_brain`) — **Proto Brain** (consumed) + **2500 mB Protein Blend** → **Brain Block**, same price/timing as the Craw/Skin Tank Mutator routes (see Metal Blends' "Explicit non-fits" section for that precedent). Supersedes an earlier Craw-incubation design (full stack of Nerve Clusters + Primitive Catalyst injection), which is no longer implemented. This **completes the FL** as a functional Stage 1 control — no further step is required to *use* the FL (see Brain — Tier 1 Control for its limits). (Brain Block is a revived item from the pre-rebuild version of the mod, previously called "Smooth Brain," now upgraded to block form — also usable as a decoration block, giving it a second use beyond this recipe per the mod's "every item needs more than one use" convention.)
2. **Tier 2 evolution (Stage 2) — Core (revised — now matches the Gear Worx Stations evolution model, see `dermicraft-gear-worx-notes.md`):** the Brain **evolves in place** — it is never removed, pulled into a tank's input slot, or otherwise physically extracted during the process; it stays functioning as the FL's live control block right up until the transformation completes. The FL's screen displays the evolution's fluid requirements; once the connected floor network's shared pool holds enough, the player presses a button to begin a **timed** evolution process. **If interrupted, the consumed fluids are lost but the Brain itself is unaffected** — same fail-safe as Gear Worx Station evolution.
   - **Requirements:** **250 mB Evolution Catalyst** (bumped up from the old single-shot 100 mB) + **20,000 mB Molten Redstone** (one Chitin Tank's now-confirmed capacity), both consumed on completion — consistent with the shared-pool model, it **does not need to come from a single tank**; any combination of tanks on the platform totaling 20,000 mB satisfies the requirement.
   - **Chitin Tank** (see its own full entry under Known machines → Skin Tank → Chitin Tank) is **not consumed** by this evolution — only the fluids inside it are. This recipe was simply its first mention, not its defining purpose.
   - Mechanically distinct from the old fill-and-inject ritual, but still a **fixed, one-time recipe** (unlike Gear Worx Stations' per-tier-scaling costs) — same family of mechanic, different species: a singular, higher-stakes event for the FL's own control block vs. a repeatable per-tier process for Gear Worx Stations.

**Cross-reference resolved:** This gives **Molten Redstone** a second confirmed use beyond the Redstone Torch Dip, resolving the open question logged in `dermicraft-crafting-notes.md`.

**Open questions:** Core's exact range value and recursion depth cap number (methodology now decided — see below — but the actual numbers are deferred to Code). Exact heal→use-rate efficiency formula for the FL (resolved in principle — see Fuel/HP — but the math is deferred to Code).

#### Brain — Tier 1 Control (limitations)

**Status:** Limitations decided. FL-only recipe short list still open.

The Brain is the FL's Stage 1 control block. It runs **all** the same crafting/network systems documented in the Core sections below (recursion, universal batching, drain-to-storage, direct machine access, storage-first fulfillment, Knitting), but under these hard limits until evolved into the Core:

- **Tier gate:** controls only **Tier 1** machines and storage devices. Higher-tier blocks are out of reach until the Core evolution.
- **Range — 5×5×5 cube centered on the Brain block:** ±2 on every axis. A cube/box check, not a radial sphere — also cheaper to compute than a distance check. A machine/storage block must be inside this cube **and** touching the Floor network (any-face rule) to be controlled. (The Core's own range is larger, deferred to Code.)
- **Recursion — full, but shallow in practice:** the Brain runs the complete recursive resolver (cycle detection, sub-craft routing, batch loop), not a cut-down version; its other limits keep trees shallow on their own. **Depth anchor:** Primitive Catalyst — `Effluencer(F-Stuff + C-Stuff)` → each `Effluencer(two Blends)` → `Masticator(Blend)`, ~3 crafting layers, every machine Tier 1 — is the confirmed in-scope case the Brain must handle comfortably.
- **Recipe breadth:** crafts anything its connected Tier 1 machines can, **plus a few FL-only recipes** (candidate list below — some may instead be handed to a real machine later, the way F-Stuff/C-Stuff went to the Effluencer). Last-resort Early Implant/Puddle Crafting behavior is general FL behavior (see Recipe priority, below), not Brain-specific.

**FL-only recipes (candidate list — Tier 1 Brain; provisional, any may later migrate to a dedicated machine). All are NEW and additive — the existing `early_implant` recipes for these tumors are kept (no recipe removed without direct order):**
- **Variant tumors — each = 1000 mB Protein Blend + the one harvested part that defines it (all inputs consumed). REASSIGNED to the Mutator** (single item + single flesh-analog reagent is a textbook Mutator recipe; see the Mutator entry above). They stay FL-craftable because the FL drives any connected Tier 1 machine's recipes — they're no longer *FL-only*, just FL-invokable Mutator recipes:**
  - **Eye Tumor** ← 1000 mB Protein Blend + 1 **Eye**
  - **Nerve Tumor** ← 1000 mB Protein Blend + 1 **Nerve Cluster**
  - **Muscle Tumor** ← 1000 mB Protein Blend + 1 **Dense Muscle**
  - (The base **Inert Tumor** is *not* an FL-only recipe — it uses the existing Metastasizer Protein-Blend duplication recipe. **Marred Tumor** is likewise a Metastasizer recipe, 900 mB Protein Blend — not FL-only.)
- **Stitched Tumor** ← 950 mB Protein Blend, no defining item — *printed* directly from fluid (see method note below). **Stays FL-native — does NOT fit the Mutator** (no input item to mutate).
- Because each defining part has a Metastasizer duplication recipe (Protein Blend, 250 mB) and the FL runs the Metastasizer **pattern-free**, this whole chain is cleanly FL-craftable end-to-end with no last-resort recipes: Masticator → Protein Blend, Metastasizer → the part, FL → the variant tumor.

**FL recipe method — "printing" vs "combining":** FL recipes currently favor **printing a result directly from fluid** (materializing it — e.g. Protein Blend → tumor) over **combining multiple components** into a result. That's why **binding agents** — the term covers **catalysts and slurries injected in hand-crafted recipes** (e.g. Primitive Catalyst) — barely feature in the FL's print recipes so far; printing doesn't need them (it's also why even a "Stitched" Tumor is printed from Protein Blend rather than assembled from stitching/binding materials). Combining-style recipes — which *do* use binding agents — become more common as the FL starts building **machines** (see FL-native machine recipes, below), the first real home for this pattern. **Do not retro-add binding agents to the tumor print recipes above** without direct order.

**FL-native machine recipes — standing formula (corrected/clarified).** Unlike tumors (pure prints), machines are inherently **combine** recipes — every implant recipe pairs one defining vanilla item with organic "flesh" parts and an injected binding agent, and physically starts from an **Inert Tumor** block (the vessel that gets sutured shut and injected — see "Every Machine here is physically born from an Inert Tumor block," above). The FL-native version of each machine recipe follows one consistent, derived formula rather than being invented per-machine:
- **Keep the machine's one defining/structural vanilla item physical** — whatever gives the machine its functional identity (Beaker, Cauldron, Chest, Glass Flask, Bone, etc.). This is deliberate: it keeps the Metastasizer *meaningfully in the loop* — if every ingredient became fluid, the Metastasizer (and the whole physical-parts economy) would lose its purpose within the FL.
- **Total flesh cost = (Inert Tumor's own Metastasizer duplication cost + every listed "flesh" ingredient's Metastasizer duplication cost) summed, then discounted to 75%** of that total, rounded down to the nearest 10 mB. **The Inert Tumor's cost is universal, not conditional** — every machine is born from one whether or not it appears as a listed ingredient in the implant recipe (since the FL skips having a physical Inert Tumor at all when it prints a machine directly, its fluid-equivalent cost still has to be paid). "Flesh" ingredients are Dense Muscle, Nerve Cluster, Eye, and any other harvested-part item a specific recipe lists.
- **Any other "simple" (non-flesh, non-structural) item in the base recipe** — if a recipe calls for something else entirely (not the structural item, not flesh) — also converts to its own established fluid-equivalent cost and gets the **same 75% discount**, applied separately from the flesh sum above. None of the machines documented so far have needed this term (it's 0 for both worked examples below), but it's part of the general formula for future recipes that do.
- **Keep the recipe's injected binding agent** (e.g. Primitive Catalyst) **unchanged** — same amount as the implant, no discount.
- **Drop the suture-tool requirement** — the FL assembles automatically; suturing is a hand-crafting step that doesn't apply.

**Worked example — Metastasizer (resolves the previously-missing implant recipe too):**
- **Implant recipe (new — the Metastasizer had none before):** 1 **Beaker** + 2 **Dense Muscle** + 4 **Nerve Cluster** + 1 **Eye**, injected with **100 mB Primitive Catalyst** (sutured). Pricier than the other implants (8 items vs. ~5) to reflect its powerful duplication ability. Conceptual framing: the Metastasizer is a small **3D printer** — it scans a pattern and prints copies from fluid "ink"; the Eye fits as the scanner.
- **FL-native recipe:** Beaker stays physical (the printer's housing). Flesh cost: (2 Dense Muscle + 4 Nerve Cluster + 1 Eye) × 250 mB each = 1750 mB, **plus** the (universal) Inert Tumor cost (1000 mB) = 2750 mB raw, × 75% = 2062.5 mB, rounded down to **2060 mB Protein Blend**. No other simple items. Catalyst unchanged.
  - **Final: 1 Beaker + 2060 mB Protein Blend + 100 mB Primitive Catalyst.** FL-assembled, no suture.

**Worked example — Skin Tank (corrected — previously omitted the universal Inert Tumor cost):**
- **Implant recipe (live in code):** 1 **Beaker** + 2 **Dense Muscle** + 2 **Nerve Cluster**, injected with **100 mB Primitive Catalyst** (sutured).
- **FL-native recipe:** Beaker stays physical. Flesh cost: (2 Dense Muscle + 2 Nerve Cluster) × 250 mB each = 1000 mB, **plus** the (universal) Inert Tumor cost (1000 mB) = 2000 mB raw, × 75% = **1500 mB Protein Blend** (clean multiple of 10, no rounding needed). No other simple items. Catalyst unchanged.
  - **Final: 1 Beaker + 1500 mB Protein Blend + 100 mB Primitive Catalyst.** FL-assembled, no suture.

**Worked example — Effluentcer:**
- **Implant recipe (live in code, `effluentcer_implant.json`):** 3× **Glass Flask** + 2 **Dense Muscle** + 2 **Nerve Cluster** (sutured), injected with **100 mB Primitive Catalyst**.
- **FL-native recipe:** all 3 Glass Flasks stay physical (Effluentcer's defining/structural requirement, same role Beaker plays elsewhere — it just happens to need three). Flesh cost: (2 Dense Muscle + 2 Nerve Cluster) × 250 mB = 1000 mB, **plus** the universal Inert Tumor cost (1000 mB) = 2000 mB raw, × 75% = **1500 mB Protein Blend** (same flesh composition as Skin Tank, same result). No other simple items. Catalyst unchanged.
  - **Final: 3 Glass Flask + 1500 mB Protein Blend + 100 mB Primitive Catalyst.** FL-assembled, no suture.

**Worked example — Drooling Cauldron:**
- **Implant recipe (live in code, `drooling_cauldron_implant.json`):** 1 **Cauldron** + 2 **Nerve Cluster** + 2 **Dense Muscle** (sutured), injected with **100 mB Primitive Catalyst**.
- **FL-native recipe:** Cauldron stays physical (its own Metastasizer-sourcing route — 7000 mB Ferrous Blend, see Defining-item sourcing below — is a separate way to *obtain* a physical Cauldron, not a substitute for keeping it physical here). Flesh cost: (2 Nerve Cluster + 2 Dense Muscle) × 250 mB = 1000 mB, **plus** the universal Inert Tumor cost (1000 mB) = 2000 mB raw, × 75% = **1500 mB Protein Blend**. No other simple items. Catalyst unchanged.
  - **Final: 1 Cauldron + 1500 mB Protein Blend + 100 mB Primitive Catalyst.** FL-assembled, no suture.

**Worked example — Masticator:**
- **Implant recipe (live in code, `masticator_implant.json`):** 2× **Bone** + 2 **Dense Muscle** + 1 **Nerve Cluster** (sutured), injected with **100 mB Primitive Catalyst**.
- **FL-native recipe:** both Bones stay physical (Masticator's defining/structural requirement, same "needs more than one" pattern as Effluentcer's 3× Glass Flask). Flesh cost: (2 Dense Muscle + 1 Nerve Cluster) × 250 mB = 750 mB, **plus** the universal Inert Tumor cost (1000 mB) = 1750 mB raw, × 75% = 1312.5 mB, rounded down to **1310 mB Protein Blend**. No other simple items. Catalyst unchanged.
  - **Final: 2 Bone + 1310 mB Protein Blend + 100 mB Primitive Catalyst.** FL-assembled, no suture.

**Worked example — Craw:**
- **Implant recipe (live in code, `craw_implant.json`):** 1 **Chest** + 2 **Dense Muscle** + 2 **Nerve Cluster** (sutured), injected with **100 mB Primitive Catalyst**.
- **FL-native recipe:** Chest stays physical (its own Metastasizer-sourcing route — 2000 mB Pulp Blend, `metastasizing_chest`, see Defining-item sourcing below — is a separate way to *obtain* a physical Chest, not a substitute for keeping it physical here, same relationship as the Drooling Cauldron/Cauldron pairing above). Flesh cost: (2 Dense Muscle + 2 Nerve Cluster) × 250 mB = 1000 mB, **plus** the universal Inert Tumor cost (1000 mB) = 2000 mB raw, × 75% = **1500 mB Protein Blend**. No other simple items. Catalyst unchanged.
  - **Final: 1 Chest + 1500 mB Protein Blend + 100 mB Primitive Catalyst.** FL-assembled, no suture.

**Defining-item sourcing, resolved for all five machines (see Metastasizer entry above for full detail):**
- **Beaker** (1000 mB Silica Blend) — sources the Metastasizer's own recipe and the Skin Tank's.
- **Glass Flask** (250 mB Silica Blend each) — sources the Effluentcer's 3× Glass Flask requirement.
- **Cauldron** (7000 mB Ferrous Blend) — sources the Drooling Cauldron's.
- **Chest** (2000 mB Pulp Blend, `metastasizing_chest`) — sources the Craw's. Corrected 2026-07-19: previously documented as having no Metastasizer route at all ("no sensible fluid identity"); that was stale — Chest is species-agnostic wood furniture, same treatment as Crafting Table, and the recipe already existed in code.
- **Bone** (1000 mB Calcium Blend, existing) sources the Masticator's. With Chest now confirmed sourced too, all five machines are fully FL-sourceable — no remaining manual-sourcing gap in this set.

**Open questions:** whether the Beaker or Glass Flask should ever get the same physical→fluid treatment applied to *themselves* in some other recipe (deliberately not applied within their own Metastasizer recipes, to preserve the "keep one real item" principle above).

#### Core — Recursive Crafting & Network Behavior

**Status:** Crafting-resolution rules, error handling, and network fluid/item lifecycle fully decided. Exact numeric limits (recursion depth cap, Core range) still deferred to Code — only the surrounding methodology is a design decision now.

**Why this is feasible, and where the real risk is:** the Core/Floor network graph and range checks are the same category of problem already solved by other mods' block-network systems (Applied Energistics 2's cable networks, etc.) — feasible as long as the graph is recomputed only on structural change (see Knitting below), never continuously. The recursive crafting resolver is the one genuinely heavy system here, comparable in complexity to AE2/Refined Storage's autocrafting — feasible, but only safe with the cycle-prevention rules below.

**Cycle prevention (layered):**
1. **Ancestor-chain cycle detection (primary):** while resolving a crafting tree, the Core tracks which items are currently being resolved in the active branch. If resolving an ingredient would require producing an item that's already an ancestor in that branch, the branch is rejected as circular immediately — catches a cycle at any depth, not just past an arbitrary limit.
2. **Hard depth cap (secondary):** a backup safety net for legitimately deep, non-circular chains. Exact number deferred to Code.
3. **Memoization per craft attempt:** once the Core determines whether it can/can't make N of an item within a single resolution pass, that result is cached instead of being re-derived every time the item recurs elsewhere in the tree.
4. **Author-time cycle validation:** since every FL-craftable recipe is first-party, a load-time/datagen-time validation pass walks the entire known recipe graph and flags a genuine circular dependency as a build-time error — caught during development, not discovered by a player at runtime. Rule 1 remains as runtime defense-in-depth (e.g. against a future datapack/mod interaction introducing a cycle).

**Error feedback:** any rejected or failed craft (circular dependency, depth cap exceeded, insufficient ingredients, drain lockout, etc.) surfaces as an **error sound + message in the Core's GUI** — the standard error-feedback pattern used throughout the FL's crafting system.

**Universal batch processing.** All FL-driven production runs one batch at a time — craft a batch → wait for it to finish → craft the next → repeat until the request's amount-remaining counter hits zero. This is **structure-wide**, applying to every machine and every recipe (no "batchable by the stack" fast path — that earlier provisional idea is retired). Beyond consistency, one-batch-at-a-time deliberately spreads a large job across many ticks rather than resolving it in a single burst — the same anti-lag-spike approach used by One Punch's staggered block-breaking, so universal batching is the cheaper option on the server, not just the tidier one. There is no separate "Vague recipe" handling: because batching loops until the counter is satisfied, nutrition-scaled variable yield is absorbed automatically — the loop simply runs another batch if the last one fell short. (An earlier "Vague recipes excluded from auto-resolution" rule, and its associated no-amount mode and upfront cost warning, are all scrapped as unnecessary under this model. If playtesting shows a warning is wanted, revisit then.)

**Multiple identical machines — parallel throughput.** Duplicate machines of the same type run **in parallel** to multiply throughput — the primary way the player scales FL speed (alongside fuel grade, which speeds each machine individually). Parallelism is *within* a request (the FL still resolves one player request at a time); the batch loop is per-machine rather than global. Distribution priority:
1. **Distinct recipes first** — if a request needs several *different* sub-recipes a machine type can make, spread them across the available duplicates (e.g. with two Masticators and a need for both Carbon Blend and Calcium Blend, one machine takes each) so all the different outputs progress at once.
2. **Then same-recipe parallelism** — once each distinct needed recipe is assigned, any remaining duplicate machines double up on a recipe that still has **multiple batches** left, running those batches in parallel.

Because machines must be within the control block's range to be used, the control tier caps how parallel the FL can get (a Brain's 5×5×5 cube fits few machines; a Core's larger range fits more) — the parallelism ceiling falls out of the range rule, no separate cap needed.

**Fuel across parallel machines:** the FL fuels active machines from the shared storage pool, **best grade first, cascading down the Main Line tier chain** (Superior → … → Crude) as better grades run out. If there's enough of the best available grade to fuel every machine in use, they all run on that same grade; if not, the best grade goes as far as it reaches and the remaining machines take the next grade down, and so on.

**Recipe priority & last-resort hand-crafts:** the FL prefers machine/native recipes. **Early Implant and Puddle Crafting recipes are last-resort fallbacks** — used only when a hand-craft recipe is the *only* available path to a needed output. When one is used, the FL does **not** simulate the physical puddle/injection; it materializes the result directly from the recipe's own numbers (consume inputs → produce output). Because those recipes are inherently a worse deal per result than the machine path, a single per-request warning surfaces before crafting begins, showing the **total count of last-resort invocations** across the resolved tree (e.g. "This job will shortcut 2 hand-crafted recipe(s). Proceed?") with a Continue/Cancel choice. The count is total invocations (not distinct recipes) because each invocation re-incurs that higher cost. This is a distinct, narrower warning from the scrapped vague-yield warning — it fires specifically for last-resort hand-crafts.

**Per-machine invocation rules — when the Core may auto-call each of the three special machines:**
- **Masticator:** **never** called during an **item** crafting request. **May** be called during a **fluid** crafting request, via the universal batch loop; the item ingredient each batch consumes is chosen by the structure-wide item source setting (below).
- **Metastasizer:** **never** called during a **fluid** crafting request (prevents its item-duplication mechanic from becoming a free fluid source). **May** be called during an **item** crafting request.
  - **Pattern-free operation (FL-exclusive):** the FL can run a Metastasizer recipe **without owning the pattern item** — it spawns a **transient pattern** into the Metastasizer for the duration of the job, which vanishes on completion. Only the fluid is consumed; no pattern is retained. Manual (non-FL) Metastasizer use still requires a real pattern, so this is a reward for building the automation hub. It's bounded (full fluid cost per item, no retained pattern → no free-matter loop, and the fluid-request exclusion above still holds) and it **simplifies the recursive resolver** — Metastasizer recipes become effectively *fluid-only* to the resolver, since the pattern no longer needs sourcing.
- **Drooling Cauldron:** each cauldron in the family **passively auto-produces one fluid on its own**, so by the time the FL needs that fluid it may already hold enough that no production request is triggered at all (ties into storage-first fulfillment). Its fluid is **pulled into storage at both the start and end** of a crafting process (harvesting whatever it passively accumulated). **Not** called to produce during a **food-item** request; otherwise called to produce when needed.

**Source settings (structure-wide) — two independent settings, one for items and one for fluids:** when a machine has a choice between multiple eligible input stacks, the FL picks by player-set options in the Core GUI. There are **two separate toggles** — one governing **item** ingredient selection, one governing **fluid** ingredient selection — each independently set to **most plentiful** (burn down overflow, protect scarce inputs) or **least plentiful** (clean out small/odd stacks, consolidate storage). Once a source is selected it is used continuously until the request completes or that source is exhausted, then the FL fails over to the next stack per the same rule. Both settings apply to the whole structure and **cannot be changed while a craft is running** — switching either mid-job requires canceling the current craft first.

**Manual ingredient insertion** is always available as a standing fallback, independent of any automation rule — the player can walk up and load any machine by hand regardless of what the Core's resolver would do automatically.

**Running out of ingredients mid-request:** triggers the standard error (sound + GUI message). The player can load more eligible ingredients into the network and press **Continue** to resume exactly where the counter left off, or **Cancel** to abandon the remainder — whatever was already produced/delivered is unaffected either way.

**Amount-remaining counter (general rule):** any FL job with a target quantity displays a running amount-remaining counter in the Core GUI.

**Storage-first fulfillment:** any request first draws from whatever matching item/fluid already exists in connected storage before triggering new production; only the shortfall is actually crafted.

**Full network drain-to-storage, after every FL-driven job:** once any job finishes, **every machine in the network** — not just the one that ran the job — is cleared of both fluids and items, pulled into connected storage. The sole exception is the **designated result storage** (the Skin Tank + Craw flanking the Core), which holds finished output for the player to collect and is never auto-drained. This keeps every machine on the network fully stateless between jobs, reinforcing the FL's existing modularity — swapping or moving a machine never stranded fluid or items inside it.
- **Manual pull buttons:** the Core GUI also offers buttons letting the player force a drain-to-storage pass on demand, independent of the automatic post-job trigger.
- **Lockout on drain failure:** if a drain can't complete because no storage capacity is available, the Core throws the standard error (sound + GUI message) and refuses to start **any further crafting at all** — not just the job that triggered it — until the problem is resolved.
- **Recovery sequence (strict order):** player adds sufficient storage → the structural change triggers **Knitting** (see below) → once Knitting confirms the new capacity, the stalled drain completes → only then does the Core unlock crafting again.

**Machine access — direct, not through capabilities (Dermicraft machines only):** the FL reads and writes attached Dermicraft machines' inventories and tanks **directly**, bypassing the NeoForge capability layer rather than going through `getCapability`. Rationale: capabilities enforce *polite* access — sided rules and input/output slot restrictions — that the FL's fill-completely / drain-completely model specifically needs to ignore, so direct access removes a fight rather than just adding a shortcut.
- **Contract-backed, not raw field access:** direct access goes through a defined API on `MachineBaseBlockEntity` (accessor methods / a small interface — e.g. input handler, output handler, reagent tank, fuel tank) so every machine exposes the same stable surface and a new machine just implements it. Avoids brittle per-machine casts.
- **Machines still expose capabilities.** This changes only how the *FL* accesses them — each machine keeps its normal capabilities for standalone use with hoppers, pipes, and other mods.
- **Foreign blocks still use capabilities.** The rule: `instanceof MachineBaseBlockEntity` → direct path; anything else on the floor network (vanilla chest, another mod's tank) → standard capability path. This is the "at least for Dermicraft machines" scope.
- **Resolves the fluid-output-face conflict** (see Lab Floor) — direct access ignores block faces entirely, so the bottom-face-output-vs-Floor-block concern no longer applies.
- **Caveats:** `getBlockEntity` may return null (unloaded chunk / removed block); and since direct access skips slot validation, the FL honors input/output slot roles by its own contract rather than relying on the handler to reject a bad insert.

**Knitting** (renamed from "settling process"): the FL's network-graph recompute, triggered by any structural change (block placed/broken/moved) rather than running continuously every tick. Named for the mod's organic/body-horror theme — the network "knits" itself back together after a change, same metaphor family as the Sutured/Bloodletting status effects. This event-driven approach — recompute only on change, with a short debounce delay rather than a per-tick scan — is the main safeguard keeping the network-graph side of the FL cheap on the server.

### Imago Engine (name confirmed — "imago" = the final adult form after metamorphosis; working name was "Network Evolver")

**Status:** Concept designed in brainstorming; not yet implemented. Solves the mod-wide tier-upgrade tedium problem.

**The problem it solves:** most/all machine families follow the same tier progression, so a player with a built-out factory would otherwise re-run the evolution ritual on every machine individually each time a new tier unlocks. The first evolutions are the interesting teaching moment; the tenth is busywork. This machine keeps the first ones meaningful and batch-automates the repeats.

**What it is:** a **small multiblock** — a **controller** (the evolver itself) plus **special floor blocks** that reach connected **storage** holding the payment fluids (mini-FL architecture: controller + floor + storage, its own smaller system, no FL required). The player selects an **available target tier** on the controller; the machine then walks the connected network and evolves everything below that tier up to it, paying costs from its own reachable storage.

**Cost model — "cost in fluids":** the standard evolution recipe is already nearly all-fluid (Tier 2 template = 1000 mB Lava + a tank of Water + Evolution Catalyst; the Lava Bucket converts trivially), so the evolver charges each machine's evolution cost as fluids, reusing the established physical→fluid-equivalent convention. **Open:** exact parity — same cost as the manual recipe, or a premium/discount.

**Network walk:**
- **Connected = touching** (Knitting-Protocol-style adjacency graph walk). **Ducts count as connective tissue** — any machine touching a duct is in the walk.
- **Ducts (and Nodes) get evolved too** — they already have upgrade tiers planned in their design, so the wave upgrades the connective tissue along with the machines.
- **Order: closest to farthest** from the controller — the mod's established wave idiom (Mr. Farmer's rings/rows). The upgrade visibly ripples outward across the factory.
- **Anything at or above the target tier is ignored.** If storage can't cover everything, whatever is affordable gets upgraded (closest-first order determines which).
- **Deliberately time-spread, not instant** — the walk takes time, keeping it light on the server (same philosophy as Knitting's event-driven/debounced design).

**Evolution mechanics — materials never touch the network:**
- The evolver does all the work. Payment fluids are consumed from the evolver's own storage; **nothing is inserted into the target machines.**
- Each target block is simply **swapped for its upgraded version in place**, with its **inventory/tank contents preserved** (unlike the manual route, which consumes the target's tank fill as part of the ritual — same price, different plumbing).
- **Mid-process machines: wait until the current craft finishes, evolve, then let it continue.** No interrupted or lost work.

**Structural mastery gate (emergent, no knowledge system needed):** to *pay* a tier's evolution cost, the evolver's storage must *hold* that tier's fluids — and Tier 2 costs include Lava (`Extreme Heat`), which Tier 1 tanks reject via the hazard-profile system. So **the evolver must itself be evolved to Tier N before it can push anything else to Tier N** — the player's first evolution at every tier is forced to be manual (evolving the evolver), and only then does batch upgrading unlock. The "first ones stay manual, repeats get automated" gate falls directly out of [[project_hazard_profile_system]] with zero extra mechanics.

**Route coexistence:** every machine keeps its **manual upgrade recipe** and its **FL-craftable recipe** alongside this — manual = bootstrap, FL = automation route, evolver = the batch convenience layer. Consistent with the mod-wide machine-primary/bootstrap philosophy.

**Open questions:** Exact cost parity vs. the manual recipe. Floor-block specifics (shared design with FL Lab Floors, or its own simpler variant?). Whether ducts/Nodes have their own per-block evolution costs in the wave, and what they are. Controller GUI (tier selector + cost preview?). Range/size limits on the walk, if any.

### Filling Station — folded into the Mutator (see below)

**Status:** No longer a standalone machine. The original concept (a machine that fills fluid-handling items — Flasks, Syringes, Beakers, Bladders) has been **absorbed as the Mutator's "fill" mode** rather than built as its own block. See the Mutator entry below for the full design; the fill-mode section covers everything this concept was for. This entry is kept only as a pointer so the old "Filling Station" name resolves.

**Why folded in:** both machines want the same physical layout (item slot + fluid tank + output buffer) and both are conceptually "apply a fluid to an item to change it" — filling is just the gentlest version of that verb (an empty flask and a full flask are different item-states). A GUI mode toggle time-shares the two operations with no internal resource clash, so one block does both.

### Mutator

**Status:** Concept fully brainstormed and coherent; not yet implemented. Values (per-recipe reagents, yields, timings) not yet decided.

**What it is:** A crafting machine whose verb is **"apply a fluid to an item to change the item."** Takes fuel + a fluid + an item; a **GUI mode toggle** (reusing the existing item/fluid button pattern — swap between item-button and fluid-button, they're distinct enough) switches between two modes:

- **Mutate mode** (primary): fuel + **reagent fluid** + item → a **different, more complex** item. The reagent is *consumed*. Example: Fluid Bladder + Cuprous Blend → Fuel Bladder; Fluid Bladder + Protein Blend → Feeder Bladder.
- **Fill mode** (secondary — this is the absorbed Filling Station): fluid + a fluid-handling container item → the **same** item, now holding that fluid (the fluid becomes *cargo*, not fuel). No fuel/reagent cost. Filling a flask/syringe/beaker/bladder.

The two modes never run at once (toggle picks one), so they share the item slot, fluid tank, and output buffer without clashing. The fluid's *role* inverts between modes — **reagent (burned)** in mutate vs. **cargo (packaged)** in fill — so the GUI must make the current meaning of the tank unmistakable when the mode is toggled.

**Complexity pattern (NOT written as a hard law):** each mutate output is *slightly* more complex than its input — the machine tends to act as an "item complexity escalator." This is the observed pattern the recipes will take, **not** a declared rule; it may turn out to be a law in practice but is deliberately left un-lawed for now (don't design as if skipping tiers is forbidden).

**Reagent-mapping rule (mutate mode):** Mutator recipes are derived from an existing crafting-table recipe by mapping its ingredients, **Metastasizer-style** (nearest fluid analog to the replaced item):
- **Primary item → the input slot** (the thing being mutated, e.g. the Bladder).
- **Consumable secondary item → the nearest reagent fluid** (dense_muscle → Protein Blend; a metal/mechanism part → Cuprous Blend; etc.).
- **Tool secondary → absorbed by the machine, not mapped to a fluid.** A tool ingredient (e.g. the feeder bladder's suture_kit) has no fluid analog; its *function* is what the machine performs. The Mutator does the surgical assembly the suture_kit did by hand, so the tool simply drops out of the recipe. (Thematic beat: the machine does the surgery the kit used to.)

**Recipe roster (confirmed fits — reagents/yields still provisional):**
- **Fluid Bladder + Cuprous Blend → Fuel Bladder** (copper = mechanism).
- **Fluid Bladder + Protein Blend → Feeder Bladder** (Protein Blend replaces the table recipe's Dense Muscle; the suture_kit is absorbed by the machine).
- **Eye + Protein Blend → Eye Tumor**, **Nerve Cluster + Protein Blend → Nerve Tumor**, **Dense Muscle + Protein Blend → Muscle Tumor** — migrated from the Brain's FL-only candidate list (see "Brain — Tier 1 Control" below), each currently `1000 mB Protein Blend + the one defining part`. Textbook Mutator shape (single item + single flesh-analog reagent → a more-complex tumor block), and exactly the "hand an FL-only recipe to a dedicated machine" move the FL notes anticipated (the way F-Stuff/C-Stuff went to the Effluencer). Migrating them does **not** remove them from FL reach — the FL drives any connected Tier 1 machine's recipes, so they become Mutator recipes the FL can still invoke; their `early_implant` bootstrap recipes also stay.
- **Cobblestone + Crude Slurry → Mossy Cobblestone** — a *new* recipe, not migrated and not a parallel route (vanilla 1.21 has no crafting recipe for Mossy Cobblestone at all), so it's an additive production path. Crude Slurry (organic biomass) as the moss reagent is the most self-explanatory mapping in the roster. **Wrinkle:** Crude Slurry is also the machine's fuel, so this recipe feeds the same fluid into both the fuel tank and the reagent tank (two separate tanks, no conflict). **Cost: flat 10 mB Crude Slurry, implemented.** Other mods commonly gate their mossy-cobble recipe behind plain water — this stays on Crude Slurry (keeping the overgrowth family's reagent identity intact) but deliberately prices it as a *token* cost rather than a scaled one, since it's purely decorative. The earlier block-vs-slab volume-scaling idea (block/stairs/wall at 100 mB, slab at 50 mB) was dropped in favor of one flat number across the whole family.
  - **Mossy Cobblestone stair/slab/wall variants** — mirror the base recipe for each vanilla Cobblestone form: **Cobblestone Stairs + Crude Slurry → Mossy Cobblestone Stairs**, **Cobblestone Slab → Mossy Cobblestone Slab**, **Cobblestone Wall → Mossy Cobblestone Wall**. All decorative, all overgrowth-family, all at the same flat **10 mB**.
- **Stone Bricks + Crude Slurry → Mossy Stone Bricks** — same overgrowth operation as the cobblestone set, one tier of vanilla processing up (Stone Bricks instead of Cobblestone). Decorative, overgrowth-family, **flat 10 mB, implemented.**
  - **Mossy Stone Brick stair/slab/wall variants** — mirror the base for each vanilla Stone Brick form: **Stone Brick Stairs → Mossy Stone Brick Stairs**, **Stone Brick Slab → Mossy Stone Brick Slab**, **Stone Brick Wall → Mossy Stone Brick Wall**. Same flat **10 mB** treatment as the Mossy Cobblestone variants.
- **String + Crude Slurry → Vines** — a second "overgrowth" recipe (see family note below). Same organic-growth mapping (Crude Slurry animates the fiber into a living vine). Structurally the first arguably-*lateral* recipe (String → Vine is a transmutation, not a clear step up) — fine, since the complexity-escalator is a pattern not a law. **Cost intent: higher on Crude Slurry than Mossy Cobblestone**, for two reasons — the slurry is doing most of the work (String is a token input), and a Vine is a *self-propagating bootstrap*: once a player has one they can let it grow and harvest indefinitely, so the recipe is priced as a one-time investment rather than per-block. Exact mB TBD.

- **Botanical transmutation chain (overgrowth family, plant-to-plant wing):** **Sugar Cane + Crude Slurry → Bamboo**, then **Bamboo + Crude Slurry → Cactus**. The real payoff is **biome access**: sugar cane grows near any water, while bamboo (jungle) and cactus (desert) are biome-locked — the chain converts the universal farmable into the locked ones, sparing biome-less spawns a thousand-block expedition. Internal logic: water plant → jungle plant → desert plant, each step drier and hardier, Crude Slurry as the mutagenic feed. **Chain-only — no direct Sugar Cane → Cactus shortcut** (bamboo stays meaningful as the middle rung). Lateral transmutations like Vines (fine under pattern-not-law), and priced like Vines too: each output is a **self-propagating bootstrap** (one plant → farm forever), so each step costs **more Crude Slurry, as a one-time investment**. No exploit surface — all three plants are trivially renewable in vanilla already. Exact mB TBD.

**"Overgrowth" recipe family (documentation label only — NOT a code recipe type).** Mossy Cobblestone and Vines (and likely future entries — mossy stone brick, moss block, other plant/overgrown blocks) share a theme: **Crude Slurry as a life/growth agent that turns inert matter into overgrown/living blocks.** This is purely a design/doc grouping for talking about them as a set — in code they are ordinary Mutator recipes with no special "overgrowth" type. Pricing within the family is per-recipe, not uniform (contrast Mossy Cobblestone's cheap per-block cost with Vines' higher one-time bootstrap cost).

- **Silica Blend glassmaking family — Stained Glass (Mutator half), Stained Glass Panes (Metastasizer half). IMPLEMENTED, resolved from an earlier collision.** Original design wanted the Mutator to make both **Dye + Silica Blend → Stained Glass** and **Dye + Silica Blend → Stained Glass Pane**, but both recipes share the identical `{dye, fluid}` input pair — `OneFluidOneItemRecipeInput`'s fluid-amount matching is "at least N mB," not exact, so with both recipes registered the recipe manager would resolve to whichever it iterates first, arbitrarily. **Resolved by splitting the family across machines by verb instead of inventing new matching semantics:**
  - **Mutator makes ONLY the stained glass block** (`mutating_stained_glass_<color>`, ×16): Dye + 1000 mB Silica Blend → Stained Glass Block, 200 ticks. Silica Blend on its core silica→glass identity, dye = the color.
  - **Dyed Stained Glass Panes are a Metastasizer duplication recipe instead** (`metastasizing_stained_glass_pane_<color>`, ×16): 500 mB Silica Blend + the specific-colored pane as a non-consumed pattern → duplicate pane, 120 ticks. Keyed by the pane item itself, not the dye, so there's no collision — this is *why* panes live here rather than on the Mutator. The player bridges block→pane via vanilla's own 8-stained-glass → 16-stained-glass-pane table recipe (no Dermicraft recipe needed for that step).
  - **Plain (undyed) Glass and Glass Pane belong on the Metastasizer too** (same reasoning as Stitched Tumor — no dye means no input item to mutate, a pure fluid→item print), and **the plain-glass family is now built** alongside the dyed recipes (see the Metastasizer entry's glass-family table) — it had been wrongly marked "Confirmed" for a long time without ever actually being datagenned; caught and fixed in the same session.
  - So the glassmaking family spans two machines by verb: **Metastasizer duplicates (plain glass, plain panes, dyed glass blocks, dyed panes, Beaker, Glass Flask, Calcium Glass); Mutator transforms dye into the stained block.**
  - **Colored/Glazed Terracotta below has the identical collision** (dye + Clay Blend keys both) and is deferred pending the same kind of split.

- **Clay Blend ceramics family — Colored & Glazed Terracotta (Mutator half). IMPLEMENTED, resolved from the glass family's collision by a different, better fix.** Original design had both **Dye + Clay Blend → Colored Terracotta** and **Dye + Clay Blend → Glazed Terracotta** sharing the identical `{dye, Clay Blend}` input pair — the same ambiguous-match problem the glass family hit. **Resolved by changing Glazed's ingredient, not by splitting across machines:**
  - **Colored Terracotta** (`mutating_terracotta_<color>`, ×16): Dye + 1000 mB Clay Blend → Colored Terracotta, 200 ticks. Unchanged from the original design.
  - **Glazed Terracotta** (`mutating_glazed_terracotta_<color>`, ×16): **Colored Terracotta (the item, not the dye) + 500 mB Clay Blend → Glazed Terracotta**, 300 ticks. Colored and Glazed no longer share an ingredient (dye vs. the colored-terracotta item), so there's no collision — and this is *more true to the real process* than the original design: clay → terracotta → dye → **Colored** → fire again → **Glazed**. Glazed is made *from* Colored, not conjured from raw dye.
  - **Pricing logic for the ingredient swap:** Glazed's input already embeds Colored's own 1000 mB Clay Blend cost, so the *marginal* dose for firing-only is priced lower than a from-scratch estimate would need to be (500 mB, half of Colored's) — while total investment across the two steps together still ends up pricier than Colored alone, preserving the original "Glazed costs more" intent. **Time still runs longer** (300 vs. 200 ticks) independent of the ingredient change — firing takes longer, that half of the original decision holds regardless.
  - **Plain (uncolored) Terracotta is NOT a Mutator recipe** — no dye = no input item to mutate (same non-fit reason as plain Glass / Stitched Tumor). It lives on the **Metastasizer** instead (Clay Blend + Terracotta pattern → Terracotta, 1000 mB / 200 ticks). So the ceramics family spans two machines by verb, same split as glass: **Metastasizer duplicates plain Terracotta, Mutator colors/glazes it** (with Glazed now itself being a second Mutator step layered on Colored).
  - **Glazed has no uncolored form** — it only exists in 16 dyed colors. It's Mutator-only regardless of the ingredient swap (Colored Terracotta isn't a Metastasizer pattern for it — Glazed is a transformation, not a duplication).
  - Bulk add: 16 colors × 2 forms = 32 Mutator recipes, trivial to datagen by looping the dye set.

- **String + Protein Blend → Cobweb.** The most scientifically-literal reagent mapping in the roster — spider silk *is* protein (fibroin), so this is "re-spin the fiber into a web with more silk protein." Value: cobwebs are **structure-locked in vanilla** (not craftable, only shear-harvested from mineshafts etc.), so this makes them renewable for builders and mob-trap designs. Additive; standalone recipe (not overgrowth — no Crude Slurry). **Pricing note — the vanilla decomposition loop prices this recipe:** 1 Cobweb → 9 String already exists, so this recipe composes into "Protein Blend → 8 net String per cycle." Not dangerous (string is farmable, low-value), but the Protein Blend cost is effectively buying ~8 string worth of silk mass and should be set with that conversion in mind — or the Protein→String converter side effect consciously accepted as intended. Exact mB TBD. **Companion recipe:** the Metastasizer also duplicates Cobweb (Protein Blend, light tier — see the Metastasizer entry); this Mutator recipe is the *bootstrap* half of that loop (first cobweb from String), duplication is the cheaper steady-state.
- **Rotten Flesh + Calcium Blend → Leather.** Grounded in the real first step of leather tanning — **liming**, soaking hides in calcium hydroxide to strip hair, fat, and rot — so the calcium bath eats the rot and the remainder cures into leather (same factually-real justification class as Blood Nugget's hemoglobin iron and the silica/lime glass family). Turns vanilla's most useless mass-drop into the early game's classic bottleneck material (books). Also gives the underused **Calcium Blend** a real second job (every-ingredient-multiple-uses convention). Reagents considered and passed on: Primitive Catalyst (semantically legal but generic), Protein Blend (wrong direction — rotten flesh already *is* protein). Balance: zombie farms make the flesh free but the conversion is paid in Calcium Blend (bones aren't free), 1:1, no Fortune surface — a convenience, not an exploit. Escalator holds: waste → refined material. Exact mB TBD. **Companion:** Metastasizer Leather duplication (see its entry) is the steady-state loop; this is the bootstrap.
- **Eye + Primitive Catalyst → Spider Eye.** Species rewrite: a generic grown Eye transformed into a specific creature's, giving the tumor farm a night-hunting-free brewing-supply route (Spider Eye → Poison; Fermented Spider Eye → Weakness). **Reagent choice is doctrinal:** this recipe adds no material — it *changes what the thing is* — and identity-transformation is the Catalyst family's defined role (Blends = materials, Catalysts = transformation steps). Protein Blend was considered and rejected as the lazy default (it fits material-addition recipes like Cobweb/tumors, not species change). Primitive Catalyst being a *produced* fluid also prices the "big change, major convenience" appropriately by nature. First Catalyst-reagent recipe on the Mutator. **Reagent doctrine (general, for future recipes): specific material analog when one exists (Calcium Blend liming for leather); Catalyst when the operation is identity-change rather than material-addition.** **First de-escalating recipe** — the input Eye is the more complex/valuable item (250 mB Protein Blend to duplicate), so this spends a grown part to buy convenience rather than upgrading; fine under pattern-not-law, noted as the first bend in that direction. Balance self-secures (spider eyes are common mob drops; the Eye + Catalyst are the real payment). Exact mB TBD.
- **Aurous Blend gilding family — golden foods.** **Apple + Aurous Blend → Golden Apple**, **Carrot + Aurous Blend → Golden Carrot**, **Melon Slice + Aurous Blend → Glistering Melon Slice**. Dye-style reagent mapping: the gold becomes fluid, the food stays physical. Gives Aurous Blend its first consumer beyond the ingot/nugget storage loop. **Pricing anchors from vanilla at the established rates** (1000 mB/ingot, 110 mB/nugget): Golden Apple = 8 ingots = 8000 mB raw; Golden Carrot and Glistering Melon = 8 nuggets = 880 mB raw; with the machine-efficiency discount (75% convention) the working targets are **~6000 mB** and **~660 mB** respectively. Exact mB TBD.
  - **Blackstone + Aurous Blend → Gilded Blackstone.** Same gilding logic applied to a block: vanilla has no recipe (bastion-only), so this makes a structure-locked decorative block renewable (same value case as Cobweb). **Self-pricing via vanilla's own drop table:** Gilded Blackstone has a 10% chance to drop 2–5 gold nuggets when mined, so its canonical gold content is ≤5 nuggets — price at the top of that range, **~550 mB Aurous Blend**, and the decomposition loop self-closes (expected nugget return from mining the product ≈ 0.35 nuggets ≈ 38 mB per block, hopelessly under cost — no exploit, no discount needed). No stage complication: Aurous Blend is non-hazardous (Tier 1 Mutator), access gates naturally through mining Blackstone in the Nether (the Sediment blacklist's "Nether rocks deferred" note is about duplication rosters, not player-supplied inputs).
  - **Enchanted Golden Apple — deliberately excluded.** Not craftable in vanilla (loot-only; recipe removed ages ago), so a Mutator route would be a major balance decision rather than a reagent mapping — softer cousin of the ore-block exclusion's loot-locked-value reasoning. **Revisit clause:** could someday exist as a very late-stage, very expensive recipe (end-game-defining if ever added); not planned.
- **Cobblestone + Lava → Magma Block (STAGE 2 — requires a Tier 2 Mutator).** The Mutator's first Stage 2 / lava recipe. Fits the shape (item + reagent → item, Magma Block has a normal item form) and the "Stage 2 = forged with heat" identity (lava fuses into the stone). **Gating (same wall as the Molten crafting-fluid family):** Lava is `HAZARDOUS → Extreme Heat`, which the Tier 1 machine tanks reject (`HazardProfile.TIER_1` / `VulnerableTank`), so this can only run on an evolved **Tier 2 Mutator** — which doesn't exist yet. **Do not implement ahead of the Mutator's Tier 2 evolution**, mirroring the "no Molten recipe before a Tier 2 Masticator" rule. Establishes that the Mutator gets the standard Tier 2 evolution like every other machine family ([[project_machine_family_tier_architecture]]). First member of a potential lava-based "molten transformation" cluster (more may follow).
- **Glass + Molten Amethyst → Tinted Glass (STAGE 2 — requires a Tier 2 Mutator).** Second member of the Stage 2 cluster, and the Stage 2 extension of the glassmaking family (Silica Blend handles Tier 1 stained glass; Molten Amethyst handles Tier 2 tinted). Textbook reagent-mapping of vanilla's recipe (4 Amethyst Shards + 1 Glass → 2 Tinted Glass): the shards become their fluid form, the glass stays physical. Same gating as Magma Block — Molten Amethyst carries Extreme Heat (family-wide lava rule), so Tier 1 tanks reject it; **do not implement ahead of the Tier 2 Mutator.** **This is Molten Amethyst's first confirmed consumer** — a real start on its open "what distinguishes it from Molten Quartz" identity question (answer forming: light manipulation). **Pricing anchor:** vanilla = 2 shards + ½ glass per tinted glass, so ~2 shards' worth of Molten Amethyst per glass minus the usual machine-efficiency discount; exact mB TBD (Molten Amethyst's own yields aren't decided yet).
- **Blaze Powder + Ender Essence → 2 Eyes of Ender (STAGE 2 — requires a Tier 2 Mutator; unblocked by the Mind Rule).** Reagent-mapping of vanilla's Eye of Ender (Blaze Powder + Ender Pearl): the pearl becomes its fluid form (Ender Essence is literally liquefied Ender Pearl), the blaze component stays physical. **Yield: 1 Blaze Powder + 2 pearls' worth of Ender Essence → 2 Eyes per cycle** — double vanilla's per-powder yield, the machine-efficiency payoff expressed on the item side. No progression circularity: vanilla hand-crafting of Eyes stays (parallel-route philosophy), so this is convenience, not a gate. **Gating, resolved by the Metaphysical Mind Rule** (see `dermicraft-hazard-effects-notes.md` → Metaphysical vs. machines): the Mutator is a *dumb* machine (no Brain, no mind), so Ender Essence's `Metaphysical Severe` tag passes through it harmlessly — only its `Extreme Heat` tag gates, requiring the Tier 2 Mutator. Lands cleanly at **Stage 2** alongside Magma Block and Tinted Glass (the Eye is the pre-End bridge item, matching Ender Essence's own when-you-can-acquire-it Stage 2 placement). **This is Ender Essence's first confirmed consumer**, and the recipe that motivated designing the Mind Rule. Exact mB TBD (Ender Essence's own yields aren't decided yet).

**Explicit non-fits (checked against the FL roster, do not force into the Mutator):**
- **Stitched Tumor** (950 mB Protein Blend, *no input item*) — a pure fluid→item print; the Mutator needs an item to mutate. Stays FL-native.
- **Most of the six FL-native machine recipes** (Metastasizer, Effluentcer, Drooling Cauldron, Masticator) — each needs **two** fluids (Protein Blend + Primitive Catalyst binding agent) plus a structural item and is "born from an Inert Tumor." That two-reagent combine breaks the Mutator's single-reagent-tank identity; they belong to the FL's combine method. **Craw and Skin Tank are the confirmed exceptions** — simple enough (one structural item each, no second reagent needed conceptually) to also support a single-fluid Mutator alternate route, both priced identically: `mutating_craw` (Chest + 2500 mB Protein Blend → Craw) and `mutating_skin_tank` (Beaker + 2500 mB Protein Blend → Skin Tank), both parallel to (not replacing) their own implant recipes.
- **Ore blocks (decided: not doing it — but door deliberately left ajar, see below).** No Mutator recipes producing ore blocks (e.g. Stone + Ferrous Blend → Iron Ore). Two reasons: (1) it trivializes the Metal Blends' Ore-tier economy the same way the Sediment Blend blacklist already guards against — ore blocks are valuable *because they're mined, not made* (Silk Touch effort as its own reward axis); (2) the **Fortune loop can't be priced away** — an ore priced at its Masticator value (1500 mB Ferrous) mined with Fortune I returns up to 4000 mB via Raw items (Copper is far worse), and any price high enough to close the loop makes the recipe pointless. General screening rule: **anything Fortune/loot-multiplication can act on is a value container, not a craft target.**
  - **Revisit clause:** if balanced the right way, ore-crafting could genuinely reward players for building intricate farming systems — so this may be **added later upon enough player request**, as a deliberate, carefully-balanced feature rather than a casual recipe. Not planned; just not permanently forbidden.
- **Blocks with no item form** (e.g. **Farmland**) — the Mutator outputs finished items into a buffer, so any block that exists only as a world block-state and has no BlockItem (Farmland drops Dirt, can't be held/given) simply has nothing to output. General rule for screening creative-tab candidates: if it can't sit in an inventory slot, it can't be a Mutator output. (Farmland specifically is also already covered in-world by Mr. Farmer's hydration/tilling.)

**Parallel route, not a replacement — but effectively a soft gate.** Mutator recipes sit **alongside** the crafting-table recipes (where one exists; the Mossy Cobblestone recipe has no table counterpart at all). The table version isn't removed. But per the mod-wide machine-primary crafting philosophy (machines are the real crafting layer, table/puddle/implant are bootstraps), the Mutator route is intended to become strictly better once available:
- **Automation (primary payoff):** the Mutator is duct/Node/Gate-feedable, so it's the only way to auto-produce these items in a factory setup.
- **Material efficiency (secondary payoff):** most Mutator recipes are meant to be a little cheaper than hand-crafting, because the reagent is "printed onto" the item rather than materials being wasted in manual assembly. Example floated: **750 mB Cuprous Blend** for a fuel bladder instead of the full hand-craft material cost.
So once a player has a Mutator they'd realistically never hand-craft these again — that dominance is intended, not a balance bug (the table recipe is just the pre-machine on-ramp).

**Fuel / HP behavior:** follows the standard fuel-optional/HP machine template (same as the Masticator). HP rules apply to **both** modes:
- **Mutate** consumes fuel/reagent and runs under the normal HP mechanic.
- **Fill** costs no fuel or reagent, but is still gated by HP: **no filling while HP is 0 or recovering from 0**, and filling may be **slower while recovering from HP > 0**. (Exact numbers TBD.)

**Tier / hazard:** processes an item + a blend, no lava/heat — a clean **Tier 1** machine like the Masticator with metal blends. Both modes **respect hazard profiles** on the fluid tank like every other machine ([[project_hazard_profile_system]] in Claude Code memory).

**Architecture fit:** the fuel + reagent-tank + item-slot + output-buffer shape is already what `MachineBaseBlockEntity` supports; build it on the machine family/tier bases like the other machines.

**Recipe architecture (decided) — one custom recipe type, not two.** The GUI mode toggle selects which *behavior* runs, and only Mutate is recipe-driven:
- **Mutate mode → a custom `MutatingRecipe` type** (under `recipe/mutating/` with its paired `RecipeInput` record + serializer, registered in `ModRecipes`, following the mod's standard recipe pattern). Hand-authored item + reagent-fluid → output transformations.
- **Fill mode → a generic capability operation, NOT a recipe type.** Fill just pushes fluid into whatever the item's own fluid-handler capability accepts — so any fluid-holding container works (Bladders, Flasks, Syringes, Beakers, *and* modded containers) with zero per-pairing authoring. This also composes with the liquid-foods system, where the Feeder Bladder's `edible_fluid` data map already governs what it accepts. The hazard profile still gates the tank.
- **Why the toggle disambiguates cleanly:** because the mode picks the behavior, an item+fluid combo can never accidentally fire a mutate when the player wanted a fill (or vice versa) — no overlap-resolution logic needed. (Design instinct noted: "machine ⇒ recipe" is the natural reflex, but fill is better modeled as an item-capability interaction than a recipe.)

**Build spec (decided — ready to implement):**
- **Implant recipe:** copy of the Metastasizer's with the defining item swapped — **1 Chest + 2 Dense Muscle + 4 Nerve Cluster + 1 Eye**, sutured, injected with **100 mB Primitive Catalyst**. The Chest fits the machine's identity (the item goes into the box and comes out changed — chrysalis flavor).
- **FL-native recipe (derived by the standing formula):** 1 Chest + **2060 mB Protein Blend** ((2 DM + 4 NC + 1 Eye) × 250 + 1000 Inert Tumor = 2750 × 75% = 2062.5 → rounded down) + 100 mB Primitive Catalyst. Same flesh math as the Metastasizer. Chest stays physical the same way as the Craw's (its own Metastasizer route — 2000 mB Pulp Blend — sources it, not a hand-supply requirement, corrected 2026-07-19).
- **Tanks/slots:** fuel tank + reagent tank (5000 mB each, Masticator convention); 1 item input slot; **single-slot output buffer**.
- **GUI:** copy of the Metastasizer screen, plus the **craft/fill mode toggle button directly above the input item slot** (reusing the existing item/fluid button pair — swap between them on click).
- **Craft-time convention:** Metastasizer tier timing as the default (light 50t / aggregate 120t / solid 200t), with documented per-recipe exceptions (e.g. Glazed Terracotta longer). mB values set per-recipe at datagen time against the anchors in the roster above — functional-not-final, Effluentcer precedent, flagged for a later balance pass.
- **Fill mode rate: 250 mB per cycle**, with an **invisible accumulation buffer** for rigid-volume containers: if the target container can't accept a partial fill, cycles accumulate 250 mB into the hidden buffer until the target amount is gathered, then the container's fluid handler is filled in one atomic operation. **While recovering from partial HP, fill follows the established crafting recovery rule — 0.1× the standard rate (25 mB/cycle)**, accumulating through the same buffer; no fill at all at HP 0 or while recovering from 0.
- **HP/fuel:** Masticator standard (200 max HP, 1 HP/cycle unfueled while processing, 0.1× unfueled/recovery speeds).
- **GUI access — opens directly, no Outerface required (built).** A plain empty-hand right-click on any face opens the menu directly (`useWithoutItem`, same pattern as Mr. Farmer). **Quick-extraction is a crouch action:** crouch + empty-hand right-click pulls from the face's slot (sides = input slot, bottom = output slot); if the slot is empty, the crouch-click falls through to the GUI like a plain click. The block still carries `HAS_SCREEN`, so the Outerface continues to work as an alternate route.
- **Visual state — three-way face texture (built), not just an on/off toggle.** A `STATE` blockstate property (`MutatorVisualState`: IDLE / RUNNING / RECOVERING) drives which of three textures renders — `mutator_face` (idle), `mutator_face_on` (actively processing in either mode), `mutator_face_error` (HP below max, i.e. "recovering"). **Recovering takes priority over Running** — a damaged machine shows distress even if still limping through a cycle. Computed every tick-cycle (piggybacked on `tickHealing()`, the one hook the engine calls unconditionally regardless of crafting state) and only re-set on the block when it actually changes, preserving the BE (same idiom as Mr. Farmer's `ACTIVE`). **Debounced:** a new state must hold for 2 consecutive cycles (~1s) before the texture commits — kills strobing on borderline conditions (e.g. hopper-fed input arriving every 8 ticks against the 10-tick cycle flapping RUNNING/IDLE) and avoids wasted setBlock/re-render churn; a one-cycle blip never flashes the face. Debounce state is transient, not NBT-saved.

**Open questions (remaining):**
- Whether the complexity-escalator pattern eventually becomes a hard law.
- The precise HP-vs-fill interaction (does fuel-in-tank prevent HP drain during fill even though fill doesn't consume fuel as a reagent?). (The recovery slowdown itself is resolved: 0.1× standard rate, per the established crafting rule.)
- Final GUI treatment that keeps the tank's reagent-vs-cargo meaning unmistakable across the mode toggle (the toggle button placement is decided; the tank-label presentation isn't).
- Whether any recipes need more than one secondary consumable (would strain the single-fluid-tank model — the tool-absorption rule handles tools, but two *consumable* secondaries would not map cleanly).
- Final per-recipe mB values (provisional at build time, deliberate balance pass later).

---

### Render Furnace and Grafting Table (early-game convenience machines)

**Status:** Both implemented and playtested.

**What they are:** Deliberately minor, single-instance, Tier 1-only machines automating vanilla's smelting and crafting-table loops — **the mod's first machines that run on vanilla's own recipe books rather than a Dermicraft-owned recipe type.** Positioned explicitly as **early-game training wheels, not a parallel production system**: they use whatever vanilla (or other mod's) smelting/crafting recipes already exist, with zero Dermicraft-side datagen, and are intended to become "almost useless" once the player has the real machines — a Masticator/Metastasizer/Mutator route for the same output is meant to always end up cheaper and duct-automatable, so nothing about the mod's actual economy routes through these two once better options exist. They exist purely to smooth the earliest window (the flagship case: **scalpels wear out fast, and there's currently no way to keep a supply going before the player has a Metastasizer**).

**Why full vanilla recipe books, not a curated whitelist (design debate, resolved):** the original pitch was a hand-picked Dermicraft recipe type covering only a few tool recipes, to avoid two risks — (1) diluting the mod's "get away from vanilla crafting" identity, and (2) a generic auto-crafter flattening the entire bootstrap-vs-machine tiering in one stroke (since it wouldn't be scoped to any one machine family the way every other bootstrap→machine relationship is). **Resolved: both machines get the full vanilla recipe book after all**, because the single-instance/no-evolution/"almost useless later" framing changes the shape of the risk — these aren't a competing permanent production route, they're a transient convenience that real machines out-compete on cost and automation the moment they exist. The tiering isn't flattened; it's just given a soft, generically-useful floor under it for the earliest game.

**No HP mechanic — both machines, hard fuel gate (revised from the original "standard template" plan).** Neither uses the usual fuel-optional/HP-drain template every other machine has. Instead: a new `MachineTier.NO_HEALTH` (0 max HP) plus an override on `hasCraftingInputs()` requiring fuel to be present — so an unfueled machine **freezes progress in place** rather than limping along at the standard 0.1× trickle while taking damage. No HP bar in either GUI at all. Fuel formula (speed multiplier from the fuel's own stats × tier multiplier) is otherwise unchanged — any biofuel works, faster fuel just means faster crafting/smelting, exactly like every other machine.

**Render Furnace** (the auto-furnace):
- Resolves against vanilla's own `RecipeType.SMELTING` via a `SingleRecipeInput` — 1 input slot, 1 output slot, fuel tank, no reagent tank.
- **Time comes from the recipe itself** — vanilla `SmeltingRecipe` already carries `getCookingTime()` (200 ticks default), scaled by fuel speed like every other machine's `ticks`. No invented duration needed.
- **Visual state:** simple two-texture swap (`render_furnace_face` / `render_furnace_face_on`), a plain `BooleanProperty ACTIVE` — same idiom as Mr. Farmer's, not the Mutator/Masticator/Effluentcer's 3-state debounced `VisualState` system (that pattern is specifically for HP-recovery signaling, which this machine doesn't have).
- **4-way horizontal facing** (like every other directional machine) — front face shows the ACTIVE texture, `skin_tank_end` on the other 5 faces (placeholder pending dedicated side/top art).
- Output auto-pushes to a valid inventory directly below every 5 seconds (`drainOutputs`, same cadence as every other machine) — no hopper required, though one still works.
- GUI: input/arrow/output in the middle, fuel tank + its own bucket-fill slot on the far right — same coordinate convention as every other machine's screen, no HP bar.

**Grafting Table** (the auto-crafter):
- Resolves against vanilla's own `RecipeType.CRAFTING` via a `CraftingInput` — a real **3×3 grid, 9 input slots**, structurally new (every other machine has 1-2 item slots). Works with any vanilla or other-mod crafting-table recipe, no Dermicraft recipe authoring needed. `CraftingInput.of(width, height, items)` / `CraftingRecipe.assemble`/`getRemainingItems` all matched assumptions exactly on the first compile — no API surprises.
- **Ingredient consumption uses the recipe's own `getRemainingItems(input)`**, not a blind per-slot shrink — vanilla crafting has container-return recipes (a bucket recipe returns the emptied bucket rather than consuming it), and `getRemainingItems` is exactly the API that already knows this.
- **Not instant, unlike vanilla crafting or the 1.21 Crafter block** (both are truly instantaneous — `CraftingRecipe` carries no time field at all). Duration is **invented, not read from data**: `40 + 20 × non-empty-ingredient-count` ticks, scaled by fuel speed — a 1-ingredient recipe takes ~3s at speed 1.0, a full 9-ingredient recipe ~11s. A tuning knob, not a fact; revisit after playtesting.
- **No FACING property at all** — unlike every other machine, a 3×3 grid has no directional "front" to orient (vanilla's own crafting table doesn't rotate either), so placement is unconditional.
- Automation: all 9 grid slots exposed as one flat item handler on every side face (same model vanilla's own Crafter uses for hopper-feeding) — no smart ingredient-to-slot matching. Top = fuel, bottom = output (auto-pushes below every 5s, same as the Render Furnace).
- **No visual ACTIVE state** — only a top texture (`grafting_table_top`) exists so far, no on/off pair; sides/bottom use `skin_tank_end`. Can gain a lit-state swap later if art is made for it.
- GUI: the one genuinely new screen layout in the mod — a literal 3×3 grid of slots (top-left area) + progress arrow + output slot + fuel tank/slot on the far right. No HP bar.

**Implant recipe — deliberately NOT the standard shape.** Every other machine's implant is 1 structural item + flesh parts (Dense Muscle/Nerve Cluster/Eye) + Primitive Catalyst, sutured. These two skip the flesh entirely: **just the vanilla Furnace or Crafting Table itself, sutured, injected with 100 mB Primitive Catalyst.** No flesh cost at all. This isn't an oversight — it's a deliberate signal: these are barely-modified vanilla objects, so their "birth" doesn't need much biological investment, just enough tumor-vessel + suture + catalyst to bring them to life. The cheapest implant in the mod, matching their "makes the beginning easier, almost useless later" role. **No FL-native recipe** — minor enough that it doesn't need one.

**Naming:** Render Furnace ("rendering" — real-world term for melting down fat/tallow, a genuine smelting-adjacent synonym, reads as body-horror without straining). Grafting Table (skin-graft imagery, reinforces the mod's existing surgical vocabulary — Suture Kit, Stitched Tumor — without introducing a new metaphor).

**Tier:** Tier 1 only, no evolution line for either — single, permanent instances.

**Playtest results (both confirmed working):** implant crafting, direct GUI-open (no Outerface needed), crouch-to-extract, correct smelting/crafting output, hard fuel-stop-and-freeze behavior, ACTIVE texture swap (furnace), fuel-slot stack-safety (only 1 container fills/consumes from an inserted stack — the mod-wide tank-slot fix holds here too), hopper/duct automation, output auto-push to a valid inventory below, and NBT persistence across reload.

**Open questions:**
- Grafting Table's duration constants (`40 + 20/ingredient`) are a first guess, not balanced.
- Whether the Grafting Table's 9-slot automation ever needs smarter per-slot item routing, or flat-handler-for-any-side stays sufficient.
- Dedicated side/top texture art for the Render Furnace and side/bottom art for the Grafting Table (both currently reuse `skin_tank_end`).
- Whether the Grafting Table ever gets a lit/active texture pair to match the Render Furnace.

---

### Render Kiln (implemented, 2026-07-20)

**Status:** Built and verified in-game — crafting, GUI (all slots/gauges), HP bar, and idle/on/error textures all confirmed working. See `dermicraft-render-kiln-build-plan.md` for the implementation record.

**Bug hit and fixed during bring-up:** `FluidOnlyRecipeInput` (the new zero-item `RecipeInput` this machine introduced) never matched any recipe even with the correct fluid loaded, because `RecipeInput.isEmpty()` defaults to an item-based check that short-circuits to `true` whenever `size() == 0` — and `RecipeManager.getRecipeFor` uses `isEmpty()` as an early-out *before* ever calling `matches()`. Fixed by overriding `isEmpty()` to check the fluid directly, mirroring the identical fix already in `TwoFluidRecipeInput` (used by the Effluentcer, which hit the same bug earlier). See [[feedback_item_less_recipe_input_isempty]].

**What it is:** A furnace-style machine that cooks a **fluid alone** (no item input) into a **default item** — the fluid's "natural solid form." Genuinely new shape, distinct from every existing machine:
- **Not the Render Furnace** — that's item→item via vanilla's `SmeltingRecipe`. This is fluid→item, which vanilla has no recipe type for at all, so it needs its **own Dermicraft recipe type** (same family as Masticator/Metastasizer/Mutator's recipe types), unlike the Furnace/Grafting Table pair which got to freeload on vanilla.
- **Not the Metastasizer** — that's fluid + a *player-supplied pattern item* → duplicate of that pattern. The Kiln has no pattern slot; the output is fixed per-fluid, not "whatever you show it." Its real differentiator from the Metastasizer's existing reverse-duplication recipes (Ingot/Nugget, Sediment Blends, Carbon Blend→Coal, etc.) is exactly this: no pattern item to already own, which matters for unattended automation loops.

**Decided:**
- **Slot layout confirmed:** fuel tank + fuel slot, 1 fluid input tank (renamed `INPUT_TANK`, not "reagent" — the fluid *is* the whole recipe) + its own fill slot, 1 output item slot. No reagent tank, no second item input, no pattern slot at all.
- **Fuel/HP: standard template** (Masticator convention — fuel-optional, HP-gated, unfueled trickle) — explicitly **not** the newer `NO_HEALTH`/hard-stop pattern used by the Render Furnace/Grafting Table pair.
- **Name: Render Kiln** — reuses "Render" from the Render Furnace's own naming logic (reducing something to its base material via heat); this machine does the same thing in the opposite direction (fluid rendered down into its default solid). Backups considered: Curing Kiln, Sediment Kiln (narrower — only fits if scope ends up limited to the Stone/Silica/Clay Blend family specifically).
- **Tier 1 launch roster confirmed, mirroring the Metastasizer's existing reverse-recipe mB/ticks exactly (no discount — the missing pattern requirement is the reward on its own):**

  | Fluid | Default item | mB | Ticks |
  |---|---|---|---|
  | Stone Blend | Stone | 1000 | 200 |
  | Silica Blend | Sand | 750 | 120 |
  | Clay Blend | Clay Ball | 250 | 50 |
  | Ferrous Blend | Iron Ingot | 1000 | 200 |
  | Cuprous Blend | Copper Ingot | 1000 | 200 |
  | Aurous Blend | Gold Ingot | 1000 | 200 |
  | Carbon Blend | Coal | 112 | 50 |
  | Calcium Blend | Bone Meal | 334 | 50 |
  | Protein Blend | Meat Flavored Meat | 900 | 160 |
  | F-Stuff | MRE | 900 | 160 |

  **Deliberately excluded:** Crude Slurry — confirmed, no solid form defined anywhere in the design notes.

- **Implant recipe confirmed:** 2 Dense Muscle + 2 Nerve Cluster + Furnace + Bucket, sutured, injected with 100 mB Primitive Catalyst. **Alternate recipe:** same ingredients with Beaker swapped in for Bucket — two implant routes to the same machine, a shape not used elsewhere in the mod yet.

**Open questions:**
- Whether Crude Slurry ever gets a Kiln recipe if a genuine solid form is defined for it later — currently a deliberate gap, not a locked exclusion.

### Tier 2 Render Kiln (design only, not yet built)

**Status:** Design session confirmed the concept and an initial recipe roster; not yet implemented. Grew directly out of a fluid-roster change — see below.

**What it is:** The Tier 2 evolution of the Render Kiln — **two fluids in, one item out**, no item/pattern input at all. A natural evolution step for the family: the Tier 1 Kiln is fluid-only → item, this is simply the two-input version of the same shape, the same way the Effluentcer's own tier line goes 2-input → 3-input (Living Catalyst) → 4-input (All Metal). Any two-fluid recipe belongs here by definition — the machine's whole identity *is* the two-fluid input, so there's no separate "should this be Tier 2" gating question the way there sometimes is elsewhere; if a recipe needs two fluids, it's Tier 2 Kiln, full stop.

**Origin — replaces Molten Obsidian.** This design session started as a proposal to cut Molten Obsidian entirely: its documented identity was deliberately "no divergence from Lava at all" (see `dermicraft-crafting-notes.md`), which made it the one member of the Molten family with no real mechanical trick — a passive fluid rather than something that does anything. Real obsidian formation in Minecraft (and reality) is water meeting lava, so a genuine two-fluid Kiln recipe using that exact pairing is a strict upgrade over a static fluid bucket: it's more thematically accurate, and it gives Stage 2 a new machine tier instead of just another roster entry. Confirmed: Molten Obsidian is cut (fluid + all registration removed from code and docs).

**Cobblestone/Stone differentiation — explicitly rejected.** Real vanilla obsidian formation is directional (still water + lava source = Obsidian; still lava + flowing water = Cobblestone), but a two-fluid recipe has no way to encode that distinction — there's no "flow direction" input to key off of. Confirmed: the Kiln only ever produces Obsidian from Water + Lava, no Cobblestone variant. Not a gap — the mod already has an easy, established Cobblestone source (Stone Blend duplication), so nothing is lost.

**Recipe roster (confirmed, not yet implemented):**

| Fluid A | Fluid B | Result | Reasoning |
|---|---|---|---|
| Water | Lava | Obsidian | Direct citation of real obsidian formation; replaces Molten Obsidian. |
| Water | Molten Redstone | Redstone Block | Real metallurgical quenching (rapid water-cooling) applied to the family; a tidier bulk-output route than the existing Torch Dip. |
| Water | Molten Netherite | Netherite Ingot | Same quenching logic; gives Molten Netherite a second use beyond the Living Netherite chain. |
| Protein Blend | Stone Blend | Netherrack | Meat + rock, no fire — reads as organic corruption fused into stone rather than a cooking metaphor, fitting the Nether's alien/diseased-landscape feel. Confirmed to require the Tier 2 Kiln specifically (not gateable any other way, since it's a two-fluid recipe by definition) — this is what finally pays off the long-standing "Netherrack deferred to a later Stage" note on the Sediment Blend blacklist (see `dermicraft-crafting-notes.md` → Sediment Blends), rather than leaving it open indefinitely. |

**Considered and rejected:**
- **Water + Molten Quartz → Glass** — redundant, the mod already has good established Glass routes (vanilla smelting, Beaker/Flask recipes).

**On hold, not committed:**
- **Blaze Essence + Ghast Essence → Fire Charge** — opposite-natured Essences (fire vs. sorrow) combining into vanilla's own fire-charge item. Held pending further research into the concept before locking it in.

**Open questions:** Full technical implementation (new two-fluid-in/item-out recipe type — likely a fluid-only variant paired with `TwoFluidRecipeInput`'s existing zero-item-check fix, see [[feedback_item_less_recipe_input_isempty]]; block/BE/menu/screen; mB and tick values for each recipe; datagen). Whether more recipes get added to the roster before or after implementation.

---

## Farming automation concepts (early planning)

Four related ideas from early planning, sharing a personified-name pattern (Mr. Farmer, Mr. Shepard, Mr. Logger, Mr. Hunter) and the same status: preserved for future consideration, not committed as finished concepts, placement within the mod's Stage/Tier timeline undecided for all four.

### Shared mechanics (starting point, not locked)

- **Visual design:** shared body model with per-block textures to begin with; individual models later.
- **Range/placement:** starts small (3x3), grows under certain conditions.
- **Fuel-driven range (replaces fuel-optional/HP pattern):** unfueled, range is just the single block the machine is placed on/above. Range is keyed directly off a new `BiofuelProperties.tier` field (added as the last property in the record, alongside `speed`/`useRate`/`heal`; Crude Slurry is tier 1) rather than derived from the `getSpeed()` ratio — tier is a discrete, explicit value so fuels don't need near-identical speed values to land in the same bucket. Each tier adds one ring: tier 1 = 3x3, tier 2 = 5x5, tier 3 = 7x7, tier 4 = 9x9. Capped at 9x9 — a 5th (or higher) tier fuel is expected to exist but doesn't grant more range once capped. `ModFluidUtil.getTier()` / `FuelTank.getFuelTier()` expose the value.
- **Output handling:** internal buffer, player-accessible, drained by automation (e.g. Node/duct). If the buffer is full, items drop on the ground instead.
- **Stage/tier timing:** confirmed to release together as a set. **Open question:** which tier/stage.
- **Hazard interaction (resolved): the farming family never handles hazardous materials, full stop.** They consume ordinary biofuel slurry and produce crops/livestock/wood drops — nothing on either side of the machine is hazardous. The old "nearby hazardous fluids may disrupt their operation" idea is **dropped**, not deferred. This is a defining property of the family rather than an omission, and it has a direct architectural consequence: see the tier note below.
- **No machine-tier ladder, because of the above.** What a `MachineTier` ladder buys elsewhere in the mod is largely *hazard tolerance* — climbing tiers to safely handle worse fluids. A family that by definition only ever touches non-hazardous materials has no such ladder to climb, and **fuel grade is already its progression axis** (driving range, population cap, growth, pacing and cost). Adding tiers on top would be a second, redundant progression track. **Revisit condition:** if the farming machines ever gain hazardous-material handling, tiered variants become worth reconsidering — but there are no plans for that.

### Mr. Farmer

**What it is:** Conceived as a machine that vaguely resembles a human head in pain. Placed in the middle of, or above, a garden plot. Automatically keeps the garden hydrated and lit, and harvests/replants crops — a full auto-farm device for crops.

**Mechanics (starting point):**
- **Hydration:** the machine itself hydrates tiles in range — no vanilla water-source requirement. In-fiction, it runs off the nutrition in its fuel slurry and waters farmland with the water left behind as byproduct.
- **Lighting:** actively lights the area in range (not just a spawn/growth check bypass).
- **Crop scope:** any crop tagged `c:crops` (NeoForge's common-tags convention) — leans on the existing cross-mod convention rather than a Dermicraft-specific tag, so any actively-maintained crop mod that supports common tags works with zero extra effort.
- **Growth acceleration (fuel heal rate):** repurposes the fuel `heal` stat — dead weight on Mr. Farmer since it dropped HP for range — as a growth-speed multiplier. Each immature crop/stem the wave passes gets `getHeal() × GROWTH_ATTEMPTS_PER_HEAL` extra vanilla random-ticks on average — a fractional rate (whole attempts + a probabilistic extra for the remainder), not a flat rounded int, so weak fuel can average under 1 extra tick per visit. **Tuned down from an initial 2 (way too strong — Crude Slurry crops grew as fast as they could be harvested) to 0.3** as a base rate; better fuel (higher heal) scales the boost up from there, so the intended "overpowered later, restrained early" curve holds. Random-ticking respects light/moisture/fertility (all of which the machine already maxes), so it reads as faster natural growth, not a forced age jump, and works with any `CropBlock`. Folded into the existing per-step fuel burn (no surcharge). Thematically: the nourishing slurry that waters the field also feeds the crops. Rate is a tuning knob, still open to further adjustment during playtesting.
- **Orientation drives range direction:** the block uses full 6-way `FACING` (placed against any surface). The mounted surface determines which way the working area extends — floor mount projects the area outward on the ground, ceiling/wall mounts are supported (a case the design explicitly wants). Face points in the FACING direction (blockstate rotations hand-authored, verified in-game).
- **Range shape & wave (revised — the working footprint is always a square N×N sized by fuel tier; facing changes where it sits, how it scans Y, and the wave order):**
  - **Horizontal facing (N/S/E/W, "facing forward"):** the N×N field projects *in front of* the machine (not centered) — N wide × N deep, starting 1 block ahead in the facing direction. Vertical tolerance is **±2 blocks on Y around the machine's own plane = 5 candidate planes**; per column, scan that window and act on the *first valid* farmland/crop target (uneven-ground tolerance). Wave sweeps **row by row**, nearest row first, marching away from the face.
  - **Vertical facing (up/down):** the N×N field is centered on the machine's X/Z on a horizontal plane offset along the facing axis (below for `down`, above for `up`). **Scan along the facing axis up to 5 blocks and stop at the first valid plane** (a single working plane). Wave is **concentric square rings**, inner to outer ("circular" refers to this ring ordering only — the footprint stays square).
  - The Y numbers are intentionally asymmetric (vertical 5, horizontal ±2/5-plane) because vertical mounts sit far from their field while horizontal/wall mounts sit near it.
  - **Reachability:** "scan and stop at first valid" must resolve to a plane the machine can actually reach, so it can replant — one reachable target per column (horizontal) or one plane (vertical).
- **Output buffer:** 9 slots, single row. Player-accessible (pull by hand), drained by automation (Node/duct). Buffer full → harvested items drop on the ground (shared overflow rule). Sized to be forgiving for a typical garden while still pressuring automation at higher ranges.

**GUI plan (starting point):** composited `screen_parts` layout following the Masticator template ([[feedback_machine_gui_composited_layout]]), but this is a collector, not a processor, so no progress arrow and no HP bar (HP is replaced by fuel-driven range family-wide). Elements:
- Fuel fluid gauge + a fuel-container input slot (bucket/flask/syringe), reusing `tank_and_slot` + `FluidTankRenderer` — same as the Masticator fuel input.
- A single full-width row of 9 buffer output slots (18px each = 162px, aligns with the player-inventory columns).
- A **visual range grid** in the upper band: the shared self-tiling `grid_square` tile blitted N×N (top-left anchored, grows down-right) to depict the current fuel-tier workspace size. Reusable helper `renderRangeGrid()` on `AbstractModScreen` so the other auto-farmer machines share it. A "Range: NxN" text readout sits to its left, colored `#007F0E` to match the GUI's green accent.
- Standard player inventory below.

**Range preview (feature):** closing the GUI triggers a ~30s particle visualization of the working area — it loops the wave sweep (rings for vertical facing, rows for horizontal) so it communicates both the footprint shape and the wave order, then tapers particle density over the last ~5s to fade out. Preview-only (does NOT play during live farming), restarts its timer on each GUI close, shows whatever the *current* fuel-tier range resolves to (so unfueled shows the minimal 1-block reach), and does not consume fuel. Triggered server-side from `MrFarmerMenu.removed()`; visible to all nearby players by design. Grew out of the debug particles used to verify the range geometry — repurposed rather than removed. **The preview marks any ground surface, not just farmland** (dirt, grass, path, stone, farmland — anything with a sturdy up-face), so the player can see the full reachable footprint and tell which cells still need tilling; the actual farming logic will use a strict farmland test instead. Target resolution is parameterized by a `GroundTest` predicate to support both.

**Implementation status:** Block, block entity, menu, and screen are built (`MrFarmerBlock`/`MrFarmerBlockEntity`/`MrFarmerMenu`/`MrFarmerScreen`). Block entity has a `FuelTank` (fuel-container slot with bidirectional bucket transfer, same pattern as Masticator) and a single `ItemStackHandler(10)` (slot 0 = fuel passthrough, slots 1-9 = buffer), `getRange()` derives the tier→ring value live from the fuel tank. GUI renders the fuel gauge bare (no decorative frame yet — reuses only the cropped 18x18 slot-square from `tank_and_slot.png` as a generic slot backdrop under all 10 slots) plus the text range readout; a proper fuel-tank-specific frame texture is still needed from the art side. GUI opens **directly on right-click** (unlike other machines, which require the Outerface) — `useWithoutItem` opens the menu; holding a fluid container instead fills/drains the fuel tank. Still carries the `HAS_SCREEN` tag so the Outerface also works. The block has an `ACTIVE` (`lit`) blockstate that swaps the texture `mr_farmer` → `mr_farmer_on` while fueled (12 blockstate variants: 6 facings × on/off), driven from `setActive()` in the fuel-gated tick (only re-sets the block on transition, preserving the BE). Right-clicking with any fluid-container item (bucket/flask/syringe) fills the tank through the block's registered `FluidHandler` capability (biofuel filter + fluid-match enforce "biofuel, matching if non-empty").

**Automation:** both capabilities are registered for `MR_FARMER_BE` on all six faces (`ModBusEvents`). Fluid = the fuel tank directly (biofuel-filtered fill, drain) — this is the only automated fueling path. Items = a dedicated automation wrapper (`getAutomationItemHandler`, separate from the GUI's full-access `getItemHandler`): buffer slots 1-9 are **extract-only** (harvest piped out, nothing pushed in); the fuel slot (0) is **player-only** — no automated insert or extract, so pipes can't shuttle fuel containers in/out of it.

**Farming engine — sliced build (functional order):** (1) fuel-gated farming wave skeleton ✅ — `tickFarming()` runs the strict-farmland wave alongside the free preview, advancing only when fueled and burning `max(1, useRate)` mB per worked step; (2) hydrate + light ✅ — each worked cell forces farmland `MOISTURE` to 7 (no water source needed). Lighting uses a **minimal covering set** of invisible `Blocks.LIGHT` (level 15) placed one block above chosen crop cells: a greedy pass over the field only drops a new light where a cell isn't already lit ≥9 (reach = 6 blocks) by an existing one, so 3×3/5×5 use 1 light and larger fields only a handful — no crop cells are blocked (lights sit above). Lights are NBT-tracked in `placedLights` and extinguished on unfuel / field-shrink (reconcile) / block removal, so nothing is left permanently lit; (3) harvest ✅ — mature crops (`CropBlock.isMaxAge`, covers vanilla + CropBlock-extending modded crops) are broken and their natural drops routed into buffer slots 1-9, overflow spilled on the ground (buffer-full rule). **Maturity grace period:** a cell isn't harvested the instant it ripens — `readyToHarvest`/`matureSinceTick` track when each cell was first seen mature and wait `currentMatureGraceTicks()` before harvesting, so ripe crops stay visibly ripe for a beat rather than vanishing immediately (transient, not persisted — a reload may harvest a near-ready crop slightly early); (4) replant ✅ — harvest and replant happen in the same wave pass, so the machine replants the **same crop** it just harvested with no persistent per-cell tracking. Seeds route into a hidden **seed reserve** (`Map<Block,Integer>`, crop block → banked seed count), products go to the buffer. Reserve is capped per crop type at `ceil(range²×1.1)` (a full replant + 10% bumper); seeds over the cap flow to the public buffer, or stay **held** in the reserve if the buffer is full (seeds never hit the ground except on block break). Replant takes the matching crop's seed; if exhausted it falls back to a **random** reserved seed; genuinely-empty tilled cells also get a random reserved seed to keep the field full. Reserve is NBT-persisted and dropped on block break. Fuel drain rate and light coverage/perf are first-pass and open to tuning. Crop-detection currently only recognizes `CropBlock` subclasses for the harvest/replant cycle (nether wart / other non-CropBlock crops are a known gap).

**Wave pacing & maturity grace, fuel-speed-scaled:** both are BASE values at fuel speed 1.0 (Crude Slurry) that get faster/shorter with better fuel — `currentWaveStepTicks() = round(WAVE_STEP_TICKS_BASE / speed)`, `currentMatureGraceTicks() = round(MATURE_GRACE_TICKS_BASE / speed)`, where `speed = max(0.1, FuelTank.getSpeed())` (floored so pacing math never divides by near-zero). Bases: `WAVE_STEP_TICKS_BASE` = 40 ticks (2s, delay between rows/rings), `MATURE_GRACE_TICKS_BASE` = 40 ticks (2s, ripe-crop hold time before harvest) — both up from earlier flat values (6 and 20) that felt too fast. So fuel quality now drives four things: range (tier), run cost (use rate), growth speed (heal), and wave/harvest pacing (speed). The GUI-close preview sweep shares `currentWaveStepTicks()` with the real wave, so it always shows the true current pace.

**Pumpkins & melons (special case, ✅):** stems (`StemBlock`) aren't `CropBlock` and don't fit the harvest-and-replant model — a stem is perennial (never harvested, keeps producing) and its fruit spawns on an **adjacent** tile, not the stem's own farmland cell. Handled separately: (1) growth acceleration is broadened (`isGrowable`) to random-tick stems the same way as crops — and critically, ticking **does not stop at max age for stems** (unlike crops, where a maxed CropBlock has nothing left to do): a mature stem's `randomTick` is what rolls the chance to spawn fruit on an adjacent tile, so continuing to tick past maturity accelerates *fruit* growth, not just the stem reaching maturity; (2) each worked cell's wave step also scans the cell **plus its 4 cardinal neighbors** (`harvestFruitNear`) for a mature `Blocks.PUMPKIN`/`Blocks.MELON` and harvests any found — catches fruit landing just outside the tilled footprint, not only fruit sitting directly above farmland. Fruit harvest routes drops straight to the buffer with no seed banking and no replant (the stem regrows more on its own).

**Open questions:** Exact range step formula and cap (see Shared mechanics — tier→ring is decided: tier 1=3x3 … tier 4=9x9, capped). Does hazardous-fluid disruption (see Shared mechanics) apply here in a specific way, e.g. contaminating hydration? Whether/when a dedicated fuel-gauge frame texture gets made.

### Mr. Shepard

**What it is:** The livestock counterpart to Mr. Farmer's crop automation — a self-running animal farm that maintains a breeding herd and harvests the surplus for meat/leather/wool.

**Visual design (confirmed):** personified like Mr. Farmer, keeping the family's shared visual language — a body-horror head themed around livestock rather than a distinct form.

**Scope:** collects dropped items in range, shears sheep, feeds animals to breed them, accelerates babies to maturity, and culls surplus adults into the output buffer.

#### Fuel stats — what each one drives

Mirrors Mr. Farmer's "fuel quality drives everything" model; all four `BiofuelProperties` stats have a job, none is dead weight.

| Stat | Job on Mr. Shepard |
|---|---|
| `tier` | Working range (3×3 → 9×9, family-shared formula, capped) |
| `useRate` | Fuel drain per work cycle |
| `speed` | Work-cycle pacing (`WAVE_STEP_TICKS_BASE / speed`) |
| `heal` | **Max population cap** *and* **baby growth acceleration** |

**Growth acceleration — babies only, not parent cooldown (decided).** There are two separate delays between "I have two adults" and "I have another adult": how long a **baby takes to mature**, and how long an **adult waits before it can breed again**. `heal` shortens **only the first**. Accelerating both was judged overpowered — it would compress the entire production pipeline at once — and baby maturation is the longer of the two timers, so it's the one that actually gates throughput. The parent cooldown stays at vanilla rates and acts as a natural brake on how fast a herd can turn over regardless of fuel grade. Mirrors Mr. Farmer's use of `heal` (extra random-ticks toward maturity) rather than inventing a second mechanism.

**Why `heal` for the cap (decided):** `speed` and `useRate` are both already spoken for, so using either would double-dip one stat — and `useRate` would additionally be inverted (lower = better), semantically odd, and has poor resolution at current values (Crude 1.0 vs Concentrated 0.90 both round to 1). `heal` was the only free stat. Letting it drive *both* cap and growth is deliberate and coherent — one semantic ("how much life the slurry pushes into things") with two expressions — and each use gets its own tuning constant, so only the *shape* of the curve across fuel grades is shared, not the magnitudes. This also keeps `heal → growth acceleration` consistent with Mr. Farmer, where it already means exactly that.

#### Population cap — the production model

**The cap is the breeding stock, not the total population.** This distinction is load-bearing: any model where the cap counts *total* population goes static by construction — you breed up to the cap, headroom hits zero, breeding stops, nothing is ever surplus, and the machine never produces meat. Babies counting toward the cap is what kills production.

- **Cap = adults maintained.** The player's chosen number is how many adults Shepard keeps.
- **Breed** while `adults + babies < cap + BABY_HEADROOM`, rate-limited to **one pair per work cycle**.
- **Grow** — babies mature naturally, accelerated by the fuel's `heal`.
- **Cull** adults while `adults > cap`, **one per work cycle**, oldest first.
- Steady state at cap 8 / headroom 4: 8 breeders, ≤12 total, continuous throughput as matured surplus is culled and the freed headroom lets breeding resume. **Production is the overflow** — the breed→cull cycle *is* the product, not a bug to be suppressed.

**`BABY_HEADROOM` is a flat internal constant (decided).** Fixed at 4 regardless of cap, rather than scaling with it — one number is easier to reason about, and scaling was judged unnecessary for now. Deliberately **not** player-facing: a second on-screen stepper would be a knob most players never touch. It may still be *documented* to the player in the in-game guide later, since knowing the total ceiling (cap + 4) helps with pen sizing even if it isn't adjustable. Open to revisiting if large herds feel like they trickle.

**Rate balance is the real constraint (learned the hard way).** An early implementation set *every* eligible adult in love each cycle (8 cows → 4 babies per ~2s cycle) while culling only one per cycle — a 4:1 imbalance that made the cap unenforceable and the population run away. The fix is pairing + one-pair-per-cycle rate limiting, **not** forbidding breeding at cap (which was an over-correction that removed the machine's whole purpose).

**Max cap is fuel-driven, player selects up to it:**
- `maxCap = clamp(round(BASE_CAP × heal), 2, 32)`. Crude Slurry (`heal` 1.0) → 8; Concentrated (`heal` 1.25) → 10.
- **Minimum 2**, deliberately — always leaves the player a breeding pair as seed stock to scale back up from. At cap 2 the pair holds steady and doesn't reproduce, which is correct: it's preservation, not production.
- **Hard ceiling 32**, mirroring the range formula's own "capped at 9×9 even though tier-5 fuel will exist" safety rail. Without it a future high-`heal` slurry silently allows enormous per-species herds.
- **Downgrade is destructive (decided):** if a fuel change lowers the max below the player's selection, the selection is clamped down to the new max and does *not* restore when better fuel returns.
- **Empty tank is the exception (decided):** an empty tank *retains* the last fuel's cap data rather than reading `heal` as 0.0 (which `FuelTank` returns when empty, and which would wipe the player's setting on every refuel cycle). The value only changes when a *different* fuel is loaded. Since `FuelTank` rejects a non-matching biofuel while non-empty, a fuel change can only ever happen from empty — so the recompute trigger is exactly "empty tank receives a fluid different from the remembered one." Requires the last-known value be NBT-persisted. Shepard still does not *operate* unfueled; this preserves configuration and the GUI readout only.

#### Species handling

**Per-species, always (design change).** The cap applies independently to each species in range — cap 8 means up to 8 cows *and* up to 8 sheep. **The one-species/many-species toggle from the original concept is dropped**: once caps are per-species by default, the toggle has no clear job left.

**Cull targeting:** when multiple species are over cap, cull from whichever has the **largest population**; within that species, take the **oldest** (culls worn-out breeders and keeps freshly matured stock). **Adults only** — vanilla babies drop nothing, so culling them is pure waste.

**Shear before cull (confirmed):** a sheep must never be culled with its wool still on. The tick order already shears before culling in the same cycle, but the cull path also explicitly shears a woolly target before killing it, so the guarantee doesn't rest on incidental ordering.

**Mooshrooms are deliberately untouched.** They're shearable, but shearing one *permanently converts it into a regular cow* — a destructive transformation, not a harvest. `MushroomCow` isn't a `Sheep` subclass so the shear pass skips it by default; any future mooshroom handling should be opt-in.

#### Breeding specifics

- Food is **consumed** from dedicated food slots — breeding is a real supply chain, not automatic (decided).
- Animals are **paired** (each baby costs two fed parents); if the second parent can't be fed, the first parent's food is refunded rather than wasted on a half-pair.
- Eligibility uses vanilla's own breeding gate (`!isBaby() && getAge() == 0 && canFallInLove()`), which respects post-breeding cooldown.

#### Gate compatibility (built, family-wide)

**Both Mr. Shepard and Mr. Farmer now implement `IHasChannels`**, so the Innards Gate can service them — previously the whole farming family was unreachable by the Gate while eleven other machines were not. Implementing the interface is the entire integration; the Gate Controller and I.D.E.P. resolve against it generically.

- **Shepard's channels, in Gate priority order** (list order *is* the priority, convention is starvation-risk-first): `fuel` (IN) → `feed` (IN) → `output` (OUT). Fuel first because the machine stops dead without it; feed next because breeding stalls without it; the output buffer last because it only backs up.
- **Farmer's channels:** `fuel` (IN) → `harvest` (OUT).
- **All channels are exposed on all six faces**, since both machines register their capabilities face-agnostically. `describeFace` is correspondingly face-independent, and `describeFluidFace` always names the fuel tank (mirroring `getTank`, which ignores the face).
- **The fuel *container* slot is never a channel** on either machine — it stays player-only, matching what the automation item handler already enforces. Automated fueling goes through the fluid channel.
- **Farmer's hidden seed reserve is deliberately not a channel** — it's internal replant stock rather than storage, and surplus above its per-crop cap already spills into the buffer where automation can reach it.
- Shared helper `util/SlotRangeItemHandler` gives each channel a permission-locked view of its own slot range (feed is insert-only, buffers are extract-only). Generalises the single-slot anonymous handler Masticator uses, since the farming machines need multi-slot rows.

#### Divergences from Mr. Farmer

- **No positional wave.** Farmer sweeps ring-by-ring / row-by-row because crops are fixed to cells; animals move freely, so wave *ordering* is meaningless. Shepard scans a live AABB each work cycle instead. The original "wave behavior likely carries over" note is superseded.
- **The field includes the machine's own row (confirmed).** Farmer's N×N field starts **1 block ahead**; Shepard's starts at **depth 0**, making it (range+1) deep × range wide. Reason: a Shepard is set *into* the pen's fence/wall line, so it sits flush with the boundary — and unlike crops, dropped items and animals move. Items landing directly beside the machine sit in the machine's own plane, and with a Farmer-style 1-block-ahead field they would be permanently uncollectable. The range preview outlines the same extended footprint (skipping the machine's own cell), so the guide shows the wall running *through* the machine rather than in front of it.
- **Vertical tolerance is ±4 blocks** (`VERTICAL_REACH`), against Farmer's ±2 — animals jump and fall, crops don't.
- **Pickup reaches 1 block wider than the working area on every horizontal side (`PICKUP_MARGIN`).** Drops drift and settle, so an item produced well inside the range can come to rest against a fence post or pen wall just *outside* it — and would then sit there permanently, since nothing ever nudges it back in. The margin is **pickup-only**: breeding, shearing, growth and culling all stay bounded by the true working area, so this widens what the machine *cleans up* without widening what it *manages*. The range preview still outlines the working area, not the pickup margin — the preview's job is siting the pen walls, and the margin is a safety net rather than something to build to.
- **Fence-height collision box (Shepard only, confirmed).** `getCollisionShape` is raised to 24px (1.5 blocks), matching a **vanilla fence** rather than the model's own ~22px hat. Reason: a Shepard is set *into* the pen's fence line, so a plain full-cube block leaves a 1-block-tall gap in an otherwise 1.5-tall wall — an escape hatch animals can jump. Blocks can still be placed on top: placement only tests the target position for replaceability and entities, never a neighbour's collision shape (identical to placing a block atop a fence). **Only collision is raised, not `getShape`** — the selection outline stays a normal cube so it doesn't fight with whatever block sits above. Horizontal extent stays 0–16 even though the model overhangs sideways, so entities never collide with apparently-empty space in the neighbouring column. The block above visually clips the hat; accepted, pending a new model. **Not applied to Mr. Farmer** — nothing needs containing there.
- **Known defect, vertical facing only (deferred):** for up/down mounts Shepard still centres its box `half + 1` along the facing axis and then extends ±4, which **bleeds back past the machine's own plane** (a DOWN-facing Shepard at range 3 covers 3 blocks *above* itself; the preview for an UP-facing one floats in the air). Farmer instead resolves a single working plane strictly on the facing side. Left alone pending possible new models for both machines; candidate fixes are clamping the box to the facing side, mirroring Farmer's single-plane resolution, or restricting Shepard to horizontal facing entirely.

**Range preview — static footprint guide (confirmed).** Shepard still gets a GUI-close particle preview, but **not** Farmer's swept version — with no wave to communicate, the sweep would be showing an ordering that doesn't exist. Instead the preview's job is narrower and more practical: **show the player where to build the pen.** It marks the **perimeter of the N×N footprint** all at once rather than filling it — at 9×9 that's 32 edge cells instead of 81, which reads as a wall line rather than particle soup, and a wall line is exactly the thing the player is about to build. Same trigger and lifetime conventions as Farmer's (fires on GUI close, ~30s, fades out, costs no fuel, shows whatever the current fuel tier resolves to). Farmer's preview machinery lives in `MrFarmerBlockEntity` and wants extracting to a shared home so both machines draw from one implementation.

#### GUI

Follows Mr. Farmer's layout conventions. Top-left band carries a "Range: N×N" readout with the shared `renderRangeGrid()` tile grid, and below it a "Cap: N" readout flanked by **+/- stepper buttons** (18×18, using the shared `gui/buttons/` art). The steppers are **momentary, not toggles** — clicking shows the `_pressed` texture for a few ticks as click feedback, then reverts.

**Steppers: click for precision, hold to accelerate (decided).** A single click steps by exactly 1, so the player can always land on an exact number. Holding the button down pauses for **~2 seconds** and then starts running the value rapidly, so covering 2→32 doesn't cost 30 individual clicks. The delay before acceleration is what preserves precision — a short hold still reads as one deliberate click rather than accidentally racing past the target. Standard key-repeat behaviour, applied to both steppers.

**The cap readout must update live (confirmed).** The value has to change on screen the moment a stepper is clicked, not on the next GUI open. Cause of the current lag: `changePopulationCap()` calls `setChanged()` only, which marks the block entity dirty for *saving* but never pushes a packet, so the client's copy keeps the stale number until it re-syncs. Fix is to also call `updateBlock()`, matching how every other machine in the mod syncs BE state to nearby clients (the value is already carried in `saveAdditional`/`getUpdateTag`, so nothing else needs adding). Fuel slot + horizontal fuel gauge sit on one row, with a 4-slot food row right-aligned to the 9-slot output buffer below it.

**Implementation status: feature-complete against every decision above, and tested in-game.** Block, block entity, menu, screen, registrations, implant recipe (Carved Pumpkin + Shears + flesh), models/textures, lang and tags all built. Working and verified: item pickup (with the player-owned-drop guard), sheep shearing, the breeding-stock/`BABY_HEADROOM` production model, adults-only culling with the shear-before-cull guard, all four fuel stats (`tier`→range, `speed`→pacing, `useRate`→cost, `heal`→cap + baby growth), remembered-fuel-on-empty, live cap-readout syncing, click-for-precision/hold-to-accelerate steppers, the static footprint range preview, slot tooltips, the fence-height collision box, and Gate compatibility via `IHasChannels`.

**Outstanding on Shepard:**
- The **vertical-facing collision-volume defect** (see Divergences above) — deferred pending new models.
- **New models** for Shepard (and Farmer) are planned; the hat currently clips whatever block sits above, which is part of the motivation.
- Tuning anchors that have not been playtested at scale: `GROWTH_SECONDS_PER_HEAL` (1.0), `BABY_HEADROOM` (4), `BASE_POPULATION_CAP` (8). Note that **only tier-1 biofuels exist today** (Crude and Concentrated Slurry), so the fuel-driven range and cap ladders cannot be exercised past their first rung until higher-grade slurries get `BIOFUELS` data-map entries — the same untestable-at-scale gap Mr. Farmer's range has.

#### Deferred to later passes

- **`TieredMachine`/`MachineTier` migration: not doing it (decided).** Fuel grade is already the family's progression axis, and — the load-bearing reason — the farming machines never handle hazardous materials, so there's no hazard-tolerance ladder for tiers to gate. Full reasoning and the revisit condition live in Shared mechanics above. Both farming machines keep hard-coding `BASIC`.
- **Hazardous-fluid disruption: dropped, not deferred.** See Shared mechanics — the family is defined as non-hazardous on both the input and output side.
- **No-kill mode: not doing it.** Herd size is considered sufficient control — a player who doesn't want animals harvested sets the cap where they want the herd to sit. Revisit only if it's requested enough to justify the extra UI.
- **Hazardous-fluid disruption: not doing it.** The family-wide idea inherited from the original notes stays unimplemented; Shared mechanics already flags it as possibly getting cut outright.
- **More design work is expected** on Shepard after the culling/raising rework is built and playtested — the current decisions cover the production model, not necessarily the whole machine.

### Mr. Logger

*(formerly "Lumbering Jack" — renamed.)*

**What it is:** Working name for a machine to automate tree farming (planting/harvesting trees for wood).

**Mechanics (starting point):**
- **Scope:** auto-plants saplings and auto-harvests grown trees in range. Wave behavior (ring-by-ring, closest to farthest) applies to both planting and harvesting passes.
- **Harvest trigger:** listens for a tree-growth event rather than polling/timing growth itself. Likely candidate is NeoForge's `BlockGrowFeatureEvent` (successor to the old `SaplingGrowTreeEvent`), which fires when a sapling/tree feature grows — not yet confirmed against this project's NeoForge version, needs verification at implementation time.

**Open questions:** Visual design — personified like Mr. Farmer, or different? Does it clear leaves along with logs, or leave them? Does it handle multiple tree/wood types simultaneously (mirrors Mr. Shepard's multi-species question), or is that not a concern since saplings are more uniform?

### Mr. Hunter

**What it is:** Working name for a machine to automate mob grinding — the combat/mob-drop counterpart to the rest of this family's crop/animal/tree automation.

**Open questions:** Everything — this is a bare concept pitch, not yet fleshed out. What counts as a valid target (hostile mobs only, or passive too — overlaps with Mr. Shepard's animal scope)? How does it kill (direct damage, environmental trigger like fall/fire, a spawner-adjacent design)? Does it need a spawning-platform/mob-farm component, or does it only act on mobs already in range? Visual design — personified like Mr. Farmer, or different? Range/wave behavior — does the shared ring/row pattern even apply to a combat machine, or does this one need its own targeting model? Loot handling — same buffer-and-overflow convention as the rest of the family?

---

## Template for new entries

```
### [Machine Name]

**Status:**

**What it is:**

**Recipe/logic:**

**Open questions:**
```
