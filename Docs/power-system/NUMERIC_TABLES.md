# Stage 3 numeric tables

Канонический машинный результат находится в `Catalog/views/power/calculations.json`. Все числа вычисляются только из exact installed JAR/data и подтверждённых JEM-формул; неизвестное значение остаётся символическим.

## Текущее покрытие

| Таблица | Количество | Что численно доказано |
|---|---:|---|
| Spell damage | 45 | Каждый уровень, PvE World Tier I–V, PvP и потеря HP против пяти vanilla reference targets для одного native damage application |
| Registered boss scaling | 56 | Tier I–V, 1–6 игроков, hosted/Raid health и owned-attack multiplier над native значением |
| Weapon cards | 177 | Для каждого weapon ID: exact factory/owner, basic attributes где доказаны, offensive methods, config, durability и capability evidence |
| Defense cards | 219 | Exact attributes, durability и passive/active method evidence по каждому defense ID |
| Four-piece armor-set cards | 39 | Сумма только доказанных attributes для полного registry stem; set bonus не выводится из имени |
| Accessory cards | 52 | Attributes, active/passive methods, cooldown/config evidence и явные Curios blockers |
| Spell method cards | 106 | Owner/delegated methods для всех spells; 67 effect/combat methods прослежены |
| Boss attack tables | 61 | 231 offensive methods прослежены; 14 имеют literal numeric sink result |
| Vanilla mitigation | 1 | Armor/toughness/Protection для входящего урона 1–160 HP |

Полный combat inventory содержит 2 365 записей. Из 654 item/spell/boss calculation cards полностью resolved `0`, partial `368`, unresolved `286`: ни одна карточка не называется полностью resolved, пока не известны все обязательные native каналы, runtime ItemStack, acquisition и representative target context. Это не отменяет локально закрытых полей: 34 weapons имеют exact basic melee, 12 из 63 traced weapon offensive methods — literal-numeric, а 14 из 231 boss offensive methods — literal-numeric.

## Exact basic melee examples

| ID | Damage addition | Player base | Basic pre-mitigation | Скорость |
|---|---:|---:|---:|---:|
| `skarrier_mobs:ban_sword` | 1499 | 1 | 1500 | -2.4 |
| `efn:ruinsgreatsword` | 15 | 1 | 16 | -3.0 |
| `cataclysm:ceraunus` | 15 | 1 | 16 | ≈-3.3 |
| `cataclysm:the_incinerator` | 13 | 1 | 14 | ≈-2.6 |
| `legendary_monsters:enderitium_axe` | 8.5 | 1 | 9.5 | -3.0 |
| `legendary_monsters:enderitium_sword` | 6.5 | 1 | 7.5 | -2.4 |

`ban_sword` — source-verified severe outlier only if player-obtainable: Atlas не нашёл recipe/loot/acquisition route. Stage 3 не превращает отсутствие route в доказательство недоступности.

## Эталонные цели

- без брони;
- полный iron: 15 armor, 0 toughness;
- полный diamond: 20 armor, 8 toughness;
- полный netherite: 20 armor, 12 toughness;
- полный netherite Protection IV: те же armor/toughness и EPF 16.

Таблица применима только когда DamageSource не обходит armor/enchantments. Shield, Resistance, absorption, iframe, school resistance и модовые passives — отдельные слои.

## Пример Sunbeam

Level I без caster bonus: spell power 24, native damage `24 × 0.5 = 12`. В PvE Tier I JEM применяет `×0.65`, поэтому до защиты получается `7.8 HP`. Эталонный vanilla full netherite пропускает `2.04672 HP`, а full netherite Protection IV — `0.736819 HP`; Iron school resistance и другие модовые эффекты в эти reference numbers не подставлены.

## Активные и неактивные combat providers

Epic Fight и Nightfall установлены, поэтому их capability/skill JSON являются активными кандидатами. Совпадающие resource IDs из нескольких compat-JAR не объединяются: до доказательства datapack precedence они помечены collision-unresolved. Better Combat не установлен, а Punchy compatibility выключен; `weapon_attributes` внутри Cataclysm являются dormant compatibility resources и не участвуют в расчётах.

Извлечено 111 активных Epic Fight/Nightfall capability records. Полностью числовых combo chains — `0`: animation duration/trail keyframes сами по себе не доказывают attack phase, hit multiplier, iframe и успешное число целей. Они сохраняются как timing evidence, но не выдаются за `Hit 1 → Hit 2` damage table.

## Чего таблица не выдумывает

- число тиков/попаданий continuous beam без трассировки native scheduling;
- projectile, explosion, summon и AI damage, делегированный другому классу;
- config multiplier без доказанного текущего config value;
- animation damage window или stamina behavior, отсутствующий в capability/skill JSON;
- runtime NBT/attribute modifiers конкретного ItemStack;
- school resistance, баффы и target-specific mod integration без заданного сценария.

Установленные TOML разобраны в 3 932 scalar values; dependency становится exact только при единственном совпавшем effective key. Например, Incinerator cooldown доказан как `400 ticks`, но его flame-strike total damage остаётся partial, потому что cooldown не закрывает разветвлённую damage formula.
