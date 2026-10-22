# Engineering v2 delivery

Requested source: `JEM_JAVA_FORGE_ENGINEERING_V2_2026-10-22.zip`, audit snapshot `9c10424f3d0f3dc10db54098ef173f8556932aa1`. Implementation started from `814ecede` and preserves the newer dedicated-only SMP and independent Rewards contracts.

The user's explicit follow-up keeps the no-tests policy. E20 test/ArchUnit/CI rollout is excluded; no tests are added, retained or run. Formatter/analyzer execution, profiling and gameplay acceptance are also outside this delivery. Builds and native local artifact publication follow the current standing rules, rather than sample prompts embedded in the archive.

## Contracts

The engineering changes preserve server authority, existing registry/save/channel identities, receipt/recovery ordering and product availability. The source is compared with the current implementation, not assumed identical to the audit snapshot. The implementation must remove superseded mutation paths rather than add a parallel facade. External reflection is retained only where optional installed-mod ownership requires it.

## Finding status

| Finding | Source result |
| --- | --- |
| E01 | Removed `all/find/changed`; detached snapshots, server-thread command scopes, automatic revision/dirty commits and explicit owner binding. Recovery checkpoints and retention remain. Partial against the requested end state: internal event/party collaborators still use NBT working records, rather than fully typed aggregates. |
| E02 | Sealed operation-shaped SMP commands preserve wire IDs and reject invalid envelope combinations. |
| E03 | Profile capture moved to lifecycle; shared party/event admission predicates serve commands and projections. |
| E04 | Optional Professions integration uses its typed API behind the existing presence gate. |
| E05 | Request/view/combat maps share one logical server session and logout/stop lifecycle. |
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
| E20 | Automated enforcement/test/CI rollout excluded by the user's explicit no-tests instruction and current project execution rules. |

## Delivery evidence

Production JAR builds succeeded for Economy, Settlements, TwelveEyes, Roles and Server through the root graph. The final Roles/Server build completed after the defensive-record and explicit binding changes. No tests, linters, formatters, automated code analyzers or gameplay clients were run. Compilation does not prove rendering, crash recovery or player acceptance.

Exact task artifact hashes and local publication/application evidence are recorded in [engineering-v2-artifacts.json](engineering-v2-artifacts.json). The static Atlas is regenerated from the synchronized artifacts; the same-task impact record is `Catalog/changes/records/engineering-v2-20261022.json`.

## Boundaries retained

- Pre-existing unrelated documentation/runtime changes remain outside this delivery commit. The initially modified VFX status entries had no content difference, so they did not prevent E15 removal.
- External compile dependencies still come from the exact managed pack; no third-party files are redistributed or APIs invented.
- No performance percentage, GL runtime acceptance, crash-recovery exercise or gameplay acceptance is inferred from source review or compilation.
- MineRENT, stable profile and immutable release baseline remain outside this implementation.
