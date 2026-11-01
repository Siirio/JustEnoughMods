# JEM Building selection rework

Status: planned only. No implementation, build, installation, or runtime check is authorized by this note

## User requirements

- A left-click with the construction hammer selects the first endpoint
- A right-click with the construction hammer selects the second endpoint
- Endpoint selection must work when the crosshair hits an existing block
- Existing blocks must be valid anchors for creating the construction plane on or from that surface
- Air selection must remain available and must not be replaced by block-only targeting
- The selected construction span is limited to 256 blocks on each of X, Y, and Z
- The limit is an inclusive axis span, so `max - min + 1` may not exceed 256 for any axis
- Construction remains axis-aligned and orthogonal. This request does not authorize diagonal planes or diagonal interpolation
- The current plane or axis selection interaction is not approved and must be redesigned before implementation

## Planned interaction work

1. Separate endpoint selection from construction confirmation so left-click and right-click have stable, distinct meanings
2. Resolve a block hit to a precise block position and hit face without suppressing the hammer interaction on existing blocks
3. Preserve the current air-target route as an explicit fallback when no block is hit
4. Store both endpoints and their hit context in hammer state so client preview and server authority use the same selection
5. Derive only an orthogonal construction plane from the two endpoints and the eventual approved plane-selection gesture
6. Reject a second endpoint when any inclusive X, Y, or Z span exceeds 256, with a clear player-facing reason
7. Keep all placement, placeholder replacement, Creative instant fill, and Survival inventory-consumption rules from the current sandbox implementation

## Product decision still required

The user explicitly dislikes the current axis-selection method. Before coding, define one unambiguous gesture for choosing among XY, XZ, and YZ when the two endpoints alone do not determine a unique plane. Do not infer or ship that gesture from this planning note

## Acceptance boundary for the future implementation

- Left-click changes only endpoint one
- Right-click changes only endpoint two
- Both actions work against existing blocks
- Both actions still work in air
- Every accepted result is aligned to X, Y, or Z and never diagonal
- No accepted selection spans more than 256 blocks inclusively on any axis
