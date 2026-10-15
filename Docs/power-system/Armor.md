# Броня, аксессуары и прочность — Stage 3.5C

Машинные источники: `Catalog/views/power/defenses.json`, `accessories.json`, `durability.json` и `calculations.json`. Проход читает точные установленные JAR, но не меняет Java, конфиги, datapack или игровой баланс.

## Закрыто численно

| Область | Результат |
|---|---:|
| Defense records | 219/219 карточек |
| Точные material attributes | 125/219 |
| Полные четырёхчастные наборы с точными суммами | 24/39 |
| Сравнения входящего урона | 1 080 |
| Accessories | 52/52 карточек |
| Точные Curios slots | 22/52 |
| Точные attribute modifiers | 10/52 |
| Accessory effects с доказанным механическим evidence | 15/52 |
| Durability audit | 448/448 карточек |
| Exact max damage | 155/448 |
| Доказанный consumption path | 138/448 |
| Полностью прослеженные max + consumption | 120/448 |
| Explicitly unbreakable armor | 8 |

Точные armor-профили получены через цепочку `registry supplier -> material field -> ArmorMaterial constructor/getter`. В них входят armor, toughness, knockback resistance и nominal max damage. Vanilla-константы используются как такая же точная исходная модель, а не как догадка.

## Сравнимые комплекты

Полностью закрыты 24 набора: два Alex's Caves, Cursium и Ignitium Cataclysm, Cardboard Create, двенадцать наборов Iron's Spells, Annihilator Legendary Monsters и шесть vanilla-наборов. Для каждого построены пять World Tier-вариантов JEM Progression и девять одинаковых входящих сценариев: обычный физический, heavy, projectile, explosion, Iron's magic, armor-bypass, armor+enchantment-bypass, Frostbitten Golem T1 solo hosted и T5 four-player hosted.

Группировка четырёх ID по stem не доказывает set bonus. Единственный статически подтверждённый именованный full-set gate среди 39 групп — `legendary_monsters:annihilator`, метод `isFullSetActive`. Остальные пассивы остаются отдельными blockers, пока owner-код не докажет условие и формулу.

JEM Progression применяется условно только к четырём реально зачарованным частям: Tier II даёт +5% native armor/toughness, Tier III–V +10%; boots получают соответственно +0.15/+0.25 knockback resistance. Projectile/heavy/guard/fall ветки рассчитываются отдельно и не выдаются за постоянный armor stat.

## Аксессуары

Curios evidence строится из точных item tags, factory `AttributeContainer` и owner methods. `valid_slots` показывает разрешённый слот и stack limit; `attributes` сохраняет UUID, operation и amount. Отсутствующий slot или attribute не превращается в «ничего не делает»: это именованный blocker для dynamic capability, event handler, config или runtime NBT.

## Прочность

Для обычной брони подтверждена native формула износа: каждая часть теряет `max(1, floor(incoming/4))` на damage sources, которые повреждают armor. Unbreaking уменьшает ожидаемый расход брони до вероятности `60% + 40%/(level+1)`: уровни 0–3 дают `1.0/0.8/0.733333/0.7`. Mending чинит 2 durability за XP point у выбранного подходящего damaged stack; выбор предмета и доступный XP остаются runtime-dependent.

Восемь Cataclysm armor items точно переопределяют `isDamageable(ItemStack)` константой `false`: Cursium boots/helmet/leggings и Ignitium boots/chestplate/elytra_chestplate/helmet/leggings. Cursium chestplate использует другой класс и не входит в этот список. Это доказанный review-only balance outlier; Stage 3.5C его не исправляет.

Отсутствие найденного `hurtAndBreak` никогда не считается доказательством бесконечной прочности. `nominal_max_damage`, `observed_consumption` и `explicit_unbreakable` хранятся раздельно; Epic Fight capability durability остаётся отдельным слоем, пока не доказана его связь с vanilla stack wear.

## Непокрытая граница

Не закрыты 94 material/dynamic defense профиля, 15 полных set totals, 30 Curios slot contracts и 310 consumption profiles. Shields, Resistance, absorption, school-specific resistance, iframe, proc uptime, cooldown и runtime NBT не подставляются без конкретного источника или сценария. Поэтому 1 080 результатов — сопоставимые reference outcomes, а не окончательный рейтинг экипировки.
