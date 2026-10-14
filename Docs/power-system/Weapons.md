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

Stage 3 исправляет потерю boss-reward оружия: все 36/36 ID из `major_rewards.json` и `expanded_rewards.json` теперь присутствуют, зарегистрированы и прошли item-owner inspection. Итоговый view содержит 175 weapon entries, потому что к очищенной Phase 2 выборке добавлены source-backed активные Epic Fight capability IDs, даже если имя предмета не проходит эвристику (`efn:ruinsgreatsword` и аналоги).

Nightfall/Epic Fight capability и связанный `skill_parameters` читаются прямо из установленных JAR. Armor negation, impact, max strikes, consumption, stacks и заданные cooldown считаются точными data-driven фактами. Если несколько установленных compat-JAR поставляют один resource ID, обе версии сохраняются с `resource_collision_precedence_unresolved`, а их числа не складываются. Animation hit windows и Java-эффекты также остаются отдельными unresolved полями.

Better Combat **не установлен** ни на каноническом клиенте, ни на сервере; Punchy задаёт `betterCombatCompat: false`. Встроенные в Cataclysm `weapon_attributes` сохранены только как `inactive_optional_compatibility_data` и не участвуют ни в combo, ни в damage calculations.
