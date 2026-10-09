# Boss Fight

## Primary state

Boss Fight — host-controlled encounter для одного зарегистрированного босса. Все структурные боссы, Campaign и Raid используют один `SOLID` boundary owner: четыре невидимые full-height collision planes участвуют в обычном Minecraft movement resolution и совпадают с видимыми стенами. До Start граница закрыта; после Start её проходят только host и accepted non-eliminated participants. Outsiders, mobs и projectiles остаются заблокированы без tick teleport, velocity reversal, physical shell blocks или повторного `setPos`.

Boss HP и damage увеличиваются на 20% за каждого дополнительного присутствующего участника. После этого к итоговому damage hosted boss применяется только один correction: 12% reduction для direct, projectile, effect и hardcoded damage. Gear score не участвует.

Помеченные тематические bosses, включая Magnetron, Scylla и Cloud Golem, используют общий эффект «Любимчик Зевса»: раз в семь секунд позиция одного живого участника фиксируется компактным электрическим телеграфом на полсекунды, затем туда ударяет boss-owned молния. Игрок может увернуться, покинув отмеченную область до impact.

Start требует уже существующего живого boss identity и никогда не создаёт или не перемещает босса. Игроки перемещаются к проверенной позиции снаружи его реальной структуры. Endpoint policy до телепорта проверяет полный player bounding box, dimension, build height, world collision и virtual wall; небезопасная точка один раз заменяется проверенной внешней позицией без roof fallback. После завершения участники возвращаются наружу через общий safe-exit flow. Native awakening, entity UUID, здоровье, AI и исходная структура сохраняются.

## Arena

Операторский revive является восстановлением структуры, а не rematch unlock: зарегистрированной структуры и отсутствия живого босса достаточно даже до первой победы. Устаревший UUID отсутствующей entity заменяется новым, первая последующая легитимная победа сохраняет право прогрессировать World Tier, а Raid остаётся закрытым до этой победы.

Campaign не создаёт отдельную shell: canonical prerequisite state только запрещает Start и показывает полный список недостающих боссов, а физику всегда предоставляет общий `SOLID` owner. Старые `jem_server:structure_barrier` и `jem_twelve_eyes:locked_boss_barrier` сохраняют registry ID как инертную migration-совместимость и удаляются только из известных arena shell при загрузке чанка.

## Rejected

- Start только изнутри арены отвергнут: закрытая shell делала вход невозможным.
- Глобальная отмена teleport commands/items отвергнута: boundary lifecycle владеет возвратом и явным выходом из боя.
- Gear-based scaling отвергнут пользователем.
- Любой solid collision block можно поставить прямо вместо невидимой ячейки structure shell; не-блоки и blocks без collision стену не заменяют.
- Несколько progression layers на main boss отвергнуты: native encounter плюс максимум один JEM layer.

