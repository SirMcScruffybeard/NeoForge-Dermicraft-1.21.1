# Dermicraft Flask Notes

Running log of decided design choices for Flasks (Tools sub-family) and the reasoning behind them. Add a new entry per Flask variant as decisions get made — the "why" matters as much as the "what" so later additions stay consistent with earlier logic.

---

## Flask — overview

**Status:** Core identity and capacity decided; secondary uses and fluid-variant roster still open — the reason this doc exists.

**What it is:** A hand-held Tool, sub-family alongside Syringes (see `dermicraft-project-primer.md`). A Flask holds a fluid, and the specific fluid it's filled with defines each variant.

**Naming convention:** `Flask of [Fluid]` (e.g. Flask of Calcium Blend, Flask of Crude Slurry). **Glass Flask** is the one named exception — the empty/unfilled base state, not "Flask of Glass."

**Capacity:** `250 mB` (millibuckets) — exactly one quarter of a vanilla bucket (1000 mB).

**Primary use:** Fluid transport — the straightforward bucket-like role, at smaller scale than a vanilla bucket.

**Secondary uses:** Multiple confirmed and growing: Glass Flask doubles as an air source, Flask of Calcium Blend acts like bone meal, Flask of Crude Slurry converts dirt/plants crops on farmland, Flask of Lava triggers a drink-based fire mechanic, Flask of Protein Blend is planned to tame wolves (see variant entries below). Pattern so far: each variant's use ties to the real-world/in-game association of the fluid it holds, not picked arbitrarily.

**On drunk vs. applied:** Some variants are drunk by the player, others are applied to a block/surface. No need to split these into separate sections — each variant entry just states its **Mode of use** directly.

**Crafted via:** shaped crafting-table recipe — 4× `c:glass_blocks/cheap`-tagged block in a chalice pattern (`" G "` / `" G "` / `"G G"`) → 4 Glass Flasks. Same vanilla-crafting exception precedent as Beaker's 3-glass-block recipe.

**Filled via:** Glass Flask registers a standard NeoForge `IFluidHandler.ITEM` capability, fixed/"rigid" at 250 mB (`RigidFluidDataFluidHandler`) — the same generic interface any fluid-handler item uses, so it fills/empties through normal fluid-handler interactions (e.g. against a machine tank or another fluid handler), not a bespoke bucket-style world raytrace like Beaker's manual pickup.

**Open questions:**
- Which fluids actually get a Flask variant beyond the current list (all Catalysts/Slurries/Crafting fluids? a subset?)
- How does it relate to the Syringe (100 mB, purpose-built for Machine/block crafting) beyond capacity — is Flask meant as the "general purpose" counterpart to Syringe's "specific purpose" role?

---

## Flask variants

**Current roster (all variants worked out so far):** Glass Flask, Flask of Calcium Blend, Flask of Crude Slurry, Flask of Lava, Flask of Protein Blend (planned). Logged in the order they were decided.

### Glass Flask

**Status:** First defined variant — the Flask's base/unfilled state.

**What it is:** What a Flask is before any fluid fills it — equivalent to an empty vanilla bucket as a starting point for the fluid-defines-the-variant logic established above.

**Mode of use:** Used while holding breath underwater (not clearly "drunk" in the same sense as other variants, since it starts empty — flagged for confirmation rather than assumed).

**Use(s):**
- Extends underwater air, functioning like a small emergency air supply — restores ~90 air ticks (code comment: "about 3 bubbles") when used, capped at the player's max air supply.
- Using it this way fills the Flask with water as part of the process — Glass Flask becomes a Flask of Water as a side effect of drawing air from it. This doubles as one of the "other uses beyond transport" the Flask was flagged as having.

**Open questions (resolved):** Restores ~90 air ticks per use (~3 bubbles' worth). **Not** consumed — the same item transitions in place from Glass Flask to Flask of Water (250 mB) and remains usable. The resulting Flask of Water has no special property — functionally identical to a Flask of Water obtained any other way.

### Flask of Calcium Blend

**Status:** Use defined.

**What it is:** Flask filled with Calcium Blend (see Crafting fluids in `dermicraft-project-primer.md`).

**Mode of use:** Applied to a block.

**Use(s):** Functions like bone meal — forces/speeds plant growth when applied (implemented via vanilla's own `BoneMealItem.applyBonemeal`).

**Open questions (resolved):** Consumed **only on a successful** application — if `applyBonemeal` has nothing to act on, the flask/fluid is kept and nothing happens (no waste on a whiffed use). Scope is identical to vanilla bone meal, not broader — it delegates straight to the vanilla method rather than a custom effect list.

### Flask of Crude Slurry

**Status:** Use defined.

**What it is:** Flask filled with Crude Slurry (the mod's baseline fuel; also Primitive Catalyst's base — see `dermicraft-slurry-notes.md`).

**Mode of use:** Applied to a block.

**Use(s):**
- Converts dirt into a grass block or a mycelium block.
- Applied to farmland, plants a random crop.

**Open questions (resolved):** Grass vs. mycelium is a flat **50/50 random roll** — no environmental condition (proximity to existing mycelium, etc.) is checked. The "random crop" pool is drawn straight from vanilla's `CROPS` block tag, filtered to whichever candidate can actually survive on the target block. Consumed **only on a successful** application in both cases (dirt must be dirt-tagged; farmland must have the space above empty) — a whiffed attempt keeps the flask.

### Flask of Lava

**Status:** Operational. Re-implemented in the current build (`GlassFlaskItem.java`'s `Lava` inner class, `SpicyRegretEffect.java`, `spicy_regret` damage type) — no longer just a carryover placeholder.

**What it is:** Flask filled with Lava.

**Mode of use:** Drunk.

**Use(s):** Drinking it inflicts a custom status effect called **Spicy Regret**:
- The player catches fire (force-relit every tick via `setSharedFlagOnFire(true)`, which is also what defeats water/rain/normal extinguishing — the effect keeps re-igniting faster than vanilla can put it out).
- Deals custom damage (`1 + amplifier` per effect tick) through a dedicated `spicy_regret` damage type — **bypasses armor and shield** (`bypasses_armor: true`, `bypasses_shield: true`), and is deliberately *not* tagged as fire damage (`is_fire: false`), which is what lets it bypass Fire Resistance too — Fire Resistance only blocks damage sources tagged as fire.
- **Also grants +200% movement speed for the full duration** (`ADD_MULTIPLIED_TOTAL` attribute modifier, factor 2.0) — a "panic sprint" read: the player moves drastically faster while burning, not just taking damage. This is a real, coded part of the effect, not just flavor.
- A looping lava-ambient sound plays every second (20 ticks) while active, reinforcing "you are basically on fire like lava" even though the damage type isn't flagged as fire.
- The fire/effect can't be put out the normal way — only two things end it: drinking a **Flask of Water**, or surviving until the effect naturally expires on its own after **20 seconds** (confirmed: `DURATION_IN_SECONDS = 20`).
- If the player drinks a Flask of Water to cure it, an **Obsidian block item** spawns at their feet — a direct callback to the vanilla lava+water → obsidian interaction, reframed as happening *to the player* internally.
- **Death message:** `"%1$s succumbed to spicy regret"` (confirmed, lowercase in the actual translation key).

**Open questions (resolved):** Duration is 20 seconds. Damage is custom (not vanilla fire damage) and explicitly routed around Fire Resistance by not being tagged as fire. No separate on-drink damage — only the recurring effect-tick damage applies. Naming convention needed no changes ("Flask of Lava" already fit).

### Flask of Protein Blend

**Status:** Planned — use decided, mechanic not yet detailed.

**What it is:** Flask filled with Protein Blend (see Crafting fluids in `dermicraft-project-primer.md`; corresponds to Primitive Catalyst's raw-meat component).

**Mode of use:** Applied to a mob (wolf).

**Use(s):** Intended to tame wolves — Protein Blend standing in for vanilla raw meat's taming role, consistent with the Blend's meat-correspondence.

**Open questions:** Single use per tame, or chance-based like vanilla taming? Does it work on other tameable mobs too, or wolves specifically? Crafted/sourced before this can be tested — depends on the Crafting-fluid machine being defined.

---

## Template for new entries (per Flask variant, once fluids are assigned)

```
### Flask of [Fluid Name]

**Status:**

**What it is:**

**Mode of use:** (drunk / applied to block / applied to mob / etc.)

**Use(s):**

**Open questions:**
```
