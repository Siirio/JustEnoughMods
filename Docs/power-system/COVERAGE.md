# Coverage после Phase 2

Машинный источник — `Catalog/views/power/coverage.json`. Текущий очищенный combat inventory содержит 2 272 записи:

| View | Identified |
|---|---:|
| Реальные/подтверждённые weapon candidates | 87 |
| Armor/defense candidates | 216 |
| Registered accessory candidates | 52 |
| Enchantments | 218 |
| Iron's Spells spell IDs | 106 |
| Boss inventory | 61 |
| Other entity candidates | 1 429 |
| Allies/troops/summons/mount candidates | 103 |

Старый denominator 2 775 не переносится как будто он был точным: 379 weapon entries включали bowls, spawn eggs, language/model IDs и presentation variants. Phase 2 сохраняет 2 802 literal registration evidence records, затем применяет combat semantics и quarantine rules. Поэтому уменьшение списка — исправление единицы учёта, а не удаление игровых предметов.

## Native mechanics coverage

- 106/106 Iron's Spells IDs связаны с exact installed owner class.
- Создано 1 060 spell channel slots; 94 formula-bearing methods найдены, 77 выражений безопасно восстановлены простым symbolic bytecode evaluator.
- 61/61 bosses связаны с exact installed owner class или official-mapped Minecraft/Forge class.
- Создано 671 boss channel slots — 11 на каждого босса.
- 389 зарегистрированных weapon/armor/accessory candidates прошли item-class inspection; 350 получили matched specialized/shared owner class.
- 95/103 ally candidates получили matched exact-version owner class; восемь vanilla/dynamic candidates оставлены с конкретной причиной unresolved.

## Что остаётся unresolved

- helper-built registrations, которые не оставляют literal ID в item-registry class;
- ItemStack/NBT variants и runtime attribute modifiers;
- Epic Fight/Nightfall data-driven combo timing, stamina и animation multiplier для каждого предмета;
- сложные spell formulas, вынесенные в projectile/effect class и не входящие в безопасный symbolic subset;
- boss/ally arithmetic, делегированная goal/projectile/effect classes;
- runtime target mitigation, AI uptime, acquisition probability и конкретный наблюдавшийся beam ItemStack.

`not_observed` никогда не означает `0` или «механики нет». Это означает, что сигнал не найден в указанном owner-классе и следующий owner назван в `unresolved_reason`.
