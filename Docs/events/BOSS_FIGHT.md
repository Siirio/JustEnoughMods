# Boss Fight

## Primary state

Boss Fight — host-controlled encounter для одного зарегистрированного босса. Все структурные боссы, Campaign и Raid используют один `SOLID` boundary owner: четыре невидимые full-height collision planes участвуют в обычном Minecraft movement resolution одновременно на dedicated server и клиенте и совпадают с видимыми стенами. До Start граница закрыта; после Start host и accepted non-eliminated participants могут войти, а после полного входа их collision box и ridden vehicle существующий arenaEntered снова закрывает ту же SOLID стену. Физика до входа и во время боя принадлежит общему с Blood Moon SolidBoundary и не зависит от касания края. Outsiders, mobs и projectiles остаются заблокированы без tick teleport, velocity reversal, physical shell blocks или повторного `setPos`.

Boss HP и damage увеличиваются на 20% за каждого дополнительного присутствующего участника. После этого к итоговому damage hosted boss применяется только один correction: 12% reduction для direct, projectile, effect и hardcoded damage. Gear score не участвует.

До canonical Start зарегистрированный босс в своей структуре находится в dormant lobby: AI и target остановлены, combat damage с участием босса в обе стороны отклоняется, а native `ServerBossEvent`, Bosses Rise music state и другие tracking-time presentation signals скрыты. Вход игрока в tracking range или саму структуру не пробуждает босса. Start одним переходом освобождает AI и native presentation; отмена или поражение живого босса возвращает его в dormant lobby.

Смерть участника является окончательной для текущей попытки: JEM помечает его eliminated, но не отменяет `LivingDeathEvent`, не выдаёт скрытые дополнительные жизни, половину здоровья или post-death immunity и не переносит игрока к боссу. При фактической смерти внутри активной структурной арены обычный inventory, armor, offhand и Curios сохраняются на respawn без grave; это не отменяет elimination и не действует вне активного Boss Fight/Boss Raid. Групповое downed/revive состояние может существовать только до фактической смерти через установленный revive lifecycle.

После `arenaEntered` active accepted non-eliminated participant и его ridden root vehicle не могут покинуть arena ни движением, ни `/tp`, TPA, pearl/fruit, waypoint teleport, portal/dimension transfer или modded reposition API. Проверяется source/destination dimension и полный collision box. Accepted participant до первого полного входа сохраняет штатный arrival access; completion, defeat, elimination, respawn recovery и operator emergency recovery используют существующие lifecycle states, а не общий публичный bypass. Outsider не может телепортироваться внутрь закрытой structure arena. Event-owned boss и его authored entities не могут телепортироваться или перейти dimension наружу.

Host завершает активный бой одной server-authoritative операцией по Party ID и текущему boss/attempt token. Показанная chat-кнопка не зависит от короткого proximity timeout; старый token не может отменить новую попытку той же Party. Успешная отмена проходит через canonical HostedEncounter close, после чего единым terminal flow снимаются AI/target ownership, collision, participant state и return session. Повторная или устаревшая команда ничего не меняет.

Помеченные тематические bosses, включая Magnetron, Scylla и Cloud Golem, используют общий эффект «Любимчик Зевса»: раз в семь секунд позиция одного живого участника фиксируется компактным электрическим телеграфом на полсекунды, затем туда ударяет boss-owned молния. Игрок может увернуться, покинув отмеченную область до impact.

Start требует уже существующего живого boss identity и никогда не создаёт или не перемещает босса. Игроки перемещаются к проверенной позиции снаружи его реальной структуры. Endpoint policy до телепорта проверяет полный player bounding box, dimension, build height, world collision и virtual wall; небезопасная точка один раз заменяется проверенной внешней позицией без roof fallback. После завершения участники возвращаются наружу через общий safe-exit flow. Native awakening, entity UUID, здоровье, AI и исходная структура сохраняются.

Verified native anchor записывается только при первом допустимом наблюдении native boss и после этого остаётся неизменным. Смерть, текущая позиция переместившегося босса, revive command и encounter cleanup не могут заменить его координатой смерти или передвинуть живую entity обратно к anchor.

Телепорт с Xaero доступен только для server-owned discovered boss marker со стабильным arena ID. Сервер заново разрешает ID через canonical registered arena и Campaign eligibility, игнорирует клиентские coordinates и ищет безопасную поверхность минимум за два block centers от outer collision shell. Personal/home/base/death и поддельные markers не получают JEM teleport action. Если безопасная загруженная точка с полным player box не найдена, переход отклоняется.

## Arena

Операторский revive является восстановлением структуры, а не rematch unlock: зарегистрированной структуры и отсутствия живого босса достаточно даже до первой победы. Устаревший UUID отсутствующей entity заменяется новым, первая последующая легитимная победа сохраняет право прогрессировать World Tier, а Raid остаётся закрытым до этой победы.

Campaign не создаёт отдельную shell: canonical prerequisite state только запрещает Start и показывает полный список недостающих боссов, а физику всегда предоставляет общий `SOLID` owner. Старые `jem_server:structure_barrier` и `jem_twelve_eyes:locked_boss_barrier` сохраняют registry ID как инертную migration-совместимость и удаляются только из известных arena shell при загрузке чанка.

## Rejected

- Start только изнутри арены отвергнут: закрытая shell делала вход невозможным.
- Generic mod-facing bypass отвергнут: boundary lifecycle сам снимает containment на завершении, defeat/elimination и recovery, а operator permission остаётся emergency route.
- Gear-based scaling отвергнут пользователем.
- Любой solid collision block можно поставить прямо вместо невидимой ячейки structure shell; не-блоки и blocks без collision стену не заменяют.
- Несколько progression layers на main boss отвергнуты: native encounter плюс максимум один JEM layer.


Защита лобби от урона определяется HostedBoundary.authorized: ACTIVE, accepted и non-eliminated. nativePending управляет только ожиданием native activation/spawn и снимается после bind босса; он не означает неуязвимость участника активного боя. Native Mob teleport endpoints используют ту же закрытую geometry и не позволяют боссу покинуть арену.
