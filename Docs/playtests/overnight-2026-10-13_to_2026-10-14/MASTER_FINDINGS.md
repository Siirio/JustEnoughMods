# Master finding index

42 indexed observations include setup errors and recovered conditions; they are not all open product defects. In-game failures, source findings and hypotheses have distinct statuses.

| ID | Severity | Finding | Evidence | Status |
|---|---|---|---|---|
| [JEMQA-20261014-0001](SYSTEMS/13-performance-crashes-and-compatibility/FINDINGS.md#jemqa-20261014-0001) | HIGH | Local server and native test client do not contain the current five-system runtime | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0002](SYSTEMS/13-performance-crashes-and-compatibility/FINDINGS.md#jemqa-20261014-0002) | HIGH | JEMCompat build still requires deliberately removed BackToBed | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0003](SYSTEMS/14-other-legacy-and-cross-system/FINDINGS.md#jemqa-20261014-0003) | HIGH | Atlas selects stale server owner metadata for newer canonical modules | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0004](SYSTEMS/14-other-legacy-and-cross-system/FINDINGS.md#jemqa-20261014-0004) | HIGH | Atlas leaks item tags into block/entity records that share a namespaced ID | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0005](SYSTEMS/13-performance-crashes-and-compatibility/FINDINGS.md#jemqa-20261014-0005) | HIGH | Current Settlements source cannot compile its town guidance | SOURCE_CONFIRMED | IMPLEMENTED_UNVERIFIED |
| [JEMQA-20261014-0006](SYSTEMS/02-economy-and-market/FINDINGS.md#jemqa-20261014-0006) | HIGH | Coin merchant consumes two native stock uses for each one purchase/sale | IN_GAME_OBSERVED | VERIFIED_IN_GAME_AWAITING_USER_APPROVAL |
| [JEMQA-20261014-0007](SYSTEMS/03-professions-and-races/FINDINGS.md#jemqa-20261014-0007) | MEDIUM | Profession tree hover repeats the node title instead of effect description | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0008](SYSTEMS/03-professions-and-races/FINDINGS.md#jemqa-20261014-0008) | DESIGN_OPPORTUNITY | Most profession branches are declared unavailable even at level 50 | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0009](SYSTEMS/06-settlements-villagers-and-workers/FINDINGS.md#jemqa-20261014-0009) | HIGH | Quarantined settlement production is retried as if ordinary pending work | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0010](SYSTEMS/06-settlements-villagers-and-workers/FINDINGS.md#jemqa-20261014-0010) | MEDIUM | Settlement manual acceptance still tells operators background production is disabled | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0011](SYSTEMS/13-performance-crashes-and-compatibility/FINDINGS.md#jemqa-20261014-0011) | HIGH | ServerCore rejects the local spawn-category configuration during initialization | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0012](SYSTEMS/08-blocks-biomes-worldgen-and-builds/FINDINGS.md#jemqa-20261014-0012) | MEDIUM | Native tag groups are rejected because required addon references are absent | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0013](SYSTEMS/10-ftb-quests-guides-and-events/FINDINGS.md#jemqa-20261014-0013) | HIGH | Bundled guide children still reference the explicitly retired boss root | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0014](SYSTEMS/03-professions-and-races/FINDINGS.md#jemqa-20261014-0014) | MEDIUM | Mage cast XP bypasses the ordinary profession daily/creative anti-farm guards | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0015](SYSTEMS/12-home-inventory-persistence-and-network/FINDINGS.md#jemqa-20261014-0015) | LOW | Native local certificate verification completed after an overly broad tooling interpretation | IN_GAME_OBSERVED | RECOVERED_SETUP |
| [JEMQA-20261014-0016](SYSTEMS/14-other-legacy-and-cross-system/FINDINGS.md#jemqa-20261014-0016) | HIGH | Static Atlas population includes translation/model variants that are not registry IDs | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0017](SYSTEMS/14-other-legacy-and-cross-system/FINDINGS.md#jemqa-20261014-0017) | HIGH | Actual native dynamic objects are absent from the static integration audit universe | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0018](SYSTEMS/03-professions-and-races/FINDINGS.md#jemqa-20261014-0018) | HIGH | Human +5% profession trait grants +100% on ordinary one-XP construction actions | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0019](SYSTEMS/03-professions-and-races/FINDINGS.md#jemqa-20261014-0019) | HIGH | Merchant and Mayor tracks have no production XP producer | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0020](SYSTEMS/03-professions-and-races/FINDINGS.md#jemqa-20261014-0020) | MEDIUM | Farmer harvest XP omits ordinary melon and pumpkin harvests | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0021](SYSTEMS/02-economy-and-market/FINDINGS.md#jemqa-20261014-0021) | MEDIUM | NPC payout caps in current code contradict owning Economy documentation | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0022](SYSTEMS/14-other-legacy-and-cross-system/FINDINGS.md#jemqa-20261014-0022) | HIGH | Roles/Professions retain integrated-server gameplay despite the native singleplayer boundary | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0023](SYSTEMS/07-mountables-entities-and-ai/FINDINGS.md#jemqa-20261014-0023) | MEDIUM | Eligible skeleton horse has no legitimate native player-tame event into collection | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0024](SYSTEMS/11-ui-visuals-shaders-and-audio/FINDINGS.md#jemqa-20261014-0024) | MEDIUM | Expanded profession card grids have fixed sizes without viewport scrolling or text wrapping | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0025](SYSTEMS/03-professions-and-races/FINDINGS.md#jemqa-20261014-0025) | HIGH | Explorer has a permanent level ceiling and loses future XP for exploration beyond a daily cap | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0026](SYSTEMS/02-economy-and-market/FINDINGS.md#jemqa-20261014-0026) | HIGH | Coin merchant native callback runs with no actor or the previous customer | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0027](SYSTEMS/02-economy-and-market/FINDINGS.md#jemqa-20261014-0027) | HIGH | Human race refunds emeralds from a Coin transaction that spent no emeralds | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0028](SYSTEMS/05-bosses-world-tier-and-progression/FINDINGS.md#jemqa-20261014-0028) | HIGH | Actual boss participant death is cancelled by the legacy fallback when PlayerRevive is absent | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0029](SYSTEMS/12-home-inventory-persistence-and-network/FINDINGS.md#jemqa-20261014-0029) | HIGH | Architect direct hotbar keys change only server selection without a client selection path | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0030](SYSTEMS/06-settlements-villagers-and-workers/FINDINGS.md#jemqa-20261014-0030) | HIGH | Loaded dead workforce entities can overwrite the canonical death mark as living | SOURCE_CONFIRMED | OPEN |
| [JEMQA-20261014-0031](SYSTEMS/13-performance-crashes-and-compatibility/FINDINGS.md#jemqa-20261014-0031) | BLOCKER | Stale clone BackToBed blocks the newly synchronized client at Forge registry handshake | IN_GAME_OBSERVED | RECOVERED_SETUP |
| [JEMQA-20261014-0032](SYSTEMS/11-ui-visuals-shaders-and-audio/FINDINGS.md#jemqa-20261014-0032) | HIGH | Player avatar missing in inventory and third person with non-finite EMF animation state | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0033](SYSTEMS/10-ftb-quests-guides-and-events/FINDINGS.md#jemqa-20261014-0033) | HIGH | FTB Teams local identity lookup uses the launcher UUID; quest-book barrier has a separate unresolved gate | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0034](SYSTEMS/10-ftb-quests-guides-and-events/FINDINGS.md#jemqa-20261014-0034) | MEDIUM | Russian newcomer guide opens with English engineering prose instead of initial player actions | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0035](SYSTEMS/03-professions-and-races/FINDINGS.md#jemqa-20261014-0035) | HIGH | Race-selection cards overlap Russian traits and footer crosses selection controls | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0036](SYSTEMS/11-ui-visuals-shaders-and-audio/FINDINGS.md#jemqa-20261014-0036) | MEDIUM | SMP event hover uses a single horizontal tooltip that runs off both viewport edges | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0037](SYSTEMS/01-onboarding-and-multiplayer/FINDINGS.md#jemqa-20261014-0037) | MEDIUM | Town sidebar gives a hidden no-town chat line and no founding action | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0038](SYSTEMS/02-economy-and-market/FINDINGS.md#jemqa-20261014-0038) | MEDIUM | Coin overlay blocks a natural novice Farmer pitchfork offer; exact ordinary-tool scope needs review | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0039](SYSTEMS/02-economy-and-market/FINDINGS.md#jemqa-20261014-0039) | HIGH | Coin merchant hides direction and shows conflicting no-market tooltip plus overflowing instruction | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0040](SYSTEMS/13-performance-crashes-and-compatibility/FINDINGS.md#jemqa-20261014-0040) | MEDIUM | Graceful cloned server saves and closes ports but idle non-daemon scheduler prevents JVM exit | IN_GAME_OBSERVED | OPEN |
| [JEMQA-20261014-0041](SYSTEMS/13-performance-crashes-and-compatibility/FINDINGS.md#jemqa-20261014-0041) | HIGH | QA installer selected stale full JEM Server after building only slim jar | IN_GAME_OBSERVED | VERIFIED_IN_GAME_AWAITING_USER_APPROVAL |
| [JEMQA-20261014-0042](SYSTEMS/13-performance-crashes-and-compatibility/FINDINGS.md#jemqa-20261014-0042) | BLOCKER | Fresh full embedded JAR omits generated Mixin refmap and fails before world startup | IN_GAME_OBSERVED | VERIFIED_IN_GAME_AWAITING_USER_APPROVAL |
