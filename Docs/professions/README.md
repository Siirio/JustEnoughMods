# Profession progression

## Current sandbox state — rolled back, 2026-10-18

The user rejected the standalone migration as too closely tied to the full modpack and explicitly chose **remove professions entirely**, including the older partial version. JEMProfessions, Puffish Skills and TinyInv are absent from `jem sandbox`. JEM Economy and the eight external JARs added for the78-skill migration are also absent. Standalone Settlements is restored to its exact pre-migration artifact, retaining the recruit food navigation/chest-meal fix. Races, Recruits and the other pre-existing sandbox mods remain.

Current runtime receipt: [sandbox-rollback.json](sandbox-rollback.json). Prior delivery records and the implementation contract below are historical, not the installed sandbox state or successful player acceptance. Sources remain in Git; this request rolls back the sandbox runtime only. The original modpack, ordinary Server, ManagedPack, stable profiles and release baseline remain unchanged. Worlds and saved progress are preserved; dormant configurations/data were not deleted. No build, test or game launch was performed for the rollback.

## Historical rejected sandbox migration contract — 2026-10-17

Status: implementation rejected and removed from sandbox on 2026-10-18 at the user’s request. The prior design approval and implementation history below do not establish successful migration or current runtime acceptance.

Source diagnosis: professions is already a standalone mod, not part of Races. The sandbox screen selects one profession through a left rail; its details renderer does not show the profession XP instruction. The 78-node presentation catalog contains many entries with generic rank text. `sandbox-availability.json` admits only eight nodes because most promised gameplay adapters do not exist; the packager deliberately makes unsupported nodes unaffordable. Thus importing the data or removing the gate cannot create the missing effects. The owning correction is to implement each effect and native gain signal, bind descriptions to their real contract, then admit those nodes and replace the sandbox presentation. This is a source-backed cause chain, not a claim that gameplay was launched or that every one of the eight existing effects was manually verified.

### Sandbox startup correction — 2026-10-17

The user’s actual Forge crash report `crash-2026-10-17_23.45.47-fml.txt` identified `IllegalClassLoadError` for `SandboxProfileMarker`. The Mixin configuration declared the entire sandbox application namespace, so Mixin rejected the normal marker before Forge could construct it; moving only that marker would leave the same fault on the gameplay helpers. All16 Mixin/accessor classes now live in `com.siirio.jemprofessions.mixin.sandbox`, with matching configuration names and fishing accessor import. Marker, plugin and effect helpers remain outside that reserved namespace. This changes class-loading ownership only; native injection targets, dependencies and approved mechanics remain. Required sandbox JAR rebuilt/reobfuscated and replaced with backup/hash evidence in the delivery record. User startup acceptance remains in testing; no client was launched by Codex.

### User decisions

- Work in the `jem sandbox` profile and use the existing local test server as evidence for installed versions, native behavior and existing JEM owners.
- Keep professions a separate mod; preserve the existing ten professions and their 78 stable skill IDs.
- Restore the supplied one-screen radial tree, remove the profession sidebar, and eliminate icon overlap.
- Implement actual effects and profession XP. Use short, ordinary Russian descriptions explaining the action and result, without internal financial terminology or generic “final rank” filler.
- Sandbox testing must allow free individual claim/unclaim without XP or prerequisite requirements.
- Preserve Create, spellcasting, commerce and town features and add the required systems; do not replace them with unrelated vanilla bonuses.

The supplied [reference and extraction](assets/reference/radial-2026-10-17/README.md) establish composition and rejection of crowding, not approval of the eventual screen.

### Architecture and delivery boundary

Puffish Skills 0.19.2 remains the only owner of XP, levels, earned points and actual unlocked skills. JEMProfessions owns the catalog, validated XP signals, effect adapters, cooldowns and screen. JEM Races is not a dependency or the profession entry point. F7 and `/professions` remain available without a race selection. Existing Identity resolves the integrated owner; use its actual API rather than adding a parallel identity ledger.

The sandbox artifact receives the new behavior through its existing profile source set. Shared fixes must preserve the production profile's present behavior until that profile is explicitly promoted. Changes required in Economy/Settlements/CurrencyCore must be packaged for this sandbox boundary as well; do not silently deploy them to ordinary Server or ManagedPack. Canonical source is still edited in its owning module, never patched in an installed JAR. No MineRENT, stable profile, friend release or baseline promotion is part of this request.

Add exact existing installed dependencies from the test pack, with their mandatory transitive dependencies and nested JAR declarations inspected before copying: Create 6.0.8 (including native Ponder/Flywheel), Iron's Spellbooks 1.20.1-3.15.6, Iron's Lib 1.0.2, Curios 5.14.1, Player Animator, the actual base Epic Fight installation, PlayerRevive 2.0.31/CreativeCore, and the relevant JEM Economy owner. Nightfall is an addon, not the base Epic Fight mod. Add its dependency family only if an exercised native contract actually needs it. Do not add Workers, Saints Dragons or unrelated pack contents. Preserve sandbox worlds and launcher metadata.

Economy owns all money, reservations, auction/order assets, payment recovery and receipts. Settlements owns residents, towns, stock, taxes and guard orders. Native Create owns machines, train schedules and schematic semantics. Native Epic Fight owns stamina, dodge and guard. Native Iron owns mana, spells and schools. PlayerRevive owns downed-state revival. Professions grants bounded access/modifiers and consumes committed native results; it never reconstructs those systems' state. The existing Economy reserve/settle/refund primitives are insufficient by themselves to constitute an auction/order workflow: add that workflow to its owner.

### Screen and interaction

One player center, ten recognizable profession roots, and all 78 skill nodes form one connected radial tree. Each profession gets a separate angular sector. Three branches use parallel lanes with a fixed minimum node gap; increase graph radius to fit rather than placing several icons on top of one another. Render one native item per node, explicit parent links, distinct locked/available/unlocked states, and a selected-node outline. Keep the compact dark background and gold selection from the supplied reference. No new decorative art or cropped screenshot textures are required.

Wheel zooms around the pointer; dragging empty graph space pans. Provide a visible fit/reset-view control. Uniformly scale nodes/icons with the graph and derive hit boxes from that same transform. Reserve header and bottom details bounds outside the graph viewport. On small GUI sizes, allow pan/zoom and wrapped details rather than shrinking text below its native readable size. Selected profession header shows real level, XP and unspent points. The detail area says: skill name; **Что даёт**; **Как использовать**; **Как получить опыт**; exact required level/parent/point; state/action button. Hover previews; click selects; a separate explicit button learns or tests a skill, preventing accidental learning while panning.

Normal mode checks native points, level and parent. Sandbox test mode has a conspicuous `Свободное изучение` switch and `Изучить / Убрать` button for the selected skill. It calls native Skill.unlock/lock on the integrated owner only, verified against the installed 0.19.2 public API. It does not raise XP, forge prerequisites or maintain a second enabled-skill map. Free claims use a dedicated native point source per node so earned points are unchanged; remove only that node's test-point grant on unclaim. Never reset an entire category. Unclaiming a parent leaves independently selected children visibly marked as test-unlocked; higher-rank effects must be directly exercisable without secretly requiring the parent. Passive modifiers reconcile immediately; active operations recheck unlock state and cancel safely if permission is removed. Ordinary dedicated servers reject this test route. When normal mode is restored, existing free claims remain explicitly marked; they are not silently presented as earned skills or silently erased.

Active skills use a separately bindable profession-action key and one selected active skill. Select it in the detail area; show the exact key/item/target/cost there. Use server ray tracing and server-owned selection/settings, not arbitrary client positions. Native modifiers stay passive. Simple existing item gestures such as crouch-use-pickaxe remain supported and documented. Selection tools, template management, fish/school selection and commerce/town actions need real bounded controls reachable from the selected skill, not chat-only placeholders.

### Real XP and plain-language instruction

Rates below are Codex proposals where they extend existing source. Existing cumulative level costs and one-point skill costs remain. Awards happen after a successful server-owned action, once per receipt/action. No XP for creative/spectator/fake players, machine output, merely opening a screen, buying from oneself, moving money between one's accounts, repeated claim/unclaim or a failed/cancelled action. Operation batches award only their actual eligible results and obey the same daily limits as manual work.

| Profession | Player-facing instruction | Actual proposed award and abuse boundary |
| --- | --- | --- |
| Архитектор | Ставь блоки и строй из новых материалов. | Existing +1 per eligible placement, at most 600/day; first material +5. Persist the position suppression needed to stop place/break farming across reconnect/restart; no XP for replayed placement. |
| Фермер | Собирай спелый урожай, разводи и стриги животных. | Existing +2 per mature crop, up to 500/day. New +6 per successful breeding and +2 per actual sheep shearing, combined 200/day; native readiness and item costs apply. |
| Рыбак | Лови рыбу удочкой. За новый вид — больше опыта. | Existing +12 for the first species and +3 for repeat fish, repeats up to 300/day. Award once per hook result, excluding skill-created bonus fish. |
| Шахтёр | Добывай природную руду подходящей киркой. | Existing +3 per natural ore, 750/day. Keep natural/player-placed provenance authoritative; canceled breaks and repeat placements earn nothing. |
| Исследователь | Исследуй новые места пешком, вплавь или на лодке. | Existing +2 per newly visited chunk, 200/day. Dimension is part of the persisted discovery key; teleport/spectator/creative movement does not count. |
| Инженер | Собирай детали Create, строй механизмы и железную дорогу. | First placed Create component +10, as in current source. New +2 per manually crafted component recipe and +1 per new eligible component/rail position, combined 400/day; recipe/output receipts and position replay guards exclude automation and cycling. |
| Воин | Побеждай враждебных существ. | Preserve health-based 1–20 XP, cap 120 per enemy type/day. Restrict to genuine hostile combat: no villagers, pets, players or passive-animal farming; one victim death receipt. |
| Торговец | Продавай товары жителям и выполняй чужие заказы. | New +3 per completed native NPC purchase of the player's goods, or distinct paid player order, 300/day. Existing funded NPC quotas remain. Player counterpart must be another verified account; no cancellation/refund/self-trade XP. Buying from a resident is not a profession-XP source. |
| Мэр | Заселяй город, доставляй еду и улучшай условия для жителей. | New +20 per first valid resident enrollment and +10 per first achieved housing/food/safety improvement, plus +1 per resident meal actually accepted as a result of the player's explicit food delivery/order, combined 200/day. Automatic background meals award no XP. Persist per-town/resident milestones; no unregister/register or chest transfer farming. |
| Маг | Применяй заклинания из книги магии. | Existing +max(1, native spell level) per actual paid successful cast. Add a 300/day cap and native cast receipt deduplication; equipping, previews, failed casts and skill-generated recasts earn nothing. |

Every skill detail repeats its profession's corresponding simple XP instruction, then its own exact level/parent requirement. No player has to understand “escrow”, “affordable”, “parent-child”, “backed liquidity” or internal account names.

### Skill effects: complete proposed contract

Existing concrete values are retained below. Entries that formerly said only “second/final rank” receive the explicit proposed effect shown here. These completions are Codex design choices awaiting review, not previous user-approved mechanics. The catalog's existing required levels, stable IDs and graph edges remain unchanged.

#### Архитектор — 9 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| long_arms_1 | Ставь блоки на 3 блока дальше. Только строительство. |
| long_arms_2 | Ставь блоки на 5 блоков дальше. Заменяет предыдущий бонус. |
| long_arms_3 | Ставь блоки на 7 блоков дальше. Заменяет предыдущий бонус. |
| phantom_3 | Выбери грань: поставь стенку 3×3 из блоков в сумке. |
| phantom_5 | Та же установка площадью 5×5. Все блоки расходуются. |
| phantom_7 | Та же установка площадью 7×7. Все блоки расходуются. |
| architect_hotbar | Открывает 13 настоящих ячеек быстрого доступа. |
| architect_supply | Подкладывает такой же строительный блок из сумки, когда рука пустеет; максимум 126 за действие. |
| architect_command | Повторяет последнюю выбранную плоскость на соседней грани; до 49 блоков из сумки. |

Placement extensions never increase attack/entity reach. Plans reject protected/unloaded/blocked cells before spending, respect native placement events and never copy block-entity contents or bypass material costs. TinyInv remains the hotbar owner. Repeating a plane is a new validated placement operation, not creative cloning.

#### Фермер — 9 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| crop_sense | Используй мотыгу: увидишь, где созрел урожай в 12 блоках. |
| sweep | Выбери спелую грядку: собери до 64 соседних культур и посади их заново, если есть семена. |
| harvest_master | Сбор грядки охватывает до 1024 культур; обрабатывает не больше 64 за тик. |
| herd_call | Созови до 8 подходящих животных на 10 секунд. Держи их обычный корм. |
| feed_herd | Покорми до 8 взрослых животных из своей сумки; корм и готовность к размножению обычные. |
| quick_shear | Подстриги до 8 готовых овец; нужны ножницы, прочность расходуется. |
| greenhouse_master | В теплице 9×9 растения получают на 10% больше попыток роста. Нужна заправка костной мукой. |
| greenhouse_keeper | Увеличивает добавочные попытки роста теплицы до 15%. |
| greenhouse_abundance | Увеличивает добавочные попытки роста теплицы до 20%. |

One visible greenhouse controller owns enclosure/fuel status. Glass enclosure and crops are validated within the 9×9 area, only loaded crops advance, and one consumed bone meal funds 64 eligible additional growth attempts. Higher ranks replace lower percentages; no global randomTickSpeed change. Sweep creates real loot, consumes actual replant seeds, and safely stops on unclaim, logout or changed permissions. No animal teleport or bypass of native breeding/shearing cooldowns.

Implementation detail: the controller is an actual composter, with interior crops one block above its base, glass walls at ±4 and a roof four blocks above. Bonus growth pauses without spending fuel while the owner is offline or in another dimension, so native interaction permissions are always checked in the correct level. Ordinary crop growth remains native.

#### Рыбак — 6 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| target_fishing | Выбери обычную рыбу: её вес в улове станет на 20% выше. |
| rare_bait | Используй одну рыбу как наживку при забросе: выбранный вид получает +35% веса вместо +20%. |
| trophy_catch | С наживкой выбранный вид получает +50% веса вместо +35%. Сокровища не становятся чаще. |
| double_catch | 10% шанс получить одну дополнительную обычную рыбу. |
| steady_line | После успешного улова удочка с шансом 20% не теряет обычную прочность. |
| net_master | Шанс дополнительной рыбы увеличивается до 20%; максимум одна за улов. |

Selection changes only the relative native fish pool weight, never treasure/junk rules. One bait is consumed once at a real cast; failed/cancelled casts cannot create fish or repeated consumption. Do not blindly replace already generated modded or NBT-bearing loot. The selected fish and bait state have one server owner.

#### Шахтёр — 9 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| ore_sense | Присядь и используй кирку: узнай, есть ли природная руда в 6 блоках. Откат 45 с. |
| ore_memory | Чувство руды также называет ближайший вид руды и направление. |
| deep_resonance | Увеличивает радиус чувства руды до 12 блоков. |
| vein_miner | Добывай присев: разрушишь до 8 связанных блоков одной природной руды. |
| vein_master | Увеличивает жилу до 32 блоков. |
| bedrock_precision | Собирает твои выпавшие предметы от добытой жилы в сумку; лишнее остаётся на земле. |
| stone_step | На 10% меньше урона от падения на камень. |
| lava_shield | При огненном уроне: огнестойкость на 8 с. Откат 10 минут. |
| cave_breath | Под землёй воздух под водой расходуется в среднем на 15% медленнее. |

Search remains demand-only, bounded and loaded-only. Connected mining uses real break permissions, native drops and tool durability. No arbitrary nearby-player item pickup or remote chunk loading. Higher rank free claims must exercise their described behavior independently.

#### Исследователь — 6 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| wall_climb | Выбери стену: заберись на свободный уступ высотой до 2 блоков. |
| fast_traveler | +8% скорости пешком в недавно открытом тобой месте; действует 30 с после нового чанка. |
| ledge_catch | Удерживай действие у уступа: повисни на 2 с и подтянись. Расходует одну нить. |
| ration_saver | Бег расходует на 10% меньше сытости. |
| river_runner | Твоя обычная лодка движется на 8% быстрее. На поезда не влияет. |
| portable_camp | Выбери свободное место: поставь костёр и кровать из сумки. Действие можно отменить до установки. |

Mobility validates the whole player collision box, border, clearance and permissions; no void rescue, revive or damage immunity. Portable camp uses ordinary placed blocks and item drops, never a second temporary inventory or silently created supplies. No train modifier; no assumption that modded ships share vanilla boat physics.

#### Инженер — 6 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| gear_swap | Выбери установленную шестерню и замену в сумке: обменяй детали без потери старой. |
| shaft_builder | Уложи прямую линию до 16 валов Create из сумки. |
| factory_copy | Сохраняй до 3 чертежей размером 7×7×7; строй через обычный принтер Create со своими материалами. |
| track_run | Уложи до 12 подходящих участков пути из сумки, по обычным правилам Create. |
| schedule_templates | Сохраняй до 3 расписаний; записывай выбранное в настоящий предмет расписания. |
| cargo_contract | Создай оплаченный заказ доставки: курьер получит награду после сдачи нужного груза. |

Create remains responsible for placement/kinetics/tracks/schematic printing/schedule validation. No copying inventories, fluids, active machines or arbitrary NBT. The cargo order is an Economy-owned delivery order with real reserved payment and exact item/quantity/destination; it does not create a parallel currency or teleport the goods. Existing native train speed stays unchanged.

#### Воин — 9 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| endurance | Уклонение Epic Fight тратит на 5% меньше выносливости. |
| battle_recovery | После настоящего уклонения верни 2 выносливости. Откат 30 с. |
| second_wind | При здоровье ниже 30% получи Скорость I на 4 с. Откат 60 с. |
| parry_master | Идеальный блок Epic Fight тратит на 15% меньше выносливости. |
| counter_step | После идеального блока следующий удар в течение 3 с наносит +1 урона. Откат 20 с. |
| duelist_focus | После 3 идеальных блоков за 10 с: +10% скорости атаки на 6 с. Откат 30 с. |
| field_medic | Поднимай упавшего союзника PlayerRevive на 20% быстрее. |
| battle_aid | После успешного подъёма союзник получает Регенерацию I на 5 с. Откат 60 с на получателя. |
| revival_master | Поднимай союзника на 35% быстрее вместо 20%. |

Only actual native dodge/guard/revive events qualify. Effects cannot add hidden lives, cancel an actual death, restore dead participants or bypass hosted-encounter elimination. PlayerRevive support is disabled wherever the canonical hosted lifecycle requires immediate death/elimination. No JEM recreation of a downed state. A solo user can freely unlock the support branch but needs a real eligible second player to exercise revival; sandbox mode does not spawn a fake player.

#### Торговец — 6 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| buy_orders | Размести до 3 заказов на покупку. Деньги снимаются сразу и ждут продавца. |
| auction | Выставь предмет на торги на 1–24 часа. Победитель платит, ты получаешь деньги. |
| commission_escrow | Закажи работу на срок до 72 часов. Оплата выдаётся при сдаче заранее указанного товара. |
| stall_sign | Размести до 3 вывесок своих настоящих магазинов. Вывеска открывает нужный магазин. |
| trading_post | Открой торговый пост: продавай товары из своего запаса по своей цене. |
| market_network | Показывай до 8 своих действующих объявлений в общей торговой доске. |

Missing workflows belong to Economy/CurrencyCore. A listing removes/reserves the exact real item or quantity/payment before publication. Auction bids fund only the new actual reservation, refund an outbid account exactly once, reject self-bids and settle on expiry without the seller staying online. Buy orders accept only the declared safe item fingerprint/count; no arbitrary “job done” client claim. Cancellation/expiry returns real reserved assets to a persisted recoverable delivery claim when inventory is full or the owner is offline. Use recoverable journal phases for inventory/payment transitions; native idempotent transfers alone do not make a multi-step item transaction atomic. One receipt can pay/award XP once. No unbacked payment, second wallet or free item. Native NPC trading remains the native screen/list/slots/restock owner.

#### Мэр — 9 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| invite_residents | Пригласи до 2 ближайших свободных жителей в город за 3 игровых дня. Нужны свободные дома. |
| supply_notice | Выбери 3 вида припасов; город предупредит об их нехватке не чаще раза в игровой день. |
| resident_move | Пересели до 2 согласных жителей в другой подходящий дом за игровой день. |
| taxes | Выбери налог 0/5/10/15% с настоящего оплаченного дохода жителей. Деньги поступают в казну. |
| treasury_audit | Показывает, откуда пришли и куда ушли деньги города. |
| civic_fund | Отложи деньги казны на выбранную городскую цель; тратить их можно только на неё или вернуть. |
| rally | Созови до 12 своих бойцов в 64 блоках к выбранной безопасной точке. |
| patrol_route | Задай до 6 точек патруля для 12 своих стражей. |
| captain_order | Назначь своего бойца командиром: до 12 выбранных бойцов следуют за ним. |

All actions delegate to Settlements/native Recruits and enforce the selected town UUID, owner, loaded nearby entities and native movement/permission contracts. Inviting moves existing eligible villagers; no NPC spawning or Workers. Housing and assignment are real canonical settlement facts. **Proposed precise tax base:** a real Coin payment from a player buying goods from an explicitly enrolled town villager. Economy splits that committed receipt: the configured percentage goes to the town treasury and the rest goes to NPC_MARKET. Selling goods to a villager is the NPC's expense and is not taxed. Item barter, transfers, payroll, virtual production and reservation/refund flows are not taxed. A reversed receipt reverses its exact split once. Resident membership, town and rate are captured/revalidated at commit; no later tick reconstructs income from balances. This definition is Codex's proposal to resolve the absent resident-income receipt, not an existing feature. Fund reservation/return is an Economy-owned purpose-restricted account and does not duplicate treasury balance. Patrol/rally/captain changes native movement orders and never grants combat ownership of another player's soldier.

#### Маг — 9 skills

| Stable ID | Short Russian effect and use |
| --- | --- |
| mana_saver | Обычные заклинания расходуют на 5% меньше маны. |
| meditation | После 10 с без движения и заклинаний мана восстанавливается на 5% быстрее. |
| combo_magic | Примени 3 разных заклинания за 10 с: следующее за 5 с будет дешевле на 20%. |
| magic_school | Выбери школу: её заклинания получают +8% силы. |
| school_master | Увеличивает бонус выбранной школы до +12%. |
| arcane_specialist | Увеличивает бонус выбранной школы до +16%. |
| arcane_guard | При уроне потрать 25 маны и поглоти до 2 HP. Не работает без нужной маны. |
| magic_circle | Создай круг радиусом 6 блоков на 8 с: +10% восстановления маны союзникам внутри. Цена 50 маны, откат 60 с. |
| sanctuary | Твой магический круг также даёт союзникам Регенерацию I, пока они внутри. |

Use the exact installed native spell cost, mana state, school attributes and cast events. No post-cast refund that can manufacture mana or award XP twice. Higher school ranks replace the previous bonus; Combo applies once to the next eligible native cast. Circles affect consenting/allied targets within the loaded radius, have one server lifetime and cannot revive a dead entity. Free-claiming Sanctuary directly grants its described circle behavior; it must not appear inert because Magic Circle was not separately claimed.

### Implementation and acceptance sequence

1. Finish exact native API/bytecode investigation for each integration; document an actual missing seam before selecting a narrow Mixin. The dependency list above is not evidence that all APIs have been inspected.
2. Make the standalone native state/test-control/UI slice complete, then the vanilla effect families, then the native combat/magic/Create adapters, then Economy/Settlements workflows and their committed XP signals. This is an implementation order, not authorization to call partial delivery fully functional.
3. Keep the same machine-readable skill contract as the source for the screen descriptions and packaged native definitions; do not maintain separate conflicting effect prose. Native XP/skill state remains authoritative across reconnect/death/reload, with no resets of existing saves.
4. Update Atlas applied changes/effective coverage only as actual source behavior lands. The pre-change scan currently also reports unrelated worktree edits and existing canonical-JAR drift; do not absorb, revert or falsely claim to have fixed those as part of this task. Use exact task-path/index scope for delivery review.
5. Build only required reobfuscated sandbox artifacts with test tasks excluded, install exact fresh outputs only while `jem sandbox` is closed, and record source/target hashes, dependencies and backups in a professions-owned delivery record. The standing artifact authorization permits this; it does not authorize gameplay launches or automated scenarios.
6. Update owning docs, Atlas, CHANGELOG and exact task paths; fetch/review current branch divergence, commit and push without touching unrelated changes. Ordinary local Server/ManagedPack/native publication remain unchanged for this sandbox-only migration. Report their actual state rather than promising a new generation.
7. No automated tests, fixtures, lint, formatter, static-analysis run or gameplay client launch. Runtime appearance and player acceptance remain pending manual sandbox play. Do not mark the “no effects” defect resolved until user approval.

### Implementation plan and work ledger

**Goal:** implement the approved 78-skill sandbox contract as a separate module with native progression and complete effects.
**Architecture:** existing Forge profile separation, native Puffish state, three independent effect families, one bounded shared action/selection protocol. Native financial/town operations stay in their existing source owners.
**Tech stack:** Java17, Forge1.20.1-47.4.10, installed native APIs, existing ForgeGradle/reobfuscation. **Spec:** this document's full sandbox migration section.

Global constraints are the delivery/ownership rules above. Work in the existing main checkout; no worktree/branch, tests, automated scenarios, static-analysis tools or game launch. Required artifact compilation and Atlas data maintenance are permitted by project standing authorization. No extra design-approval round: user explicitly says “Implement this contract.”

Review focus: full-inventory/insufficient-material results; stale packets/unclaim during operations; offline/restart financial recovery; optional/native class loading and exact artifact versions; GUI scaling and server-authoritative target validation. Inspect these in source and compilation; leave runtime acceptance explicitly unproven.

- [x] Shared action/progression/UI slice: root owns JEMProfessions build.gradle, JemProfessions, profile bridge, sandbox network/client/support/data and description resources. `SandboxProfessionSupport` exposes `enabled(player,profession,node)`, `record(player)`, `cooldown(player,key,ticks)`, `credit(player,profession,kind,source,amount,counter,cap,receipt)` and `message(player,ru,en)`. Action handler signature for each effect family: `public static boolean activate(ServerPlayer player, String node, CompoundTag input)`. Shared dispatch validates integrated owner, known/unlocked node, bounded input and rate; handlers validate effects/material/targets.
- [x] Core family: worker owns new `src/sandbox/java/com/siirio/jemprofessions/sandbox/core/**`. Implement Architect/Farmer/Fisher/Miner/Explorer effects and their actual XP signals. `SandboxCoreEffects.register()` registers its native events; `activate` routes its active nodes. Existing production ProfessionGameplay is not registered in sandbox, preventing duplicate awards/effects.
- [x] Native family: worker owns new `src/sandbox/java/com/siirio/jemprofessions/sandbox/nativecompat/**` and any new sandbox Mixin resources/classes for exact installed native seams. Implement Engineer/Warrior/Mage, XP and native adapters; `SandboxNativeEffects.register()` and `activate`. Optional classloading is isolated; report exact compile dependencies to root, which owns build.gradle.
- [x] Civic family: worker owns new `src/sandbox/java/com/siirio/jemprofessions/sandbox/civic/**`, required new Economy/CurrencyCore/Settlements workflows and narrow source hooks, preserving unrelated edits in those modules. Implement Merchant/Mayor, recovery and XP. `SandboxCivicEffects.register()` and `activate`; root owns shared action UI. Package explicit sandbox variants when required. Shared `jem_professions_sandbox` marker exists only in sandbox artifacts; native owner hooks use that marker to keep ordinary profile behavior unchanged.
- [x] Integration: root reviews each family against the approved table, compiles only production sandbox artifacts, repairs real compiler failures, and requests source review. Build logs are compilation evidence, not tests or runtime acceptance.
- [x] Delivery: root backs up and installs matching exact artifacts/dependency closure only into closed jem sandbox; records hashes and source coverage; updates Atlas/docs/changelog; stages exact task paths, commits/pushes main and verifies remote HEAD. Ordinary local Server and published native generation stay as observed unless the approved sandbox boundary requires otherwise.

Ruling: use three independent implementation workers under the dispatching/subagent-development skills, with fixed file ownership — the effect families have separate native owners and can be implemented concurrently without editing the shared protocol/UI. Root integrates/reviews them. Wrong interface assumptions cost compilation repair; no worker may commit, push, build, launch or overwrite another worker's changes.

Ruling: restrict free claim/unclaim to the integrated world owner, while normal skill effects/actions accept real authenticated integrated/LAN players — otherwise a second real player could not buy, bid or participate in native revival. Dedicated servers still reject every sandbox action. This narrows the privileged route without making multiplayer commerce inert; incorrect guest admission would cost a LAN authorization correction.

Ruling: parameterized skills open their action form from the assigned profession key; direct actions use the key immediately, and ledge hold uses press/release heartbeats. Choose a skill, click `Назначить на клавишу`, then assign the separate profession action control in Minecraft settings. F7 or `/professions` opens the tree. Free claim/unclaim uses the visible `Свободное изучение` switch and the selected node’s learn/remove button. It changes native unlocks and a per-node test point source without changing earned XP.

Ruling: normal native graph clicks enforce level and parent in a narrow server Mixin, because Puffish `required_points` is remaining points rather than a level requirement. Only an exact owner/player/profession/node server scope may bypass admission for free claims; a client force flag is never sufficient. Legacy migration has a separate scope constrained to already recorded historical unlock keys.

Ruling: commerce receipts use a pending native-XP target/audit/daily-cap checkpoint before XP is applied. Recovery saves the native Puffish owner and audit before removing the checkpoint. It is an interrupted-operation journal, not a second live XP ledger. Taxed native villager purchases deliver into real inventory through the durable goods journal; nonempty cursor or side-input slots reject before charging with an explanation. Native window, offers, slots, restocking and trade callbacks remain. Untaxed purchases retain their existing delivery behavior. Supporting arbitrary taxed cursor/drop movement across crashes would require tracking the full native cursor lifecycle; this implementation chooses recoverable inventory delivery instead.

Ruling: source review corrected actual fishing-loot interception, persistent ore/discovery provenance, transient ledge gravity, greenhouse dimension permissions, terminal receipt replay, town-purpose refunds and unloaded guard restoration. Create schematics project into the real client schematic directory with bounded transfer and acknowledgement before consuming a blank. Build and archive inspection do not establish gameplay correctness or visual acceptance.

## Historical eight-effect sandbox delivery — superseded by the approved migration

User asks to bring the existing test-server profession tree into `jem sandbox` and prefers the prior reference for cohesive wooden panels, icons and compact arrangement. Build `Mods/JEMProfessions` with `sandboxJar`: separate output `build/sandbox-profile/libs/jem-professions-1.0.0-sandbox.jar`. Default production profile retains its original radial screen and unlock behavior; its source screen moved byte-for-byte into src/production. No ordinary runtime JAR is replaced.

F7 or `/professions` opens the authoritative native snapshot in an integrated sandbox world. Source-owned JEMProfessions gameplay and canonical78node catalog/data remain owners. Puffish Skills0.19.2 owns XP, levels, points, unlocked skills and persistence. Existing real IdentityPolicy resolves the integrated owner UUID; the Identity1.0.0 already embedded in full Settlements satisfies it. No second XP counter, copied progression state or identity adapter is introduced.

Sandbox packaging reads canonical `Client/ManagedPack/config/paxi/datapacks/jem-profession-skills/data` and embeds native data resources into its own build output. It does not rewrite Client, Server or source graph. Missing Create/Iron item icons are presentation-mapped to recognizable Minecraft items through sandbox-icons.json; no whole Create/Iron distribution is added. Exact native Puffish and TinyInv1.1.0.1 plus its existing default config are installed only into sandbox. Paxi is not required because native SkillsMod resource reload reads the embedded `puffish_skills/config.json` namespace.

Availability is deliberately honest: seven existing core effects (ore_sense, vein_miner, vein_master, stone_step, lava_shield, cave_breath, double_catch) and TinyInv architect_hotbar are available. Other catalog entries lack active standalone effect owners; they are visibly unavailable rather than charging points for fictional gameplay. One sandbox availability resource feeds UI, server unlock admission and native packaged requirements. Sandbox packet admission checks support, actual native AFFORDABLE state, positive points and existing level/parent requirements. Native unavailable definitions require2147483647points; TinyInv node retains a real required_mods condition. Existing sandbox worlds may still retain historical native unlock state; no saves are rewritten. Production policy is not silently changed by this sandbox-specific correction.

The selected profession displays connected branches, semantic item nodes, live points/levels, native unlock state, readable effect/requirement text and a fixed detail panel. Black race-origin stage remains a different protected visual contract. Source/build evidence does not establish runtime appearance or gameplay acceptance. No tests or gameplay client were launched.

Exact artifacts, hashes, backups and sandbox target: [delivery](../settlements/standalone/sandbox-only-iteration-delivery.json).
