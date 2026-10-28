# JEM Cinematic Interactions

Sandbox-only client integration for Cinematic Interactions block scenes:

- removes the vanilla recipe-book panel and toggle from recipe-aware screens so JEI remains the recipe interface
- leaves JEI's ingredient list on the right and bookmark list on the left
- opens JEI recipe lookup through Cinematic Interactions' native consultation lifecycle, then returns to the active scene
- exposes the active Cinematic block menu to JEI so JEI can show its native recipe-transfer `+` button
- Shift + left-click and drag quick-moves every crossed stack using Cinematic Interactions' native quick-transfer paths
- works in both directions between the player inventory and the active Cinematic block
- does not intercept Creative inventory or any ordinary container screen, leaving Mouse Tweaks behavior untouched

The gesture does not invent insertion rules or bypass a menu. Invalid machine slots remain protected by the owning menu's normal transfer behavior.
