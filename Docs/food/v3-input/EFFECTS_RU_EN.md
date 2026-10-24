# JEMFood — 30 способностей / 30 Blessing Traits

## Общий статус / Shared status

**Благословение еды / Food Blessing** (`jemfood:food_blessing`) — единственный видимый эффект для всех 30 способностей, одна рисованная 18×18 PNG-иконка еды. В мире существует **один активный набор**, а не множество неуправляемых зелий. Под курсором: имя статуса, продукт, категория, таймер, затем для каждой способности собственные название, описание и текущее состояние. Наименования способностей на обоих языках содержат не более **двух слов**, а пояснения могут быть развёрнутыми.

**I/II/III** ниже — сила способности у Вкусной / Изысканной / Шедевральной. Это **не усилители vanilla-потий**, а преднастроенные параметры JEMFood, кроме трёх специально помеченных мостов к чужим эффектам.

## Дорога и исследование / Travel & Exploration
| ID | Русский | English | Механика (I / II / III) | Description (I / II / III) |
|---|---|---|---|---|
| `fleet` | **Прыть** | **Fleetfoot** | Ускорение I до 20/30/40 с; исходный эффект выбирается только если доступен. | Speed I for 20/30/40s; use preferred effect only when available. |
| `surefoot` | **Легкоступ** | **Surefoot** | Урон от падения −10/16/22%; не предотвращает смертельное падение полностью. | Fall damage −10/16/22%; cannot guarantee survival from fatal falls. |
| `swimmer` | **Пловец** | **Swimmer** | Морская подвижность через родной эффект или Грацию дельфина I на 20/30/40 с. | Water mobility via native effect or Dolphin’s Grace I for 20/30/40s. |
| `deepbreath` | **Глубокодых** | **Deepbreath** | Каждые 20/14/10 тиков возвращает 1 ед. воздуха под водой; утопление всё ещё возможно. | Restore 1 air every 20/14/10 ticks underwater; drowning remains possible. |
| `highstep` | **Высокошаг** | **Highstep** | Высота автоматического шага +0,15/0,25/0,35 блока, не обходит столкновения. | Step height +0.15/0.25/0.35 blocks without bypassing collision. |
| `trailrunner` | **Путник** | **Trailrunner** | Истощение от бега −6/10/14%; не влияет на другие действия. | Sprint exhaustion −6/10/14% only. |

## Работа и добыча / Work & Gathering
| ID | Русский | English | Механика (I / II / III) | Description (I / II / III) |
|---|---|---|---|---|
| `miner` | **Рудокоп** | **Miner** | Скорость добычи подходящей киркой +6/10/14%. | Mining speed with valid pickaxe targets +6/10/14%. |
| `digger` | **Землекоп** | **Digger** | Скорость добычи подходящей лопатой +6/10/14%. | Mining speed with valid shovel targets +6/10/14%. |
| `woodcutter` | **Лесоруб** | **Woodcutter** | Скорость рубки брёвен подходящим топором +6/10/14%. | Mining speed on tagged logs with a valid axe +6/10/14%. |
| `harvester` | **Жнец** | **Harvester** | За каждые 12/9/6 зрелых культур: +1 XP, не чаще раза в 10 с; лишь проверенные культуры. | +1 XP per 12/9/6 mature crops, at most once per 10s; vetted crops only. |
| `angler` | **Рыболов** | **Angler** | +1 XP за каждые 3/2/1 успешные поимки; не чаще раза в 30 с. | +1 XP per 3/2/1 successful catches, no more than once per 30s. |
| `sage` | **Знаток** | **Sage** | Дополнительный опыт с обычных мобов +3/5/7%, максимум +2 XP с одного; не действует на боссов. | +3/5/7% normal mob XP, capped at +2 per mob; excludes bosses. |

## Защита / Defense
| ID | Русский | English | Механика (I / II / III) | Description (I / II / III) |
|---|---|---|---|---|
| `stonehide` | **Камнекожа** | **Stonehide** | Дубовая кожа или Сопротивление I на 8/12/16 с, 120 с общий откат. | Oakskin or Resistance I for 8/12/16s; 120s shared cooldown. |
| `bulwark` | **Заслон** | **Bulwark** | Получаемый прямой урон от снарядов −4/7/10%; взрывы исключены. | Direct projectile damage −4/7/10%; excludes explosions. |
| `cinderward` | **Жаростой** | **Cinderward** | Получаемый урон от огня и лавы −10/15/20%; нет иммунитета. | Fire and lava damage −10/15/20%; no immunity. |
| `frostward` | **Хладостой** | **Frostward** | Урон от замерзания −12/20/28%; не предотвращает обледенение. | Freeze damage −12/20/28%; does not prevent freezing. |
| `steadfast` | **Несдвиг** | **Steadfast** | Сопротивление отбрасыванию +0,04/0,07/0,10 в рамках стандартного атрибута. | Knockback resistance attribute +0.04/0.07/0.10. |
| `heartroot` | **Живучесть** | **Heartroot** | Макс. здоровье +1/+2/+2 HP без дополнительного исцеления. | Maximum health +1/+2/+2 HP with no free healing. |

## Бой / Combat
| ID | Русский | English | Механика (I / II / III) | Description (I / II / III) |
|---|---|---|---|---|
| `keenedge` | **Остриё** | **Keenedge** | Урон обычной ближней атаки +2,5/4/5,5%; общий лимит пищевого урона +7%. | Standard melee damage +2.5/4/5.5%; total food damage bonus ≤7%. |
| `fervor` | **Рвение** | **Fervor** | Скорость атаки +2,5/4/5,5% от базового атрибута. | Attack speed +2.5/4/5.5% of base attribute. |
| `marksman` | **Меткость** | **Marksman** | Прямой урон не-взрывных снарядов +3/5/7%; не создаёт вторую атаку. | Direct nonexplosive projectile damage +3/5/7%, never a second attack. |
| `beastbane` | **Зверобой** | **Beastbane** | Урон по обычным враждебным мобам +3/5/7%; не действует на игроков. | Damage vs regular hostile mobs +3/5/7%; no PvP. |
| `onrush` | **Натиск** | **Onrush** | Ближний урон при беге +2,5/4/5,5%; только принятые удары. | Melee damage while sprinting +2.5/4/5.5%; accepted hits only. |
| `resolve` | **Упорство** | **Resolve** | Ближний урон при здоровье ниже 50% +2,5/4/5,5%; нет иммунитета к смерти. | Melee damage below 50% health +2.5/4/5.5%; no cheat-death. |

## Питание и восстановление / Food & Recovery
| ID | Русский | English | Механика (I / II / III) | Description (I / II / III) |
|---|---|---|---|---|
| `satiety` | **Сытность** | **Satiety** | Общее истощение сытости −3/5/7%; снижение от бега с Путником не перемножается. | General hunger exhaustion −3/5/7%; does not multiply with Trailrunner. |
| `quickbite` | **Скороед** | **Quickbite** | Следующая еда съедается на 1/2/3 тика быстрее; зелья и молоко без изменений. | Future food use finishes 1/2/3 ticks faster; excludes potions and milk. |
| `hearth` | **Очаг** | **Hearth** | Вне боя: +1 HP раз в 100/80/60 с, если сытость ≥16 и не было урона 15 с. | Out of combat heal +1 HP every 100/80/60s when food ≥16 and undamaged for 15s. |
| `reserve` | **Запас** | **Reserve** | При следующем успешном приёме другой еды: +0,5/0,75/1 насыщения, откат 60 с. | On successful consumption of a different food: +0.5/0.75/1 saturation, 60s cooldown. |
| `purity` | **Чистота** | **Purity** | Урон от яда и иссушения −8/12/16%; эффекты и их время сохраняются. | Poison and Wither damage −8/12/16%; does not remove status effects. |
| `secondwind` | **Второй ветер** | **Second Wind** | После съедения другой еды: +4/6/8% скорости на 5 с, откат 60 с; без стакания. | After eating another food: +4/6/8% speed for 5s, 60s cooldown, no stacking. |


## Опциональные внешние эффекты и обязательные fallback

| Способность | При наличии проверенного мода | При отсутствии / несовпадении ID | Длительность на выдачу |
|---|---|---|---|
| Прыть / Fleetfoot | `irons_spellbooks:hastened` | `minecraft:speed` I | 20/30/40 с |
| Пловец / Swimmer | `alexsmobs:orcas_might` | `minecraft:dolphins_grace` I | 20/30/40 с |
| Камнекожа / Stonehide | `irons_spellbooks:oakskin` | `minecraft:resistance` I | 8/12/16 с |

Fallback выбирается автоматически. Он **никогда не понижает категорию**, не меняет список способностей и не требует наличия стороннего мода. Три эффекта-моста выдаются скрыто от стандартного списка иконок (`showIcon=false`, `showParticles=false`) и принадлежат только JEMFood. Если совпадает чужой эффект того же ID, JEMFood его не заменяет и не очищает. Перед включением моста Codex обязан проверить реальную механику native эффекта; непроверенный/несовместимый путь идёт в заранее определённый vanilla fallback.

**Важное ограничение дизайна:** Сопротивление I, Грация дельфина и Поспех могут быть существенно сильнее маленьких процентных баффов. Поэтому мостам назначены короткие длительности и независимые cooldown. Они повторно срабатывают только если активен тот же Food Blessing, наступил cooldown и нет независимого чужого экземпляра. Это не бесконечный источник потион-эффектов.

## Пределы защиты и урона

- Суммарная прибавка наносимого JEMFood урона для одной атаки не более **+7%**, независимо от сочетания талантов, и не используется для обхода иммунитетов боссов или иных запретов.
- Суммарное снижение физического и снарядного урона от JEMFood не более **10%**, а специализированного урона огня, холода, яда и иссушения — **30%** на hit. *Ванильное* Resistance — отдельный статус, поэтому для него действуют короткий таймер и cooldown.
- Food damage boosts не действуют против игроков; никакого PvP-пэй-ту-вина. Атрибуты скорости атаки и движения не умножаются безгранично.
- Все % считаются только для подходящего типа события, сервером, один раз на итоговое событие; запрет рекурсивных атак и дублирования наград.

## Английские названия

Fleetfoot, Surefoot, Swimmer, Deepbreath, Highstep, Trailrunner, Miner, Digger, Woodcutter, Harvester, Angler, Sage, Stonehide, Bulwark, Cinderward, Frostward, Steadfast, Heartroot, Keenedge, Fervor, Marksman, Beastbane, Onrush, Resolve, Satiety, Quickbite, Hearth, Reserve, Purity, Second Wind.
