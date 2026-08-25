# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Dermicraft is a NeoForge mod for Minecraft 1.21.1 (Neo version 21.1.228, Java 21). It's a body-horror/biology-themed tech mod: tumors that are harvested for organic parts, fluids (blends, slurries, catalysts) processed through custom machines (Drooling Cauldron, Masticator, Skin Tank), and surgical tools (scalpel, forceps, suture kit, syringe) used to interact with tumor blocks and entities.

Mod id: `dermicraft`. Base package: `net.scruffy.dermicraft`.

## Commands

This is a Gradle project using the NeoForge ModDevGradle plugin. Always use the wrapper (`./gradlew` / `gradlew.bat`), not a system Gradle.

- Build: `./gradlew build`
- Run the client (launches a dev Minecraft client with the mod loaded): `./gradlew runClient`
- Run a dedicated server: `./gradlew runServer`
- Run registered gametests headlessly: `./gradlew runGameTestServer`
- **Run data generators** (regenerates everything under `src/generated/resources`, e.g. block states, item models, recipes, tags, loot tables): `./gradlew runData`
- There is no separate lint or unit test task configured beyond the gametest server run.

After changing anything in `datagen/` (providers for block states, item models, recipes, tags, loot tables, data maps), re-run `runData` and check the diff under `src/generated/resources` — that directory is committed and must stay in sync with the providers.

## Architecture

### Registration pattern

Every registry (`ModBlocks`, `ModItems`, `ModBlockEntities`, `ModFluids`, `ModFluidTypes`, `ModRecipes`, `ModEffects`, `ModDataComponentTypes`, `ModMenuTypes`, `ModDamageTypes`) follows the same NeoForge `DeferredRegister` shape: a static `DeferredRegister` field, static `DeferredHolder`/`DeferredBlock`/`DeferredItem` constants, and a `public static void register(IEventBus)` that's called from the mod constructor in `main/Dermicraft.java`. When adding a new block/item/etc., follow the existing pattern in the corresponding `Mod*.java` file rather than registering ad hoc.

`ModBlocks.registerBlock(...)` auto-registers a matching `BlockItem` in `ModItems.ITEMS` — blocks and their items are not registered independently.

Fluids are more involved: each fluid defines a source `FlowingFluid`, a flowing `FlowingFluid`, a `LiquidBlock`, a bucket item, and a `BaseFlowingFluid.Properties` tying them together (see `ModFluids.java`). The fluid's rendering (`FluidType`/render layer) is registered separately client-side in `DermicraftClient`, and the `FluidType` itself lives in `ModFluidTypes`/`BaseFluidType`.

Bucket items do **not** get a dedicated texture per fluid. `ModItemModelProvider` builds a two-layer item model for every bucket: `layer0` is the untinted vanilla `minecraft:item/bucket`, and `layer1` is one of two shared fill overlays (`bucket_chunky_fluid` for blends/slurries, `bucket_fluid_thin` for thin/water-like fluids like catalysts and Stuff outputs) picked via `chunkyBucketItem`/`thinBucketItem`. The fill layer is tinted per-fluid at runtime by a `registerBucketTint` call in `ModClientEvents`, which reads the fluid's `getTintColor()`. When adding a new fluid, add its bucket to both `ModItemModelProvider` (pick chunky or thin) and `ModClientEvents.registerItemColors` — don't paint a new bucket texture.

`Dermicraft.java` (common) vs `DermicraftClient.java` (`@Mod(..., dist = Dist.CLIENT)`) — keep client-only concerns (block entity renderers, screens, render layers, item color/property handlers) out of the common class.

**Checklist for adding a new fluid** (grep every `SOURCE_<EXISTING_FLUID>` reference across `src/main/java` to catch project-specific extras like `GlassFlaskItem`'s per-fluid dispatch — this list covers the general/infrastructure touchpoints only, not one-off mechanics tied to a specific existing fluid):
- `ModFluidTypes.java` — register the `FluidType` (tint color, fog color vector, viscosity/density/temperature/motionScale). If the fluid is emissive, `FluidType.Properties.lightLevel(int)` only affects fluid-holding **items** (e.g. the Beaker reads it via `getFluidType().getLightLevel(fluid)`) — it does **not** light up the block placed in the world. For that, also set `.lightLevel(state -> N)` on the `LiquidBlock`'s own `BlockBehaviour.Properties` in `ModFluids.java`. Both are needed for a fluid to glow everywhere.
- `ModFluids.java` — source `FlowingFluid`, flowing `FlowingFluid`, `LiquidBlock`, bucket item (`getBucket` helper), `BaseFlowingFluid.Properties`.
- `ModItemModelProvider.java` — `chunkyBucketItem(...)` or `thinBucketItem(...)` for the bucket.
- `ModClientEvents.java` — `registerBucketTint(...)` so the bucket's fill layer actually gets tinted.
- **`DermicraftClient.java` — easy to forget, breaks the bucket/block's rendering silently (shows as missing/broken model) if skipped:**
  - `renderTranslucentFluid(...)` in `onClientSetup`.
  - `registerFluidType(...)` in `onClientExtensions`.
- `ModFluidTagProvider.java` — `THICK` or `THIN` tag (drives Beaker/Glass Flask/Syringe fill-level rendering automatically, no per-item code needed), plus any hazard tags (`EXTREME_HEAT`, etc.) and family tags (e.g. `BIOFUELS`) that apply.
- `ModItemTagProvider.java` — the **item-side** mirror of any fluid-side family tag added above (e.g. `BIOFUELS` exists on both the fluid tag and an item tag over the bucket — easy to add one and miss the other).
- `ModDataMapProvider.java` — `BIOFUELS` data map entry if it's a fuel (speed/use-rate/heal/tier), `EDIBLE_FLUID` if it's meant to be drinkable.
- A recipe (Masticator/Effluencing/etc., via `RecipeBuilders`) so the fluid is actually reachable in survival — see [[feedback_survival_reachability_check]].
- `en_us.json` — `item.dermicraft.<bucket_id>` and `fluid_type.dermicraft.<fluid_type_id>` lang keys.
- `ModCreativeModeTabs.java` — the bucket (`output.accept(ModFluids.<X>_BUCKET)`), and a filled Beaker stack (`buildBeakerContents`) if other fluids in its family get one. **Separate from registration** — easy to assume the tab picks new items up automatically; it doesn't.
- Run `./gradlew runData` afterward and check the diff under `src/generated/resources`.

### Machines: block + block entity + menu/screen

Each machine (Drooling Cauldron, Masticator, Skin Tank) follows a 4-part pattern:
1. **Block** in `block/custom/` (e.g. `MasticatorBlock`) — extends `ModBaseEntityBlock`.
2. **Block entity** in `block/entity/custom/` extends `MachineBaseBlockEntity` (`block/entity/custom/MachineBaseBlockEntity.java`), which provides shared crafting-progress tracking (`progress`/`maxProgress`), item-handler/fluid-tank factory helpers that auto-sync (`updateBlock()`/`setChanged()`), drop-on-break logic, and the block-entity update-packet boilerplate.
3. **Menu** in `screen/custom/<machine>/` extends `AbstractModMenu`.
4. **Screen** in `screen/custom/<machine>/` extends `AbstractModScreen`, registered client-side in `DermicraftClient.registerScreens`.

**A variant with its own `BlockEntityType`** (a Charred-family evolution, Drooling Crucible, or any other block that reuses a base class's block entity via a distinct registered type) **needs its own `RegisterCapabilitiesEvent` entries in `ModBusEvents.java`** — capabilities are registered per `BlockEntityType`, not inherited by subclassing. Skipping this doesn't crash or show an error: `FluidUtil.interactWithFluidHandler`/hopper automation just silently find nothing there, and for a machine block whose `useItemOn` falls through to `useWithoutItem` on `PASS_TO_DEFAULT_BLOCK_INTERACTION` (the Masticator/Metastasizer/Mutator interaction shape), the *visible* symptom is holding a bucket and getting the GUI to open instead of the tank filling/draining. Has recurred three times already (Drooling Crucible, Charred Masticator, Charred Metastasizer) — check this immediately for any new variant block entity type.

Fluid handling inside machines goes through `tank/ModFluidTank` (extends NeoForge's `FluidTank`, adds fill/drain/transfer convenience) and its subclasses `FuelTank`, `WaterTank`, `VulnerableTank`. Item/fluid transfer helpers used by tanks live in `util/ModFluidUtil.java`.

### Recipes

Custom recipe types live under `recipe/<type>/` (`drooling`, `early_implant`, `masticating`, `puddle_crafting`), each with a recipe class + nested `Serializer`, and most with a paired `RecipeInput` record (`OneFluidRecipeInput`, `OneFluidOneItemRecipeInput`, etc. in `recipe/`). All serializers/types are registered centrally in `recipe/ModRecipes.java`. "Vague" recipes (`VagueDroolingRecipe`, `VagueMasticatingRecipe`) implement `interfaces/IVagueRecipe.java`, which derives crafting time/output amount from an ingredient's food properties (nutrition × saturation) rather than fixed values — used for recipes whose output scales with how "nutritious" the input is.

Datagen recipe builders live in `datagen/recipe/RecipeBuilders.java` and are invoked from `datagen/recipe/ModRecipeProvider.java`.

### Tumors and tools

Tumor blocks (`block/custom/tumor/`) all extend the abstract `TumorBlock`, which exposes `isCollectionTool` / `isExtractionTool` / `isInjectionTool` / `isSutureTool` checks backed by `util/ToolUtil.java`. Different tumor variants (Eye, Muscle, Nerve, Marred, Stitched, Inert, EarlySurgeryTumor) implement the relevant capability interfaces from `interfaces/` (`IHarvestableBlock`, `IInjectableBlock`, `ISutableBlock`, `IBloodLet`, `ISuture`, `IHarvestParts`, `ICollectBlocks`) depending on what tools can act on them. Tools in `item/custom/` (`ScalpelItem`, `ForcepsItem`, `SyringeItem`, `SutureKitItem`) extend `item/custom/base/ToolItem` and dispatch to those interfaces on right-click/use. `PartItem` (`item/custom/base/PartItem.java`) is the base for harvested organic parts (eye, nerve cluster, dense muscle), which carry food properties from `property/ModFoodProperties.java`.

### Datagen

`datagen/DataGenerators.java` is the single `GatherDataEvent` subscriber wiring up every provider: loot tables, block/item/fluid tags (`datagen/tag/`), item models, block states, recipes, and data maps (`datagen/datamaps/`). Run `./gradlew runData` after editing any provider and commit the resulting changes under `src/generated/resources`.

### Config

`main/Config.java` defines the `ModConfig.Type.COMMON` spec, registered in the `Dermicraft` constructor.
