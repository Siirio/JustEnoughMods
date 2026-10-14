# Броня и аксессуары — Phase 3

Броня находится в `Catalog/views/power/defenses.json`, аксессуары — в `Catalog/views/power/accessories.json`.

Модельный шум очищен тем же способом, что и оружие: основной каталог требует реальный зарегистрированный ItemStack или канонический Minecraft ID. Presentation-варианты `_charge_`, `_invisible_` и старый asset-only namespace `deep_dark_extended` не подменяют реальные предметы `deep_dark_regrowth`.

Для каждой записи отдельно учитываются armor, toughness, knockback resistance, damage-type reduction, passive, active, cooldown и durability. Для аксессуаров дополнительно учитываются Curios slot/capability, trigger и resource cost. Совпавшие Item/Armor-классы и наблюдаемые механические сигналы записаны, но сырые константы не назначаются полям без доказанного метода.

Текущий JEM Progression Armor подтверждён исходниками:

- шлем: projectile reduction; на Tier III также 20% шанс полностью отменить projectile hit и отразить 50% входящего урона с cap 4;
- нагрудник: reduction для hit от 8 damage; на Tier III отдельный guard на 35% с cooldown 240 ticks;
- поножи: после полученного урона дают speed, а на Tier III временно +15% total attack speed;
- ботинки: fall reduction; на Tier III после падения дают Speed II;
- несколько reduction складываются с diminishing overlap: сильнейшая полностью, каждая следующая с четвертью своей силы.

Guard сейчас расходуется первым подходящим `LivingHurtEvent`, потому что собственный guard-branch не требует heavy hit, projectile или минимальный damage. Это зафиксированный source finding, а не уже внесённая правка.

Текущий main сохраняет нативный износ, Unbreaking и Mending. Старой искусственной выдачи max durability больше нет; защита выпавшего предмета от огня/despawn/void не делает экипировку неразрушимой. Stage 3 отдельно фиксирует только доказанные вызовы `Item.Properties.durability(...)` и `hurtAndBreak(...)`; отсутствие такого вызова в matched owner class не считается доказательством бесконечной прочности, пока не прослежена registration factory.

Активные Nightfall armor capabilities добавляются source-backed выборкой, а не по имени предмета. Поэтому текущий defense view содержит 219 записей и включает точные `stun_armor`/`weight` профили Duskfire и Ruinfighter из установленного JAR.
