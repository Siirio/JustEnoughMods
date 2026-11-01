# Changelog

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
