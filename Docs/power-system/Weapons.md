# Оружие — Phase 2

Основной список находится в `Catalog/views/power/weapons.json`. Phase 2 больше не считает оружием каждую строку или модель, в имени которой встретилось `bow`, `sword` или похожее слово.

В основной список попадают только канонические Minecraft-предметы, точные ID из boss-reward/JEM allowlist либо предметы, для которых найдено регистрационное доказательство в exact-version JAR. Отброшены bowls, spawn eggs, блоки профессий, компоненты апгрейда, `_visual_`, `_pulling_`, `_inventory`, модели руки и другие presentation-only варианты. Поэтому `minecraft:bowl` и визуальные стадии Simply Bows больше не отвечают на `query_atlas.py power`.

Для каждой оставшейся записи хранятся:

- доказательство регистрации и owning artifact;
- совпавший специализированный Item-класс, если он найден;
- сигналы attributes, active/use, projectile/range, AoE, effects, cooldown, resource cost и durability;
- отдельные причины, почему base damage, attack speed, special formula, penetration или Epic Fight moveset ещё не доказаны;
- acquisition routes и применимые JEM-модификаторы.

Сырые numeric constants класса не считаются уроном автоматически: одна константа может быть радиусом, длительностью, шансом или визуальным параметром. Число становится формулой только после привязки к конкретному методу.

Nightfall/Epic Fight capability, animation timing, stamina и combo остаются отдельными обязательными полями. Отсутствие этих данных помечено конкретно как data-driven/inherited moveset unresolved, а не как нулевой бонус.
