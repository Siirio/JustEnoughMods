# Combined migration, rewards and native FTB review, bucket D

Status: source fixes complete, awaiting the parent's combined acceptance

`migration_code_ready=false`, `migration_runtime_observed=false`, `source_reviewed=false`, `runtime_observed=false`

This review started with `Docs/quests/visual-v4/migration-source-review.json`. The previous 15-owner source review was retained as the baseline. The current review target is the parent's complete 606-card, 414-blueprint source batch. No test, checker, compilation, game, runtime migration, Git action or additional agent was run. No central registration change is required.

## Material findings fixed

### 1. An unpersisted item queue was reported as permanent issuance

`PendingRewardContainer.enqueue` adds a bundle and its `Issued` entry before `RewardsData.flush`. Previously `hasIssued` read that same mutable entry. If the save failed, `BookItemReward.claimFrom` interpreted the entry as a durable delivery and could leave the original native parent claimed even though the bundle was absent from disk.

`RewardsData` now keeps an in-memory snapshot of the last read-back-verified `Issued` compound. It is initialized from the native loaded reward data and replaced only after the existing temporary write, replacement and actual target read-back all succeed. Public `hasIssued` reads this verified snapshot. Internal `issued` still reads the pending journal so retries keep the exact original queued container and source identity without appending another one.

The stored `jem_rewards.dat` format is unchanged. Completed bundles and permanent receipts remain intact. All existing public method signatures remain intact.

### 2. Historical UI and claim metadata were treated as source membership

The installed FTB `TeamData.mergeData` copies per-player data and claim keys. `ServerQuestFile.playerChangedTeam` retains the old team data and, on leaving a party, merges claimed rewards into the personal team. `TeamData.getOrCreatePlayerData` does not remove old entries after a player leaves.

Consequently the old union of native Teams members, `player_data` keys and claimed-reward UUIDs could attach later party completions to a former member or to a person whose UI metadata arrived through a merge.

New cutover freezes now derive subjects solely from native `FTBTeamsAPI` team membership, including offline members. Original `TeamData`, native claimed flags and personal receipts are still preserved as evidence and remain authoritative for previously paid rewards. They no longer manufacture additional unclaimed eligibility.

### 3. XP could be acknowledged before the player save contained XP or its receipt

Native `TeamData.claimReward` marks the parent before calling the reward. Historical claim then calls `file.saveNow`. Previously `BookXpReward` awarded native XP and wrote only an in-memory player receipt. A restart after the TeamData save but before the player save could therefore retain a claimed parent without the corresponding XP or receipt.

`BookXpReward` now preserves the original `XPReward.claim` call and existing receipt key, then saves and reads back the native player's XP fields and receipt before returning success. Fixed and historical claims pass the exact native source team and parent through `claimFrom`. A failed save reopens only that source parent. A bounded weak map distinguishes these unfinished save operations from permanent receipts. Same-process retry persists the existing receipt without awarding XP again. Player clone handling preserves both the existing receipt and any unfinished operation.

No XP amount, reward identity, claim key or persistent save schema changed. This adds no quest-completion authority.

### 4. The integrated host's claim receipt was verified in the wrong native save

Mapped MC 1.20.1 `PlayerList.load` first uses `WorldData.getLoadedPlayerTag` for the singleplayer owner. That is `level.dat/Data/Player`, rather than the ordinary `playerdata/<UUID>.dat` fallback. The existing JEMRewards code requested a full host save but verified only the UUID file.

`RewardsData.savedPlayer`, inventory claim read-back and offline receipt recovery now select the actual native owner path. The integrated host uses `level.dat/Data/Player`. Dedicated players and integrated guests use their UUID file. XP persistence uses the same native distinction. A failed host-level write cannot acknowledge a claim based only on the secondary UUID save.

### 5. An unavailable earlier source could hide an available earned source

Archive pagination previously deduplicated a parent as soon as it encountered the first eligible source team, even when that source was locked or blocked. A later eligible unlocked source for the same original parent could not appear.

The proposal list now retains one parent group and prefers an available eligible source over an unavailable earlier source. Every option and request still undergoes the original frozen definition, native source completion, native blocked/locked state, parent, option and receipt validation. No new entitlement or extra parent is created.

## Whole-bucket source pass

- The 602 original cards, 2146 original reward decisions, original choice tables and immutable legacy definition resources were not edited by this bucket
- The eight newly retired source cards are NEW objectives and were not recovered or restored by this bucket
- Independent advancement criteria remain native advancement criteria. No completion bitmap, historical NEW-goal credit, reward reprice or extra payout was introduced
- `BookMigrationDefinitions` and `BookMigrationRecovery` retain the prior exact native-definition freeze, actual history reference and exact dormant-transform requirements. Source-only baseline data still does not establish a world entitlement
- Archive requests retain server-thread handling and exact source eligibility checks. Choice options remain proposals until the server resolves the actual native parent and option and checks the existing shared claim source
- Mixed fixed, XP and choice rewards retain separate original parent identities. A paid choice source prevents another option, while an unrelated unpaid fixed parent remains claimable
- Failed enqueue retries retain the original pending source and bundle. A pending bundle cannot issue live inventory until `commitClaim` first flushes the claim journal, the bundle and its issuance entry. The subsequent inventory/receipt native save must pass read-back before remaining slots commit
- Empty and partially collected bundles retain permanent issuance receipts. Source and player UUIDs remain generic runtime values. No authored quest ID, team ID or personal player ID was added to runtime code
- The existing native collect-rewards control still exposes the right-click archive action. The native list, icons and tooltips remain in use. Search, persisted selection, deep links and reward table presentation still use the existing archive filters. No eighth tab or informational archive was added
- Typed FTB access remains in the existing optional FTB family. Existing reflected JEMRewards and item compatibility APIs retain their public signatures. Ordinary standalone and dedicated saves keep their native owner paths

## Changed production sources

- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/compat/ftb/BookMigration.java`
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/compat/ftb/BookXpReward.java`
- `Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/compat/ftb/BookEvents.java`
- `Mods/JEMRewards/src/main/java/com/siirio/jemrewards/PendingRewardContainer.java`
- `Mods/JEMRewards/src/main/java/com/siirio/jemrewards/RewardsData.java`

## Exact native lookup evidence

Installed API owners inspected were `ftb-quests-forge-2001.4.22.jar`, `ftb-teams-forge-2001.3.2.jar` and the existing Forge 1.20.1-47.4.10 official-mapped MC artifact. Only exact relevant class bytecode was read.

- `build/questbook-v4-review-teamdata-native.txt`, native claim ordering, reset behavior, metadata and merge behavior
- `build/questbook-v4-review-serverquestfile-native.txt`, native team changes, source TeamData retention and membership resolution
- `build/questbook-v4-review-ftbevents-native.txt`, native team event delegation
- `build/questbook-v4-review-playerlist-native.txt`, native integrated host versus ordinary player load and save ownership

## Acceptance limits

The parent must rebuild both Guide and JEMRewards in the single combined build. Compilation, packaging, optional class loading and failure/restart behavior remain unobserved, with all readiness and runtime flags left false.

Existing cutover checkpoints remain immutable. The membership correction applies to new freezes. If an unpublished older draft created a `visual-v4-1` checkpoint, its historical subjects must be reconciled against that cutover's actual native Teams evidence before reuse. This review does not infer or rewrite historical membership from present-day teams. No V4 runtime activation is recorded by the existing review baseline.

Source is frozen for the parent's combined acceptance. No central files, books, localization, compiler output, migration resources, Atlas files or other producer families were edited.

## Subsequent direct user amendment

The user confirmed that nobody used the previous quests. Historical progress/reward migration and old archives are not required and are disabled before native preparation. Current rewards retain ordinary admission, durability and native queue delivery. The current decision is recorded in history-policy.json and migration-source-review.json. Earlier historical accounting in this report is evidence only, not a release requirement
