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

### Machines: block + block entity + menu/screen

Each machine (Drooling Cauldron, Masticator, Skin Tank) follows a 4-part pattern:
1. **Block** in `block/custom/` (e.g. `MasticatorBlock`) — extends `ModBaseEntityBlock`.
2. **Block entity** in `block/entity/custom/` extends `MachineBaseBlockEntity` (`block/entity/custom/MachineBaseBlockEntity.java`), which provides shared crafting-progress tracking (`progress`/`maxProgress`), item-handler/fluid-tank factory helpers that auto-sync (`updateBlock()`/`setChanged()`), drop-on-break logic, and the block-entity update-packet boilerplate.
3. **Menu** in `screen/custom/<machine>/` extends `AbstractModMenu`.
4. **Screen** in `screen/custom/<machine>/` extends `AbstractModScreen`, registered client-side in `DermicraftClient.registerScreens`.

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
