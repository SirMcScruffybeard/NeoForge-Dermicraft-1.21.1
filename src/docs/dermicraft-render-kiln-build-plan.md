# Render Kiln — Build Plan

Implementation plan for the Render Kiln machine. Design authority: `dermicraft-machine-notes.md` → Render Kiln entry (concept, decided points, open questions). This doc is the build order — when the two disagree, the machine notes win and this doc should be corrected.

**Template machine: the Metastasizer**, with the pattern slot removed entirely. The Metastasizer is `[fluid] + [non-consumed pattern item] → duplicate of that pattern`; the Kiln collapses that to `[fluid] → fixed default item` — no pattern, no second item input. Structurally it's the Metastasizer's `REAGENT_TANK` + `OUTPUT_SLOT` half only, with the recipe keyed on fluid alone instead of `{fluid, pattern}`.

---

## Phase 1 — Recipe type

New package `recipe/rendering/`:

- **`RenderingRecipe`** (record) + nested `Serializer`, copied from `MetastasizingRecipe` with the pattern field dropped:
  - Fields: `Fluid fluid, int fluidAmount, ItemStack result, int ticks`.
  - **New `FluidOnlyRecipeInput`** record in `recipe/` (sibling to `OneFluidOneItemRecipeInput`/`TwoFluidRecipeInput`): `record FluidOnlyRecipeInput(FluidStack fluid) implements RecipeInput` — `size()` returns 0, no `getItem(int)` override needed beyond the interface default (or throw, matching how a fluid-only input has no item slot to report). This is the one genuinely new recipe-input shape the Kiln introduces; every other machine's input record carries at least one item.
- **`ModRecipes`**: register `RENDERING_TYPE` + `RENDERING_SERIALIZER` (`dermicraft:rendering`), following the existing registration block pattern.

## Phase 2 — Block + BlockEntity

- **`RenderKilnBlock`** in `block/custom/` — copy `MetastasizerBlock` (extends `ModBaseEntityBlock`). Register in `ModBlocks`. Carry the same tags the Metastasizer has (`HAS_SCREEN`, COLLECTIBLE, etc.).
- **`RenderKilnBlockEntity`** in `block/entity/custom/` — copy `MetastasizerBlockEntity`, extends `AbstractFueledMachineBlockEntity<RenderingRecipe>`. Register in `ModBlockEntities`.

Changes from the Metastasizer copy:

1. **Slots (2, not 4):** 0 = fuel container, 1 = `OUTPUT_SLOT`. No pattern slot at all — `PATTERN_SLOT` and its bookkeeping (`hasPattern()`, the pattern half of `getItemHandler`/`getChannels`) are deleted outright, not stubbed.
2. **Tanks:** `FUEL_TANK` (from base) + one `VulnerableTank` **input tank** (rename `REAGENT_TANK` → `INPUT_TANK` throughout the copy — "reagent" implies a co-ingredient, this fluid *is* the whole recipe), tier-sized same as Metastasizer. Standard fuel/HP template per the machine notes (explicitly not the Furnace/Grafting Table `NO_HEALTH` hard-stop pattern) — no change needed here since `AbstractFueledMachineBlockEntity` already provides that; just don't special-case anything.
3. **`onCraftComplete()`:** drain `INPUT_TANK` by the recipe's `fluidAmount`, insert `result` into `OUTPUT_SLOT`. No item to consume (there is no input item slot).
4. **Recipe resolution:** `getRecipeOptional()` builds `new FluidOnlyRecipeInput(INPUT_TANK.getFluid())` instead of `OneFluidOneItemRecipeInput`; resolution now only triggers from `INPUT_TANK`'s `onContentsChanged()` (no pattern-slot trigger to remove, since there is no pattern slot).
5. **Face routing / channels:** drop the pattern channel/face entirely. UP = fuel, DOWN = output, sides = input fluid only (no item channel on the sides at all, unlike the Metastasizer where sides carry both reagent fluid *and* pattern item). `describeFace` simplifies to three cases (fuel/output/input) instead of the Metastasizer's three-with-a-shared-default.
6. **`getItemHandler(Direction)`:** UP → fuel slot, DOWN → output slot (extract-only, same "never accept automation inserts" rule), sides → **no item handler at all** (there's no item slot to expose there — the input is pure fluid). Simplify rather than returning an empty handler for parity with the Metastasizer's shape.
7. **`drops()` / `drainOutputs()`:** unchanged pattern from the copy (single output slot, push-below-neighbour every 5s).
8. **`ModBusEvents` capability registrations:** copy the Metastasizer's registration lines, adjusted for the narrower face set (no side item capability).
9. **Visual state (idle/on/error front textures)** — the Metastasizer copy has none of this; pull it from `MasticatorBlockEntity`/`MutatorBlockEntity` instead (both implement the identical mechanism). New `RenderKilnVisualState` enum (`IDLE`/`RUNNING`/`RECOVERING`, `StringRepresentable`, serialized names `"idle"`/`"running"`/`"recovering"`), copied from `MasticatorVisualState`. `RenderKilnBlockEntity` overrides `tickHealing(boolean fueled)` to call `super.tickHealing(fueled)` then `updateVisualState()`, same debounce as the Masticator (`VISUAL_STATE_STABLE_CYCLES = 2`, a state must hold for 2 consecutive cycles before it commits via `level.setBlock(..., Block.UPDATE_CLIENTS)` — avoids texture flapping from a hopper feeding on a different cadence than `CRAFT_TICKS`). `computeVisualState()`: RECOVERING if `health < maxHealth` (takes priority — a damaged machine signals distress even mid-cycle), else RUNNING if `!isStarved() && isRecipeValid(activeRecipe) && hasCraftingInputs() && hasCraftingOutputRoom()`, else IDLE. `RenderKilnBlock` gets a `STATE` blockstate property (mirrors `MasticatorBlock.STATE`) alongside `FACING`.

## Phase 3 — Menu + Screen

New package `screen/custom/render_kiln/`, copied from `metastasizer/`:

- **`RenderKilnMenu`** — copy, minus the pattern slot. Two `Slot`s total: fuel container, output. Register in `ModMenuTypes`.
- **`RenderKilnScreen`** — copy of the Metastasizer screen (composited `screen_parts` layout per [[feedback_machine_gui_composited_layout]]), minus the pattern slot and its tooltip. Elements: HP bar, fuel gauge + slot, input-fluid gauge (`createFluidRenderer16x40` + `renderFluidTooltipArea`), progress arrow, output slot. No second tank, no pattern-item slot — visually simpler than every other fueled machine screen so far (closest in spirit to a single-tank Masticator run without the ingredient item).
- Register the screen in `DermicraftClient.registerScreens`.

## Phase 4 — Datagen

1. **Implant recipe** (`render_kiln_implant`, `EarlyImplantRecipe`, copy the shape of `render_furnace_implant`/`metastasizer_implant`) — **decided:** 2 Dense Muscle + 2 Nerve Cluster + Furnace + Bucket, sutured, injected with 100 mB Primitive Catalyst (matches the standard injection amount every other early-implant recipe uses). **Alternate recipe:** identical ingredient list with Beaker swapped in for Bucket. Two separate `EarlyImplantRecipe` JSON entries (`render_kiln_implant` / `render_kiln_implant_alt`), both producing the Render Kiln — same "two ingredient routes, one result" shape as nothing else currently in the mod, but mechanically just two ordinary implant recipes sharing a result.
2. **`RecipeBuilders`**: add a `rendering(...)` builder (fluid, amount, result, ticks) mirroring the metastasizing builder but with no ingredient parameter.
3. **Launch roster** in `ModRecipeProvider` — mirrors the existing Metastasizer reverse-duplication recipes **exactly** (same mB, same ticks — confirmed pricing convention: the Kiln's convenience is not needing to already own the pattern item, not a discount). Values below are pulled directly from the generated recipe JSONs (`src/generated/resources/data/dermicraft/recipe/metastasizing_*.json` and `meat_flavored_meat_metastasizing.json`/`mre_metastasizing.json`), not re-derived:

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

   **Deliberately excluded from launch:** Crude Slurry — confirmed no Kiln recipe (no solid form at all in the design notes, and no single default item was decided). Protein Blend and F-Stuff are now both in, defaulting to their existing Metastasizer duplication targets (Meat Flavored Meat, MRE) rather than any of Protein Blend's other roster items (Inert Tumor, Marred Tumor, Dense Muscle, Nerve Cluster, Eye) — those stay Metastasizer-only, no ambiguity since Meat Flavored Meat/MRE are each fluid's one processed "ration" item.
4. **Assets/data:** blockstate + block models, item model, `en_us.json` lang entries (block only — every recipe output is a vanilla item with an existing lang key), loot table, block tags (mirror Metastasizer's), `HAS_SCREEN` tag.
   - **Blockstate = `facing × state` matrix, copied from `masticator.json`'s shape**: 4 facings (`north`/`east`/`south`/`west`, via `y` rotation) × 3 states (`idle`/`running`/`recovering`) = 12 variants. Three block models needed: `render_kiln` (idle/base), `render_kiln_on`, `render_kiln_error` — same three-texture front-face set as the Masticator/Mutator (`_on`/`_error` suffixes), not a Metastasizer-style single static model.
5. **`./gradlew runData`** — commit the full `src/generated/resources` diff.

## Phase 5 — Verify (runClient)

1. Craft path: implant recipe produces the Render Kiln (creative-place is fine for the first pass once the implant recipe exists; otherwise creative-place only for this pass).
2. Place; GUI opens via Outerface (`HAS_SCREEN`); fuel gauge, input-fluid gauge, output slot render — no pattern slot present.
3. **Render:** fill input tank with (e.g.) 1000 mB Stone Blend + fuel → Stone appears in output slot after 200 ticks at speed 1.0; input tank drains by exactly 1000 mB.
4. **HP:** run unfueled → HP drains during processing (unfueled trickle, not a hard stop); at 0, processing pauses until healed, matching the standard template (not the Furnace/Grafting Table's `NO_HEALTH` behavior).
5. Automation spot-check: side face accepts fluid input via duct/Node; bottom face drains the output slot; top face accepts fuel.
6. Confirm no pattern-item slot is exposed anywhere (menu, screen, capabilities, channels) — the one thing that must NOT carry over from the Metastasizer copy.
7. `./gradlew runGameTestServer` still green; `./gradlew build` clean.

## Build order rationale

Recipe type first (Phase 1) because the BE compiles against it and introduces the one new primitive (`FluidOnlyRecipeInput`); block/BE second because menu/screen compile against *that*; datagen last because it needs every registry name to exist. Each phase compiles independently — safe stopping points between sessions.

## Known deviations to expect

- `FluidOnlyRecipeInput` is new — every existing recipe input record carries at least one `ItemStack`. Double-check `RecipeInput`'s interface contract (`getItem(int)`/`size()`) doesn't assume at least one item slot anywhere in vanilla's recipe-manager plumbing before assuming a `size() == 0` input behaves correctly end-to-end.
- The two-recipe implant (bucket vs. beaker route to the same result) is a shape not used anywhere else in the mod yet — worth confirming `EarlyImplantRecipe`/its datagen builder doesn't assume a 1:1 result↔recipe relationship anywhere before assuming two JSON files sharing a `resultFluid` "just works."
