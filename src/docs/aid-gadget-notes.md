# A.I.D. — Adaptive Intervention Device

**Target file for merge:** `dermicraft-gadget-notes.md` — place as a new entry under Known Gadgets, alongside Drinker/Sipper/Eater/Lobber/Grapple/Injection Gun.

**Status:** Concept fully specified. Model and display file (animations, bone-swap setup) done. Textures remaining, then the rest of the item logic (mode component, GUI, recipe, `HandShreddingEvent` wiring).

**Origin note:** Revival of a precursor-Dermicraft gadget originally named F.R.I.E.N.D. ("D" for Device), abandoned partway through implementation before the mod's rebuild and never fully functional (only some of its four tool functions existed). Redesigned from scratch on the current mode-driven interaction model rather than resurrected as-is — old name kept only as a documented fallback, not the intended name. A single Gadget-tier item that fully replaces the Scalpel, Forceps, Suture Kit, and Syringe by reproducing all of their existing tag-driven behavior (`COLLECTION_TOOLS`/`EXTRACTION_TOOLS`/`INJECTION_TOOLS`/`SUTURE_TOOLS`) under one mode switch. Explicitly meant to coexist with the Injection Gun, not replace it — this covers general one-tool convenience across all four surgical functions, while Injection Gun stays the specialized multi-fluid loadout for a player who wants to carry several injectable fluids at once.

## Core concept

One handheld Gadget, four modes — Scalpel, Forceps, Suture, Syringe — each reproducing that base tool's real behavior. Mode-driven rather than target-driven disambiguation: the active mode alone decides which of the four actions a right-click performs, never inferred from what the target could accept. Chosen deliberately as "the safest way" over letting a target's own capabilities pick the verb.

## Interaction model

- **Cycle:** tap with no valid target cycles through the four modes, single direction only (no crouch-reverse the way Drinker has).
- **A valid target for the current mode always wins over cycling** — same precedence as Drinker/Injection Gun.
- **GUI:** crouch-right-click opens a screen, unconditionally (not gated on target). Two slots: one 1000mB fluid tank (Syringe mode's single-fluid buffer) and one item slot for String stock (Suture mode's consumable).

### Scalpel mode
- **Harvest** — same as base `ScalpelItem.useOn`'s `IHarvestParts` harvest action, durability cost.
- **Innards Duct connection cycling** — same free, no-durability plumbing fixup `ScalpelItem` already does on ducts. Included and gated behind Scalpel mode being active.
- **Hand-shredding recipes** (wool→string, pumpkin→carved pumpkin, etc.) — `HandShreddingEvent` matches by exact tool-item identity against `HandShreddingRecipe` entries, so A.I.D. needs its own parallel recipe entries (mirroring how Primitive Scalpel already has separate entries from the iron Scalpel) rather than inheriting Scalpel's automatically. Gated behind Scalpel mode: the event handler must check A.I.D.'s current mode when the matched tool is this item, and refuse if not in Scalpel mode.
- **No self blood-let.** The base Scalpel's `use()` (right-click in air removes poison / applies blood-let to the player) is deliberately dropped — that gesture is claimed by the mode-cycle on A.I.D., and there's no other free gesture to give it.

### Forceps mode
- **Collect** — identical to standalone `ForcepsItem.onItemUseFirst`/`collect()`: picked-up blocks go straight to the player, no internal storage of any kind on A.I.D.

### Suture mode
- Consumes String **from A.I.D.'s own item slot only** — no fallback search into the player's general inventory (unlike the standalone `SutureKitItem`, which does fall back to the player's inventory).
- **No string in the slot → hard refuse + action-bar warning.** No durability-damage fallback like the standalone kit has.

### Syringe mode
- Single 1000mB fluid tank (one fluid at a time — this is the deliberate contrast with Injection Gun's 5-cell system).
- **Empty tank → hard refuse + action-bar warning**, same treatment as Suture mode's empty-string case.

## Presentation

- **GeckoLib model, Gadget-tier:** HP + death flourish, matching the rest of the `IGadget` family.
- Physical form/shape not yet decided (Injection Gun landed on a revolver shape tied to its name; A.I.D.'s shape is still open).

### Model & animation (in progress)

Each mode has its own exclusive bone(s), swapped at mode-cycle: current mode's retract animation plays, its bone is hidden, the next mode's bone becomes visible, that mode's deploy animation plays. Per-mode clips (all built):
- **Forceps:** deploy, retract, idle, grab.
- **Scalpel:** deploy, retract, cut. No idle clip — deliberately holds still after deploy rather than looping a separate idle pose.
- **Suture:** deploy, retract, sow, idle.
- **Syringe:** deploy, retract, inject. Same no-idle-clip choice as Scalpel, same reasoning (holds still by design).

**Rapid-cycling plan:** first pass is to let deploy/retract simply play out in full for each step of a fast cycle (they're short clips, so this is expected to feel fine). If playtesting shows this is too cumbersome during a fast multi-step cycle, the fallback is to suppress animation and hide all bones while the player is still actively clicking through modes, then only play the model normally once they stop.

**Deferred:** how the four per-mode bones/controllers avoid the cross-controller bone-bleed Eater hit (see [[project_gadget_hp_mechanic]]-adjacent GeckoLib notes above) — not designed yet, to be resolved during implementation.

## Naming

**A.I.D. — Adaptive Intervention Device.** "Adaptive" names the mode-cycling mechanic directly; "Intervention" is the generic clinical term covering incision/injection/suturing/extraction alike, so it doesn't overcommit to any one of the four modes. "AID" itself reads as help/first-aid before expansion, the same kind of found-word gift as Repeater's Redstone pun. Old precursor name **F.R.I.E.N.D.** kept on file as a documented fallback only, not the intended name.

## Fabrication recipe

**Gadget Fabricating**, Tier 1, 90 seconds (Eater's complexity tier, reflecting four distinct mode-gated behaviors).

- **Items:** 1× Chassis, 1× Proto Brain, 1× Scalpel, 1× Forceps, 1× Suture Kit, 1× Syringe — full craft-up consuming all four base tools it replaces.
- **Fluids:** 1000 mB Ferrous Blend + 1000 mB Aurous Blend ("a bucket of gold and iron blend").
- **Proto Brain included** (unlike Injection Gun, which explicitly omits it) — deliberate contrast: Injection Gun only routes fluid, a purely mechanical action, while A.I.D. has to decide which of four distinct surgical functions to perform, which justifies a processor.

## Open questions

- Overall physical form/shape (mode is now communicated via bone-swap + deploy/retract animation, so this is more resolved than a separate dial/indicator would need, but the base housing shape itself is still undecided).
- Cross-controller bone bleed between the four per-mode animation controllers — deferred to implementation, see Model & animation above.
- Implementation details: mode data component shape, `HandShreddingEvent` mode-check wiring, GUI slot filtering specifics — not yet raised.
