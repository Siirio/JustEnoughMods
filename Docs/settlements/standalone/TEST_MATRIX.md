# Evidence and acceptance matrix

Audit checkpoint: 2026-10-17. This is an evidence ledger and acceptance specification, **not an executable test suite, fixture or runtime scenario**. No gameplay tests, builds, launchers, static-analysis tools or development-preview fixtures were created or run in this checkpoint. The master prompt's verification goals are recorded below; reconcile exact execution permissions before implementation-stage runs. Never infer a pass from a source comment, historical report, compile success or artist export.

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

All rows below are **NOT RUN / NOT ACCEPTED**. They define evidence needed, not invented results.

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
