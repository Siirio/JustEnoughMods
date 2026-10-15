# Stage 3.5C numeric tables

Канонический машинный результат находится в `Catalog/views/power/calculations.json`. Все числа вычисляются только из exact installed JAR/data и подтверждённых JEM-формул; неизвестное значение остаётся символическим.

## Текущее покрытие

| Таблица | Количество | Что численно доказано |
|---|---:|---|
| Spell damage | 45 | Каждый уровень, PvE World Tier I–V, PvP и потеря HP против пяти vanilla reference targets для одного native damage application |
| Registered boss scaling | 56 | Tier I–V, 1–6 игроков, hosted/Raid health/attack multiplier; 54 профиля дополнительно имеют абсолютный melee и reference mitigation |
| Weapon cards | 177 | Для каждого weapon ID: exact factory/owner, basic attributes где доказаны, offensive methods, config, durability и capability evidence |
| Defense cards | 219 | Exact attributes, durability и passive/active method evidence по каждому defense ID |
| Four-piece armor-set cards | 39 | Сумма только доказанных attributes для полного registry stem; set bonus не выводится из имени |
| Accessory cards | 52 | Attributes, active/passive methods, cooldown/config evidence и явные Curios blockers |
| Spell method cards | 106 | Owner/delegated methods для всех spells; 67 effect/combat methods прослежены |
| Boss attack tables | 61 | 155 настоящих offensive methods после удаления 76 inbound handlers; 22 имеют resolved formula, ещё 56 боссов имеют exact base-melee channel |
| Vanilla mitigation | 1 | Armor/toughness/Protection для входящего урона 1–160 HP |
| Boss-reward weapon deep cards | 36 | Basic, specials, conditional World Tier, cooldown windows, durability and exact blockers |
| Exact armor material profiles | 125 | Armor, toughness, knockback resistance and nominal max damage from installed constructors/getters |
| Complete comparable armor sets | 24 | Four exact pieces, five conditional Progression tiers and nine incoming-damage scenarios |
| Armor mitigation outcomes | 1 080 | 24 sets × 5 tiers × 9 attacks, including bypass semantics and EHP |
| Exact Curios slots / attributes | 22 / 10 | Installed item tags plus factory attribute containers, without inferred effects |
| Explicitly unbreakable armor | 8 | Exact constant-false `isDamageable` override, separated from unresolved wear |

Для reward-36 теперь доказаны все 36 обычных melee значений и 38 числовых special branches/abilities. Шесть способностей имеют статически безопасную activation upper bound и поэтому дают 18 отдельных cooldown-limited окон 30/60/120 секунд. Это **special-only upper bounds**, а не полный sustained DPS оружия: обычные атаки, stamina и ещё не доказанные successful hit counts туда не подмешиваются.

Stage 3.5B добавляет 3 240 абсолютных boss-melee строк: 54 зарегистрированных профиля × 5 World Tier × 6 размеров группы × hosted/Raid. Каждая строка начинается с tier-specific profile attack attribute либо exact native `ATTACK_DAMAGE`, затем применяет JEM composition и пять reference armor targets. Два зарегистрированных профиля (`aquamirae:captain_cornelia`, `legendary_monsters:bomber`) остаются без доказанного base-melee числа; ranged/special поведение не объявляется нулём.

Stage 3.5C добавляет 1 080 одинаково вычисленных defensive outcomes. В таблицу допускается только полный набор, у которого все четыре material-профиля точны; поэтому denominator равен 24, а не всем 39 сгруппированным stems. Armor/Protection bypass обрабатывается по damage-type flags. Модовые пассивы, shield, Resistance, absorption и school resistance остаются отдельными слоями и не маскируются reference-числом.

Ключевые формулы:

- Incinerator: одно принятое попадание `6 + 0.02 × target max HP`, с обходом armor, Protection и shield;
- Laser Gatling: `7` на луч, луч каждые 2 ticks, максимум 49 запланированных лучей до redstone reload; `343` — scheduling upper bound;
- Knight Sword: `slashes = charge<5 ? 0 : min(((charge-5)/20)*2+1, 7)`, каждый projectile `5`;
- Ceraunus: четыре wave entity по `6`; `24` — overlap upper bound, не гарантированный hit;
- Withered Scythe: отдельные `10` dash damage и условные `+41` через 2 ticks против undead;
- Wand of Clouds: `15/max(distance/2,1)` и три отложенных `6/max(distance/2,1)`; fallback large cloud сохраняет default `5`.

Полный combat inventory содержит 2 365 записей. Ни одна составная карточка не называется полностью resolved, пока не известны все обязательные native channels, target mitigation и runtime context. Это не отменяет локально закрытых полей: 36/36 boss-reward weapons имеют exact basic melee; у боссов есть 56 exact base-melee channels и 22 resolved special formulas из 155 корректно классифицированных offensive methods.

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
