# Dermicraft — Beaker: Block & Item Behavior Outline

**Purpose of this document:** A behavior/requirements outline for the Beaker item and its matching block form, written so an engineer can implement it without needing additional design clarification. **No code or implementation detail is specified here** — only what the thing must do. Built from scratch through direct confirmation of each piece, replacing the earlier draft pulled from the legacy notes file.

**Status vs. `dermicraft-beaker-notes.md`:** The Beaker is now operational (`BeakerItem.java`/`BeakerBlock.java`/`BeakerBlockEntity.java`), and most of this outline matches the shipped behavior. **One confirmed reversal:** §4.1's "no automation support, player-interaction-only" call was later overturned — the current implementation allows automation (hoppers, pipes, etc.) to access the block's tank from any face, same as most other tank blocks in the mod. See `dermicraft-beaker-notes.md` for the current, authoritative word on that point; everything else here (capacity, naming, stack size, spill/break-vs-pickup logic) still holds and isn't restated there.

**Out of scope:** Crafting recipe (handled code-side, intentionally excluded from this doc). Exact texture/visual design. Code-level implementation.

---

## 1. Overview

- **Category:** Tool (hand-held implement), with a matching placeable block form.
- Beaker is one object moving between two states — item and block — not two separate things. Fluid contents persist across both forms and across the transition between them.

---

## 2. Core Properties (apply to both item and block form)

- **Capacity:** 1000 mB.
- **Fluid type:** No restriction — can hold any fluid.
- **Single fluid only:** Holds one fluid type at a time. No mixing.
- **Fluid amount is a value, not a state:** Beaker holds anywhere from 0–1000 mB. There is no separate "full" or "empty" flag — just the current amount. (0 mB and 1000 mB are simply the two ends of that range.)

---

## 3. Item Form

### 3.1 Filling/Emptying
- Behaves like a vanilla bucket: right-click a fluid source to fill, right-click to empty.

### 3.2 Stack Size
- Max stack size: **1**.

### 3.3 Visual Representation
- The item's appearance reflects the fluid it currently holds (e.g. a visible fluid fill showing through the glass).

### 3.4 Display Name
- When holding fluid (1–1000 mB): item name displays as **"Beaker of [Fluid]"**.
- When empty (0 mB): item name stays plain **"Beaker"** — same naming exception pattern as Glass Flask.

### 3.5 Light Emission — Stretch Goal
- **Not a blocking requirement.** If feasible, the item should emit light matching the held fluid's own light level (e.g. glows when holding an emissive fluid, stays dark for a non-emissive one).

### 3.6 Placement
- Player places the block form by **crouching + right-clicking**.

---

## 4. Block Form

### 4.1 Tank Behavior — superseded, see note below
- Functions as a tank: fluid can be added to or removed from it.
- ~~No automation support of any kind (hoppers, pipes, etc. cannot interact with it)~~ — **not what shipped.** This section originally called for player-interaction-only access, reversing an even earlier top-face-only automation rule from the legacy notes. That call didn't carry through to implementation: `BeakerBlockEntity` registers its fluid-handler capability block-wide with no face gating, so automation works from any face. Confirmed by reading `BeakerBlockEntity.java`/`BeakerBlock.java` directly — see `dermicraft-beaker-notes.md` for the current, authoritative behavior.
- Interaction mechanic that *did* ship (right-click with a bucket, Forceps, or empty hand) is documented in `dermicraft-beaker-notes.md`.

### 4.2 Durability
- Low hardness — deliberately easy to break.

### 4.3 Light Emission — Requirement
- The placed block emits light matching the held fluid's own light level. Confirmed feasible; this is a firm requirement, not a stretch goal.

### 4.4 Breaking / Retrieval Behavior

Two distinct player actions against the block, with different outcomes:

**A. Vanilla-style break** (breaking it with any tool, the normal way — i.e. *not* using the proper pickup method below):
- The block is **always destroyed**. No item is returned under any circumstance.
- If it was holding fluid (1–999 mB, i.e. some amount short of completely empty): that fluid is **lost** — not spilled, not preserved, not returned.
- If it was holding 0 mB: nothing further happens beyond the block being destroyed (there's no fluid to lose).

**B. Proper pickup** (Forceps, or right-click with an empty hand):
- The block returns to the player as an **item**, retaining whatever fluid amount it held at the time (anywhere from 0–1000 mB).
- This is the only way to recover the block as a usable item with its contents intact.

---

## 5. Items Deferred to Code / Not Yet Specified

- **Recipe** — intentionally excluded from this document.
- **Exact block fill/drain interaction mechanic** — left for code to determine.
- **Item light-emission feasibility** — stretch goal, implement if practical; not blocking.
