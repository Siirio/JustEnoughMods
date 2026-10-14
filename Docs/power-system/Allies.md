# Союзники, призывы и войска — Stage 3

`Catalog/views/power/allies.json` содержит 103 кандидата: Recruits, Workers, companions, summons, tameables, golems и combat mounts. Exact-version owner class найден для 95; восемь записей остаются с именованным owner blocker.

## Числовой состав карточки

| Поле | Источник | Что считается доказанным |
|---|---|---|
| HP, armor, toughness, movement, attack damage, knockback | Native attribute-builder bytecode | Только literal/default, привязанный к конкретному attribute builder |
| Melee/projectile/effect/heal | Метод entity и ограниченная цепочка goal/projectile/effect | Sink и входы названы; итоговая формула числовая лишь при безопасно доказанном аргументе |
| Частота атаки | Goal scheduler/animation cooldown | Не выводится из одного `ATTACK_DAMAGE` |
| Дальность и uptime | Projectile/AI navigation/target access | Остаются unresolved, если цепочка не раскрывает точные значения |
| Экипировка и прочность | Реальный ItemStack/equipment path | Не наследуются от абстрактного типа юнита |
| Wage/upkeep | JEM Settlements | Экономический владелец; не является множителем native damage |

JEM Settlements сохраняет ownership roster/payroll/morale. Он не реконструирует native combat stats Recruits/Workers. Сила армии не равна `урон одного × число солдат`: congestion, pathfinding, range, target selection, survival и реальный attack uptime могут менять результат и перечислены как отдельные blockers.

Для summons отдельно требуется доказать spell level/caster scaling и lifetime; для tameables — owner buffs и условия приручения; для mounts — может ли всадник и само существо атаковать одновременно. До этого Stage 3 показывает source facts и method records, но не выдаёт их за готовый army DPS.
