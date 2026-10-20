# Передача обновления JEM: тест → исправления → оригинал

Дата20.10.2026. **Сейчас выполнена подготовка планов и внешнего арта. Gameplay-задачи ниже не выполнены. MineRENT не менять и не перезапускать в этом запросе.**

Это входная точка следующего чата. Полная спецификация и единственный проект прайса: [цены и поведение](2026-10-20-shipping-economy-prices.md). Задачи с владельцами: [реализация](2026-10-20-shipping-economy-implementation.md). Не заменять эти документы новым конкурирующим прайсом. Текущие исходники, logic locks и owning Docs важнее исторических чатов/отчётов.

## Что сказать следующему чату

> Делай по Docs/plans/2026-10-20-test-to-original-handoff.md и двум связанным планам. Закончи всю реализацию, production-сборки и обычную native AutoModpack-поставку в test jem. Подготовь и выполни поставку на тестовый мир MineRENT с чистой тестовой регистрацией и новым выбором расы, затем перезапусти именно тест. Это разрешение на описанный тестовый перезапуск после готовности артефактов. Оригинальный world не переключай, не мигрируй и не запускай до моей отдельной команды. Автотесты и самостоятельный запуск Minecraft запрещены.

**Одна команда исполнения включает всю тестовую фазу**, не только запись исходников. Перед действием заново прочитать текущие правила и живую панель; не переспрашивать уже выбранные значения. Если конкретный пароль/доступ/неразрешённое финансирование действительно отсутствует, закончить независимую подготовку и назвать точный блокер. Не останавливать работающий сервер ради неподготовленных файлов.

## Подтверждённые решения пользователя

| Область | Тестовая копия | Оригинальный мир после отдельной команды |
| --- | --- | --- |
| Save target | `jem-test-2026-10-20/world` | исходный `world`, проверить живую конфигурацию |
| Пароли | Новая штатная регистрация всем; свой изолированный store | Сохранить исходные регистрации; тестовые пароли не переносить |
| Расы | Новый первый выбор; затем unlimited switching | Существующую расу/прогресс сохранить; смена раз в Minecraft-день |
| Wallet |10000Arcane на аккаунт один раз на test epoch; снимать по необходимости | Ровно20Gold (2000Copper) новый стартовый wallet один раз на release/account; не прибавка и не reset при логине |
| Денежная масса | Учтённая test-only эмиссия, конечный grant | Конечная обеспеченная экономика; тестовая эмиссия запрещена |
| Права | Пользователь просит OP; предлагается level2 только после auth | Только исходные администраторы, без тестовых OP/creative |
| Режим | Начальныйcreative, свободный survival/creative/spectator; force-gamemode=false | Исходные production props/режимы, без тестовых inventories |
| Миграция данных | Обновляются только тестовые данные через canonical owners | Никаких копий test SavedData/playerdata/inventory/auth/ledger/race обратно |

Полностью исключены совместные взносы и общий фонд проекта. Architect/instant placement/новая система проектов не входят в релиз и не возвращают удалённый Professions. Workers/его Settlements compatibility и Saints Dragons не возвращать.

## Факты, известные при подготовке

- Историческая квитанция `Docs/releases/2026-10-20-minerent-test-world.json` указывает исходный `world` и тестовый `jem-test-2026-10-20/world`. Это не снимок живого сервера. Его работоспособность/состояние сейчас неизвестны.
- Копия происходила из уже выбранного пользователем архива примерно11:23. Пользователь сообщил о существующем исходном backup. **Второй свежий backup не подтверждён.** Сейчас новый не создавать; перед будущим production cutover сделать тогда актуальный снимок по отдельной команде перехода.
- Последний обычный local `jem.py status`: successful, local native sequence49, staged=published manifest `e5eab7846e05f552d03eab2c407de279e9c72d54`, published=true. Это не доказательство установленного active token в test jem и не live MineRENT publication.
- Последний доставленный до этого планового запроса base commit `bcfb7c24643ba0a0e64c35dd89a8db5e42193866`, main. Перед продолжением получить текущий HEAD/diff: есть чужие незавершённые Roles VFX/Settlements/Atlas/runtime changes. Не включать их в сборку/commit автоматически и не откатывать.
- 37 Settlements страниц/состояний и две вкладки сборщика — внешние Aseprite/PNG кандидаты, не установленный Screen и не пользовательская визуальная приёмка. Узкое удаление «Все жители»/«По имени» уже есть в source, но не объявлять установленным без matching artifact.
- Исторические проблемы: первый race screen мигает, Nature’s Compass отсутствует в creative у пользователя, bootstrap journal mirror падал. Это симптомы/план расследования, не доказательство текущей первопричины и не разрешение удалять клиентский cache. Connection reset без свежего server/client log тоже не считать исправленным.

## Этап A. Реализация в текущем checkout

1. Прочитать AGENTS, global logic locks, DEVELOPMENT, owning economy/race/backpack/Settlements Docs. Выполнить `python tools/jem.py status`, inspect branch/diffs/staged/remotes/local-only commits. Работать на текущей intended branch, без worktree/новой task branch; чужое оставить на месте.
2. Пройти Atlas lookup → impact scan по объектам/потребителям до source. Не использовать неполные/пустые registry captures как факты. До exact mod integration прочитать реально установленный JAR/API/конфиг/bytecode.
3. Выполнить все задачи отдельного implementation plan: instant shipping без scheduler, admission без контекстной метки на предметах; полный охват ores/foods/crops/seeds/discs; rational lot pricing/стройопт и обеспеченная доставка; coin exchange и wallet withdrawal во второй вкладке; test profile/auth/race/OP/money; original money migration/daily race; keybinding preset; first-login lifecycle; Nature’s Compass; Settlements/layout/icons; Backpacked furnace/AutoFeed с сохранением Workbench.
4. Арт: открывать реальные references при каждом visual work. Только Aseprite/native PNG, Blockbench geometry/UV. Блок — весовая станция с товарами выше края, по3–5 отдельных diamond/fish/oak_log, не oak_planks и не карточки. [Модель и anchors](../art/shipping-station-2026-10-20/model/README.md), [сборщик](../art/shipping-station-2026-10-20/index.html), [все Settlements страницы](../art/settlements-pages-2026-10-20/index.html). Game inventory оставить, right column без списка товаров, baseline/hit/focus/scissor из одного layout.
5. Один canonical owner на состояние. Не дублировать CurrencyCore ledger/prices, Roles record, Backpack storage, auth principal. Dedicated-only shared gameplay, client presentation only. Сохранить protected native merchant/Xaero/Market visual family.
6. Обновить matching Atlas history/effective в том же source task и owning documentation/CHANGELOG; не объявлять applied прежний план. Построить только нужные production artifacts по действующим standing rules; не выполнять tests/linters/QA или клиентский launch.

## Этап B. Обычная локальная поставка и точные артефакты

1. Согласовать client/server registrations, newly exercised channels и exact owning JAR versions. GUI/packet/textures/Backpacked меняются: полный client+server boundary, не server-only.
2. Свежие reobfuscated outputs в ordinary Server и canonical `Client/ManagedPack`; clean bootstrap-only installer не превращать в managed tree. Не запускать игру. Не raw-copy в активный AutoModpack profile.
3. Native `stage-pack-sync` → `/jempack preview` → `/jempack publish`; при необходимости разрешён обычный local dedicated restart. Проверить staged/published hash equality и published=true. Далее закрытый `test jem` получает native review/application exact generation. Если он запущен, попросить закрыть, затем закончить; не называть downloading/публикацию установленным обновлением.
4. Installed active token=published head, exact JAR hashes, no pending transaction. Stable JustEnoughMods cache не удалять/не preseed; для bootstrap recovery только matching standalone bootstrap через разрешённый native путь. Исторический mirror failure требует исправления owner по evidence-first-debugging, без ослабления trust gate.
5. Подготовить точный host staging manifest из ManagedPack, не из TLauncher/runtime/test substitution. Restore local transport-test generations, если имеются; не переносить Server/local, миры, credentials, private Xaero data или temporary JAR.

## Этап C. Подготовка быстрого тестового рестарта

До остановки всё уже готово: source/artifacts, hash manifest, upload staging, profile transaction/rollback instructions. Не обещать секунды без размера загрузки/скорости панели.

- Через authenticated MineRENT прочитать live сервер/файлы/console/network; зафиксировать реальный текущий level-name, server ID, путь/identity обоих миров, состояние и выбраный backup. Имя каталога само по себе не доказывает, что это original. Не создавать/удалять мир заново.
- Подготовить два **конфигурационных профиля**, не две расходящиеся сборки: test и original. Общие артефакты одни; отличия — save target, test marker/epoch, auth-store binding, props, OP policy, finite money migration profile. `.properties` не заменять целиком из чужого шаблона.
- Изучить реальное ServerLogin UserDatabase/LoginCommands/LoginConfig в exact JAR. Установить, где DB/session storage: world-local или server-global. Подготовить namespace binding либо приватное staged swap по конкретным файлам. **Не сбрасывать глобальную DB до изоляции original.** Не выводить содержимое/hashes, не коммитить auth/ops/playerdata.
- Сохранить исходный admin set/levels приватной квитанцией; test OP отдельно. Это сохранение необходимой конфигурации для обратимого профиля, не запрос на новый world backup сейчас.
- Test fresh registration/race selection/grant имеют versioned epoch receipts; обычный restart повторно их не сбрасывает. Новый тестовый epoch — только явная новая команда reset, не каждый startup.
- На тестовом world совпадают profile gate + save identity + epoch; auth/OP/grants включаются только после нативного auth completion.40×100000000 требует long во всём пути. Форсированный creative при login выключен.

## Этап D. Выполнить тестовую поставку по команде исполнения

1. Если игроки онлайн, предупредить их штатной server-console командой о согласованном обслуживании. Чат/почту вне игры не рассылать. Clean stop, дождаться полного завершения JVM/сохранения; не редактировать живые SavedData/DB.
2. Загрузить/проверить staged release files; применить только manifest replace/add/delete. Сохранить предыдущий **артефактный bundle/конфигурационные bindings** для rollback. Это не новый world backup и не повод копировать personal data в Git.
3. Bind test save/auth/operator profile. Original save не переименовывать/не перезаписывать; authoriginal не удалять. Отключить claims только в согласованном release manifest, сохранив original данные приватов для возможного возврата; подтвердить отсутствие нежелательного claims JAR, не удалять неизвестные зависимости наугад.
4. Применить staged epoch reset через canonical owners при безопасном offline/load lifecycle: test auth/session reset, first race selection, one-time wallet grant policy. Не править NBT/ledger вручную, если владелец уже предоставляет migration/service. Если подходящего безопасного API нет — сначала реализовать его в source.
5. Запустить один раз. Проверить именно console startup success/loaded save path, auth readiness, exact JAR set, test gate и native AutoModpack публикацию; состояние кнопки панели не заменяет лог. При ошибке остановить, исправить причину, не просить игроков бесконечно перезапускать launcher.
6. Прочитать живую страницу `Сеть`: основной `ForeverSMP.minerent.io:25565`, резервный только IP **с показанным динамическим портом**, не IP:25565 по догадке.
7. Пользователь/друзья вручную подтверждают download → commit → reconnect → новая регистрация → устойчивый первый выбор → 10000Arcane wallet → withdrawal → OP modes → unlimited race switching. Не требовать паролей в ответе. Успех одного startup не означает успешный вход или player acceptance.

## Этап E. Поправки после теста

Каждая правка идёт в тот же canonical source → resources/Atlas/docs → fresh production outputs → ordinary local ManagedPack/Server → native published+installed generation → exact host test artifact. Ни одной правки только в live JAR/config, которую потеряет original release. Вести одну cumulative release manifest с hashes/replace/add/delete и последним исправленным generation; не замораживать первоначальный test ZIP.

План/условия production профиля сохраняются между итерациями. Изменение тестового UI/баланса не должно вернуть production unlimited money/race/OP. Перед каждым тестовым restart уже готовы конкретные артефакты; каждый дополнительный restart согласуется текущим пользовательским запросом и живым состоянием. Current freeze не даёт права вмешиваться сейчас.

## Этап F. Только после «переноси на оригинальный мир»

1. Проверить финальные принятые исправления и cumulative artifacts, exact client/server channel match. Повторить financial preflight по **актуальным original данным**, не тестовой копии. Для40 wallets нужно80000Copper; CENTRAL_RESERVE исходно60000. Если перераспределение/источник ещё не согласован или средств не хватает — закончить staging и показать точный короткий вопрос о фактическом дефиците. Не начинать миграцию без обеспеченного плана.
2. В согласованное окно clean stop. **Теперь создать новый актуальный backup оригинального мира и всех его относящихся global auth/ledger/config state**, проверить завершённость/читаемость и путь. Не использовать старый11:23 снимок как будто он отражает весь игровой прогресс. Записать приватный backup receipt без secrets.
3. Выключить test profile/epoch/issuance/quotas overrides, restore original auth store/admin levels/props и level-name. Ни один test user не остаётся OP по наследству. Запрещено сливать test playerdata, деньги, Coins, расы, инвентарь и advancements.
4. Применить **те же финальные исправленные** source-derived artifacts, поддерживающие оба профиля. На original выполнить once-only funded wallet migration к2000/person с durable receipt; сохранить остальные обязательства и персональные данные. Первый выбор расы для original не сбрасывать, cooldown1Minecraft-день. Не использовать test grant path.
5. Запуск: подтвердить actual original identity, auth original, test gate false, finite supply invariant/20Gold migration count, исходных админов и production props. Проверка startup/read-only receipts разрешена; gameplay acceptance выполняет пользователь.
6. Обычный authorized release boundary: clean Client/stable distribution/friend cumulative point-update с exact replace/add/delete и полным kubejs, matching bootstrap; native install, local release timestamp, baseline promote **только на этом явном production release** по DEVELOPMENT. Не raw-copy managed stable cache. Отдельный test deployment не объявлять production baseline.

## Ошибка и быстрый возврат

- До старта при неполной загрузке/невалидном profile binding оставить сервер выключенным, восстановить предыдущий согласованный artifact/config profile целиком. Не запускать наполовину обновлённый набор.
- При запросе «верни original без применения обновы» подготовить старый совместимый bundle, original bindings/auth/admin и props; test data не переносить. Проверить, допускает ли original schema старые JAR. Если original ещё не мигрировал, не откатывать его к раннему архиву.
- Если original уже изменён новой схемой и downgrade несовместим, rollback только из свежего проверенного **передмиграционного** снимка согласованного набора данных + matching old artifacts. Не лечить пересохранённый мир заменой одного JAR. Не обещать возврат без потери последующего gameplay; объяснить точную границу снимка.

## Квитанция каждого завершённого этапа

В owning release record/CHANGELOG без credentials: branch/commit/remote SHA, source/artifact hashes, exact mod/network set, save identity/path и profile, test epoch или production migration version/count, staged/published manifest/head, подтверждение native installation token/no pending transaction отдельно, stop/start success отдельно, user acceptance отдельно. Auth/OP/backup только идентификаторы/статус, приватные детали остаются вне Git.

Завершить текущий этап: fetched remote/base diff, explicit-path commit и normal push текущей intended branch, remote SHA verification. Не включать чужие изменения, private data или миры. Финальный отчёт правдив: что реализовано, что синхронизировано/опубликовано/применено, что не запускалось и что ждёт пользователя.
