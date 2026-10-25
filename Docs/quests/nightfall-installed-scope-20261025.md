# Installed Nightfall scope, 2026-10-25

The user's requested Nightfall fight coverage must use actual installed mechanics. The inspected release supplies weapon combat and allied summons, not genuine Nightfall boss encounters. Codex therefore rejected invented Guardian/Doppelganger kill objectives. This is installed-static evidence, not gameplay acceptance or a new canonical registry.

Artifact: [EpicFight_20Nightfall-3.4.1-all.jar](../../Server/mods/EpicFight_20Nightfall-3.4.1-all.jar), `META-INF/mods.toml` declares `modId="efn"`, `version="3.4.1"`

SHA256: `cd778690df2aad55f2025351a349f599d0c98830eb8644524813c86a39a05b7a`

The pre-existing [research profile](../../Catalog/research/web-mechanics/efn.json) describes weapon mechanics and explicitly qualifies newer author-page material. Atlas `behavior efn:guardian` and `behavior efn:doppelganger` returned no records. The installed registration and lifecycle bytecode below establishes their role despite that catalog gap.

## Registration and lifecycle evidence

`com.hm.efn.entity.EFNEntity` declares 24 registered entity types. Its registration-field inventory is:

```text
FIREWIND, SUMMONED_SWORD, SIN_SUMMONED_SWORD, BLAST_SUMMONED_SWORD,
HEAVY_RAIN_SUMMONED_SWORD, DAMOCLES_SWORD, SUMMONED_SWORD_IN,
SUMMONED_SWORD_OUT, SECLUDED, CO_TACHI_SLASH, TRAIL, SOUL_HUNT_ORB,
FALCHION_SKILL_AREA, DOPPELGANGER, GUARDIAN, MURASAMA_SLASH,
MURASAMA_CHARGING, HF_BLADE_SLASH, HF_BLADE_CHARGING,
JUDGEMENTCUT_NORMAL, JUDGEMENTCUT_PERFECT, SOULHUNT_LIGHTNING,
EXCALIBUR, CRIMSON_SLASH
```

The remaining types are effect, projectile, particle or area entities. The apparent combat-mob candidates are owner-bound effects:

| Bytecode locator inside the linked artifact | Established behavior |
|---|---|
| `com.hm.efn.entity.EFNEntity.<clinit>`, `lambda$static$2`, `lambda$static$3` | Registers `efn:doppelganger` and `efn:guardian`, their `MONSTER` category does not establish hostile encounter semantics |
| `com.hm.efn.entity.falchion.GuardianEntity.<init>(ServerPlayer)`, `m_21828_(Player)` | Extends `TamableAnimal`, assigns the player as owner and equips the native `FLAG_BEARER` weapon |
| `GuardianEntity.m_6469_(DamageSource,float)`, `m_8119_()` | Damage always returns false, the ordinary summon is discarded after 30 seconds |
| `com.hm.efn.gameasset.animations.EFNFalchionAnimations.lambda$build$15` | A player weapon-animation callback creates two owned Guardians beside the player |
| `com.hm.efn.entity.doppelganger.DoppelgangerEntity.<init>(ServerPlayer)`, `summon(ServerPlayerPatch,boolean)` | Creates an owned Yamato companion beside the player or behind the player's living target |
| `DoppelgangerEntity.m_6469_(DamageSource,float)`, `m_8119_()` | Damage always returns false, removes the companion when its owner is absent/dead or stops holding Yamato |
| `com.hm.efn.skill.weapon_innate.YamatoInnate.handleServerDoppelganger` | Native skill state, enchantment and stack gates control summon creation, this is an ability rather than a boss challenge |
| `com.hm.efn.entity.effect.SinSummonedSwordEntity.SinSummonedSwordEntity` | Extends `Mob` but stores owner identity and finite `lifetimeTicks`, damage always returns false and expiry discards it |

The JAR contains no `data/efn` worldgen, encounter structures or entity loot tables. The [installed common configuration](../../Server/config/efn-common.toml) controls weapon abilities and rewards, it does not provide a Guardian/Doppelganger boss encounter. No genuine challenge-boss contract was found among these 24 registrations.

The class and method locators above can be read without launching Minecraft, for example from the repository root:

```powershell
javap -classpath Server/mods/EpicFight_20Nightfall-3.4.1-all.jar -p -c com.hm.efn.entity.falchion.GuardianEntity
```

## Native acquisition signals

All 12 bundled `data/efn/advancements/<path>.json` files use criterion `obtain` with `minecraft:inventory_changed` for the matching `efn:<path>` item. Their exact advancement IDs are:

```text
efn:air_tachi
efn:co_tachi
efn:exsiliumgladius
efn:fire_exsiliumgladius
efn:hf_blade
efn:hf_murasama
efn:meen_spear
efn:nf_dual_sword
efn:ruinsgreatsword
efn:sword_of_pioneer
efn:yamato_dmc4_in_sheath
efn:yamato_dmc_in_sheath
```

These signals prove weapon acquisition only. They must not be described as boss defeats or successful execution of a weapon ability. `efn:yamato_dmc_in_sheath`, `efn:ruinsgreatsword` and `efn:air_tachi` are supported native item-icon choices for corresponding acquisition milestones.

## Installed FTB presentation contract

For [FTB Quests 2001.4.22](../../Server/mods/ftb-quests-forge-2001.4.22.jar) with [Library 2001.2.13](../../Server/mods/ftb-library-forge-2001.2.13.jar), `QuestScreen.handleClick(String,String)` opens native chapter/quest IDs through an image `click: "#<hex ID>"`. There is no native `change_page` handler. `QuestScreen.onBack()` closes quest detail or exits the screen, it does not maintain chapter history. Native reopening preserves chapter, zoom and scroll.

The exact public parser is `net.minecraft.network.chat.Component dev.ftb.mods.ftbquests.util.TextUtils.parseRawText(String)`. Quest/chapter/group/reward-table titles, quest subtitle/description and chapter subtitle use it. Image hover strings instead pass to `TooltipList.translate`, so they take plain language keys. `ChapterImage.readData` reads `color` as integer RGB and `alpha` separately, while `image` may itself be a native color icon string. The scoped [JEM image adapter](../../Mods/JEMAchievementGuide/src/main/java/com/justenoughmods/achievementguide/mixin/QuestChapterImageMixin.java) owns authored caption extensions.

Evidence collection used archive/resource reads, Atlas/research queries and bytecode inspection. No builds, automated tests, gameplay scenarios or game launches were performed.
