# Evidence and acceptance matrix

Audit checkpoint: 2026-10-17. This is an evidence ledger and acceptance specification, **not an executable test suite, fixture or runtime scenario**. No gameplay tests, builds, launchers, static-analysis tools or development-preview fixtures were created or run in this checkpoint. The master prompt's verification goals are recorded below; reconcile exact execution permissions before implementation-stage runs. Never infer a pass from a source comment, historical report, compile success or artist export.

## Implementation evidence — approved continuation

The paragraph above describes the initial design checkpoint. In the approved implementation continuation, production-only builds were authorized by the standing project rules and performed with `-x test -x check`:

| Activity | Actual evidence and limit |
| --- | --- |
| CurrencyCore compilation/reobfuscation | `jar/reobfJar` succeeded; shared artifact SHA256 is recorded inside delivery.json |
| Migrated full Economy | `jarJar/reobfJarJar` succeeded; exact final full artifact and embedded dependency hashes in delivery.json |
| Settlements2.1.0 | `jarJar/reobfJarJar` succeeded; native GUI/common code compile and production refmap packaged |
| JEMServer adapter | `jarJar/reobfJarJar` succeeded; native SMP entry and Xaero Gold quote updated |
| Actual ordinary dedicated startup | First launch failed before world loading on JPMS split package. Corrected full-only Economy namespaces; final ordinary local server reached `Done (3.327s)!` at2026-10-17 15:13:00 local time. Confirms full-pack loading, not clean standalone/integrated gameplay or screen behavior |
| Runtime synchronization | Exact fresh full Economy/Settlements/Server bytes match ordinary Server and Client/ManagedPack; no active AutoModpack gameDir/raw-copy installation |
| Native publication | Initial `/jempack preview`: +2/~2/-2; initial publish at15:03:09. After the final Jade tooltip change, native preview +0/~2/-0 and PUBLISHED at15:14:53; journal20 has matching staged/published SHA1 `ba33232d4a87a800429a2272c2afbff959c800cc`. Exact artifacts in delivery.json |
| Independent source reviews | Corrected read-ACK pending deadlock, old session/transient rejection semantics, bounded journal retirement, worker dimension, native channel typo and known no-effect menu failure classification |
| Not performed | Automated tests, QA scenarios/fixtures, disposable server, gameplay client, stable-profile launch/mutation, MineRENT release, clean standalone/integrated acceptance, runtime visual review or art approval |

All acceptance rows below remain unaccepted unless explicitly supported above. A loaded ordinary server does not establish interrupted-operation safety, optional-mod absence, entity AI behavior or visual quality.

## Evidence obtained in this checkpoint

| Evidence | Result |
| --- | --- |
| Required `python ../tools/jem.py status` | Completed; baseline delta exists; stableSync missing/changed empty; native pack publication true, journal18 |
| Manifest identity | staged/published SHA1 `b479c75beb82aec9b80e3be94568033a3045a804` at initial status |
| Git inventory/fetch | Current `main`; origin fetched; existing unrelated Catalog, JEMRoles and runtime changes preserved; no preexisting changes to standalone design paths |
| Source audit | FEATURE_AUDIT references actual source; no runtime claim |
| Independent builder review | Read-only reviewer confirmed optional-class linkage risk, integrated authority boundary and single canonical shared money extraction; no product-code edits |
| User scope decision | Workers/Recruits gameplay remains optional and requires those mods |
| Visual references | User intent recorded; URL text retrieval did not expose actual pixels; no visual acceptance |

## Pending acceptance

Rows below define the complete acceptance goals. Only the explicit implementation-evidence table above establishes performed activities; the remaining gameplay and visual goals are NOT RUN / NOT ACCEPTED.

| Area | Required evidence / acceptance |
| --- | --- |
| Clean standalone startup | Forge1.20.1/Java17 with distributable Settlements/shared embedded prerequisites only; no Identity/Economy/Jade/Xaero/Workers/Recruits linkage failure; actual install manifest |
| Singleplayer authority | Commands/key/create and simulation on integrated server; unrelated SMP gameplay still inactive |
| Dedicated loading | Same artifact loads without client class linkage; same GUI works through authenticated snapshots/actions |
| Unsupported remote | Clear absent/incompatible message, no fake requests/success or indefinitely loading UI |
| World isolation | World exit/re-entry, restart, reconnect, player death and dimension switch preserve authoritative data; client state never leaks to another world/server |
| Save migration | Existing owner/town/entity UUIDs, Town schema6, registry identities, money accounts/provenance/receipts, payroll periods and hostility preserved |
| Multiple settlements | Zero/one/many entry; selector always shown for many; all actions affect explicit selected ID; no per-owner cap or first-town mutation |
| Founding | Real bell/name/fee/location validation, pending state, duplicate/changed request handling; exact created town opens |
| Map | Real filled territory, pan/zoom/centering, released drag, exact full-strip preview;1 Gold per new block and correct total; same geometry as saved rectangle |
| Claim transactions | Insufficient funds/overlap/stale quote/double click/replay/interrupted commit; one successful territory/payment outcome, no extra debit |
| Residents | Living list excludes confirmed dead immediately; unloaded remains living; native profession persists; removal clears only affiliation and cannot evade hostility |
| Food | Real vanilla/modded consumer-sensitive values; safe and harmful food; physical source decremented once; refusal consumes nothing;10 accepted/11th rejected by per-resident/item game day |
| Food progression | Nether/End requests unavailable before actual unlock; existing already acquired food not newly prohibited; cache invalidates on progression/data reload |
| Happiness | Separate labelled mood/satiety bars and real contribution formula; below10 actually attacks proper owner; readable states persist/reload |
| Workers absent | Honest capability state, no native class load or fake assignments/output |
| Workers present | Exact installed native API/ownership/work-area actions and physical output; no invented yield for unsupported roles |
| Catch-up | Max6 complete inactive days per activation; no forced chunks; stable obligation identity; missing input/full/unloaded output handled without spending pending goods |
| Physical transaction safety | Partial/nonconforming item-handler response, interrupted extraction/insertion/save and repeated reconciliation cannot duplicate or silently lose goods; actual recovery evidence |
| Recruits absent | Honest capability state; ordinary vanilla trading intact |
| Recruits present | Native hire/combat,7-day payroll, roster generation/arrears, third unpaid period rebellion, death/restart/late-payment persistence, actual defeat+paid recovery |
| Treasury | All five actual denominations; one ledger; deposit/withdraw provenance, destination capacity, duplicate/interrupted operations, no unbacked coins |
| Settings | Rename propagated; delete confirmation, unpaid/hostile denial, one refund, personal recruit payroll/loyalty retained, selector cleared; blocks/entities/items preserved |
| Guide/Jade/menu | Guide reflects actual capabilities/prices; every real happiness category page; Jade optional; modifier menu does not replace native trading |
| Input/layout | Mouse hits match pixels, focus/keyboard/narration/scroll/Escape/resizing, smallest supported bounds and long Russian/English; no overlapping tooltip or clipping |
| Notifications | Transition/severity dedup, reconnect behavior, persistent warnings and usable navigation |
| Assets | Layered native-size source and exact exports,1×/nearest previews, real in-game comparison against labelled references; user visual acceptance separate |
| Final distribution | Fresh required source artifacts synchronized to ordinary Server/ManagedPack; native preview/publish; matching manifest hashes and published=true; no stable/MineRENT/baseline mutation without release authorization |

Developer visual preview remains future work. It must be isolated from gameplay saved data and production mutation channels, gated as a developer facility, and explicitly authorized before creation under current project rules. A design wireframe is not an in-game preview.

Control-family review candidate: [native1x sheet](../assets/review/control-family-r1/dark-native1x.png), [nearest4x sheet](../assets/review/control-family-r1/light-nearest4x.png), [editable source/manifest](../assets/source/control-family-r1/README.md). Eight original exports cover panel, four button states, tabs and two bars. The offline reference sheet is art-only, has no gameplay transport or world mutation, and is not an in-game screenshot. `CANDIDATE_NOT_APPROVED / SOURCE_EDITABLE / WAITING_FOR_REAL_GAME_CAPTURE`; no JAR/resource integration.
