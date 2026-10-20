# Оружие — Stage 3.5A

Correction2026-10-20: Laser Gatling sets five seconds of fire, not five invulnerability ticks. Its49scheduled beams are not49accepted hits. Five Cataclysm collisions now have explicit JEM specialized native preset declarations; see `Balance.md` for current implementation. Runtime combo DPS remains unresolved.

Основной список находится в `Catalog/views/power/weapons.json`. Phase 2 больше не считает оружием каждую строку или модель, в имени которой встретилось `bow`, `sword` или похожее слово.

В основной список попадают только канонические Minecraft-предметы, точные ID из boss-reward/JEM allowlist либо предметы, для которых найдено регистрационное доказательство в exact-version JAR. Отброшены bowls, spawn eggs, блоки профессий, компоненты апгрейда, `_visual_`, `_pulling_`, `_inventory`, модели руки и другие presentation-only варианты. Поэтому `minecraft:bowl` и визуальные стадии Simply Bows больше не отвечают на `query_atlas.py power`.

Для каждой оставшейся записи хранятся:

- доказательство регистрации и owning artifact;
- совпавший специализированный Item-класс, если он найден;
- сигналы attributes, active/use, projectile/range, AoE, effects, cooldown, resource cost и durability;
- отдельные причины, почему base damage, attack speed, special formula, penetration или Epic Fight moveset ещё не доказаны;
- acquisition routes и применимые JEM-модификаторы.

Сырые numeric constants класса не считаются уроном автоматически: одна константа может быть радиусом, длительностью, шансом или визуальным параметром. Число становится формулой только после привязки к конкретному методу.

Stage 3.5A углубляет именно boss-reward оружие: все 36/36 уникальных ID выводятся из category полей `major_rewards.json` и `expanded_rewards.json`, проходят строгую проверку равенства множеств и имеют отдельный exact-version профиль. Повтор Incinerator из постановки не создаёт 37-ю запись.

Итоговый view содержит 177 weapon IDs. Для каждого создана numeric card; exact basic melee получен для 34. Offensive chain содержит 63 traced methods, из них 12 имеют доказанный literal numeric sink. Остальные методы сохраняют owner/calls/config inputs и конкретный blocker вместо выдуманного результата.

## Как получается basic melee

Для `SwordItem`/`AxeItem`-подобной фабрики Stage 3 разрешает `DeferredRegister` supplier через настоящую JVM `BootstrapMethods` запись, читает constructor damage/speed arguments и Tier attack bonus. Итоговая basic attack до защиты — `player base 1 + attack_damage_addition`. Это не используется вместо projectile, spell или special damage.

| ID | Exact factory | Damage addition | Basic total | Speed addition | Max durability source |
|---|---|---:|---:|---:|---|
| `legendary_monsters:enderitium_sword` | `SwordItem(Tier, 3, -2.4)` | `3 + 3.5 = 6.5` | `7.5` | `-2.4` | Enderitium ForgeTier: `2100` |
| `legendary_monsters:enderitium_axe` | `AxeItem(Tier, 5, -3.0)` | `5 + 3.5 = 8.5` | `9.5` | `-3.0` | Enderitium ForgeTier: `2100` |
| `efn:ruinsgreatsword` | `RuinsgreatswordItem(Tiers.NETHERITE, 11, -3.0)` | `11 + 4 = 15` | `16` | `-3.0` | Netherite tier: `2031` |
| `cataclysm:the_incinerator` | `The_Incinerator` / Cataclysm weapon base | `13` | `14` | `-2.6` | Separate owner path; see durability card |

Этот разбор также устраняет прежний false-owner риск: совпадение слов `axe`/`sword` больше не позволяет Enderitium наследовать механику `Axe_Of_LightningItem` или другого соседнего класса.

## Спецатаки и power ladder

| Выход | Что в нём есть | Чего он не утверждает |
|---|---|---|
| `calculations.json` weapon card | Basic attributes, offensive owner methods, exact config refs, durability/cost, capabilities | Что любая найденная константа является уроном |
| `power-ladder.json` | Exact basic-melee sub-ranking, armor negation и boss-reward intent | Полный actual rank при неизвестном special/combo DPS |
| `blockers.json` | Конкретные missing combo/special/acquisition fields | Generic «нужно исследовать» без owner/method |

Например, Incinerator имеет exact `attack_damage_addition=13`, `attack_speed_addition≈-2.6`, `entity_reach_addition=2` и установленный `incinerator_cooldown=400 ticks`. Его отдельное принятое flame-strike попадание теперь доказано как `6 + 2% max HP`; damage type обходит броню, Protection и щит. Общий burst всё ещё не выдуман: десять отложенных зон и повторные проверки могут пересекаться только в зависимости от геометрии и iframe.

Nightfall/Epic Fight capability, связанный `skill_parameters` и animation resource читаются прямо из установленных JAR. Armor negation, impact, max strikes, consumption, stacks, заданные cooldown и длительность animation resource считаются точными data-driven фактами. Trail/keyframe time не называется active hit window без Java attack-phase evidence. Если несколько установленных compat-JAR поставляют один resource ID, обе версии сохраняются с `resource_collision_precedence_unresolved`, а их числа не складываются. Поэтому число полностью доказанных Epic Fight combo chains остаётся отдельной честной метрикой, а не равно количеству capability JSON.

Better Combat **не установлен** ни на каноническом клиенте, ни на сервере; Punchy задаёт `betterCombatCompat: false`. Встроенные в Cataclysm `weapon_attributes` сохранены только как `inactive_optional_compatibility_data` и не участвуют ни в combo, ни в damage calculations.

## Все 36 boss-reward предметов

`Basic` — полный обычный удар до защиты. Для bow/utility предметов `1` означает только базовый melee игрока; рядом отдельно указана способность. `Burst` — только доказанная величина либо явно названная верхняя граница. `∞` означает доказанно неуменьшаемую прочность и является balance issue, а не рекомендацией.

| Registry ID | Basic | Доказанная способность / burst | CD, ticks | Durability | Epic Fight/Nightfall |
|---|---:|---|---:|---:|---|
| `block_factorys_bosses:ice_gauntlet` | 5 | spike branch 4; fall/burst branch 10, total runtime | 80 | 1200 | нет capability |
| `block_factorys_bosses:knight_sword` | 14 | 1/3/5/7 waves × 5; max 35 | 80 | 1480 | нет capability |
| `block_factorys_bosses:kraken_trident` | 10 | throw 8 + AoE 5; direct-target upper bound 13 | — | 2500 | нет capability |
| `block_factorys_bosses:sandworm_gauntlet` | 1.5 | sand column 5/application | — | 1000 | нет capability |
| `block_factorys_bosses:undying_tentacle` | 3 | ghost entity owner traced; total runtime | 40 | 1000 | нет capability |
| `cataclysm:ancient_spear` | 9.5 | sandstorm 6/application | — | ∞ | standard spear, phases open |
| `cataclysm:astrape` | 10.5 | lightning 11 direct + 2 area; upper 13 | 60 | ∞ | p1nero Astrape, phases open |
| `cataclysm:ceraunus` | 16 | throw 16; 4 waves × 6, overlap upper 24 | 150 | ∞ | p1nero ARC, phases open |
| `cataclysm:cursed_bow` | 1 | full charge 8; Power `+0.35L+0.5` | — | ∞ | resource collision |
| `cataclysm:gauntlet_of_bulwark` | 11 | charge/control, Blazing Brand 40 | 80 | ∞ | нет capability |
| `cataclysm:gauntlet_of_guard` | 11 | guard/pull | — | ∞ | p1nero Bedivere, phases open |
| `cataclysm:gauntlet_of_maelstrom` | 11 | vortex total runtime | 180 | ∞ | нет capability |
| `cataclysm:infernal_forge` | 13 | earthquake expression branched | 80 | ∞ | mixed greatsword/p1nero |
| `cataclysm:laser_gatling` | 1 | 49 scheduled beams × 7 = 343 upper bound | 20 reload | 50 | ranged bow |
| `cataclysm:meat_shredder` | 8.5 | `(attack+enchant)/8.5` per use tick | — | ∞ | standard sword, phases open |
| `cataclysm:sandstorm_in_a_bottle` | 1 | 2 storms for 200 ticks; accepted hits runtime | 300 | ∞ | нет capability |
| `cataclysm:soul_render` | 15 | halberd 12/application; rush stores attack damage | 100 | ∞ | resource collision + p1nero |
| `cataclysm:the_annihilator` | 8.5 | charged AoE `2×attack` = 17 base | 100 | ∞ | resource collision + p1nero |
| `cataclysm:the_incinerator` | 14 | application `6 + 2% max HP`, armor/Protection/shield bypass | 400 | ∞ | resource collision + p1nero |
| `cataclysm:tidal_claws` | 8 | hook range 30; tendon total runtime | — | ∞ | p1nero/EFN claw, phases open |
| `cataclysm:wrath_of_the_desert` | 1 | full charge `7.5×(1+0.05×Power)` | — | ∞ | resource collision |
| `legendary_monsters:atom_splitter` | 1 | beam 16/application; bomb 10, radius parameter 16 | 30 | 4 | нет capability |
| `legendary_monsters:axe_of_lightning` | 13 | electricity = attack damage; bolt 8 | 60/90 | ∞ | нет capability |
| `legendary_monsters:chorus_blade` | 7 | 8 to each living target in radius 8; 50% infection | 80 | 750 | нет capability |
| `legendary_monsters:enderitium_axe` | 9.5 | native axe only | — | 2100 | нет capability |
| `legendary_monsters:enderitium_sword` | 7.5 | native sword only | — | 2100 | нет capability |
| `legendary_monsters:knights_sword` | 6 | native sword only | — | 250 | нет capability |
| `legendary_monsters:monstrous_anchor` | 14 | grounded hit +6 = 20; slam stun/launch | 100 | 1100 | нет capability |
| `legendary_monsters:mossy_hammer` | 9 | two handlers each expose 10 AoE | 30/60 | 850 | нет capability |
| `legendary_monsters:shattered_greatsword` | 14 | native greatsword only | — | 1500 | нет capability |
| `legendary_monsters:soul_great_sword` | 13 | 6 daggers × 8 = 48 overlap upper bound; Soul Rage | 120/280 | ∞ | нет capability |
| `legendary_monsters:the_great_frost` | 7 | spike 3/application, up to 4 checks; 25% Freeze | 50/80 | 750 | нет capability |
| `legendary_monsters:the_tesseract` | 15 | portal branches 8 or 12.5/application | 150 | ∞ | нет capability |
| `legendary_monsters:void_sword` | 10 | native sword only | — | 250 | нет capability |
| `legendary_monsters:wand_of_clouds` | 11 | center upper 15 + 3×6 = 33; fallback 23 | 80 | 2000 | нет capability |
| `legendary_monsters:withered_scythe` | 10 | dash 10; +41 after 2 ticks against undead | 75/120 | 700 | нет capability |

Полные записи с owner class, формулой, эффектами, ресурсами, World Tier I–V, reference armor и конкретными blockers находятся в `Catalog/views/power/weapons.json` и `calculations.json`. Ни один upper bound не переименован в гарантированный урон.
