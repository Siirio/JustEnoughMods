# Союзники, призывы и войска — Phase 2

`Catalog/views/power/allies.json` содержит кандидатов Recruits, Workers, Companions, summons, tameables, golems и mounts.

Для каждого кандидата Phase 2 пытается связать exact-version entity owner class и фиксирует те же combat-channel signals, что и для босса. Отдельно остаются unresolved ownership/tameability, AI uptime, target access, owner scaling, survival, congestion, acquisition и payroll/upkeep.

JEM Settlements не получает права реконструировать нативные combat stats Recruits/Workers: он владеет roster/payroll/morale и переводит нативные сигналы. Сила армии не считается как `один юнит × количество`, пока не доказаны реальный uptime, доступ к цели и взаимодействие разных типов войск.
