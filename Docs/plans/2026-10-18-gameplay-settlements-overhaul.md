# Gameplay, Settlements, GUI and integration overhaul

Requested 2026-10-18 in the attached Russian master brief. This is the delivery
worklist, not a replacement for owning module documentation or the Registry Atlas.

## Baseline and boundaries

- Audited checkout: main, initially 0b216c0c; fetched origin/main has no divergence.
- The initial tree contains 901 modified/untracked paths, principally generated
  Atlas material, CHANGELOG, Roles VFX, three Settlements files and runtime data.
  Preserve these changes; stage only this request's explicit paths/hunks.
- Initial `jem.py status`: generation 32 published; staged and published manifest
  SHA1 both 81b3913d32cf6b69c65aba0bd4c5accb3f376fd8. Release baseline unchanged.
- User requests audit first, followed by implementation without another approval.
  This brief authorizes the listed gameplay checks; no deployment to MineRENT,
  stable-profile mutation, baseline promotion or destructive save migration.
- All seven referenced screenshots are absent from the attachment directory. Only
  the 45,977-byte text is present. Reference-specific comparison is pending.
- Preserve native merchant/Xaero presentation, five currency IDs/values, existing
  UUIDs, worlds, personal map data and native AutoModpack ownership.
- Existing Settlements edits in NobleQuotePacket, RecruitRank and
  SettlementSavedData predate this request. Inspect them before any intersecting
  edit; do not include them wholesale in task commits.

## Requirement-to-owner audit

“Confirmed” below means source evidence, not reproduced gameplay. Unproven causes
remain open rather than being presented as established defects.

| Requirement | Current behavior / evidence | Cause or remaining investigation | Implementation owner and route | Acceptance |
| --- | --- | --- | --- | --- |
| 1: fuel 90 s, capacity 16, grounded loading, lift | Installed Immersive Aircraft 1.5.0; JEM EngineVehicleFuelMixin multiplies grounded consumption by .25 | Confirmed environment-dependent JEM multiplier; inspect native engine buffer, inventory, slots, networking, persistence and thrust before designing change | JEMCompat transport adapter and exact upstream bytecode; server validates all insertion paths; configurable vertical parameters | Count active seconds over land/water and low TPS, all insert gestures, reconnect, lift/horizontal/braking |
| 2: keybinds, shaders, skins | options.txt is already excluded; native server.conf main group has editable: []; Oculus and skin configs are pack inputs | Confirmed missing editable policy; shader/skin reset mechanism still requires native planner and exact installed mod inspection | AutoModpack server policy and tools manifest boundaries; preserve local bytes and native editable merge, no authentication replacement | Save keybinds/shader disabled/skin, restart/update/reconnect; compare persisted user state |
| 2: remove Carry On | carryon-forge-1.20.1-2.1.2.7.jar exists in canonical ManagedPack | Dependency and carried-entity persistence audit pending | Remove current distribution inputs only after backup; update Atlas and publication | Matching client/server absence, dependency loading, existing saves |
| 3: empty town zero | TownRecord initializes happiness 50 and needs 50/50/0/25/40; recomputeTown supplies nonzero empty defaults | Confirmed constructor and recalculation defaults create unsupported scores | TownRecord and SettlementRuntime; preserve loaded records until factual recalculation | New town all zero; existing UUIDs/storage/territory preserved |
| 4: city comfort | Existing decoration is average home/work amenity points capped 20, bonus capped 10 | No settlement-wide evidence owner; nearby resident scans cannot establish whole-town comfort | New bounded incremental town evidence through existing amenities registry; expose categories and invalidation | Add/remove sources in distant town locations, category breakdown, happiness |
| 5: decor integrations | AmenityRegistry accepts exact block rows; AmenityEvidence keys include state values | Need real registry coverage and category rules; state variants risk counting one cheap block as variety | Extend existing registry/data owner; bounded repetitions, tags and explicit exclusions; entity paintings require real entity evidence | Exact supported/unclassified IDs in Atlas; lights/pots/paintings/removal |
| 6: safety | recomputeTown uses min(100, active recruits * 25) | Confirmed territory area and actual defender validity absent from formula | SettlementRuntime + recruit adapter/config; ceil(area / configured coverage), unique living assigned defenders | Small/large/expanded town, dead/unassigned/duplicate recruits, UI required vs effective |
| 7: jobs | Counts any stored workplace against every resident | Stored non-null POI is not proof of existence/access/unique assignment; baby denominator not separated | Shared resident/workplace evidence, native POI ownership and path/access observation, server-derived reasons | Broken/blocked/duplicate station, mod POI, children, recruits, unemployed reasons |
| 8: population and animals | Community is average resident mood; no animal factor in HappinessService | No area-scaled social requirement or animal diversity evidence in current formula | Existing happiness owner + bounded town entity evidence/config; explicit social cap | One resident in large town, diverse friendly animals, removal/death/borders |
| 9: Pat Pat | Installed PatPat 1.3.1+1.20.1 Forge | No success hook found in SettlementRuntime; exact network/event audit pending | Native successful interaction adapter; ResidentMood day persisted, server validated, +8 once per villager/day | Two players, same day/restart/next day, invalid packet, cap |
| 10: children | Both SettlementApi.registerResident overloads reject babies; runtime feeds registered villagers | Confirmed enrollment boundary excludes babies; inspect native birth callback | Register born child through shared SavedData owner using registered parent town; existing meal/needs path | Birth/load/restart/grow/death; one UUID record and no adult work obligation |
| 11: Handcrafted beds | Handcrafted 3.0.6; HousingInspection tests instanceof BedBlock | Needs exact bed implementation/POI and native recruit AI audit | Tags/POI/native seam before mixin; do not teleport NPCs | AI finds bed, occupied state, sleep/wake for villager/recruit |
| 12: city isometry, no X-ray | HousingInspection captures all nonair blocks in 17x17x11 including enclosed blocks | Confirmed snapshot transmits hidden states; renderer cutaway is not information control | Server visibility sanitization shared by existing/new captures; cached bounded city snapshots and client camera | Hidden resource bytes absent from snapshot/tooltips/filters; scale, camera, LOD, cost |
| 13: expansion cost/layout/state | TerritoryQuote hardcodes 100 units per added block; Xaero uses dragging boolean/draft | Source confirms fixed price and incomplete explicit lifecycle; supplied screenshot missing | Shared server-quoted config price; native Xaero adapter state machine; responsive full price layout | Cancel/release/focus loss/reopen/ack, long five-denomination total |
| 14: Backpacked Augment Bay | Installed Backpacked 3.0.9 | Exact augment/menu/crafting API and native persistence audit required; reference 3 absent | Compare supported extension/event/menu then narrow mixin/fork; reproducible source, authored assets | Resource/augment validation, persistence, shift-click, crafting result, no duplication |
| 15: five coins | Currency registrations owned by JEMCurrencyCore | Asset/reference comparison pending; IDs/values protected | Exact Minecraft GUI/texture Taste pair, editable source and native-size review | Inventory/market/native trade/tooltip; no denomination changes |
| 16: merchant coins | Shared CurrencyCore and Economy native merchant integration exist | Need exact monetary rendering and quote/receipt chain; screenshots 5/6 absent | Native monetary cells only, server authoritative price/payment | Buy/sell/mixed denomination/modded profession/levels/restock/item conservation |
| 17: category-only search | MarketScreen has category 'all', search at 270,101 and category-aware server query | Current UI still allows global category; screenshot 7 absent | MarketScreen plus server category validation; category-panel bottom search, page reset | Category switch, clear/Escape/Backspace, pagination and JEI |
| 18: persistence/architecture | Existing UUID SavedData, town schema 7, native sync protocol 9 | Every stage must preserve one owner and bounded client projection | Source owners first, backward-readable versioned fields and backups before destructive migration | Save/restart, bounds invalidation, no second client authority |

## Ordered implementation and dependencies

1. Preserve user settings through native AutoModpack policy, audit skin source;
   back up then remove Carry On. No client gameDir copying.
2. Inspect exact aircraft bytecode; implement consumption/loading/capacity/lift as
   one inventory/engine contract, then build and publish matching JEMCompat.
3. Fix empty initialization and child enrollment, then validated resident/work
   and defender observations. These are prerequisites for meaningful formulas.
4. Introduce configured territory-scaled safety/social needs, exact employment,
   animal diversity and separate city comfort with UI explanations. Never label
   unloaded observations as zero or invent live evidence from stale records.
5. Expand data-driven decor and bed integration using actual installed registries.
6. Sanitize shared housing snapshots before building bounded cached city isometry.
7. Configured cheaper expansion and complete pointer/confirmation lifecycle.
8. Backpacked native extension after comparing exact version seams; real crafting
   inventory/results and persisted augments precede artwork integration.
9. Coin art, native trade cells and category-only market search. Missing supplied
   reference images remain an explicit visual acceptance limitation.
10. Per-stage production build/artifact synchronization/Atlas update/native pack
    publication and application; commit exact paths and push current main. Gameplay
    acceptance only after actually running the requested scenario; compilation
    does not close acceptance. No broad staging of the pre-existing changes.

## Execution evidence

- Read-only: project status, Git baseline/remotes/divergence, installed JAR names,
  source owner discovery and the source excerpts described above.
- No implementation, build, tests, game launch, migration or deployment had run
  when this plan was first shown. No requirement is marked fully delivered yet.


## Progress checkpoint — 2026-10-18

The entire overhaul is NOT delivered. Phase0 audit/plan is recorded; phase2 has a
native preference preservation change, stopped-world backup, Carry On removal and
generation33 applied with22 preference files unchanged. Skin-provider persistence
and removed-mod keybinding retention are still open investigations.

User then added screenshot-backed native player preview, SMP skin/race labels,
villager outfit variety and recruit interaction corrections. Their source is
implemented, production JARs built and local generation34 applied with exact
approved JAR hashes and no pending transaction. Free-recruit hire source/artifacts match sandbox; runtime UI comparison
remains pending. The requested general Minecraft item-generation Taste pair was
created; no coin artwork is presented as completed.

Fuel/flight implementation and master phases3–11 remain open: settlement metrics,
children/Pat Pat, city comfort, exact decor/bed compatibility, city isometry and
X-ray prevention, territory pricing/drag lifecycle, Augment Bay, coin art, native
merchant cells, category-only search and real full regression acceptance. Existing
source observations in the table are not implemented fixes or passed scenarios.

## Continuation implementation — current session

User explicitly requests real implementation of every approved requirement without further design approval. Continue existing main checkout; origin/main fetched with zero divergence at start. Earlier checkpoint source/artifact changes remain uncommitted in the shared checkout and are preserved.

In progress owners (shared files coordinated, no broad staging):
- aircraft agent: JEMCompat transport fuel/inventory/lift, then Backpacked section14.
- settlement_metrics agent: sections3–11, metrics/lifecycle/decor/PatPat/beds and bounded comfort observations.
- territory agent: section13 pricing/state/layout, then CurrencyCore native merchant monetary cells section16.
- root: city surface snapshot/view and server disclosure boundary section12, category-only market section17, item artwork section15, integration/production builds/Atlas/publication/native client application/commits/push.

Source additions are in progress, not yet built, synchronized, published or gameplay accepted. Automated tests remain prohibited. Production artifact builds are authorized by the standing project rules. Missing original seven screenshots limit direct reference comparison; do not invent their content. No MineRENT deployment, stable profile mutation or immutable baseline promotion authorized.
