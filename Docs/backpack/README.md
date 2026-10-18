# Backpacked: Augment Bay and Workbench

Owner: JEMCompat `feature/backpack` and narrow `mixin/backpack` adapters around the
installed Backpacked3.0.9 + Framework0.8.0. No patched third-party JAR or fork.

## Player flow and native boundaries

Craft `jemcompat:augment_bay` from4 iron ingots,4 planks and1 crafting table.
Place it and use it on the dedicated server. Insert the actual backpack in the
left clamp,4 lapis lazuli in the middle, and a crafting table in the right slot
when purchasing that backpack's Workbench for the first time. Select an installed
native augment type and an already-unlocked native bay, then press Install.

Each successful station installation/change costs4 lapis. A first Workbench also
consumes1 crafting table and records its purchase on that exact backpack. Later
native Workbench swaps use that purchase entitlement. Existing native augment
selection, prices for unlocking bays, settings, saved removed-augment data,
backpack names, item contents and inventory limits keep their upstream owners.
Ordinary native augments retain their native selection route; the station does
not retroactively price or remove that established route.

The station validates the live backpack item, selected registered/non-disabled
augment, unlocked bay, duplicates across all4 bays, prerequisites and dependent
augments before taking resources. It saves the replaced native augment through
`SavedAugments`, installs through `Augments.cached`, and enables the selected bay.
Closing/breaking/moving away returns all unused input items through the normal
server menu lifecycle. There is no detached backpack-inventory copy.

Install and enable Workbench, then equip/open that backpack normally. A functional
3×3 ingredient grid and native result slot appear between backpack/player
inventories. The existing native backpack/player slots retain their indices;
new crafting slots append after them, with player Y positions moved by76 pixels.
Shift-click from either inventory preserves the native backpack/player direction;
shift-click from crafting/result sends to the player inventory. Native `ResultSlot`
owns ingredient consumption, remaining buckets/bottles, recipe rewards and craft
hooks. A partial shifted result is dropped exactly as in vanilla CraftingMenu.

The9 ingredient stacks persist in that backpack's `JemWorkbenchIngredients` NBT.
The result is a recipe preview, recomputed authoritatively from saved ingredients
and the current recipe manager when opened/changed. All9 ingredient slots and
the authoritative grid use native `BackpackInventory.isAllowedItem`, preserving
the configured banned items and `Item.canFitInsideContainerItems` rejection of
backpacks/shulkers. Remainder insertion uses the same grid setter. Before saving,
loaded or directly mutated prohibited contents are removed from the grid then
returned to the player inventory (or dropped if full), with no item deletion.
The result is never serialized as a
second freely claimable item. The native limited-crafting policy and enabled-item
features remain effective. Client initial slot synchronization includes real grid
contents; subsequent result packets name the actual appended result index.

Changing/removing/toggling the Workbench uses the existing native packet and then
reopens the native backpack only when slot layout must change. Ingredients remain
stored while the augment is disabled/removed and return when it is enabled again.
Dock/shelf and pickpocket screens retain their native non-owner behavior and do
not receive a detached crafting grid. Integrated singleplayer keeps upstream
Backpacked runtime behavior; shared registry/asset compatibility remains present.

## Decisions and evidence

User requested a real Augment Bay workstation, native-compatible augments,
resources/duplicate protection, saved upgrades, original pixel assets, a working
persistent crafting section between inventories and shift-click support. User
screenshots3/4 were not present in the supplied attachment folder.

Codex selected the native augment registry/codec extension, separate station
menu,4-lapis change cost, one crafting-table Workbench purchase, persistent item
NBT ingredients and76-pixel native-menu insert. These are implementation/product
details selected independently under the user's approval to proceed.

Installed Backpacked JAR SHA256:
`2b9b07d873fc3a913e6f259cd0e85c45999e1f1c03927a7202fbd4f7fb185d14`.
Exact source/bytecode inspected: AugmentType/ModRegistries, Augments/SavedAugments,
BackpackItem/BackpackInventory, BackpackContainerMenu/BackpackScreen,
ServerPlayHandler native augment packets, Framework registry registration and
vanilla1.20.1 CraftingMenu/TransientCraftingContainer/ResultSlot.

Native data/API and events alone cannot add slots/layout to the already-registered
Backpacked menu. A native custom AugmentType plus constructor/transfer/layout and
packet-admission Mixins is narrower than a fork or replacement backpack GUI.

## Art and acceptance boundary

Editable native-size source, original exports, asset contract, references and
inspection contact sheet: [art source](../art/backpack-augment-bay/README.md).
The original Backpacked cream/brown recessed-pixel material is preserved; native
Backpacked buttons are rendered through its public TextureDefinitions API.
No third-party art pixels are copied into the new authored assets.

Source and export inspection do not establish gameplay or visual acceptance.
No gameplay launch or automated tests were run. Still requires player review of
reopening/shift crafting and remainders, station closure/disconnect, changed or
disabled augments, backpack death/drops and GUI Scale. For scaled viewports shorter than the menu, wheel scrolling moves the native
menu, controls and hit coordinates together within bounded top/bottom limits; a
localized hint explains the scroll. Native popup scrolling retains priority.
This source-level viewport handling and screenshot fidelity remain unproved by
runtime footage.
