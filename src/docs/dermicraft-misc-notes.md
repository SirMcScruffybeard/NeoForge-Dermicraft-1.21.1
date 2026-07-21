# Dermicraft Misc Notes

Running log of decided design choices for ideas that don't fit cleanly into the other per-category companion docs (Catalysts, Slurries, Crafting, Flask, Beaker, Gadgets, Machines, Tools). Add a new entry per idea as decisions get made — the "why" matters as much as the "what."

---

## In-game Guide System (Notebook → Tablet)

**Status:** Overall arc and tone decided. Individual item mechanics (Notebook scope/unlock, Tablet capabilities) still largely open — Tablet's own gadget-specific detail lives in `dermicraft-gadget-notes.md`; this entry covers the system's shared narrative/tone framework and the parts that don't fit a single category doc.

**What it is:** Dermicraft's in-game guide is split into two sequential items rather than one flat reference, giving the guide itself a progression arc tied to the mod's own Tier 1 → OT milestone:
- **The Notebook** — a book-style item, available from early game, guiding the player toward building the Operating Theater. Non-living, fixed/finished content.
- **The Tablet** — an intelligent, living Gadget crafted by the OT (see `dermicraft-gadget-notes.md`), built from the Notebook plus other ingredients. The Notebook is consumed but its information carries over. Self-authoring — actively records and writes new notes as the player works, rather than presenting only pre-written content.

An online/external guide is still expected to be necessary for full reference, but the goal is for the in-game guide to cover most of what a player needs without leaving the mod.

**Narrative framing — the unsettled scientist.** Both guides are framed as the work of a single recurring figure: an unsettled/unstable scientist whose lab and research the player is stepping into throughout the whole mod. The Notebook is their already-written, left-behind notes — the player starts out **reconstructing** this scientist's earlier work rather than inventing it themselves. Reaching the OT and crafting the Tablet marks a deliberate narrative turn: the player shifts from reconstructing the scientist's past work to **carrying it onward** into new territory the scientist never finished or reached. This retroactively reinforces existing framing already on record — the Tier 1 → Tier 2 wall as "leaving the tutorial phase, entering the real game," and the OT as the mod's central milestone — by giving that transition an actual narrative beat, not just a mechanical one.

**Tone rule — flavor never garbles function.** The scientist's unsettled voice is meant to come through in framing, asides, and margin-note-style commentary, but the mod's existing "aesthetic flavor over punishing/obscuring mechanics" principle (see primer's tone target) extends directly to the guide's own readability: actual recipe/mechanic instructions must stay clean and clearly distinguishable from flavor text (e.g. a visually/structurally distinct "notes" vs. "procedure" formatting), so a player skimming for practical information never has to wade through unreliable prose to find it. The unsettling tone is something a player can choose to read into, not an obstacle to using the guide.

**Scientist's fate — a genuinely dark, currently-presumed outcome, confirmed to resurface as the final boss.** Not yet detailed beyond that. Open questions this raises, flagged for a future session: whether the Tablet's own voice reflects any awareness of what happened to the scientist (or is equally in the dark until a late-game reveal); and what the scientist's transformation/fate actually was mechanically (a "living machine" possibility was floated, unconfirmed).

**Gradual-hint mechanism now has a confirmed answer (partially resolves the "surface gradually vs. stay hidden" question above) — The Scientist hallucination, part of Metaphysical (Mild)'s Hallucination Table.** See `dermicraft-hazard-effects-notes.md` → Metaphysical (Mild) for full detail. A player exposed to Metaphysical hazards can hallucinate the scientist himself, deliberately behaving differently from the table's other (random doppelganger/Herobrine) entries — the mechanism for hinting he may be more than a hallucination, well before the final-boss reveal confirms it. The entry's exact behavior/frequency/evolution-over-progression is still undesigned, flagged there as its own future design pass.

**Tablet declutter mechanic:** Player can manually lock/unlock sections of the Tablet's content for personal organization — no mechanical cost, purely presentation control. Full detail in `dermicraft-gadget-notes.md`.

**Hazard-information gating (confirmed) — the Notebook/Tablet's first concretely-scoped content.** Fluid hazard info (which specific `hazard/*` tag(s) — Extreme Heat, Radiation Mild/Severe, Biohazard, Metaphysical Mild/Severe — a fluid carries; see `dermicraft-project-primer.md` → Hazard tag hierarchy) is gated behind the player simply **possessing** the Notebook or Tablet **anywhere in inventory** (not held/equipped — passive possession is enough), with one exception:
- **Vanilla lava needs no gate at all.** It's self-evidently dangerous without any in-game explanation — free to every player, no item required.
- **The Notebook's hazard knowledge is capped at Stage 2 fluids** (Extreme Heat and whatever else debuts that Stage) — not a hard wall, an intentional narrative **grace**: a player who reaches past the Operating Theater before crafting the Tablet still gets partial coverage, because the scientist who wrote the Notebook had gotten "a little further along" than the player has. Ties directly into the narrative framing above (reconstructing the scientist's earlier, already-further-along work).
- **The Tablet inherits the Notebook's full knowledge on construction** (this is the in-fiction reason the Notebook is consumed as a crafting ingredient rather than just referenced). Anything past that Stage 2 baseline is **not automatic** — the player has to discover it in-world (samples, research, reverse-engineering, etc.), and discovery is deliberately **many-to-one**: finding one thing can unlock several hazard entries at once, not a strict one-find-one-unlock checklist. Mirrors the "batch of interconnected notes" feel of the Notebook rather than a linear tech tree, and gives later Stages a built-in reason to scatter discoverable lore/knowledge instead of just gating by Tier number.
- **The existing lock/unlock declutter mechanic doubles as the on/off for this info** — hazard tooltip detail (coarse + specific kind, shown together, not separately tiered) is one togglable section rather than a new UI concept. **Diagnostic/troubleshooting info is a separate, ungated concern** — container tier, "why is this run blocked," routing — not covered by the possession-gate above, and not yet designed beyond "the Tablet is the natural home for it" (see Tablet's general design stance below).
- **Hard line:** neither item is ever mandatory. A player with neither survives on the free vanilla-lava warning alone (silent rejection otherwise, no error message); the Notebook/Tablet make the player **fluent**, never merely **functional** — reward, not a progression tax.
- **General Tablet stance this sets a precedent for:** when a mod system turns out too opaque to the player (duct routing, Node leg toggles, machine HP, hazards), the go-to fix is a **new Tablet content/inspection channel**, not a new machine-GUI widget or in-world clutter — keeps individual machine screens lean.

**Open questions:** Notebook's exact scope beyond hazard info (critical-path-to-OT only, or does it also cover other side content the player might encounter earlier?) and its unlock model for that broader content (available from the start vs. discovery-gated — hazard info specifically is available-from-start, capped at Stage 2, per above). Whether the Tablet's voice shifts once "carrying on" begins, or the content shifts while the voice stays consistent. Exact nature of the Tablet's physical connection to the player (see `dermicraft-gadget-notes.md`). Whether/when the player starts suspecting the scientist's fate before the final-boss reveal. Exact shape of the diagnostic/troubleshooting channel (not yet designed).
- **TODO, confirmed needed:** a warning that crafting with a *filled* fluid container as an ingredient (e.g. a filled Bladder used in the Fuel Bladder/Feeder Bladder recipes) silently destroys the stored fluid — recipes match by item type only, not fluid-fill state. Fits the diagnostic/troubleshooting channel described above rather than the hazard-info channel (it's a "why did this happen" warning, not a hazard classification) — no tooltip stopgap added; deliberately deferred until this guide content gets built out for real.
- **TODO, confirmed needed — Innards Gate Port mixing input + output Buffers:** a single Port can intentionally serve *multiple* touching Buffers at once (one duct line fills/drains all of them — e.g. feeding fuel to two stacked machines from one line). Because a Port is a deliberately dumb block with no channel-IN/OUT awareness, it does **not** guard against a Port that touches both an **input** Buffer (fuel/ingredient) and an **output** Buffer (result) simultaneously — a duct *pulling* through such a Port drains whichever Buffer is non-empty first, which could siphon back the input you just pushed in (short-circuit). This is an accepted, player-fixable plumbing error (give the output its own Port, or don't share a Port across input+output), same class as pointing a hopper the wrong way. Belongs in the diagnostic/troubleshooting channel: "a Port serving both an input and an output can drain your inputs — keep input Ports and output Ports separate." Deferred with the rest of this guide content; no in-world guard added by design.

---

## Template for new entries

```
### [Idea Name]

**Status:**

**What it is:**

**Recipe/logic:**

**Open questions:**
```
