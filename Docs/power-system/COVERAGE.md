# Coverage Stage 3.5A после deep boss-reward pass

Машинный источник — `Catalog/views/power/coverage.json`. Текущий combat inventory содержит 2 365 записей:

| View | Identified |
|---|---:|
| Weapon entries | 177 |
| Armor/defense | 219 |
| Registered accessories | 52 |
| Enchantments | 218 |
| Iron's Spells spell IDs | 106 |
| Boss inventory | 61 |
| Other entity candidates | 1 429 |
| Allies/troops/summons/mount candidates | 103 |

Рост weapon view с 87 до 177 не возвращает модельный шум. Stage 3 source-backed добавляет все 36 boss-reward weapon IDs и active Epic Fight/Nightfall capability IDs; пересекающиеся записи остаются одним ItemStack ID. Все 36/36 reward ID зарегистрированы, прошли owner inspection и присутствуют в итоговом view.

## Stage 3.5A: 36 boss-reward weapons

| Метрика | `41b53db` | Сейчас |
|---|---:|---:|
| Канонические reward IDs | 36/36 | 36/36 |
| Exact basic для этих 36 | 20/36 | 36/36 |
| Численно закрытые special branches/abilities | 9 у reward-36 | 38 у reward-36 |
| Полные verified combo chains | 0 | 0 |
| Weapons с доказанным cooldown | 13/36 | 23/36 |
| Burst upper-bound calculations | 0 | 6 |
| Sustained cooldown windows | 0 | 18 |
| Закрытые durability profiles | 6/36 reward profiles | 34/36 reward profiles |
| Все named blockers | 4 105 | 3 900 |
| Точные Stage 3.5A blockers | — | 40, из них 31 runtime |

`20/36` для прежнего exact basic вычислен по карточкам boss-reward в снимке `41b53db`; глобальная прежняя метрика оставалась `34/177`. Новые 36 значений включают честный `1` для bow/utility предметов без main-hand damage modifier — это обычный melee игрока, не projectile damage.

Снижение blockers произошло не из-за переименования: для этих 36 отключена генерация повторяющихся dimension-заглушек и оставлены только конкретные остатки — capability precedence, animation phase, projectile overlap, iframe либо branched helper. 31 поле действительно требует минимального server-authoritative наблюдения; Minecraft в этой задаче не запускался.

## Numeric coverage

- 45 spells имеют безопасную `getDamage` формулу и полную таблицу по level, PvE Tier I–V, PvP и пяти reference targets.
- 56 зарегистрированных JEM boss profiles имеют таблицу Tier I–V × 1–6 players × hosted/Raid; пять дополнительных native boss candidates не получают выдуманный JEM profile.
- 177/177 weapons, 219/219 defenses, 52/52 accessories, 106/106 spells и 61/61 bosses имеют calculation cards, даже когда результат честно unresolved.
- 34 weapons имеют exact basic melee total; остальные не получают число из сырой константы.
- 63 offensive weapon methods прослежены до bounded owner chain, 12 имеют literal-numeric sink result.
- 231 offensive boss methods прослежены, 14 имеют literal-numeric sink result; ещё две combat method records относятся к support/recovery и не раздувают attack denominator.
- 67 spell effect/combat methods прослежены; 45 spells имеют безопасную per-application damage table.
- 39 четырёхчастных armor set stems сгруппированы без выдумывания set bonus.
- Vanilla mitigation reference содержит armor/toughness/Protection результаты для входящего 1–160 HP.
- 106/106 spell, 61/61 boss и 95/103 ally owner classes связаны.

## Resolution denominator

| Статус calculation cards | Количество |
|---|---:|
| Fully resolved | 0 |
| Partial | 368 |
| Unresolved | 286 |

Ноль fully resolved — намеренный строгий критерий: карточка должна закрыть не только один damage literal, но также обязательные runtime ItemStack, special/combo, target mitigation и acquisition context. Локально доказанные числа при этом не теряются.

## Provider boundary

- Epic Fight/Nightfall: установлен и активен; 111 capability records связаны с итоговыми weapon cards, но полностью числовых combo chains `0`.
- Better Combat: не установлен; Punchy `betterCombatCompat=false`. 60 embedded `weapon_attributes` profiles помечены inactive и не применяются в damage/combo calculations.

## Что остаётся unresolved

- helper-built registrations и runtime ItemStack/NBT attributes;
- projectile/explosion/effect damage, делегированный из item/spell/boss owner;
- continuous hit scheduling и итоговый hit count;
- Epic Fight animation hit windows и Java skill behavior за пределами capability/skill JSON;
- boss/ally арифметика в goals/projectiles/effects;
- runtime target effects, school resistance, shields, iframe и acquisition probability;
- конкретный наблюдавшийся beam ItemStack.

## Durability и blockers

- Audit охватывает 448 weapon/defense/accessory records.
- Exact max damage найден для 25, consumption path — для 21, обе стороны — для 8.
- Tier-based durability читается из `Tiers.uses`/`ForgeTier.uses`; например Enderitium `2100`, Netherite-tier Ruinsgreatsword `2031`.
- 3 900 named blocker records перечисляют ID, missing field, owner/candidate method, причину и необходимость runtime observation; reward-36 больше не создаёт прежние boilerplate dimensions.

`not_observed` никогда не означает `0` или «механики нет».
