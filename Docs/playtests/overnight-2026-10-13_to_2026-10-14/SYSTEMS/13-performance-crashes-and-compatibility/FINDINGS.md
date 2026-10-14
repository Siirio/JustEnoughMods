# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0001

**HIGH — Local server and native test client do not contain the current five-system runtime**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T18:59:56.852635+00:00; latest: 2026-10-13T20:30:58.587194+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Inventory exact JAR filenames and SHA-256 independently on all three surfaces.

**Expected player experience:** A successful local connection exercises current Economy, professions, races and settlement behavior.

**Actual behavior and impact:** Canonical has Economy 1.2.0 and Settlements 2.0.0; original server has 1.0.0 for both. Test active has neither of those modules, Identity, Professions nor Roles. Successful startup of that client would not prove newest gameplay.

**Established cause / stated uncertainty:** Confirmed artifact drift; a1b04b4 source was pushed while these older runtime surfaces were retained. This is deployment evidence, not a live MineRENT observation.

**Proposed owner correction and native reuse:** Build from source, install in isolated local server, publish canonical pack locally through native AutoModpack, verify committed active generation and hashes.

**Acceptance:** Matching client/server module hashes and negotiated channels; genuine in-world evidence.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [EVIDENCE/traces/artifact-baseline.json](../../EVIDENCE/traces/artifact-baseline.json)

## JEMQA-20261014-0002

**HIGH — JEMCompat build still requires deliberately removed BackToBed**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T18:59:56.855081+00:00; latest: 2026-10-13T20:30:58.616372+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Run gradlew jar with JEM_INSTANCE pointing to Client/ManagedPack.

**Expected player experience:** Fresh source builds against the canonical current distribution without restoring removed mods.

**Actual behavior and impact:** jar fails during configuration: Expected one installed mod matching BackToBed-*.jar, found 0. No BackToBed symbol remains in JEMCompat source.

**Established cause / stated uncertainty:** Five whys: build cannot configure -> installedMod throws on zero matches -> build.gradle still resolves BackToBed -> removal deleted runtime dependency and compatibility source but retained compileOnly -> build dependency owner was not updated with the removal.

**Proposed owner correction and native reuse:** Remove only unused compileOnly declaration; preserve /home server ownership and native singleplayer behavior.

**Acceptance:** Reobfuscated JEMCompat jar builds from canonical pack; no BackToBed runtime reintroduction; gameplay behavior unchanged.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMCompat/build.gradle:62](../../../../../Mods/JEMCompat/build.gradle)
- [build/overnight-qa/JEMCompat-build.log](../../../../../build/overnight-qa/JEMCompat-build.log)
- [Docs/DEVELOPMENT.md](../../../../DEVELOPMENT.md)
- [CHANGELOG.md](../../../../../CHANGELOG.md)

## JEMQA-20261014-0005

**HIGH — Current Settlements source cannot compile its town guidance**

Status: **IMPLEMENTED_UNVERIFIED** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:07:22.371200+00:00; latest: 2026-10-13T20:30:58.678104+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Build JEMSettlements from current main via existing wrapper.

**Expected player experience:** Fresh matching Settlements 2.0.0 builds for local player QA.

**Actual behavior and impact:** compileJava fails twice at SettlementRuntime lines 144/146: cannot find symbol BlockPos.

**Established cause / stated uncertainty:** Five whys: compilation fails -> guideResidentsHome references unresolved BlockPos -> net.minecraft.core.BlockPos import is absent -> new guidance code was committed without a fresh compile -> source/build handoff lacked verification, so installed 1.0.0 could not demonstrate 2.0.0 behavior.

**Proposed owner correction and native reuse:** Add the single missing import; preserve all guidance expressions and native AI ownership.

**Acceptance:** Reobfuscated source build succeeds and matching runtime starts; actual home-guidance replay is still required.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:villager`

**Source/evidence links:**

- [Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/runtime/SettlementRuntime.java](../../../../../Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/runtime/SettlementRuntime.java)
- [build/overnight-qa/JEMSettlements-build.log](../../../../../build/overnight-qa/JEMSettlements-build.log)
- Canonical lookup for `minecraft:villager`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0011

**HIGH — ServerCore rejects the local spawn-category configuration during initialization**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:22:01.640950+00:00; latest: 2026-10-13T20:30:58.847863+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Start exact isolated clone with original Server config and ServerCore 1.5.2; inspect native config loader error.

**Expected player experience:** Authored ServerCore performance/spawn configuration loads with its intended category contract.

**Actual behavior and impact:** Real isolated runtime rejects config.yml category cave_creature as an invalid enum and reports configuration loading failure. This is a configuration/lifecycle finding; default fallback effects and gameplay spawn rates have not been observed.

**Established cause / stated uncertainty:** The invalid category value is demonstrated by runtime. Whether custom categories are renamed, absent, or not yet registered at this lifecycle point needs exact mod/native-loader inspection. Do not claim a deeper cause until demonstrated.

**Proposed owner correction and native reuse:** Inspect exact category registrations and ServerCore initialization order; preserve locked activation-range.enabled=false and native spawn balance. Repair the owning configuration/lifecycle only after evidence.

**Acceptance:** Configuration loads without fallback/error and exact categories/rates remain intended; separately measure spawn/performance.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [EVIDENCE/logs/isolated-startup-excerpts.txt](../../EVIDENCE/logs/isolated-startup-excerpts.txt)
- [Server/config/servercore/config.yml](../../../../../Server/config/servercore/config.yml)

## JEMQA-20261014-0031

**BLOCKER — Stale clone BackToBed blocks the newly synchronized client at Forge registry handshake**

Status: **RECOVERED_SETUP** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T21:04:33.180632+00:00; latest: 2026-10-13T21:30:19.643096+00:00. Recheck: JOIN_SUCCEEDED at02:11:48 after removing stale clone BackToBed and unintended Claims.

**Mode/environment:** Actual native updated test client / dedicated clone Forge registry negotiation

**Attempt/reproduction:** Connect updated test jem to127.0.0.1:31605 after native commit56mj6t5; actual disconnect screenshot lists BackToBed1.1.0.

**Expected player experience:** The correctly updated local test client can negotiate registries with its intended matching clone.

**Actual behavior and impact:** AutoModpack reports Active projection is up to date, then Forge disconnects with missing backtobed:magical_returner. BackToBed1.1.0 remains only in the clone copied from old Server. All same-filename shared JAR hashes match canonical; broader filename/ID parity is checked separately. No login or in-world case is reached. After graceful restart and both clone-only removals, 242 shared mod IDs are byte-identical; the intended remaining server-only mods are Flan, ServerCore and ServerLogin. Actual join succeeded02:11:48 and native first registration later completed. This was recovered setup, not a current gameplay blocker.

**Established cause / stated uncertainty:** Five Whys: Forge cannot sync item -> client lacks magical_returner -> canonical intentionally removed BackToBed -> clone preparation retained old server-only JAR -> installation helper added current shared deps without applying approved removals. This is operator clone-parity setup failure, not an AutoModpack download failure.

**Proposed owner correction and native reuse:** After save/stop and confirmed JVM exit, move only the stale clone JAR to ignored backup; original Server and canonical remain unchanged. Restart clone and reattempt actual client join.

**Acceptance:** Real Forge registry negotiation succeeds, no BackToBed rejection; legitimate authentication then determines in-world availability.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `backtobed:magical_returner`

**Source/evidence links:**

- [EVIDENCE/screenshots/actual/006-registry-parity-backtobed.png](../../EVIDENCE/screenshots/actual/006-registry-parity-backtobed.png)
- [EVIDENCE/traces/clone-backtobed-parity-repair.json](../../EVIDENCE/traces/clone-backtobed-parity-repair.json)
- [Docs/releases/2026-10-13-two-stage-atlas-and-implementation.md:24](../../../../releases/2026-10-13-two-stage-atlas-and-implementation.md)
- Canonical lookup for `backtobed:magical_returner`: [Catalog/registries/items/backtobed.json](../../../../../Catalog/registries/items/backtobed.json) (registry identity must be respected).

## JEMQA-20261014-0040

**MEDIUM — Graceful cloned server saves and closes ports but idle non-daemon scheduler prevents JVM exit**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-14T00:15:21.135460+00:00; latest: 2026-10-14T00:15:21.135471+00:00. Recheck: NOT_FIXED; stopped ownedPIDterminated only after save/port/thread checks.

**Mode/environment:** Actual owned clone lifecycle, no production/stable server

**Attempt/reproduction:** Issue save-allflush thenstop to owned clone; wait grace; inspect process/listeners and nativeThread.print.

**Expected player experience:** After native save/stop the isolated JVM terminates and releases process memory.

**Actual behavior and impact:** All dimensions saved05:03:59; Framework unload completed05:04:00. Runner120second grace expired with JVM22884 alive. No31605/31606listeners or ServerThread; sole application non-daemon pool14-thread1 parked inScheduledThreadPoolExecutor queue, DestroyJavaVM waiting. Exact allocating mod/owner not established.

**Established cause / stated uncertainty:** A non-daemon scheduled executor remains unclosed after server lifecycle shutdown. Thread ownership beyond pool14 is unknown; do not guess culprit from generic pool numbering.

**Proposed owner correction and native reuse:** Trace allocation/lifecycle of remaining executor and close it in owning server stop handler; no broad process-kill fallback presented as product fix.

**Acceptance:** Native stop exits without external termination; all saves remain durable; idle scheduler absent.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [EVIDENCE/traces/stopped-server-thread-summary.txt](../../EVIDENCE/traces/stopped-server-thread-summary.txt)
- [EVIDENCE/logs/merchant-native-before-after.txt](../../EVIDENCE/logs/merchant-native-before-after.txt)

## JEMQA-20261014-0041

**HIGH — QA installer selected stale full JEM Server after building only slim jar**

Status: **VERIFIED_IN_GAME_AWAITING_USER_APPROVAL** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-14T01:27:00.307195+00:00; latest: 2026-10-14T02:11:27.484103+00:00. Recheck: PASS fresh+ordinaryfullbuild/refmap/cloneDone/nativegen3/worldjoin/currenthomevalidation; Claimsnotinstalled.

**Mode/environment:** Actual isolated setup/current source comparison

**Attempt/reproduction:** Run jar in module applying embed.gradle; installer selects version-ending full jar excluding slim implicitly.

**Expected player experience:** Current source must actually execute in the isolated environment.

**Actual behavior and impact:** Client/clone/canonical SHA parity held but full JEM Server timestamp16:01 predates build23:57. Source/compiled class has home, installed class lacks it; actual /home absent. Prior JEMServer gameplay cannot represent newest source.

**Established cause / stated uncertainty:** embed.gradle changes jar classifier to slim; full classifier-empty artifact is owned by jarJar/reobfJarJar. Operator build/install selected old full output despite successful slim reobf.

**Proposed owner correction and native reuse:** Build explicit full artifact, inspect current owner class, stage via official native route and qualify earlier observations. Document correct per-module packaging contract.

**Acceptance:** Fresh full artifact contains current HomeTravel registration, native client/clone match, actual /home reaches current validation.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMMenuSupport/embed.gradle](../../../../../Mods/JEMMenuSupport/embed.gradle)
- [Mods/JEMServer/build.gradle:43](../../../../../Mods/JEMServer/build.gradle)
- [build/overnight-qa/JEMServer-build.log](../../../../../build/overnight-qa/JEMServer-build.log)
- [EVIDENCE/screenshots/actual/061-non-op-home-unknown-command.png](../../EVIDENCE/screenshots/actual/061-non-op-home-unknown-command.png)
- [EVIDENCE/traces/full-server-build-recheck.json](../../EVIDENCE/traces/full-server-build-recheck.json)
- [EVIDENCE/traces/generation3-native-commit.json](../../EVIDENCE/traces/generation3-native-commit.json)
- [EVIDENCE/screenshots/actual/066-current-home-missing-respawn-rejection.png](../../EVIDENCE/screenshots/actual/066-current-home-missing-respawn-rejection.png)

## JEMQA-20261014-0042

**BLOCKER — Fresh full embedded JAR omits generated Mixin refmap and fails before world startup**

Status: **VERIFIED_IN_GAME_AWAITING_USER_APPROVAL** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-14T01:41:44.169878+00:00; latest: 2026-10-14T02:11:27.484130+00:00. Recheck: PASS fresh+ordinaryfullbuild/refmap/cloneDone/nativegen3/worldjoin/currenthomevalidation; Claimsnotinstalled.

**Mode/environment:** Actual isolatedclone startup; no production/stable client

**Attempt/reproduction:** Build embedded modulefull via jarJar and inspect configuredrefmap; start isolatedclone.

**Expected player experience:** Fresh current-source full server starts using its existing mapped injection contracts.

**Actual behavior and impact:** Fresh full4532afa has current HomeTravel but no jem_server.refmap.json; slim had map. Clone failed06:31:48 beforeworld on SolidBoundaryMixin collideBoundingBox with No refMap loaded. A subsequent successful cached compile also left slim/full maps absent; success exit alone insufficient.

**Established cause / stated uncertainty:** MixinGradle attaches generated map to jar; full assembly needs explicit metadata inclusion. Plugin compileJava before action deletes rawmaps; incremental javac may not regenerate annotationprocessor metadata and after action removes renamed map. Missing maps were not declared compiler outputs, so later UP-TO-DATE retained absence.

**Proposed owner correction and native reuse:** Reuse generated slim refmap only in fullassembly, track plugin native refMapFile/outTsrgFile outputs, disable incremental compilation for these Mixin modules and fail packaging on absent map. Keep classes/nativeMCPmapping/embeddedlibrary ownership unchanged.

**Acceptance:** Fresh and ordinary repeated fullbuild contain equal generated map/SolidBoundary mapping, then clone/world starts and nativeclientcommit/replay succeeds.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMMenuSupport/embed.gradle](../../../../../Mods/JEMMenuSupport/embed.gradle)
- [EVIDENCE/logs/full-server-missing-refmap.txt](../../EVIDENCE/logs/full-server-missing-refmap.txt)
- [EVIDENCE/traces/independent-review.md](../../EVIDENCE/traces/independent-review.md)
- [EVIDENCE/traces/full-server-packaging-correction.json](../../EVIDENCE/traces/full-server-packaging-correction.json)
- [EVIDENCE/traces/full-server-build-recheck.json](../../EVIDENCE/traces/full-server-build-recheck.json)
- [EVIDENCE/traces/generation3-native-commit.json](../../EVIDENCE/traces/generation3-native-commit.json)
- [EVIDENCE/screenshots/actual/066-current-home-missing-respawn-rejection.png](../../EVIDENCE/screenshots/actual/066-current-home-missing-respawn-rejection.png)
