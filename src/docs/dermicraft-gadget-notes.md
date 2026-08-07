# Dermicraft Gadget Notes

Running log of decided design choices for Gadgets and the reasoning behind them. Add a new entry per gadget as decisions get made.

---

## Gadget naming convention

Each gadget's official name follows an acronym + suffix pattern (`[ACRONYM].Gadget` or `[ACRONYM].Rig`). The last letter of the acronym is the first letter of the gadget's full written-out word (exact full words not given yet). The two carried-over gadgets keep their old acronym names for now; renaming is part of the rework but not finalized, and it's undecided whether a rename will change the in-game display name, just the internal code identifier, or both.

**Simplified discussion names:** Because the acronym names are tedious to type repeatedly, simplified shorthand names will be used in conversation going forward. The acronym names above remain the canonical/real names for documentation purposes — this doc tracks entries under the real names regardless of what shorthand gets used in a given session. Confirmed so far: D.R.I.N.K.E.Rig = "Drinker," S.I.P.P.I.N.Gadget = "Sipper." Eater, Lobber, and Grapple (G.R.A.P.P.L.E) are also confirmed acronyms (full expansions still pending); Sunder, Drill Hammer, and Capture Net Gun are discussion shorthand only, not yet confirmed as acronyms.

## Shared infrastructure

### Bulk item storage (`IHaveItemData` / `BulkItemData` / `BulkSlot`)

**Status:** Built 2026-07-29, wired to Eater's base tier as of 2026-07-31 (see Eater's entry below) and confirmed working in-game. M.A.W.S. (Mobile Autonomous Ware Storage — formerly discussed as "Portable Mass Storage", renamed since; see `maws-gadget-notes.md`), the other planned consumer, hasn't started yet.

**What it is:** The item-side twin of `IHaveFluidData`, for gadgets that carry a multi-slot bulk item buffer inside their own held `ItemStack`. Built while planning Eater's internal item buffer, once it became clear Eater and the not-yet-built **M.A.W.S.** gadget would both need this and should share one implementation rather than diverge.

**Why not vanilla's `DataComponents.CONTAINER`:** vanilla's container component (used by Bundles/Shulker Box items) validates every stack through the standard `ItemStack` codec, which caps count at 99 and crashes with `IllegalStateException` on save above that. M.A.W.S.'s per-slot stack multiplier (2–5× a normal stack, scaling with upgrades) can exceed 99, so vanilla's component can't back it — same problem Craw (`CrawBlockEntity`) already hit and fixed for block-entity storage.

**The fix, applied to a held item instead of a block entity:** `BulkSlot` keeps an item's identity as a count-1 template (component data and all) plus the real count tracked separately as a plain int, exactly like Craw's pattern. `BulkItemData` is a fixed-size (resizable via `withSize`, for tier upgrades) list of `BulkSlot`s, registered as the `BULK_ITEM_DATA` data component. `IHaveItemData.BulkItemHandler` wraps that into a normal `IItemHandlerModifiable`, parameterized by slot count, per-slot capacity, and an optional filter predicate.

**Decision — single shared backing for both consumers, not two:** Eater's slots (small, fixed at 4, never expected to exceed a normal stack) could have used plain vanilla container storage instead. Went with the bulk-capable store for both anyway: it costs Eater nothing at small numbers, future-proofs it if a later tier ever scales capacity past 99 (avoiding a breaking migration), and keeps one handler implementation to trust rather than two parallel ones drifting apart — consistent with the mod's existing preference for shared bases across tiers/families.

**Not yet decided by this groundwork:** how a "disposal slot" (M.A.W.S.'s void-on-insert feature) is expressed — no dedicated Disposal handler class was built here, since items have no mod-wide hazard-profile concept to gate against the way fluids do; left to whichever gadget needs it (a filter that always rejects, paired with the gadget discarding on insert, or something else) rather than baked into the shared interface.

**Open questions:** Whether M.A.W.S.'s filter-per-slot and disposal-slot features end up needing anything beyond what `BulkItemHandler`'s constructor already exposes (capacity, filter). Whether Eater's eventual registration wants a plain `BulkItemHandler` or needs its own thin subclass for any Eater-specific insert behavior (e.g. interaction with its Storage/Transfer/Disposal modes).

### Gadget upgrade points (shared mechanic, separate pools)

**Status:** Design decided 2026-07-31. Not yet implemented — no code exists for this yet.

**What it is:** The answer to the long-open "Gadget Bench"/"Gadget Upgrade System" question that's been sitting unresolved across several gadgets' docs (Grapple's frame/install question, Drill Hammer's head-swap system, Sunder's unfueled-mode upgrades). Every complex gadget — and the suit — gets a point-spend upgrade menu, mirroring the suit add-on convention M.A.W.S. already committed to (see `maws-gadget-notes.md`). Suit-style 1-point-per-rank pricing, magnitude tuned per category rather than cross-balanced, is the expected default shape for a gadget's menu, though not mandated for every one.

**Critical clarification (corrects an earlier misstatement in `maws-gadget-notes.md`):** what's shared across gadgets/suit is the **point-spend mechanic itself**, not a currency. Each suit and each complex gadget holds its **own separate point pool** — M.A.W.S. does not draw from the same pool as suit add-ons, despite what that doc said before this correction. This is the same shape as `HazardProfile`/`MachineTier`: a reusable pattern instantiated independently per consumer, not a single shared global instance.

**Which gadgets use it:**
- **M.A.W.S.** — point-spend only (slots, stack cap, filtering, disposal slots — see its own doc). No physical part-swapping involved.
- **Drill Hammer / Sunder** — point-spend **plus** exchangeable physical parts (Drill Hammer's material heads already established; Sunder's chain material mirrors this — see its own entry below) — two mechanics layered together, not point-spend alone. Their "different nature" from M.A.W.S. is exactly this: magnitude/utility upgrades via points, identity/material upgrades via swappable parts. Points aren't limited to smooth per-rank stats — they can also buy discrete ability unlocks (precedent: E.A.T.E.R. and M.A.W.S. both gate features this way), which is how Sunder's mutually-exclusive Safety/"mundane sword" unfueled-mode upgrades fit in: point-purchased abilities, not a separate mechanic.
- **Sunder specifically** also layers in a **tier ladder** (Growth Chamber) on top of the point-spend + physical-part combo above — reworked 2026-08-04 into stat bumps + point-pool growth + exotic-chain-material gating, not the "capability leaps only" framing this used to carry (see Sunder's own entry for why that changed). See Sunder's own entry for its full four-axis breakdown (chain material / tier / points / fuel grade).
- **Grapple** — likely reworked, including a probable name change; its current bespoke 6-tier capability-leap ladder (see Grapple's own entry above) should NOT be treated as the template going forward. Whether Grapple ends up using the point-spend pattern too, keeps its own tier ladder, or some hybrid is unresolved pending the rework.

**How points are earned (2026-07-31, follows the suit's own lead — see `dermicraft-suit-notes.md` → Tier & Point System):** a fixed number of points granted per tier-up, plus — once a gadget/suit is at its final tier and no more tier-ups are coming — an additional system to buy further points at an ever-increasing cost, paid in large volumes of difficult-to-produce materials. This gives continued progression past the tier ceiling while the escalating material cost naturally decelerates it. Deliberately the same rule for gadgets as for the suit, not a separate invented system — consistent with "shared mechanic, separate pools" (this entry's own title). The exact per-tier point count and the max-tier material/cost curve are still open even for the suit itself, so they're open here too, not a gap specific to gadgets.

**Where the point-spend menu lives (2026-07-31, resolved):** the **Workbench** (one of the three Gear Stations — see `dermicraft-gear-stations-notes.md`), Duty 7. Not a new/separate "Gadget Bench" structure — that was always just a working placeholder name, and it resolves onto the Workbench GUI, mirroring how the suit's own point-spend menu already lives at the Dock.

**Open questions:** Whether every complex gadget is required to have a point-menu, or whether some (a reworked Grapple, e.g.) legitimately stay tier-ladder-only. Exact shape of the "exchangeable parts" mechanic for Drill Hammer/Sunder and how it interacts with the point-spend menu (same Workbench UI, presumably, but not explicitly confirmed). Exact per-tier point count and max-tier material/cost curve (open for the suit too, not gadget-specific).

## Known gadgets

### D.R.I.N.K.E.Rig ("Drinker")

**Status:** Carried over from the old (pre-rebuild) version; needs reworking (including eventual rename — see naming convention above). Core function, capacity, fluid restriction, modes, and a durability quirk all established.

**What it is:** A fluid vacuum — drains fluid directly from world fluid blocks into its internal tank.

**Capacity:** `1000 mB` internal tank.

**Fluid restriction:** Tier 1 (see `dermicraft-project-primer.md` Stage structure) — cannot handle hazardous fluids like lava. Confirmed behavior: it simply will not attempt to drain a hazardous fluid at all, rather than draining it and failing/breaking afterward.

**Additional pull source (under review):** Currently can also pull from fluid-handling blocks (tanks, other machines with fluid capability), not just raw world fluid blocks. This may be removed — not finalized.

**Modes:**
- **Disposal** — erases fluid (void/trash mode, no recovery).
- **Storage** — holds fluid in the internal tank.
- **Transfer** — moves fluid to a fluid-handling item in the player's inventory if one is available; if not, falls back to storing the fluid in the internal tank until something becomes available.

**Mode switching:** Right-click cycles through the three modes. Whether the player is crouching or standing while right-clicking determines which direction the cycle moves — lets the player step forward or backward through Disposal/Storage/Transfer without overshooting. If fluid is currently stored, attempting to change mode starts a confirmation timer — repeat the action to confirm the change, or wait and it cancels (stays on the current mode). Prevents accidentally dumping/disposing of stored fluid with a stray right-click.

**Durability:** Takes damage from being dropped on the ground. Currently, if it breaks while holding fluid, the fluid simply disappears (no spill) — a spilling mechanic is being considered for the future but not yet implemented.

**Tier 2 requirement — hazardous mob-sourced fluids (confirmed):** Drinker's Tier 1 hazardous-fluid refusal now has a confirmed real case: **Dragon's Milk** (harvested from the Ender Dragon — see `dermicraft-crafting-notes.md`) is hazardous, so a base Tier 1 Drinker will refuse to draw it outright — same "won't even attempt it" pattern as any other hazardous fluid. A **Tier 2 Drinker** is required to harvest it at all. This is the first confirmed case of a Tier 2 Gadget requirement gating *access* to a fluid entirely, rather than adding a bonus capability on top of an already-functional Tier 1 base (compare Sprayer's Tier 2, below).

**Mob-target harvesting (confirmed):** Drinker's hold-to-use draw mechanic, previously only used on world fluid blocks, also works on a living mob target — right-click-and-hold on the Ender Dragon to harvest Dragon's Milk directly, no cooldown between harvests. First confirmed non-block fluid source for Drinker.

**Open questions:** How much damage from a drop (how many drops before it breaks)? Whether pulling from fluid-handling blocks stays or gets removed. Whether Tier 2 Drinker changes anything besides hazard access (capacity, mode set). Exact per-tick drain rate while holding on a mob target — deferred to Code.

### S.I.P.P.I.N.Gadget ("Sipper")

**Status:** Carried over from the old (pre-rebuild) version; needs reworking. Core function, shape, and capacity established.

**What it is:** Shaped like a tin can. Placed into a Machine to either transfer or dispose of the fluid currently inside that Machine — functionally the machine-facing counterpart to Drinker's world-facing fluid pull.

**Capacity:** `1000 mB` — same as Drinker.

**Modes:** Two of Drinker's three — **Disposal** and **Storage** — behaving identically to their Drinker counterparts (Disposal erases the fluid, Storage holds it in the internal tank). No Transfer mode.

**Mode switching:** Right-click toggles between the two modes (with only two modes, crouch/stand direction-switching like Drinker's doesn't come into play). Same confirmation-timer safeguard as Drinker: if fluid is stored, changing mode requires repeating the action to confirm, or waiting for it to cancel.

**Difference from Drinker:** A Machine can pull fluid directly from a Sipper while it's holding fluid (i.e. Storage mode's contents are accessible to the Machine it's placed in, not just a one-way dump into the Sipper). Not stated as something Drinker supports.

**Durability:** Doesn't currently have Drinker's drop-damage mechanic — Sipper was built before that mechanic existed. Planned to be added when Sipper gets reworked for the new version.

**Open questions:** How it physically interacts with a Machine (placed in a slot? right-clicked on the Machine block?). Does it share Drinker's Tier 1 hazardous-fluid restriction?

### S.P.R.A.Y.E.Rig ("Sprayer")

**Status:** Concept stage only — not yet built, mechanic ideas only.

**What it is:** A spray gun. Effect produced depends on the fluid currently in its tank — the same "fluid defines behavior" pattern seen with Flask variants, here applied to a Gadget instead.

**Known fluid effects:**
- **Water** — acts like a flamethrower specifically against Endermen; also usable to water crops.
- **Calcium Blend** — area-of-effect bone meal (an AoE version of Flask of Calcium Blend's single-application effect).

**Tiers:** Base version is Tier 1 (per the general Gadget/Machine rule — can't handle hazardous fluids). A **Tier 2** version is planned that can use **Lava** as fuel, functioning as an actual flamethrower — the first confirmed example of a Tier 2 Gadget. Tier 2's defining upgrade over Tier 1 is specifically lava capability (see `dermicraft-project-primer.md` Stage structure).

**Dragon's Milk — further-evolved tier (confirmed):** A **second Tier 2+ evolution**, beyond the existing Lava-flamethrower Tier 2, unlocks Dragon's Milk (see `dermicraft-crafting-notes.md`) as a Sprayer fuel. Effect: a **direct damage/effect spray** — not a lingering AoE cloud like vanilla Dragon's Breath or lingering potions — fired straight at whatever the player targets. Establishes Sprayer's evolution line as **branching at Tier 2** rather than linear (the Lava tier and the Dragon's-Milk tier are parallel further evolutions, not one leading to the other) — first confirmed branching evolution path in the mod, as opposed to the Effluentcer's straight-line 2→3→4 input progression. Recipe logic: consistent with Sprayer's "fluid defines the effect" identity — Dragon's Milk being sourced from the Ender Dragon justifies a dragon's-breath-flavored effect directly, no invented justification needed.

**Open questions:** What other fluids might get a Sprayer effect (other Slurries, Catalysts, or Crafting Blends)? Capacity? Mode-switching mechanic, if any (right-click cycle like Drinker/Sipper, or does the effect just follow whatever fluid is loaded with no separate "mode")? What's the actual process for a player to get/craft the Tier 2 version (separate item, an upgrade process, something forced-evolution-style like Machines)? Exact damage/effect numbers and range for the Dragon's Milk spray. Name for this evolved tier. Whether this evolution also requires Tier 2 hazard-handling the same way Drinker's did.

### Eater

**Status:** Base tier BUILT and working end-to-end (2026-07-31) — item logic, buffer, modes, GeckoLib rig, and animation all in place and tested in-game. One known cosmetic issue left unresolved (see below); everything else confirmed working.

**What it is:** The item-side counterpart to Drinker (which vacuums fluid) — Eater vacuums loose dropped items, and at higher tiers, blocks and ore. Confirmed as an acronym, expansion not yet decided (deliberately left untyped for now).

**Base tier:** Vacuums loose dropped items on the ground.

**Base-tier mechanic (built):**
- **Activation:** hold right-click, same held-trigger identity as Drinker. A real target always wins over crouch/stand gesture handling; crouching with nothing to vacuum acts on the buffer per mode (Transfer pushes it to the player, Disposal voids it behind an arm/confirm dance identical to Drinker's, Storage just reports itself inert), standing with nothing to vacuum cycles mode.
- **Targeting (revised from the original design):** 4-block range, narrowed to a 60°-half-angle forward cone (120° total) off the player's look vector — not a flat 360° sphere as first planned; in-game testing showed the sphere felt untargeted. A separate close-range "capture bubble" (1.5 blocks) bypasses the cone check entirely once an item is already this close, so an item mid-pull can't "escape" the cone as it nears the player and drops below eye level — without this, a pulled item would stall at the player's feet requiring the player to look back down at it.
- **Pull-then-consume, not instant vacuum:** items outside a 0.75-block "consume distance" ease toward the player (40% of remaining distance per tick, easing out rather than constant speed) instead of vanishing the instant they're detected — a deliberate cosmetic tail. Both the pull target and the consume-distance check are anchored to the same point (roughly chest height, not the feet) after an early bug where the two disagreed and items stalled forever just outside the threshold.
- **20-tick windup:** holding the trigger starts the mouth-bloom animation immediately, but actual pulling/consuming is gated behind `player.getTicksUsingItem() >= 20` so the covers have time to visibly open before anything moves. Vacuum candidates still get their vanilla pickup-delay refreshed for the whole windup (not just once pulling starts), otherwise vanilla's own walk-over-item pickup wins the race and grabs items before Eater ever touches them.
- **Modes:** all three of Drinker's — Storage, Transfer, Disposal — full family parity, including Disposal's arm/confirm safeguard.
- **Internal buffer:** 4-slot bulk item handler (see Shared infrastructure above), each slot a normal stack cap. Reasoning: a fluid buffer is "one fluid, variable quantity" so Drinker's single tank works, but a dropped-item pile is usually mixed (e.g. a skeleton's bones/arrows/string/gunpowder) — a single slot would jam on the first item type touched and ignore the rest of the same pile.
- **HP:** 10, matching Drinker.

**Model & rig (built):** GeckoLib model with a wide intake maw, four corner cover plates that iris open on activate (not the originally-imagined straight-line open), three gill-like partial ribs that flutter at idle and pulse in sync while active, and a top-mounted 2×2 grid of "screen" bones (one per buffer slot) that slide flush and show a live floating `ItemStack` render (via a custom `GeoRenderLayer`, not a baked icon) when their slot is occupied, staying dark/recessed when empty. Mode lights mirror Drinker's mutually-exclusive lit-texture swap.
- **Animation controllers:** one `Body` controller drives idle ↔ activate→active_hold (a single authored direction, relying on `transitionLength` blending for the return rather than a mirrored deactivate clip) plus the rib/mouth flutter; one controller per screen bone drives its own slide, gated on that slot's occupancy.
- **Fixed bug:** the four screen controllers' idle fallback originally reused the `idle` animation, which also contains the mouth-cover flutter keyframes — since GeckoLib doesn't blend bone writes across separate controllers, every empty screen's controller was silently re-applying `idle`'s mouth pose after `Body`'s own pass each frame, permanently masking the activate animation. Fixed by giving the screen controllers a genuinely empty `none` fallback clip instead.
- **Closed, not tracked further:** the `active_hold` loop was observed to flicker at the exact moment an item lands in the buffer. Investigated and ruled out the obvious cause (GeckoLib's `AnimationController` compares `RawAnimation` by content equality, not reference, per its own source/javadoc, so rebuilding an equivalent `RawAnimation` each tick — which both Eater's and Drinker's controllers already do — isn't it). No confirmed root cause found; decided not worth fighting the engine over for a cosmetic-only, rare flicker — closed as a non-issue rather than left open.

**Fabrication recipe (built 2026-07-31, via the new `gadget_fabricating` Workbench recipe type — see `dermicraft-gear-stations-notes.md`'s Workbench Duty 6/7):** Tier 1, 90 seconds. **Items:** 2× Chassis, 4× Craw, 1× Hopper, 1× Proto Brain. **Fluids:** 500mB Primitive Catalyst, 500mB Protein Blend, 500mB Silica Blend. Built directly off Drinker's own recipe (2 Chassis, 500mB Primitive Catalyst/Protein Blend each, 250mB Silica Blend for its one screen) as the closest precedent — Primitive Catalyst/Protein Blend scale with Chassis count, not overall complexity, so both stayed at Drinker's 2-Chassis rate; Silica Blend bumped to 500mB for Eater's 4 screen bones but deliberately not a literal 4× multiply, since Eater's screens are small item-icon windows rather than a full target-scan display assembly like Drinker's. No Eye/Bladder (Eater's buffer is item-based, not fluid-based, so neither applies). Craw ×4 supplies the item-handling guts (matches Eater's whole identity), Hopper is the intake justification (no invented lore needed — a Hopper's actual vanilla function already is "sucks items in").

**Open questions:** Numeric/tuning gaps flagged elsewhere in this entry (wool yield, fireball range, etc.) still apply. Recipe numbers above are a first pass, not playtested.

**Upgrade points (design decided 2026-07-31, not yet built).** Uses the shared Gadget upgrade points mechanic (see that entry above) — Eater has its own separate point pool, not shared with the suit or other gadgets. This section supersedes the earlier "Mid/Later tier" framing below it in spirit: block/ore/organic material access is **not** tier-gated or sequenced behind a difficulty ladder — every material category is independently purchasable, "rule of fun" over strict logical progression, since a player who only wants one category (wool, say) shouldn't be forced to buy others first.

**Tier-up grants (fixed, not bought):**
- **+1 buffer slot per tier.** The physical model only has 4 screen bones, so only the first 4 slots (by index — the same ones the vacuum already fills preferentially, see the base-tier buffer notes) ever get a visible display; slots beyond 4 are real but invisible overflow.
- **Active range increases per tier.**
- Cone angle (60° half-angle) stays fixed at every tier — no upgrade path for it.

**Point-bought, but deliberately fixed (not upgradeable at all):** pull speed (the 40%-per-tick easing), windup time (20 ticks), wool-shear duration (2 seconds/40 ticks).

**Free base abilities (no purchase, no gate):**
- **Loose dropped items** — the base-tier ability, always available (see above).
- **Container vacuuming** — aim and hold right-click on a container block (chest, hopper, furnace, etc.) to pull its inventory into Eater, routed through the same Storage/Transfer/Disposal modes as everything else. A manual convenience only, not automation — deliberately kept out of the point-shop and made free specifically for consistency with **Drinker**, whose equivalent (aim-and-drain a machine's fluid tank) has never been gated behind anything either. Sources from the container's `IItemHandler` via `extractItem` rather than the ground-item scan, but shares the same held-trigger gesture and mode routing. **Range is normal vanilla block-interaction reach, not Eater's tiered active range** — this is a "point at the thing you're already standing next to" convenience, not a long-range pull. The targeting raycast should be forgiving (generous hit tolerance / aim-assist toward the container under the crosshair) so the player doesn't need pixel-perfect aim on a chest's exact hitbox — exact leniency amount deferred to Code.

**Modes apply uniformly to every material category (2026-07-31, resolved):** ore, wool, fish, ink, and every other extraction all route through Storage/Transfer/Disposal exactly like loose items — no special-casing, no mode restriction per category. If a player forgets they're on Disposal while shearing a flock, the wool is voided; that's a player-error risk, not something the mechanic protects against. Consistent with "keep the mode system simple and universal" rather than adding per-category exceptions.

**Point-bought material categories (each independently purchasable, no forced sequencing, 1 point per rank — same convention as M.A.W.S.):**
- **Aggregates** — dirt, sand, gravel, clay, snow. Suck up loose blocks directly.
- **Ores** — pull material directly out of an ore block; drained block converts to its base stone-equivalent (Iron Ore → Stone) rather than being removed, matching Eater's "hollow it out" identity. Vein-detection depth (visible-only → full connected vein) lives as its own internal rank within this category, not a separate purchase.
- **Plant** — non-destructive crop harvest (wheat/carrots/potatoes pulled without breaking the plant, same "extract the product, leave the host" logic as everything else Eater does), leaves. **Fungi** (mushrooms, mycelium, shroomlights) is its own purchase but requires Plant already owned as a prerequisite — closer to plants than animals for this purpose despite being biologically distinct. Combines with **Fluid Bypass** (below) to unlock kelp harvesting.
- **Animal** — mass wool shearing (see full mechanic below) and honey extraction (pulled from a hive without breaking it). Combines with **Fluid Bypass** to unlock fish and squid ink — both living mobs, so both reuse the wool-shearing target-lock mechanic (aim, lock on, hold), not a block-vacuum the way kelp is; a real mechanical fork behind what reads as one narrative idea ("underwater organic harvest"). Ink specifically yields a non-lethal **Ink Pouch** item, distinct from vanilla's kill-only Ink Sac drop — squid ink is a solid item in vanilla (not a fluid requiring a bucket, unlike milk), so it belongs here rather than being a Drinker candidate the way milk is.
- **Fluid Bypass** — standalone, no prerequisite, player's own call whether to buy it. Fluids block Eater's block-targeting raycast by default (deliberately, not a bug to fix); this purchase swaps to a fluid-transparent raycast specifically for reaching submerged Aggregate deposits (sand/gravel/clay riverbeds/seabeds) from dry land/a boat without wading in. Combining it with Plant or Animal is what unlocks kelp/fish respectively (see above) — an emergent capability from two independent purchases together, same pattern as Grapple's Grapple Anchor add-on unlocking PvP grappling on top of base Grapple.
- **Fireball catching** — gated behind Tier 2 as a hard prerequisite (mirrors Drinker's Dragon's Milk gate exactly: Tier 2 doesn't grant it automatically, it's still its own separate purchase once reached). Catches ghast/blaze fireballs mid-flight and converts them to items — ghast → Fire Charge, blaze → Blaze Powder (no invented justification needed, matches existing drops/uses). Skill-based: targeting the source mob counts as targeting its fireball, so this reuses the same entity-raycast Eater already does elsewhere rather than needing real projectile tracking.
  - **Activation is near-instant, not the standard 20-tick windup.** The windup exists everywhere else so the mouth-bloom animation has time to open, but a defensive catch has to react in real time — a full second of windup would mean the fireball already hit the player before Eater did anything. This exception is bundled into the purchase itself (buying the ability grants both the capability and its special near-instant activation), not a separate cost.
  - **Tracking range is its own special value**, larger than Eater's normal tiered active range, specifically for detecting/locking onto ghast/blaze at the distance they actually engage from. The **cone stays the default for whatever tier Eater is on** — only the tracking distance is special-cased, not the aim precision required.
  - **No pull-in tail.** Unlike items/sheep, a fireball is already heading toward the player under its own momentum, so there's nothing to ease toward — on a successful catch it simply disappears and the converted item appears directly in the buffer. Exact interception timing/distance (how close it can get before catching becomes too late) will need in-game testing to tune, not fully specified yet.

**Mass wool suction (design decided 2026-07-31, not yet built):** Eater can mass-shear sheep, extracting wool directly by suction rather than cutting — lives under the Animal category above.
- **Reframe that unlocked this:** Drinker is a wet vac, Eater is a dry vac. Ore-hollowing already established Eater's real identity as "extract material by raw suction force, leave the host intact" rather than strictly "consumes matter" — wool is just the living-target version of the same move, not a departure from it. Considered and rejected an "overlaps with Nose on a Stick" objection: Nose on a Stick is a dowsing rod (detection only), so it's complementary (detects readiness) rather than competing with an extraction tool.
- **Target lock, not continuous re-aim:** the forward-cone/aim check only gates *acquiring* a target. Once a sheep is locked, it stays the active target — immune to incidental camera drift — until the shear finishes, the trigger is released, the sheep dies, or it exceeds a generous max-range escape distance. A stronger version of the same fix built for the item vacuum's "stuck at the feet" bug (see the base-tier mechanic notes above), needed even more here since a multi-second shear has to survive far more re-aim drift than a quick item grab.
- **Drag mechanic:** suction nudges the sheep's velocity toward the player while active (a push, not a leash-style position lock) — deliberately mirrors **Grapple's existing mob-pull logic** (its mass-comparison rule: mob mass < player mass → mob pulled toward player) rather than inventing new mob-pull physics, since a sheep will almost always qualify as lighter than the player under that rule.
- **Duration:** ~2 seconds (40 ticks, fixed — see above), continuous while locked, no persisted partial progress across an interruption.

**Shelved/parked ideas (not currently in the category list above):**
- **Frozen materials** (ice/packed ice/blue ice) — shelved: too solid/rigid to fit Eater's "loose or extractable surface material" identity the way Aggregates or ore-hollowing do. May resurface as a Sunder or Drill Hammer capability instead rather than Eater.
- **Sculk family** — parked pending a decision on how "solid" sculk blocks should be treated (same identity question as frozen materials, unresolved).
- **Targeting through fluids for non-Aggregate purposes** — resolved differently than first framed; see Fluid Bypass above, which is specifically scoped to Aggregates/Plant/Animal, not a general "ignore fluid" toggle.
- **Milking cows** — explicitly deferred to a future Drinker upgrade discussion, not Eater — fits Drinker's wet-vac identity (liquid animal product) rather than Eater's dry-vac one.

**Open questions:** Exact wool yield per shear (vanilla-matching or bulk-tool-generous). Whether sheared sheep get a vanilla-style wool-regrow cooldown or something else. Whether dyed wool color carries through. Point-of-failure behavior if the target dies or escapes max range mid-shear (partial yield vs. nothing). Fish extraction specifics (yield, whether it's lethal or catch-and-release). Exact fireball-catch numbers (range, any cooldown). Resolution on Frozen materials/Sculk, if they ever get revisited.

### Lobber (Fluid Grenade Launcher)

**Status:** Concept stage — core mechanics defined; name confirmed as an intentional acronym (expansion not yet written out).

**What it is:** A grenade launcher whose ammo is filled with various fluids that shatter and release an AoE effect on impact. Fluid effects **mirror Sprayer's fluid-effect list exactly** (e.g. Water, Calcium Blend) — same fluid identity, different delivery method (sustained spray vs. burst impact).

**Ammo:** A new, dedicated shatter-on-impact container — explicitly not the Flask. Name/material not yet decided.

**Aiming:** No separate mode toggle — purely aim-based. Aiming high lobs the grenade in an arc (for AoE placement at range); aiming flat/level fires it directly at short range.

**Reload:** Manual — right-click to load a vial. Single-shot to start; a magazine capacity is a planned later upgrade, mechanism deferred to the broader Gadget upgrade system (its own future design pass).

**Tier:** Not yet decided whether Lobber gets a Tier 2 lava-based upgrade mirroring Sprayer's.

**Dragon's Milk — lingering AoE cloud (confirmed):** Where Sprayer's evolved Dragon's Milk tier delivers a direct damage/effect spray, Lobber's version of the same fluid produces a **lingering AoE cloud** on impact — matching vanilla Dragon's Breath/lingering-potion behavior. Consistent with Lobber's core rule that its fluid effects mirror Sprayer's list exactly in *identity* but differ in *delivery* (sustained spray vs. burst impact) — this is the first case where that difference in delivery also changes the effect's *shape* (instant spray vs. lingering cloud), not just how it's fired. **Tier implication:** since Dragon's Milk is hazardous, this presumably requires an evolved/Tier 2+ Lobber, same pattern as Sprayer's own Dragon's Milk tier — this recipe may be what actually forces that Tier 2 Lobber into existence, given Lobber doesn't have a confirmed Tier 2 otherwise yet.

**Open questions:** What the full acronym expansion is. What AoE effects map to which fluids beyond Sprayer's existing list. Magazine-upgrade mechanism. Tier restriction (a Tier 2 version using lava for an AoE fire effect would fit the established Tier 2 = lava-capability pattern, similar to Sprayer). Cloud radius, duration, and damage/effect tick rate for the Dragon's Milk cloud. Whether Dragon's Milk is the same evolution step that brings Lobber's long-flagged Tier 2/lava question along with it, or a separate one.

### G.R.A.P.P.L.E ("Grapple")

**Status:** Concept stage — core mechanics defined. Name confirmed as the acronym **G.R.A.P.P.L.E** (shorthand "Grapple"); full expansion still pending. A suit **Grapple Add-on** reuses this same logic — see `dermicraft-suit-notes.md` (Universal Add-Ons).

**What it is:** A traversal Gadget modeled on Deep Rock Galactic's grapple — not a pendulum swing, but a **direct pull toward the anchor point**: fire at a valid target, get pulled straight to it at speed. What happens at arrival and how the tether can be exited is configurable — see **Tether States & Gadget Bench Options** and **Momentum Retention** under Tier Upgrades below.

**Projectile drop:** Deliberately little-to-no drop by design — this is a core identity trait of the Grapple's projectile, not a stat. This is also why fuel grade does **not** affect drop (unlike the Capture Net Gun, where drop is a real aiming factor at range).

**Fuel:** Slurry-powered, **Main Line only — no Serums** (Serums' defining unconditional-burn behavior fits a machine's continuous fuel tank, not a reload-cost item that sits loaded between shots). Cost is charged **at reload**; if the cost can't be paid, reload simply doesn't happen (no partial charge, no failure state beyond "can't reload yet"). Better fuel grade improves Range, Projectile Speed, and Pull Speed, using the Main Line's existing effective-speed multiplier (see `dermicraft-slurry-notes.md`) — no separate fuel curve invented for the Grapple.

**Base Tier-0 (Crude) values:**
- **Range:** 16 blocks (deliberately enough to clear a chunk in one pull).
- **Reload cost:** 100 mB per shot (matches the mod's existing single-use discrete-action cost, e.g. tumor implant recipes), before Chambered Fuel Cell's bulk-cell pricing applies.
- **Projectile speed:** anchored faster than a thrown Trident — concept locked, exact tick value deferred to Code.
- **Pull speed:** 2× player sprint speed.

**Fuel grade scaling** (Range shown as reference; Projectile Speed and Pull Speed scale by the same multiplier):

| Grade | Multiplier | Range (Tier 0) |
|---|---|---|
| Crude | ×1.00 | 16 |
| Concentrated | ×1.25 | 20 |
| Refined | ×1.75 | 28 |
| Enriched | ×2.50 | 40 |
| Superior | ×3.50 | 56 |

Fuel-scaled results round **up** to the nearest whole block wherever the multiplier doesn't land cleanly (not triggered at this particular base value, but the rule stands for future retuning).

**Fuel type lock:** the loaded fuel type/grade can only be changed while the Grapple is **empty** — one of several planned "plan-ahead" mechanics meant to reward preparation rather than penalize on-the-fly flexibility (more of this family planned elsewhere in the mod).

**Wall/impact handling:** No dedicated collision-damage code needed — vanilla Minecraft doesn't apply damage for horizontal collision with a wall regardless of speed, so pulling into a wall is naturally harmless. The only real risk is a **vertical component** to a pull (grappling up onto a ledge or down into a pit) — a landing after that kind of pull is subject to normal fall-damage rules, same as jumping off something, and interacts with whatever fall-damage mitigation the player's suit/add-ons already provide rather than needing a bespoke exemption.

**Mob interaction:** The Grapple can target mobs as anchor points, not just blocks. Resolution is based on **mass**, not combat power or the Net Gun's threat-based tier system (a deliberately different axis from the Net Gun) — deferred to Code for exact values/thresholds:
- **Mob mass < player mass:** the mob is pulled to the player.
- **Mob mass > player mass:** the player is pulled to the mob.
- **Override — Grapple Anchor add-on:** with the suit's **Grapple Anchor** add-on equipped (see `dermicraft-suit-notes.md`), **crouching before firing** at a mob **braces and holds it in place** — the mob is immobilized while the brace is active, rather than either party being pulled. Costs **extra fuel while active**. (Pulling a *heavier* mob to the player, rather than just holding it, is flagged as a possible future companion add-on — not part of Grapple Anchor itself.)

**PvP grappling — via Grapple Anchor only.** The base Grapple is mob-only. The **Grapple Anchor** add-on additionally enables grappling **other players** and pulling them in **harmlessly** (no crouch required for player targets). This is the confirmed mechanism for PvP grappling; without the add-on it remains disallowed.

**Primary combat use case:** Closing distance on targets that are otherwise hard to reach, especially **flying bosses** (the Ender Dragon being the clearest example) — since a boss's mass will always exceed the player's, grappling a boss pulls the player to it, letting a mobile, melee-focused loadout force an engagement mid-flight rather than waiting for the target to come down.

**Design goal note:** Per the mod's stated endgame-equipment philosophy — the player should feel **godlike by the end, but it has to be earned** — tier upgrades below are budgeted as **capability leaps** (new verbs, new thresholds), while fuel grade owns all **smooth numeric scaling**. Pull Strength is the one exception, a straightforward per-tier magnitude stat (precedent: Assault's Toughness/knockback resistance).

**Tier Ladder — 6 tiers total,** mirroring the vanilla 6-rung armor-material scale (Leather→Chainmail→Iron→Gold→Diamond→Netherite) the suit tier system is already modeled on:

| Tier | Unlock |
|---|---|
| 1 | Chambered Fuel Cell — introduced |
| 2 | Chambered Fuel Cell — improved |
| 3 | Chambered Fuel Cell — improved again |
| 4 | Chain Firing (requires Chambered Fuel Cell) |
| 5 | Harder Pull |
| 6 | Momentum Retention |

**1–3. Chambered Fuel Cell:** reload loads a whole multi-chamber **cell** instead of a single charge. Loading the cell costs fuel once, proportionally cheaper than paying full price per shot the old way — savings come from buying in bulk, not a free shot. Each fire burns one chamber; only once the cell is fully spent does the player need to reload. Chamber count doubles each tier; per-chamber cost drops each tier:

| Tier | Chambers | Cost/chamber | Cell total | Cost/shot |
|---|---|---|---|---|
| 0 (no cell) | 1 | — | 100 mB | 100 |
| 1 | 2 | 75 mB | 150 mB | 75 |
| 2 | 4 | 50 mB | 200 mB | 50 |
| 3 | 8 | 25 mB | 200 mB | 25 |

(Tier 2→3 doubling chamber count at the same total cell price is a happy accident of these particular numbers, not a deliberate rule — kept as-is since it lands as a satisfying jump right before Chain Firing unlocks.)

**4. Chain Firing** (requires Chambered Fuel Cell): removes the manual "advance to next chamber" step entirely — fire, release, fire again almost instantly, gated only by remaining fuel. Reload cost is identical whether triggered manually or automatically. Since chambers are pre-paid at load time, if the player's live fuel source runs dry mid-use, any chambers still loaded keep firing regardless — an emergency reserve that only exists because Chambered Fuel Cell front-loads the cost.

**5. Harder Pull:** per-tier stat increase to pull force in both directions of the existing mass rule (mob-to-player and player-to-mob) — the mass-vs-player threshold logic itself is unchanged, only the strength of the pull scales.

**6. Momentum Retention:** grants a **manual Release** action (its own dedicated keybind, distinct from Fire and Lock) usable **while actively mid-pull only** — cuts the pull short on command and launches the player off with retained velocity, instead of riding it out to natural arrival. Retention is what makes this worth using: without it, cutting a pull short would just dump speed like the normal stop does. Placed last since it's the tier most likely to catch a player off guard the first time it fires.

**Per-Tier Stat Growth:** applies the suit tier system's existing layering rule (base → tier bump → new base → fuel multiplier applies on top of *that*) to Range, Projectile Speed, Pull Speed, and Pull Strength. Deliberately modest and accelerating — the tier's headline reward is always the capability above, this is a bonus layered on top, not the main draw.

Range / Projectile Speed / Pull Speed (cumulative % bonus to the Tier-0 base):

| Tier | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|
| Bonus | +5% | +11% | +18% | +27% | +38% | +51% |

Pull Strength (bigger curve — no fuel multiplier of its own to lean on):

| Tier | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|
| Bonus | +10% | +22% | +36% | +54% | +76% | +102% |

All stat-growth values provisional and easy to retune without breaking the underlying accelerating-curve pattern (same framing used for Slurry's own progression numbers).

**Tether States & Gadget Bench Options:** independent of the tier ladder above — available from **Tier 0**.
- **Hanging:** once a pull ends, the tether either auto-detaches or leaves the player suspended at the anchor point ("Hanging"), governed by a **Gadget Bench** toggle:
  - **Auto Release on Stop** (Yes/No, default **Yes**): Yes = tether detaches the instant the pull ends (today's baseline behavior, no Hanging state). No = the player remains tethered and Hanging at the anchor after arrival, enabling Controlled Drop below.
- **Controlled Drop:** while Hanging, holding crouch slowly lowers the player down the tether. Available at **Tier 0** — not treated as a meaningful power upgrade, just an early utility. **Fuel-free**; fuel only ever pays for firing/reload, never for holding a state.
- **Controlled Drop distance:** capped at the Grapple's **current effective Range** (same tier+fuel-scaled number that governs firing distance) — one continuous tether, same maximum length in either direction, no separate stat needed. Descent speed itself still needs a value (own flat rate, or a fraction of Pull Speed — open question).
- **Running out of tether before reaching the ground:** the player simply stays Hanging at full extension — same as any other Hanging state, no forced auto-release.
  - **Tier 0 (no cell):** no spare charge — Release commits to the remaining fall under normal fall-damage rules.
  - **Tier 1+ (Chambered Fuel Cell):** the original pull only spent one chamber, so a spare is already loaded — Release then Fire again immediately to grab a new, lower anchor, turning one long fall into a series of controlled drops. An early, manual preview of what Chain Firing (Tier 4) later automates fully.
- **Auto Release on Ground Touch** (Yes/No, only relevant when Auto Release on Stop = No): Yes = touching the ground during a Controlled Drop auto-detaches the tether; No = the player must manually press Release even once grounded.
- **Release, dual context:** the Release action means different things depending on tether state — mid-pull (requires Momentum Retention, retains velocity) vs. Hanging/stationary (available from Tier 0, no velocity to retain, just detaches).
- **Gadget Bench:** working name for wherever this kind of gadget customization/configuration happens — may end up being its own block/structure, or a function folded into the existing Suit Dock. Not yet decided.

**Add-on Frame:** the suit's **Grapple Add-on** (see `dermicraft-suit-notes.md`) is not a separate upgradeable item — it's this same physical Grapple, docked into a mounting frame occupying a suit add-on slot. Single source of truth: tiers, loaded cell, and fuel grade all live on the one item whether it's held or docked, so there is no duplicate upgrade path to maintain. Nesting for full removal is Player → Suit → Add-on (frame) → Grapple. The existing Suit Dock simplifies the outer two layers (suit removal, and swapping the add-on/frame without a full undress); extracting/inserting the Grapple itself into its frame is a more deliberate, specialized-station operation (possibly also the Dock — undecided), and depends on the not-yet-designed **Gadget Upgrade System**.

**Open questions:** Exact mass thresholds/values. (PvP grappling resolved — enabled via the Grapple Anchor add-on; see Mob interaction above.) Exact Controlled Drop descent speed (own flat rate vs. a fraction of Pull Speed). Whether the frame stays in the suit slot (as an empty, non-functional socket) when the Grapple is pulled out, or the slot frees entirely. Exact Grapple↔frame install/remove interaction and station, pending the Gadget Upgrade System (working name: Gadget Bench). Recipes and materials — deliberately deferred until the rest of the Grapple's structure is settled.

### Capture Net Gun

**Status:** Concept stage — core mechanics and tier roster defined.

**What it is:** A non-lethal capture Gadget — fires a net that captures a mob into a spawn-egg-style item rather than killing it. Continues the mod's existing thread of non-lethal mob interaction (alongside Syringe's Squid/Creeper effects and Nose on a Stick's harvest-ready mob logic).

**Fuel:** Slurry-powered, same reload-cost rule as Grappling Hook (charged at reload, fails to reload if unaffordable). Better fuel grade improves range, projectile speed, and reduces projectile drop (same scaling as Grapple).

**Capture mechanic:** Guaranteed on hit, provided the net's tier is equal to or above the target's required tier — no chance roll, no HP/condition dependency. Skill-based (land the shot) rather than RNG-based.

**Net tiers** (material progression: additive, mod-standard escalation — e.g. String → +Copper → further materials upward). Each tier can also capture everything at or below it:
1. **Passive mobs**
2. **Early hostiles** — Creepers, Skeletons, Spiders, Zombies, most Illagers
3. **Basic Nether mobs**
4. **Powerful non-boss mobs** — Blaze, Ghast, Enderman, and any remaining overworld mobs not covered above
5. **Bosses** — Ender Dragon, Wither, Warden

**Boss capture timing:** Guaranteed on hit, but only once the boss is genuinely battle-ready — respects each boss's existing vanilla vulnerable/not-yet-active window rather than allowing an early cheese capture:
- **Ender Dragon:** capturable from the start of the fight (no comparable vulnerable-spawn window exists).
- **Wither:** must wait until after its charge-up/summon invulnerability phase ends.
- **Warden:** must wait until it has finished emerging from the ground.

**On capture:** The mob becomes a spawn-egg-style item, **automatically added to the player's inventory** (drops nearby as overflow only if the inventory is full, matching standard item-give behavior).

**Release:** Right-click the ground with the captured-mob item to release it, following the same interaction pattern as vanilla spawn eggs.

**Primary intended uses:** Relocating a mob (including a boss) rather than killing it, or — see future idea below — feeding it into a controllable mob farm.

**Future idea (not yet designed):** A Dermicraft-native mob spawner block that a captured mob item can be fed into, turning it into a live, controllable spawn source. Control scheme would follow the standard redstone-toggle behavior already established by vanilla spawners and other mods' spawner blocks, rather than inventing new redstone logic. This would extend the Net Gun from a one-off capture tool into the front end of a real mob-farming/automation system — including, notably, boss-tier mob farms.

**Open questions:** Exact net-tier material progression beyond String/Copper. Exact fuel-cost values per reload.

## Weapons

Housed under Gadgets as their own subsection — melee/combat-focused Gadgets sharing the Slurry-fuel-heals-durability pattern.

### Sunder

**Status:** Concept stage — name, core use mechanic, and upgrade model all substantially fleshed out (2026-08-01). Numbers not yet set.

**What it is:** A melee weapon in the Weapons subsection of Gadgets, inspired by Warhammer 40k's chainsword but built organic to fit Dermicraft's aesthetic. OT-crafted; recipe explicitly flagged as easy to iterate/change. **Named "Sunder" (2026-08-01, finalized)** — plain name, not a backronym (Weapons aren't required to follow the acronym convention `dermicraft-gadget-notes.md`'s Gadget naming convention section describes; Drill Hammer is the existing precedent for a plain Weapons name). Considered and rejected: "Ripper" (too generic, overused across games, reads more chaotic-messy than a controlled cutting machine) and "Render" (name collision — already used elsewhere in the mod in a different context).

**Core use mechanic — rev and dig-in (2026-08-01, base tier):** Replaces the old "one-hits 1-block-wide trees" instant behavior, which was judged too strong as a base-tier default.
- Hold right-click to **rev up**. Revving **requires fuel to start** — cannot be initiated on an empty tank.
- Once revved, landing a hit **locks the target in place relative to the player** and begins a **pulsed dig-in**: each pulse deals damage at the start of the pulse, then the pulse length acts as a cooldown before the next one fires. (Chosen over end-of-attack damage calculation specifically because per-pulse damage is easy to track incrementally, mirrors the mod's existing progress-tracking patterns.)
- **Resource draw:** fuel drains per pulse as damage is dealt. If fuel runs out mid-dig-in, the sequence **continues on hunger** instead — rev-up itself is fuel-gated, but sustaining an already-started dig-in isn't.
- **Mobs:** a hard time cap applies — if the target isn't dead when the cap is reached, the chainsword auto-releases (also releases early if fuel-then-hunger both run dry). This is deliberate anti-grind: prevents parking on a tanky mob indefinitely.
- **Trees:** no separate time cap — the fuel/hunger budget itself is the limiter (see Tree Felling below). Bigger trees cost more resource, not more difficulty.
- **Interruption:** canceled by **knockback dealt to the player** (not knockback dealt to the target, not damage taken by the player). Damage already dealt via completed pulses **sticks** regardless of how the sequence ends (kill, timeout, or interruption).

**Tree Felling (2026-08-01, built 2026-08-03):** Standard tree-capitator-style contiguous log detection (flood-fill from the hit log, connected logs only), adapted to the mod's own style — **only affects logs**, leaves are untouched. Reuses `SAWING` wholesale rather than a separate state — `SunderModeData` stores a tree-felling origin `BlockPos` alongside (mutually exclusive with) the mob-targeting UUID, so the existing animation controllers (which key off `State.SAWING` directly) don't need to know tree-felling exists at all. Auto-targeting checks mobs first, then a log at the same reach, so a mob standing in front of a tree isn't shadowed by the trunk.
- **Detection range widened** versus the vanilla-mod-convention 6-face adjacency: uses **26-neighbor (diagonal-inclusive) adjacency**, plus a **single-air-gap hop** (checks one block past an immediate air neighbor for a log), specifically to catch detached/diagonal branches that don't directly touch the main trunk.
- **Safety cap on the flood-fill's scanned-block count: 64 logs** (first pass, untuned) — a performance/technical guard, not a gameplay number. Without one, touching canopies between adjacent trees could chain the fill across an entire forest in one swing, which is both a bad felling result and a tick-cost risk. Once the cap is hit, whatever's been found so far is treated as "the whole tree." Recomputed fresh every tick rather than cached in the persisted state (bounded by the cap, so cheap enough) -- a log disappearing mid-fell (mined, exploded, reload) just shrinks the next flood-fill's result instead of needing a separate invalidation path.
- **Per-log cost = 15 (first pass, untuned), same fixed per-pulse damage the combat side uses** — 5 pulses per log, derived automatically (`TREE_PULSES_PER_LOG`) rather than a second independent number, so the two can't drift out of sync if either gets retuned. No separate tree-specific damage stat; tree size raises resource cost, not time-to-kill difficulty (no time cap on trees, per the core mechanic above).
- **Superseded (2026-08-03) — logs are consumed as cutting happens, not "the whole tree comes down at once."** Every 5th pulse finishes cutting through the current **top-most remaining log** (per the flood-fill's own top-down sort) — that log breaks immediately and drops **at the player's feet** right then, real-time feedback matching other tree-feller mods' own feel, rather than a silent damage counter with nothing to show until the very end. This also resolved the "partial-completion: persist or reset?" open question for free — there's no separate partial-harvest case to handle at all, since whatever's already been cut is already in the player's hands the moment SAWING stops for any reason (release, knockback interruption, resources exhausted, or the origin log itself gone, which a now-empty flood-fill covers automatically). The shared exit (`SunderItem#exitFellingState`) is just the state-transition tail, nothing to pay out.
- **Requires a mounted chain (2026-08-03, fixed a gap)** — unlike a mob (a broken/missing chain still swings as "a much weaker plain sword," per Combat above), there's no bare-bar equivalent for cutting wood, so felling can't even start without one, and re-checks every tick so a chain breaking mid-cut (wear, same as combat) stops the cut rather than continuing on a bare bar.
- **Hunger fallback: same treatment as mob combat, not a special case.** Tree felling reuses the exact same fuel-then-hunger `payForPulse` mob SAWING already uses -- no separate tree-only payment path. A future buyable **Safety** upgrade (points system, not built yet) will let a player *block* the hunger fallback entirely (protects hunger, at the cost of felling/SAWING just stopping when fuel runs dry instead of continuing on hunger) -- base tier has no way to opt out yet, so this applies uniformly to mobs and trees alike once it exists.

**Combat (2026-08-02, reworked — standard hits and dig-in fully decoupled):** Early planning assumed Sunder would rev for every hit; that's no longer the design.
- **Standard right-click attacks** function as a plain sword — **no fuel or hunger cost at all**, no revving triggered or required. Carries its own independent chances at both Bleed and decapitation, rather than dig-in's guarantees. This fully retires the earlier "unfueled mode drains hunger per swing" concept, and with it the Safety/"mundane sword" upgrade pair that existed to soften that drain (see Chain durability below for their replacement).
  - **The two chances are not equal, and are not the same kind of number** (clarified 2026-08-03): standard-hit **Bleed** was specified as a "high chance"; standard-hit **decapitation** as roughly **20–25%**. Setting them to one shared value would collapse a real distinction.
  - **The chain carries the entire chance, not a bonus on top of a Sunder-side base** (2026-08-03). Sunder's own base for both is **0** — the whole probability lives in the chain material's `ChainProperties`. This is deliberate: it makes the "broken chain = flat 0%" rule below fall out automatically rather than needing a special-case override anywhere in the combat path.
- **Dig-in (revved) attacks** keep the original guarantees — decapitation guaranteed on a revved kill; Bleed guaranteed if a revved hit runs 1 second or longer before the attack ends (kill, timeout, and interruption all count). But these are now a **boolean gate on chain presence**, not a probability at all: an intact chain always guarantees them, a broken chain can't dig-in in the first place (nothing to rev).
- **Broken-chain state:** standard-hit Bleed/decap drop to a flat **0%**, plus a large standard-hit damage reduction (exact amount TBD) — Sunder still functions as a much weaker plain sword with the chain gone, it doesn't become fully unusable.
- Decapitation drops the head as an item alongside the mob's standard drops — **mechanical**, not cosmetic.

**Chain stats — storage and the Iron baseline (2026-08-03, built):**
- **Stored in a data map, not per-material Java classes** — `ModDataMaps.SUNDER_CHAIN_PROPERTIES`, a `DataMapType<Item, ChainProperties>` keyed on the chain item, same shape as the existing `BIOFUELS`/`EDIBLE_FLUID` fluid data maps. Adding a material later is a datagen entry, not a new class.
- **`ChainProperties` fields:** damage multiplier (unified across standard-hit and dig-in for now), additive Bleed chance, additive decapitation chance (both standard-hits-only — dig-in is a boolean gate on chain presence and never consults these), durability pool, tint, and an **optional** status effect (reserved for special-case materials, not a slot every material fills).
- **Tint lives here too** — folding it into the same entry keeps "what this material is" in one place instead of a separate colour registration list that has to be kept in sync with the material roster. Stored via vanilla's `TextColor.CODEC` (hex string in JSON), read as an int the same way `registerBucketTint` reads a fluid's tint.
- **Iron is the explicit baseline** every other chain is defined *relative to* — a later material reads as "+15% damage over iron" rather than needing its own from-scratch justification. First-pass values: `1.0` damage multiplier (baseline by definition), `0.50` Bleed, `0.20` decapitation, `250` durability (matching the standalone item, itself anchored to vanilla iron tools), no status effect.
- **Tint is the one field that is NOT part of the baseline idea** — it's independent per chain, each taking the tint of its **equivalent fluid material** rather than deviating from some base colour. Iron's is Ferrous Blend's `#D8D8D8` (adjustable toward a more iron-like shade later). An earlier `#FFFFFF` identity-multiply choice was considered and dropped for this reason.
- **Sunder's own base attack damage tracks the same metal** — pinned to vanilla's Iron Sword (5.0 modifier = 3.0 sword base + Iron's 2.0 tier bonus) rather than stone, for consistency with the Iron chain being the reference material. Attack speed is `-2.6` (1.4 attacks/sec vs a vanilla sword's 1.6). Both are real `ItemAttributeModifiers`, so they show in the tooltip and combine through vanilla's own attribute system — the same `AttributeModifier.Operation` enum the stat layering model below is built on.

**Chain durability (2026-08-02, new):**
- **Only the chain wears from use — not Sunder's own Gadget HP.** Sunder keeps the standard `IGadget` drop-damage mechanic unchanged, identical to every other gadget (this was an open question — dropping the old "whole-sword gauge" idea does **not** mean opting out of `IGadget`; the two systems are fully independent, one drop-damage-based, one combat-wear-based).
- Chain durability is a **separate custom-tracked stat**, not vanilla item durability — vanilla's durability mechanism destroys the whole stack at 0, which would contradict "the chain disappears off the model, Sunder itself survives." Needs its own data component (same shape as `SunderModeData`), not a reuse of `Item.Properties#durability`.
- **UI (not yet built):** a new custom **vertical** bar overlaid on Sunder's hotbar slot, showing chain life — separate from, and in addition to, Sunder's own standard horizontal Gadget-HP durability bar. Real, distinct client-side work: vanilla only exposes one built-in horizontal bar per item slot (`Item#isBarVisible`/`getBarWidth`/`getBarColor`); a second bar needs a direct GUI/HUD rendering hook.
- **Chain durability upgrade (replaces the retired Safety/"mundane sword" pair):** a single ranked, point-purchased category, Unbreaking-enchantment-style — each rank adds a chance to ignore a would-be wear event, rather than a flat wear reduction. Fits the existing "points can buy discrete abilities, not just smooth ranks" precedent (Eater/M.A.W.S.).

**Stat layering model (2026-08-02, new; tier's own behavior reworked 2026-08-04) — five inputs (base, tier, chain, fuel, points) combined via vanilla's own `AttributeModifier.Operation` tiers rather than a bespoke formula, chosen specifically so the combination behavior is a proven, off-the-shelf system rather than something hand-maintained:**
- **`ADD_VALUE`** (flat additive, all sum together first): chain's additive Bleed/decap-chance contribution (standard hits only), points' additive rank component, and wherever tier's own additive component lands if one gets added later (tier is multiplicative-only for now, may gain an additive piece).
- **`ADD_MULTIPLIED_BASE`** (percentage-of-base bonuses that sum with each other, applied once as a group): chain's damage multiplier (unified across standard-hit and dig-in for now, split later only if needed), fuel grade. **Tier is no longer in this bucket** (see below) — the "deliberately summed, not compounding, to avoid runaway growth" reasoning that used to justify grouping tier in here was written specifically to protect against tier's own compounding, which is now the intended behavior; chain/fuel stay summed against each other exactly as before, that part is unchanged.
- **`ADD_MULTIPLIED_TOTAL`** (a final multiply, applied last, a deliberately compounding layer): points' multiplicative component — self-limiting via the existing escalating point-cost curve (see the shared Gadget upgrade points section above), and guaranteed to apply last as a structural property of vanilla's own calculation order rather than something to maintain by hand.
- **Tier compounds by design (2026-08-04, reworked from "summed, not compounding"): a tier-up is a discrete event, not a live modifier recalculated from a stored tier level.** At the moment Sunder tiers up, its *current effective stats* (base plus every tier bonus already baked in from prior tier-ups) combine with the new tier's own modifier, and the result becomes the new stored base going forward — "the result of the tier-up process is a new device," not a bigger number layered on top of the original unchanged base. Concretely, this means tier's contribution can't be realized the same way chain/fuel are (a live `AttributeModifier` recomputed fresh each time from "what's currently equipped") — it needs Sunder's own persisted base-stat value to actually change at each tier-up event, not just a stored tier-level int fed through a formula on demand. Chain/fuel/points are unaffected by this and keep working exactly as already built (computed live, every time, off whatever's currently equipped/spent).
  - **Runaway growth is a tuning problem, not an architecture problem** (2026-08-04, resolved) — no structural anti-compounding rule needed; the ladder is a small, finite ranked sequence (not an open-ended stacking system like enchantments or chain materials), so per-tier magnitudes just need to be picked with the compounding in mind rather than the system needing to prevent it outright.
- Mirrors the suit's already-confirmed layering language ("base → tier adjustments → the result becomes the new base → point-spend calculated after that") — tier's output becomes the next layer's starting point, same shape, just with chain and fuel now inserted into the pipeline too. This is now consistent with tier's own compounding above rather than in tension with it.

**Upgrade model (2026-08-02, updated) — four axes, following the mod's established gadget/suit convention rather than a bespoke system:**
- **Chain material (physical part swap):** sets the base stat block for the identity layer everything else scales from — damage multiplier, additive Bleed/decapitation chance (standard hits only), and the chain's own durability pool. Mirrors Drill Hammer's material-head pattern. **Status effects per material are a maybe, not a commitment** — reserved for a small number of special-case materials as a signature trait, not a slot every material fills (avoids multiplying the balancing surface across every material for a fifth, differently-shaped stat).
- **Tier ladder (Growth Chamber) — reworked 2026-08-04, instant one-hit tree felling retired as a tier unlock (no replacement capability-leap threshold committed yet).** No longer "capability leaps only, no smooth scaling" — that framing was built specifically to justify the tree-felling threshold, which is gone. Each tier-up now grants, together: **(1)** a stat bump contributing to `ADD_MULTIPLIED_BASE` in the stat layering model above (same mechanism as before, just no longer the axis's only job), **(2)** points into the shared Workbench pool (mirrors "a fixed number of points granted per tier-up," the general shared-mechanic rule above — tiering up is what grows the pool, not a separate system), and **(3)** access to progressively better/more exotic chain materials — a gating mechanism on `ChainProperties` (not yet built: no `requiredTier`-equivalent field exists on it today), the reason Netherite/the boss Essence fluids were deliberately left off the chain roster when Copper/Gold/Diamond/Bone were built. The five materials that already exist (Iron/Copper/Gold/Diamond/Bone) are all base-tier-accessible — this gating is what a *future* higher-tier material roster hangs off of, not retroactive restriction on what's already built and tested.
  - **Open:** total tier count, the stat-bump curve per tier, how many points per tier-up, and which specific tier each future exotic material (Netherite, the boss Essences) requires. None of this is implementable yet regardless -- Growth Chamber has no block/BE/menu, and the Workbench's own point-spend menu (Duty 7) doesn't exist either, so nothing here can actually be reached by a player until both exist.
- **Points (Workbench, shared gadget point-spend mechanic — see that section above):** everything else. Smooth per-rank stats (pulse damage multiplier, pulse cooldown, fuel-drain efficiency, rev-up speed, mob time cap length, flood-fill scan cap, the chain durability upgrade above) **and** discrete ability purchases, including **auto-fueling** (base tier requires manually re-triggering each pour at the Scrench's maintenance screen — see its own entry below — the upgrade unlocks continuous auto-drain). The old mutually-exclusive Safety/"mundane sword" pair is **retired** — see Combat and Chain durability above for why (standard hits no longer have a resource-drain problem to soften).
- **Fuel grade:** existing smooth-scaling axis (mirrors the Main Line's speed scaling) — better fuel, better dig-in performance. Contributes to `ADD_MULTIPLIED_BASE` in the stat layering model above. **The old "fuel heals Sunder, no durability damage while fueled" rule is superseded**, not carried forward — Sunder's Gadget HP is now purely drop-damage-based (standard `IGadget`, untouched by combat), and chain wear is a separate stat fuel isn't stated to affect.

**Model & animation (2026-08-01) — GeckoLib, following the `DrinkerItem`/`SippingItem` pattern (see [[project_geckolib_pipeline]]):**
- **Idle-to-running transition is instant, not a gradual spin-up.** Ruled out animating the chain physically sliding around the bar (down one side, around the tip, back the other side) — GeckoLib's bone-transform animation model has no built-in way to slide a texture/mesh along an arbitrary looping path, and faking it with real geometry would be disproportionate effort for the visual payoff. A blurred/streaked "running" chain texture (or similar still-vs-motion treatment) sells the effect without needing true motion, the same way real chainsaws/propellers are usually depicted at speed.
- **Chain swap technique (decided over two rejected alternatives):** one combined `.geo.json` containing the handle/bar/guard geometry **once**, plus both chain variants (still-chain and running-chain) as sibling bone groups in the **same file/skeleton**. The revved state toggles which chain bone is visible; the non-active one is hidden. This was chosen specifically because the chain bones inherit the bar's transform automatically (parented in the same skeleton), so they can't drift out of alignment the way independently-authored separate files could.
  - **Rejected: three separate whole-item/part models** (base sword + a standalone still-chain model + a standalone running-chain model, composited at render time). Would require custom multi-model composite rendering (GeckoLib's default flow is one animatable → one model resource → one render pass, not built for stitching multiple independently-loaded models together) and risks alignment drift between the base model and each separately-authored chain model whenever either is edited.
  - **Rejected: two full whole-item models** (idle-chainsword.geo.json / revved-chainsword.geo.json, swapped via `getModelResource(T)`), each duplicating the unchanged handle/bar/guard geometry. Simpler code (one if-check) but duplicates maintenance burden — any future edit to the unchanged parts has to be made twice.
- **Aesthetic direction (2026-08-01, resolved):** geometry stays mostly mechanical/utilitarian across the whole body (grip, trigger, handguard, arm brace are all plain blocky shapes, no organic sculpting) — the "living" quality is sold entirely through **texture work and animated movement** on select parts, not through the geometry itself deviating into organic shapes. This decision governed the engine block's own shape call below.
- **Engine block shape (2026-08-01, resolved): stretched, not cubed** — a stretched cube (not a perfect cube), same primitive-shape philosophy as the rest of the body (Minecraft/Blockbench-blocky, per the aesthetic direction above — the "everything is stretched cubes" reality of the engine ruled out the earlier organic-bulge idea, which is superseded by the texture/animation-carries-life decision above). Reasoning that still applies even fully mechanical: (1) a housing elongated parallel to the bar reads as "chainsaw" at a glance, where a pure cube reads as "a block bolted onto a handle"; (2) the grip is already a stretched cube, so giving the engine distinct proportions (not identical dimensions) avoids the whole weapon reading as a row of repeated boxes.
- **"Alive in movement" part(s) (2026-08-01, resolved): the idle animation itself.** Sunder's idle pose already has a subtle vibration built in — that existing idle animation *is* the answer to which part carries the living-movement quality, not a separate treatment layered on afterward. Dig-in **may** get additional chain vibration on top of this (still tentative, not committed).
- **Activation/deactivation sequence (2026-08-01, decided; arm delay tuned 2026-08-03):** click to activate → **5-tick delay** (originally 10, shortened after in-game testing) → chain model swaps from idle to running (instant swap per the transition decision above) → **rev-up animation** plays, showing the `adjuster` bone flexing while the `bar`, rear guard, and chain (the `blade`-bone assembly, which `bar` and `chains` both hang off as siblings — animating `blade`'s own position pushes both outward together without needing to hand-sync two separate bone animations) extend outward slightly. The **front guard stays stationary** — only the rear guard, which meets the moving assembly, extends with it. Release (**voluntary or forced by interruption — same sequence either way**, no separate harsher cut for a knockback-forced release) → **10-tick delay** (unchanged) → the **same rev-up animation plays in reverse** for rev-down (one authored clip, reused both directions, not two separate animations) → once the reverse animation ends, chain model swaps back to running→idle.
- **Chain material's visual representation (2026-08-01, resolved): tint-based, not per-material textures.** One grayscale still-chain texture and one grayscale running-chain texture total; each chain material is a runtime color tint over those same two files, reusing the mod's existing bucket-tint mechanism (`registerBucketTint` in `ModClientEvents`) rather than inventing a new one. Deliberately the cheaper/simpler option given the model's "somewhat simple" scope — fully distinct per-material textures (different link shapes, not just color) was considered and explicitly deferred, not ruled out, as a later upgrade path for a specific material if one ever needs to look structurally different rather than just differently colored.
- **Texture layout — SUPERSEDED (2026-08-01, replaced 2026-08-03).** The original plan was a **separate chain render layer** (mirroring `SippingGlowLayer`) holding the chain textures on their own canvas, on the reasoning that GeckoLib's base render pass binds one texture per model (`getTextureResource(T)`) so a separately-tinted chain needs its own layer. That constraint is real, but the conclusion was wrong — see below.
- **Texture layout — CURRENT (2026-08-03, built): everything on one canvas, tinted per-bone.** The chain stays painted into the same body texture; no separate file, no UV re-pointing, no extra render layer. `SunderItemRenderer` overrides `GeoRenderer#renderRecursively` and substitutes the material tint into the **per-bone `colour` parameter** when it reaches the `chains` bone. GeckoLib threads that colour down through `renderChildBones`, so one check covers both variants and every tooth; the body bones never get reassigned and render untinted from the same texture. The colour is a packed ARGB int (`Color.argbInt()`), and it's a plain vertex-colour multiply against the sampled texel.
  - **Hard requirement:** the chain pixels must stay **grayscale**. The tint multiplies with whatever the texture holds, so real hue painted into the chain would compound rather than be replaced — and *that* is the case that would force pulling the chain into its own texture after all.
  - Why this beats the superseded plan: a second render layer would have worked too, but it's strictly more machinery (extra texture file, re-pointed UVs, a layer class) for the same result. Per-bone vertex colour is the cheaper tool for "recolour part of one model."
- **Model progress (2026-08-01):** `sunder.geo.json` exported and in progress (handle, trigger, handguard, arm brace built; chain bone-visibility swap — Blockbench editor toggles only so far, not yet wired to runtime state — confirmed working for preview/export). Cube-count check against shipped models: 13 bones / 25 cubes so far (Sipping: 14/21, Drinker: 22/31, Eater: 23/36) — well within the range of what's already running fine in-game; held items aren't rendered at the scale (dozens of instances per frame) where cube count becomes a real performance concern, unlike entity models. The running-chain variant is expected to add only ~5 more cubes (cheaper than the still variant, consistent with the blurred/simplified "running" look rather than modeled individual teeth), landing the finished model around 30 cubes total — still inside Drinker/Eater's range.
- **No custom swing animation needed.** The attack swing (arm/hand/item moving through the attack arc) is applied automatically by vanilla's `ItemInHandRenderer` before the custom GeoItemRenderer draws the item mesh — confirmed by how `DrinkerItem` has to deliberately return `CONSUME` instead of `SUCCESS` from its use handler specifically to *suppress* the vanilla swing (it fights Drinker's motionless-hold siphon animation), which only makes sense if the swing happens automatically by default. Sunder wants the normal swing, so no extra authoring needed for it — Blockbench work only needs to cover idle pose, held-in-hand transform (if non-default), and the state-driven bones (chain visibility, rev/power-down). **Not yet visually verified in-game** (no model exists yet) — worth a sanity check once a placeholder model exists, since this is inferred from `DrinkerItem`'s code rather than observed directly.

**Fabrication recipe (built 2026-08-04, via the `gadget_fabricating` Workbench recipe type — see `dermicraft-gear-stations-notes.md`'s Workbench Duty 6/7):** Tier 1, 90 seconds. **Items:** 2× Chassis, 1× Fuel Bladder. **Fluids:** 500mB Primitive Catalyst, 500mB Protein Blend, 2500mB Ferrous Blend. Deliberately no Proto Brain, unlike Sipping/Drinker/Eater — Sunder isn't a smart device; the auto-swing/auto-targeting logic exists to work around vanilla blocking the attack key while revved, not because the weapon itself is making decisions. Fuel Bladder specifically (not the plain Bladder every other gadget uses), since Sunder's own tank is explicitly a fuel tank. Protein Blend held at the same 500mB baseline as everything else ("there isn't much meat to it" vs. a full sensor/buffer gadget); Ferrous Blend bumped hard to 2500mB — by far the largest single fluid line in the current roster — with no separate Iron Ingots, since that fluid is what actually builds the bar/housing/guards. **Comes out chainless** — mounted afterward via Scrench, matching every other path that produces a Sunder, since the generic result type can't carry nested chain data.

**Open questions:** Rev-up time, pulse damage, pulse cooldown length, mob time cap length, fuel drain rate per pulse. Exact standard-hit Bleed/decapitation chance within the ~20-25% range. Broken-chain standard-hit damage reduction amount. Whether the flood-fill's diagonal/gap-hop detection has any material/tier gating or is available from base tier. Whether dig-in gets additional chain vibration on top of the idle animation's existing movement, or reuses it as-is (see Model & animation above — tentative, not committed). Exact tick timing beyond the two confirmed 10-tick delays (rev-up animation length, reverse/rev-down animation length). Chain durability's actual data-component shape and the vertical hotbar-bar rendering hook (both designed, not yet built). Which specific chain materials (if any) get a special-case status effect, and what those effects are. Exact per-tier/chain/fuel percentages feeding the `ADD_MULTIPLIED_BASE` sum. **A running-chain sound loop while revved (2026-08-04, raised)** — nothing too loud/intrusive, but silent doesn't feel right; no volume/sound-file/looping-mechanism decisions made yet.

### Scrench

**Status:** Concept stage (2026-08-02) — full mechanic decided, not yet built. Named after the real chainsaw tool's common nickname (combo screwdriver/wrench, sold with real chainsaws for field chain/bar maintenance).

**What it is:** A new, simple, cheap basic tool (same tier as Forceps/Scalpel) enabling **field** physical-part swaps (chain material for Sunder, head material for Drill Hammer — see each's own "exchangeable physical parts" entry) as an alternative to the Workbench, **not a replacement for it** — the Workbench stays a fully valid, safer/costless way to do the same swap. Shared by both Weapons rather than each getting its own tool, since both are already unified under the same "physical part swap" concept in their upgrade models.

**Why a field option at all:** real chainsaws ship with exactly this tool for exactly this reason — the self-balancing property is what makes it work as a mechanic rather than a shortcut: the Scrench only *enables* a swap, it never conjures the replacement part, so the player still has to be carrying whatever they want to switch to.

**Trigger:** bound to the **Scrench's own right-click**, which checks the *other* hand for a compatible weapon (Sunder or Drill Hammer) — reuses `SippingItem`'s existing cross-hand-pairing check (main hand vs. off hand, whichever pairing matches) rather than a new lookup. Deliberately not bound to the weapon's own right-click, since Sunder's is already claimed by rev-up.

**GUI flow:**
- Right-clicking with the pairing correct opens a swap GUI ("by hand" framing — the player is physically working the part loose, not just menu-browsing).
- **Interrupted by damage:** taking damage while the GUI is open force-closes it immediately.
- **Movement penalty on a completed swap only:** a **temporary movement-speed slowdown** (a raw attribute modifier, not vanilla's Slowness potion effect — deliberately no status icon/particles, since the intent is "off-balance from wrenching while walking," not a magic debuff) applies after a chain is successfully seated and the GUI closes. No penalty at all if the GUI closes without a completed placement.
- **Cost:** the Scrench itself wears down with use — plain vanilla item durability (no special component needed; unlike chain durability, a spent Scrench breaking/being consumed at 0 is completely normal, expected tool behavior). **1 point of wear per completed chain swap only** (2026-08-02) — a refuel-only session (see the maintenance-screen contents below) costs nothing, since the wrench's identity is mechanical part-swapping, not fuel handling. **Generous max durability**, deliberately — this is kept as a long-tail flavor mechanic (the tool is eventually a recurring resource, not a one-time craft) rather than a real balancing lever; the GUI's damage-interrupt risk and the movement penalty are what actually carry the field-vs-Workbench tradeoff. Cheap to implement either way (one `hurtAndBreak(1)` call on a successful swap), which is part of why it was worth keeping even though it's minor.

**GUI contents (2026-08-02, decided) — a general Sunder maintenance screen, not swap-only:**
- Standard player inventory, plus:
- **Chain slot:** one slot, pre-filled with the currently-mounted chain the moment the GUI opens (see the dual-representation section below for the mechanics of that).
- **Fuel gauge + fill-slot pairing:** shows Sunder's current fuel level; drop a filled fluid container in to top it off. **Transfer is immediate** on contact, same as every other fluid-handling interaction in the mod — no "confirm on close" step the way the chain slot has.
- **No movement penalty for refueling** — only a completed chain swap triggers the slowdown; topping off fuel in the same screen is free of it, regardless of whether a chain swap also happens in the same session.
- **Auto-fueling is a buyable upgrade, not a starting feature** (2026-08-02) — base tier requires the player to manually re-trigger each pour; the upgrade (fits the existing Points axis, same "points buy discrete abilities" precedent as Eater/M.A.W.S. and the chain-durability-wear-chance upgrade) unlocks continuous auto-drain from a held stack of containers.

**GUI layout (2026-08-02, decided) — reuses existing shared screen assets, no new art needed for now:**
- **Chain slot on the left, fuel tank+slot on the right** — fuel-on-the-right matches the mod's existing convention for "most things" (per `MetastasizerScreen`'s own REAGENT/PATTERN/OUTPUT/FUEL left-to-right ordering, fuel last).
- **Reuses the shared `screen_background.png`** and the existing `AbstractModScreen` base (background + player inventory backdrop), same as every machine screen — no dedicated Scrench background for now, though a custom one is an open door later if wanted.
- **Fuel:** the existing `tank_and_slot.png` (18×66) + `FluidTankRenderer` pairing, the same combined gauge-plus-fill-slot asset `MetastasizerScreen`'s REAGENT/FUEL columns already use — no new texture needed.
- **Chain:** a single plain `item_slot.png` (18×18), same asset used for Metastasizer's pattern/output slots. **Gets vanilla's own durability bar for free** once a chain is pulled into the slot — because it's a real vanilla-durability `ItemStack` at that point (per the dual-representation design above), not something this screen needs custom rendering for. The still-open custom vertical hotbar-bar idea from earlier is a *different* use case — glancing at chain life while just holding Sunder normally, without this GUI open — not something this screen's slot needs.

**Failure modes (2026-08-02, decided) — governs every way the GUI can close without a clean completed swap:**
- **GUI closes with the mount slot empty** (backed out, interrupted by damage, anything short of a completed placement): the weapon ends up with **no part mounted, and no movement penalty** — flavor is "thrown back together loosely," no penalty because no careful work actually happened.
- **GUI closes while a part is cursor-held** (mid-drag, not yet placed in any slot): drops on the ground, same as vanilla's own default behavior for any `AbstractContainerMenu` closing with a cursor-held stack (a real crafting-table behavior already built into vanilla menus, not custom code to write) — the weapon is still left part-less, no penalty. Never silently lost, just physically dropped where the player has to walk over and retrieve it.
- The only way to get a mounted part (and the movement penalty that comes with it) is a deliberate, completed placement into the slot before the GUI closes.

**Chain durability's dual representation (2026-08-02, decided — resolves the "must survive reaching 0" constraint cleanly):** the chain is two different things depending on context, not one stat tracked one way everywhere.
- **Standalone (a spare, in inventory or mid-swap in the GUI):** a normal item using **plain vanilla durability** — no constraint against destruction here, a spare wearing out and being consumed at 0 is completely normal tool behavior. Free UI too: vanilla's own per-item durability bar just works for spares, no custom rendering needed.
- **Mounted (installed on Sunder):** tracked via Sunder's own custom data component (per the Chain durability section above) — because this state still needs "reaches 0 without destroying the parent weapon," which vanilla durability can't do. **Implementation refinement (2026-08-02):** built as a real nested `ItemStack` (the mounted chain, damage value and all) rather than a bespoke int stat — reuses `HeldItemData`'s existing shape (already built for I.D.E.P.'s held-item slot), registered separately as `SUNDER_MOUNTED_CHAIN`. This works because vanilla's break-on-zero-durability behavior only fires through code paths that actively call `hurtAndBreak` on a stack sitting in a real slot — a chain nested as inert component data never goes through that pipeline, so "reaches 0 without destroying Sunder" falls out for free. It's still the same "dual representation" idea, just realized as *one* ItemStack shape relocated between a real inventory slot and this nested value, not two genuinely different tracking schemes.
- **The GUI's install/uninstall actions are the conversion points:** installing a spare reads its vanilla durability value, copies it into Sunder's custom component, and consumes the spare item. Opening the GUI on an already-mounted chain does the reverse — pulls Sunder's stored value back out, materializes it as a fresh vanilla-durability item, and clears Sunder's stored value (Sunder is chain-less for as long as the GUI has it pulled out).
- **Chains are reusable until broken.** A chain hitting 0 durability *while mounted* (from actual combat/dig-in wear) breaks and is lost outright — matches the chain-disappears-from-the-model behavior already decided. A chain pulled out via the GUI (not broken, just being swapped) comes back as a real, reusable standalone item with whatever durability it had left.

**Completed-swap costs (2026-08-03, built):** both fire together from `ScrenchMenu#applyCompletedSwapCosts`, only on a completed chain install (matches the Failure modes section above — never on empty-slot or cursor-held closes).
- **Movement penalty:** a new custom `ScrenchOffBalanceEffect` (`effect/ScrenchOffBalanceEffect.java`) — a raw `MOVEMENT_SPEED` attribute modifier via the same `addAttributeModifier` mechanism `Bleed`/`BloodLet` already use, applied as a `MobEffectInstance` with `showParticles`/`showIcon` both `false` so it never shows a status icon or potion swirl. Placeholder values: `-0.2` (20% slower), 100 ticks (5 seconds) — not tuned, see Open questions.
- **Scrench durability wear:** Scrench now actually has durability (`250`, generous per the design above, reusing the same number as the Iron chain's own baseline) — previously `Item.Properties()` had none set, so `hurtAndBreak` would've silently no-opped. 1 point via `hurtAndBreak(1, player, slot)` on a completed swap; checks both hands for the Scrench the same way `stillValid` does, since the menu only tracks `sunderHand`.

**Workbench path (2026-08-04, planned — not yet built):** the "safer/costless" Workbench alternative promised above is now fully designed — see `dermicraft-gear-stations-notes.md` → Workbench → Swap page. Same chain slot + fuel gauge shape as this screen (code-reuse planned, not duplicated), minus the movement penalty and Scrench durability wear, plus an added "fill from the shared pool" button alongside the hand-fill slot.

**Open questions:** Scrench's own crafting recipe/cost. Exact movement-penalty magnitude and duration (placeholder values now in place, see above — untested/untuned). Whether Drill Hammer's head-swap needs any Scrench-flow differences beyond swapping "chain" for "head" (not yet checked against Drill Hammer's own entry below).

### Drill Hammer

**Status:** Concept stage — most core mechanics defined.

**What it is:** A slow, heavy-swinging Weapons-subsection Gadget built for both mining and combat, filed as a Weapon. Same Slurry fuel relationship as Sunder (grade affects performance, heals with fuel, mirrored unfueled fallback options). **Field head-swaps via the Scrench** (see its own entry above), alongside the existing Workbench path.

**Area of effect:** 3×3 face, with the center column striking **2 blocks deep** and the surrounding 8 blocks striking **1 block deep**. This AoE was originally designed for mining and applies identically in combat.

**Targeting logic** (governs whether the AoE hits blocks or mobs):
- **Hit a mob:** AoE only affects mobs — no incidental block damage while fighting.
- **Miss:** the swing connects with the first block in its line, and the AoE applies to blocks on that impact.
- **Exception:** if the player is flying and farther than the hammer's reach from any block, the swing does nothing.
- **PvP:** other players only need to worry about direct hits — no AoE risk to them.
- **Risk:** no dedicated damage mechanic against the player — the danger is purely environmental (e.g. removing the ground under your own feet, breaking something unintended).

**Combat effects:**
- Heavy bonus damage against skeletons and wither skeletons, possibly extra drops, and possibly an additional status effect (not yet decided).
- Heavy bonus durability damage against armor, **except chainmail**, which resists it.

**Heads (material-based, swappable):**
- Heads swap via the same system used for other Gadget customization/upgrades.
- Damage scaling follows a vanilla-inspired material curve (numbers not yet set).
- Vanilla materials (Iron, Diamond, Netherite, etc.) are the confirmed starting point; non-vanilla/exotic head materials not yet decided.

**Open questions:** Exact bonus-damage/status-effect values vs. skeletons. Non-vanilla head materials.

### The Tablet

**Status:** Concept stage, core identity and construction path decided. Capabilities largely undecided — deliberately left open. See `dermicraft-misc-notes.md` for the full narrative/guide-system context this gadget is part of.

**What it is:** An intelligent, **living** guide device — the second half of the mod's two-part in-game guide system (the first being the Notebook, a non-living book-style item; see `dermicraft-misc-notes.md`). Unlike every other Gadget so far, the Tablet is self-authoring: it actively records and writes its own notes as the player works, rather than presenting fixed, pre-written content the way the Notebook does.

**Naming convention note:** "The Tablet" doesn't yet follow the established acronym + suffix pattern (`[ACRONYM].Gadget`/`[ACRONYM].Rig`) used by Drinker/Sipper/Sprayer — open question whether it gets folded into that convention later or stays a deliberate naming exception (a case could be made either way: consistency vs. the Tablet being narratively/mechanically unlike the others).

**Construction — confirmed as OT-crafted, not hand-crafted via Syringe.** Built by the **Operating Theater** from the **Notebook plus other ingredients** — the first confirmed example of an OT crafting recipe, and the first confirmed OT-built item made from **living materials**. The Notebook is consumed in the process, but its information **carries over** into the Tablet rather than being lost — the player's early guide becomes incorporated into the new one, not discarded.

**Living, but constructed differently than other living things:** Every other living thing in the mod so far (Inert Tumor chain, Machines) is built through the hand-crafted tumor-genesis process via Syringe. The Tablet is OT-assembled instead — raising an open question (flagged in `dermicraft-machine-notes.md` Operating Theater entry) about whether the OT runs its own distinct construction logic for living things, or secretly automates the same underlying tumor-chain mechanic.

**Physical connection to the player (lore-confirmed, mechanically undetermined):** The Tablet is described as physically connected to the player — likely not something shown through dedicated graphics, but through other in-game means not yet decided (e.g. possible interaction with the player's own health/state, though this is speculative and unconfirmed). A genuinely new relationship type, distinct from every other item in the mod so far.

**Decluttering via lock/unlock:** The player can manually lock and unlock sections of the Tablet's content, purely for personal organization — no mechanical cost or restriction tied to it, just letting the player control how much information is visible at once.

**First concretely-scoped guide content: fluid hazard info (confirmed).** Which specific hazard tag(s) a fluid carries is shown in tooltips whenever the Notebook or Tablet is anywhere in the player's inventory (possession, not held) — vanilla lava excepted, which needs no gate. The Tablet inherits the Notebook's Stage-2-capped knowledge wholesale on construction, then grows past it only through in-world discovery (many-to-one — one find can unlock several entries). Full detail, including the narrative reasoning, lives in `dermicraft-misc-notes.md` → In-game Guide System.

**Operating cost — tied to capabilities, not yet set.** If the Tablet's role stays purely informational (guide/reference only), it carries **no operating cost** — the core promise of an always-available in-game reference stays protected. Any cost only enters the picture if/when the Tablet gains active (non-informational) capabilities beyond guide functions; those costs would be balanced against whatever those capabilities turn out to be.

**Open questions:** Full capability list beyond the guide function. Exact mechanic for the physical player-connection. Does it fit the acronym naming convention or stay an exception? Operating cost, once capabilities are set. Whether OT-built living items are a one-off (just the Tablet) or a pattern for future OT-crafted Gadgets.

## Template for new entries

```
### [ACRONYM].[Gadget/Rig]

**Status:**

**What it is:**

**Capacity:**

**Modes:** (if applicable)

**Open questions:**
```
