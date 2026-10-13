# Recruits 1.15.2 and Workers 2.0.3 integration

The exact installed JARs define this adapter. Recruits exposes owner UUID, morale, hunger and native command state through `AbstractRecruitEntity`; Workers NPCs derive from its worker/chunk-loader hierarchy and retain their native AI and work-area behavior.

Recruits entity paths are `recruit`, `recruit_shieldman`, `bowman`, `crossbowman`, `nomad`, `horseman`, `messenger`, `scout`, `patrol_leader`, `captain`, `villager_noble`, `siege_engineer`. Workers NPC paths are `animal_farmer`, `lumberjack`, `farmer`, `miner`, `builder`, `merchant`, `fisherman`, `cook`, `courier`. Work-area paths are `croparea`, `lumberarea`, `buildarea`, `miningarea`, `storagearea`, `marketarea`, `kitchenarea`, `fishingarea`, `animalpenarea`, `homearea`.

JEMSettlements records the genuine native entity UUID and verified native owner in one Workforce registry. Recruits are the `RECRUIT` workforce type, not a parallel defense/payroll system. Native morale/hunger become readiness evidence. Native Workers item handlers and work areas remain the storage/work owners.

Pack defaults disable Recruits autoclaim, claim-based block denial/explosion protection and native Recruits payment. Coin wages are also disabled until one explicit funded payroll owner is connected, preventing double charging. No adapter grants block permissions or simulates NPC AI.

