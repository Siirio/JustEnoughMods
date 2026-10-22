# Engineering v2 delivery

Requested source: `JEM_JAVA_FORGE_ENGINEERING_V2_2026-10-22.zip`, audit snapshot `9c10424f3d0f3dc10db54098ef173f8556932aa1`. Implementation started from `814ecede` and preserves the newer dedicated-only SMP and independent Rewards contracts.

The user requested all E01-E20 findings and explicitly kept the no-tests policy. Tests, ArchUnit and quality CI are excluded; no tests are added, retained or run. E20 includes opt-in non-test tool configuration and inventory-driven metrics tooling. Those tools have not been executed. Profiling and gameplay acceptance remain separate. Builds and native local artifact publication follow the current standing rules, rather than sample prompts embedded in the archive.

## Contracts

The engineering changes preserve server authority, existing registry/save/channel identities, receipt/recovery ordering and product availability. The source is compared with the current implementation, not assumed identical to the audit snapshot. The implementation must remove superseded mutation paths rather than add a parallel facade. External reflection is retained only where optional installed-mod ownership requires it.

## Finding status

| Finding | Source result |
| --- | --- |
| E01 | Completed typed ownership for parties, profiles, events, shops, legacy contracts, event schedules and boss quarantine. Concrete aggregates expose named fields/operations; snapshots freeze nested state, and command copies use typed values. NBT remains at save/wire codecs and native payload adapters. Server-thread command scopes own binding, revision/dirty commits and recovery checkpoints; generic `all/find/changed` mutation paths are removed. |
| E02 | Completed sealed operation-shaped SMP commands and named owner commands. Concrete requests dispatch their own party/event/shop operations; the legacy table/kind/payload encoding is decoded at the wire boundary, preserves wire IDs and rejects invalid combinations. |
| E03 | Completed pure profile/view reads and shared party/event admission predicates. Profile capture belongs to lifecycle commands; typed owner snapshots feed projections, and explicit shop refresh runs before projection. Reads do not initialize records, attach missing nested state or mark persistence dirty. |
| E04 | Optional Professions integration uses its typed API behind the existing presence gate. |
| E05 | Completed explicit server start/stop ownership and one player session containing request receipts, subscription and combat presentation state. Server-thread guards protect access; logout removes the player session independently of gameplay authorization. |
| E06 | Starter entitlements, delivery and native player-file access have separate owners; current independent Rewards flow and legacy receipts remain. |
| E07 | Removed the single-use campaign shell visitor indirection; native chunk guards and geometry remain. |
| E08 | Explicit race initialization/migration, detached public records, typed mutation results and scalar owner reads. |
| E09 | Selection scenes have a logical server owner; the lock query does not create state. |
| E10 | Screen transport state and party lobby state/rendering extracted; native buttons preserve the existing palette, icons and click path. |
| E11 | Description localization, wrapping and icons cached by content/layout/language; live countdowns still update. |
| E12 | Scalar snapshot accessors replace array copying for individual reads. |
| E13 | Shared preview render-state scope; entity pose and clipping remain renderer-owned. |
| E14 | Typed race inputs preserve current v9 wire encoding, rather than reverting to the older audit snapshot. |
| E15 | Removed the inactive Roles screen/menu/VFX graph; narrow bounded receive-only legacy protocol decoding remains. |
| E16 | Root Gradle producer graph, pinned ForgeGradle, explicit production inventory and declared Xaero extraction. No foreign build-directory assumptions. |
| E17 | Economy owns settlement financial receipts and durable persistence; Settlements uses the public command/result API. |
| E18 | Derived resident-by-town index replaces repeated global scans while preserving ordering. |
| E19 | Malformed/unknown settlement rows retain raw NBT; unsupported root formats remain inaccessible and preserved. |
| E20 | Non-test enforcement configuration is opt-in through `-PjemQuality=true`: Spotless, Checkstyle, PMD, SpotBugs and CPD are pinned and grouped by `qualityCheck`. Metrics source scope follows the module inventory and selected source profiles; artifact manifests use exact Gradle-produced paths/hashes. No quality/metrics tools were executed. Tests, ArchUnit and quality CI remain excluded. |

## Delivery evidence

Roles, Settlements and Server production JAR builds succeeded for the final typed-owner source. The final Server build also includes the deferred boss-binding callback correction, which resolves current party state and participants inside its command scope. No tests, linters, formatters, automated code analyzers, metrics tools or gameplay clients were run for this implementation. Compilation does not prove rendering, crash recovery or player acceptance.

Exact task artifact hashes and local publication/application evidence are recorded in [engineering-v2-artifacts.json](engineering-v2-artifacts.json). The static Atlas is regenerated from the synchronized artifacts; the same-task impact record is `Catalog/changes/records/engineering-v2-20261022.json`. Native publication and installation must match the receipt before delivery.

## Boundaries retained

- Pre-existing unrelated documentation/runtime changes remain outside this delivery commit. The initially modified VFX status entries had no content difference, so they did not prevent E15 removal.
- External compile dependencies still come from the exact managed pack; no third-party files are redistributed or APIs invented.
- No performance percentage, GL runtime acceptance, crash-recovery exercise or gameplay acceptance is inferred from source review or compilation.
- MineRENT, stable profile and immutable release baseline remain outside this implementation.
