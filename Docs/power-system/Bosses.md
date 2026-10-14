# Боссы — Phase 3

`Catalog/views/power/bosses.json` содержит 61 босса: 56 зарегистрированных JEM-профилей и 5 дополнительных native boss candidates. Учтены Legendary Monsters, Cataclysm, Bosses'Rise (`block_factorys_bosses`), Luminous Beasts, Luminous Nether, Aquamirae, Alex's Caves, Iron's Spells и vanilla bosses.

Для каждой из 61 записи найден exact-version owner class либо официальный mapped Minecraft/Forge class. Каталог создаёт 11 независимых channel slots: melee, AoE, projectile, beam, explosion, magic/custom damage, DoT, summons, grab/stun, healing/shield и phase/enrage. В сумме это 671 проверяемый слот. `not_observed` означает только отсутствие сигнала в выбранном owner-классе; делегированный goal/projectile/effect остаётся названной причиной unresolved.

Карточка также хранит полный зарегистрированный JEM profile: native threat, T1–T5 attributes, special damage multiplier, damage/healing/stagger adapters и arena/anti-cheese ownership.

Общий damage chain для зарегистрированного не-event босса:

`native channel × (1 + 0.15 × world tier) × optional non-Raid special multiplier × hosted participant damage factor`

Для attack, принадлежащей hosted boss, после этого применяется `×0.88`. Raid не получает profile `special_damage_multiplier`, но использует собственные raid/party factors.

`Catalog/views/power/calculations.json` содержит 56 полных таблиц JEM scaling (по одной на зарегистрированный профиль): Tier I–V × 1–6 игроков × обычный hosted/Raid. Это точный множитель над native health/attack, но не подмена ещё не извлечённой арифметики projectile/goal/effect.

Критический source finding: Frostbitten Golem имеет special multiplier `[2.7, 2.8, 3.2, 5.0, 5.0]`. Например, Tier V hosted на четырёх игроков вне Raid даёт owned attack `×9.625` от native; Raid в том же сценарии пропускает special и даёт `×4.8125`. Stage 3 это не меняет, только численно раскрывает существующую цепочку.
