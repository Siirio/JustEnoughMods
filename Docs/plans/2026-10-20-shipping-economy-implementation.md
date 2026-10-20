# Сборщик, экономика и тестовый режим — план реализации

> Исполнение после отдельного запроса пользователя: `superpowers:executing-plans`, по задачам ниже. Сейчас это не разрешение менять MineRENT. Следующий запрос «делай по handoff» включает отдельно описанную тестовую поставку и перезапуск; original только по последующей явной команде. Автотесты, игровые запуски и проверки, запрещённые правилами проекта, в план не добавляются.

**Цель:** немедленная безопасная продажа из сборщика, полный согласованный охват товаров, дневная смена рас и изолированные возможности публичного теста.

**Архитектура:** JEMCurrencyCore сохраняет единственный ledger, ограничения рынка и политики товаров; JEMEconomy сохраняет контейнер и его транзакцию. Roles владеет выбором и ожиданием смены расы; VisualSandboxR3 только адаптирует существующую презентацию. Серверный test profile ограничивает послабления точным тестовым сохранением. У UI нет собственной цены или полномочий.

**Стек:** существующие Java 17 / Forge 1.20.1 / SavedData / SimpleChannel / AutoModpack; Aseprite и штатные PNG/Java model JSON для арта. Новые библиотеки не требуются.

**Точка входа следующего чата:** [handoff и переключение](2026-10-20-test-to-original-handoff.md).

**Спецификация и цены:** [проект решения](2026-10-20-shipping-economy-prices.md).

## Обязательные ограничения

- Сейчас меняются только планы, внешние арт-кандидаты, история и глобальные инструкции. Все задачи реализации ниже не выполнены.
- Не изменять оригинальный `world`, stable-профиль, release-baseline и MineRENT в рамках подготовки.
- Не переносить тестовые Saves/SavedData, деньги, identity или creative-инвентари в production.
- Перед исходниками — Atlas lookup и impact scan; после — одни канонические change-records/effective update и разрешённая Atlas-процедура.
- Native merchant, Xaero и принятый Market UI сохраняются; новый дизайн относится только к сборщику.
- Один владелец денег, цен, выбранной расы и расписания готовности. Нет клиентских денежных решений или второго race record.
- Никаких тестов/фикстур/автоматических сценариев. Ручные критерии ниже — вопросы для будущей проверки пользователем после отдельного разрешения, не задания агенту запустить игру.
- Актуальные клиентские артефакты требуют обычного client+server релиза; эту задачу нельзя объявить server-only при изменении Screen, пакета, текстур или keybinding defaults.

## Что особенно важно просмотреть

1. Повторный клик, чужой owner, изменение инвентаря и прерванная выдача не оплачивают один товар дважды.
2. Сжатие, округление, скидки и побочные продукты не создают цикл «купил → переработал → продал дороже».
3. Диск продаётся за полную арканную монету; старый общий потолок не блокирует его скрыто.
4. Test marker не активируется на оригинальном мире или только по просьбе клиента.
5. Первый race screen не борется с авторизацией/загрузкой и не пересоздаётся при snapshot; F6 не даёт иммунитет.

## 1. Зафиксировать реальную исходную точку

**Владельцы:** `tools/jem.py`, `Catalog/README.md`, текущие исходники и документы из спецификации. Не менять baseline.

- [ ] Прочитать текущие status/diffs/staged/upstream и сохранить чужие изменения на месте. Последний status в подготовке завершился успешно: local published/staged manifest `e5eab7846e05f552d03eab2c407de279e9c72d54`, sequence49; это не доказательство live MineRENT или установленного test jem. При исполнении получить свежий status.
- [ ] Получить точные версии установленных модов и происхождение соответствующих JAR; старый loaded capture использовать как подсказку, сверяя с текущим Atlas/ManagedPack.
- [ ] Через Atlas запросить `jem_economy:shipping_box`, обычную/глубинную руду, сырьё/блок, seed, готовое блюдо и реальные музыкальные диски; собрать прямые и косвенные потребители.
- [ ] Сформировать рабочую матрицу покрытия из реальных Item registrations, FoodProperties, crop/seed tags, рецептов и классов RecordItem. Не заменять её эвристикой имён или новым постоянным каталогом.
- [ ] Исполнить конкретную редакцию прайса: ordinary caps400/10000, disc quota1/Minecraft-день, партии и точные исключения. Числа остаются предложением до команды исполнения; при новом факте несовместимости показать конкретный расчёт, не спрашивать заново уже решённые требования.

Результат: перечень реальных охваченных семейств и исключений, без заявлений о выдуманных API/товарах. Не обновлять оригинальные registry facts ценовыми предложениями.

## 2. Изолированный test profile, регистрация, расы и OP

**Владельцы:** dedicated lifecycle в `Mods/JEMServer`, существующий `JEMIdentity/IdentityPolicy`, exact installed ServerLogin, `JEMRoles/race`. Один общий читающий gate предлагается в Identity API; обратную зависимость Identity → Server не вводить.

- [ ] `active(server)` требует dedicated + явный marker + canonical save identity/path `jem-test-2026-10-20/world` + epoch. Отказ при symlink escape/другом мире. Не доверять клиенту/creative/одному названию.
- [ ] Изучить exact `Server/mods/serverlogin-1.0.3.jar`, `ru.iwoss.serverlogin.auth.UserDatabase`, LoginConfig/LoginCommands через реальный исходник/bytecode. Установить реальный store path, формат, нативный reset/session lifecycle и scope. Не придумывать `/auth reset` и не выводить credentials/hashes.
- [ ] Только после clean stop подготовить независимый test auth store и отозвать тестовые активные сессии; original store сохранить приватно. Учесть глобальные файлы вне world. Все впервые регистрируются штатно с собственным паролем; не собирать список паролей. Login остаётся fail-closed.
- [ ] Через canonical `RaceSavedData` (`jem_roles`, UUID records) сбросить только тестовый выбранный race/first-choice marker один раз на epoch. Сохранить прочие необходимые данные; не удалять целиком SavedData/оригинальные записи. Auth completion предшествует новому обязательному экрану.
- [ ] После auth каждому тестеру выдаётся тестовый OP: предлагаемый уровень2, с отдельным приватным набором grant UUID/epoch. Сохранить исходных администраторов и их уровни. При logout убрать временное повышение; при startup до допуска подключений снять оставшиеся после аварии epoch-only grants, затем восстанавливать только после нового AuthGate. Командные и денежные handlers не обходят auth из-за OP. При original переходе восстановить исходный набор до запуска.
- [ ] `gamemode=creative`, `force-gamemode=false`, survival/creative/spectator доступны тестерам; выбранное состояние переживает вход. Настройки original сохраняются отдельно. Все grants, reset и test overrides запрещены при несовпадении профиля.

Ручной критерий: два успешно зарегистрированных тестера сами меняют режим и расу; неавторизованный пользователь не получает OP/деньги/выбор; оригинальная регистрация и администраторы сохранены.

## 3. Тестовые10000 арканных и production старт20Gold

**Владелец:** `Mods/JEMCurrencyCore/src/main/java/com/siirio/jemeconomy/ledger/EconomySavedData.java`, Economy API, физическая выдача, market allowance/snapshots. Одного `ensureStarter` недостаточно: он уже имеет lifetime `starterRecipients`.

- [ ] Test issuance —100000000 copper/account **один раз на epoch**, receipt с world identity/account/epoch и checked long. Для40 аккаунтов4000000000: не использовать int в суммах/пакетах/menu. Сохранить инвариант ordinary supply + testIssued = wallets/system balances/escrow/physical liabilities. Не включать локальный одиночный development-wallet на MineRENT.
- [ ] Никакого автоматического восстановления денег при покупке/withdraw/read/login. Снятие по нужной сумме через existing PhysicalCoinService второй вкладки; не выдавать157 стаков сразу. Тестовый рынок при необходимости получает отдельную записанную test-only эмиссию, без подмены original reserve.
- [ ] Test bypass меняет только ordinary quotas при включённом override; auth/owner/fingerprint/provenance/anti-replay сохраняются. Конечный wallet виден честно. Загрузка тестового ledger без его профиля блокирует запись с явной диагностикой.
- [ ] Production migration `release/account` один раз выставляет каждый **личный wallet ровно2000** (20Gold), не добавляет2000 и не повторяет на входе. Не трогает выбранные расы, inventories, town/escrow/physical Coins. Новые игроки тоже получают конечный starter2000 один раз, обеспеченный резервом.
- [ ] Перед original миграцией финансовый preflight показывает account count, старые/целевые балансы, освобождаемую сумму, funding accounts, physical liability и остатки.40×2000=80000, исходный CENTRAL_RESERVE60000: без отдельного согласованного конечного перераспределения или достаточного текущего резерва запуск миграции запрещён. Никакой test emission/отрицательных счетов/изъятия казны молча.
- [ ] Миграция использует существующий durable transaction owner с versioned receipts и восстановлением после прерывания; не второй ledger и не повторный starter для старых recipients. `dry preflight` здесь — расчёт перед операцией, не автоматический test scenario.

Ручной критерий: тестовый grant ровно один/epoch, оригинальный rebase ровно один/release; траты переживают релог, тестовые средства отсутствуют в original.

## 4. Политики торговли и прайс

**Изменить в JEMCurrencyCore:**

- `src/main/java/com/siirio/jemeconomy/policy/ItemPolicy.java`
- `src/main/java/com/siirio/jemeconomy/policy/ItemPolicyRegistry.java`
- `src/main/java/com/siirio/jemeconomy/policy/TradeRestrictions.java`
- `src/main/java/com/siirio/jemeconomy/market/MarketSavedData.java`
- `src/main/resources/data/jem_economy/economy/item_policies.json`
- `src/main/resources/data/jem_economy/economy/trade_restrictions.json`
- Все фактические Market/Buyer/native trade projection consumers этих policy methods; найти вызовы до изменения enum.

- [ ] Добавить настоящий `SELL_ONLY`; sellable не требует buy price, buyable никогда не включает SELL_ONLY. Обновить JSON parser, валидаторы ресурсов, карточки, Buyer, Shipping Box и нативные адаптеры вместе.
- [ ] Разделить входящий выкуп у игрока и исходящую продажу NPC. Удаление полного `forge:ores` запрета не должно автоматически разрешать покупку редкого/прогрессионного сырья.
- [ ] Узко разрешить продаваемые ores/raw/storage и реальные Create food/crop/mineral goods из запроса, сохраняя ограничения машин, трофеев, служебных предметов, ядерных устройств и прогрессионной покупки.
- [ ] Семена дают реальный buy+sell offer в существующей категории; подходящие посадочные материалы не теряются из-за отсутствия FoodProperties.
- [ ] Расширить политику безопасных ItemStack: поддержанные пищевые метаданные обрабатываются явным адаптером; произвольный NBT, вложенный инвентарь, Coin provenance и уникальные экземпляры не обнулять ради продажи.
- [ ] Обратимые 9:1 пары берут одну каноническую цену и общую квоту материала. Считать рациональную семейную цену и спрос до единственного округления всей сделки; блок9:1 равен девяти единицам до округления. Quartz/amethyst и multi-output/alternate recipes разобрать отдельно.
- [ ] Применить прайс из спецификации по текущей редакции; согласованно пересчитать native offers с расовыми скидками и округлением по полной сделке, не менять их counts для маскировки дробных цен.
- [ ] Диски: настоящий RecordItem/подтверждённый jukebox-compatible вариант; фиксированная цена 10 000, без demand/расового множителя. Разрешить sell-only; обойти только обычный денежный потолок, проверить дисковую квоту и реальный общий резерв.
- [ ] Поднять ordinary caps только до отдельно выбранных в плане значений; сделать их конфигурацией владельца, а не магическими числами в каждом UI/адаптере.

Ручные критерии: сырые/обычные/глубинные формы и девятикомпонентные блоки продаются по одному правилу; seed покупается; еда из разных модов принимается; диск даёт ровно одну арканную монету при достаточном резерве. Полный аудит производственных данных не считать уже выполненным этим планом.

## 5. Немедленная продажа и миграция старого ящика

**Изменить в JEMEconomy:** `shipping/ShippingBoxBlockEntity.java`, `shipping/ShippingBoxMenu.java`, `full/market/MarketRuntime.java`.

- [ ] Сохранить block/menu ID, Owner, Instance, 54+9 slots, текущие монеты и существующий transaction journal. Предложенный новый entry point `sellAndCollect(ServerPlayer)` проверяет owner и current container на серверном потоке.
- [ ] Из обработчика кнопки выполнить fresh quote → существующую последовательность резервирования/потребления/выдачи/commit → collect. Не превратить collect в повторный вызов, потребляющий старый snapshot.
- [ ] Preview пересчитывать по bounded dirty/revision update, без full-registry scan каждый кадр. Quote содержит общую сумму/причину; клиент не передаёт цену как authority.
- [ ] Удалить только автоматический вызов `processScheduled` и ненужный реестр загруженных ящиков. Существующий периодический reconcile Buyer и суточная смена спроса/квот не являются scheduler продажи и сохраняют отдельного владельца.
- [ ] Незавершённая legacy-транзакция восстанавливается при загрузке независимо от удалённого scheduler. `LastCycle` принимается для чтения старого сохранения, но не запускает новую продажу.
- [ ] Полные платёжные слоты не уничтожают товар; частичная продажа оставляет остаток. Монеты, не поместившиеся в инвентарь, остаются в контейнере без дропа/исчезновения.
- [ ] Бюджеты и суммы выше диапазона старого синхронизируемого значения передавать корректным существующим/новым bounded snapshot; не обрезать до маленького signed/unsigned menu int.

Ручной критерий: продажа сразу после клика в любое время; повторное открытие/рестарт не повторяет выплату; старый заполненный ящик и его Owner/Coins сохранены.

## 6. Отказ предмета и подсказки

**Изменить:** `ItemPolicyRegistry.acceptsShipping`, slot predicate/shift-click/block entity insertion; `Mods/JEMCurrencyCore/src/main/java/com/siirio/jemeconomy/client/WalletClientState.java`; соответствующие локализации.

- [ ] Один predicate используется обычным кликом, shift-click и реальными внешними путями вставки блока. Отказ возвращает исходный предмет источнику; не игнорировать insert после того, как source уже уменьшился.
- [ ] При отказе дать понятную локализованную причину возле сборщика/курсорного действия. Не добавлять `Lore`, торговый NBT или глобальное «за это нельзя получить деньги» в предмет.
- [ ] Диагностически различить tooltip и изменение ItemStack: нынешний ShippingBox setter не пишет такой метки; `WalletClientState` добавляет динамический tooltip. Не удалять чужое NBT на основании недоказанной гипотезы.
- [ ] Удалить из обычного предметного tooltip служебный countdown и отказ рынка; нормальные подсказки исходного мода и пользовательские имена сохранить. Нужную цену показывать контекстно в существующем рынке без списка товаров в правой колонке сборщика.

## 7. Единый дневной выбор расы и первый вход

**Изменить:** `Mods/JEMRoles/src/main/java/com/siirio/jemroles/race/{RaceRecord,RaceService,RaceRules,RaceSavedData}.java`, `api/RaceSnapshot.java`, `network/{RaceStatusPacket,RolesNetwork}.java`, клиентские consumers; `Mods/JEMVisualSandboxR3/src/main/java/com/siirio/jemvisualsandbox/workbench/{RaceExperience,ProductionRaceBridge}.java`, `client/races/{RaceExperienceClient,RaceExperienceScreen}.java` и их фактический packet route.

- [ ] Добавить в RaceRecord schema поле готовности по monotonic overworld gameTime; контракт `remainingReselectDays` — отображение, authority хранится в обычных единицах движка. Первый выбор и UNCHANGED не начинают повторное ожидание.
- [ ] `RaceService.choose` — единственная проверка. Test profile пропускает ожидание, сохраняя auth, nonce и cleanup старых эффектов. Не ограничиваться прежним JVM-only `jem.test.allowRaceReselect`.
- [ ] Удалить дублирующую проверку FreeSwitchUsed/начального игрового окна из production-ветки RaceExperience; presentation bridge читает готовность Roles, не решает повторно.
- [ ] Передавать в snapshot выбранную расу, доступность смены, положительный остаток и тестовую метку; обновить обе стороны канала и всех потребителей. Миграция старого RaceRecord сохраняет прогресс и выбранную расу, даёт однократную готовность по договору спецификации.
- [ ] Для мигания сначала собрать цепочку login screen → auth principal ready → race snapshot → ScreenEvent → setScreen → user choice/ack. Текущий source показывает несколько потенциальных конкурирующих мест, но полная runtime-причина ещё не доказана.
- [ ] Ввести одно connection-scoped состояние показа в существующем RaceExperienceClient: ожидание авторизации/мира → одно обязательное открытие → подтверждение → один выход. Повторный snapshot обновляет существующий screen и не сбрасывает выбранную карточку/камеру.
- [ ] Не перекрывать ServerLogin, disconnect, receiving level и другие обязательные native screens. После отказа выбора показать отказ в текущем экране; после logout/смены подключения сбросить pending request и stale snapshot.
- [ ] Сохранить ручной F6, отключение HUD/восстановление камеры и единственный post-choice выход. Не выдавать сценовый иммунитет за повторный F6 или поздний ack.

Ручные критерии: первый выбор можно спокойно дочитать и подтвердить после логина; F6 продолжает работать; переподключение/сон/измерение не сбрасывают ожидание; две разные расы на тесте выбираются подряд.

## 8. Раскладка пользователя и Minecraft-дни

**Клавиши:** исходный `test jem/options.txt` читается только как пользовательский input. Канонический будущий curated resource — предлагаемый `Mods/JEMCompat/src/main/resources/assets/jemcompat/input/default_keys.json`; применение — отдельный client consumer рядом с существующими options integrations, без замены `OptionsKeyRetentionMixin`.

- [ ] Составить diff только `key_` строк и реально зарегистрированных действий, включая default keys новых модов; не распространять profile options целиком.
- [ ] Зафиксировать M/V/B/F6 согласно спецификации и назначить конфликтующим global actions отдельные свободные сочетания. Контекстные совпадения сохранить, если они действительно взаимоисключающие.
- [ ] Чистая установка применяет defaults; существующий игрок может один раз явно применить общий preset с показом изменений. Своевольное редактирование настроек при каждом update не допускается.

**Время:** общий presentation formatter, предложенный `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/GameDayText.java`, только если этот мод уже доступен всем потребителям без новой циклической зависимости; иначе поместить его в реальный общий presentation owner.

- [ ] Форматировать положительный остаток с округлением вверх до трёх десятичных знаков; менее 0,001 дня отображать отдельным порогом. Ноль означает только фактическую готовность.
- [ ] Применить к Shipping/Wallet/Market tooltip и race UI в этом scope; другие существующие экраны перечислить отдельной ручной инвентаризацией до заявления «все экраны обновлены».
- [ ] Не менять игровые часы, длительности, сетевые поля и старые save keys ради новой подписи. Глобальная норма Minecraft-проектов сохраняется в пользовательских инструкциях.

## 9. Авторские пиксели: блок, item icon, UI

**Владельцы:** существующие `Mods/JEMEconomy/src/main/resources/assets/jem_economy/{blockstates,models,textures}` и Screen; внешние editable-кандидаты — `Docs/art/shipping-station-2026-10-20/`.

- [ ] После текущей пользовательской калибровки читать exact GUI и pixel-UV Pairs. Почтовый ящик отклонён. AI concepts — только references; UI со списком отдельных товаров отклонён.
- [ ] Новый UI и значок сборщика нарисовать через Aseprite в точных native PNG-размерах; source `.aseprite` сохранить отдельно. Не рисовать HD с последующим уменьшением и не вырезать пиксели из AI.
- [ ] Сам block model при реализации описать штатным Java JSON с настоящими UV. Сохранить один размещаемый block ID, owner/hit/collision semantics; верхние весы не должны обещать дополнительное место хранения или создавать ложные непроходимые области.
- [ ] Нарисовать front/side/top texture studies 16×16 и новый семантический item icon в выбранном native размере; существующие commodity/Coins icons использовать как реальные ItemStack, без массового ненужного перерисовывания принятых монет.
- [ ] Нарисовать поверхность UI без baked labels/цен, control sprites normal/hover/focus/pressed, точную slot geometry 54+9+36; демотекст держать в отдельном скрываемом preview layer.
- [ ] Справа не создавать перечень, названия или стоимость каждого предмета. Общая сумма и budgets, большая «Забрать товар», краткий статус — весь рабочий контент.
- [ ] Привязать artwork к настоящему ShippingBoxScreen; динамические локализованные строки и server quote остаются кодовыми. Установленные Merchant/Xaero/Market screens не трогать.

- [ ] Взять editable Blockbench-кандидат из `Docs/art/shipping-station-2026-10-20/model/`. Базовый export только Java1.20.1-compatible cuboids/UV; не26.3-only rotations. Декоративные товары — native ItemRenderer/ItemStack у client renderer с anchors, без новых item ID/storage/drop. В preview геометрия нативных generated-items выдавлена по alpha, в игре используется настоящая модель, включая resource pack overrides.
- [ ] Не компрессировать товары:3–5 diamond,3–5 fish,3–5 oak logs отдельными экземплярами, выше борта; без пересечения чаш/стойки/коллизии. Удобочитаемые весы важнее дополнительного мусора. Сохранять общий block ID и server authority; decorative goods не копируют реальные содержимые слотов и не выдаются при ломании. Окончательный view в игре остаётся ручной пользовательской приёмкой.

В этом плановом запросе арт-кандидаты можно показать без установки. `.aseprite` + PNG доказывают редактируемый native-sized арт, но не работающий Screen и не визуальное принятие пользователем. Реальная игровая съёмка требует отдельного разрешения.

## 10. Settlements UI и переиспользуемые native controls — добавлено пользователем

### Вторая вкладка сборщика: обмен и получение монет

**Пользователь:** перенести обмен из `/smp market` в блок сборщика, сделать второй вкладкой и обеспечить фактическое получение монет.

**Текущие владельцы:** `JEMCurrencyCore/.../client/MarketScreen.java#renderExchange`, `network/EconomyNetwork.java#handleExchange`, `physical/{CoinDenomination,PhysicalCoinService}.java`; `JEMEconomy/.../shipping/ShippingBoxMenu.java` и `full/client/ShippingBoxScreen.java`. Обмен уже является physical-to-physical, а отдельный wallet withdraw уже выдаёт обеспеченные ItemStack.

- [ ] Вкладки «Продажа» / «Обмен монет». Удалить переключатель и exchange-only controls из MarketScreen, сохранив принятую витрину, поиск, категории, покупку/продажу и Market artwork. Серверный currency service остаётся единственным владельцем.
- [ ] Перенести выбор исходного/целевого номинала, количества, точного результата и подтверждение. Дать дробить и объединять все пять номиналов по текущему 10:1; запретить дробный результат, одинаковые номиналы, переполнение и неподтверждённые монеты прежним владельцем.
- [ ] Раздел «Получить монеты»: показать собственный wallet balance, сумму снятия и реальную выдачу в инвентарь через существующий withdraw, с отдельным request ID и подтверждённым результатом. Выдача не означает бесконечный кошелёк production; тестовая экономика изолирована отдельно.
- [ ] При недостатке денег, места или некорректных монетах показывать конкретную причину. Двойной клик/повтор пакета не платит второй раз; успешный обмен обновляет видимый inventory/balance. Не ограничиваться картинкой результата.
- [ ] Привязать вкладку/действия к актуальному containerId, открытому ShippingBoxMenu, доступу и расстоянию; на сервере проверять источник запроса, не доверять состоянию клиентской вкладки. Новый shipping adapter вызывает общий service, не реконструирует ledger.
- [ ] На второй вкладке скрыть и деактивировать товарные/payout слоты одновременно для rendering, hover, click, drag и quick-move. Предметы первой вкладки сохраняются в настоящем BlockEntity и не продаются при смене вкладки; player inventory остаётся на прежних координатах. Скрытые слоты не должны принимать невидимые клики.
- [ ] Ответ exchange/withdraw направлять открытому сборщику, не открывать MarketScreen старым catalog callback. Закрытие/смена вкладки не повторяет уже отправленную транзакцию. Не вводить вторую валюту, новую комиссию или новую эмиссию.
- [ ] Перед реализацией пройти Atlas lookup/impact scan для shipping block и всех coin IDs; applied record создать только вместе с исходниками. В текущем запросе добавлены план и внешний Aseprite-кандидат, обмен ещё не перенесён в runtime.

### Уточнения пользователя после первого рисунка

- Игровой инвентарь сборщика сохранить. Значок весов целиком внутри header; монеты и цифры каждой суммы имеют общую baseline и собственные непересекающиеся bounds. Не выводить перечень предметов справа.
- В блоке сборщика использовать настоящие native ItemStack-модели алмазов и рыбы, в узкой части — `minecraft:oak_log` (дубовые брёвна, не доски). По3–5 объёмных экземпляров каждого вида выступают над бортом с разной высотой и наклоном; не плоские карточки и не сжатая кучка на дне. Это контракт будущих render anchors, а не разрешение нарисовать поддельные vanilla-предметы или считать плоский texture study готовой моделью.
- Убрать «Все жители» и «По имени» из GUI и кода. Оставить живой поиск и автоматический порядок по имени. Выплату задолженности рекрутам сохранить доступной из карточки рекрута с прежней серверной проверкой.
- Для счастья, сытости, жилья/кровати, безопасности, работы, сообщества, уюта и жителей нарисовать различимые native16 иконки и ставить рядом с соответствующими показателями. Не заменять ими смысловые подписи и не добавлять украшения ко всему тексту.
- Пользователь отверг скучный плоский черновик. Открывать реальные положительные GUI references перед рисунком; использовать читаемые силуэты, объём материалов и осмысленные детали. Глобальный порядок принадлежит `taste-engine`, не этому плану.
- [Все страницы и состояния — внешний арт-кандидат](../art/settlements-pages-2026-10-20/index.html): editable Aseprite + native PNG, без установки. Эти картинки не доказывают отсутствие runtime overlap или пользовательское принятие.

**Существующие владельцы:** `Mods/JEMSettlements/src/main/java/com/siirio/jemsettlements/client/{SettlementScreen,TownFoundingScreen,CityInspectorScreen,RecruitHirePresentation}.java`, соответствующие resources. `CityInspectorScreen` не является разрешением вернуть удалённую изометрию всего города. Для hire сохранять настоящий native menu/slots и согласованную семью Recruits.
**Предлагаемые общие ресурсы:** отдельный PNG atlas контролов в реальном общем клиентском owner после проверки dependency graph; editable оригинал в `Docs/art/shipping-station-2026-10-20/`. Если общего обязательного owner нет, не вводить dependency ради одной картинки: использовать один authored resource pack с явным владельцем и модулями-потребителями.

- [ ] Зафиксировать настоящий источник каждого overlap: world/screen/local координаты, GUI scale, восстановление resize, независимые text baselines, scissor, z и hit bounds. Исторический скриншот/старый чат не заменяет текущую геометрию.
- [ ] Нарисовать в Aseprite в конечном PNG-размере общий slot18 с внутренней областью16, button states normal/hover/focus/pressed/disabled и требуемые tab/scrollbar states; для новых action icons отдельные native16 PNG. Рисунок не содержит baked labels/чисел.
- [ ] Описать один layout result на `init/resize`: header/navigation/content/footer, roster/detail/scroll bounds. Все render, mouse, keyboard focus и tooltip используют этот результат, без ручных вторых offsets в каждом обработчике.
- [ ] Реальные Slot/Menu и native Button/EditBox/scroll mechanics остаются настоящими контролами. Native-derived art не даёт разрешения скопировать всю чужую Screen или поменять settlement actions.
- [ ] Строка roster имеет одну вычисленную высоту; portrait, text и action regions не пересекаются. Длинные имена и пояснения ограничены своим wrap/clip, tooltip выводится поверх UI без изменения layout.
- [ ] Для малого окна определить конкретный переключатель layout по доступной ширине: roster и detail по вкладкам либо вертикальная прокрутка. Пустые области не растягивать до огромных пустых панелей. Scale меняет всю систему согласованно.
- [ ] Сохранить текущую прежнюю семью интерфейсов/согласованный Market; не возвращать отвергнутые AI-иконки и иллюстрации. Семантические предметные/сущностные native icons сохранить, новые рисовать в Aseprite.
- [ ] Обновить `Docs/settlements/DESIGN_JOURNAL.md` после каждого согласованного layout/asset решения; текущая запись помечается планом, не реализованной механикой.

Ручные критерии после разрешённой реализации: длинные русские имена/цифры, пустое и большое поселение, разные размеры окна и GUI scale не дают overlap; видимый slot/button и зона нажатия совпадают. Проверку игры агентом этот план не запускает.

## 11. Документация и будущая поставка

### Дополнение: Nature’s Compass — регистрация, creative и получение

- [ ] Проверить exact установленный `NaturesCompass-1.20.1-1.12.0-forge.jar`, зарегистрированный `naturescompass:naturescompass` и его фактическую загрузку в обычном и test клиенте. JAR в ManagedPack сам по себе не доказывает native AutoModpack installation.
- [ ] Проследить вкладку Creative, JEI и поиск по русскому/английскому имени; проверить текущие datapack/KubeJS/creative-tab filters и конфигурацию. Не добавлять вторую копию мода или новый предмет вместо обнаружения причины.
- [ ] Проверить настоящий survival-рецепт и реально разрешённые ингредиенты; согласованно вернуть доступность существующего предмета. Скрытие в меню и отсутствие рецепта — разные дефекты.
- [ ] После реализации обновить канонического владельца, matching Atlas change и обычный client/server pack; не утверждать, что Nature’s Compass доступен игрокам, пока установленный клиент это не подтверждает.


- [ ] После реализации обновить один owning economy document, race README, `Docs/DEVELOPMENT.md`, `Docs/SERVER_COMMANDS.md`; ценовые экспорты строить из реального владельца, не оставить этот проект прайса вторым исполняемым источником.
- [ ] В тот же task обновить Atlas changes/effective: admission/prices, instant shipping, test profile, race lifecycle и input/units по своим selectors. Не писать applied-правило для ещё не реализованного предложения.
- [ ] Собрать только необходимые production artifacts по действующим standing rules; не запускать tests/QA/клиент. Синхронизировать свежие обычные Server и ManagedPack, не захватив посторонние подготовленные, но не установленные JAR.
- [ ] При managed-client изменениях пройти `stage-pack-sync` → native preview → publish → закрытый `test jem` native apply с точным review. Сверить published head/active token/хеши/отсутствие pending transaction до слова «готово».
- [ ] MineRENT пока под запретом на изменения/перезапуск. После отдельного разрешения использовать существующий test-world runbook, ставить новые артефакты только в изолированный test target. Не копировать local test generations или credentials.
- [ ] Все последующие поправки теста вести в canonical source, оттуда в обычный client/server release. Отдельно перед будущим переключением на original создать тогда актуальный backup по уже указанному пользователем моменту; не объявлять его сделанным сейчас.
- [ ] После завершённого этапа просмотреть точный diff относительно fetched upstream; CHANGELOG различает user decisions / Codex choices / не проверенные гипотезы. Commit и обычный push только task-owned paths на текущей main; подтвердить remote commit.

## 12. Рациональные цены, строительный опт и честная доставка

**Единственный денежный owner:** CurrencyCore ItemPolicy/Registry + item_policies.json + EconomyApi, существующие Market/Buyer/native adapters; Shipping потребляет quote. Runtime второго price CSV не создавать.

- [ ] По текущему Atlas snapshot собрать exact registered IDs, block/item связь, tags, recipes+output counts, loot, effective modifiers, фактические BE/container/redstone/energy/transport/effects/gating/NBT. Отсутствующая/пустая запись =NEEDS_REVIEW, не покрытие. В том числе Handcrafted/Woodworks/Create/Quark/Natures Spirit/Regions Unexplored — только реально установленные версии, без whitelist по namespace.
- [ ] Один доказанный класс на ID: TERRAIN_FILL / COMMON_BUILD / DECOR_BUILD / PREMIUM_DECOR / INDIVIDUAL_FURNITURE / FUNCTIONAL_RESOURCE_OR_MACHINE / PROGRESSION_PROTECTED / NEEDS_REVIEW. Самый строгий признак побеждает. Алгоритм и фиксированная семейная матрица описаны в прайсе; соседние равноценные варианты не получают случайные разные цены.
- [ ] Создать generated audit рядом с owning economy docs с колонками mod_id,item_id,display_name,class,why,source_registry,source_recipe,recipe_output_count,base_material,effective_bulk_price_1024,price_x64,price_x256,price_x4096,buy_allowed,sell_allowed,arbitrage_status,requires_manual_review. Это отчёт из canonical policies, не второй каталог/ledger. Сначала завершить coverage/арбитраж, потом publish; неизвестные товары не допускать наугад.
- [ ] Расширить реальные ItemPolicy/quote consumers с integer unit на numerator/denominator и lot size; buy=ceil(total), sell=floor(total) с обеспеченным остатком у существующего ledger. При payout0 оставить товар. Escrow покрывает округлённое вверх обязательство дробного остатка; повтор/прерывание не теряет и не удваивает остаток. Все цены/наборы/продажа/вилладжеры используют один расчёт, counts native offers не менять ради округления.
- [ ] Existing purchaseMarketItem сейчас запрещаетquantity>64. Добавить серверный bulk order в том же API owner с64/256/1024/4096, конечным quote/revision/requestId и реальной destination capacity/access. Не менять max stack и не ставить buy_copper0. Цены стройопта фиксированы, BUY_ONLY; обычный дуб1024=1120, не дешёвый terrain.
- [ ] Добавить действующий фильтр «Строительные материалы» с terrain/stone/wood/finish/mod decor и размером заказа в сохранённую Market-витрину. Не менять её художественную семью/нативных торговцев. Показать реальное количество/цену/назначение и результат, без муляжей кнопок.
- [ ] Доставка обычными стаками в существующий свой/поселенческий санкционированный склад; Tom’s Storage только после exact API/access проверки. Нет новой системы проектов, вкладов или общей копилки. Пока destination не доказан/полон — отказ до списания либо существующая durable order rollback, без потери денег/материалов. Client не назначает цену и не повторяет выдачу пакетом.
- [ ] Полный recipe graph: crafting/output counts/Create crushing,washing,milling,sawing/переплавка/рециклинг/красители/splits/byproducts/NBT/native discounts/demand. BUY_ONLY сам по себе не предотвращает прибыль от продажи производных. Согласовать вход/выход policy, не ломая полезные машины ради дешёвого декора.
- [ ] Сохранить доход exact cod3/salmon4/beef4/leather3, сверить реальные per-item quotas (в частности рыба32/вид/Minecraft-день); all foods/crops/seeds/minerals покрыть точными данными. Ordinary caps400/10000, funded NPC_MARKET; покупки стройматериалов идут в тот же рынок. Не обещать гарантированную выплату40 игрокам или manual-caught detector без сигнала.

Ручные ориентиры:16384 земли стоят640 медных;8192 блока примерного набора480;20cod дают базовые60 при наличии квоты/резерва; нет прибыли из покупки дешёвого блока и продажи переработки. Это критерии, не запущенные тесты.

## 13. Backpacked: печь и Auto Feed, существующий Workbench сохраняется

**Исходная точка:** `Docs/backpack/README.md`, exact Backpacked3.0.9 / Framework0.8.0 и реальный JEM Workbench source owner. Сначала прочитать registrations/bytecode/API/augment storage текущих JAR и найти существующие классы в Mods; не выдумывать API/новый мод/вторую мастерскую. Удалённый Professions/Architect сюда не возвращать.

- [ ] Расширить native BackpackContainerMenu настоящими Slot/ResultSlot/RecipeManager и существующим augment unlock/settings. Workbench3×3+result между backpack/player остаётся. Layout один для render/hit/focus/tooltip, native18px controls; новые пиксели только Aseprite/native PNG, совместимая семья Backpacked.
- [ ] Печь: input/fuel/output, серверные реальные smelting recipes, расход топлива, прогресс, remainder/container/XP. Состояние в canonical storage конкретного рюкзака, не второй inventory. Работает при equipped+GUI closed; pause on unequip, нет offline progress или бесплатной мгновенной плавки. Atomic consumption/result, место/shift-click/drop/death/transfer/reload не дублируют предмет.
- [ ] Auto Feed: equipped+enabled+auth+FoodData accepts, первая подходящая обычная еда расходуется на одну настоящую порцию; hunger+saturation. **Нет eating animation, hand use, звука и частиц.** Конечный cooldown, не расходовать при насыщении. Эффектная/редкая еда opt-in; native finishUsingItem/тара/эффекты — точный адаптер или отказ без потери/NBT wipe. Выключено после снятия/смерти/чужого ownership/отсутствия еды.
- [ ] Сервер единственный владелец процесса; client только renders snapshots. JEM shared gameplay остаётся dedicated-only; не протащить печь/AutoFeed через common listener в native singleplayer. Atlas impact/history/effective, документация и matching JAR на обеих сторонах обязательны.

Ручная приёмка:3×3 не пропало; печь требует топлива/сохраняет процесс; AutoFeed реально тратит еду и насыщает без анимации; UI не overlap. Не считать пройденной без пользователя.

## Критерии готовности будущей реализации

| Область | Что должно наблюдаться пользователем |
| --- | --- |
| Тест | два зарегистрированных тестера получают10000Arcane в wallet один раз/epoch, unlimited race switching и OP2 для трёх режимов |
| Оригинал | исходный мир/инвентари/auth сохранены; wallet ровно20Gold после однократной обеспеченной миграции, далее конечный; race cooldown1Minecraft-день; test privileges отсутствуют |
| Сборщик | мгновенная кнопочная продажа; нет scheduler, контекстной метки на предмете и списка товаров справа |
| Цены | продать можно все согласованные группы; 9:1 точно; все seeds покупаются; диск оплачивается ровно одной арканной |
| Расы | первое окно не мигает; F6 работает; обычная смена один раз за Minecraft-день |
| Клиент | native AutoModpack применил точное поколение; первого входа для повторной установки не требуется |
| Арт | настоящий Aseprite source и native PNG; динамические значения; отдельно пользовательская оценка уже работающего UI |

Ни один из этих будущих критериев не объявляется пройденным сохранением плана или показом картинки.
