# Предложения жителей без цены — 2026-10-21

Запрошено пользователем: выявить товары, из-за которых нативная торговля показывает красный барьер. Цены в этой работе не назначаются.

Основание: существующий экспорт загруженных native provider pools от `2026-10-20T18:27:03.876130700Z`, 643 строк; SHA-256 `0e3bedb35229babdfb51d219f3a7567db7e5ac32f8ecb08a8c466672a3f5cd17`. Старые цены из экспорта не используются: сопоставление сделано с текущими canonical `item_policies.json`, `native_item_prices.json`, `profession_pools.json` и `TradeRestrictions`.

В доступных образцах: **128 разных товаров без цены**, 165 строк таких предложений; ещё 18 строк имеют цену только для другого направления. 0 строк отнесены к явным постоянным запретам, 20 provider-строк остались без образца.

Полный построчный список с профессией, уровнем, количеством, NBT и причиной: [CSV](native-offer-price-gaps-2026-10-21.csv). `buy` означает покупку игроком у жителя, `sell` — продажу игроком жителю.

## Лучник на приложенном скриншоте

В экспортированном пуле лучника первого уровня есть `xtraarrows:iron_arrow`: житель принимает 2 железные стрелы за валюту, а также продаёт 5 железных стрел. Для этого ID нет цены ни в canonical commodity policy, ни в native merchandise definitions. `NativeTradePricing` возвращает `UNPRICED`, а `MerchantScreenMixin` рисует барьер. Обычные `minecraft:arrow` и `minecraft:tipped_arrow` уже имеют native prices; это другие ID. Совпадение строки на скриншоте с пулом — вывод по количеству и виду, без чтения NBT конкретного жителя.

## Без цены по установленным образцам

| Товар | Кто предлагает | Направление |
| --- | --- | --- |
| `alexsmobs:acacia_blossom` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:ambergris` | minecraft:fisherman L2 | sell |
| `alexsmobs:animal_dictionary` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:banana_peel` | minecraft:wandering_trader L2 | buy |
| `alexsmobs:bear_fur` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:blobfish_bucket` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:blood_sac` | minecraft:wandering_trader L2 | buy |
| `alexsmobs:cockroach_ootheca` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:crocodile_egg` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:crocodile_scute` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:mosquito_larva` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:roadrunner_feather` | minecraft:wandering_trader L1 | buy |
| `alexsmobs:sombrero` | minecraft:wandering_trader L2 | buy |
| `beachparty:beach_ball` | beachparty:sandymerchant L4 | buy |
| `beachparty:beach_goal` | beachparty:sandymerchant L4 | buy |
| `beachparty:beach_hat` | beachparty:sandymerchant L5 | buy |
| `beachparty:beach_parasol` | beachparty:sandymerchant L3 | buy |
| `beachparty:beach_towel` | beachparty:sandymerchant L3 | buy |
| `beachparty:bikini` | beachparty:sandymerchant L5 | buy |
| `beachparty:floaty_boat` | beachparty:sandymerchant L5 | buy |
| `beachparty:message_in_a_bottle` | beachparty:sandymerchant L5 | buy |
| `beachparty:mini_fridge` | beachparty:sandymerchant L5 | buy |
| `beachparty:palm_leaves` | beachparty:sandymerchant L1 | buy |
| `beachparty:palm_torch_item` | beachparty:sandymerchant L1 | buy |
| `beachparty:pool_noodle` | beachparty:sandymerchant L2 | buy |
| `beachparty:radio` | beachparty:sandymerchant L4 | buy |
| `beachparty:rubber_ring_blue` | beachparty:sandymerchant L5 | buy |
| `beachparty:rubber_ring_pink` | beachparty:sandymerchant L5 | buy |
| `beachparty:sand_bucket_filled` | beachparty:sandymerchant L3 | buy |
| `beachparty:sandwaves` | beachparty:sandymerchant L2 | buy |
| `beachparty:thatch` | beachparty:sandymerchant L2 | buy |
| `beachparty:trunks` | beachparty:sandymerchant L5 | buy |
| `dustydecorations:anchor` | dustydecorations:antique_dealer L4 | buy |
| `dustydecorations:big_megalodon_tooth` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:big_nautilus_shell` | dustydecorations:antique_dealer L4 | sell |
| `dustydecorations:big_shark_jaws` | dustydecorations:antique_dealer L5, minecraft:wandering_trader L1 | buy |
| `dustydecorations:books` | dustydecorations:antique_dealer L1 | buy |
| `dustydecorations:bookstack` | dustydecorations:antique_dealer L1 | buy |
| `dustydecorations:camera` | dustydecorations:antique_dealer L5 | buy |
| `dustydecorations:ceramic_pot` | dustydecorations:antique_dealer L2 | buy |
| `dustydecorations:cerulean_seaglass` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:cerulean_seaglass_fragments` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:cluttered_shelf` | dustydecorations:antique_dealer L1 | buy |
| `dustydecorations:cow_skull` | dustydecorations:antique_dealer L4, minecraft:wandering_trader L1 | buy, sell |
| `dustydecorations:displayed_oars` | dustydecorations:antique_dealer L1 | buy |
| `dustydecorations:embedded_fish_fossil` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:equine_skull` | dustydecorations:antique_dealer L4, minecraft:wandering_trader L1 | buy, sell |
| `dustydecorations:fishing_lures` | dustydecorations:antique_dealer L2 | buy |
| `dustydecorations:glass_buoy` | dustydecorations:antique_dealer L5 | buy |
| `dustydecorations:glazed_ceramic_pot` | dustydecorations:antique_dealer L2 | buy |
| `dustydecorations:gold_coins` | dustydecorations:antique_dealer L1, dustydecorations:antique_dealer L3 | buy, sell |
| `dustydecorations:hanging_knives` | dustydecorations:antique_dealer L2 | buy |
| `dustydecorations:hazel_seaglass` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:hazel_seaglass_fragments` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:honey_seaglass` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:honey_seaglass_fragments` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:jars` | dustydecorations:antique_dealer L1 | buy |
| `dustydecorations:life_preserver` | dustydecorations:antique_dealer L5 | buy |
| `dustydecorations:mint_seaglass` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:mint_seaglass_fragments` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:mocha_seaglass` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:mocha_seaglass_fragments` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:monster_wallpaper` | dustydecorations:antique_dealer L5, minecraft:wandering_trader L1 | buy |
| `dustydecorations:portable_camera` | dustydecorations:antique_dealer L2 | buy |
| `dustydecorations:poster_decor` | dustydecorations:antique_dealer L2 | buy |
| `dustydecorations:regal_wallpaper` | dustydecorations:antique_dealer L5, minecraft:wandering_trader L1 | buy |
| `dustydecorations:sailor_wallpaper` | dustydecorations:antique_dealer L5, minecraft:wandering_trader L1 | buy |
| `dustydecorations:scarlet_seaglass_fragments` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:scarlett_seaglass` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:small_bookshelf` | dustydecorations:antique_dealer L2 | buy |
| `dustydecorations:small_glass_buoys` | dustydecorations:antique_dealer L5 | buy |
| `dustydecorations:sunflower_wallpaper` | dustydecorations:antique_dealer L5, minecraft:wandering_trader L1 | buy |
| `dustydecorations:taupe_seaglass` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:taupe_seaglass_fragments` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:teal_seaglass` | minecraft:wandering_trader L1 | buy |
| `dustydecorations:teal_seaglass_fragments` | dustydecorations:antique_dealer L1 | sell |
| `dustydecorations:treasure_map` | dustydecorations:antique_dealer L4 | buy |
| `dustydecorations:vine_wallpaper` | dustydecorations:antique_dealer L5, minecraft:wandering_trader L1 | buy |
| `dustydecorations:wolf_skull` | dustydecorations:antique_dealer L4 | buy, sell |
| `dustydecorations:wooden_buoy` | dustydecorations:antique_dealer L3 | buy |
| `farm_and_charm:compost` | minecraft:farmer L4 | buy |
| `farm_and_charm:dungarees` | minecraft:farmer L1 | buy |
| `farm_and_charm:fertilizer` | minecraft:farmer L5 | buy |
| `farm_and_charm:pitchfork` | minecraft:farmer L1 | buy |
| `irons_spellbooks:common_ink` | minecraft:wandering_trader L1 | buy, sell |
| `irons_spellbooks:epic_ink` | minecraft:wandering_trader L1 | buy, sell |
| `irons_spellbooks:legendary_ink` | minecraft:wandering_trader L1 | buy, sell |
| `irons_spellbooks:rare_ink` | minecraft:wandering_trader L1 | buy, sell |
| `irons_spellbooks:uncommon_ink` | minecraft:wandering_trader L1 | buy, sell |
| `kaleidoscope_cookery:iron_kitchen_knife` | kaleidoscope_cookery:chef L2 | buy |
| `kaleidoscope_cookery:kitchen_shovel` | kaleidoscope_cookery:chef L2 | buy |
| `kaleidoscope_cookery:recipe_item` | kaleidoscope_cookery:chef L2, kaleidoscope_cookery:chef L3, kaleidoscope_cookery:chef L4 | buy |
| `kaleidoscope_cookery:stockpot_lid` | kaleidoscope_cookery:chef L2 | buy |
| `minecraft:firefly_bush` | minecraft:wandering_trader L1 | buy |
| `minecraft:goat_horn` | dustydecorations:antique_dealer L1 | sell |
| `minecraft:open_eyeblossom` | minecraft:wandering_trader L1 | buy |
| `minecraft:pale_hanging_moss` | minecraft:wandering_trader L1 | buy |
| `minecraft:pale_moss_block` | minecraft:wandering_trader L1 | buy |
| `minecraft:pale_oak_log` | minecraft:wandering_trader L2 | buy |
| `minecraft:pale_oak_sapling` | minecraft:wandering_trader L1 | buy |
| `minecraft:prismarine_shard` | dustydecorations:antique_dealer L1 | sell |
| `minecraft:spyglass` | dustydecorations:antique_dealer L1 | sell |
| `minecraft:sulfur_spike` | minecraft:wandering_trader L1 | buy |
| `minecraft:tall_dry_grass` | minecraft:wandering_trader L1 | buy |
| `minecraft:totem_of_undying` | dustydecorations:antique_dealer L5 | buy |
| `minecraft:wildflowers` | minecraft:wandering_trader L1 | buy |
| `natures_spirit:anemone` | minecraft:wandering_trader L1 | buy |
| `natures_spirit:black_iris` | minecraft:wandering_trader L1 | buy |
| `natures_spirit:blue_iris` | minecraft:wandering_trader L1 | buy |
| `natures_spirit:hibiscus` | minecraft:wandering_trader L1 | buy |
| `natures_spirit:lotus_flower` | minecraft:wandering_trader L1 | buy |
| `natures_spirit:red_moss_block` | minecraft:wandering_trader L1 | buy |
| `primal:cold_seashells` | minecraft:wandering_trader L1 | buy |
| `primal:crocodile_scute` | minecraft:wandering_trader L1 | buy |
| `primal:seashells` | minecraft:wandering_trader L1 | buy |
| `primal:temperate_seashells` | minecraft:wandering_trader L1 | buy |
| `vinery:basket` | vinery:winemaker L4 | buy |
| `vinery:dark_cherry_beam` | vinery:winemaker L4 | buy |
| `vinery:flower_box` | vinery:winemaker L3 | buy |
| `vinery:flower_pot_big` | vinery:winemaker L4 | buy |
| `vinery:grass_slab` | vinery:wandering_winemaker L1 | buy |
| `vinery:window` | vinery:winemaker L4 | buy |
| `vinery:wine_box` | vinery:winemaker L5 | buy |
| `xtraarrows:diamond_arrow` | minecraft:fletcher L3 | buy, sell |
| `xtraarrows:golden_arrow` | minecraft:fletcher L2 | buy, sell |
| `xtraarrows:iron_arrow` | minecraft:fletcher L1 | buy, sell |
| `xtraarrows:netherite_arrow` | minecraft:fletcher L4 | buy, sell |
| `xtraarrows:netherite_explosive_arrow` | minecraft:fletcher L5 | buy, sell |

## Цена есть, направление не разрешено

Это отдельные ограничения текущей policy, а не пропущенные числовые значения. Не открывать buyback или NPC-продажу автоматически.

| Товар | Направление | Кто предлагает |
| --- | --- | --- |
| `beachparty:coconut_cocktail` | buy | beachparty:sandymerchant |
| `beachparty:coconut_open` | buy | beachparty:sandymerchant |
| `farm_and_charm:farmer_salad` | buy | minecraft:farmer |
| `minecraft:andesite` | sell | minecraft:mason |
| `minecraft:black_wool` | sell | minecraft:shepherd |
| `minecraft:brown_wool` | sell | minecraft:shepherd |
| `minecraft:cookie` | buy | minecraft:farmer |
| `minecraft:diorite` | sell | minecraft:mason |
| `minecraft:golden_carrot` | buy | minecraft:farmer |
| `minecraft:granite` | sell | minecraft:mason |
| `minecraft:gray_wool` | sell | minecraft:shepherd |
| `minecraft:pumpkin_pie` | buy | minecraft:farmer |
| `minecraft:quartz_block` | buy | minecraft:mason |
| `minecraft:rabbit_stew` | buy | minecraft:butcher |
| `minecraft:stone` | sell | minecraft:mason |
| `minecraft:white_wool` | sell | minecraft:shepherd |
| `vinery:cherry_wine` | buy | vinery:wandering_winemaker |
| `vinery:lilitu_wine` | buy | vinery:winemaker |

## Границы доказательства

Это аудит существующего сохранённого пула, не новый запуск игры и не полный перебор случайных NBT, зачарований, эффектов, карт и всех возможных callbacks. Проверка нового runtime не запускалась. Forge события могли изменить набор после снимка; названия и наличие здесь взяты из того снимка. Непроверенные world-dependent providers ниже не выданы за покрытые.

Изменившиеся/удалённые outer JAR относительно снимка: `jem-bootstrap.jar`, `jem-economy-1.3.0.jar`, `jem-pack-core-2.0.0-dev.jar`, `jem-races-1.1.0.jar`, `jem-roles-2.0.0.jar`, `jem-server-2.0.0-dev.jar`, `jem-settlements-2.1.0.jar`, `jem-world-boss-tiers-2.5.0-dev.jar`, `jemcompat-3.7.0.jar`, `jem_twelve_eyes-1.7.0-dev.jar`, `servercore-forge-1.5.2+1.20.1.jar`, `serverlogin-1.0.3.jar`. Общие JEM-адаптеры изменились; списки выше — сверка сохранённых предложений с текущими исходниками, не актуальный runtime-export.

| Профессия | Уровень | Provider | Причина отсутствия образца |
| --- | --- | --- | --- |
| `minecraft:cleric` | 5 | `com.teamremastered.endrem.events.ModEvents$EREyeTrade` | not invoked: world-dependent or optional provider |
| `minecraft:cartographer` | 2 | `net.minecraft.world.entity.npc.VillagerTrades$TreasureMapForEmeralds` | not invoked: world-dependent or optional provider |
| `minecraft:cartographer` | 2 | `com.github.alexmodguy.alexscaves.server.entity.util.VillagerUndergroundCabinMapTrade` | not invoked: world-dependent or optional provider |
| `minecraft:cartographer` | 3 | `net.minecraft.world.entity.npc.VillagerTrades$TreasureMapForEmeralds` | not invoked: world-dependent or optional provider |
| `minecraft:cartographer` | 3 | `net.salju.trialstowers.events.TrialsVillagerManager$TreasureMapForEmeralds` | not invoked: world-dependent or optional provider |
| `kaleidoscope_cookery:chef` | 5 | `com.github.ysbbbbbb.kaleidoscopecookery.event.ModTradesEvent$EnchantedItemForEmeralds` | not invoked: world-dependent or optional provider |
| `minecraft:fisherman` | 4 | `com.li64.tide.loaders.forge.ForgeEventHandler$$Lambda$46016/0x000001cdd92093e8` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 1 | `com.github.alexmodguy.alexscaves.server.entity.util.VillagerUndergroundCabinMapTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 1 | `net.mehvahdjukaar.moonlight.api.misc.ModItemListing` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 1 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$RandomScrollTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 1 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$RandomScrollTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 1 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$RandomScrollTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 1 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$RandomCurioTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 2 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$SimpleTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 2 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$SimpleTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 2 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$RandomCurioTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 2 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$RandomCurioTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 2 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$RandomCurioTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 2 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$ScrollPouchTrade` | not invoked: world-dependent or optional provider |
| `minecraft:wandering_trader` | 2 | `io.redspace.ironsspellbooks.player.AdditionalWanderingTrades$ScrollPouchTrade` | not invoked: world-dependent or optional provider |

Постоянный запрет, отсутствие identity, замороженный ledger, исчерпанные запасы, деньги игрока/резерва и ограничения продажи — самостоятельные причины недоступности. Они не устраняются назначением произвольной цены.

Кожа: текущая покупка 12 медных (1 серебро + 2 меди), продажа 3 медных. Запрос сохранить цену покупки выполнен без изменения прибыли от продажи.
