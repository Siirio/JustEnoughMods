# Settlements standalone design

## Final sandbox promotion — 2026-10-18

The user approved the final Races and Settlements for the local dedicated server and `test jem` via native AutoModpack. Matching fresh production Races1.1.0, Roles2.0.0 and full Settlements2.1.0 artifacts now form the local managed generation. MineRENT, stable profiles and the immutable release baseline are unchanged.

Races uses the final authored ability pipeline and previews on dedicated connections. Existing authenticated JEMRoles account records remain the sole save owner: final per-race mastery/cooldown data lives in their Mechanics compound, initialized from existing selection/progress/cooldowns. Legacy gameplay/key/loot owners yield while final Races is installed. Production has normal cooldowns and no developer authority. Origin count, DEV tier/reset controls and the crossed preview/DEV cooldown explanations are removed from the shared screen for every origin. Settlements promotes the existing final protocol9 source, without redesigning its town simulation.

Only necessary production compilation/reobfuscation and ordinary server installation/publication were performed. No tests, client launch or gameplay/visual acceptance is claimed. Artifact and publication receipt: `Docs/races/server-test-migration.json`.

## Current sandbox-only revision — 2026-10-17

User now restricts delivery to `jem sandbox`. Ordinary Server, Client/ManagedPack, native generation26, stable profile and release baseline stay unchanged. The source below supersedes earlier GUI/enrollment notes only when this fresh sandbox artifact is installed; prior deployment receipts remain history.

Navigation is Residents, Happiness, Chests, Treasury, Settings. Map/Food/Recruits tabs are removed; Recruits is a Residents filter. Existing native Xaero territory integration remains separate. NPC names use actual native custom/translatable components and their last observed persisted name, never UUID abbreviations. Villager Names is optional and not newly installed: its existing CustomName is respected, as are name tags, without JEM generating another name. [Upstream1.20.1 naming owner](https://github.com/Serilum/Villager-Names/blob/1.20.1/Common/src/main/java/com/serilum/villagernames/events/VillagerEvent.java).

JEM recruit needs require explicit bell registration in an owned town. Hiring and automatic payroll adoption do not enroll needs. Legacy records lack reliable enrollment provenance, so they load unenrolled and need one explicit bell registration; native employment/payroll survive. Jade hides JEM needs on unenrolled recruits. Loaded enrolled recruits use native needs; unloaded needs are unknown rather than zero. Critical hunger below10 permits a32tick native-goal meal pause, throttled100ticks, using actual carried/immediately reachable food. Native hunger clock/damage remain owners; JEM extends only the upstream disabled-starvation branch for registered recruits, at0hunger with0.25damage, avoiding a duplicate native hit.

Chests lists linked physical stores and reads one selected inventory page (max54slots, arbitrary nonnegative offset). The server validates owner/link/dimension and loaded halves of a double chest, without forcing chunks. Large per-stack NBT is explicitly omitted beyond8192bytes; item/count remain visible and the hover names that limit. The command Everyone eat queues all registered living town residents and explicitly enrolled recruits at the selected chest. Full NPCs waste no food; not-full NPCs receive a real meal request regardless of spontaneous craving threshold. Real pathing, one-item extraction, harmful-food policy, serving cooldown and daily limits remain. Orders are transient120seconds, superseded per NPC and cleared at stop; authoritative MealOrders reports pending work. Explicit chest orders reserve native movement and may interrupt ordinary combat; item use is released for serving. Spontaneous meals also reserve native movement instead of competing with hold/follow goals from the server tick. Native pending asynchronous paths are passed to navigation unchanged, with physical reach still required before extraction. Automatic critical nearby meals retain their existing combat limits.

Town wire protocol9 is installed only in sandbox; ordinary deployed protocol8 remains unchanged. No client/test/server launch accompanies this delivery. [Initial sandbox installation](sandbox-only-iteration-delivery.json); [recruit-meal correction](sandbox-recruit-meal-delivery.json).


Status: **approved and implemented in source; production artifacts built; runtime and visual acceptance tracked in TEST_MATRIX.md**. Approval and implementation date: 2026-10-17.

This directory owns the standalone GUI contract and delivery evidence. Settlements2.1.0 supersedes the dedicated-only entry and Copper expansion behavior in `../settlements-update-1.md` and `../../economy/settlement-implementation-notes.md`. Source comments and historical test reports are not acceptance evidence.

## User decisions

- Forge 1.20.1 / Java 17; independently installable Settlements, integrated logical-server authority and the same screens on compatible dedicated servers.
- Native Minecraft proportions and compact icons; map atmosphere informed by Antique Atlas. No Overview, web dashboard, replacement merchant GUI or replacement Xaero controls.
- Existing mechanics survive except explicit changes: Gold replaces Copper expansion pricing; vanilla edible items must work; Settlements itself becomes available in singleplayer. This exception must not activate unrelated JEM SMP systems in singleplayer.
- **Current user decision:** Villager Workers and its compatibility are removed from current local/future MineRENT inputs. Ordinary Minecraft villagers keep their professions; optional Villager Recruits owns combat NPCs. No replacement worker AI or simulated production is introduced.
- Actual state/actions and exact asset contracts precede artwork production. Production artwork is not approved by this design.

## Documents

| Document | Owns |
| --- | --- |
| [FEATURE_AUDIT.md](FEATURE_AUDIT.md) | Source evidence, existing behavior, gaps, ownership and delivery stages |
| [GUI_SPEC.md](GUI_SPEC.md) | Screen/component geometry, fields and action contracts |
| [GUI_STATE_MACHINE.md](GUI_STATE_MACHINE.md) | Entry, selection, request/acknowledgment, synchronization and lifecycle |
| [ART_BRIEFS.md](ART_BRIEFS.md) | Exact PNG contracts, editable sources, states, placement and art gates |
| [TEST_MATRIX.md](TEST_MATRIX.md) | Actual evidence and pending acceptance; no fabricated passes |

## Implementation choices by Codex

1. Keep `Mods/JEMSettlements` as the owning module. Replace its reflection-based SMP entry with its own screens and typed, bounded network projections. JEMServer becomes an optional navigation consumer of those screens, never their implementation owner.
2. Add a local Settlements authority boundary, not a global change to `DedicatedSmp`. Use player UUID in a standalone world; use authenticated `IdentityPolicy` when that integration is installed. Preserve saved owner/town/entity UUIDs.
3. Extract reusable coin/ledger primitives into `Mods/JEMCurrencyCore`, proposed mod ID `jem_currency_core`, artifact `jem-currency-core`, bundled with standalone Settlements using Forge Jar-in-Jar and also consumed by JEMEconomy. The master prompt explicitly permits a small shared dependency. This is **one runtime mod**, not a source directory compiled twice: identical Maven identity/compatible bounded version range permits Forge to resolve a single dependency when both consumers bundle it. CurrencyCore alone registers the five existing `jem_economy:*_coin` IDs and obtains/writes the one `jem_economy` SavedData. Preserve copper unit values, provenance, receipt semantics and deterministic `jem:town:<UUID>` account derivation. Remove those registrations/classes from Economy when moving them; Economy retains a delegating public facade for callers and full market features. Existing old Economy cannot coexist with the new core: enforce the exact migrated compatibility floor and provide matching artifacts together. Never combine balances or shade duplicate classes. Verify legacy namespace/save migration before delivery; a proposed mod ID is not proof of Forge loading compatibility.
4. Audit all Economy registrations and callers before moving source ownership. The standalone earning route must reuse existing funded Coin merchant transactions/approved quotes through an extracted adapter; do not invent starting grants, new earning recipes, creative-only progression or emerald fallback. Keep native offers/slots/restocking and existing prices. Full market/catalog/SMP event behavior is not activated by extracting settlement transactions. Exact class/file migration and adapter closure belong to the implementation plan and require evidence before compilation/startup claims; simply making metadata dependencies optional is insufficient.
5. Native Recruits remains optional and guarded. Workers adapters/dependencies are retired; historical records load as inactive. Jade and the independently usable Xaero adapter remain optional presentation integrations.
6. Use block-area expansion: exact rectangular union, price = newly added blocks × one canonical Gold Coin (currently 100 copper units). Initial territory remains 97×97, center-relative bounds ±256, 16-block separation. A clicked neighboring block may add a whole strip because the saved territory is rectangular; show the entire strip and its total cost before confirmation.
7. Keep the existing legitimate founding cost (config default 300 copper), bell interaction, 3–32-character normalized name and location restrictions. No extra GUI fee.
8. Default window 352×240 logical GUI pixels with8px screen margins; compact layout below368×256 available screen pixels. Shared widgets bind immutable server views; cached view state never becomes gameplay authority.

## Approval and completion boundaries

The user approved this contract and full implementation on2026-10-17. Production source now lives in `Mods/JEMSettlements` and `Mods/JEMCurrencyCore`; migrated full Economy1.3.0 shares the same currency implementation. The full `jem-settlements-2.1.0.jar` contains CurrencyCore and Identity dependencies. Do not install its `-slim` artifact by itself. Recruits, Jade, Xaero, full Economy, Compat and Server are not standalone prerequisites. Villager Workers is not supported.

Native Minecraft buttons/items and programmatic pixel surfaces currently render the GUI. The separate control-family candidate is produced but unapproved; remaining custom artwork briefs remain unproduced; in-game visual review and clean-instance singleplayer acceptance have not been claimed. No automated tests, visual fixtures or gameplay clients were created/launched. Ordinary dedicated-server startup is permitted solely for installing these artifacts and native pack publication. Stable profiles, MineRENT and immutable release baseline remain outside this update.

## Recovery limits

Mutations persist a bound intent before execution and save backing data before a terminal result. Known results replay without a new charge. Server sessions stamp new request IDs; completed retired-session entries may be bounded safely, while unknown old IDs cannot become new financial operations. Interrupted intents remain explicitly uncertain and block automatic resubmission with a new operation ID. They require operator reconciliation against actual ledger receipts, town state and inventory; there is no claim of atomic commits across arbitrary third-party item handlers and separate player/chunk/SavedData files. Physical production quarantines ambiguous partial delivery rather than retrying or refunding all inputs after a partial output.

## Manual entry

Install the full standalone JAR in a supported Forge1.20.1 instance. Use `/settlements`, configure the Settlements key, or place a bell. With several towns, choose one in the selector. Shift+configured key while looking at a registered NPC focuses its details after authorized selection. Shift+bell interactions retain physical storage/resident registration; ordinary villager trading remains native. `/settlements help` opens the guide. Optional worker/recruit Manage opens the owning mod's actual menu when the real NPC is loaded and within reach.

Control-family review candidate: [native1x sheet](../assets/review/control-family-r1/dark-native1x.png), [nearest4x sheet](../assets/review/control-family-r1/light-nearest4x.png), [editable source/manifest](../assets/source/control-family-r1/README.md). Eight original exports cover panel, four button states, tabs and two bars. The offline reference sheet is art-only, has no gameplay transport or world mutation, and is not an in-game screenshot. `CANDIDATE_NOT_APPROVED / SOURCE_EDITABLE / WAITING_FOR_REAL_GAME_CAPTURE`; no JAR/resource integration.

## Emerald default and optional native maps —2026-10-17

User direction: physical emeralds are primary without the actual jem_economy mod; only full Economy enables Coins. Embedded CurrencyCore is not the Economy product. The same server-selected currency applies to founding, territory, hiring, noble offers, payroll, treasury controls, quotes and native Xaero confirmation. No private waypoints or map controls are replaced.

Standalone defaults preserve numerical prices with independently named emerald configuration: founding300emeralds; normal recruit hire120/200/400/700 and weekly wages20/40/70/120 by supported rank; territory100emeralds per newly added block. No exchange rate was supplied or invented. Full Economy keeps its existing Coin cost path. Opposite-currency balances/debts and penalties are preserved separately; no starter grant or Coin→emerald conversion occurs. A town cannot be deleted while frozen funds/debts would be orphaned.

Emerald transfers prepare a durable intent, save actual inventory with an intent marker through the native player save, then commit the treasury receipt. Recovery reads the authoritative inventory store (integrated owner level.dat Player; other players playerdata) and blocks overlapping uncertain transfers. Full storage or unreadable/unsupported atomic persistence fails closed. Pending owners must log in or request a transfer for recovery. Manual world rollbacks remain outside this transaction contract. Current journal capacity100000 refuses new transfers rather than evicting replay protection. Actual crash recovery is source-reviewed, not runtime-tested.

Xaero World Map1.46.0 and Minimap26.4.2 are installed into sandbox with native embedded XaeroLib selection. Open the native map, start a held-left-click drag inside your settlement, release on the desired extension and confirm its native context-menu entry. Low-alpha fill and outline show public town bounds; only the server-verified owner can extend them. The same server journal, expected bounds, overlap checks, radius clamp and payment owner serve both built-in and native maps. Protocol8 carries map-only snapshots without opening the Settlements screen. The old JEM Server client implementation is retired so there is one interface owner.

Workers JAR/config are removed from ManagedPack/local Server inputs; old assignments stay inactive history and unmaterialized production obligations are quarantined. Real loaded vanilla professions plus recruit guards satisfy the existing job-diversity thresholds. Existing worlds are not directly edited. Stable profiles and MineRENT deployment remain untouched until an explicit release.

No automated tests or gameplay client launch were performed. Production compilation and exact artifact/publication receipts are recorded separately; native map/AI/payment/player acceptance remains pending.

## Shared food storage and actual housing inspector —2026-10-17

User direction: every bell-marked container feeds all registered residents automatically. The saved logical store has a stable town-local number (`Сундук 1` / `Chest 1`), shared by both native chest halves. Existing links migrate to shared food access; a loaded surviving half retains the number after its partner breaks. Repeated bell marking does not duplicate stores; unavailable chunks are not forced. Normal player inventory permissions and native trade ownership are unchanged.

Food selects a reachable requested dish first, then another edible item. Native hunger, per-item daily portions and serving interval stay in force; asleep, threatened or recently hurt villagers do not walk out to eat. Food in owner-authorized marked stores works even with mobGriefing disabled; loose item pickup still obeys that gamerule. Missing food no longer abandons the request for the remainder of a hunger day. Successful physical extraction opens a native chest lid for20ticks, emits paired opening/closing sound and container events, and preserves real player opener counts. Other inventory blocks keep their native visuals.

Resident details → My home opens a detached native-block 3D inspector: LMB drag rotates; wheel zooms; right-drag pans; See Inside0–10 removes a rounded camera-facing voxel cavity; Reset restores framing; My home/Example switches between real snapshot and an explicitly illustrative scene. Hover explains bound bed, exact amenity category, emitted lighting, duplicate state and category caps. Refresh requests a new authorized server snapshot. No chunks are loaded just to draw it.

The existing rule is a bound bed with a17×11×17 amenity search area, not a structural room detector. Green shows blocks in this real area, blue shows recognized amenities and red marks a missing bound bed. No roof, door, table or decoration is newly required. Shared AmenityEvidence is the sole scorer for background scans and inspection:2points/category,+1 per additional distinct block state up to category cap; home cap12. Current snapshot evidence and previously saved mood evidence are shown separately; unloaded regions are explicitly unknown. Native trades and Xaero controls remain protected. Protocol8 requires the matching Settlements client/server artifact.

The supplied isometric cutaway/exploded-view references guide inspection controls and separation, not copied art. This implementation renders actual native block models; successful compilation does not establish visual approval. Gameplay client/visual acceptance remains pending.

Jade profession is native-only: Settlements supplies town, mood, hunger and food wishes, with no duplicate profession field/line (user correction2026-10-17).

## Native recruit needs, revised house view and hiring —2026-10-17

The supplied complete brief and five labelled references live at `../assets/reference/house-recruits-2026-10-17/README.md`. Original screenshots01/02 are before evidence;03/04/05 are design direction, not after screenshots. The brief's suggested tests/launches do not override project execution policy.

All registered residents and active hired JEM Recruits use one serving selector. It orders actual available desired dishes, other good food, then bad food; dropped items participate with physical inventories and authorized shared stores. A desired dish in a reachable distant chest keeps priority over an ordinary nearby serving. Drops respect mobGriefing and pickup delay/owner locks. Sleeping, hurt and engaged NPCs wait; ordinary hunger, per-food daily portions and serving intervals remain.

Bad means the authoritative food classifier identifies harmful effects or reviewed negative mood. Such food is eligible only at actual satiety below10; it grants1satiety, with the existing negative-mood/history budget. This applies to inventory, chest and dropped food. The user request permits a narrow starvation exception to Recruits' default poisonous-potato/spider-eye/pufferfish blacklist; good blacklisted food remains denied, and no config is rewritten. Native recruit per-serving healing by food saturation modifier is preserved.

Recruits owns getHunger/setHunger and getMorale/setMoral, entity persistence, native decay, combat, ownership, equipment and groups. JEM records serving/wish history without saving duplicate hunger/morale fields; native needs are synchronized before decisions. Unhired/unassigned recruits expose native needs in Jade without being enrolled or gaining access to private settlement stores. Hired town garrisons may eat while their owner is offline; town/owner/dimension checks remain authoritative.

Jade uses reference-directed labelled rails, numeric percentages, happiness star/smiley and satiety food icon; high happiness maps to the green end. No duplicate profession line is added. The original9x9 smile icon has an editable pixel SVG under `../assets/source/jade-needs/`.

The House Viewer replaces exploded floors with a0–10 depth slider and rounded voxel cavity derived from the actual preview yaw/pitch.0 retains the full snapshot; increasing depth clips from its visible surface, including diagonal/roof obstructions, while retaining recognized functional objects and opposite interior. Filtering is cached between meaningful angle/depth/snapshot changes. Orbit, pan, zoom, reset, contained legend and default fit of the real scan boundary do not change world blocks or scoring. The existing17×11×17 bed-region/amenity owner remains unchanged.

The Recruit hire screen uses a300×223 logical-pixel wood/parchment panel, actual equipped model, drag rotation and nine icon stats. Server quote carries actual modified statistics (attack is not natively client-syncable), price, wages, affordability and reason, refreshed every20ticks while the menu is open. Fee appears once, Hire contains no repeated currency icon; wages remain every168000ticks. Native36player slots, stack/click synchronization and real command groups are retained. Infantry is a native group, not a class transformation. Emerald default/full Economy Coin ownership is unchanged. See `RECRUITS_COMPARISON.md` for15feature rows, exact installed IDs/versions/licenses and the separate GuardVillagers coverage gap.

Production compilation/source review and exact artifacts are recorded in `recruit-housing-delivery.json`; no test suite, gameplay scenario, automated multi-scale check or after screenshot is fabricated. Fresh player visual/AI/inventory acceptance remains pending. Protocol8 requires the matching Settlements client and server artifact.


## Fed resident recovery — 2026-10-17

User requests player-like gradual recovery after eating. Registered living ordinary villagers now recover1HP per80loadedticks while satiety>=90, after an actual recorded serving, only with naturalRegeneration enabled and missing health. Codex chooses4satiety cost per healed HP on the existing0–100scale. The timer persists, resets when ineligible, never catches up offline and never revives dead residents. Existing craving/food selection, professions, home/work AI, morale and starvation remain owned by their prior systems. Recruits1.15.2 already heals0.02HP per native update with hunger>=70 and keeps its native food healing; it does not receive a second healing loop.

Delivery evidence: [readability/recovery delivery](readability-recovery-delivery.json). GUI direction and limits are recorded in GUI_SPEC.md.
