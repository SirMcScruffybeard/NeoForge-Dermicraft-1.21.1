# Mutator — Build Plan

Implementation plan for the Mutator machine. Design authority: `dermicraft-machine-notes.md` → Mutator entry (concept, recipe roster, build spec). This doc is the build order — when the two disagree, the machine notes win and this doc should be corrected.

**Template machine: the Metastasizer.** Block/BE/menu/screen/datagen are all copied from it and modified. Key structural difference: the Mutator **consumes** its input item (Metastasizer's pattern is non-consumed), and it has a second operating mode (fill).

---

## Phase 1 — Recipe type

New package `recipe/mutating/`:

- **`MutatingRecipe`** (record) + nested `Serializer`, copied from `MetastasizingRecipe`:
  - Fields: `Ingredient ingredient, Fluid fluid, int fluidAmount, ItemStack result, int ticks` — identical shape to Metastasizing; only the semantics change (ingredient is consumed).
  - Reuses **`OneFluidOneItemRecipeInput`** (already exists — the Masticator/Metastasizer input record).
- **`ModRecipes`**: register `MUTATING_TYPE` + `MUTATING_SERIALIZER` (`dermicraft:mutating`), following the existing registration block pattern.

## Phase 2 — Block + BlockEntity

- **`MutatorBlock`** in `block/custom/` — copy `MetastasizerBlock` (extends `ModBaseEntityBlock`). Register in `ModBlocks` (auto-registers the BlockItem). Carry the same block tags the Metastasizer has (`HAS_SCREEN`, COLLECTIBLE, etc. — check its tag datagen and mirror).
- **`MutatorBlockEntity`** in `block/entity/custom/` — copy `MetastasizerBlockEntity`, extends `AbstractFueledMachineBlockEntity<MutatingRecipe>`. Register in `ModBlockEntities`.

Changes from the Metastasizer copy:

1. **Slots (4, same layout):** 0 = fuel container, 1 = reagent container, 2 = `INPUT_SLOT`, 3 = `OUTPUT_SLOT`.
   - `INPUT_SLOT` is a normal stackable slot (drop the pattern slot's `getSlotLimit == 1` override) — the machine processes one item per cycle (Masticator convention) but the slot may hold a stack.
2. **Tanks:** `FUEL_TANK` (from base) + `VulnerableTank` reagent tank, 5000 mB each — unchanged from the copy. `VulnerableTank` = the Tier 1 hazard guard; the Metaphysical Mind Rule needs no code here yet (Metaphysical-tagged fluids are all also Extreme Heat, which `VulnerableTank` already rejects — the Mind Rule only matters code-wise when smart structures and the duct filter carve-out get built).
3. **Mode state:** `enum Mode { MUTATE, FILL }` field, default `MUTATE`, NBT-persisted (`"mode"`), exposed to the menu via `ContainerData` (int ordinal) so the toggle survives GUI reopen and syncs client-side.
4. **`onCraftComplete()` (mutate mode):** consume the fluid **and shrink the input stack by 1** (the one behavioral difference from the Metastasizer's non-consumed pattern), insert result into `OUTPUT_SLOT`.
5. **Fill mode (new tick path, active only when `mode == FILL`):**
   - Bypasses the recipe system entirely — generic capability operation.
   - Target = item in `INPUT_SLOT` exposing a fluid-handler capability with room for the reagent tank's fluid.
   - **Rate: 250 mB per cycle** (a cycle = the same progress/maxProgress machinery; pick a short fixed maxProgress, e.g. 20t, tune later).
   - **Rigid-container handling:** if the target only accepts an atomic fill (simulate-fill of 250 returns 0 accepted but a full-volume fill would succeed), accumulate 250 mB/cycle into a hidden `fillBuffer` (int, NBT-persisted, drained from the reagent tank as it accumulates) until it reaches the target's required volume, then execute one atomic fill and clear the buffer. Flexible containers (accept partial fills) just get 250 mB/cycle directly.
   - On completion (target full or can't accept more): move the container to `OUTPUT_SLOT` if empty, else leave in place until there's room.
   - **HP gating:** no fill while HP == 0 or recovering from 0 (the base class's starved/healing state). While recovering from partial HP, fill runs at the machine standard's **0.1× recovery multiplier** — **25 mB per cycle** — accumulating through the internal buffer as necessary (same buffer path as the standard rate, just slower). No new rule: this is the existing crafting recovery-speed convention applied to fill.
   - **If the mode is toggled with a nonzero `fillBuffer`:** return the buffered fluid to the reagent tank (or block the toggle until buffer clears — pick whichever is simpler; returning is friendlier).
6. **Recipe resolution:** copy `resolveRecipe()`/`getRecipeOptional()` using `MUTATING_TYPE` and `INPUT_SLOT`; only resolve in MUTATE mode.
7. **Face routing / channels / capabilities:** keep the Metastasizer's exact scheme (UP = fuel, DOWN = output, sides = reagent + input item) including `describeFace`, `getChannels` (Gate channels: fuel, reagent, input, output — fuel first per starvation-first convention), and the `ModBusEvents` capability registrations (copy the Metastasizer's lines).
8. **`drops()` / `drainOutputs()`:** unchanged from copy.

## Phase 3 — Menu + Screen

New package `screen/custom/mutator/`, copied from `metastasizer/`:

- **`MutatorMenu`** — copy, plus the mode `ContainerData` slot and a `clickMenuButton` id that toggles the BE's mode server-side. Register in `ModMenuTypes`.
- **`MutatorScreen`** — copy of the Metastasizer screen (same composited `screen_parts` layout), plus:
  - **Mode toggle button directly above the input item slot** — reuse the existing item-button and fluid-button textures, rendering whichever matches the current mode and swapping on click (sends the menu button id).
  - **Tank meaning must read differently per mode** (reagent = consumed vs. cargo = packaged): minimum viable version is the tank tooltip text switching with the mode; fancier treatment is a flagged open question, don't block on it.
- Register the screen in `DermicraftClient.registerScreens`.

## Phase 4 — Datagen

1. **Implant recipe** (`mutator_implant`): copy the Metastasizer's implant builder, swap the defining item — **1 Chest + 2 Dense Muscle + 4 Nerve Cluster + 1 Eye**, sutured, injected with **100 mB Primitive Catalyst**.
2. **`RecipeBuilders`**: add a `mutating(...)` builder (ingredient, fluid, amount, result, ticks) mirroring the metastasizing builder.
3. **Launch roster** in `ModRecipeProvider` — Tier 1 only (all Stage 2 recipes wait for the Tier 2 evolution). Values are **provisional** (functional-not-final, Effluentcer precedent; anchors documented in machine notes). Times follow the Metastasizer tier convention (light 50t / aggregate 120t / solid 200t) unless noted:

   | Recipe | Reagent | mB (prov.) | Ticks |
   |---|---|---|---|
   | Bladder → Fuel Bladder | Cuprous Blend | 750 | 200 |
   | Bladder → Feeder Bladder | Protein Blend | 750 | 200 |
   | Eye → Eye Tumor | Protein Blend | 1000 | 200 |
   | Nerve Cluster → Nerve Tumor | Protein Blend | 1000 | 200 |
   | Dense Muscle → Muscle Tumor | Protein Blend | 1000 | 200 |
   | Cobblestone → Mossy Cobblestone (+ stairs/wall) | Crude Slurry | 100 | 120 |
   | Cobblestone Slab → Mossy Slab | Crude Slurry | 50 | 50 |
   | Stone Bricks → Mossy Stone Bricks (+ stairs/wall) | Crude Slurry | 100 | 120 |
   | Stone Brick Slab → Mossy Slab | Crude Slurry | 50 | 50 |
   | String → Vines | Crude Slurry | 400 | 120 |
   | Sugar Cane → Bamboo | Crude Slurry | 500 | 120 |
   | Bamboo → Cactus | Crude Slurry | 500 | 120 |
   | Dye → Stained Glass (×16) | Silica Blend | 1000 | 200 |
   | Dye → Stained Glass Pane (×16) | Silica Blend | 500 | 120 |
   | Dye → Colored Terracotta (×16) | Clay Blend | 1000 | 200 |
   | Dye → Glazed Terracotta (×16) | Clay Blend | 1250 | 300 |
   | String → Cobweb | Protein Blend | 750 | 120 |
   | Rotten Flesh → Leather | Calcium Blend | 500 | 200 |
   | Apple → Golden Apple | Aurous Blend | 6000 | 200 |
   | Carrot → Golden Carrot | Aurous Blend | 660 | 120 |
   | Melon Slice → Glistering Melon | Aurous Blend | 660 | 120 |
   | Blackstone → Gilded Blackstone | Aurous Blend | 550 | 200 |
   | Eye → Spider Eye | Primitive Catalyst | 100 | 50 |

   The 16-color families are datagen loops over the dye/output arrays, not hand-written entries. (~75 recipes total.)
   **Deferred to Tier 2:** Magma Block, Tinted Glass, Eye of Ender.
   **Metastasizer additions riding along in the same datagen pass:** plain Terracotta (Clay Blend 1000/200t), Cobweb (Protein Blend 250/50t), Leather (Protein Blend 1000/200t) duplication recipes.
4. **Assets/data:** blockstate + block model (placeholder texture cloned from the Metastasizer's until art exists), item model, `en_us.json` lang entries (block + recipes' new items have none — all outputs are vanilla or existing), loot table, block tags (mirror Metastasizer's), `HAS_SCREEN` tag.
5. **`./gradlew runData`** — commit the full `src/generated/resources` diff.

## Phase 5 — Verify (runClient)

1. Craft path: implant recipe produces the Mutator (creative-place is fine for the first pass; implant verification can ride the existing early-implant flow).
2. Place; GUI opens via Outerface (`HAS_SCREEN`); fuel gauge, reagent tank, slots render.
3. **Mutate:** Bladder + Cuprous Blend + fuel → Fuel Bladder appears in output; input shrank by 1; reagent drained by 750.
4. **Toggle** to fill: button swaps item↔fluid icon; recipe processing stops.
5. **Fill:** empty Flask in input + Water in reagent tank → flask fills at 250 mB/cycle (rigid-buffer path exercised if the Flask is atomic-fill).
6. **HP:** run unfueled → HP drains during mutate; at 0, both mutate *and* fill refuse until fully healed.
7. Automation spot-check: hopper on a side feeds the input slot; duct/Node drains the output from below.
8. `./gradlew runGameTestServer` still green; `./gradlew build` clean.

## Build order rationale

Recipe type first (Phase 1) because the BE compiles against it; block/BE second because menu/screen compile against *that*; datagen last because it needs every registry name to exist. Each phase compiles independently — safe stopping points between sessions.

## Known deviations to expect

- The mode toggle is the only genuinely novel UI element; everything else is a straight copy. If the item/fluid button reuse reads badly in practice, fall back to a simple two-state label button and flag the art need (same "no dedicated texture yet" pattern as Mr. Farmer's fuel gauge).
- Fill-mode edge cases (container already partially full of a *different* fluid, stack of containers in the input slot) — fill only the top item, reject mismatched fluids via simulate; don't over-engineer the first pass.
