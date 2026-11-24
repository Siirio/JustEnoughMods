# Changelog

## Unreleased - 2026-11-24

- Move Trees below Building and map its shared category kind to Building
- Move 554 exact reviewed Create, Infinity Buttons, Railways, Unearthed Journey, Kaleidoscope Cookery, Handcrafted, flower and component IDs to their semantic catalog leaves
- Keep crop-yielding wild plants in Food, Casual Swing in Furniture and Decorations, and Create machinery in Create and Redstone
- Render the curated overlay at the native container tooltip boundary so the screen's own tooltip always renders after catalog items, controls and search fields
- Render a curated-grid tooltip once after all catalog elements and immediately before the native screen tooltip
- Remove the custom all-language item and tooltip index, English alias injection and Creative search Mixin after they caused severe freezes while typing
- Keep native JEI and Creative search scoped to the active client language. Curated category search uses only the current localized item name and registry ID
- Restrict an existing Warden's vibration reactions to the same player attribution used by vanilla sculk shriekers, including player-owned projectiles such as snowballs, while ignoring ordinary mobs and mob projectiles
- Built the prepared 3.11.6 artifact with SHA-256 `1696f40909569e83d611a501e88a007b9bae1b300361d66e525c3d53ff14bb2d`

## Unreleased - 2026-11-23

- Replace the retired Components group with top-level Trees and Music Discs groups
- Rename Dolls to Cosmetics and classify verified decorative heads with dolls
- Reclassify the exact Archie's Dinosaurs, Royal Variations, building-tool, weapon, potion, light and wearable corrections from the reviewed manifest
- Preserve native Handcrafted hammer behavior when no JEM construction selection exists
- Yield the next ordinary right-click to a server-confirmed JEM construction selection

## 3.9.3 - 2026-11-01

- Promote `jem_building:builder_hammer` to the single player-facing hammer when JEM Building, Handcrafted and Building But Better are installed
- Reuse BBB's native `bbb:hammers` tag and Handcrafted's native `Hammerable.onHammer` contract without rewriting either mod's behavior
- Preserve Handcrafted-first interaction priority, then BBB native handling, then JEM Building construction fallback
- Retire both original hammer items from new acquisition and presentation while keeping their registered IDs loadable for compatibility
- Built the production compatibility artifact with SHA-256 `2cb4a3c2f33a9c0d1b12e1cb018dc25c5b53576af98e36c7be52a6e98816bfc6`

## 3.9.1 - 2026-10-29

- Collapse repeated closed-channel diagnostics to one report per connection
- Preserve the first disconnect diagnostic while preventing Netty log storms after queued writes fail

## 3.9.0 - 2026-10-28

- Apply the GitHub-derived JEM keymap once per client profile through a versioned local receipt
- Preserve every player edit after that first post-update application across reconnects, launcher restarts and later AutoModpack generations
- Keep Backpacked and TrashSlot bindings excluded

## 3.8.0 - 2026-10-28

- Move Animal Hints, Excavation, Freecam, Seasons HUD and Inventory Shortcuts to their standalone owners
- Remove duplicate handlers and registrations while preserving unrelated compatibility behavior
- Refresh current integrations, localization and public release metadata
