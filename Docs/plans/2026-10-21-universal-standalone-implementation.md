# Universal standalone implementation

Status: the original universal stage below records completed source/artifact work, not player acceptance. The 2026-10-22 mandatory Twelve Eyes stage and the SMP/rewards/Market/race amendments are planned only. The newest amendments override conflicting earlier requirements, including integrated SMP, solo JEM Raid and preparation-time Raid scaling. They do not claim a runtime change or authorize implementation in this documentation task.

## Актуальное дополнение: SMP, награды, Market и первый выбор расы — 2026-10-22

**Статус:** только архитектурный план; код не реализовывать. Forge 1.20.1 / Java 17. Все требования поведения ниже заданы пользователем и заменяют противоречащие пункты прежнего плана. Отмеченные далее выполненные задачи относятся к предыдущему этапу и не означают выполнение этого дополнения. Обязательный Twelve Eyes baseline остаётся отдельным запланированным этапом.

### 1. Доступность и серверная граница

| Возможность | Одиночный integrated server, включая открытый LAN | Dedicated server с одним игроком или группой |
| --- | --- | --- |
| `/smp`, все способы открытия SMP, создание/приглашение/подтверждение пати и остальные её обработчики | Полностью отключены | Доступны при установленном и включённом владельце функции |
| Все события JEM, включая Raid: расписание, ручной запуск, телепортация, создание участников и восстановление | Полностью отключены | Доступны по серверным правилам и действующему разрешению хоста на события |
| Обычные боссы | Доступны без пати, подтверждения состава и группового усиления | Сохраняют серверный encounter lifecycle |
| Личная панель предметных наград | Доступна без SMP и Economy | Доступна без SMP и Economy |
| Market | Доступен при включённой Economy, в том числе без SMP | Доступен при включённой Economy, в том числе без SMP |
| Settlements | Самостоятельный доменный интерфейс и механики без SMP | Самостоятельный доменный интерфейс и механики; SMP может дать переход |
| Обычный Minecraft XP из всех источников | Общий базовый множитель ×2 | Тот же базовый ×2, дополнительные бонусы сверху |

Уточнение пользователя: `/smp` — только серверное окно маршрутизации к самостоятельным доменным интерфейсам, в частности Settlements и Market. Оно не владеет их экранами, состоянием или операциями и не требуется для их открытия. В одиночке `/smp` отключён, а оба домена работают через собственные способы открытия. Сохранённые серверные пати/события остаются у своих владельцев; окно маршрутизации не дублирует их реализации.

В части дополнительного усиления обычных боссов одиночка использует только World Tier: множитель количества участников равен 1, пати отсутствует. На dedicated World Tier сохраняется, а групповой множитель накладывается сверху по числу подтверждённых участников конкретного боя, не по общему online сервера. Независимые нативные настройки сложности остаются согласно исходному требованию. Рейдовое усиление добавляется только в соответствующем серверном событии.

В одиночке и LAN нет вообще событий JEM, а не только рейдов: отключены также Blood Moon, Resource Rush, сезонные и остальные JEM event flows, включая ручные команды, пакеты, таймеры и восстановление сохранённых событий. Обычные бои с боссами не считаются такими событиями. Этот запрет не распространяется на самостоятельные ванильные и сторонние механики.

Серверный режим определяется типом dedicated server, никогда количеством игроков. LAN не включает SMP автоматически; это принятое пользователем рабочее допущение. Один серверный предикат доступности проверяется на регистрации/исполнении команд, открытии меню, обработке каждого сетевого запроса, прямых domain-вызовах, планировании, старте и восстановлении события. Скрытые кнопки не являются защитой. Клиент получает только проекцию доступности; поддельный или устаревший запрос не создаёт состояние и не телепортирует игрока.

В integrated-мире обычный босс не должен ждать отключённого SMP Start и оставаться с JEM `noAI`, неуязвимостью или подавленной презентацией. Владелец обычного encounter обеспечивает доступный одиночный путь боя и при загрузке снимает только принадлежащую отключённому JEM flow блокировку; чужие нативные фазы/причины неуязвимости не сбрасываются. Сохраняются независимая сложность, World Tier, Twelve Eyes prerequisites, native AI и существующие правила обычных боссов. Старое безусловное требование dormant hold до hosted Start в одиночке уступает этому контракту. На dedicated сохраняется canonical Start и защита подготовки.

### 2. Минимальная пати и два независимых усиления

Пати отвечает только за сбор участников, подтверждение участия, групповое усиление босса, участие/телепортацию на серверное событие и определение получателей личных наград. Удаляются страницы профилей игроков и связанная социальная функциональность. UUID остаётся техническим идентификатором; удаление интерфейса/социальных данных не удаляет расу, прогресс, права участия, историю допуска или сохранённые награды.

Окончательное подтверждение боя и его запуск образуют одну серверную границу фиксации состава: immutable snapshot UUID участников и параметров группового усиления принадлежит конкретной попытке боя. Поздние приглашения и изменения пати не меняют этот snapshot. Правила действительного участия/выбывания продолжают определять право на награду.

Рейдовое усиление применяется при старте JEM-рейда; групповое — при подтверждении и запуске конкретного боя. У этих слоёв отдельные сохранённые идентификаторы применения, параметры и владельцы. Итог вычисляется из исходных характеристик и сохранённых слоёв, а не повторным умножением текущего значения. Переподключение, загрузка сущности и restart восстанавливают тот же результат. Retry различает новую попытку и восстановление прежней; старые модификаторы не складываются с новыми. Уже подготовленные старым кодом Raid-модификаторы требуют явного переноса/снятия по их происхождению, чтобы новая стадия старта не применила их второй раз.

### 3. Общая очередь предметных наград

Все предметные награды JEM получают единый серверный путь начисления и получения, независимый от SMP, пати и Economy. Переносятся дроп боссов под управлением JEM, предметные награды событий и всё предметное содержимое старого раздела "Награды"; сам раздел удаляется. Перед реализацией составить полный перечень остальных JEM-источников предметных наград и включить их в тот же путь. Обычный мировой и сторонний лут целиком не перехватывается. Опыт, разблокировки и остальные непредметные результаты остаются у своих владельцев. Покупки Market и перемещение принадлежащих игроку предметов сами по себе не становятся наградами.

Каждый подходящий участник получает свою полную копию награды по действующим правилам; предметы не делятся между участниками и не выбрасываются на землю. Владелец источника определяет состав награды и допустимых получателей, общий владелец хранит и доставляет её. Результат случайного выбора сохраняется при начислении и не перебрасывается при повторном запросе.

Предложенное Codex архитектурное решение: отдельный небольшой домен `JEMRewards` с серверным хранилищем по UUID, API начисления, протоколом получения и клиентской панелью. Это будущий модуль, а не уже существующий API. Он не зависит от SMP/Server, Economy, профилей или пати; WorldBossTiers и Events становятся его потребителями. Не возвращать ради этого универсальные PackCore/MenuSupport. Конкретные Java-сигнатуры, SavedData schema и transport определяются при реализации по существующим нативным контрактам.

Контракт хранилища и миграции:

- Один стабильный ключ выдачи связывает источник, instance/attempt, тип награды и UUID получателя. Повторная обработка события или пакета возвращает прежний результат. Для каждого перенесённого источника старый прямой inventory/drop/legacy путь выключается в той же миграции.
- Сервер сохраняет точные ItemStack, включая количество и данные предмета, остаток, версию записи и квитанции выдачи/получения. Клиент не задаёт UUID владельца, состав стека, размер награды или право на неё.
- Получение сериализуется сервером; проверяются текущая сессия меню, revision/идентификатор записи и допустимый объём. Повторные и устаревшие запросы не дают повторной выдачи. Полный инвентарь оставляет награду без изменений; частичное получение уменьшает её только на реально переданное количество.
- Закрытие GUI, смерть, выход и штатный restart сохраняют остаток. До удаления старого владельца описать durable порядок записи передачи, квитанций и восстановления прерванной операции; один `setDirty` или флаг в памяти не доказывает отсутствие потери/дублирования между разными сохранениями.
- Старые pending rewards, `Profiles.deliveries`, незавершённые `rewardMigration` и delivery/claim receipts импортируются с устойчивыми migration IDs. Старую запись можно погасить только после подтверждённого импорта; повторный запуск миграции не начисляет её снова. Уже полученные награды не импортируются заново. Сохраняются неизвестные/временно недоступные предметные записи до разрешения их содержимого.
- Миграция охватывает offline UUID и одиночные миры, а не только вход игроков на dedicated. Чтение legacy-формата не должно требовать установленного SMP-модуля. Удаление профилей допускается лишь после переноса нужных данных к их существующим владельцам и сверки неполученных остатков.

### 4. Панель слева от инвентаря

Компактная панель размещается слева от основного интерфейса инвентаря и доступна без `/smp` в обоих режимах. При наличии неполученных предметов она появляется, после получения последнего исчезает. Для большого количества используются страницы или прокрутка; конкретный вариант остаётся реализационным выбором. Содержимое можно только забирать.

Уточнение пользователя: это обычный интерфейс инвентаря, по умолчанию открываемый E (с учётом переназначения клавиши). Вкладка/раздел "Награды" исчезает из SMP и на dedicated; отдельного серверного исключения или второго экрана получения не остаётся. Все перенесённые предметные награды забираются из этих слотов инвентаря.

Сервер запрещает помещение предметов обычным кликом, Shift-click, drag и hotbar swap. Обратный quick-move, обмен с заполненным курсором и остальные виды container-click не могут использовать панель как хранилище. Клиентские ограничения дополняют, но не заменяют серверную проверку. Получение и закрытие с предметом на курсоре должны сохранять обычную согласованную семантику inventory/menu, без выпадения неполученного остатка.

Слоты, кнопки, подсказки и шрифт используют стандартные ресурсы Minecraft и их исходные resource locations. Не рисовать ванильные слоты жёстко заданными цветами и не переносить копии текстур в собственный namespace, обходящий существующие ресурспаки. Этот пункт задаёт контракт будущего GUI; новые изображения и макеты в данной задаче не создаются.

Размещение рассчитывается относительно фактических границ inventory при текущем GUI Scale. Учитываются открытая/закрытая книга рецептов, JEI и установленные расширения инвентаря; область панели участвует в соответствующем согласовании свободного пространства. На узком экране панель остаётся достижимой слева, не перекрывает действующие слоты и не перехватывает чужие клики. Точные seams и resource locations подтвердить по установленной версии перед реализацией, без выдуманных API.

### 5. Market принадлежит Economy

Экран рынка, торговые операции, сетевой протокол и самостоятельное открытие принадлежат Economy. Рынок работает в integrated и dedicated при включённой Economy, включая сборку без SMP-модуля; `/smp`, пати и профиль не являются зависимостями открытия или торговли. При отключённой Economy закрываются открытие и операции рынка по ранее принятому полному отключению экономики, без удаления сохранённых средств/записей. Клиентский экран не может обходить серверную проверку Economy enabled. Конкретная команда/кнопка самостоятельного открытия выбирается при реализации внутри Economy.

### 6. Модель при обязательном первом выборе расы

Зафиксированный пользователем симптом: первый обязательный экран открывается без модели, позднее F6 показывает модель. Корневая причина пока не установлена; наличие lifecycle-проверок в исходниках не является воспроизведением или доказательством исправности.

До изменения кода сравнить обе реальные цепочки открытия: готовность player и level, идентичность подключения, получение race snapshot, подготовку внешности/скина и preview entity, создание/init/resize/render экрана. По `evidence-first-debugging` установить первый неверный переход и его владельца, затем подтвердить его разрешённым воспроизведением нового мира и первого подключения. Произвольная задержка не является исправлением.

Обязательный вход и F6 используют один корректный путь подготовки и отображения модели в JEMRoles. Пока пользовательский скин загружается, используется стандартная внешность Minecraft; готовый скин обновляет ту же модель. Это разрешённое ожидание внешности, а не маскировка отсутствующего player/world или сломанной инициализации. Первый обязательный выбор остаётся обязательным и работает без закрытия экрана/F6. Изоляция client-only rendering от серверной загрузки классов сохраняется.

### 7. JEM Raid — только серверное событие

Жизненным циклом JEM Raid как серверного события владеет JEMServer. Он сохраняет запуск, участие, телепортацию и рейдовое усиление. Общий владелец босса/арены остаётся WorldBossTiers; не создавать вторую структуру, allowlist, формулу усиления или encounter authority. JEMServer вызывает этот домен через ограниченный event adapter. Моменты усилений заданы в разделе 2 и заменяют старую preparation-time схему.

В integrated/LAN запрещены также восстановление сохранённого активного JEM Raid, отложенные задачи и создание event participants при загрузке сущности/мира. Сохранённый Raid переходит в неактивное состояние без запуска боя, новых наград или повторной выдачи. Снять принадлежащие ему combat holds/scaling безопасным общим recovery-путём, сохранить историю и уже заработанные награды; не оставлять обычного босса заблокированным. Конкретный способ нормализации старой event-сущности подтвердить по её происхождению и существующему lifecycle, без массового сброса NBT.

Ванильные рейды и независимые механики сторонних модов не отключаются. Отключение SMP не удаляет самостоятельные доменные функции, World Tier, награды или Market.

### 8. Дополнение пользователя: общий базовый Minecraft XP ×2

В одном production-паке обычный Minecraft XP из любых источников имеет базовый множитель ×2 и в integrated/LAN, и на dedicated. Пользователь отдельно уточнил: опыт рас и профессий не удваивается. Это изменение начисления обычного XP, а не уровней, порогов прокачки, очков навыков или отдельного прогресса доменов.

Серверные настройки сохраняют тот же базовый ×2; наличие одинакового пака на клиенте и сервере не превращает его в ×4. Дополнительные действующие XP-множители применяются поверх этого базового слоя ровно один раз: `итоговый XP = исходный XP × 2 × дополнительные множители`. Например, 100 XP без бонуса дают 200, а с бонусом ×1,5 — 300. Один logical-server owner отвечает за базовый слой в обоих режимах; клиент только отображает результат. Начисление не зависит от SMP, пати, Economy или наличия активного события.

При реализации проследить все пути обычного XP: мобы/боссы, добыча, печи, рыбалка, торговля, разведение, бутылочки опыта, advancement/quest/event rewards и прямые начисления модов. Обработчик только orb pickup не считается покрытием всех источников. Источник, появление сферы, её сбор и прямое зачисление не должны последовательно умножать одно начисление. Существующий серверный ×2 перенести/объединить с общим владельцем, а не добавить поверх него второй ×2. Обычное распределение XP, включая Mending, должно проходить через согласованный нативный путь без повторного начисления.

Предложенная техническая граница: отличать вновь начисленный XP от восстановления уже сохранённого баланса, сохранённого при смерти XP и переноса через хранилище. Загрузка мира, смерть/recovery и переподключение не умножают уже заработанное повторно. Точный owner, перехваты и сохранение происхождения определить по исходникам и установленным интеграциям перед реализацией; название `RaceExperience` само по себе не означает обычный Minecraft XP. В этой задаче меняется только план.

### 9. Последовательность будущей реализации и владельцы

Это план работ, не разрешение выполнять их сейчас. Перед будущими gameplay-изменениями обязательны Atlas lookup и before-change impact scan; после них — matching history/effective update и разрешённая Atlas validation. Никаких тестовых fixtures или автоматических runtime-сценариев не создавать.

| Этап | Известные точки исходников и результат |
| --- | --- |
| 1. Граница режима и обычные боссы | `Mods/JEMServer/.../smp/SmpEnvironment.java`, `SmpRuntime`, `SmpRequests`, `SmpActions`, party/event handlers; `Mods/JEMWorldBossTiers/.../encounter/HostedEncounters.java`. Сначала отделить SMP-only вызовы от общих доменов; закрыть все входы в integrated, одновременно обеспечить обычный solo encounter без ожидания SMP. |
| 2. Награды и перенос данных | `Mods/JEMWorldBossTiers/.../encounter/HostedRewards.java`, `PendingRewardContainer.java`; `Mods/JEMServer/.../smp/LegacyRewards.java`, `Profiles.java`, `SmpData.java` и все producers. Ввести независимого владельца/панель, миграцию и единственный путь выдачи до удаления старого раздела/профилей. |
| 3. Пати и Raid | `Parties.java`, `HostedParties.java`, `EventParties.java`, event lifecycle в JEMServer; `HostedRaidScaling.java` в WorldBossTiers. Зафиксировать состав, разнести два момента усиления и recovery. Удалить социальные projection/handlers после отделения нужных domain-данных. |
| 4. Market | Существующие `Mods/JEMEconomy/.../client/MarketScreen.java`, `network/Market*Packet.java`, `full/market/MarketRuntime.java`. Удалить зависимость самостоятельного открытия от SMP; сохранить торговую authority Economy и её off gate. |
| 5. Первый выбор расы | `Mods/JEMRoles/.../client/RaceSelectionScreen.java`, `.../jemvisualsandbox/client/races/RaceExperienceClient.java`, `RaceExperienceScreen.java`. Сначала причинная диагностика двух входов, затем общий lifecycle модели; без произвольной задержки. |
| 6. Общий Minecraft XP ×2 | Найти владельца текущего серверного ×2 и всех обычных начислений; включить `Mods/JEMWorldBossTiers/.../api/ExperienceRewardApi.java` и источники сфер/прямых зачислений в разбор потребителей. Сделать один общий базовый слой на logical server для обоих режимов, оставить отдельный XP рас/профессий неизменным и проверить отсутствие повторного умножения. |
| 7. Документация и поставка | Обновить owning docs, dependency inventory, Atlas и CHANGELOG по реально выполненному. Производственные сборки/локальная синхронизация и native publication — только в задаче реализации по действующим правилам. |

Пути с `...` обозначают уже найденные пакеты внутри соответствующего `src/main/java/com/siirio`; это карта владельцев, не полный перечень будущего diff. Перед правкой раскрыть всех потребителей и проверить сохранённые схемы. Архитектурный выбор отдельного JEMRewards и стабильных ключей/квитанций предложен Codex для выполнения требований; пользователь не задавал имён классов или новой библиотеки.

### 10. Приёмка будущей реализации

Все пункты ниже не выполнены в этой задаче. Это критерии ручной приёмки после реализации и отдельного разрешения на игровые запуски, а не создаваемые автоматические тесты.

- [ ] Новый одиночный мир и тот же мир, открытый по LAN: нет SMP UI/команды, пати и JEM Raid; прямые/устаревшие сетевые обращения и отложенные callbacks также ничего не включают. Обычные боссы доступны без party-confirmation/группового множителя, включая повторную загрузку; World Tier/сложность работают.
- [ ] Dedicated с одним игроком и с группой: SMP, участие/телепортация и разрешённые серверные события доступны независимо от online count. Состав фиксируется при подтверждении/старте, рейдовое и групповое усиление применяются в свои моменты ровно один раз; reconnect, entity reload, restart и retry не наращивают характеристики.
- [ ] Перенос мира с активным JEM Raid в integrated: событие не возобновляется и не создаёт участников, обычные боссы не остаются замороженными; заработанные награды сохранены. Ванильные/сторонние рейды продолжают работать.
- [ ] Каждый допустимый участник получает полную личную награду; чужой UUID, повторный producer callback/claim, позднее изменение пати и устаревшая страница не дают лишних предметов. Обычный лут мира не перехвачен, XP/unlocks выдаются обычным способом.
- [ ] Полный инвентарь, частичное получение, все попытки помещения (обычный клик, Shift-click, drag, hotbar swap), закрытие с курсором, смерть, выход и restart: нет потерь/дубликатов; после последнего предмета панель исчезает, большие очереди доступны через страницы/прокрутку.
- [ ] Старые неполученные награды и незавершённые миграции, включая offline UUID и запуск без SMP, переносятся один раз с точными ItemStack. Повторная загрузка миграции не создаёт копий; удаление профилей сохраняет race/progress/eligibility/history/rewards.
- [ ] Economy без SMP в одиночке и на dedicated: самостоятельное открытие и торговля работают. Economy off блокирует открытие/старые пакеты и сохраняет данные. Панель наград работает также при отсутствии/отключении Economy.
- [ ] Первый выбор в новом мире, первое подключение к dedicated и повторный F6: модель видна. При медленном скине сначала видна стандартная внешность, затем пользовательская; не нужны закрытие и повторное открытие.
- [ ] Несколько GUI Scale, узкий экран, книга рецептов, JEI и установленные inventory extensions: панель доступна слева без перекрытий/конфликта ввода. Ресурспак с изменёнными стандартными GUI-текстурами меняет соответствующие элементы панели через исходные ресурсы.
- [ ] На dedicated в SMP отсутствует вкладка "Награды"; получение происходит через обычный инвентарь E. Без SMP Settlements и Market имеют собственные работающие способы открытия в обоих режимах; серверный SMP лишь открывает доменные экраны.
- [ ] В integrated/LAN отсутствуют все события JEM, включая Blood Moon, Resource Rush и сезонные события: ни ручной запуск, ни сохранённый event не обходят запрет. Для обычного босса применяется World Tier без группового множителя; на dedicated тот же World Tier сочетается с множителем подтверждённых участников, независимо от посторонних online-игроков.
- [ ] Обычный Minecraft XP из каждого пути начисления даёт одинаковый базовый ×2 в одиночке/LAN и на dedicated, включая прямые модовые начисления и сферы. 100 исходного XP дают 200 без бонусов или 300 с дополнительным ×1,5; одинаковый client/server pack не даёт ×4. Mending не создаёт второй проход множителя; восстановление/перенос уже начисленного XP не удваивает его снова. Расовый и профессиональный XP остаются прежними; SMP/Economy off не отключают базовый ×2.

## Предыдущий этап: исходная universal-реализация

Следующие требования, отметки исполнения и evidence сохраняют историю предыдущего этапа. Для будущей реализации при конфликте действует дополнение выше; исторические слова solo Raid, integrated SMP и завершённая поставка не распространяются на него.

## Binding requirements

- Forge 1.20.1 / Forge 47.4.10 / Java 17; one shared production implementation for integrated and dedicated servers.
- Merge Currency Core into Economy. Preserve `jem_economy` registries, accounts, physical outstanding, receipts, prices and saved data.
- Merge all production race mechanics/presentation into one Races artifact. Preserve the `jem_roles` saved-data authority, five races and developed powers/assets.
- Distribute every Pack Core responsibility/resource to its actual domain; remove the Pack Core artifact and source owner.
- Remove the Menu Support runtime artifact/dependency. Required native controls become private to their consuming modules; GUI ownership is independent.
- Professions, Villager Talking and Claims remain development-only, permitted in sandbox/test jem, excluded from the original/shareable pack.
- Bootstrap is included in the shareable universal pack. Fix the reported long-path acquisition failure through its native owner. Replace repeated warnings/countdown with one file list, risk consent and download action.
- Economy defaults on. A world-owned off switch makes all currency functions dormant without deleting balances, coins or records. Settlements uses its existing emerald provider whenever Economy is absent/off.
- Solo supports non-event gameplay and unlimited settlements. Preserve native optional integrations and hide only unavailable features.
- World Tier adapts to available registered bosses. Twelve Eyes remains the same strict twelve original MAIN bosses; a fresh incomplete world must not initialize an impossible campaign or block vanilla End access. Existing active campaign data remains protected when content is missing.
- Events remain under JEM Server, explicitly enabled by a dedicated host; integrated worlds never schedule them.
- One content-aware FTB book preserves quest IDs/completion and shows the actual five races. FTB observes domain state.
- Authority stays on the logical server; dedicated identity/authentication, worlds, prices, cooldowns, race powers and unrelated contracts remain preserved.
- Preserve unrelated working-tree changes. Work on current main checkout without branches/worktrees. Production packaging/local sync follows standing AGENTS authorization. No automated tests, gameplay launch, MineRENT/stable changes or baseline promotion.

## Owner-aligned work

- [x] Economy/Settlements: consolidate currency ownership, optional provider boundary, world feature switch, integrated runtime, scripts and compile dependencies.
- [x] Races: consolidate source/resources/registration/network into one production artifact, canonical shared save owner, integrated gameplay, optional adapters, private controls.
- [x] Pack Core: migrate all source/resources to Achievement Guide, Compat, World Boss Tiers, Twelve Eyes and Server according to actual consumers; retain namespaces and protocol identities.
- [x] Bosses/campaign: integrated solo starts, idempotent raid preparation/rewards/respawn, content-aware progression profile and strict missing-content campaign policy.
- [x] Server: logical-server non-event services, explicit dedicated event activation, optional domain adapters and private native menus.
- [x] Bootstrap: establish exact causal path for long-name failures, fix acquisition/projection owner, simplify only consent presentation, preserve hashes/native trust/transactions.
- [x] Distribution/quests: universal offline source, Bootstrap included, development modules excluded, stable-ID adaptive quest content.
- [x] Delivery: update owning docs/Atlas/CHANGELOG, production builds only, source/JAR consistency, local native publication/application where permitted, scoped review, exact staging/commit/push and remote verification.

## Integration interfaces

Economy exposes `EconomyApi.isEnabled(MinecraftServer)` and accepts only logical-server authoritative currency mutations. Settlements owns its provider interface and never exposes Economy classes in common signatures. Race mechanics consume RaceService/RaceSavedData in both server environments. Events retain their existing implementation under one host gate. Native AutoModpack remains the sole client generation/apply owner.

## Review focus

Missing optional classes must not load through common signatures/mixins. Disabling Economy must not charge/reward/replay money or poison merchant offers. Source/resource moves must preserve every registry/save/localization/model/packet path. Existing active campaign worlds must fail closed on missing content. A Windows path failure must be fixed without bypassing download integrity or writing outside managed ownership.

## Evidence and outstanding acceptance

Before-change Atlas lookups and scan were recorded in `.superpowers/sdd/universal-standalone-20261021/`. The initial scan already reports unrelated uncovered changes; these remain separate. Builds, permitted catalog generation and source review do not establish gameplay or visual acceptance. No automated tests will be created or executed.

Source review corrected ordinary Bootstrap confirmation, Economy-off native offers/UI, native Eye admission, missing post-End targets and bypassed event handlers. Exact reviews remain in task evidence. Ordinary Server startup exposed an impl-cache staging directory lock during Bootstrap replacement; the publication mechanism was repaired and the final startup and native application completed. No client/gameplay or Linux/macOS run was performed.

Final artifact revisions: source review and production builds complete; Bootstrap cache publication now writes verified native files/stamp without replacing its directory. Guide building/7 native predicate format repaired. Shader auto-enable disabled for cross-platform fresh installation; original shader archives retained optional. The ordinary dedicated server is restarted solely for native publication/application; no gameplay/client launch or player acceptance.

Native generation 59 is published and fully applied to closed test jem. Installed active/head tokens match; all nine task artifact hashes match; old PackCore/Roles/Races artifacts are absent and no native transaction remains. All 2,750 archive file hashes and cross-platform case uniqueness pass. Existing stable/bootstrap bytes and release baseline remain unchanged. Remaining acceptance is explicitly unrun.

Final Atlas effective rebuild/audit/validator pass (118 records); scan reports only the excluded preexisting local provenance copy. No artifact drift. Source/compiled artifact/native installation/archive delivery is complete; player/gameplay and Linux/macOS acceptance remain explicitly unrun.

## Next stage: mandatory Twelve Eyes baseline - requested 2026-10-22

Status: planned only. The user requested Twelve Eyes and its complete boss dependency set as the mandatory universal gameplay baseline. No dependency metadata, gameplay, installed JAR, world or published generation is changed by this planning entry. The availability behavior described above remains the current implementation until this stage is implemented.

- [ ] Require Twelve Eyes in the universal client, integrated/LAN host and dedicated-server inventories. Preserve one shared campaign implementation and the original twelve MAIN bosses, twelve Eyes, sixteen prerequisite entities, progression proof and saved completion.
- [ ] Resolve and require the complete content closure from the canonical campaign definition: Cataclysm (`cataclysm`), Alex's Caves (`alexscaves`), Block Factory's Bosses (`block_factorys_bosses`), Legendary Monsters (`legendary_monsters`), Aquamirae (`aquamirae`), Luminous Beasts (`luminous_beasts`) and End Remastered (`endrem`). Include WorldBossTiers as the campaign's existing encounter/progression integration owner where its calls require it; do not create a second owner.
- [ ] Resolve transitive requirements against the exact installed artifacts and real source/bytecode, including Citadel, Lionfish API, Fragmentum and compatible GeckoLib. Metadata inspection alone is insufficient where native requirements are undeclared. Keep side/version requirements explicit and use compatible shared versions.
- [ ] Include every required native structure, dimension, tag and existing campaign-site adapter in validation. A present entity JAR alone does not establish a reachable campaign. Evaluate the existing post-End/ending content closure explicitly as part of the full Twelve Eyes feature set.
- [ ] For this mandatory pack baseline, reject incomplete distributions with an actionable missing-content report instead of silently deactivating the campaign and treating vanilla End access as success. Preserve the current world's history and exact prerequisites; never replace missing bosses or award completion. Decide the appropriate pack/dependency and world-registration validation boundaries during implementation.
- [ ] Trace and synchronize metadata, source, managed pack, bootstrap/manifest inventories, native publication/application, FTB projection, Atlas and owning documentation under the existing delivery rules. Keep independently distributed unrelated domains modular; this requirement defines the universal pack baseline.

Installed metadata observations on 2026-10-22: Alex's Caves 2.0.2 declares Citadel >=2.6.0; Cataclysm 3.31 declares Lionfish API >=3.0; Aquamirae 7.1.13 declares Fragmentum >=1.5.2 and GeckoLib >=4.8.0; Block Factory's Bosses 2.1.2 declares GeckoLib >=4.8.3,<5. Legendary Monsters 2.2.2 and Luminous Beasts 1.2.7 have no additional mandatory declarations in their inspected `mods.toml`; this does not prove they have no undeclared runtime requirements. These are planning inputs, not a completed dependency migration.
