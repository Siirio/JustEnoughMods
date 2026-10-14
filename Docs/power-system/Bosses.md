# Боссы — Phase 2

`Catalog/views/power/bosses.json` содержит 61 босса: 56 зарегистрированных JEM-профилей и 5 дополнительных native boss candidates. Учтены Legendary Monsters, Cataclysm, Bosses'Rise (`block_factorys_bosses`), Luminous Beasts, Luminous Nether, Aquamirae, Alex's Caves, Iron's Spells и vanilla bosses.

Для каждой из 61 записи найден exact-version owner class либо официальный mapped Minecraft/Forge class. Каталог создаёт 11 независимых channel slots: melee, AoE, projectile, beam, explosion, magic/custom damage, DoT, summons, grab/stun, healing/shield и phase/enrage. В сумме это 671 проверяемый слот. `not_observed` означает только отсутствие сигнала в выбранном owner-классе; делегированный goal/projectile/effect остаётся названной причиной unresolved.

Карточка также хранит полный зарегистрированный JEM profile: native threat, T1–T5 attributes, special damage multiplier, damage/healing/stagger adapters и arena/anti-cheese ownership.

Общий damage chain для зарегистрированного не-event босса:

`native channel × (1 + 0.15 × world tier) × optional non-Raid special multiplier × hosted participant damage factor`

Для attack, принадлежащей hosted boss, после этого применяется `×0.88`. Raid не получает profile `special_damage_multiplier`, но использует собственные raid/party factors.

Критический source finding: Frostbitten Golem имеет special multiplier `[2.7, 2.8, 3.2, 5.0, 5.0]`; вне Raid он действительно умножается вместе с boss tier и hosted participant factor. Phase 2 это не меняет, только делает цепочку явной.
