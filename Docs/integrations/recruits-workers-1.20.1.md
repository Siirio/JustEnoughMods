# Recruits 1.15.2 and Workers 2.0.3 integration

The exact installed JARs define this adapter. Recruits exposes owner UUID, morale, hunger and native command state through `AbstractRecruitEntity`; Workers NPCs derive from its worker/chunk-loader hierarchy and retain their native AI and work-area behavior.

Recruits entity paths are `recruit`, `recruit_shieldman`, `bowman`, `crossbowman`, `nomad`, `horseman`, `messenger`, `scout`, `patrol_leader`, `captain`, `villager_noble`, `siege_engineer`. Workers NPC paths are `animal_farmer`, `lumberjack`, `farmer`, `miner`, `builder`, `merchant`, `fisherman`, `cook`, `courier`. Work-area paths are `croparea`, `lumberarea`, `buildarea`, `miningarea`, `storagearea`, `marketarea`, `kitchenarea`, `fishingarea`, `animalpenarea`, `homearea`.

JEMSettlements records the genuine native entity UUID and verified native owner in one Workforce registry. Recruits are the `RECRUIT` workforce type, not a parallel defense/payroll system. Native morale/hunger become readiness evidence. Native Workers item handlers and work areas remain the storage/work owners.

Pack defaults disable Recruits autoclaim, claim-based block denial/explosion protection and native Recruits payment. Coin wages are also disabled until one explicit funded payroll owner is connected, preventing double charging. No adapter grants block permissions or simulates NPC AI.

Phase 2 assigns exact seven-day JEM Coin wages by installed entity ID: `recruit`, `scout`, and `messenger` cost 20 Copper; `bowman`, `crossbowman`, `recruit_shieldman`, and `nomad` cost 40; `horseman` and `patrol_leader` cost 70; `captain`, `villager_noble`, and `siege_engineer` cost 120. One persisted roster snapshot is debited atomically from the town account to `CENTRAL_RESERVE`. `RecruitsPayment=false` remains the mutually exclusive native configuration. Missed payroll applies a bounded native `setMoral` reduction once per period; JEM never replaces native combat or hunger.

Workers 2.0.3 exposes native working state and work-area identity, but the installed API/bytecode does not expose a safe remote recipe, input reservation, production-rate and output-commit contract. Phase 2 therefore records zero off-screen Worker output with `No supported remote evidence`. It does not invent items, mine unseen ore, duplicate loaded AI, or place pending stock into a linked container. A later adapter needs all four contracts before materialization can be enabled.

