# Settlements2.1.0 delivery

Use the **full** `Mods/JEMSettlements/build/libs/jem-settlements-2.1.0.jar`, not the slim JAR. It embeds the single CurrencyCore and Identity dependencies. For a clean supported Forge1.20.1/Java17 installation, full Economy, JEMCompat and JEMServer are not prerequisites. Add Workers/Recruits only to enable their own native entities and controls; their external dependencies still follow those mods' requirements. No built-in replacement NPCs were created.

Exact production artifacts, SHA256 values, embedded hashes and synchronization/publication evidence: [delivery.json](delivery.json). Keep Economy1.3.0 and Settlements2.1.0 together in the full JEM pack; an old Economy must not coexist with the new shared Core.

## Review in the game

No gameplay client or automated preview fixture was launched. To review real state manually: install the full standalone JAR in your chosen supported instance, open a world, use `/settlements` or configure the Settlements key, and place a real bell to found a settlement at the actual quoted fee. More than one owned settlement opens the selector. Native funded villager trades provide the established Coin earning path; the implementation adds no creative-only grants or new currency recipes.

Use Map for filled territory, full-strip preview and1Gold per added block; Residents for living UUIDs, actual bars and details; Food for classified effects, fatigue and real linked-container stock; Treasury for real ledger amounts/receipts; Settings for rename/deletion confirmation. Shift+bell retains registration/linking interactions. Shift+Settlements key focuses a looked-at registered NPC after authorized selection. Optional Manage opens the actual nearby native NPC menu. `/settlements help` opens15 localized guide chapters. All gameplay mutations await server acknowledgment.

Native Minecraft controls, canonical coin/item artwork and programmatic pixel surfaces are integrated. The control-family candidate has editable ORA source and exact PNG exports; it remains unapproved and unintegrated. The remaining custom sprites/guide illustrations are unproduced. This build is not a claim that the final visual acceptance criteria have passed.

## Established startup cause chain

Bug / Both Sided / Java module split packages after shared currency extraction / in testing.

1. The first ordinary dedicated launch stopped before world opening with `ResolutionException` naming Economy/Core exports of `com.siirio.jemeconomy.client`.
2. Both JARs retained different classes in the same Java package; `market` had the same ownership problem.
3. Extracting individual financial classes preserved binary package names but left full-only shipping/market presentation in those packages.
4. Forge's Java17 module layer checks package ownership before mod initialization; distinct class filenames do not make a split package legal.
5. The source extraction boundary lacked complete package ownership. Full-only client/market/audit classes now belong to `com.siirio.jemeconomy.full.*`, and their actual callers were updated. Shared API/ledger packages remain solely Core-owned. Final ordinary dedicated startup reached Done; user gameplay acceptance remains pending.

## Established Atlas discovery cause chain

Bug / Both Sided / Atlas source/model candidates promoted without registration evidence / in testing.

1. Final catalog review found `jem_currency_core:all`, `jem_economy:coin_base` and `jem_settlements:kaleidoscope_nether` among item identities.
2. `all` and the Nether mod namespace came from ordinary `Set.of(...)` application constants, while `coin_base` is an unbound parent render model.
3. The source scanner treated any file mentioning the item registry as a declaration owner and interpreted unrelated literal sets as registrations; the asset scanner treated the parent model filename as an item.
4. File-level proximity and an asset path were insufficient evidence for Forge registration, but the candidate promotion path did not distinguish them.
5. The generator now requires an actual DeferredRegister owner for literal registration discovery and excludes unbound parent-model templates as standalone item evidence. Dynamic registration gaps remain explicit; the five real Coin registrations and their original IDs are retained. Exact task-created false partitions are removed without deleting unrelated catalog work.

## Established tag-kind cause chain

Bug / Both Sided / Atlas matched memberships by bare ID across different registry kinds / in testing.

1. Ingredient item tags appeared on same-named block rows and the egg entity row.
2. Static enrichment searched every tag value set for the same namespaced ID.
3. Minecraft permits matching IDs in separate registries, so string equality cannot establish registry identity.
4. The tag-folder kind was recorded in the graph key but omitted from the membership predicate.
5. The generator now matches the tag-folder kind to the owning registry before deriving row memberships. Actual tag files remain raw evidence; cooking item tags do not grant block/entity membership.

## Known limits

Interrupted financial or physical-container commits are explicitly uncertain/quarantined rather than automatically charged/replayed. An operator must reconcile an interrupted intent against actual retained receipt, town and inventory state; there is no universal atomic transaction across third-party capabilities and separate save files. Known committed request results replay without new charges, and ordinary unavailable native-menu actions are terminal rejections rather than artificial financial uncertainty.

The built-in map renders real territories over parchment/unknown terrain and does not invent explored terrain or reveal personal Xaero points. Existing native Xaero remains optional and retains its controls. Clean-instance singleplayer, absence of each optional mod, all GUI scales, gameplay persistence/rebellion and final visual craft still require actual acceptance. Source/build/ordinary-server evidence is separated from those checks in TEST_MATRIX.

Control-family review candidate: [native1x sheet](../assets/review/control-family-r1/dark-native1x.png), [nearest4x sheet](../assets/review/control-family-r1/light-nearest4x.png), [editable source/manifest](../assets/source/control-family-r1/README.md). Eight original exports cover panel, four button states, tabs and two bars. The offline reference sheet is art-only, has no gameplay transport or world mutation, and is not an in-game screenshot. `CANDIDATE_NOT_APPROVED / SOURCE_EDITABLE / WAITING_FOR_REAL_GAME_CAPTURE`; no JAR/resource integration.
