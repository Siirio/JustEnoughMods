# Universal standalone implementation

Status: source, production artifacts and native local installation complete, 2026-10-22; Atlas validated; Git delivery prepared. The user explicitly requested execution of the previously supplied Universal Standalone plan with the amendments below. This file tracks implementation, not player acceptance or release approval.

## Binding requirements

- Forge 1.20.1 / Forge 47.4.10 / Java 17; one shared production implementation for integrated and dedicated servers.
- Merge Currency Core into Economy. Preserve `jem_economy` registries, accounts, physical outstanding, receipts, prices and saved data.
- Merge all production race mechanics/presentation into one Races artifact. Preserve the `jem_roles` saved-data authority, five races and developed powers/assets.
- Distribute every Pack Core responsibility/resource to its actual domain; remove the Pack Core artifact and source owner.
- Remove the Menu Support runtime artifact/dependency. Required native controls become private to their consuming modules; GUI ownership is independent.
- Professions, Villager Talking and Claims remain development-only, permitted in sandbox/test jem, excluded from the original/shareable pack.
- Bootstrap is included in the shareable universal pack. Fix the reported long-path acquisition failure through its native owner. Replace repeated warnings/countdown with one file list, risk consent and download action.
- Economy defaults on. A world-owned off switch makes all currency functions dormant without deleting balances, coins or records. Settlements uses its existing emerald provider whenever Economy is absent/off.
- Solo supports non-event gameplay and unlimited settlements. Preserve native optional integrations and hide only unavailable features.
- World Tier adapts to available registered bosses. Twelve Eyes remains the same strict twelve original MAIN bosses; a fresh incomplete world must not initialize an impossible campaign or block vanilla End access. Existing active campaign data remains protected when content is missing.
- Events remain under JEM Server, explicitly enabled by a dedicated host; integrated worlds never schedule them.
- One content-aware FTB book preserves quest IDs/completion and shows the actual five races. FTB observes domain state.
- Authority stays on the logical server; dedicated identity/authentication, worlds, prices, cooldowns, race powers and unrelated contracts remain preserved.
- Preserve unrelated working-tree changes. Work on current main checkout without branches/worktrees. Production packaging/local sync follows standing AGENTS authorization. No automated tests, gameplay launch, MineRENT/stable changes or baseline promotion.

## Owner-aligned work

- [x] Economy/Settlements: consolidate currency ownership, optional provider boundary, world feature switch, integrated runtime, scripts and compile dependencies.
- [x] Races: consolidate source/resources/registration/network into one production artifact, canonical shared save owner, integrated gameplay, optional adapters, private controls.
- [x] Pack Core: migrate all source/resources to Achievement Guide, Compat, World Boss Tiers, Twelve Eyes and Server according to actual consumers; retain namespaces and protocol identities.
- [x] Bosses/campaign: integrated solo starts, idempotent raid preparation/rewards/respawn, content-aware progression profile and strict missing-content campaign policy.
- [x] Server: logical-server non-event services, explicit dedicated event activation, optional domain adapters and private native menus.
- [x] Bootstrap: establish exact causal path for long-name failures, fix acquisition/projection owner, simplify only consent presentation, preserve hashes/native trust/transactions.
- [x] Distribution/quests: universal offline source, Bootstrap included, development modules excluded, stable-ID adaptive quest content.
- [x] Delivery: update owning docs/Atlas/CHANGELOG, production builds only, source/JAR consistency, local native publication/application where permitted, scoped review, exact staging/commit/push and remote verification.

## Integration interfaces

Economy exposes `EconomyApi.isEnabled(MinecraftServer)` and accepts only logical-server authoritative currency mutations. Settlements owns its provider interface and never exposes Economy classes in common signatures. Race mechanics consume RaceService/RaceSavedData in both server environments. Events retain their existing implementation under one host gate. Native AutoModpack remains the sole client generation/apply owner.

## Review focus

Missing optional classes must not load through common signatures/mixins. Disabling Economy must not charge/reward/replay money or poison merchant offers. Source/resource moves must preserve every registry/save/localization/model/packet path. Existing active campaign worlds must fail closed on missing content. A Windows path failure must be fixed without bypassing download integrity or writing outside managed ownership.

## Evidence and outstanding acceptance

Before-change Atlas lookups and scan were recorded in `.superpowers/sdd/universal-standalone-20261021/`. The initial scan already reports unrelated uncovered changes; these remain separate. Builds, permitted catalog generation and source review do not establish gameplay or visual acceptance. No automated tests will be created or executed.

Source review corrected ordinary Bootstrap confirmation, Economy-off native offers/UI, native Eye admission, missing post-End targets and bypassed event handlers. Exact reviews remain in task evidence. Ordinary Server startup exposed an impl-cache staging directory lock during Bootstrap replacement; the publication mechanism was repaired and the final startup and native application completed. No client/gameplay or Linux/macOS run was performed.

Final artifact revisions: source review and production builds complete; Bootstrap cache publication now writes verified native files/stamp without replacing its directory. Guide building/7 native predicate format repaired. Shader auto-enable disabled for cross-platform fresh installation; original shader archives retained optional. The ordinary dedicated server is restarted solely for native publication/application; no gameplay/client launch or player acceptance.

Native generation 59 is published and fully applied to closed test jem. Installed active/head tokens match; all nine task artifact hashes match; old PackCore/Roles/Races artifacts are absent and no native transaction remains. All 2,750 archive file hashes and cross-platform case uniqueness pass. Existing stable/bootstrap bytes and release baseline remain unchanged. Remaining acceptance is explicitly unrun.

Final Atlas effective rebuild/audit/validator pass (118 records); scan reports only the excluded preexisting local provenance copy. No artifact drift. Source/compiled artifact/native installation/archive delivery is complete; player/gameplay and Linux/macOS acceptance remain explicitly unrun.
