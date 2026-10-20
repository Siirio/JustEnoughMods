# Боссы — Stage 3.5B

The Frost figures and difficulty branch below describe the historical native/pre-revamp snapshot. Current2026-10-20 dedicated corrections and calculated before/after values are in `Balance.md` and `FORMULAS.md`; no all-boss DPS observation is claimed.

`Catalog/views/power/bosses.json` содержит 61 босса: 56 зарегистрированных JEM-профилей и 5 дополнительных native boss candidates. Учтены Legendary Monsters, Cataclysm, Bosses'Rise (`block_factorys_bosses`), Luminous Beasts, Luminous Nether, Aquamirae, Alex's Caves, Iron's Spells, Companions и vanilla bosses.

Для всех 61 найден exact-version owner class либо официальный mapped Minecraft/Forge class. Исправленный extractor понимает как mapped `AttributeSupplier.Builder.add`, так и реальный Forge 1.20.1 вызов `m_22268_`: exact native attributes теперь есть у 60/61 боссов, а `ATTACK_DAMAGE` — у 56/61.

## Исправленный denominator

Прежние `231 offensive methods` включали 76 обработчиков входящего урона: `m_6469_`/`hurt`, `reallyHurt`, `processHurt`, `hurtParts` и `applyPartDamage`. Их вызов `super.hurt(...)` ошибочно считался атакой босса. Stage 3.5B сохраняет эти методы в `inbound_damage_handlers`, но исключает из offensive denominator.

| Метрика | Stage 3 | Stage 3.5B |
|---|---:|---:|
| Boss records | 61 | 61 |
| Exact owner classes | 61 | 61 |
| Bosses с exact native attributes | 2 | 60 |
| Bosses с exact native base melee | 0 отдельных каналов | 56 |
| Traced offensive special methods | 231, включая inbound false positives | 155 |
| Resolved special formulas | 14 | 22 |
| Inbound handlers в offensive denominator | 76 | 0 |
| Registered bosses с absolute scaling table | multiplier-only | 54/56 |
| Absolute Tier/player/Raid melee rows | 0 | 3 240 |

`base_melee_channel` — доказанный `ATTACK_DAMAGE` attribute через inherited/native melee handling. Он не означает, что AI гарантированно попал, и не подменяет projectile/AoE damage.

## JEM scaling и абсолютный melee

Для каждого зарегистрированного босса и каждого World Tier используется tier-specific profile `minecraft:generic.attack_damage`, если профиль его задаёт; иначе exact native `ATTACK_DAMAGE`:

`base × (1 + 0.15 × tier) × non-Raid special × hosted damage factor × Raid factor × 0.88`

Raid пропускает profile `special_damage_multiplier`, но использует собственный raid factor. Каждая из 3 240 строк хранит raw damage и потерю HP против unarmored, full iron, full diamond, full netherite и full netherite Protection IV. Reference armor применяется только к обычному не-bypass DamageSource.

Пример Frostbitten Golem с native melee `16`:

| Контекст | До защиты | Full netherite | Netherite Protection IV |
|---|---:|---:|---:|
| Tier I, 1 игрок, hosted | 43.7184 | 24.034068 | 8.652264 |
| Tier V, 4 игрока, hosted non-Raid | 154 | 129.36 | 46.5696 |
| Tier V, 4 игрока, Raid | 77 | 62.832 | 22.61952 |

Это раскрывает прежнее абстрактное `×9.625`: для native melee оно действительно даёт `154` до защиты. Но другие Frostbitten attacks используют собственные параметры и не обязаны равняться melee attribute.

## Закрытые специальные формулы

- Frostbitten Golem `attackTargets`: `base = 14 × configured multiplier`; HARD `base−4`, NORMAL `base`, EASY `base+13`. В effective config multiplier равен `1.0`; нелогичный порядок difficulty находится в установленном коде и не исправлялся.
- Frostbitten Golem `AreaAttack`: `damage_parameter × configured multiplier`; доказаны call variants `12` на animation tick 16 и `13` на ticks 18/12 с разными hitbox.
- Ignis `AreaAttack`/`BodyCheckAttack`: `attack_damage × damage_parameter + target_max_health × hpdamage_parameter`. Exact call-site пары идут от `(0.4, 0.03)` до `(2.0, 0.25)`.
- Ignis `Poke`: `attack_damage + 10% target max health`.
- Ignis mounted sequence: `4 + 2% target max health` каждые 4 ticks начиная с tick 10; до dismount tick 46 планируется до 10 применений, но accepted hits зависят от iframe/state.
- Maledictus `ComboAreaAttack`/`Grab`: `DMG × damage + min(DMG × damage, target max health × hpdamage)`; config HP term остаётся точным именованным input.
- Leviathan bite: `attack_damage + 10% target max health`.

Полные structured overrides находятся в `Catalog/power-sources/boss-combat-stage35b.json`; исходный bytecode evidence остаётся в `native-mechanics-phase3.json`.

## Что всё ещё нельзя назвать DPS

Для каждого босса остаётся frequency blocker. Animation tick или modulo schedule доказывает попытку атаки, но не вероятность выбора AI-ветки, положение цели, прерывание, успешность hitbox, iframe acceptance и смену цели в multiplayer. Поэтому Stage 3.5B не умножает один attack на теоретическое число тиков и не выдаёт scheduling upper bound за sustained DPS.

Полный TTK/TTD также требует конкретный loadout, Resistance/absorption/shield, damage-type tags и runtime accepted-hit trace. Эти пробелы перечислены в `Catalog/views/power/blockers.json`; ни один nerf, buff, config override или gameplay patch не применён.
