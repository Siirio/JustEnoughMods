# Coverage после Stage 3

Машинный источник — `Catalog/views/power/coverage.json`. Текущий combat inventory содержит 2 363 записи:

| View | Identified |
|---|---:|
| Weapon entries | 175 |
| Armor/defense | 219 |
| Registered accessories | 52 |
| Enchantments | 218 |
| Iron's Spells spell IDs | 106 |
| Boss inventory | 61 |
| Other entity candidates | 1 429 |
| Allies/troops/summons/mount candidates | 103 |

Рост weapon view с 87 до 175 не возвращает модельный шум. Stage 3 source-backed добавляет все 36 boss-reward weapon IDs и 104 активных Epic Fight weapon capabilities; пересекающиеся записи остаются одним ItemStack ID. Все 36 reward ID зарегистрированы, прошли owner inspection и присутствуют в итоговом view.

## Numeric coverage

- 45 spells имеют безопасную `getDamage` формулу и полную таблицу по level, PvE Tier I–V, PvP и пяти reference targets.
- 56 зарегистрированных JEM boss profiles имеют таблицу Tier I–V × 1–6 players × hosted/Raid; пять дополнительных native boss candidates не получают выдуманный JEM profile.
- 136 weapon cards содержат хотя бы одно точное числовое/data-driven доказательство: attribute, durability/cost, cooldown, Epic Fight capability или skill parameter.
- Vanilla mitigation reference содержит armor/toughness/Protection результаты для входящего 1–160 HP.
- 106/106 spell и 61/61 boss owner classes по-прежнему связаны; сохраняются 1 060 spell и 671 boss channel slots.

## Provider boundary

- Epic Fight/Nightfall: установлен и активен; извлечено 170 capability и 73 skill-parameter resources.
- Better Combat: не установлен; Punchy `betterCombatCompat=false`. 60 embedded `weapon_attributes` profiles помечены inactive и не применяются в damage/combo calculations.

## Что остаётся unresolved

- helper-built registrations и runtime ItemStack/NBT attributes;
- projectile/explosion/effect damage, делегированный из item/spell/boss owner;
- continuous hit scheduling и итоговый hit count;
- Epic Fight animation hit windows и Java skill behavior за пределами capability/skill JSON;
- boss/ally арифметика в goals/projectiles/effects;
- runtime target effects, school resistance, shields, iframe и acquisition probability;
- конкретный наблюдавшийся beam ItemStack.

`not_observed` никогда не означает `0` или «механики нет».
