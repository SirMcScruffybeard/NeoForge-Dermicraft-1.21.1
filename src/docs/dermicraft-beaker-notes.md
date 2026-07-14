# Dermicraft Beaker Notes

Running log of decided design choices for the Beaker (Tools sub-family) and the reasoning behind them. For the fuller implementation-ready spec (stack size, naming exception, light emission, break-vs-pickup rules), see `dermicraft-beaker-block-item-outline.md` — that doc predates full automation support being added here (see its note at the top) but is otherwise still current and isn't restated in full below.

---

## Beaker — overview

**Status:** Operational. Re-implemented in the current build (`BeakerItem.java`, `BeakerBlock.java`, `BeakerBlockEntity.java`), including item ↔ block fluid-preserving transitions and a dedicated renderer.

**What it is:** A hand-held Tool (see `dermicraft-project-primer.md`) that holds a fluid, with a feature unique among the fluid-holding Tools so far: it can also be **placed as a block**, functioning in both item and block form rather than item-only like Syringe/Flask.

**Recipe:** Three Glass Blocks in a crafting table — deliberately cheap/simple.

**Capacity:** `1000 mB` — one full vanilla bucket. Notably larger than Flask (250 mB) and Syringe (100 mB); the bulk-storage option of the three fluid-holding Tools.

**Fluid restrictions:** None confirmed so far — unlike Flask (whose variants are tied to specific named fluids), the Beaker can hold any fluid as of now.

**Block-form behavior:**
- Functions like other tank blocks while placed — fluid can be added to or removed from it the same way.
- Holds its fluid across the item ↔ block transition (placing a full Beaker keeps its contents).
- If the placed block is broken while holding at or above its **spill threshold** (950 mB), and the held fluid has a valid in-world fluid block, the fluid spills into the world as that block. If that condition isn't met (below the spill threshold, or no valid fluid block for what it's holding), the fluid is simply **lost** — not preserved, not returned to the item, just gone.
- **Automation confirmed unrestricted, verified against code:** `BeakerBlockEntity.getTank(Direction face)` ignores the `face` parameter entirely and always returns the same single tank, and the fluid-handler capability is registered block-wide (`Capabilities.FluidHandler.BLOCK` via `BeakerBlockEntity::getTank` in `ModBusEvents`) with no per-side gating anywhere in `BeakerBlock`. So hoppers/pipes/etc. can access the tank from **any face** — same as most other tank blocks in the mod. This supersedes the earlier outline doc's "player-interaction-only, no automation" call (see `dermicraft-beaker-block-item-outline.md`), which was never carried into the actual implementation.

**Open questions (resolved):**
- **Breaking vs. picking up are two separate mechanics with different outcomes.** The block is registered with `.noLootTable()`, so ordinary breaking (pickaxe, explosion, anything that isn't the dedicated pickup interaction) **never returns an item** — full, empty, or in between. It either spills (per the spill-threshold rule above) or just destroys the fluid.
- **The only way to get the Beaker back as an item is the dedicated pickup interaction** — right-clicking the block with **Forceps**, or right-clicking it empty-handed. That path drains the tank and transfers the exact held fluid into the returned item's fluid data, so the item always comes back with its fluid intact (never forced empty) when fluid was present.
- Given the above, "top-only automation" doesn't apply in either direction — access is unrestricted on all faces.
- **Legacy "no fluid type limit" still holds.** The current tank implementation has no fluid-type validation, so any of the newer Catalysts/Slurries/Crafting fluids (which didn't exist when this was designed pre-rebuild) work in it exactly the same as any other fluid — no exception needed.

