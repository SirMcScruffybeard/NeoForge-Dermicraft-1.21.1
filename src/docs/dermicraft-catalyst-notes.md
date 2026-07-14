# Dermicraft Catalyst Notes

Running log of decided design choices and the reasoning behind them. Add a new entry per fluid/block/item as decisions get made — the "why" matters as much as the "what" so later additions stay consistent with earlier logic.

---

## Fluids — Catalyst family

### Primitive Catalyst

**Status:** Color decided, fluid properties decided, texture pending

**What it is:** First catalyst in the catalyst line. Simple to make, easy-to-gather ingredients. "Primitive" = first created + crude/simple recipe.

**Recipe logic:**
- Base: Crude Slurry (custom fluid, dark green, represents plant life)
- Raw meat (any vanilla) — represents animal life, adds red/pink
- Bone — calcium as binding agent, adds white/chalky lightening
- Coal/charcoal — purifying agent, darkens and desaturates rather than tinting; doesn't fully dissolve

Process: ingredients tossed into 1x1 puddle of Crude Slurry, wait, collect with bucket.

**Second recipe — added (Effluencer, operational):** A parallel, non-puddle route now exists: F-Stuff (500 mB) + C-Stuff (500 mB) → Primitive Catalyst (750 mB) via the Effluentcer machine, 900 ticks. Same "resolve conflicts with a parallel/alternate recipe" pattern as Evolution Catalyst's two-recipe setup below — the original puddle recipe is unchanged, this is an added route, not a replacement. F-Stuff and C-Stuff are themselves Effluentcer-made mixes (Crude Slurry + Protein Blend, and Carbon Blend + Calcium Blend, respectively) — see `dermicraft-crafting-notes.md` and the primer's Machines → Effluentcer entry.

**Color decision:** `#5C4A30` — muddy olive-brown, balanced mix where no single ingredient visibly dominates. Green + red are near-complementary and collapse toward brown; bone lightens/chalks it; charcoal darkens/desaturates without dissolving cleanly.

**Alternates considered (not chosen):**
- `#4A3D28` — heavier charcoal weighting, darker
- `#6B5638` — heavier bone weighting, chalkier/lighter
- `#4F3A2E` — heavier meat weighting, warmer red undertone (would read as more "violent origin" if that's wanted instead)

**Texture note:** Base off vanilla water texture. Consider a few near-black pixel flecks (undissolved charcoal) breaking up the surface — sells "crude mixture" better than a flat fill, especially animated in a tank.

**Note (updated):** Catalysts are not a tiered/progression series — each has a distinct purpose, though some use others as a base (e.g. Evolution Catalyst uses a Primitive Catalyst pool). The earlier idea of a shared palette-progression logic across the family doesn't apply; each catalyst's color should be derived from its own ingredients/purpose instead.

**Fluid properties (decided):**
- `.viscosity(1500)`
- `.density(1500)`
- `.temperature(305)`
- `.motionScale(0.025)`

Slightly thicker/heavier than vanilla water across viscosity, density, and temperature. Note: motionScale of 0.025 is *higher* than water's 0.014, whereas the conventional pattern (water → lava) has motionScale decrease as viscosity increases. This was flagged during discussion as a possible inversion of the usual relationship — confirmed as intentional, kept as-is for added strangeness.

**What motion scale actually does:** Governs how much an entity's own movement input gets converted into actual velocity while inside the fluid — i.e. how responsive moving/swimming feels, not how the fluid flows on its own (that's viscosity). Water's 0.014 lets swim strokes "count" normally; lava's lower 0.007 mutes input, making movement feel stuck/unresponsive — separate from lava's heat/damage.

**Why the reversal is unsettling here:** Primitive Catalyst's high viscosity/density means it should look and behave like a thick, heavy substance — spreads slowly, sinks things readily. But its motion scale being *higher* than even water's means an entity actually inside it would have *more* responsive movement than swimming in water, not less. The fluid looks and pools like heavy sludge, but doesn't resist physically moving through it the way something that thick should — it reads as not pushing back correctly for its apparent weight. A small, physically "wrong" detail that fits the bio-horror theme well.

---

### Evolution Catalyst

**Status:** Ingredient list decided, color decided. Now has two confirmed recipes (see below).

**What it is:** Not part of a tier system — has a distinct purpose from other catalysts, though it uses Primitive Catalyst as a base. Used as the final step in a machine's "forced evolution" process: machine is loaded with its own specific items/fluids over time, then Evolution Catalyst is injected to complete the transformation. Visually, a successful evolution swaps the machine for a unique new block (details TBD in a later discussion).

This catalyst also marks the player's transition into Stage 2 — the introduction of lava as a generated fluid/ingredient, which Stage 1 blocks/items can't handle. The Nether is the gate into Stage 2.

**Recipe logic (original — Stage 1, the gate recipe):**
- Base: pool of Primitive Catalyst
- Phantom Membrane — unnatural regrowth (Phantoms are born of unnatural conditions; vanilla's only "regrowth" item). Carries the "forced/unnatural change" theme.
- Nether Quartz — non-living, Nether-gated (reinforces the Stage 2 gate). Also vanilla's signal-amplifier material (comparators), giving it an energy/resonance read.
- Glowstone Dust — non-living, pure light-energy rather than heat-energy. In vanilla brewing, glowstone intensifies an existing effect — fills the "push past natural limits" role without poison/instability connotations.
- Copper Ingot — non-living, vanilla's metal most directly tied to conducting electricity (lightning rods) — reads as "high energy" more literally than iron or gold.

**Second recipe — added (late Stage 2):** Evolution Catalyst now has two valid recipes. The original recipe above remains completely unchanged and still serves as the Stage 1→2 gate — every ingredient stays reachable within Stage 1, which is the whole point of a gate recipe. A **second, late-Stage-2 recipe** substitutes **Living Glowstone** (see entry below) for the raw Glowstone Dust, only available once that fluid exists. Both recipes produce the **exact same Evolution Catalyst result** — the late recipe is purely an efficiency upgrade over the original (either a lower material cost or a higher yield for equivalent cost; exact numbers not yet decided, both approaches considered equally valid options to choose between later). This follows the same "alternate efficient route to an already-existing result" pattern already used by Carbon/Calcium/Protein Blend.

**Design constraints kept in mind:**
- No fire/heat imagery and no lava in the recipe itself — the catalyst foreshadows Stage 2 without being fire-themed. Theme is "high energy," not "heat."
- Nether access is assumed available in Stage 1 (for gathering Nether Quartz) — it's specifically Dermicraft's own machines/fluids that can't handle lava yet, not Nether access itself.

**Ingredients considered and dropped:**
- Turtle Scute, Nether Wart — earlier draft, both leaned "more organic," dropped once non-living variety was requested.
- Obsidian — non-living and Nether-adjacent, but read as too fire/heat-coded for a catalyst that's meant to feel high-energy rather than fire-themed; swapped for Nether Quartz.
- Spider Eye (x2) — was carrying the "forcing/instability" role; dropped per preference. That role shifted to Glowstone Dust instead (intensifies rather than poisons).
- Blaze Powder — mixed feelings noted, dropped for being too fire-coded alongside Obsidian.
- Gold Ingot — alternative metal option discussed; also Nether-sourced (gold ore) but weaker "energy" association than copper (mainly powered rails). Copper chosen instead.

**Color decision:** `#E0B83C` — glowstone-dominant vivid gold. Derived from the ingredient mix the same way as Primitive Catalyst (literal blend of base + ingredient colors: muddy brown base, pale Phantom Membrane, white Nether Quartz, bright yellow Glowstone Dust, warm orange-pink Copper Ingot), then pushed more vibrant by request — letting Glowstone Dust read as the dominant visual driver rather than splitting weight evenly, since it's the most "energetic"/"alive" ingredient and that's the intended read. This is the brightest option before the color stops looking like a mixture and starts reading as straight liquid glowstone.

**Alternates considered (not chosen):**
- `#9C7A2E` — balanced literal mix, no ingredient weighted more than others (first-pass recommendation before vibrancy was requested)
- `#A66B30` — heavier copper weighting, warmer/more orange
- `#C7A83A` — heavier glowstone weighting, brighter (first vibrancy step)
- `#B3A573` — heavier quartz/phantom membrane weighting, paler/cooler
- `#D9A82E` — vivid amber-gold, glowstone given more visual weight (second-pass recommendation)
- `#D98F2E` — vivid copper-warm lean, more orange than gold

**Texture/visual note:** Glowstone Dust is light-emitting in vanilla — if the fluid system supports any glow/emissive property, giving this catalyst a faint light emission (even subtle) would reinforce "high energy" beyond what color alone communicates, and would distinguish it from Primitive Catalyst's flat, non-emissive look. Not yet confirmed as feasible in the current texture/fluid setup. The late-Stage-2 recipe using genuinely-emissive Living Glowstone as an ingredient makes this idea more justified than before — an emissive result would now feel earned by the recipe, not just requested.

---

### Living Catalyst

**Status:** Core mechanic and recipe now defined — self-replication, three-fluid recipe, and applying machine all confirmed. Exact numeric values (yield, ratios) still open. (Renamed from "Life Catalyst" — matches the "Living X" naming used across the family it produces: Living Glowstone, Living Netherite, Living Iron/Copper/Gold.)

**What it is:** The mod's "add life" reagent — mixed with a non-living fluid in the **Gestator** (see `dermicraft-machine-notes.md`) to produce that fluid's living, self-replicating counterpart (e.g. Molten Glowstone → Living Glowstone, Molten Netherite → Living Netherite). Unlike every other catalyst so far, which transforms or completes a process in one shot, the *output* of a Living Catalyst reaction is a material that **continues to act on its own afterward** — self-replicates over time once created, rather than the catalyst producing a single finished result.

**Recipe (confirmed):** Three fluid inputs, no solid/injection item — **Dragon's Milk + Molten Quartz + Molten Blaze Essence**, combined in an **upgraded Effluentcer** (see `dermicraft-machine-notes.md` — the base Effluentcer only has two fluid input tanks; this recipe needs a third).

**Recipe logic:** Dropped Living Glowstone as an ingredient — the original draft used Living Glowstone as Living Catalyst's base, but since Living Glowstone is itself now a *product* of Living Catalyst (Molten Glowstone + Living Catalyst → Living Glowstone), that would have been circular. Dragon's Milk supplies genuine life-essence from the mod's strongest vanilla creature — thematically grounding what makes the result "alive." Molten Quartz represents time/duration, standing in for the process needing time to take hold rather than triggering instantly off raw living material alone. Molten Blaze Essence adds a heat/energy-forged component, consistent with every other Stage 2 material being processed with Lava rather than soaked.

**Applying machine — confirmed: the Gestator.** Takes Living Catalyst + one other fluid; if that second fluid has a living counterpart, the two mix to produce it. If the second fluid is already living, the Gestator instead produces a large batch of that same living fluid (accelerated-replication illusion, not a permanent change to the fluid's own replication speed). See `dermicraft-machine-notes.md` Gestator entry for full mechanic detail (including the softer pooling-not-refusing fail-safe for invalid input).

**Placement:** Introduced at the **Stage 2/3 boundary** — every ingredient (Dragon's Milk, Molten Quartz, Molten Blaze Essence) is a Stage 2 fluid, but the result and everything it produces (Living Glowstone, Living Netherite, Living Metals) is Stage 3. Not pinned to an exact Tier yet; likely **Tier 3 or Tier 4** territory for any machine that needs to *handle* a Living fluid at scale, separate from the Gestator itself which only needs to *produce* it.

**Tier vs. Stage — clarifying note:** Tier operates at the **micro level** — a capability rating on an individual item, Gadget, or Machine (e.g. lava-handling capability). Stage describes the **macro level** — the player's overall progress through the mod at major milestones (e.g. Nether access gating Stage 2 as a whole). The two are related but track separately. See `dermicraft-project-primer.md` Stage structure section.

**Living Metals — confirmed role, Mild Radiation handling:** Living Iron, Living Copper, and Living Gold (names tentative — concepts confirmed, exact naming not locked) are confirmed to be what eventually lets Stage 2 machines safely handle **Mild Radiation** — Living Glowstone's own hazard classification (see entry below). The "handling" here isn't blocking-style shielding in the literal sense — it's framed as **toughness + self-repair outpacing the damage**: a living material heals faster than radiation degrades it, rather than a material that blocks radiation outright. A distinct flavor of hazard-handling, kept deliberately separate from the Obsidian-carapace heat-blocking approach planned for early Tier 2 machines (see `dermicraft-project-primer.md` Stage structure for both).

**Introduction timing (clarified):** Rather than waiting for a clean Tier 3/4 unlock, Living Catalyst's first, deliberately difficult and low-yield form is introduced at the **Stage 2/3 boundary**, alongside Living Glowstone. Precedent: **Tools were never Tier-gated to begin with** — Tier ratings only ever applied to Gadgets/Machines — which is exactly why Flask of Lava already worked without contradicting Tier 1's hazard restriction. The same logic extends here: small, hard-won amounts of something this dangerous don't break anything, the same way a Flask safely containing lava doesn't.

**Stage 3 — first defined as a result of this:** Centers on scaling Living Catalyst / Living Metal production from this difficult trace-access state up to real, repeatable production at scale — the Stage-level equivalent of Evolution Catalyst's two-recipe pattern (a later, easier route to something already unlocked), just applied across a Stage boundary instead of within one recipe.

**Open questions:** Exact fluid ratios/yield for the three-input recipe. Self-replication rate and mechanics for "living" materials in general — likely related to/derived from Living Glowstone's own open replication-rate question. Full roster of which materials get a Molten (or otherwise non-living) precursor and thus a Living counterpart — currently confirmed for Glowstone and Netherite; deliberately **not** extended to the plain Metal Blends (Ferrous/Cuprous/Aurous), which go straight from their existing Blend + Living Catalyst without a Molten intermediate, since they were never gated materials to begin with. Exact yield/difficulty numbers for the Stage 2/3 boundary trace-access version vs. Stage 3's scaled-up version.

---

### Ascendant Catalyst

**Status:** Purpose and hazard classification confirmed; ingredients, recipe, and color not yet decided. (Logged under the placeholder name "Ascending Catalyst" in earlier notes — confirmed as **Ascendant Catalyst** going forward.)

**What it is:** The mod's top-tier, endgame catalyst — used to craft the mod's "god tier" equipment. Sits beyond Evolution Catalyst and Living Catalyst in the overall progression, though its exact Tier/Stage placement isn't pinned down yet.

**Hazard classification:** `Hazardous → Radiation → Severe`. Deliberately difficult to handle — something powerful enough to craft the mod's best gear shouldn't be casually safe. No confirmed handling solution yet: an earlier idea (a dense metal-blend fluid for shielding) was floated and dropped, and Living Metals were considered but redirected specifically to Mild Radiation instead (see Living Catalyst above) — Severe Radiation's eventual handling method is a genuinely open question, not just an unset number.

**Open questions:** Exact recipe/ingredients. Color. What machine (if any) is needed to apply/process it. What eventually allows a player or machine to safely handle Severe Radiation. Exact Tier/Stage placement.

---

### Living Glowstone

**Status:** Core identity, key FluidType properties, hazard classification, and creation recipe all decided. Not a Catalyst itself — logged here for proximity, since its confirmed uses are all Catalyst-adjacent (Evolution Catalyst's late-game alternate ingredient, Living Catalyst's flagship product). **Moved to Stage 3** (previously "late Stage 2") — reworked now that its recipe requires Living Catalyst, which is itself a Stage 2/3-boundary product; Living Glowstone can't exist any earlier than the catalyst that creates it. (Renamed from "Liquid Glowstone" — better reflects its self-replicating identity.)

**Recipe (confirmed):** **Molten Glowstone + Living Catalyst → Living Glowstone**, via the **Gestator** (see `dermicraft-machine-notes.md`). Molten Glowstone itself is a plain Stage 2 fluid (Glowstone Dust + Lava, Masticator — see `dermicraft-crafting-notes.md`) — Living Glowstone is its Stage 3 "activated" counterpart, not a standalone recipe of its own anymore.

**Open flag — Evolution Catalyst's late-Stage-2 recipe:** Evolution Catalyst's second recipe substitutes Living Glowstone for raw Glowstone Dust (see Evolution Catalyst above), and was framed as a **late-Stage-2** upgrade. Now that Living Glowstone itself doesn't exist until Stage 3, that recipe is either (a) actually a Stage 3 recipe and the "late-Stage-2" label needs updating, or (b) fine as-is on the theory that a Stage 2 recipe can opportunistically use an early-Stage-3 ingredient once a player has reached it. Not resolved — flagging rather than assuming.

**Hazard classification:** `Hazardous → Radiation → Mild`. See Living Catalyst above for how this eventually gets handled (Living Metals) and how small amounts can be accessed safely before that exists (the same Tools-aren't-Tier-gated precedent that already lets Flask of Lava work).

**What it is:** A liquid form of glowstone — built specifically around what makes glowstone feel unique rather than treating it as just another ingredient. Also the mod's **first self-replicating ("living") fluid** — its cellular, bubbling appearance is meant to be read as a literal indicator of the living mechanic, not just flavor.

**Light:** Emits light matching the vanilla Glowstone block's full light level — the mod's **first genuinely emissive fluid**.

**Temperature:** Low, but **not zero**. (Corrected from an earlier draft assumption of "zero heat" — real glowstone does carry enough heat to melt nearby snow/ice, the same effect torches and jack-o'-lanterns have via their light level.) Sits low, well below lava, preserving the "light-energy, not heat-energy" identity Evolution Catalyst's original ingredient list already established — without overstating it to a literal zero.

**Density:** Set **negative**, using NeoForge's FluidType behavior where negative density makes a fluid flow *upward* instead of pooling downward. First fluid in the mod to use this property — a deliberate physical "wrongness" lever, parallel in spirit to Primitive Catalyst's inverted motion scale, but using a different property so it isn't a repeat of the same trick.

**Self-replication ("living"):** Confirmed to actually self-replicate, not just look alive — the same literal mechanic Living Catalyst generalizes onto other materials. Growth rate is intended to be **painfully slow by default**, with player-accessible ways to boost the rate planned for later — giving it a real progression curve (slow-but-infinite, not a free/easy resource) rather than either extreme. The same "infinite is fine, just shouldn't be easy" philosophy applied elsewhere in the mod (see Sediment Blends in `dermicraft-crafting-notes.md`).

**Function — potions (flagged, not committed):** Could directly intensify potions if/when the mod builds out a potion-brewing system, echoing Glowstone Dust's existing vanilla brewing role. Not decided as an active mechanic yet.

**Open questions:** Exact temperature value. Motion scale value (not yet discussed). Self-replication rate and what specifically boosts it. Whether/how the potion-intensify function gets implemented if brewing is added. Whether Evolution Catalyst's late-game recipe needs its Stage label corrected (see flag above).

---

## Reference — Vanilla Fluid Properties (NeoForge FluidType)

For comparison when setting properties on new Dermicraft fluids. These four properties are all arbitrary integers/doubles (no required real-world units), but should stay roughly consistent with each other in scale.

| Property | Vanilla Water | Vanilla Lava |
|---|---|---|
| Viscosity | 1000 | ~6000 |
| Density | 1000 | ~3000 |
| Temperature | 300 | ~1300 |
| Motion Scale | 0.014 | ~0.007 |

- **Viscosity:** higher = flows slower/thicker.
- **Density:** higher = heavier; affects buoyancy (whether entities/items float or sink). Can be set **negative** to make a fluid flow *upward* instead — confirmed used for Living Glowstone, the first Dermicraft fluid to do this.
- **Temperature:** higher = hotter; arbitrary scale, roughly Kelvin-like.
- **Motion Scale:** how much the fluid's current affects entity velocity. Conventionally moves *opposite* viscosity — thicker fluids (lava) push entities around less, not more.

(Water's values are confirmed defaults; lava's are commonly-cited reference values used across NeoForge modding tutorials — worth double-checking against current NeoForge source if exact precision matters.)

---

## Template for new entries

```
### [Name]

**Status:**

**What it is:**

**Recipe/logic:**

**Decision:**

**Alternates considered:**

**Open questions:**
```
