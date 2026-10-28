# JEM Cinematic Interactions

Sandbox-only client integration for container screens:

- removes the vanilla recipe-book panel and toggle from recipe-aware screens so JEI remains the recipe interface
- leaves JEI's ingredient list on the right and bookmark list on the left
- opens JEI recipe lookup through Cinematic Interactions' native consultation lifecycle, then returns to the active scene
- Shift + right-click and drag quick-moves every newly crossed stack using the active menu's native `QUICK_MOVE` rules
- works in both directions between the player inventory and any vanilla or modded container

The gesture does not invent insertion rules or bypass a menu. Invalid machine slots remain protected by the owning menu's normal transfer behavior.
