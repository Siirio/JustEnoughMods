# Boss Fight

## Primary state

Boss Fight — host-controlled encounter для одного зарегистрированного босса. До Start lobby хранит boss identity, host, accepted participants и event instance. Host может стартовать в пределах трёх блоков от реальных bounds, даже находясь снаружи стены. Solo host и все accepted participants получают passage через shell немедленно после старта; outsiders, mobs и projectiles остаются заблокированы.

Boss HP и damage увеличиваются на 20% за каждого дополнительного присутствующего участника. После этого к итоговому damage hosted boss применяется только один correction: 12% reduction для direct, projectile, effect и hardcoded damage. Gear score не участвует.

Помеченные тематические bosses, включая Magnetron, Scylla и Cloud Golem, используют общий эффект «Любимчик Зевса»: раз в семь секунд позиция одного живого участника фиксируется компактным электрическим телеграфом на полсекунды, затем туда ударяет boss-owned молния. Игрок может увернуться, покинув отмеченную область до impact.

Start требует зарегистрированного boss identity. Boss Fight является владельцем общего encounter lifecycle: зарегистрированная структура, её реальные bounds, безопасный вход, boundary enforcement и безопасный выход одинаковы для обычного боя и Raid. Travel переносит accepted consenting participants к безопасной позиции снаружи структуры; после Start shell открывает проход только им, и каждый входит самостоятельно. Сервер проверяет фактическую конечную точку любого player teleport: попытка попасть внутрь закрытой арены через Xaero, команду, `/smp` или event action перенаправляется к сохранённой staging-позиции либо к ближайшей безопасной позиции снаружи shell. Tick fallback немедленно выводит наружу игрока, которого сторонняя система поместила внутрь без обычного входа; уже вошедший active participant остаётся внутри. Выход за bounds возвращает active participant внутрь и предлагает host завершить бой для всех, а member — покинуть только себя. Структурная граница рисуется только в основном цветовом проходе мира и никогда не отправляется в shadow pass шейдерпака, поэтому возле арены нет второй camera-relative коробки. Структурный босс всегда сохраняет исходную точку, в которой его создала генерация структуры: revive, recovery и старт боя не заменяют её вычисленным safe-position и не переносят босса на крышу. Lobby marker не выключает AI, не отменяет entity tick и не блокирует входящий или исходящий урон; при старте ближайший живой participant назначается первой целью. После завершения участник возвращается в сохранённую точку перед входом; bounded fallback около структуры не использует dimension heightmap, поэтому Nether roof не может стать точкой входа, выхода, respawn или reinforcement spawn. Ambient mobs не входят в arena; разрешены encounter boss и его authored summons. System-created encounter party не расходует public posting cooldown.

## Arena

Операторский revive является восстановлением структуры, а не rematch unlock: зарегистрированной структуры и отсутствия живого босса достаточно даже до первой победы. Устаревший UUID отсутствующей entity заменяется новым, первая последующая легитимная победа сохраняет право прогрессировать World Tier, а Raid остаётся закрытым до этой победы.

Campaign prerequisite управляет одной invisible indestructible physical shell вокруг полных structure bounds плюс десять horizontal blocks. Natural solid blocks не заменяются. Shell не имеет selection outline, не допускает blocks/snow на поверхности, жидкость сверху и piston crossing. После выполнения prerequisite campaign shell снимается; overlapping staging wall не создаётся. Если shell пересекает территорию активной Blood Moon, сервер временно снимает её физические blocks и boundary enforcement для игроков: движение игроков и мобов принадлежит Blood Moon и не может блокироваться соседней ареной. После завершения Blood Moon shell восстанавливается из тех же structure bounds без замены natural solid blocks.

## Rejected

- Start только изнутри арены отвергнут: закрытая shell делала вход невозможным.
- Глобальная отмена teleport commands/items отвергнута: boundary lifecycle владеет возвратом и явным выходом из боя.
- Gear-based scaling отвергнут пользователем.
- Любой solid collision block можно поставить прямо вместо невидимой ячейки structure shell; не-блоки и blocks без collision стену не заменяют.
- Несколько progression layers на main boss отвергнуты: native encounter плюс максимум один JEM layer.

