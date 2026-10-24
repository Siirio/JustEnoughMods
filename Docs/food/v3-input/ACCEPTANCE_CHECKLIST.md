# JEMFood v3 — обязательные проверки до релиза

Поля должны быть заполнены реальным доказательством (команда, версия, log/screenshot, duration, exit code), а не утверждением "проверено".

| ID | Gate | Что считается доказательством |
|---|---|---|
| DATA-01 | JSON и именование | `python validate_plan.py`: 848 exact IDs, категории 320/266/213/49, 30 RU/EN traits ≤2 words, 517 bless/11 native, 569 variety |
| DATA-02 | Уникальность | 1 trait Tasty, 2 Exquisite, 3 Masterwork; no trait repeated per dish, each reference exists; original 848 ID set identical |
| DATA-03 | Ручной аудит | Проверка реальных recipes/loot/obtainability для 49 Masterwork и выборки 50 exquisite, плюс аномалии среди 320 Ordinary; если mismatch — исправить с записью в Atlas, quota lock |
| ARCH-01 | Самостоятельность | Новый `Mods/JEMFood` собирается без любых сторонних JAR, кроме Forge; `mods.toml` hard requirements только Forge/Minecraft; dedicated+integrated Forge-only launch |
| ARCH-02 | Отсутствие дубликатов | Runtime `JEMCompat` не слушает food events; присутствие обоих JAR не выдаёт двойные bonuses; vanilla-only JEMFood работает без JEMCompat |
| STATUS-01 | Единый статус | `ForgeRegistries.MOB_EFFECTS` содержит один JEMFood MobEffect; одна иконка видна в inventory, реальное hover показывает имена/описания каждого trait |
| STATUS-02 | RU/EN | `ru_ru`, `en_us` без ключей-сырых ID, нет длинных имён, класс окрашен, корректный timer и native_only tooltip |
| TRAIT-01 | Все 30 реализованы | Каждый trait покрыт targetted unit/integration test, доказаны exact I/II/III coefficients, cooldown/status persistence; нет немых noop |
| BRIDGE-01 | Effect missing fallback | Forge+FoodMod без EffectMod: trait выдаёт точно `minecraft:speed`/`dolphins_grace`/`resistance` в зависимости от ID и остаётся одна видимая Food Blessing иконка |
| BRIDGE-02 | Effect present | FoodMod+EffectMod: нужный MobEffect распознан только при verified safe semantics; иначе explicit fallback; все скрытые JEM-only эффекты не скрывают external и не накладываются поверх same ID внешних |
| BRIDGE-03 | Native effect independently present | Попробовать native potion перед едой; после еды, milk, смерти, замены JEM нельзя портить duration/amplifier и чужую hidden-chain |
| EAT-01 | Еда из item | `LivingEntityUseItemEvent.Finish` выдаёт один набор, сохранён native hunger/saturation/container/food; failed/cancelled/creative не награждает |
| EAT-02 | Placed cake | Съеден реальный кусок торта → 1 award; right click в состоянии без возможности съесть, client prediction и правый клик на чужой блок → 0 |
| EAT-03 | Mod placed foods | Все 21 extra block routes проверены на **точных** активных JAR, без тп/подмены чужой логики; если пути не поддерживаются, не объявлять total acceptance |
| COMBAT-01 | Баланс | Стресс с 3 combatted traits, cap +7% outgoing, cap 10% physical and 30% environmental incoming, no PvP buff, no boss immunity bypass, boss fight JEM smoke |
| UTIL-01 | Рабочие действия | XP catches/crops/mob kills only on legit success; one bonus on success, no dupe via drop, repeat crafting, server reconnect |
| STATE-01 | Замена | Новое блюдо заменяет старый JEM blessing, vanilla food его не очищает, native-only food clears own, no stale attributes |
| STATE-02 | Lifecycle | Logout, clone, death, restart, milk, `/effect clear`, recipe eating in another dimension; no duplicate buffs, persisted variety/cooldowns |
| STATE-03 | Migration | Old `JemFoodQualityV1` world loaded without native effect loss/dupe; food-only old JEMCompat class no longer owns food; idempotent new schema migration |
| BUILD-01 | Build + evidence | Gradle jar/reobfJar, unit/gameplay tests, Atlas rebuild/audit/scan/validate, exact JAR/hash and log |
| DEPLOY-01 | Isolated publish | test jem generation/manifest verifies JEMFood present and JEMCompat changed, original/MineRENT unchanged |

**DoD**: все обязательные проверки `PASS` с logs. Непроверенные пункты — `BLOCKED / NOT VERIFIED`, а не `PASS`. Не менять production ради выдачи галочки.
