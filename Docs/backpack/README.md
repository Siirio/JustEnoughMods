# Backpacked: direct Workbench augment

Owner: JEMCompat feature/backpack and narrow mixin/backpack adapters around installed Backpacked3.0.9 / Framework0.8.0. No patched third-party JAR or replacement screen.

## Player flow

Open the equipped backpack, choose an unlocked native augment bay and click Workbench. Successful native selection immediately enables that augment and refreshes the native backpack screen with a3×3 ingredient grid and one result slot between backpack/player inventories. No workshop, lapis fee, crafting-table payment or purchase entitlement. Native bay unlocks, disabled types, duplicate selection, other augment settings and backpack limits stay upstream-owned.

Changing/removing/toggling an augment refreshes only when the actual enabled/unlocked crafting layout differs from installed menu slots. Ingredients remain on the same backpack under JemWorkbenchIngredients: removing/disabling Workbench preserves them, re-enabling restores them.

The retired jemcompat:augment_bay block/item identities and placed-block geometry remain only for existing saves. Recipe, creative/JEI entry, interaction, menu/screen, payment code and purchase messages are removed. Existing JemWorkbenchPurchased metadata is ignored without rewriting saves.

## Inventory and authority

Only the equipped owner's native BackpackContainerMenu gains crafting; docks, shelves and pickpocket views keep their native lifecycles. Existing backpack/player slot indices remain;9ingredients and vanilla ResultSlot append after them. Player inventory moves80px down and the crafting insert occupies that space. Shift-click preserves backpack/player direction and sends crafting inputs/results to player inventory. Native ResultSlot owns consumption, remainders, hooks and rewards; partial shifted results drop like vanilla CraftingMenu. Result pick-all is blocked.

Server RecipeManager, limited-crafting policy and enabled features determine results; the result is never saved as an extra claimable item. Native BackpackInventory.isAllowedItem governs ingredient slots, container setters, remainders and persistence. Invalid loaded/in-place-mutated ingredients are removed before return to player inventory/drop, without deletion or nested-container bypass.

Integrated singleplayer retains upstream behavior: Workbench is hidden there, while its registered codec/assets support dedicated connections. Clients render synchronized menu state and never own crafting outcomes.

## Appearance and evidence

The compact centered crafting insert uses native BACKPACK_BACKGROUND, BACKPACK_SLOT and ARROW_RIGHT FrameworkTexture definitions, native tiling and18px spacing. Shared BackpackWorkbench constants own its112×80 geometry, ingredient offsets12,12 and result85,30. It has no title or side tab. With Workbench active, the lower player inventory uses native AUGMENT_MENU_BACKGROUND and BACKPACK_SLOT rather than the grey inventory material. WorkbenchIcon.java is the editable12px cream/shadow crafting-table silhouette; rejected green icon and separate GUI PNGs no longer ship. Short viewports retain bounded menu scrolling with native popup priority.

Original user screenshots and exact extraction: Docs/art/backpack-augment-bay/reference/2026-10-19/README.md. Current art source: Docs/art/backpack-augment-bay/README.md. Source/build and preview evidence do not establish gameplay/visual approval. No client/gameplay launch or automated tests; click refresh, crafting remainders and GUI Scale require player review.
