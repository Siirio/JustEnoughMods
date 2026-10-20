# GUI specification

Status: approved contract implemented by `Mods/JEMSettlements/.../client/SettlementScreen.java`, `SettlementMap.java` and server projections/actions. Native controls/items currently supply materials/icons; control-family ART_BRIEFS candidate sprites are exported but not integrated or approved. `GUI_STATE_MACHINE.md` owns request/reply and lifecycle behavior. All dimensions below are **logical GUI pixels**, not framebuffer pixels; all labels are translatable Minecraft text. Runtime visual acceptance is pending.

## Native page art integration (20 October 2026)

Current owners: SettlementScreen, SettlementDashboardArt, SettlementPageArt. The37 Aseprite page/state candidates in Docs/art/settlements-pages-2026-10-20 supply reusable material and semantic icons. Sample text, numbers, portraits and positions remain reference compositions. The live Screen is the sole runtime layout owner; full-page previews are never drawn behind unrelated live controls.

Production mapping: native/04-residents.png is copied unchanged to assets/jem_settlements/textures/gui/pages/materials.png. Material regions are masonry(6,215,98,23), clean paper(314,114,64,48), wood(5,4,390,29), button(319,249,80,20). Fixed corners and repeating native texels preserve pixel density. Native icon-*.png files are copied unchanged into that directory at16x16. Editable Aseprite originals remain in the owning art directory. Existing sculpted rims, blue/gold selection, lamps and vines remain. No generated texture or reference screenshot ships.

| Candidates | Live owner |
| --- | --- |
|01 directory /02 founding | selector /creation: real directory, bell, quote and request outcomes |
|03 overview | cityWidgets: treasury, taxes, transfers, comfort, rename/delete |
|04 residents /05 villager /06 recruit /07 golem /compact | residentRows, residentsDashboard, residentPane, residentDetail: real snapshot and actual loaded entity |
|08 happiness /09 complaints/distribution | happinessRows: actual metrics and shared scrolling viewport |
|10 chests /11 contents | chestRows, chestContents: real server slots and explicitly unknown/unloaded contents |
|12 home /13 delete /14 remove /15 transfer /16 coordinates | info, ask, confirmationWidgets: wrapped live messages and initially focused Cancel |
|14 guide chapters | guideWidgets: localized current chapter, previous/next/back |
|loading /unsupported /pending /uncertain /empty /unloaded | Existing mode, request uncertainty, roster-empty and InventoryKnown state owners |

All routes share authored controls at actual widget bounds, dynamic text, keyboard focus, clipped scrolling and the existing request state machine. Summary cards use two columns below440 content pixels; that same height defines the scrolling origin. Shared roster width determines draw and wheel regions. Resident mode requires300 vertical logical pixels; extreme scale shows the existing localized instruction instead of overlapping actions. Content-sized directory/chest retain separate thresholds.

Distinct icons sit beside real happiness, satiety, housing/home, safety, work, community, comfort and resident values. Foods, currency and loaded entities retain native owners. Search filters on typing and automatically sorts names; All residents/By name remain removed. Recruit detail and indebted roster preserve the exact payroll request. Native manage/feed/home/remove/location actions remain authoritative. Money delegates to SettlementPriceRenderer/CurrencyCore; no second denomination loop. Guide/directory access is in the sidebar, without crossed header buttons.

Inspected originals: GUI+05/+06/+01 and rejected runtime-corrections-2026-10-19/02.png. Applied bounded masonry seams, layered wood edges, and distinct semantic silhouettes. This is source integration, not a game screenshot or player acceptance. Build, distribution and native publication belong to the parent delivery; runtime GUI-scale/hover/readability acceptance remains pending.

## Layout and shared components

Codex layout proposal: 352×240 centered window with 8-pixel safe margin. Header `(8,8,336,24)`; sidebar `(8,36,88,196)`; content `(100,36,244,196)`. Content header24, footer28, remaining144 scrolling pixels. Coordinates are window-relative. Item icons16×16; icon targets20×20 minimum; regular buttons20 high. Native font9 high with12-pixel line spacing. Rows32 high; text wraps rather than scaling below native font size.

The normal352×240 window plus8px screen margins requires368×256 available logical screen pixels. Below either threshold, use screen-safe window bounds (screen width/height minus16), top section picker instead of sidebar and one column. Narrow selector24 high; detail portrait collapses to48×64 before text; footer wraps vertically with its own reserved height. Each content scroll viewport clips both rendering and mouse regions. If no useful content fits at extreme GUI scale, show a localized scale instruction with Close; never draw overlapping active targets.

This structural wireframe is an asset-placement mockup, **not an in-game screenshot or approved art**:

```text
352 logical pixels
+-------------------------------------------------------------+
| 16px emblem  Settlement name       Switch*  Help  Close      | 24
+----------------+--------------------------------------------+
| Map            | Section title / current filter             | 24
| Residents      +--------------------------------------------+
| Happiness      |                                            |
| Food           | Live section viewport                      |144
| Workers        | (scroll/clipped; no text baked in PNG)      |
| Recruits       |                                            |
| Treasury       +--------------------------------------------+
| Settings       | Real quote / reason          Primary action| 28
+----------------+--------------------------------------------+
* only when multiple settlements; 8px outer inset, 4px gutters
```

| Component | Geometry/rendering | Interaction and accessibility |
| --- | --- | --- |
| SettlementPanel | 16×16 nine-slice source, fixed3 corners; content inset8 | Non-interactive; no false button seams |
| SettlementButton | 16×20 source nine-slice, corner3; native text centered | Default/hover/pressed/disabled + programmatic focus border; Space/Enter; disabled reason narration/tooltip |
| IconButton | Icon16 in target20×20 | Tooltip with action + reason; focusable; no hover-only essential action |
| SettlementTabs | Sidebar row20 or compact picker24; selected overlay | Scroll navigation when needed; selected label narrated; no Overview |
| CoinAmount | Actual registered item icons16, localized counts | Five denominations, checked integer decomposition, wrap groups; detailed amount tooltip |
| StatusBar | Source track16×8 nine-slice; width≥48; fill inner height4 | Separate Happiness and Satiety labels; actual0–100; tooltip numerator/denominator; icon + text indicate criticality |
| VillagerPortrait | Preferred64×112 in details; resident-row icon16 | Live loaded entity only; stable camera/yaw per widget; unloaded uses explicitly labelled role icon, never a fake villager |
| ResidentRow | Height32; portrait16; name/role; two48×8 bars when width permits | Whole-row Open with one clear target; search/filter/sort outside scroll content |
| WorkerRow / RecruitRow | Height40; icon16; two text lines + state | Open detail first; destructive action never hidden in row click |
| FoodRow | Height32 or48 when wrapped; item16; effect/count columns | Native item tooltip + true villager effects; stock may be unknown |
| MapCanvas | Main viewport at least128×96 | Pan/zoom anchored under cursor; drag capture released on mouse-up, Escape, blur, screen change and resize |
| MapClaimCell | Geometry calculated from current rectangle/quote | Same world-to-screen transform for hit, fill and outline; no raster terrain stamping |
| ConfirmationDialog | Width224, dynamic height; button footer20 | Cancel initially focused for destructive actions; Escape cancels; confirm separate target |
| SearchField | Height20, width≥80 | Standard edit focus, selection/caret, narration; no global hotkeys while typing |
| FilterControl | Height20, bounded dropdown/scroll | Valid server-backed options only; filters local over loaded rows or explicit server query |
| Tooltip | Nine-slice16, fixed3 corners; text inset4 | One tooltip at a time, clamp to screen, wrap long Russian; keyboard-focus equivalent |
| NotificationToast | Width160, height32/48; icon16 + title/body | Severity by icon/text and color; deduplicate per state-machine contract |
| BookPage |128×160 page surface, content inset8 | Chapters/previous/next/back; paragraphs reflow with locale |
| EmptyState / LoadingState | Available content bounds, symbol16 + wrapped text | Empty state offers valid Create/Link action; loading blocks mutation and has Close |

Display proposal for bar colors: 0–9 red,10–34 orange,35–64 yellow,65–100 green. These are presentation bands, not new simulation thresholds. Hunger UI uses **Satiety / Сытость** (higher=healthier), from `ResidentMood.satiety`; a “Hunger” label must explicitly invert fill and severity if used. Never show hunger100 as green while satiety0.

## Screens and exact data/actions

For every action below: source fields come from authorized server views, refresh follows the state-machine event table, authority is logical server unless explicitly “local”, success requires matching result/revision, rejection stays on current context with localized reason and refreshed quote. Unsupported remote servers expose only compatibility/Close; they cannot execute actions. Local filter/navigation controls require no gameplay mutation.

| # / screen | Live fields and source | Interactive elements, validation, success/failure |
| --- | --- | --- |
|1 Entry | Handshake/capabilities, authorized owned count | Configurable key and `/settlements` -> directory request;0 creation/1 map/many selector. No fake zero while loading |
|2 Selector | Town UUID/name/dimension, living count, happiness and real alerts | Search, scroll, name/dimension sort local; Open validates current owner/ID then loads Map; Create opens3; remembered ID is highlight only; deletion never inline |
|3 Creation | Name, looked-at/placed bell and dimension, initial97×97 bounds, authoritative founding cost/restrictions | Name edit, preview, Create, Cancel; normalized3–32 name, unique name, actual bell≤16 blocks, overlap/separation/loaded dimension/funds checked server. Existing300-copper default retained. Pending blocks duplicate submit; failure inline; ACK opens exact new UUID |
|4 Shell | Selected UUID/name, directory count, active section | Eight section buttons, switcher when multiple, Help, Close; local navigation, loading changed section as needed. No summary-card dashboard |
|5 Map | Authoritative bounds/dimension/center/player where available, explored map samples, selected quote and available funds | Zoom±, drag, Center settlement/player, select neighbor, Expand. Server verifies same dimension, bounds±256, adjacency/union, separation16, ownership/revision and Gold price. Preview **entire added strip**, label1 Gold/block plus added-block count and total; debit only after valid commit. Unsupported/unexplored terrain remains parchment/unknown; centering never teleports or force-loads chunks |
|6 Residents | Living registered ResidentMood UUIDs, names/professions/role, actual mood/satiety/craving/hostility | Search, name/profession/hunger/happiness sort; All/profession/role/Hungry/Unhappy/Working; In danger only if real state exists. Thresholds server-specified; row opens7. Unloaded living residents retained with unknown transient data; dead removed on confirmed death |
|7 Resident details | Same resident view, native profession distinct from assignment, needs/modifiers, home/work reference, actual recent foods and per-item day counts | Feed guidance opens food/source context; My home opens the server-derived native-block3D inspector: real bound-bed17×11×17 region, orbit/zoom/camera-facing rounded0–10 cutaway, hover scoring evidence and clearly labelled Example switch, never teleport; Work opens10 when capability exists; Remove confirmation checks ownership, membership, hostility/debt before clearing affiliation. No kill/entity deletion/native-profession conversion |
|8 Happiness | Actual settlement score and housing/food/safety/jobs/community/decoration/tax contributions, individual mood | Contributor click opens its guide page/related section; critical-resident row opens7. Six actual categories each documented; Tax is deduction, not invented seventh happiness type. Explain weighted settlement value vs individual mood |
|9 Food | Food registry/policy in villager context, physical linked-container stock, active requests, dimension-access and serving counters | Search/item sort, filter acceptance/demand/unlocked, storage list, food detail tooltip, focus resident. Link/enable food storage only via validated real block interaction. No virtual inventory or item spawning. Unknown/unloaded quantities use text, not zero. Request restriction does not forbid eating already obtained item |
|10 Workers | Native Workers ownership/job/work area/state/output/destination, existing production obligations | With Workers: select actual worker, inspect native assignment and supported native management actions; Assign/Change/Remove only where exact native API is verified. Locate focuses map if known. Without Workers: explanatory unavailable state, no assignment buttons. No made-up yields or conversion of all villagers to workers |
|11 Recruits | Native recruit entity/role, wage config, next due gameTime, unpaid period IDs, persisted loyalty/defeat | With Recruits: details, payroll funding/settlement action, authorized native hire/buyback, supported native management. Server verifies employer, roster generation, actual due periods/funds and native hire requirements. Seven-day cycle/three misses unchanged; no forced payment for a future not-yet-due cycle. Without Recruits: unavailable state |
|12 Treasury | Town account balance, spendable wallet/physical split, upcoming payroll, backed transaction history/receipts | Five-denomination amount input, Deposit, Withdraw; explicit town/request IDs, valid amount/source/destination/capacity/funds. No optimistic balance mutation; history only ledger-backed facts, not generated UI rows. Missing canonical provider is an error, never emerald fallback |
|13 Settings | Name, actual owner/dimension/location/bounds, existing tax/management rules | Rename edit/Save/Cancel; normalized name uniqueness/revision check. Tax selector only existing0/5/10/15 supported values if authorized endpoint is implemented. Delete opens18; unpaid payroll and unresolved consequence checks; refund once, unassign residents, preserve recruits' personal payroll/loyalty, clear selection/overlays after ACK. Physical world stays intact |
|14 Guide | Chapters from implemented authoritative rules/config/capabilities | Chapter list, page previous/next, Back; `/settlements help` opens GUI rather than only giving written book. One page each housing/food/safety/jobs/community/decoration, plus tax explanation. Food/fatigue, founding, map, treasury, optional workers/physical output and recruits/payroll/rebellion; absent integrations explicitly explained |
|15 Jade overlay | Same registered-resident projection scoped to looked-at entity | Name/profession/membership, two labelled bars and current refusal/status; optional Jade only. No forged settlement fields for unregistered villager; no standalone dependence on Jade |
|16 World menu | Looked-at real supported entity, owner/capabilities | Deliberate modifier/key opens View/Work/Role/Remove as applicable; entity UUID/reach/ownership checked for mutation. Ordinary right-click preserves native trade. Escape returns game; no stale entity-ID reuse |
|17 Notifications | Authoritative event/condition transitions and operation ACKs | Icon/title/one-line reason; section action in accessible list; dedup key and severity behavior in state machine. Persistent conditions remain visible in owning section |
|18 Shared states | Loading/empty/success/recoverable error/failed action/confirmation/destructive/unsupported | Retry only safe reads or original-operation reconciliation; Close/Back always available. Destructive dialog names exact town/entity and consequences. Disabled buttons explain reason; no fake success on click |

## Guide and localization contract

String keys under `jem.settlements.gui.*`, `jem.settlements.reason.*`, `jem.settlements.guide.*`; English and Russian must have equal coverage. Formatting uses Components and typed arguments, not server-generated untranslated English. Item/profession/dimension names use native translations when available; user names are literal sanitized text. Long text wraps; no abbreviating away price, debt or destructive consequences.

| Situation | Russian | English |
| --- | --- | --- |
| Unsupported | Этот сервер не поддерживает Поселения. | This server does not support Settlements. |
| No Gold | Недостаточно золотых монет. | Not enough Gold Coins. |
| Unit price | За каждый новый блок: 1 золотая монета. | Each added block costs 1 Gold Coin. |
| Fatigue | Житель больше не хочет эту еду сегодня. | This resident does not want more of this food today. |
| Full output | Работники не могут доставить предметы: хранилище заполнено. | Workers cannot deliver items: storage is full. |
| Unloaded entity | Житель сейчас вне загруженной области. | This resident is outside the loaded area. |
| Pending outcome | Результат уточняется… | Checking result… |
| Optional workers absent | Для работников нужен мод Workers. | Workers requires the Workers mod. |
| Optional recruits absent | Для рекрутов нужен мод Recruits. | Recruits requires the Recruits mod. |

No debug registry IDs/packet/null jargon in ordinary text. Advanced diagnostics may record exact IDs in logs. Guide values are bound to canonical rules, not independently copied balance constants.

Control-family review candidate: [native1x sheet](../assets/review/control-family-r1/dark-native1x.png), [nearest4x sheet](../assets/review/control-family-r1/light-nearest4x.png), [editable source/manifest](../assets/source/control-family-r1/README.md). Eight original exports cover panel, four button states, tabs and two bars. The offline reference sheet is art-only, has no gameplay transport or world mutation, and is not an in-game screenshot. `CANDIDATE_NOT_APPROVED / SOURCE_EDITABLE / WAITING_FOR_REAL_GAME_CAPTURE`; no JAR/resource integration.

## Shared storage and housing inspection follow-up (2026-10-17)

All bell-marked stores are communal resident food sources; the Food row explains shared access instead of offering a disabling toggle. Persistent town-local `Chest N` labels identify one logical store, including both halves and its surviving half. These labels are world projections, independent of the management screen.

Housing inspection lives within resident details. Back returns to the same resident; My home/Example changes only the displayed evidence; wheel zoom and held LMB orbit operate inside the bounded viewport. See Inside0–10 removes a rounded voxel cavity from the actual preview-facing surface.0 preserves all blocks; RMB pans and Reset restores framing. Refresh is an authenticated read snapshot, never a gameplay action. Actual and illustrative content remain distinct. No automatic camera teleport, entity tick, block placement or synthetic house-validity score. Green means actual scan-region structure, blue recognized amenities, red missing bound bed; hover explains exact category, working light, repeated states and caps. Unknown unloaded blocks never become red requirements. Native resource-pack block models provide the scene, with no generated house PNGs.

Jade shows native profession only; the Settlements provider owns mood/satiety/town/food wishes and adds no second profession line.

## Recruit and Jade revision (2026-10-17)

Native hire menu slots remain its original36player slots, never reinterpreted as recruit equipment. The presentation reserves a dark equipped-model portrait, two stat columns, separate fee/wage summary and the actual native command-group selector. Server-projected eligibility/reason/stats are display data; native hire remains authoritative. No fake class/training control. Source-owned parchment is reused with native wood/items; user direction is not runtime artwork approval.

Jade: compact mood gradient/star/smiley and satiety fill/foodicon use actual authoritative values and percentages. Only the native Jade provider shows profession. Native recruit morale/hunger are bridged, never duplicated by GUI or metadata.


## Recruit readability and material correction — 2026-10-17

User requests the existing hire menu corrected, using the supplied style reference for arrangement, reused icons, appropriate visual elements, and wood/metal/vegetation. Reference classification: [supplied screenshots](../assets/reference/recruit-layout-2026-10-17/README.md).

Codex implementation: hire frame252×223 logical pixels (previous300×223); native36slots and coordinates unchanged, equipped portrait and native group/action preserved. Two80pxstatcolumns, white values over dark recessed wood, compact fee and seven-day wage cells. Full stat explanations remain tooltips. Shared SettlementMaterials uses Minecraft spruce planks, fixed metal corners and vanilla tinted vines. Settlements navigation uses actual item icons and the same controls. No new directories, NPC roles or invented state from the reference.

Jade happiness fills left-to-right from red/low through yellow to green/high and shrinks when its synchronized value decreases. Remove moving star, retain smile/actualpercent. Payroll countdown displays one decimal game day (24000ticks), recurring period remains7game days.

No runtime visual acceptance is claimed. Native GUI scaling, Cyrillic labels and actual item interactions await user review. No tests, linters or gameplay clients launched.


## Current sandbox navigation/layout — 2026-10-17

Explicit user direction replaces Map/Food/Recruits tabs with Residents/Happiness/Chests/Treasury/Settings; Recruits and Villagers are roster filters. Default384×264logical window,100px icon sidebar and coherent36px wood-framed NPC cards, recessed content, white names and semantic happiness/food rails replace the prior wallpaper-like shell. Native name components preserve localized roles, custom names and optional Villager Names names. Unknown unloaded recruit needs never render0percent. Recruit filter retains due-payroll action and native management; ordinary villagers retain House Viewer.

Selected chest: Back/title, coordinates or actual queued-meal count, passive native18pxslot grid, pagination and Everyone eat button. UI page capacity shrinks with viewport and never exceeds54; server accepts arbitrary offset. No inventory take/move/copy action exists. A too-small viewport shows scale guidance rather than overlapping slots. Read-only storage selection and actual feedall requests retain existing session/acknowledgement/replay handling. No client-side success simulation.

User reaffirms the supplied second reference for cohesive wooden panels, semantic icons and compact arrangement, while rejecting the previous implemented GUI. This source candidate is not visual acceptance; no game capture was taken.
