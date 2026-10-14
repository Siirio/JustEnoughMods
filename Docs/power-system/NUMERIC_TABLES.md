# Stage 3 numeric tables

Канонический машинный результат находится в `Catalog/views/power/calculations.json`. Все числа вычисляются только из exact installed JAR/data и подтверждённых JEM-формул; неизвестное значение остаётся символическим.

## Текущее покрытие

| Таблица | Количество | Что численно доказано |
|---|---:|---|
| Spell damage | 45 | Каждый уровень, PvE World Tier I–V, PvP и потеря HP против пяти vanilla reference targets для одного native damage application |
| Registered boss scaling | 56 | Tier I–V, 1–6 игроков, hosted/Raid health и owned-attack multiplier над native значением |
| Weapon cards | 136 | Найденные attribute additions, cooldown, durability/cost, активные Epic Fight capability/skill parameters и применимые progression multipliers |
| Vanilla mitigation | 1 | Armor/toughness/Protection для входящего урона 1–160 HP |

Полный combat inventory содержит 2 363 записи; ни одна запись не называется полностью resolved, пока не известны все native каналы, runtime ItemStack, acquisition и representative target context.

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

## Чего таблица не выдумывает

- число тиков/попаданий continuous beam без трассировки native scheduling;
- projectile, explosion, summon и AI damage, делегированный другому классу;
- config multiplier без доказанного текущего config value;
- animation damage window или stamina behavior, отсутствующий в capability/skill JSON;
- runtime NBT/attribute modifiers конкретного ItemStack;
- school resistance, баффы и target-specific mod integration без заданного сценария.
