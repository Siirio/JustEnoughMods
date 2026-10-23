# Local Gemini architecture consultation

Date: 2026-10-23, Asia/Yekaterinburg.

The user explicitly requested using the installed local Gemini application while
finishing the non-art portion of this task. The consultation used the existing
Gemini desktop application, version directory `app-1.16.0`, through its visible
Windows UI. Its returned window was minimized, restoring that actual window made
the authenticated application accessible. No authentication, privacy/security
settings or unrelated application workspaces were modified.

Conversation: [Preventing Minecraft Forge entity duplication](https://gemini.google.com/app/3c8af4714029384a).
The UI mode label was `Argon High`, that label is recorded as observed, not as an
assertion about an underlying model version.

No models, icons or other art were requested or generated in this consultation.
Only a nonsecret technical architecture brief was entered. No project archive,
world data, credentials, personal records or local runtime logs were uploaded.

## Supplied question

The brief described exact-class support for native cats, wolves, horses, donkeys
and mules, stable identity/epoch and pending native NBT, one pet/home assignment,
confirmed-death capture and suppressed death loot, strict journal persistence,
native entity-storage acknowledgments, full-AABB loaded-only shelter recovery,
buried/undersized-home behavior, and the restriction against invented mount AI
locks. It asked Gemini to scrutinize loss/duplication risks, especially a process
crash between a persisted restore intent and durable native entity storage.

Gemini did not receive the complete implementation source. Its answer is therefore
external AI advice about that supplied description, not a review of every current
line, installed-bytecode proof, compilation result or gameplay verification.

## Actual response and disposition

Gemini distinguished three journal-indistinguishable crash positions: before
native entity insertion, after volatile insertion but before native storage, and
after native storage but before clearing the journal. It also observed that
allowing free movement and inventory mutation before durability makes replaying an
old pending snapshot unsafe. These observations support the existing requirement
to reconcile native persistence rather than guess that an unloaded UUID is absent.

Its proposed design added a per-attempt identity, a persisted target chunk,
startup reconciliation and rejection of older attempts. However, its proposed
pre-commit quarantine also froze mount movement and blocked inventory/leash
interaction. That extension conflicts with the user's native-movement contract
and was **not adopted** from this answer. The answer does not resolve the full
crash window under the actual no-extra-mount-lock constraints by itself.

Other topics raised by Gemini:

| Topic | Disposition |
| --- | --- |
| Wait for actual native storage completion instead of serialization/enqueue | Relevant invariant, the implementation owner separately inspected exact native storage code. The answer alone does not prove fsync/power-loss guarantees. |
| Do not insert while the dying UUID remains registered | Already present in the source reviewed before consultation, no alternative entity discard/particle replacement was adopted. |
| Restore healthy native state, clear lethal transient effects/fire/fall state | Already addressed by the restoration path, no blind NBT field recipe copied from the answer. |
| Strip passenger and leash state to avoid unsafe duplication | Already present in death capture. Native lead behavior remains an exact-source compatibility concern. |
| Mount inventory menu/cursor transaction ordering | Unverified suggestion, not a demonstrated duplication bug. A cursor stack removed from the native inventory is not itself proof of duplication. Any change needs actual native menu/source evidence. |
| Full AABB rather than feet-cell clearance | Already present. The implementation considers a centered interior candidate and external/above candidates, no geometry-derived eligibility cap was introduced. |
| Synchronous durable journal work may stall a tick during simultaneous deaths | Reasonable performance concern, not a measured runtime result. Delayed/coalesced capture must not weaken persistence-before-loot ordering. |

Gemini additionally claimed that a particular native entity-ticking query proves
that asynchronous entity loading has completed and listed exact API names and
dimensions. Those statements remain **unverified advice** in this consultation.
Only installed source/bytecode inspection by the implementation owner can promote
an exact method contract or numeric native fact to project evidence.

## Result boundary

The local Gemini consultation was completed and its actual response read through
the application UI. Findings were sent to the implementation owner. This file
records consultation provenance and dispositions, it creates no separate gameplay
authority, no additional automated tests and no claim of successful runtime QA.
Current behavior, supported entities and remaining limitations continue to belong
to the companions source, canonical Atlas change record and owning companions
documentation.

## Focused no-movement-lock follow-up

A second question in the same conversation asked about a proposed design retaining
the native UUID, incrementing attempt epochs after uncertain crashes, saving the
single already-loaded spawn chunk and briefly gating owner interaction until the
native acknowledgment. It explicitly prohibited AI/movement locks and asked about
cross-chunk movement, unsolicited inventory changes, UUID identity and save ordering.

The completed Gemini response raised the following hypotheses:

- An asynchronous saving interval permits movement before acknowledgment. Old and
  new entity chunks may then hold the same epoch, while a stale-epoch filter only
  rejects earlier epochs. This requires exact native save/load ordering analysis,
  the answer does not establish that a particular project implementation duplicates.
- Blocking only the owner's interactions would not prevent other players' native
  mount-inventory access. Second death, portals, leads and non-player equipment
  mutation also need consideration while a replayable snapshot remains pending.
- Replaying a snapshot after advancing epoch does not reverse world side effects
  caused before a crash. UUID rejection must not leave native registration state
  poisoned, exact Forge join-event ordering must be inspected rather than guessed.
- Native serialization, write completion and storage flush are distinct stages
  The original native future must be observed rather than an exception-swallowing
  wrapper or method return.

One specific follow-up claim was **disproved by the installed Forge bytecode**:
Gemini suggested the join event might run after `knownUuids` registration and
poison that registration when canceled. In cached Forge 1.20.1-47.4.10,
`PersistentEntitySectionManager.addEntity` posts `EntityJoinLevelEvent` first and
returns false immediately when canceled, only afterward does
`addEntityWithoutEvent` call `addEntityUuid`. The stale-epoch cancel therefore does
not need an invented UUID cleanup to solve that suggested ordering problem.

Gemini again proposed invulnerability, portal suppression and chunk-boundary
clamping. Those suggestions were not adopted from this answer: they change the
requested gameplay/physical movement contract. Its dispenser, UUID-event-order and
named API claims remain unverified until installed source inspection supports them.

The implementation direction subsequently chosen by the project owner removes the
asynchronous simulation interval: serialize and commit through the native entity
manager, wait for the actual write and native worker flush, and strictly commit the
journal before returning control to ordinary simulation/player interaction. It
does not freeze individual pet AI or add an owner interaction workflow. This is a
source-derived engineering decision, not a claim that Gemini proved the code.

Exact cached Minecraft Forge 1.20.1-47.4.10 bytecode was independently inspected for
this direction. `PersistentEntitySectionManager.storeChunkSections(long, Consumer)`
stores the existing section entities and then invokes its supplied consumer,
providing a no-op consumer saves without unloading the live entities.
`areEntitiesLoaded(long)` checks native `LOADED` status. `IOWorker` uses
`Util.ioPool`, its actual store future is completed after native region writing,
and `synchronize(true)` awaits pending writes then invokes native storage flush.
Those inspected stages do not require a continuation on the blocked server
thread. The source guards the exact serialized UUID/epoch before journal clearing.
Disk latency may still pause the server during this bounded durability operation,
no runtime latency or crash/power-loss test is claimed.

For multipart companions that have genuinely independent persistent entity rows,
the commit applies to every adapter-reported part and distinct already-loaded
entity chunk. Each expected part UUID/epoch must appear in the actual native
serialization, and all write futures plus the native flush must succeed before
the one recovery journal clears. Transient Forge parts are not fabricated as
separate persistent entities. The adapter owns exact body completeness and native
part identities, the persistence layer refuses missing/unaccepted parts rather
than accepting a head-only acknowledgment.
