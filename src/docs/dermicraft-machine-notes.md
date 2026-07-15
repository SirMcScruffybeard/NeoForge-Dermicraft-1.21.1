# Dermicraft Machine Notes

Running log of decided design choices for Machines and the reasoning behind them. Add a new entry per machine as decisions get made.

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
- **"Smart" structures — willing evolution, the new Gear Stations model.** The **OT's control block** (Brain → Core) and the **Gear Stations** (Dock/Workbench/Growth Chamber) are framed as intelligent/living enough to **want** to evolve on their own — no forced injection. Instead: the screen shows the fluid requirement, the shared pool fills it, the player presses a button, a **timed** process runs, and **if interrupted the fluids are lost but the structure itself is never at risk**. This is the exception, not the new default — it applies only to these "smart" structures, not to ordinary Machines.

**Why this matters going forward:** when a new Machine or structure gets a Tier 2+ form, default to the **forced/Evolution Catalyst path** unless there's a specific thematic reason (the thing is framed as intelligent/living, like the OT or Gear Stations) to give it the willing/button-press path instead.

**Hazard rejection is instinctive, not intelligent (confirmed flavor, ties "dumb" to the hazard-hard-stop rule).** A Tier 1 "dumb" Machine still hard-refuses to process a fluid above its hazard tolerance (see `dermicraft-project-primer.md` → Hazard tag hierarchy) even with no Brain anywhere in its recipe. In-fiction framing: the machine **literally feels the fluid and recoils from it** — a reflexive, sensory response, not a decision. This is consistent with (not a new mechanic layered on) every Machine implant recipe already requiring **Nerve Cluster** tissue — the sensory capacity was always built in, it just doesn't require intelligence (Brain) to act on. A future upgrade tier tolerating more hazards isn't "getting smarter about it," it's growing a *thicker hide*, not a *bigger brain*.

**Concrete "smart" test (confirmed) — the Brain as ingredient.** Rather than a judgment call per new addition, "smart" status is defined by a checkable recipe rule: **a structure only qualifies as smart if the Brain (or a Brain-derived ingredient) is required in its recipe.**
- **OT's control block:** obviously qualifies — the Brain **is** the thing being built/evolved (Brain Block → Core).
- **Gear Stations (Dock/Workbench/Growth Chamber):** qualify via their **initial construction recipe** — the Brain is a required ingredient when a Gear Station is first built, not necessarily in its later tier-up/evolution recipes. Building with the Brain from birth is what earns willing-evolution status going forward; exact quantities/other ingredients still TBD (see Gear Stations notes).
- **Ordinary Machine families** (Masticator, Effluentcer, Metastasizer, Skin Tank/Chitin Tank, and small machines generally) **never** include the Brain in any recipe, at any tier — this is what keeps them permanently "dumb," forced-evolution only, for their entire family lineage.

**Families (new concept, mod-wide):** Machines group into **families** — a family shares the same shape and function across all its tiers, gaining only better performance and higher hazard-tolerance as it climbs (e.g. every Masticator tier is still recognizably a Masticator, just faster/tougher). A family's smart/dumb status is fixed for its entire lineage — a dumb family (which is all of them, by design, aside from the OT/Gear Stations) stays dumb at every tier, not just Tier 1→2.

**Extends beyond evolution — operational behavior at fuel-empty (confirmed).** The same smart/dumb split governs how a structure behaves when it **runs out of fuel mid-operation**, not just how it evolves:
- **"Dumb" Machines** run the **standard Machine health/fuel system** (see Machine health and fuel system, above) — they don't know when to stop, so they **keep going unfueled** at a drastically reduced rate (1/10th normal progress) while their HP drains, until they hit 0 HP and stall outright.
- **"Smart" structures** (OT, Gear Stations) **know when to stop** — no HP pool at all (same as the OT's own confirmed "no HP, fuel-required" model). When fuel runs out mid-process, a smart structure **halts immediately and preserves progress** (the OT's confirmed Stop-on-fuel-out behavior — see Core, Fuel/HP, above), surfacing the standard error/warning rather than grinding on. Applies to every fuel-driven Gear Station process (Dock's refuel/repair/player-heal, Growth Chamber's tier-ups, Workbench's recharge/durability-repair) — all halt cleanly and resume once refueled, never degrade-and-drain like a dumb Machine.

**Standard Tier 2 evolution recipe template (dumb Machines, confirmed default):** most Tier 1→2 evolutions follow one consistent shape, rather than being invented per-machine:
1. A **Lava Bucket** occupies one of the machine's item slots.
2. One of the machine's tanks is **completely filled with Water**.
3. The assembly is **injected with Evolution Catalyst**.
4. **Everything is consumed** (bucket empties, water is used up, Catalyst is consumed) and the **evolved block spawns into the world** in the original's place.

This is a deliberate thematic echo of vanilla obsidian generation (lava + water) and lines up with the primer's existing **Tier 2 "obsidian carapace"** visual theme — not a coincidence, worth keeping in mind as future Tier 2 evolutions get designed, since it reinforces a theme that was already established independently. This is a **default template, not an absolute rule** (per the mod's existing "signature mechanic is a default, not a hard rule" convention) — an individual machine's evolution can deviate if it has a specific reason to.

**Tier 2 OT-native construction (new):** starting at **Tier 2**, the OT gains **build-from-scratch recipes** for these Machines too — extending the existing Tier 1 OT-native recipe formula (see OT-native machine recipes, below: keep the defining physical item, convert flesh ingredients to a discounted Protein Blend cost, keep the binding agent, drop the suture requirement) to Tier 2 Machines as they're designed. This is separate from a Tier 1 Machine's own forced-evolution path (Cauldron → Crucible via Catalyst) — the OT can now also **print a Tier 2 Machine directly**, not just evolve an existing Tier 1 one.

**Deferred discussion (not yet resolved) — a Tier 2 Inert Tumor variant?** Should there also be a **hand-crafted** route to a Tier 2 Machine — a Tier 2 Inert Tumor + Tier 2 implant recipes — as the hand-crafted counterpart to the OT-native construction above, the same way Tier 1 Machines have both a hand-crafted implant recipe and an OT-native one? Not designed yet; full note logged in `dermicraft-tools-notes.md` → Inert Tumor.

---

## Known machines

Every Machine here is physically born from an Inert Tumor block — recipe items sutured in, the tumor stitched closed, then injected (most likely with Primitive Catalyst) via Syringe. Full mechanic in `dermicraft-tools-notes.md`. **Drooling Cauldron, Masticator, Skin Tank, Effluentcer, Craw, and Metastasizer all have their tumor-genesis (implant) recipes written and live in code** — the Metastasizer's (the last outstanding one) is now implemented per the spec in its entry below. Machines still in concept stage (Gestator, Drooling Crucible, Filling Station) don't have one yet, for the obvious reason.

### Skin Tank

**Status:** Function and capacity defined.

**What it is:** A simple fluid storage tank — no processing function, just bulk storage.

**Capacity:** `10 buckets` (10,000 mB) — by far the largest fluid capacity logged so far (10x a Beaker; 10x Drinker/Sipper's internal tank).

**Implant recipe (live in code, `skin_tank_implant.json`):** 1 **Beaker** + 2 **Dense Muscle** + 2 **Nerve Cluster**, injected with **100 mB Primitive Catalyst** (sutured) — the standard implant shape.

**OT-native recipe:** 1 Beaker + 1500 mB Protein Blend + 100 mB Primitive Catalyst, OT-assembled (no suture) — derived via the standard OT-native machine recipe formula (includes the universal Inert Tumor cost, not just the listed flesh ingredients); full worked-out math lives with the other worked examples (see OT-native machine recipes, under the Core section).

**Transport (new, confirmed) — Forceps pickup with contents preserved.** By the mod's general "living block" rule, breaking a block the vanilla way destroys it and drops only its contents, and a plain Forceps pickup recovers the block itself but *still* drops its contents (see Forceps, `dermicraft-tools-notes.md`) — the **Craw** is the sole existing exception, opting into the `IPreserveContentsOnPickup` marker interface (see Craw's own entry below) that keeps its contents inside the recovered block item so it relocates fully loaded. **Skin Tank now opts into that same `IPreserveContentsOnPickup` behavior** — a Forceps pickup recovers the tank with its fluid contents intact, letting a player physically relocate a full tank (e.g. seeding a new base, moving Slurry reserves) without needing floor-network infrastructure at the destination. This makes Skin Tank the fluid-side counterpart to the Craw's item-side "relocate fully loaded" precedent — the same symmetry already implied by Skin Tank and Craw being described as fluid/item counterparts elsewhere in this doc. **Inherited by Chitin Tank** (see its own entry below) as part of the same family/evolution lineage — no separate design needed.

**Evolution:** Forced-evolves into the **Chitin Tank** (Tier 2 — see its own entry below) via the standard "dumb" Machines forced-evolution mechanic (Evolution Catalyst injection) — see Machine Evolution — Smart vs. Dumb, above. Same pattern as Drooling Cauldron → Drooling Crucible.

**Automation access (confirmed) — fuel only, any face.** Automation (hoppers/pipes/capability-based access) can interact with the tank from **any face**, but **only for fuel-type fluids** — Skin Tank's automated role is specifically as a bulk **fuel** reservoir/distribution buffer, not a generic any-fluid bulk tank for automation purposes. **Applies to the whole family** (Chitin Tank and future evolutions inherit this rule automatically).

### Chitin Tank

**Status:** Core identity, hazard capability, evolution recipe, and capacity all decided.

**What it is:** The **Tier 2 evolved form of the Skin Tank** — same family, same simple bulk-fluid-storage function, no processing. Name leans on chitin, the organic shell/carapace material, fitting the primer's Tier 2 "obsidian carapace" visual theme (a fit reinforced further by its own evolution recipe — see below). **Not** a single-use vessel specific to any one recipe — it's a general-purpose Tier 2 tank, usable anywhere a Tier 2 tank is called for. Its first confirmed mention was as the tank required for the OT's own Brain → Core evolution (holding Molten Redstone), but that's simply its first appearance, not its defining purpose.

**Capacity:** **20,000 mB** — a clean double of Skin Tank's 10,000 mB, per the mod's general multiplicative-growth convention (see `dermicraft-project-primer.md` → Working conventions). **Growth rule confirmed for this family:** capacity **doubles at every subsequent tier upgrade** beyond this one (i.e. a future Tier 3 tank in this lineage would be 40,000 mB, Tier 4 would be 80,000 mB, and so on) — a fixed ×2-per-tier rule specific to the Skin Tank/Chitin Tank family, not just a one-off starting multiplier.

**Hazard capability:** Follows the same **cumulative Tier hazard rule** as the rest of the mod (Floor tiers, etc.) — Tier 2 means it handles **Tier 1 safe fluids + Tier 2 Extreme Heat (lava)** together, not a narrow "just Molten Redstone" restriction.

**Evolution recipe (Skin Tank → Chitin Tank):** follows the **standard Tier 2 evolution template** (see above) exactly — Lava Bucket in an item slot, the tank completely filled with Water, injected with Evolution Catalyst, everything consumed, Chitin Tank spawns in its place.

**Transport:** Inherits Skin Tank's Forceps-pickup-with-contents-preserved behavior (see Skin Tank entry, above) — no separate design needed, same family/lineage.

**Automation access:** Inherits Skin Tank's family-wide rule — **fuel only, any face** (see Skin Tank entry, above). Note this only restricts *automated* (hopper/pipe) access — **manual player interaction is unrestricted**, so a Chitin Tank can still be hand-filled with non-fuel fluids like Molten Redstone (its confirmed use for the OT's Brain → Core evolution) via Beaker/bucket; only automation is limited to fuel-type fluids.

**Open questions:** None remaining for Chitin Tank itself.

### Drooling Cauldron

**Status:** Core function defined.

**What it is:** A water-generating machine with two production modes:
- **Passive generation:** continuously creates water at a fixed rate of `4 mB/second`, with no input required.
- **Food-boosted generation:** accepts food items as input to create additional water, with the amount produced scaled to that food's nutrition and saturation values (vanilla hunger/saturation stats) via the shared `IVagueRecipe` formula (`amount = round(25 × nutrition × (saturation + 1))`, time = `round(12 × nutrition × (saturation + 1))` ticks) — same nutrition/saturation-driven approach as the Masticator's food recipes.

**Balance note:** passive + food-boosted output (≈2.28 mB/tick while actively fed) is tuned to slightly exceed one Masticator's Water draw (≈2.17 mB/tick, 1:1 with its Crude Slurry output) when both are fed the same food, so a Cauldron can keep a Masticator supplied with Water with a small margin to spare.

**Evolution:** Forced-evolves into the **Drooling Crucible** (Tier 2, produces lava instead of water — see below) via the standard Machines evolution process.

**OT-native recipe:** 1 Cauldron + 1500 mB Protein Blend + 100 mB Primitive Catalyst, OT-assembled (no suture) — full worked-out math with the other worked examples (see OT-native machine recipes, under the Core section).

**Open questions:** Does passive generation require anything (placement conditions, etc.) or is it unconditional? Internal tank capacity? Tier restriction?

### Masticator

**Status:** Core function defined; recipe-yield logic established; output buffer capacity and a confirmed refusal behavior now set. Full per-fluid recipe detail lives in the companion doc: `dermicraft-crafting-notes.md`.

**What it is:** A blending machine — takes one fluid input and one item input, combines them into a different output fluid. Confirmed as the machine behind all current Crafting fluids: the original six (Carbon Blend, Calcium Blend, Protein Blend, Ferrous Blend, Cuprous Blend, Aurous Blend) plus the newer Sediment Blends (Stone Blend, Silica Blend, Clay Blend) and the Blood Nugget → Ferrous Blend alternate route — see `dermicraft-crafting-notes.md` for full detail on all of these.

**Recipe yield logic:**
- Most recipes produce a **set/fixed amount** of output fluid.
- **Food-based recipes** are the exception — confirmed so far for **Crude Slurry** (plant-based food items only) and **Protein Blend** (any `MEAT_FOOD`-tagged item) — where the output amount is instead derived from the input food item's nutrition and saturation values, the same formula-driven approach as the Drooling Cauldron's food-to-water conversion.

**Base fluid pattern — corrected:** Water is the fluid input for Crude Slurry, Carbon Blend, Calcium Blend, and Protein Blend, but this is **not a universal rule** — Ferrous/Cuprous/Aurous Blend use Primitive Catalyst as their base instead, and the Sediment Blends (Stone/Silica/Clay) use Water (see `dermicraft-crafting-notes.md`). Don't assume water as a default for future Masticator recipes without checking case by case.

**Output buffer (Tier 1):** `5000 mB` (5 buckets).

**Confirmed behavior — output buffer check:** If a recipe's output would exceed the room currently available in the output buffer, the Masticator **will not process the recipe at all** — it simply refuses to start, the same "won't even attempt it" pattern Drinker uses for hazardous fluids, rather than processing and failing or wasting the input partway through. This is currently blocking one real recipe (Cuprous Ore, 7500 mB — see `dermicraft-crafting-notes.md`) until an evolved Masticator with a larger buffer exists.

**OT-native recipe:** 2 Bone + 1310 mB Protein Blend + 100 mB Primitive Catalyst, OT-assembled (no suture) — full worked-out math with the other worked examples (see OT-native machine recipes, under the Core section).

**Reachability gap (flagged, blocking) — no Tier 2 Masticator exists yet, name undecided.** The Tier 1 Masticator's fluid tanks are `VulnerableTank` (`HazardProfile.TIER_1` — rejects Lava/Extreme-Heat), so **the entire Stage 2 "Molten" fluid family is currently unreachable** — none of Molten Redstone, Molten Quartz, Molten Glowstone, etc., or Molten Soul Silica can actually be made in-game until an evolved Masticator with at least `TIER_2` hazard tolerance exists (presumably via the same forced-evolution/Evolution Catalyst pattern as Skin Tank → Chitin Tank — see that entry above for the template). Not a bug, just sequencing: don't build/register any Molten-family fluid's recipe before this evolution exists, or a survival player will have a fluid with no way to produce it. See [[feedback_survival_reachability_check]] in Claude Code memory.

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

**OT-native recipe:** 3 Glass Flask + 1500 mB Protein Blend + 100 mB Primitive Catalyst, OT-assembled (no suture) — derived via the standard OT-native machine recipe formula; full worked-out math lives with the other worked examples (see OT-native machine recipes, under the Core section).

**Still open:** A dedicated balance pass on the real recipe numbers (mB in/out per recipe) hasn't happened yet — current amounts are functional, not confirmed final.

**Open questions:** Are more result fluids planned beyond these three? A proposed reverse machine — one fluid splitting back into two — has been flagged as a concept only, not detailed; likely scoped to reversing Effluentcer's own outputs specifically (F-Stuff/C-Stuff) rather than a generic "unmixer," and likely lossy rather than a perfect 1:1 reverse if it's ever built.

### Upgraded Effluentcer (name not yet decided)

**Status:** Concept stage — required by a specific recipe, not yet designed as a machine in its own right.

**What it is:** A Tier/evolution step up from the base Effluentcer, adding a **third fluid input tank** (base Effluentcer has only `INPUT_A`/`INPUT_B` plus fuel and result). Directly required by the **Living Catalyst** recipe (Dragon's Milk + Molten Quartz + Molten Blaze Essence — see `dermicraft-catalyst-notes.md`), which needs three simultaneous fluid inputs the base machine can't support.

**Likely path:** Reached via the Effluentcer's own forced-evolution process (matches the established Drooling Cauldron → Drooling Crucible precedent) rather than being a separately-crafted machine, though this isn't confirmed yet.

**Open questions:** Name. Evolution trigger/recipe (what items/fluids stored over time, plus Evolution Catalyst injection, per the standard evolution pattern). Whether it gains any other capability beyond the third input tank. Tank capacity/face-mapping changes, if any, relative to base Effluentcer.

### Metastasizer

**Status:** **Operational.** Block (`MetastasizerBlock`), block entity (`MetastasizerBlockEntity`), menu, and screen are all implemented, and a wide roster of duplication recipes is live across several ingredient families. Works on a recipe-by-recipe basis. Now fully distinct from **Gestator**, which has been reassigned as the name of a new, separate machine (see its own entry below) — "Metastasizer" is the sole name for this machine going forward, no dual-naming ambiguity remaining.

**What it is:** A machine that takes a fluid + an item and produces a copy of that item. The item acts as a non-consumed **pattern** — only the fluid is spent. Used for making simple items (duplication-based, rather than an ingredient-based recipe).

**Conceptual framing:** essentially a **small 3D printer** — it scans a pattern item and prints copies from fluid "ink." This framing is also the root of the OT's "printing vs combining" recipe method (see OT — Brain OT-only recipes), and it's why the OT can run it **pattern-free** (it conjures a transient pattern to scan; see the OT's Metastasizer invocation rule).

**Recipe (implant) — now live in code:** 1× **Beaker** + 2× **Dense Muscle** + 4× **Nerve Cluster** + 1× **Eye**, sutured with the **Suture Kit** → injected with **100 mB Primitive Catalyst**. Deliberately pricier than the other machine implants (8 items vs. ~5) to reflect its powerful duplication ability. The Eye fits the 3D-printer framing (the scanner that reads the pattern). This was the last built machine without an implant recipe; the gap is now closed.

**Confirmed — works on a recipe-by-recipe basis.** Not a generic "any fluid + any item" machine — each valid fluid+item combination needs its own defined recipe, the same granularity the Masticator already uses for its fluid+item pairs. This resolves the earlier open question of whether any fluid would work; it doesn't.

**Confirmed — "produces a duplicate of the pattern item" is the primary/default behavior, not a hard rule.** It's the Metastasizer's main, intended use case, and every recipe implemented so far follows it (the once-planned Blood Nugget recipe, whose output would *not* be a duplicate, remains an unimplemented concept — see below).

**Confirmed — Sediment Blend duplication, costs now set.** Stone Blend, Silica Blend, and Clay Blend each have their own per-item Metastasizer recipes duplicating every block on that fluid's roster (17 items total across all three families — see `dermicraft-crafting-notes.md` Sediment Blends section for the full tier/cost table: 750/900/1000/250 mB by tier, 6s/8s/10s/2.5s craft time).

**Confirmed — Metal Blends reverse route.** Ferrous/Cuprous/Aurous Blend can now be turned back into their Ingot/Nugget-tier item via the Metastasizer, mirroring the Masticator's forward recipes 1:1 on fluid amount (1000 mB per Ingot, 110 mB per Nugget). No reverse recipe for Raw (never a 1:1 conversion) or Cuprous Nugget (no vanilla item). See `dermicraft-crafting-notes.md` Metal Blends section.

**Confirmed — MRE and Meat Flavored Meat duplication.** F-Stuff (900 mB, consumed) + an existing **MRE** (non-consumed pattern) → more MRE, and the same setup for **Meat Flavored Meat** using **Protein Blend** (900 mB) instead of F-Stuff — a new, simpler food item (nutrition 5 / saturation modifier 0.5) added alongside MRE (nutrition 6 / saturation modifier 0.6, matched to cooked chicken). Both foods also have a furnace/smoker bootstrap recipe (1 Filled Bucket of the relevant fluid → 1 food item, 200/100 ticks, 0.35 XP — matching vanilla raw beef/chicken cook times) to get the player's first copy before the Metastasizer loop takes over.

**Confirmed — Protein Blend → tumor/part duplication.** Protein Blend also duplicates **Inert Tumor** (1000 mB, solid tier), **Marred Tumor** (900 mB), and the three tumor-drop parts **Dense Muscle, Nerve Cluster, Eye** (250 mB each, light tier) — reusing the same tier/craft-time convention as the Sediment Blends. (As with all Metastasizer recipes, the OT can run these **pattern-free** via a transient conjured pattern — see the OT's Metastasizer invocation rule.)

**Confirmed — Bone/Bone Meal duplication.** Calcium Blend duplicates **Bone** (1000 mB, mirroring the Masticator's Bone → Calcium Blend recipe) and **Bone Meal** (330 mB, mirroring the Masticator's Bone Meal recipe) — same "reverse the Masticator recipe that consumes this item" pattern as the Metal Blends above.

**Confirmed — glass family via Silica Blend and Calcium Blend, sized by volume.** Real glass is made from both silica (sand) and lime (calcium) — so the mod gives glass items **parallel duplication routes** through either fluid, not just one:

| Item | Silica Blend route | Calcium Blend route |
|---|---|---|
| **Glass Block** | 1000 mB (the reference unit) | — |
| **Beaker** | 1000 mB | **1000 mB (new)** |
| **Glass Flask** | 250 mB — a quarter of the Beaker's cost, matching its **quarter volume relative to the Beaker** | **250 mB (new)** |
| **Glass Pane** | **500 mB (new)** | — |
| **Calcium Glass** | — | **1000 mB (new)** — Calcium Glass's own duplication route, fitting since it's already a calcium-based glass item (see its existing puddle recipe) |

All routes are **additive** — the existing vanilla-style crafting-table recipes (glass block smelting, Beaker's 3-glass recipe, Flask's 4-glass chalice-pattern recipe, Calcium Glass's puddle recipe) all stay, and Beaker/Flask having *two* valid fluid routes each is intentional flexibility, not a conflict (Metastasizer already supports multiple recipes per output item). Load-bearing for the OT: the Beaker and Flask were previously crafting-table-only (which the OT can't do), so this makes both **OT-sourceable** (Metastasizer, pattern-free) — unblocking OT-native machine recipes that need one (Metastasizer's own recipe and Skin Tank need a Beaker; Effluentcer needs Glass Flasks).

**Confirmed — Cauldron via Ferrous Blend.** Silica Blend duplicates the **vanilla Cauldron**'s glass-based cousins above, but the Cauldron itself is metal — Ferrous Blend duplicates it instead, at **7000 mB**, following the existing Ferrous Blend↔Ingot reverse rate (1000 mB per Ingot) literally against the Cauldron's real vanilla recipe cost of 7 Iron Ingots. Steep, but deliberately consistent with the established rate rather than a discounted one-off. Unblocks the Drooling Cauldron's OT-native recipe.

**Chest — deliberately no Metastasizer recipe.** Unlike glass/metal items, a Chest has no obvious fluid identity, so forcing a duplication recipe for it would be arbitrary. Until a future machine exists that can produce a Chest, **the player loads a real Chest in by hand** for the Craw's OT-native recipe — the one deliberate physical-sourcing gap in the OT-native machine set.

**Still a concept, not yet implemented — Blood Nugget:** Protein Blend (consumed) + an **Iron Nugget** (non-consumed pattern) → **Blood Nugget**, feeding into a Masticator recipe (Blood Nugget + Primitive Catalyst → Ferrous Blend). This remains the one designed-but-unimplemented recipe on this machine, and would be the first real example of the Metastasizer's output *not* being a duplicate of its pattern item — no longer blocked on the machine existing, just on the Blood Nugget item/recipe itself being built. See `dermicraft-crafting-notes.md` for the full chain and "low but fair" yield reasoning.

**Open questions:** What counts as a valid pattern item beyond "simple item" now that Blood Nugget (if/when built) would show the output doesn't have to be a duplicate at all — is there still any restriction on pattern items, or is it purely whatever a defined recipe specifies?

### Gestator

**Status:** Core mechanic decided. A new, distinct machine — not a rename of the Metastasizer above (which previously carried this name before it was reassigned here). Fuel/health details still open pending the broader mod-wide fuel-optional discussion flagged below.

**What it is:** Takes **Living Catalyst** (renamed from "Life Catalyst" — see `dermicraft-catalyst-notes.md`) and one other fluid as inputs. Behavior branches on whether that second fluid is living. **Confirmed live examples:** Molten Glowstone → Living Glowstone, Molten Netherite → Living Netherite (see `dermicraft-crafting-notes.md`).
- **Non-living second fluid, with a living counterpart that exists:** the two mix to produce the **living version** of that fluid.
- **Living second fluid:** produces a **large batch of that same living fluid** — framed as a visual/functional illusion of a greatly accelerated replication rate, not an actual permanent change to that fluid's own inherent replication speed.

**Invalid input handling — deliberate deviation from the usual fail-safe refusal pattern.** If the second fluid has no living counterpart, the Gestator does **not** refuse outright the way Drinker/Masticator do. Instead the fluid simply **pools inside the machine** and must be manually extracted — either via a dedicated Gadget built specifically to make this easy, or via ordinary automation (pumping it back out). This is a softer version of the mod's fail-safe philosophy: bad input doesn't process, but it's recoverable rather than instantly rejected.

**Fuel — tube-driven, not Slurry-dependent (at least for now).** The Gestator runs without Slurry fuel, operating instead on an internal **tube-driven timing cycle** — a slow passive pulse rather than continuous fuel draw. Introducing Slurry speeds the process up, but isn't required. Reasoning: Living Catalyst is already a scarce, premium-cost input, so the Gestator's own use-frequency is inherently low — an additional Slurry cost on top would be punishing relative to how rarely the machine gets used. This also gives the Gestator a different operating *shape* than every other Machine so far (passive cycle vs. active fuel draw), fitting its "incubating/gestating" name and the bio-horror lean well.

**Flagged for a dedicated future session:** whether this fuel-optional/tube-driven model should become the standard for **all** Machines, with the tradeoff of making unfueled operation deliberately "painfully slow" across the board rather than the Gestator being a one-off exception. Not decided — a real possibility, not a commitment.

**Open questions:** Exact tube mechanic (what tubes are, how they're placed/connected). Base cycle speed unfueled vs. Slurry-boosted speed multiplier. Full roster of which fluids have a living counterpart (currently assumed universal — "every fluid can have one" — with the pooling behavior above as the safety net for any that don't). Whether the Gestator itself will ever get a tumor-genesis recipe like other Machines, or whether it's built differently given its unusual mechanic.

### Drooling Crucible

**Status:** Concept stage only. First confirmed **Tier 2** machine.

**What it is:** An evolved form of the Drooling Cauldron — produces lava instead of water. Fits the Tier 2 pattern established elsewhere (lava capability is the defining Tier 2 upgrade — see `dermicraft-project-primer.md` Stage structure).

**Confirmed:** Drooling Cauldron → Drooling Crucible is a real example of the Machines' forced-evolution mechanic — the Cauldron is forced into evolving into the Crucible via that process (Evolution Catalyst injection completes the transformation, per `dermicraft-project-primer.md` Machines).

**Open questions:** Does it keep Drooling Cauldron's food-boosted generation mode, adapted to lava (and if so, boosted by what kind of item)? Passive generation rate (same 1 mB/sec as Cauldron, or different for lava)?

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

**OT-native recipe:** 1 Chest + 1500 mB Protein Blend + 100 mB Primitive Catalyst, OT-assembled (no suture, but still requires a hand-supplied Chest — Craw's own physical-sourcing gap) — full worked-out math with the other worked examples (see OT-native machine recipes, under the Core section).

**Textures:** New `craw_side` for the block sides; top and bottom reuse Skin Tank's existing end texture. The GUI reuses the shared `screen_parts` (no bespoke GUI texture).

**Technical caveat (bulk storage vs. vanilla's 99-count cap):** the storage slot holds a single stack of up to 640, but Minecraft's ItemStack NBT codec rejects any saved count above 99. So the Craw serializes its storage manually (item type from a count-1 copy + the real count as a separate int) and splits its drops into ≤64 pieces on break. This is the reference pattern for every future bulk-storage block — see the working-conventions note in `dermicraft-project-primer.md`.

**Open questions:** Scaling mechanic (capacity is expected to scale somehow, not yet designed).

### Operating Theater ("OT") — component blocks

**Status:** Core purpose, structure, and design role established; confirmed as the standard construction path for Gadgets and, more broadly, as a universal crafting front-end. Three physical component blocks now detailed below: **Floor Block**, **Craw** (see above; doubles as OT output storage), and **Core**.

**What it is:** A **multiblock structure** — the first of its kind logged here, distinct from every other Machine so far, which are single blocks. Built around a **control block** — which advances through tiers (Tier 1 **Brain** → Tier 2 **Core**; see Control Tiers below) — plus dedicated **Floor** blocks. Any Dermicraft machines and storage devices attached to the floor, within range of the control block, feed into/provide for the OT.

**Confirmed — primary purpose is crafting Gadgets, but scope is broader.** The OT is the **standard, intended way Gadgets get built**, distinct from both the Tool category (no dedicated construction chain established yet) and Machines (hand-crafted via the Inert Tumor chain — see `dermicraft-tools-notes.md`). This gives the mod three parallel construction paths at three different scales/complexity levels:
- **Tools** — no dedicated construction mechanic yet.
- **Machines** — hand-crafted, one at a time, via the Inert Tumor chain (Syringe injection completes it).
- **Gadgets** — OT-crafted, the automation hub's signature output.

**Confirmed — craftable scope is universal, not Gadget-limited.** If the OT itself or any attached Machine on the floor network has a recipe for something, the OT can craft it. This makes the OT a universal front-end over the whole connected Machine network's combined recipe set, not a Gadget-specific device.

The three-way Tools/Machines/Gadgets construction split above is now **confirmed deliberate** — framed as a dependency ladder (Tools build Machines; the apex Machine, the OT, builds Gadgets) with a **living-matter through-line** as its real law (everything past simple Tools uses living matter, shifting from organic flesh to living metals over the Stages). Full framing, rules, and the Nose-on-a-Stick exception in `dermicraft-project-primer.md` → Construction philosophy.

**First confirmed OT-crafted Gadget — the Tablet.** See `dermicraft-gadget-notes.md` for full detail. Notable as the first Gadget built from **living materials**, raising an open question about whether the OT has its own distinct "how it builds living things" logic, separate from the hand-crafted tumor chain, or whether it's secretly still running some version of that same chain internally, just automated.

**Design role:** Envisioned as the mod's central automation hub — in the player's own words, "big daddy in automation." **Completable within Stage 1** as its Tier 1 milestone — the player builds it to a functional (if severely limited) state run by the Tier 1 **Brain** control, with no Stage 2 dependency required to *finish* or *use* it. Later stages **upgrade** it rather than complete it. Not strictly necessary to play or finish the mod.

**Control Tiers (open-ended — only Tier 1 and Tier 2 defined so far):** the OT's control block advances through tiers, each a bigger, more capable brain; more are expected beyond the two below.
- **Tier 1 — Brain:** completes the OT in Stage 1. Functional but severely limited (see Brain — Tier 1 Control, below) — this is what makes the OT a genuine Stage 1 milestone rather than a Stage 2 one.
- **Tier 2 — Core:** the Brain **evolved** in Stage 2 via the Evolution Catalyst step (see recipe below), lifting the Brain's limits. The Core sections below document the OT's control behavior in general; the Brain runs the same systems under tighter limits.

**Evolution — two senses, at two scales (the word deliberately does double duty):**
- **Component (forced) evolution** — the biological sense the mod already uses: individual machines and the control block are *made* to transform to improve, via the tumor-genesis / Evolution Catalyst mechanic — a discrete, triggered event. The control block advances through its Control Tiers (above) this way; **Brain → Core (Stage 2) is the first confirmed step**, more tiers expected beyond the Core (how many, and what each unlocks, not yet set).
- **Structural (whole-OT) evolution** — the general/dictionary sense: the OT as a whole is never "finished." It evolves gradually as the player iteratively **swaps parts out for better ones** — better Floor variants, upgraded storage, a higher control tier, adding or rearranging machines. No single trigger; it's the aggregate of the player's ongoing improvements. This is exactly the **OT-wide modularity** rule below, viewed as a progression mechanic — modular part-swapping *is* the mechanism of structural evolution, and **Knitting** is the OT re-settling after each such step.

**Relationship to Syringe (resolved):** The two split cleanly — **Syringe** crafts Machines **by hand**; **OT** is the **automated** counterpart, and now confirmed as a universal crafting front-end rather than Gadget-only. Not overlapping roles.

**OT-wide modularity (new, general rule):** All OT component parts — Floor, Core, flanking output storage — can be **upgraded, changed, or moved around** after initial construction; the OT is not a fixed one-time build. Structural changes trigger **Knitting** (see Core — Recursive Crafting & Network Behavior, below) — the network's event-driven recompute/reconnection pass — rather than an always-on continuous check.

**Open questions:** New concept or carried over from the old version? Exact range of the Core's influence (Core-level property, mechanics deferred to Code). What counts as "attached to the floor" — resolved as **touching a Floor piece on any face** (in the floor plane, under, or on top); face doesn't matter, only contact (see Floor Block below). How many evolution stages does the OT have, and what does each one unlock? Whether the OT's living-material construction (Tablet) implies a general rule for all future living Gadgets, or is a one-off.

#### Floor Block

**Status:** Core purpose, connection behavior, and a two-material bootstrap recipe pair decided. Material progression roster open-ended.

**What it is:** A pure pass-through connector — conducts both **items and fluids** between the Core and attached Machines/storage, but never stores either itself.

**Connection — any-face contact (revised, supersedes the old "on top only" rule):** a machine or storage block connects to the network if it is **touching a required Floor piece on any face** — sitting in the floor plane ("part of the floor"), directly under a Floor piece, or on top. **Face doesn't matter, only contact.** This flexibility is *why* the OT accesses Dermicraft machines directly rather than through capabilities (see Machine access, Core section) — with no single predictable connection face, face-based capability access would get in the way. The only place faces still matter is **foreign (non-Dermicraft) blocks**, which the OT reaches through the capability path where sided access applies.

**Connection requirement — within-reach connectivity:** Floor blocks must form a connected network, but "connected" is now **within-reach**, not strictly adjacent — each floor bridges up to its own tier's reach (see Floor Tiers below: Tier 1–2 adjacent-only, Tier 3–4 one-block gap), and that reach applies to both floor-to-floor spacing and machine/storage connection. The **control block's range** is still the outer boundary of the whole structure (Brain's cube, Core's larger range); floor reach is the *local* connectivity within it, and floors must be present to propagate connections through the structure at all.

**Fluid-output-face conflict — resolved.** Previously flagged: existing machines output fluid from the bottom face, which conflicts with sitting a machine on top of a Floor block. This is now resolved by the OT's **direct machine access** (see Core — Recursive Crafting & Network Behavior): because the OT reads/writes Dermicraft machines' tanks directly rather than through face-based capabilities, block faces are irrelevant to OT transfer entirely — no output-face override needed.

**Material progression (variants tied to tiers):** Floor blocks come in multiple variants tied to different structural materials, mirroring a mini progression rather than a single fixed recipe:
- **Stone Floor**, **Cobblestone Floor**, **Deepslate Floor**, **Cobbled Deepslate Floor**, **Diorite Floor**, **Andesite Floor**, and **Granite Floor** — seven separate implant recipes, each hand-craftable as an early (Tier 1) bootstrap variant (see recipe below).
- **All other variants** — most likely **OT-crafted only**, no implant recipe of their own. Once enough Floor blocks exist to build a working Core and initial structure, the OT can craft further/better Floor blocks itself — a self-replication loop, consistent with the "prefer Dermicraft-native processes" convention.
- **Metal variants confirmed planned as Tier 2:** one Floor variant per main metal (Ferrous, Cuprous, Aurous), matching the existing Metal Blend trio. Exact recipes deliberately deferred until more Stage 2 material exists to design them against.
- **Variants are functional, not cosmetic** — a variant's material sets its **tier**, which carries the functional properties below. (Supersedes the earlier "currently cosmetic only" note.) Roster still open-ended.

**Floor Tiers — function (fluid-type, throughput, reach):** a floor's tier bundles **three escalating properties** — one tier number, advancing all three at once:

1. **Fluid-type capability** — what hazard classes the floor can conduct, following the mod's Stage-hazard ladder (see `dermicraft-project-primer.md` → Stage structure / Hazard tag hierarchy). Cumulative — each tier handles its own hazard class **and all below it**, via the tier-appropriate material (it's not "free"; higher tiers incorporate the materials that actually solve each hazard, consistent with the primer's "heat-handling ≠ radiation-handling" principle):
   - **Tier 1** — safe fluids only (no hazardous). *(Stage 1)*
   - **Tier 2** — + Extreme Heat (lava). *(Stage 1→2 gate)*
   - **Tier 3** — + Mild Radiation. *(late Stage 2 / early Stage 3)*
   - **Tier 4** — + Severe Radiation and Biohazard. *(late Stage 3 / Stage 4)*
2. **Throughput** — how fast items/fluids move through the floor; higher tier = faster.
3. **Reach** — the floor's *local* connection span (see Connection requirement above): **Tier 1–2 = adjacent only** (0 gap); **Tier 3–4 = one-block gap**, applied to both floor-to-floor spacing and machine/storage connection.

**Path resolution (computed during Knitting):** a "path" is the chain of floors a fluid/item traverses between a machine/storage block and the control block.
- **Fluid-type capability = the lowest-tier floor in the path** (weakest link) — a single Tier 1 floor anywhere along a route caps that route at "no hazards," regardless of the other floors.
- **Throughput = the lowest-tier floor in the path** (the choke point) — same weakest-link rule.
- **Reach = per-floor, piece by piece** (local, *not* weakest-link) — each floor bridges its own gap by its own tier.
- **Knitting checks the paths** — path-tier calculation and reach/connectivity validation are part of the Knitting recompute pass, not a per-tick check.

**Unsupported fluid = hard block:** if a recipe needs a fluid the relevant path can't conduct, it's blocked with the standard Core-GUI error (sound + message) until the floor network is upgraded to carry it.

**Recipe (implant) — Tier 1 Floor variants, quantities finalized:** each variant is **1× structural block + 1 Dense Muscle + 4 Nerve Cluster**, injected with **100 mB Primitive Catalyst** (sutured) — the standard implant shape used mod-wide. Dense Muscle + Nerve Cluster represent plumbing/wiring (the connective function); the structural block is the only thing that changes per variant:

| Variant | Structural block |
|---|---|
| Stone Floor | 1 Stone |
| Cobblestone Floor | 1 Cobblestone |
| Deepslate Floor | 1 Deepslate |
| Cobbled Deepslate Floor | 1 Cobbled Deepslate |
| Diorite Floor | 1 Diorite |
| Andesite Floor | 1 Andesite |
| Granite Floor | 1 Granite |

**Open questions:** Exact ingredient quantities. Recipes/materials for each tier's variants (metal and beyond). Exact throughput and reach numbers per tier. How many tiers/variants total (open-ended, but at least the four hazard tiers above).

#### Autonomous Structure Growth

The OT builds itself out through **three related-but-distinct behaviors** — the first already noted (request-crafting), the latter two new. All three make the OT the mod's one visibly "living," self-extending structure, and reinforce the **Structural (whole-OT) evolution** concept above.

1. **Request-crafting (existing):** on demand, the OT crafts more Floor pieces *into* network storage (the self-replication loop already noted under Floor Block). Produces stock; does not place anything.
2. **Self-Build (new, toggleable):** when enabled via a GUI toggle, roughly **every ~30 seconds** the OT takes one eligible piece from network storage and **places it into the structure**, if one is available (if not, it simply does nothing that tick).
   - Eligible pieces: **Floor blocks, machines, and storage devices** held in storage.
   - Placement extends the existing network — a Floor block is copied adjacent to an existing Floor block; machines/storage are installed into valid connected spots (touching the Floor network, per the any-face rule), within the control block's range.
   - Player-controlled: feed the OT materials and it assembles itself. Consumes the stored piece.
3. **Growth (new, always-on, not player-controlled):** a small chance rolled **~every half hour**, **free** (nothing consumed) — the OT picks a **random existing Floor block and places an identical copy adjacent to it**, so it grows using whatever variants are already in the structure.
   - **Floor blocks only** — never machines or storage.
   - **No toggle** — an inherent living behavior, unlike Self-Build.
   - **Fuel grade modulates the chance** (better grade → slightly more likely); fuel is only *read*, never consumed for growth, so this doesn't conflict with the "idle machines don't consume fuel" rule.
   - Placement is contiguous and into valid empty space only, within the control block's range — so growth scope scales automatically with control tier (a Brain's small cube vs. a Core's larger range).

**Open questions:** exact Self-Build interval and Growth chance/cadence numbers. How machines/storage devices choose their placement spot during Self-Build (Floor placement is defined; machine/storage placement logic is not). Whether Self-Build should ever be smart about *what* it pulls (prioritizing floors vs. machines) or stay simple.

#### Core

**Status:** GUI/crafting behavior, output flanking, fuel/HP model, and the full Brain-build + Core-evolution recipe decided. Core-tier range and recursion depth deferred to Code.

**What it is:** The OT's **Tier 2** control block — the Brain evolved (see Control Tiers above). Functions like a brain (thematically, not necessarily in shape), tying back to the resurrected **Brain Block** it grows from. Everything in this Core section — GUI, crafting scope, recursion, network behavior — describes the OT's control behavior generally; the **Brain (Tier 1)** runs the same systems under the tighter limits in its own subsection below.

**Player interaction:** Right-click opens a GUI displaying available crafting options. Selecting an option displays its ingredient list. A button starts crafting.

**Craftable scope:** Universal front-end over the combined recipe set of the OT itself and every attached Machine on the floor network — not limited to Gadgets. If the OT or any connected Machine has a recipe for something, the OT can craft it.

**Recursive crafting:** If direct ingredients aren't available but the ingredients-to-make-those-ingredients are, crafting still begins — the Core actively routes sub-crafts through connected Machines (e.g. a Masticator produces a needed Blend on demand), and crafts some outputs itself directly. **Recursion depth limit is an open question for Code**, flagged specifically as a lag/performance risk on the server.

**Queueing:** Single job at a time. All production runs one batch at a time until the request's amount-remaining counter is satisfied (see Universal batch processing under Core — Recursive Crafting & Network Behavior). Explicitly provisional — may expand later.

**Output:** Flanked by a Skin Tank and Craw (the same blocks documented elsewhere — no bespoke OT-specific storage) where the player collects finished results. These flanking pieces are **separate from and unrelated to** the tank used in the Core's own construction (see the Tier 2 evolution step below), and can be swapped for different/upgraded storage as part of the OT's general modularity (see above).

**Fuel/HP — fuel-required, no HP, heal repurposed into efficiency:**
- **No HP mechanic.** The OT is the first confirmed Machine to use fuel while opting out of the standard HP/health-drain system entirely. With no HP pool, it has no damage-grace to spend, so it is **fuel-*required*** — unlike the fuel-*optional* machines that limp along on HP (the Masticator template). Since Slurry is only burned while actively processing, an idle OT never stalls.
- **Speed:** fuel grade drives OT processing speed as with other machines, and also modulates the autonomous Growth chance.
- **Heal → efficiency (repurposed):** because there is no HP for the fuel's heal modifier to act on, the OT **folds the heal factor into the use-rate factor to its own benefit** — heal *reduces* the OT's effective fuel use rate, so fuel lasts longer. On the Main Line (where heal scales with grade) this makes **better fuel disproportionately more fuel-efficient in the OT specifically** — premium fuel runs both fast *and* long here. Exact formula (e.g. use ÷ heal) deferred to Code; design intent is "heal lowers use for the OT." Thematically reframes "fuel heals the OT" into "fuel sustains it longer," fitting the living-structure theme. (Serum-family fuels, with their inverted/zeroed heal values, would interact oddly here — flagged as open if Serums are ever OT-usable; Main Line is the clean case.)

**Stop-on-fuel-out behavior:** when fuel runs out mid-craft, the OT **halts immediately** and **preserves** the in-progress batch's progress (not lost), surfaces the standard error/warning in the Core GUI, and **the player decides the next action** — add fuel and Continue (resume from preserved progress) or Cancel. Same pattern as the ingredient-exhaustion recovery.

**Recipe — build the Brain (Stage 1), then evolve it into the Core (Stage 2):**

1. **Tier 1 build (Stage 1) — Brain Block:** a **full stack (64) of Nerve Clusters** is placed in a Craw, then injected with **Primitive Catalyst** → produces the **Brain Block**. Single-part by necessity — a **Craw holds only one item type at a time**, so this step can only take Nerve Clusters; later evolution recipes (not bound to a single Craw) can add more parts. The Brain is a **living block**, born directly of the injection (the Machine living-construction method) — no separate puddle-growth step and not a plain inert block, consistent with the living-matter law (see `dermicraft-project-primer.md` → Construction philosophy). This **completes the OT** as a functional Stage 1 control — no further step is required to *use* the OT (see Brain — Tier 1 Control for its limits). (Brain Block is a revived item from the pre-rebuild version of the mod, previously called "Smooth Brain," now upgraded to block form — also usable as a decoration block, giving it a second use beyond this recipe per the mod's "every item needs more than one use" convention.)
2. **Tier 2 evolution (Stage 2) — Core (revised — now matches the Gear Stations evolution model, see `dermicraft-gear-stations-notes.md`):** the Brain **evolves in place** — it is never removed, pulled into a tank's input slot, or otherwise physically extracted during the process; it stays functioning as the OT's live control block right up until the transformation completes. The OT's screen displays the evolution's fluid requirements; once the connected floor network's shared pool holds enough, the player presses a button to begin a **timed** evolution process. **If interrupted, the consumed fluids are lost but the Brain itself is unaffected** — same fail-safe as Gear Station evolution.
   - **Requirements:** **250 mB Evolution Catalyst** (bumped up from the old single-shot 100 mB) + **20,000 mB Molten Redstone** (one Chitin Tank's now-confirmed capacity), both consumed on completion — consistent with the shared-pool model, it **does not need to come from a single tank**; any combination of tanks on the platform totaling 20,000 mB satisfies the requirement.
   - **Chitin Tank** (see its own full entry under Known machines → Skin Tank → Chitin Tank) is **not consumed** by this evolution — only the fluids inside it are. This recipe was simply its first mention, not its defining purpose.
   - Mechanically distinct from the old fill-and-inject ritual, but still a **fixed, one-time recipe** (unlike Gear Stations' per-tier-scaling costs) — same family of mechanic, different species: a singular, higher-stakes event for the OT's own control block vs. a repeatable per-tier process for Gear Stations.

**Cross-reference resolved:** This gives **Molten Redstone** a second confirmed use beyond the Redstone Torch Dip, resolving the open question logged in `dermicraft-crafting-notes.md`.

**Open questions:** Core's exact range value and recursion depth cap number (methodology now decided — see below — but the actual numbers are deferred to Code). Exact heal→use-rate efficiency formula for the OT (resolved in principle — see Fuel/HP — but the math is deferred to Code).

#### Brain — Tier 1 Control (limitations)

**Status:** Limitations decided. OT-only recipe short list still open.

The Brain is the OT's Stage 1 control block. It runs **all** the same crafting/network systems documented in the Core sections below (recursion, universal batching, drain-to-storage, direct machine access, storage-first fulfillment, Knitting), but under these hard limits until evolved into the Core:

- **Tier gate:** controls only **Tier 1** machines and storage devices. Higher-tier blocks are out of reach until the Core evolution.
- **Range — 5×5×5 cube centered on the Brain block:** ±2 on every axis. A cube/box check, not a radial sphere — also cheaper to compute than a distance check. A machine/storage block must be inside this cube **and** touching the Floor network (any-face rule) to be controlled. (The Core's own range is larger, deferred to Code.)
- **Recursion — full, but shallow in practice:** the Brain runs the complete recursive resolver (cycle detection, sub-craft routing, batch loop), not a cut-down version; its other limits keep trees shallow on their own. **Depth anchor:** Primitive Catalyst — `Effluencer(F-Stuff + C-Stuff)` → each `Effluencer(two Blends)` → `Masticator(Blend)`, ~3 crafting layers, every machine Tier 1 — is the confirmed in-scope case the Brain must handle comfortably.
- **Recipe breadth:** crafts anything its connected Tier 1 machines can, **plus a few OT-only recipes** (candidate list below — some may instead be handed to a real machine later, the way F-Stuff/C-Stuff went to the Effluencer). Last-resort Early Implant/Puddle Crafting behavior is general OT behavior (see Recipe priority, below), not Brain-specific.

**OT-only recipes (candidate list — Tier 1 Brain; provisional, any may later migrate to a dedicated machine). All are NEW and additive — the existing `early_implant` recipes for these tumors are kept (no recipe removed without direct order):**
- **Variant tumors — each = 1000 mB Protein Blend + the one harvested part that defines it (all inputs consumed):**
  - **Eye Tumor** ← 1000 mB Protein Blend + 1 **Eye**
  - **Nerve Tumor** ← 1000 mB Protein Blend + 1 **Nerve Cluster**
  - **Muscle Tumor** ← 1000 mB Protein Blend + 1 **Dense Muscle**
  - (The base **Inert Tumor** is *not* an OT-only recipe — it uses the existing Metastasizer Protein-Blend duplication recipe. **Marred Tumor** is likewise a Metastasizer recipe, 900 mB Protein Blend — not OT-only.)
- **Stitched Tumor** ← 950 mB Protein Blend, no defining item — *printed* directly from fluid (see method note below).
- Because each defining part has a Metastasizer duplication recipe (Protein Blend, 250 mB) and the OT runs the Metastasizer **pattern-free**, this whole chain is cleanly OT-craftable end-to-end with no last-resort recipes: Masticator → Protein Blend, Metastasizer → the part, OT → the variant tumor.

**OT recipe method — "printing" vs "combining":** OT recipes currently favor **printing a result directly from fluid** (materializing it — e.g. Protein Blend → tumor) over **combining multiple components** into a result. That's why **binding agents** — the term covers **catalysts and slurries injected in hand-crafted recipes** (e.g. Primitive Catalyst) — barely feature in the OT's print recipes so far; printing doesn't need them (it's also why even a "Stitched" Tumor is printed from Protein Blend rather than assembled from stitching/binding materials). Combining-style recipes — which *do* use binding agents — become more common as the OT starts building **machines** (see OT-native machine recipes, below), the first real home for this pattern. **Do not retro-add binding agents to the tumor print recipes above** without direct order.

**OT-native machine recipes — standing formula (corrected/clarified).** Unlike tumors (pure prints), machines are inherently **combine** recipes — every implant recipe pairs one defining vanilla item with organic "flesh" parts and an injected binding agent, and physically starts from an **Inert Tumor** block (the vessel that gets sutured shut and injected — see "Every Machine here is physically born from an Inert Tumor block," above). The OT-native version of each machine recipe follows one consistent, derived formula rather than being invented per-machine:
- **Keep the machine's one defining/structural vanilla item physical** — whatever gives the machine its functional identity (Beaker, Cauldron, Chest, Glass Flask, Bone, etc.). This is deliberate: it keeps the Metastasizer *meaningfully in the loop* — if every ingredient became fluid, the Metastasizer (and the whole physical-parts economy) would lose its purpose within the OT.
- **Total flesh cost = (Inert Tumor's own Metastasizer duplication cost + every listed "flesh" ingredient's Metastasizer duplication cost) summed, then discounted to 75%** of that total, rounded down to the nearest 10 mB. **The Inert Tumor's cost is universal, not conditional** — every machine is born from one whether or not it appears as a listed ingredient in the implant recipe (since the OT skips having a physical Inert Tumor at all when it prints a machine directly, its fluid-equivalent cost still has to be paid). "Flesh" ingredients are Dense Muscle, Nerve Cluster, Eye, and any other harvested-part item a specific recipe lists.
- **Any other "simple" (non-flesh, non-structural) item in the base recipe** — if a recipe calls for something else entirely (not the structural item, not flesh) — also converts to its own established fluid-equivalent cost and gets the **same 75% discount**, applied separately from the flesh sum above. None of the machines documented so far have needed this term (it's 0 for both worked examples below), but it's part of the general formula for future recipes that do.
- **Keep the recipe's injected binding agent** (e.g. Primitive Catalyst) **unchanged** — same amount as the implant, no discount.
- **Drop the suture-tool requirement** — the OT assembles automatically; suturing is a hand-crafting step that doesn't apply.

**Worked example — Metastasizer (resolves the previously-missing implant recipe too):**
- **Implant recipe (new — the Metastasizer had none before):** 1 **Beaker** + 2 **Dense Muscle** + 4 **Nerve Cluster** + 1 **Eye**, injected with **100 mB Primitive Catalyst** (sutured). Pricier than the other implants (8 items vs. ~5) to reflect its powerful duplication ability. Conceptual framing: the Metastasizer is a small **3D printer** — it scans a pattern and prints copies from fluid "ink"; the Eye fits as the scanner.
- **OT-native recipe:** Beaker stays physical (the printer's housing). Flesh cost: (2 Dense Muscle + 4 Nerve Cluster + 1 Eye) × 250 mB each = 1750 mB, **plus** the (universal) Inert Tumor cost (1000 mB) = 2750 mB raw, × 75% = 2062.5 mB, rounded down to **2060 mB Protein Blend**. No other simple items. Catalyst unchanged.
  - **Final: 1 Beaker + 2060 mB Protein Blend + 100 mB Primitive Catalyst.** OT-assembled, no suture.

**Worked example — Skin Tank (corrected — previously omitted the universal Inert Tumor cost):**
- **Implant recipe (live in code):** 1 **Beaker** + 2 **Dense Muscle** + 2 **Nerve Cluster**, injected with **100 mB Primitive Catalyst** (sutured).
- **OT-native recipe:** Beaker stays physical. Flesh cost: (2 Dense Muscle + 2 Nerve Cluster) × 250 mB each = 1000 mB, **plus** the (universal) Inert Tumor cost (1000 mB) = 2000 mB raw, × 75% = **1500 mB Protein Blend** (clean multiple of 10, no rounding needed). No other simple items. Catalyst unchanged.
  - **Final: 1 Beaker + 1500 mB Protein Blend + 100 mB Primitive Catalyst.** OT-assembled, no suture.

**Worked example — Effluentcer:**
- **Implant recipe (live in code, `effluentcer_implant.json`):** 3× **Glass Flask** + 2 **Dense Muscle** + 2 **Nerve Cluster** (sutured), injected with **100 mB Primitive Catalyst**.
- **OT-native recipe:** all 3 Glass Flasks stay physical (Effluentcer's defining/structural requirement, same role Beaker plays elsewhere — it just happens to need three). Flesh cost: (2 Dense Muscle + 2 Nerve Cluster) × 250 mB = 1000 mB, **plus** the universal Inert Tumor cost (1000 mB) = 2000 mB raw, × 75% = **1500 mB Protein Blend** (same flesh composition as Skin Tank, same result). No other simple items. Catalyst unchanged.
  - **Final: 3 Glass Flask + 1500 mB Protein Blend + 100 mB Primitive Catalyst.** OT-assembled, no suture.

**Worked example — Drooling Cauldron:**
- **Implant recipe (live in code, `drooling_cauldron_implant.json`):** 1 **Cauldron** + 2 **Nerve Cluster** + 2 **Dense Muscle** (sutured), injected with **100 mB Primitive Catalyst**.
- **OT-native recipe:** Cauldron stays physical (its own Metastasizer-sourcing route — 7000 mB Ferrous Blend, see Defining-item sourcing below — is a separate way to *obtain* a physical Cauldron, not a substitute for keeping it physical here). Flesh cost: (2 Nerve Cluster + 2 Dense Muscle) × 250 mB = 1000 mB, **plus** the universal Inert Tumor cost (1000 mB) = 2000 mB raw, × 75% = **1500 mB Protein Blend**. No other simple items. Catalyst unchanged.
  - **Final: 1 Cauldron + 1500 mB Protein Blend + 100 mB Primitive Catalyst.** OT-assembled, no suture.

**Worked example — Masticator:**
- **Implant recipe (live in code, `masticator_implant.json`):** 2× **Bone** + 2 **Dense Muscle** + 1 **Nerve Cluster** (sutured), injected with **100 mB Primitive Catalyst**.
- **OT-native recipe:** both Bones stay physical (Masticator's defining/structural requirement, same "needs more than one" pattern as Effluentcer's 3× Glass Flask). Flesh cost: (2 Dense Muscle + 1 Nerve Cluster) × 250 mB = 750 mB, **plus** the universal Inert Tumor cost (1000 mB) = 1750 mB raw, × 75% = 1312.5 mB, rounded down to **1310 mB Protein Blend**. No other simple items. Catalyst unchanged.
  - **Final: 2 Bone + 1310 mB Protein Blend + 100 mB Primitive Catalyst.** OT-assembled, no suture.

**Worked example — Craw:**
- **Implant recipe (live in code, `craw_implant.json`):** 1 **Chest** + 2 **Dense Muscle** + 2 **Nerve Cluster** (sutured), injected with **100 mB Primitive Catalyst**.
- **OT-native recipe:** Chest stays physical — and **must be physically supplied**, since Chest has no Metastasizer fluid route at all (see Defining-item sourcing below), the mod's one deliberate physical-sourcing gap. Flesh cost: (2 Dense Muscle + 2 Nerve Cluster) × 250 mB = 1000 mB, **plus** the universal Inert Tumor cost (1000 mB) = 2000 mB raw, × 75% = **1500 mB Protein Blend**. No other simple items. Catalyst unchanged.
  - **Final: 1 Chest + 1500 mB Protein Blend + 100 mB Primitive Catalyst.** OT-assembled, no suture (still requires a hand-supplied Chest).

**Defining-item sourcing, resolved for all five machines (see Metastasizer entry above for full detail):**
- **Beaker** (1000 mB Silica Blend) — sources the Metastasizer's own recipe and the Skin Tank's.
- **Glass Flask** (250 mB Silica Blend each) — sources the Effluentcer's 3× Glass Flask requirement.
- **Cauldron** (7000 mB Ferrous Blend) — sources the Drooling Cauldron's.
- **Bone** and **Chest** are the two defining items **without** a Metastasizer route: Bone already has one (Calcium Blend, existing), so the Masticator is fully OT-sourceable. **Chest has none by design** (no sensible fluid identity) — the Craw's OT-native recipe is the one deliberate physical-sourcing gap, requiring the player to load a real Chest by hand until a future machine can produce one.

**Open questions:** whether the Beaker or Glass Flask should ever get the same physical→fluid treatment applied to *themselves* in some other recipe (deliberately not applied within their own Metastasizer recipes, to preserve the "keep one real item" principle above). Whether a future machine ever produces Chests, closing the one remaining manual-sourcing gap.

#### Core — Recursive Crafting & Network Behavior

**Status:** Crafting-resolution rules, error handling, and network fluid/item lifecycle fully decided. Exact numeric limits (recursion depth cap, Core range) still deferred to Code — only the surrounding methodology is a design decision now.

**Why this is feasible, and where the real risk is:** the Core/Floor network graph and range checks are the same category of problem already solved by other mods' block-network systems (Applied Energistics 2's cable networks, etc.) — feasible as long as the graph is recomputed only on structural change (see Knitting below), never continuously. The recursive crafting resolver is the one genuinely heavy system here, comparable in complexity to AE2/Refined Storage's autocrafting — feasible, but only safe with the cycle-prevention rules below.

**Cycle prevention (layered):**
1. **Ancestor-chain cycle detection (primary):** while resolving a crafting tree, the Core tracks which items are currently being resolved in the active branch. If resolving an ingredient would require producing an item that's already an ancestor in that branch, the branch is rejected as circular immediately — catches a cycle at any depth, not just past an arbitrary limit.
2. **Hard depth cap (secondary):** a backup safety net for legitimately deep, non-circular chains. Exact number deferred to Code.
3. **Memoization per craft attempt:** once the Core determines whether it can/can't make N of an item within a single resolution pass, that result is cached instead of being re-derived every time the item recurs elsewhere in the tree.
4. **Author-time cycle validation:** since every OT-craftable recipe is first-party, a load-time/datagen-time validation pass walks the entire known recipe graph and flags a genuine circular dependency as a build-time error — caught during development, not discovered by a player at runtime. Rule 1 remains as runtime defense-in-depth (e.g. against a future datapack/mod interaction introducing a cycle).

**Error feedback:** any rejected or failed craft (circular dependency, depth cap exceeded, insufficient ingredients, drain lockout, etc.) surfaces as an **error sound + message in the Core's GUI** — the standard error-feedback pattern used throughout the OT's crafting system.

**Universal batch processing.** All OT-driven production runs one batch at a time — craft a batch → wait for it to finish → craft the next → repeat until the request's amount-remaining counter hits zero. This is **structure-wide**, applying to every machine and every recipe (no "batchable by the stack" fast path — that earlier provisional idea is retired). Beyond consistency, one-batch-at-a-time deliberately spreads a large job across many ticks rather than resolving it in a single burst — the same anti-lag-spike approach used by One Punch's staggered block-breaking, so universal batching is the cheaper option on the server, not just the tidier one. There is no separate "Vague recipe" handling: because batching loops until the counter is satisfied, nutrition-scaled variable yield is absorbed automatically — the loop simply runs another batch if the last one fell short. (An earlier "Vague recipes excluded from auto-resolution" rule, and its associated no-amount mode and upfront cost warning, are all scrapped as unnecessary under this model. If playtesting shows a warning is wanted, revisit then.)

**Multiple identical machines — parallel throughput.** Duplicate machines of the same type run **in parallel** to multiply throughput — the primary way the player scales OT speed (alongside fuel grade, which speeds each machine individually). Parallelism is *within* a request (the OT still resolves one player request at a time); the batch loop is per-machine rather than global. Distribution priority:
1. **Distinct recipes first** — if a request needs several *different* sub-recipes a machine type can make, spread them across the available duplicates (e.g. with two Masticators and a need for both Carbon Blend and Calcium Blend, one machine takes each) so all the different outputs progress at once.
2. **Then same-recipe parallelism** — once each distinct needed recipe is assigned, any remaining duplicate machines double up on a recipe that still has **multiple batches** left, running those batches in parallel.

Because machines must be within the control block's range to be used, the control tier caps how parallel the OT can get (a Brain's 5×5×5 cube fits few machines; a Core's larger range fits more) — the parallelism ceiling falls out of the range rule, no separate cap needed.

**Fuel across parallel machines:** the OT fuels active machines from the shared storage pool, **best grade first, cascading down the Main Line tier chain** (Superior → … → Crude) as better grades run out. If there's enough of the best available grade to fuel every machine in use, they all run on that same grade; if not, the best grade goes as far as it reaches and the remaining machines take the next grade down, and so on.

**Recipe priority & last-resort hand-crafts:** the OT prefers machine/native recipes. **Early Implant and Puddle Crafting recipes are last-resort fallbacks** — used only when a hand-craft recipe is the *only* available path to a needed output. When one is used, the OT does **not** simulate the physical puddle/injection; it materializes the result directly from the recipe's own numbers (consume inputs → produce output). Because those recipes are inherently a worse deal per result than the machine path, a single per-request warning surfaces before crafting begins, showing the **total count of last-resort invocations** across the resolved tree (e.g. "This job will shortcut 2 hand-crafted recipe(s). Proceed?") with a Continue/Cancel choice. The count is total invocations (not distinct recipes) because each invocation re-incurs that higher cost. This is a distinct, narrower warning from the scrapped vague-yield warning — it fires specifically for last-resort hand-crafts.

**Per-machine invocation rules — when the Core may auto-call each of the three special machines:**
- **Masticator:** **never** called during an **item** crafting request. **May** be called during a **fluid** crafting request, via the universal batch loop; the item ingredient each batch consumes is chosen by the structure-wide item source setting (below).
- **Metastasizer:** **never** called during a **fluid** crafting request (prevents its item-duplication mechanic from becoming a free fluid source). **May** be called during an **item** crafting request.
  - **Pattern-free operation (OT-exclusive):** the OT can run a Metastasizer recipe **without owning the pattern item** — it spawns a **transient pattern** into the Metastasizer for the duration of the job, which vanishes on completion. Only the fluid is consumed; no pattern is retained. Manual (non-OT) Metastasizer use still requires a real pattern, so this is a reward for building the automation hub. It's bounded (full fluid cost per item, no retained pattern → no free-matter loop, and the fluid-request exclusion above still holds) and it **simplifies the recursive resolver** — Metastasizer recipes become effectively *fluid-only* to the resolver, since the pattern no longer needs sourcing.
- **Drooling Cauldron:** each cauldron in the family **passively auto-produces one fluid on its own**, so by the time the OT needs that fluid it may already hold enough that no production request is triggered at all (ties into storage-first fulfillment). Its fluid is **pulled into storage at both the start and end** of a crafting process (harvesting whatever it passively accumulated). **Not** called to produce during a **food-item** request; otherwise called to produce when needed.

**Source settings (structure-wide) — two independent settings, one for items and one for fluids:** when a machine has a choice between multiple eligible input stacks, the OT picks by player-set options in the Core GUI. There are **two separate toggles** — one governing **item** ingredient selection, one governing **fluid** ingredient selection — each independently set to **most plentiful** (burn down overflow, protect scarce inputs) or **least plentiful** (clean out small/odd stacks, consolidate storage). Once a source is selected it is used continuously until the request completes or that source is exhausted, then the OT fails over to the next stack per the same rule. Both settings apply to the whole structure and **cannot be changed while a craft is running** — switching either mid-job requires canceling the current craft first.

**Manual ingredient insertion** is always available as a standing fallback, independent of any automation rule — the player can walk up and load any machine by hand regardless of what the Core's resolver would do automatically.

**Running out of ingredients mid-request:** triggers the standard error (sound + GUI message). The player can load more eligible ingredients into the network and press **Continue** to resume exactly where the counter left off, or **Cancel** to abandon the remainder — whatever was already produced/delivered is unaffected either way.

**Amount-remaining counter (general rule):** any OT job with a target quantity displays a running amount-remaining counter in the Core GUI.

**Storage-first fulfillment:** any request first draws from whatever matching item/fluid already exists in connected storage before triggering new production; only the shortfall is actually crafted.

**Full network drain-to-storage, after every OT-driven job:** once any job finishes, **every machine in the network** — not just the one that ran the job — is cleared of both fluids and items, pulled into connected storage. The sole exception is the **designated result storage** (the Skin Tank + Craw flanking the Core), which holds finished output for the player to collect and is never auto-drained. This keeps every machine on the network fully stateless between jobs, reinforcing the OT's existing modularity — swapping or moving a machine never stranded fluid or items inside it.
- **Manual pull buttons:** the Core GUI also offers buttons letting the player force a drain-to-storage pass on demand, independent of the automatic post-job trigger.
- **Lockout on drain failure:** if a drain can't complete because no storage capacity is available, the Core throws the standard error (sound + GUI message) and refuses to start **any further crafting at all** — not just the job that triggered it — until the problem is resolved.
- **Recovery sequence (strict order):** player adds sufficient storage → the structural change triggers **Knitting** (see below) → once Knitting confirms the new capacity, the stalled drain completes → only then does the Core unlock crafting again.

**Machine access — direct, not through capabilities (Dermicraft machines only):** the OT reads and writes attached Dermicraft machines' inventories and tanks **directly**, bypassing the NeoForge capability layer rather than going through `getCapability`. Rationale: capabilities enforce *polite* access — sided rules and input/output slot restrictions — that the OT's fill-completely / drain-completely model specifically needs to ignore, so direct access removes a fight rather than just adding a shortcut.
- **Contract-backed, not raw field access:** direct access goes through a defined API on `MachineBaseBlockEntity` (accessor methods / a small interface — e.g. input handler, output handler, reagent tank, fuel tank) so every machine exposes the same stable surface and a new machine just implements it. Avoids brittle per-machine casts.
- **Machines still expose capabilities.** This changes only how the *OT* accesses them — each machine keeps its normal capabilities for standalone use with hoppers, pipes, and other mods.
- **Foreign blocks still use capabilities.** The rule: `instanceof MachineBaseBlockEntity` → direct path; anything else on the floor network (vanilla chest, another mod's tank) → standard capability path. This is the "at least for Dermicraft machines" scope.
- **Resolves the fluid-output-face conflict** (see Floor Block) — direct access ignores block faces entirely, so the bottom-face-output-vs-Floor-block concern no longer applies.
- **Caveats:** `getBlockEntity` may return null (unloaded chunk / removed block); and since direct access skips slot validation, the OT honors input/output slot roles by its own contract rather than relying on the handler to reject a bad insert.

**Knitting** (renamed from "settling process"): the OT's network-graph recompute, triggered by any structural change (block placed/broken/moved) rather than running continuously every tick. Named for the mod's organic/body-horror theme — the network "knits" itself back together after a change, same metaphor family as the Sutured/Bloodletting status effects. This event-driven approach — recompute only on change, with a short debounce delay rather than a per-tick scan — is the main safeguard keeping the network-graph side of the OT cheap on the server.

### Filling Station (no name decided — descriptive placeholder)

**Status:** Concept stage only.

**What it is:** Fills fluid-handling items (Flasks, Syringes, Beakers, etc.) automatically. Can store a small number of filled items internally, and can move them out via automation.

**Open questions:** Exact internal storage capacity (how many items)? Does it draw from an internal fluid tank or a connected fluid source directly? Tier 1 hazardous-fluid restriction, like other machines? Final name.

---

## Farming automation concepts (early planning)

Three related ideas from early planning, sharing a personified-name pattern (Mr. Farmer, Mr. Shepard, Lumbering Jack) and the same status: preserved for future consideration, not committed as finished concepts, placement within the mod's Stage/Tier timeline undecided for all three.

### Shared mechanics (starting point, not locked)

- **Visual design:** shared body model with per-block textures to begin with; individual models later.
- **Range/placement:** starts small (3x3), grows under certain conditions.
- **Fuel-driven range (replaces fuel-optional/HP pattern):** unfueled, range is just the single block the machine is placed on/above. Range is keyed directly off a new `BiofuelProperties.tier` field (added as the last property in the record, alongside `speed`/`useRate`/`heal`; Crude Slurry is tier 1) rather than derived from the `getSpeed()` ratio — tier is a discrete, explicit value so fuels don't need near-identical speed values to land in the same bucket. Each tier adds one ring: tier 1 = 3x3, tier 2 = 5x5, tier 3 = 7x7, tier 4 = 9x9. Capped at 9x9 — a 5th (or higher) tier fuel is expected to exist but doesn't grant more range once capped. `ModFluidUtil.getTier()` / `FuelTank.getFuelTier()` expose the value.
- **Output handling:** internal buffer, player-accessible, drained by automation (e.g. Node/duct). If the buffer is full, items drop on the ground instead.
- **Stage/tier timing:** confirmed to release together as a set. **Open question:** which tier/stage.
- **Hazard interaction:** the machines won't consume or produce hazardous materials themselves, but nearby hazardous fluids may disrupt their operation. **Status: undecided, may be dropped entirely** — no hazard-tag/tolerance work exists for this family, and it's possible this mechanic gets cut rather than designed further. Left open intentionally.

### Mr. Farmer

**What it is:** Conceived as a machine that vaguely resembles a human head in pain. Placed in the middle of, or above, a garden plot. Automatically keeps the garden hydrated and lit, and harvests/replants crops — a full auto-farm device for crops.

**Mechanics (starting point):**
- **Hydration:** the machine itself hydrates tiles in range — no vanilla water-source requirement. In-fiction, it runs off the nutrition in its fuel slurry and waters farmland with the water left behind as byproduct.
- **Lighting:** actively lights the area in range (not just a spawn/growth check bypass).
- **Crop scope:** any crop tagged `c:crops` (NeoForge's common-tags convention) — leans on the existing cross-mod convention rather than a Dermicraft-specific tag, so any actively-maintained crop mod that supports common tags works with zero extra effort.
- **Growth acceleration (fuel heal rate):** repurposes the fuel `heal` stat — dead weight on Mr. Farmer since it dropped HP for range — as a growth-speed multiplier. Each immature crop the wave passes gets `round(getHeal() × GROWTH_ATTEMPTS_PER_HEAL)` extra vanilla random-ticks (base 2, Crude Slurry `getHeal()`≈1.0). Random-ticking respects light/moisture/fertility (all of which the machine already maxes), so it reads as faster natural growth, not a forced age jump, and works with any `CropBlock`. Folded into the existing per-step fuel burn (no surcharge). Thematically: the nourishing slurry that waters the field also feeds the crops. Rate is a tuning knob.
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
- A **text** range/reach readout (e.g. "Range: 5x5" / "Tier N") in the freed-up upper band where the HP bar would sit — no new textures needed for a first pass.
- Standard player inventory below.

**Range preview (feature):** closing the GUI triggers a ~30s particle visualization of the working area — it loops the wave sweep (rings for vertical facing, rows for horizontal) so it communicates both the footprint shape and the wave order, then tapers particle density over the last ~5s to fade out. Preview-only (does NOT play during live farming), restarts its timer on each GUI close, shows whatever the *current* fuel-tier range resolves to (so unfueled shows the minimal 1-block reach), and does not consume fuel. Triggered server-side from `MrFarmerMenu.removed()`; visible to all nearby players by design. Grew out of the debug particles used to verify the range geometry — repurposed rather than removed. **The preview marks any ground surface, not just farmland** (dirt, grass, path, stone, farmland — anything with a sturdy up-face), so the player can see the full reachable footprint and tell which cells still need tilling; the actual farming logic will use a strict farmland test instead. Target resolution is parameterized by a `GroundTest` predicate to support both.

**Implementation status:** Block, block entity, menu, and screen are built (`MrFarmerBlock`/`MrFarmerBlockEntity`/`MrFarmerMenu`/`MrFarmerScreen`). Block entity has a `FuelTank` (fuel-container slot with bidirectional bucket transfer, same pattern as Masticator) and a single `ItemStackHandler(10)` (slot 0 = fuel passthrough, slots 1-9 = buffer), `getRange()` derives the tier→ring value live from the fuel tank. GUI renders the fuel gauge bare (no decorative frame yet — reuses only the cropped 18x18 slot-square from `tank_and_slot.png` as a generic slot backdrop under all 10 slots) plus the text range readout; a proper fuel-tank-specific frame texture is still needed from the art side. GUI opens **directly on right-click** (unlike other machines, which require the Outerface) — `useWithoutItem` opens the menu; holding a fluid container instead fills/drains the fuel tank. Still carries the `HAS_SCREEN` tag so the Outerface also works. The block has an `ACTIVE` (`lit`) blockstate that swaps the texture `mr_farmer` → `mr_farmer_on` while fueled (12 blockstate variants: 6 facings × on/off), driven from `setActive()` in the fuel-gated tick (only re-sets the block on transition, preserving the BE). Right-clicking with any fluid-container item (bucket/flask/syringe) fills the tank through the block's registered `FluidHandler` capability (biofuel filter + fluid-match enforce "biofuel, matching if non-empty").

**Automation:** both capabilities are registered for `MR_FARMER_BE` on all six faces (`ModBusEvents`). Fluid = the fuel tank directly (biofuel-filtered fill, drain) — this is the only automated fueling path. Items = a dedicated automation wrapper (`getAutomationItemHandler`, separate from the GUI's full-access `getItemHandler`): buffer slots 1-9 are **extract-only** (harvest piped out, nothing pushed in); the fuel slot (0) is **player-only** — no automated insert or extract, so pipes can't shuttle fuel containers in/out of it.

**Farming engine — sliced build (functional order):** (1) fuel-gated farming wave skeleton ✅ — `tickFarming()` runs the strict-farmland wave alongside the free preview, advancing only when fueled and burning `max(1, useRate)` mB per worked step; (2) hydrate + light ✅ — each worked cell forces farmland `MOISTURE` to 7 (no water source needed). Lighting uses a **minimal covering set** of invisible `Blocks.LIGHT` (level 15) placed one block above chosen crop cells: a greedy pass over the field only drops a new light where a cell isn't already lit ≥9 (reach = 6 blocks) by an existing one, so 3×3/5×5 use 1 light and larger fields only a handful — no crop cells are blocked (lights sit above). Lights are NBT-tracked in `placedLights` and extinguished on unfuel / field-shrink (reconcile) / block removal, so nothing is left permanently lit; (3) harvest ✅ — mature crops (`CropBlock.isMaxAge`, covers vanilla + CropBlock-extending modded crops) are broken and their natural drops routed into buffer slots 1-9, overflow spilled on the ground (buffer-full rule); (4) replant ✅ — harvest and replant happen in the same wave pass, so the machine replants the **same crop** it just harvested with no persistent per-cell tracking. Seeds route into a hidden **seed reserve** (`Map<Block,Integer>`, crop block → banked seed count), products go to the buffer. Reserve is capped per crop type at `ceil(range²×1.1)` (a full replant + 10% bumper); seeds over the cap flow to the public buffer, or stay **held** in the reserve if the buffer is full (seeds never hit the ground except on block break). Replant takes the matching crop's seed; if exhausted it falls back to a **random** reserved seed; genuinely-empty tilled cells also get a random reserved seed to keep the field full. Reserve is NBT-persisted and dropped on block break. Fuel drain rate and light coverage/perf are first-pass and open to tuning. Crop-detection currently only recognizes `CropBlock` subclasses (nether wart / modded stems / non-CropBlock crops are a known gap), which the replant inherits.

**Open questions:** Exact range step formula and cap (see Shared mechanics — tier→ring is decided: tier 1=3x3 … tier 4=9x9, capped). Does hazardous-fluid disruption (see Shared mechanics) apply here in a specific way, e.g. contaminating hydration? Whether the text range readout later becomes a visual grid icon. Whether/when a dedicated fuel-gauge frame texture gets made.

### Mr. Shepard

**What it is:** Working name for a machine to automate animal farming — the livestock counterpart to Mr. Farmer's crop automation.

**Mechanics (starting point):**
- **Scope:** collects dropped items on the ground in range, shears sheep, and encourages breeding/growth. Wave behavior (ring-by-ring, closest to farthest) likely carries over from Mr. Farmer.
- **Culling:** player-configured population cap. Uses the same accessible-buffer UI Mr. Farmer and this family already have (player can hand-remove collected items), extended with cap config.
- **Species tracking:** UI has a one-species/many-species toggle rather than per-species switches — too many herdable mobs to give each its own control. Exact behavior of "many species" mode (shared cap vs. per-species cap once toggled on) not yet worked out.

**Open questions:** Visual design — personified like Mr. Farmer, or different? Exact cull logic (which animal gets culled when over cap — oldest, random, etc.)? In "many species" mode, is the population cap shared across all species in range or tracked per species? Does feeding factor in (does it need to supply food items to encourage breeding, or does breeding happen automatically once population is under cap)?

### Lumbering Jack

**What it is:** Working name for a machine to automate tree farming (planting/harvesting trees for wood).

**Mechanics (starting point):**
- **Scope:** auto-plants saplings and auto-harvests grown trees in range. Wave behavior (ring-by-ring, closest to farthest) applies to both planting and harvesting passes.
- **Harvest trigger:** listens for a tree-growth event rather than polling/timing growth itself. Likely candidate is NeoForge's `BlockGrowFeatureEvent` (successor to the old `SaplingGrowTreeEvent`), which fires when a sapling/tree feature grows — not yet confirmed against this project's NeoForge version, needs verification at implementation time.

**Open questions:** Visual design — personified like Mr. Farmer, or different? Does it clear leaves along with logs, or leave them? Does it handle multiple tree/wood types simultaneously (mirrors Mr. Shepard's multi-species question), or is that not a concern since saplings are more uniform?

---

## Template for new entries

```
### [Machine Name]

**Status:**

**What it is:**

**Recipe/logic:**

**Open questions:**
```
