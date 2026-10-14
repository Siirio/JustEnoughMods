# Оружие — Phase 3

Основной список находится в `Catalog/views/power/weapons.json`. Phase 2 больше не считает оружием каждую строку или модель, в имени которой встретилось `bow`, `sword` или похожее слово.

В основной список попадают только канонические Minecraft-предметы, точные ID из boss-reward/JEM allowlist либо предметы, для которых найдено регистрационное доказательство в exact-version JAR. Отброшены bowls, spawn eggs, блоки профессий, компоненты апгрейда, `_visual_`, `_pulling_`, `_inventory`, модели руки и другие presentation-only варианты. Поэтому `minecraft:bowl` и визуальные стадии Simply Bows больше не отвечают на `query_atlas.py power`.

Для каждой оставшейся записи хранятся:

- доказательство регистрации и owning artifact;
- совпавший специализированный Item-класс, если он найден;
- сигналы attributes, active/use, projectile/range, AoE, effects, cooldown, resource cost и durability;
- отдельные причины, почему base damage, attack speed, special formula, penetration или Epic Fight moveset ещё не доказаны;
- acquisition routes и применимые JEM-модификаторы.

Сырые numeric constants класса не считаются уроном автоматически: одна константа может быть радиусом, длительностью, шансом или визуальным параметром. Число становится формулой только после привязки к конкретному методу.

Stage 3 исправляет потерю boss-reward оружия: все 36/36 ID из `major_rewards.json` и `expanded_rewards.json` присутствуют и проходят item-owner inspection. К очищенной Phase 2 выборке добавляются source-backed активные Epic Fight capability IDs, даже если имя предмета не проходит эвристику (`efn:ruinsgreatsword` и аналоги).

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

Например, Incinerator имеет exact `attack_damage_addition=13`, `attack_speed_addition≈-2.6`, `entity_reach_addition=2` и установленный `incinerator_cooldown=400 ticks`. Его flame-strike цепочка прослежена до `Flame_Strike_Entity`, но разветвлённые обычный и HP-based damage inputs пока не свёрнуты в один burst; поэтому `14` basic hit не выдаётся за урон способности.

Nightfall/Epic Fight capability, связанный `skill_parameters` и animation resource читаются прямо из установленных JAR. Armor negation, impact, max strikes, consumption, stacks, заданные cooldown и длительность animation resource считаются точными data-driven фактами. Trail/keyframe time не называется active hit window без Java attack-phase evidence. Если несколько установленных compat-JAR поставляют один resource ID, обе версии сохраняются с `resource_collision_precedence_unresolved`, а их числа не складываются. Поэтому число полностью доказанных Epic Fight combo chains остаётся отдельной честной метрикой, а не равно количеству capability JSON.

Better Combat **не установлен** ни на каноническом клиенте, ни на сервере; Punchy задаёт `betterCombatCompat: false`. Встроенные в Cataclysm `weapon_attributes` сохранены только как `inactive_optional_compatibility_data` и не участвуют ни в combo, ни в damage calculations.
