# Recruit and settlement economy implementation — 2026-10-16

The user requested actual Coin hiring, seven-day wages, lasting three-miss rebellion,
paid settlement founding/expansion, and automated verification. The existing source
owners remain JEMEconomy for money, JEMSettlements for workforce/territory/payroll,
and Recruits for combat, equipment, groups, caps and native hiring screens.

## Implemented source behavior

- Hiring uses the exact installed `recruits-1.20.1-1.15.2.jar` seams:
  `CommandEvents.handleRecruiting(Player, RecruitsGroup, AbstractRecruitEntity, boolean)`
  delegates to native `AbstractRecruitEntity.hire`. Its native Hired cancellation
  and recruit-cap checks run before the Coin debit at `makeHireSound()`.
  Native ownership/group/faction effects remain. The former emerald inventory
  counting/removal is skipped on physically dedicated servers only.
- Hire payment uses existing Economy `depositToAccount`, Wallet plus backed cash,
  with an entity/hire-generation/employer operation key and OPERATIONS destination.
  Repeated already-owned hire requests fail before debit. The hire generation is
  native persistent entity data. A new hire automatically enters the existing
  WorkforceRecord roster; no settlement prerequisite is imposed.
- Standalone employers have a small persisted `EmployerPayroll` book inside the
  existing SettlementSavedData. The record's assignment is the verified employer
  UUID; it creates no town, territory or fake treasury. Its wages debit that real
  Wallet account through EconomyApi.settleEmployerPayroll. Town-assigned troops
  debit the existing real town treasury through settleTownPayroll.
- Previously hired unregistered troops are grandfathered without a new hiring fee
  when a loaded entity can be matched to its authenticated online owner. Their
  first new wage falls seven days after adoption. Unloaded entities are adopted
  when loaded and their employer authenticates; historical wages are not invented.
- Each record persists its own next payday, exactly hire/adoption +168,000 game
  ticks and subsequent +168,000 ticks. Overworld gameTime ignores `/time` changes,
  sleep/daylight and dimension switches. Polling executes within the existing
  200-tick service cadence. A new recruit never joins an older overdue roster.
- Scheduling persists immutable per-period rosters, amounts, contract generations
  and receipt state. Economy's stable period transfer keys provide debit replay
  protection. Catch-up processes chronological weeks and stops new accrual once
  three actual insufficient-funds weeks cause rebellion. Late payment of one old
  week removes only that week's miss; it does not forgive other outstanding weeks.
- Rebellion identity is retained through death, purge, chunk reload and town
  deletion. Actual native disband stops future employment accrual. A rebel remains
  hostile until the original employer wins the established defeat interaction and
  pays to hire again; a new hire generation cannot inherit old contract penalties.
  A raw native ownership assignment cannot silently convert DEFEATED to LOYAL.
- Settlements cost 300 copper to found, after name/bell/distance/overlap checks.
  Founding uses the existing Wallet/backed cash debit; territory is added only
  after committed/replayed payment. The initial 97×97 settlement remains unchanged.
- The approved expansion price stays exactly one backed physical copper per newly
  added inclusive block of rectangular area. The legacy radius command now uses
  that same union/overlap/bounds/payment path. No-op/shrink requests cost zero and
  fail without mutation. Debit operation IDs bind town, request and exact bounds;
  a receipt cannot be applied to a different expansion. Town deletion is rejected
  with outstanding payroll, preventing debt deletion. Remaining employed recruits
  move to personal payroll while retaining their next due time and loyalty.
- Noble recruitment packets validate the requested type against the actual native
  noble trade list and remaining stock, derive the villager requirement from the
  installed server config, and charge through native entity hire rather than the
  client-supplied cost. Failed hire does not consume noble stock. Native group
  ownership is checked before hiring.
- Settlement network protocol is now 4. Founding, town/personal workforce and native
  recruit/noble screens receive server Coin quotes. The shared Coin renderer and
  corresponding native client Mixins are owned by the presentation workstream.
  Exact matching JEMSettlements client/server artifacts are required.

## Balance choices made by Codex

Defaults remain editable in Forge server `jem_settlements-server.toml`:

| Native role group | Hire copper | Weekly wage copper |
| --- | ---: | ---: |
| recruit, scout, messenger | 120 | 20 |
| bowman, crossbowman, recruit_shieldman, nomad | 200 | 40 |
| horseman, patrol_leader | 400 | 70 |
| captain, siege_engineer (and native noble rank, which cannot be hired if its native API forbids it) | 700 | 120 |

Hire quotes add 5% of the base per native experience level above 1, capped at 3×.
Role groups reflect installed native equipment/combat capability; native experience
already owns level combat buffs. No second World Tier or equipment progression
ledger is introduced. Seven-day wages preserve the established 20/40/70/120 wage
scale, while up-front hiring costs roughly five to six weeks' wages. Founding is
cheaper than hiring a mounted guard, allowing ordinary settlement access before a
large army. Expansion is intentionally marginal-area-priced and preserves the
explicit physical-cash price lock rather than independently rebalancing it.

## Verification evidence and limits

Read-only exact-JAR `javap -p -c` inspection established the native hire, cap,
cancelable Hired event, disband event, menu accessor, noble trade/config, and
faction bookkeeping signatures. Installed world server config has native
`RecruitsPayment=false`, `RecruitCurrency=minecraft:emerald`; the new adapter
suppresses the second native payment owner on dedicated servers regardless.

`src/test/java/com/siirio/jemsettlements/town/SettlementEconomyContracts.java`
is a standalone production-contract executable, without opening a world/server.
It covers first/new-recruit due time; replay and persistence; observation across
loads; missed-period replay; partial arrears; rebellion through death/late payment;
defeat/paid rehire; dismissal persistence; personal and town journal receipts;
tick-zero epoch; area no-op, one-column charge, repeated expansion, legacy radius,
and overflow rejection. Root integration owns compiling and running it; this
worker has not run it and does not claim it passed.

Root integration also owns affected reobfuscated builds, local server runtime,
matching artifact installation, Atlas records/validation, CHANGELOG, commits,
push and native AutoModpack publication. This worker has performed none of those
operations and has not modified a world save, stable client or release baseline.
Runtime visual acceptance, native noble conversion/spawn behavior and deliberate
process-crash durability still require integration validation; static inspection
alone is not evidence that a running Forge transformation succeeded.

### Integration follow-up

Root integration compiled and ran `settlementEconomyContractTest`: the initial
23 assertions passed. Review then identified out-of-order arrears: a later paid
small roster advanced a high-water mark and prevented an older large unpaid
roster from clearing its own penalty. WorkforceRecord now persists the actual
unpaid penalty period IDs, restores legacy IDs from the authoritative unpaid
journal roster, and removes only the exact paid period. Four additional regression
assertions cover later-before-earlier settlement, payment replay, and legacy
identity recovery; root must rerun the updated suite.

Refmap review also found `fg.deobf(files(...))` did not remap the native Recruits
JAR for the annotation processor. The dependency now resolves through the existing
Server/mods flat directory as `fg.deobf('local:recruits:1.20.1-1.15.2')`, with the
same exact installed bytes. Root must inspect the resulting production refmap and
runtime transformation after rebuild. Incremental Java compilation is disabled so
repeat builds retain annotation-generated refmap resources.

Financial review additionally identified the native noble path dereferencing
`group.getUUID()` after a successful charged hire but before spawning the troop.
NobleHiringMixin now rejects missing, stale or another player's native group at
packet entry, before creating/hiring/debiting. Ordinary direct hires may still
use the native null-group behavior; only noble spawn/conversion requires a real
owned group. Exact VillagerEvents bytecode confirms both spawn and conversion
otherwise add/remove entities only after successful native handleRecruiting.

Root subsequently confirmed the corrected recruits dependency produced native
client method mappings (`renderLabels -> m_280003_`, noble entry
`render -> m_6311_`) and all 27 updated settlement contract assertions passed.
