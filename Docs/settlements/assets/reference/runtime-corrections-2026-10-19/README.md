# Runtime corrections — user direction, 2026-10-19

Original user screenshots preserved byte-for-byte; these are rejected/mixed runtime evidence, not production textures or approval.

- 01: settlement selector has excessive empty area and unused sidebar. Use content-sized bounds.
- 02: remove whole-town isometry; retain individual resident house isometry. Reflow comfort categories into the available width.
- 03: remove crossed header reconcile/help/close controls. Keep Escape and automatic action reconciliation.
- 04: remove Find and Name-sort buttons; search filters on typing; keep the All filter.
- 05: user explicitly rejects solid full-row hover/selection and plain outlined row hover; use authored material states and a small local cue. Text shadows on pale cards look duplicated; eliminate that shifted pass. Shared status baseline must have one message.
- 06: fit chest window to actual native-size slot grid and actions; do not stretch a tiny inventory across a full-screen empty sheet.

Implementation choices by Codex: 460px directory maximum with up to six rows, 364px chest maximum with height derived from rows, two-by-two comfort group, book hover cue, selected control sprite rim over stone. These numerical choices are not independently user-approved. Runtime acceptance remains pending; no game launched.
