# Броня и аксессуары — Phase 3

Броня находится в `Catalog/views/power/defenses.json`, аксессуары — в `Catalog/views/power/accessories.json`.

Модельный шум очищен тем же способом, что и оружие: основной каталог требует реальный зарегистрированный ItemStack или канонический Minecraft ID. Presentation-варианты `_charge_`, `_invisible_` и старый asset-only namespace `deep_dark_extended` не подменяют реальные предметы `deep_dark_regrowth`.

Для каждой записи отдельно учитываются armor, toughness, knockback resistance, damage-type reduction, passive, active, cooldown и durability. Для аксессуаров дополнительно учитываются Curios slot/capability, trigger и resource cost. Совпавшие Item/Armor-классы и наблюдаемые механические сигналы записаны, но сырые константы не назначаются полям без доказанного метода.

## Что выдаёт числовой слой

| Представление | Смысл |
|---|---|
| `defenses.json` | 219 отдельных defense-кандидатов с registration/owner/capability evidence |
| `calculations.json` / `defense_numeric_card` | Exact attributes, durability и passive/active method records для каждой записи |
| `calculations.json` / `armor_set_card` | Только настоящие четырёхчастные registry stems; значения складываются лишь из доказанных атрибутов |
| `durability.json` | Отдельная проверка max damage и consumption path |
| `blockers.json` | Неполные armor/toughness, passives, Curios slots и прочность с конкретным owner blocker |

Группировка полного сета не создаёт set bonus из названия. Например, наличие четырёх ID `*_helmet/chestplate/leggings/boots` разрешает сложить подтверждённые armor/toughness additions, но иммунитет, proc или cooldown появляется только из capability/метода владельца. Смешанные комплекты пока не объявляются оптимальными без закрытых passive stacking и legal-slot constraints.

Текущий проход создал 219 defense cards и 39 four-piece set cards. Общий durability audit охватывает 448 weapons/defenses/accessories: exact max damage найден для 25, consumption path для 21, обе стороны цепочки для 8. Остальные записи — не «неразрушимые», а unresolved до inherited Tier/Item, NBT/tag или active ability path.

Текущий JEM Progression Armor подтверждён исходниками:

- шлем: projectile reduction; на Tier III также 20% шанс полностью отменить projectile hit и отразить 50% входящего урона с cap 4;
- нагрудник: reduction для hit от 8 damage; на Tier III отдельный guard на 35% с cooldown 240 ticks;
- поножи: после полученного урона дают speed, а на Tier III временно +15% total attack speed;
- ботинки: fall reduction; на Tier III после падения дают Speed II;
- несколько reduction складываются с diminishing overlap: сильнейшая полностью, каждая следующая с четвертью своей силы.

Guard сейчас расходуется первым подходящим `LivingHurtEvent`, потому что собственный guard-branch не требует heavy hit, projectile или минимальный damage. Это зафиксированный source finding, а не уже внесённая правка.

Текущий main сохраняет нативный износ, Unbreaking и Mending. Старой искусственной выдачи max durability больше нет; защита выпавшего предмета от огня/despawn/void не делает экипировку неразрушимой. Stage 3 отдельно фиксирует доказанные значения factory/tier max damage и вызовы `hurtAndBreak(...)`; отсутствие вызова в matched owner class не считается доказательством бесконечной прочности, потому что значение или расход могут принадлежать Tier, базовому Item-классу, NBT либо Epic Fight skill path.

Активные Nightfall armor capabilities добавляются source-backed выборкой, а не по имени предмета. Поэтому текущий defense view содержит 219 записей и включает точные `stun_armor`/`weight` профили Duskfire и Ruinfighter из установленного JAR.
