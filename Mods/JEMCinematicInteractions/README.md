# JEM Cinematic Interactions

Sandbox-only client integration for Cinematic Interactions block scenes:

- removes the vanilla recipe-book panel and toggle from recipe-aware screens so JEI remains the recipe interface
- leaves JEI's ingredient list on the right and bookmark list on the left
- opens JEI recipe lookup through Cinematic Interactions' native consultation lifecycle, then returns to the active scene
- exposes a Cinematic-owned 3x3 crafting transfer contract to JEI so JEI can show its native recipe-transfer `+` button without treating the scene as the player's 2x2 inventory grid
- delegates that button to Cinematic Interactions' existing recipe guide and table layout queue, including Shift-click maximum layout
- Shift + left-click and drag quick-moves every crossed stack using Cinematic Interactions' native quick-transfer paths
- works in both directions between the player inventory and the active Cinematic block
- does not intercept Creative inventory or any ordinary container screen, leaving Mouse Tweaks behavior untouched

The gesture does not invent insertion rules or bypass a menu. Invalid machine slots remain protected by the owning menu's normal transfer behavior.
