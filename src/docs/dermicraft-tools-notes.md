# Dermicraft Tools Notes

Running log of decided design choices for Scalpel, Suture Kit, Forceps, and Syringe — plus the Inert Tumor block chain that ties them together — and the reasoning behind them. Add a new entry per tool/block as decisions get made.

---

## Overview — why this doc exists

Scalpel, Suture Kit, Forceps, and Syringe were named in the primer from the start, but sat with no detailed function until this session. All four turn out to be tightly bound to a single shared mechanism: the **Inert Tumor block chain**, which is confirmed as the literal origin point for Machines — not just a thematic flavor claim, but the actual crafting mechanism. This also retroactively resolves Syringe's long-standing open question ("likely tied into the forced-evolution process, though that link isn't confirmed yet") — it's now confirmed as exactly that link.

**Thematic pairing worth keeping in mind going forward:** Primitive Catalyst injection is what *births* a Machine (via this tumor system); Evolution Catalyst injection is what later *evolves* it. Same organism, two different life stages, two different catalysts — not designed as a deliberate pun this session, but it holds up cleanly with everything else already on record.

---

## The Inert Tumor block chain

### Inert Tumor

**Status:** Recipe and core mechanic decided. Random-drop weighting flagged as miscalibrated during testing.

**What it is:** The base, living block-form that nearly every Machine is built from (and likely will continue to be, going forward). Not made via a machine — assembled directly, the same "puddle" pattern Primitive Catalyst uses.

**Recipe:** 4× raw meat (a common raw-meat item tag — distinct from the custom `MEAT_FOOD` tag Protein Blend uses, since that one deliberately allows cooked/rotten meat and this one is strictly raw) tossed into a 1×1×1 pool of Crude Slurry. Wait, and it forms into the Inert Tumor.

**Harvesting restriction:** Breaking it the vanilla way **destroys** the block — you never recover the block itself that way. Any items the block was holding drop on the ground. The block is only recoverable with the **Forceps** (or another dedicated tool/Gadget), which hands back the block item; picking a block up still drops its contents on the ground, the same as a break — the Forceps just additionally give you the block. This split — break = contents only, Forceps = contents **plus** the block — applies broadly across the mod's "living" blocks, not just this one; see Forceps below. (One exception so far: the **Craw** keeps its contents *inside* the recovered block item on a Forceps pickup rather than spilling them, so it relocates fully loaded — opt-in via a marker interface; see its entry in `dermicraft-machine-notes.md`.) (Implemented: any item can gain the pickup ability via the `ICollectBlocks` interface; the blocks themselves stay pickup-unaware and only handle their own contents-drop on removal.)

**Cutting (Scalpel) — confirmed in code, differs from the original design description:** Rather than dropping a single random item, cutting an Inert Tumor drops a **randomized batch of 2–4 items** (`getDropCount` rolls 2–4, plus a further 20% chance of +1), and the block becomes a **Marred Tumor**. Each item in the batch is rolled **independently**, uniformly among the three types (Eye, Dense Muscle, Nerve Cluster — each an exact 1-in-3 chance per roll), so a single cut typically yields a small mixed handful rather than one item.

**Drop-weighting issue — resolved/superseded:** The per-item roll in the current code (`InertTumorBlock.java` / `IHarvestableBlock.getDropCount`) is a plain uniform `random.nextInt(3)` — no visible skew toward any one type. Whatever weighting bug was observed during earlier testing either isn't present in this version of the code, or lives somewhere else not yet located — the batch-of-2–4 behavior above is the current, real mechanic to design around.

**Balance confirmed:** The batch-of-2–4-items-per-cut behavior (vs. the originally-described single item) is the **intended balance**, not a bug to revisit.

**Open questions:** None remaining.

**Deferred discussion (not yet resolved) — a Tier 2 Inert Tumor variant?** Raised while working out Tier 2 Machine evolutions (see `dermicraft-machine-notes.md` → Machine Evolution — Smart vs. Dumb / Standard Tier 2 evolution recipe template): should a **Tier 2 Inert Tumor** exist, paired with **Tier 2 implant recipes** for Machines, as a hand-crafted counterpart to the FL's Tier 2 native-construction recipes? Currently, Tier 1→2 Machine evolution only has the forced Evolution Catalyst path (evolving an existing Tier 1 Machine in place) — there's no hand-crafted "build a Tier 2 Machine from scratch via a Tier 2 Inert Tumor" route the way Tier 1 machines have their own implant recipes. Not designed yet — flagged for a future session.

---

### Marred Tumor

**Status:** Core mechanic decided — the shared "open" state between every other stage of the chain.

**What it is:** A block entity, not a separate item — the state any tumor (Inert or a mutation) enters immediately after being cut with the Scalpel. Also the state a **Stitched Tumor** reverts to if its stitches are cut.

**What can be done to it:**
- **Suture items into it** (Suture Kit) — loads item(s) in, as the first half of forcing a new mutation or healing the tumor back to Inert.
- **Stitch it closed** (Suture Kit) — seals it into a **Stitched Tumor**, with or without items loaded inside.
- **Retrieve loaded items by hand** — the player can pull sutured items back out one at a time with an empty hand, but **only while the tumor is in the Marred state**. Once stitched closed, this window closes — retrieval requires cutting the stitches back open first (see Stitched Tumor below). This is by design rather than mechanical necessity — more a "you can't reach through stitches" gameplay-feel choice than a hard rule.

**Recipe gating — confirmed implemented.** A Marred Tumor **refuses to be stitched closed** if its current contents don't match any valid recipe (`MarredTumorBlock.suture()`: checks `recipeHolder == null || !recipeHolder.value().testSuture(sutureStack)`, plays a failure sound, and returns without sealing if the check fails). This is a third confirmed example of the project's established "won't even attempt an invalid action" pattern (alongside Drinker refusing hazardous fluid, and Masticator refusing an overflowing recipe).

**Confirmed recipe — healing:** Raw meat (sutured in) + Crude Slurry (injected via Syringe) → heals the Marred Tumor back into a fresh **Inert Tumor**.

**Open questions:** None remaining.

---

### Stitched Tumor

**Status:** Core mechanic decided.

**What it is:** A Marred Tumor that's been sealed shut via Suture Kit — either empty (the healed-and-closed state) or loaded with sutured items, awaiting injection.

**Reopening (Scalpel):** Cutting the stitches reverts it back to a Marred Tumor — **no item drop, no damage to the player**. This is the deliberate "undo" step: it's the only way to retrieve sutured items before injection, since stitching closed removes that access.

**Injection (Syringe):** Once a fluid is injected (the specific fluid determined by which recipe the sutured items match), the transformation completes — **and this step is irreversible.** There's no undo past this point, unlike the stitch-cutting step before it.

**Confirmed first three mutation recipes (fluid resolved — all three use the same fluid):**
- Eye (sutured) + **Crude Slurry (100 mB)** → **Eye Tumor**
- Dense Muscle (sutured) + **Crude Slurry (100 mB)** → **Muscle Tumor**
- Nerve Cluster (sutured) + **Crude Slurry (100 mB)** → **Nerve Tumor**

All three ended up on the same injection fluid rather than each getting its own distinct fluid as originally speculated — makes sense given Crude Slurry's identity as the mod's general "force accelerated biological growth" fluid (see `dermicraft-slurry-notes.md`), which fits a body-part regrowth/mutation recipe cleanly across all three.

**Open questions:** None remaining on mutation fluids.

---

### Eye Tumor / Muscle Tumor / Nerve Tumor

**Status:** Core mechanic decided.

**What they are:** The first three confirmed mutations of the Inert Tumor, each reached via the Stitched Tumor injection process above.

**Cutting (Scalpel):** Unlike the base Inert Tumor's random 3-way drop, cutting a mutation **reliably drops only its own matching item** (Eye Tumor → Eye, etc.). The block becomes a Marred Tumor afterward, same as cutting an Inert Tumor does — closing a clean loop where one starting item lets a player keep regenerating more of that same item indefinitely.

---

### Tumor-to-Machine pipeline (confirmed mechanic)

**Status:** Core mechanic confirmed; per-machine recipes still being filled in.

**The process:** A Machine is made by placing the correct items (per that Machine's own recipe) into a Marred Tumor, stitching it closed, then injecting it — most likely with **Primitive Catalyst** — via Syringe. This is the actual mechanism behind "Syringe is used to craft Machines by hand," and it confirms Machines aren't just thematically "living to some degree" — they're literally born from this exact process.

**Current per-machine status:**
- **Drooling Cauldron**, **Masticator**, **Effluentcer**, and **Skin Tank** — all have recipes implemented. Skin Tank's blocking ingredient issue is resolved: `skin_tank_implant.json` is 2× Dense Muscle + 2× Nerve Cluster + 1× Beaker (sutured), injected with 100 mB Primitive Catalyst.
- **Metastasizer** — implant recipe now **designed** (1× Beaker + 2× Dense Muscle + 4× Nerve Cluster + 1× Eye, injected with 100 mB Primitive Catalyst, sutured — see `dermicraft-machine-notes.md` Metastasizer), but **not yet implemented in code** (needs the recipe JSON + datagen).
- **All other Machines** in `dermicraft-machine-notes.md` (Drooling Crucible, Flesh Lab, Filling Station, the farming trio) — recipes not yet defined; standing ToDo.

**Open questions:** Whether every Machine's injection fluid is Primitive Catalyst specifically, or whether some Machines might call for a different fluid depending on their own identity (same way the tumor mutations above will each need their own fluid). Exact recipes for every Machine beyond Drooling Cauldron and Masticator.

---

## Tools

### Scalpel

**Status:** Two functions confirmed.

**Function 1 — harvesting:** Cuts a living tumor block (Inert Tumor or any mutation) to collect its item, leaving a Marred Tumor behind. See the Inert Tumor block chain above for full detail.

**Function 2 — reopening:** Cuts the stitches on a Stitched Tumor, reverting it to Marred — no drop, no harm. The deliberate safety-valve/undo step in the tumor pipeline.

**Secondary use — Bloodletting (player-facing):** Cures the Poison status effect instantly. Cost, confirmed in code (`IBloodLet.java`, `BloodLet.java`): a **bleed-over-time effect**, randomly picked from **3 discrete durations** (50/100/150 ticks, uniform chance each), damage ticking every 50 ticks at 0.5 heart per tick — so the total bleed lands at exactly **0.5, 1.0, or 1.5 hearts** (not a continuous roll, three fixed outcomes within that range). **Not literal vanilla Weakness/Slowness** — the movement penalty is a custom shared debuff (`MovementDebuffEffect`, also used by Sutured below): **-3% movement speed, -50% jump strength** as flat attribute modifiers. No attack-damage reduction, unlike real Weakness. Has its own death message, confirmed: **"%1$s starved a vampire."**

**Open questions:** None remaining.

---

### Suture Kit

**Status:** Two functions confirmed, interchangeably called "stitching" or "suturing" by the player.

**Function 1 — sealing:** Stitches a Marred Tumor closed into a Stitched Tumor, with or without items loaded inside.

**Function 2 — loading:** Sutures an item into a Marred Tumor — the first half of forcing a mutation or healing recipe, paired with a later Syringe injection. Gated by the (believed implemented, not yet confirmed) recipe-matching rule described under Marred Tumor above.

**Secondary use — Sutured status (player-facing):** Applies accelerated healing (confirmed **flat, not scaled to missing health**: 0.4 HP every 10 ticks / 0.5s, regardless of how much health is missing), at the cost of the same movement debuff Bloodletting uses (`MovementDebuffEffect`: -3% movement speed, -50% jump strength — not literal vanilla Weakness/Slowness). Currently self-use only; using it on other players is under consideration, not decided. Ends two ways:
- **Cleanly**, once the player reaches full health — no penalty.
- **Forcibly**, if the player sprints or takes any damage while active — the status is stripped and the player **instantly takes 4 hearts (8 damage)**, representing the stitches tearing from exertion. Has its own death message, confirmed: **"%1$s couldn't hold it together."**

**Resource cost:** Consumes either the kit's own durability, or **String** as an alternative.

**Open questions:** Multiplayer use-on-others decision.

---

### Forceps

**Status:** Function and scope confirmed — no secondary use.

**What it is:** The tool for **recovering** the mod's blocks — especially the "living" ones — since breaking one the vanilla way destroys it (you get nothing back but its contents). The Forceps hands back the block item; picking a block up still drops its contents on the ground exactly like a break — the Forceps just additionally give you the block itself, so you can re-place it and re-load it (see Inert Tumor above for the mechanic itself). It needs **no crouch** — as the deliberate "careful extraction" tool it takes priority over the block's own right-click (it acts before the block's interaction fires, so an interactive block like a machine or the Craw is lifted rather than opened).

**Design reasoning:** A deliberate bit of irony, not strict mechanical necessity — a mad scientist comfortable with deeply unnatural things being done to living tissue under controlled conditions still finds the idea of just *bashing something open* too barbaric to stomach. Fits the "forced/unnatural change, lab-process feel, not gore" tone target well.

**Confirmed — no secondary use.** Considered and deliberately left as a single-purpose tool — picking up the mod's blocks is treated as sufficient justification on its own. A clean real-world case of the "don't force it" addendum on the multi-use convention, applied to a Tool instead of a recipe ingredient.

---

### Syringe

**Status:** Naming convention and primary use established; now **two** confirmed secondary uses (the original expanded, plus a brand-new one on a different fluid), plus a roster of proposed-but-unconfirmed additions to the first.

**Naming convention:** Syringe variants are named **"[Fluid] Syringe"** (e.g. Crude Slurry Syringe) — distinct from Flask's "Flask of [Fluid]" pattern. The two Tool families don't share a naming style, and that's fine — first explicit convention note for Syringe specifically.

**Primary use (resolves a long-standing open question):** Draws fluid from certain blocks — exact filtering mechanism not yet refined, flagged for later — and injects that fluid into a Stitched Tumor (with sutured items already loaded) to complete whichever recipe the contents match. This is the actual mechanism behind "Syringe crafts Machines by hand," confirmed via the Tumor-to-Machine pipeline above.

**Secondary use — Crude Slurry Syringe on passive mobs:** Forces instant accelerated biological effects, consistent with Crude Slurry's real underlying identity (nutrient-dense, forces accelerated growth/repair — see `dermicraft-slurry-notes.md`). Confirmed so far:
- **Sheep** → instant wool regrowth
- **Baby mob** → instant adult growth
- **Chicken** → lays at least 1 egg instantly, possibly randomized to 2–3
- **Cow** → milk bucket availability instantly refreshed
- **Squid** → forces an instant release of its ink sac contents without killing it, repeatable with a cooldown (same shape as the Cow's refresh). **Confirmed as its own sub-category, distinct from the four above:** vanilla doesn't give Squid an existing wait-based cycle to fast-forward — ink is normally a death-drop only. This effect instead forces the *real* squid defense response (ink release as a startle/threat reaction) to fire without the kill, rather than accelerating a growth/production cycle — the roster's first "death-drop becomes a live-harvest" entry.

Roster explicitly open-ended — more passive mobs can be added anytime a natural effect comes to mind. **Rabbit** (breeding-readiness) was considered and shelved — no natural-feeling effect identified yet, left open rather than forced.

**Proposed, not yet confirmed (raised this session, reasoning logged for next time):**
- **Goat** → instant horn regrowth. Regrowth-of-a-part parallel to Sheep — goats already lose/regrow horns naturally over time.
- **Armadillo** → instant scute regrowth. Same regrowth-of-a-part shape as Sheep/Goat.
- **Turtle** → instant egg-laying. Resource-reset parallel to Chicken — turtles already lay eggs periodically in sand.
- **Bee** → instant hive/nest refill to full honey level. Resource-reset parallel to Cow's milk refresh.

**Secondary use — C-Stuff Syringe on Creeper:** The first secondary use confirmed for a Syringe variant other than Crude Slurry, and the first aimed at a hostile mob rather than a passive one. Injecting a Creeper disrupts its explosion-trigger process for a few seconds (exact duration TBD) — during this window the Creeper still swells and hisses normally (vanilla behavior untouched), but the swell is **blocked from actually detonating**, potentially across a couple of real swell/deflate cycles depending on how long the disruption runs. Once the disruption genuinely wears off, the *next* swell is real again. Repeatable indefinitely on the same Creeper, with a cooldown timed to land around when the disruption wears off — giving the player a brief, genuine window to reinject before that first real swell completes.

**Design intent — "cry wolf" tension:** Because hisses during the disrupted window are harmless bluffs and the real danger is the first hiss *after* the effect should have worn off, the player has to learn to read the timing rather than relying on a flat inert/active toggle. This costs nothing extra mechanically — the "fake" swells are just vanilla Creeper AI behaving normally; only the detonation step itself is being suppressed.

**Recipe logic — why C-Stuff, not Primitive Catalyst:** Primitive Catalyst was the original idea, but its recipe carries meat/plant "life" ingredients that have nothing to do with stopping an explosion — they're there because Primitive Catalyst's actual job is birthing Machines. **C-Stuff** (Carbon Blend + Calcium Blend — see `dermicraft-machine-notes.md` Effluentcer entry) isolates exactly the two components that justify the effect: Calcium as a binding agent (gums up/locks the Creeper's internal trigger) and Carbon/charcoal as a purifying/desaturating agent (neutralizes whatever's driving the volatile reaction), without the unrelated life-coded ingredients along for the ride. This also gives C-Stuff its first standalone use beyond being half of the fluid-based Primitive Catalyst shortcut (F-Stuff + C-Stuff → Primitive Catalyst).

**Dependency resolved:** C-Stuff is produced by the **Effluentcer**, which is now fully operational — this Syringe variant can be filled and used in-game today. (Flask of Protein Blend remains blocked on wolf-taming implementation, a separate and still-open dependency.)

**Shelved ideas (logged for history, not pursued further right now):**
- **Log block + Crude Slurry → regrown tree.** Scrapped as the first pass at this idea — felt too easy.
- **Log + Life Catalyst → reverts to sapling.** The identity logic actually holds up well (a log is dead organic matter; Life Catalyst's whole purpose is restoring "living" status; a sapling is the literal living, growing form of the same material) — shelved specifically because of implementation/programming difficulty, not a conceptual flaw. Flagged to revisit once it feels more developed.

**Open questions:** Exact fluid-draw filtering mechanism and full roster of valid draw-source blocks. Exact cooldown length for the Squid ink effect. Exact disruption duration and cooldown length for the Creeper effect (disruption needs to run long enough for a couple of fake swell cycles; cooldown should land around when disruption ends). Whether to confirm the Goat/Armadillo/Turtle/Bee proposals above. Whether Syringe variants beyond Crude Slurry and C-Stuff have further secondary uses yet to be worked out.

---

### Nose on a Stick

**Status:** Design complete, not yet implemented in code.

**What it is:** An early-game Tool for tracking down items, blocks, and entities by "scent." Fits the mod's tone — grotesque but not self-serious.

**Recipe (implant):** 1× Stick + 1× Dense Muscle + 3× Nerve Cluster, sutured, injected with Primitive Catalyst.

**Appearance:** The stick is a normal vanilla stick. The nose itself is undecided — candidates being prototyped: a cartoonishly large human nose (broad sight-gag, instantly readable), a villager nose (specific, recognizable Dermicraft-flavored silhouette, first instance of a part pulled from a specific vanilla creature rather than a generic tumor part), or a pillager nose (same silhouette as villager, but framed as a trophy taken from a hostile mob rather than harvested from an innocent one). Multiple versions may get built before settling on a favorite.

**Attunement:**
- **Crouch + sniff** (target item in hand, or pointed at a block/entity) attunes the Nose to a **scent family**, not the literal exact item — fuzzy-matched. Example: sniffing an Iron Ingot also tracks Iron Ore.
- On attunement, the Nose scans a **5-chunk radius** and locks onto **one of the first three qualifying instances** it finds.
- **Entity targets:** only **harvest-ready** mobs qualify (e.g. a shearable sheep; not a mob that's already been harvested).
- **Re-sniffing:** instant — crouch-sniffing a new target immediately overrides any existing lock, no confirmation step.
- **Sniffing nothing** (no valid scent found) blanks the Nose, clearing any current lock.
- **Lock persistence:** once locked, the Nose holds its specific target until the player manually resets it (re-sniff), the target block/item is collected or destroyed, or — for entity-based targets — the entity's relevant drop is collected/harvested (not necessarily on the entity's death).

**Reading (stand + sniff):** Framed as the player character reading the Nose's behavior, not a raw data readout.
- **Direction:** accurate, dowsing-rod style — the Nose always points true.
- **Distance:** deliberately vague, reported in whichever axis (horizontal/vertical) is most relevant. Beyond 1 chunk: reported in chunks, with fuzz that **widens the further away the target is** (tight near 1 chunk, wide out toward the 5-chunk edge). Within 1 chunk: switches to a block count for the dominant axis, transitioning **softly** rather than as a hard cutoff. Within 5 blocks: "It's close."
- **Range cap:** 5 chunks — matches the initial lock-on scan radius.

**Open questions:** Exact fuzz curve (tolerance values at each distance band). Sniff cooldown — likely to be added for server performance/anti-spam reasons; not a design-level restriction.

---

## Status effects introduced this session (cross-reference)

- **Spicy Regret** (Flask of Lava) — death message (confirmed): `"%1$s succumbed to spicy regret"`. Full mechanic in `dermicraft-flask-notes.md`.
- **Sutured** (Suture Kit) — death message (confirmed): `"%1$s couldn't hold it together."` Full mechanic above.
- **Bloodletting** (Scalpel) — death message (confirmed): `"%1$s starved a vampire."` Full mechanic above.
- **Shared mechanic note:** Bloodletting and Sutured both extend the same custom `MovementDebuffEffect` base class (-3% movement speed, -50% jump strength) — not vanilla Weakness/Slowness in either case.

---

## Open questions (general, across this doc) — left as-is

- **Per-machine tumor recipes** for everything beyond Drooling Cauldron, Masticator, Effluentcer, and Skin Tank (Metastasizer, Drooling Crucible, Flesh Lab, Filling Station, the farming trio) — none written yet.
- **Squid ink effect cooldown length** — not located in code.
- **Creeper C-Stuff disruption duration and cooldown length** — not located in code.
- **Suture Kit multiplayer use-on-others** — design decision, not a code question.
- **Goat/Armadillo/Turtle/Bee Syringe proposals** — unconfirmed, not implemented.

---

## Template for new entries

```
### [Tool/Block Name]

**Status:**

**What it is:**

**Recipe/logic:**

**Open questions:**
```
