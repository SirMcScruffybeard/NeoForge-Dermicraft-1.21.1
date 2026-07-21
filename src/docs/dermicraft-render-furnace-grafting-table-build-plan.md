# Render Furnace & Grafting Table — Build Plan

Implementation plan for the mod's two early-game convenience machines. Design authority: `dermicraft-machine-notes.md` → "Render Furnace and Grafting Table" entry. This doc is the build order — when the two disagree, the machine notes win and this doc should be corrected.

**Both machines run on VANILLA's own recipe types** (`RecipeType.SMELTING`, `RecipeType.CRAFTING`) — no new Dermicraft recipe type, no datagen roster. This is the one place in the mod where that's deliberate (see the machine notes for why).

---

## Render Furnace (build first — simpler, same shape as the Masticator)

### Block + BlockEntity
- `RenderFurnaceBlock` — copy `MasticatorBlock`'s shape (facing, tier, ticker). New face texture needed eventually; placeholder (reuse `metastasizer_front`/`skin_tank_end` or a furnace-adjacent existing texture) until art exists.
- `RenderFurnaceBlockEntity extends AbstractFueledMachineBlockEntity<SmeltingRecipe>`:
  - Slots: 0 = fuel container, 1 = input, 2 = output. No reagent tank.
  - `getRecipeType()` → `RecipeType.SMELTING` (vanilla, not a Dermicraft `ModRecipes` entry).
  - Recipe resolution: `level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(inputStack), level)` — vanilla's own input wrapper, no custom `RecipeInput` record needed.
  - `maxProgress` on resolve = `recipe.value().getCookingTime()` (vanilla field, 200 ticks default) — no invented duration, unlike the Grafting Table.
  - `onCraftComplete()`: consume 1 from input slot, insert `recipe.assemble(input, registries)` into output. Mirrors Masticator's `onCraftComplete` shape but against vanilla's `SmeltingRecipe`.
  - Fuel/HP: standard `AbstractFueledMachineBlockEntity` template, unmodified — any biofuel.
  - Face routing: top = fuel, sides = input (+ bidirectional bucket transfer if a fluid container is inserted, though the input slot is normally solid items), bottom = output (extract-only). Copy the Masticator's `getItemHandler(Direction)` pattern.
  - Slot-limit bug from the mod-wide fix: fuel slot capped to 1 unconditionally; input/output slots keep default stack limits (no fluid-handler auto-fill concern here since neither slot participates in bidirectional tank transfer the way fuel does).
- Menu/Screen: copy Masticator's layout (fuel gauge + input/output slots + progress arrow + HP bar), no reagent gauge.

### Datagen
- **No recipe datagen at all** — that's the entire point.
- Implant recipe only: `render_furnace_implant` — `1× Furnace`, sutured, injected with 100 mB Primitive Catalyst. No flesh ingredients.
- Blockstate/model, item model, `HAS_SCREEN` + `COLLECTIBLE` tags, lang entries, creative tab entry — same checklist as every other machine.

---

## Grafting Table — v1 (BUILT, superseded by the pattern/ghost rework below)

v1 shipped as a literal 3×3 vanilla `CraftingInput` clone: 9 real item slots, `getRemainingItems`-aware consumption, invented duration (`40 + 20×ingredients`), flat 9-slot automation handler, `MachineTier.NO_HEALTH` hard fuel gate (matching the Render Furnace). Playtested and mostly confirmed working, but the crafting *interaction model* itself needs a rework (see below) before this is considered done — automation's "insert into first available slot" behavior in particular is expected to change once the ghost/pattern model lands.

## Grafting Table — pattern/ghost rework (BUILT, awaiting playtest)

**Why:** a literal always-consuming 3×3 grid means the player re-fills all 9 slots by hand every time stock runs low, with no memory of what the recipe even was. The rework makes each slot remember its own recipe **permanently** (a "ghost"/pattern), decoupled from the real stock that actually gets consumed — closer in spirit to the Metastasizer's non-consumed-pattern idea than to a literal crafting table.

**Per-slot data model (9 grid slots) — TWO separate things per slot, not one `ItemStack`:**
- **Ghost/pattern**: an item-type marker (no count). Sourced from whatever item was first shown to an empty slot. Persists indefinitely, survives the real stock running out, is NEVER returned to the player once set.
- **Real stock**: an actual `ItemStack` count sitting "behind" the ghost, freely extractable, consumed 1 per craft. Can be 0 while the ghost remains.

**Two-stage recipe resolution — NOT the same as v1's single resolve:**
1. **Pattern resolution.** Build a synthetic `CraftingInput` from just the ghost *types* (ignoring real counts, empty un-imprinted slots stay empty) and resolve against `RecipeType.CRAFTING`. A match becomes the machine's locked **target recipe** — cache it (`patternRecipe`) separate from the base class's `activeRecipe`.
2. **Real-stock resolution.** Separately check whether each slot's *real stock* satisfies the target recipe's `Ingredient` for that position, in sufficient quantity. Only when true does `activeRecipe` get set and actual progress/fuel/consumption proceed — pattern-matching alone never crafts anything.

**Flexible matching once a recipe is locked in (confirmed, both player and automation):** a slot's *acceptance* is governed by the target recipe's own `Ingredient` for that position, not the literal first-imprinted item — e.g. an Oak Planks ghost that resolves into a "any planks" recipe will accept Spruce/Birch/etc. as real stock afterward, from both manual deposits and automation. The ghost's *displayed icon* stays the originally-imprinted item regardless — flexibility affects acceptance, not the ghost's identity/appearance.

**Click gesture (custom — vanilla `Slot`/`SlotItemHandler` cannot express this, needs a custom `Slot` subclass and/or `AbstractContainerMenu.clicked()` override):**
- Empty slot (no ghost), cursor holds a stack, **left-click** → imprints the ghost from the cursor stack's item. Cursor is **untouched** — nothing is taken, nothing deposited. This is the *only* way a new ghost gets created; automation never creates one.
- Same slot, now ghosted, cursor holds a matching (or Ingredient-satisfying, once a recipe is locked) stack, **left-click again** → normal deposit into real stock.
- **Right-click / shift-click**: normal vanilla-ish behavior *if the slot already has a ghost and the item matches*; **no-op** if the slot has no ghost yet (only plain left-click can establish one).
- Real stock can always be freely extracted (any click type) while the ghost remains in place.
- **Empty-hand click on a ghost with 0 real stock** → clears the ghost, resetting the slot to truly empty. A ghost can only be cleared once its backing stock is fully depleted — never while stock > 0.

**Rendering rules:**
- Grid slot: while real stock > 0, render the **actual item stack** (normal count/icon) exactly like any other slot. Once stock hits 0, render the **ghost** (translucent/overlay icon of the originally-imprinted item) instead. The ghost "returns" visually the moment stock depletes.
- Output slot: once `patternRecipe` resolves, show a **ghost preview of its result** — but only while the real output slot is empty (never layered under/over an actual crafted result sitting there). Disappears if the pattern is cleared or stops resolving.

**Automation (revised from v1's flat "any available slot"):** only ever tops up slots that **already have a ghost** — never creates new ones. Once a target recipe is locked in, incoming items round-robin across every slot whose *Ingredient* accepts that item (flexible matching, same as manual deposit), not just an exact-ghost-type match.

### Implementation surface this touches (rough shape, refine at build time)
- New BE fields: `ItemStack[] grid_ghosts` (size 9, empty = no imprint) alongside the existing real-stock `ItemStackHandler` grid slots — NBT-persisted.
- `patternRecipe` (ghost-based match, drives the output ghost + per-slot flexible Ingredient) kept separate from `activeRecipe` (real-stock-satisfied match, drives actual progress/consumption via the existing `AbstractFueledMachineBlockEntity` engine).
- Custom `Slot` subclass (or menu-level `clicked()` override) implementing the click gesture above — this is the one piece with no existing precedent anywhere else in the mod.
- Screen: ghost-icon rendering (grid + output), likely a translucent/alpha-reduced blit of the item's normal icon rather than new art.
- Automation handler rewrite: round-robin-across-flexible-matching-ghosted-slots instead of v1's plain 9-slot flat handler.

### Datagen
- Unchanged from v1 — no recipe datagen, implant recipe (`1× Crafting Table`, sutured, 100 mB Primitive Catalyst, no flesh) already built and correct.

---

## Build order rationale

Render Furnace first: it's a straight copy of an existing machine's shape (1 item in, 1 item out, vanilla recipe type swapped in) with no new UI pattern — a fast, low-risk win that also validates the "vanilla recipe type instead of a Dermicraft one" plumbing before tackling the harder machine. Grafting Table second, once that plumbing is proven, since it adds the genuinely new 9-slot grid + remainder-aware consumption + invented-duration pieces on top.

## Known open questions to resolve during/after building
- Grafting Table's exact duration constants (BASE_TICKS/PER_INGREDIENT_TICKS above are placeholders).
- Whether `CraftingInput`'s exact factory API matches what's assumed here (1.21.1-specific — verify against compiler feedback, not memory).
- Face texture art for both blocks (placeholder reuse until dedicated textures exist, same pattern as the Mutator).
