# Боссы — Phase 3

`Catalog/views/power/bosses.json` содержит 61 босса: 56 зарегистрированных JEM-профилей и 5 дополнительных native boss candidates. Учтены Legendary Monsters, Cataclysm, Bosses'Rise (`block_factorys_bosses`), Luminous Beasts, Luminous Nether, Aquamirae, Alex's Caves, Iron's Spells и vanilla bosses.

Для каждой из 61 записи найден exact-version owner class либо официальный mapped Minecraft/Forge class. Stage 3 идёт глубже прежних 11 channel-сигналов: ограниченно следует только реально вызванным или созданным goal/projectile/effect классам и сохраняет конкретные методы с `hurt`, projectile damage, explosion, effect, healing и cooldown sinks. Class literal для AI target, cast/`instanceof` и registry field не дают права импортировать методы чужой сущности. Offensive attack records отделены от recovery/support methods. `not_observed` означает только отсутствие сигнала в доказанной цепочке, а не отсутствие атаки.

## Состав boss attack table

| Поле | Что хранится | Когда считается числом |
|---|---|---|
| `native_attributes` | HP, movement, attack, armor, toughness, knockback из attribute builder | Literal, привязанный к конкретному attribute |
| `attack_methods` | Owner class, точный method declaration, calls, sinks, constants, config references | Только доказанный literal sink argument либо безопасно свёрнутая формула |
| `support_and_recovery_methods` | Healing/cooldown/служебный износ вне offensive denominator | Не увеличивает счётчик boss attacks |
| `delegated_classes` | Прослеженные goal/projectile/effect owners | Сам факт класса не означает готовый урон |
| `resolution` | Число traced/fully numeric attacks | Частота и recovery остаются blockers, пока scheduler их не доказывает |

Текущий denominator: 231 offensive method records, из них 14 literal-numeric; две дополнительные combat records относятся к support/recovery и не считаются атаками. Это покрытие методов, а не обещание, что все возможные AI transitions каждой сущности уже перечислены.

Карточка также хранит полный зарегистрированный JEM profile: native threat, T1–T5 attributes, special damage multiplier, damage/healing/stagger adapters и arena/anti-cheese ownership.

Общий damage chain для зарегистрированного не-event босса:

`native channel × (1 + 0.15 × world tier) × optional non-Raid special multiplier × hosted participant damage factor`

Для attack, принадлежащей hosted boss, после этого применяется `×0.88`. Raid не получает profile `special_damage_multiplier`, но использует собственные raid/party factors.

`Catalog/views/power/calculations.json` содержит 56 полных таблиц JEM scaling (по одной на зарегистрированный профиль): Tier I–V × 1–6 игроков × обычный hosted/Raid, а также 61 attack table. Scaling — точный множитель над native health/attack, но не подмена ещё не сведённой арифметики projectile/goal/effect.

Критический source finding: Frostbitten Golem имеет special multiplier `[2.7, 2.8, 3.2, 5.0, 5.0]`. Например, Tier V hosted на четырёх игроков вне Raid даёт owned attack `×9.625` от native; Raid в том же сценарии пропускает special и даёт `×4.8125`. Stage 3 это не меняет, только численно раскрывает существующую цепочку.

Полный TTK/TTD не заявлен там, где native attack formula, частота, iframe или representative loadout ещё неизвестны. Для каждого такого поля `blockers.json` называет entity ID, owner class, candidate methods и объясняет, почему runtime observation требуется или не требуется.
