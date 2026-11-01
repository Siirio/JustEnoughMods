# JEM Building selection rework

Status: implementation authorized on `sandbox/jem-building`

## User requirements

- A left-click with the construction hammer against an existing block opens the four-way radial shape menu and establishes the first endpoint after the player chooses a mode
- A right-click with the construction hammer selects the second endpoint
- Endpoint selection must work when the crosshair hits an existing block
- Existing blocks must be valid anchors for creating the construction plane on or from that surface
- Air selection must remain available and must not be replaced by block-only targeting
- The selected construction span is limited to 256 blocks on each of X, Y, and Z
- The limit is an inclusive axis span, so `max - min + 1` may not exceed 256 for any axis
- Construction remains axis-aligned and orthogonal. This request does not authorize diagonal planes or diagonal interpolation
- The four modes are wall, floor, one-block-wide column, and hollow sphere

## Planned interaction work

1. Cancel native block breaking while the hammer opens its radial menu
2. Use four cardinal radial choices with distinct semantic item icons and localized labels
3. Store the selected shape, anchor, and wall plane in the hammer while the server validates the clicked block and owns the selection
4. Let right-click confirm against either an existing block face or the current air preview
5. Choose a wall plane from the dominant horizontal look axis so its width can extend to either side of the anchor
6. Keep floors on XZ, columns at one block in X and Z with free Y height, and spheres as hollow shells around the first endpoint
7. Clamp every endpoint to an inclusive 256-block span and cap a symmetric sphere to radius 127
8. Keep all placement, placeholder replacement, Creative instant fill, and Survival inventory-consumption rules from the current sandbox implementation

## Interaction decision

The radial mode menu replaces clicked-face plane selection. Wall orientation follows the dominant horizontal look direction at the first click, while floor, column, and sphere have intrinsic geometry

## Acceptance boundary for the future implementation

- Left-click changes only endpoint one
- Right-click changes only endpoint two
- Both actions work against existing blocks
- Both actions still work in air
- Every accepted result is aligned to X, Y, or Z and never diagonal
- No accepted selection spans more than 256 blocks inclusively on any axis
