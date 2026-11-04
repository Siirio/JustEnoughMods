# Blueprint Roll visual brief

## Source and ownership

- User reference: `reference/user-blueprint-roll.png`
- Source: image supplied directly by the user in Codex chat on 2026-11-04
- SHA-256: `998C5577A7CAA2CEC087CEA7B66693D83214CD37E1EF0E031B468A337D603149`
- Use: visual direction only. No pixels or geometry are copied into production assets

## G0 contract

- The active tool is one blue blueprint sheet, never a bundle of two or three rolls
- The closed silhouette is a single compact roll with a pale paper edge and a tan cord tied around its center
- The open silhouette is the same single sheet partially unrolled into a readable drawing surface
- The finished Structure Blueprint has its own blue closed-roll inventory icon and uses the closed Blockbench model in hand
- Finished Structure Blueprints have no opening or sketch animation
- No arm geometry is authored in Blockbench. The only props are the blueprint sheet, cord, and pencil

## First-person action

- First corner selection opens the roll
- On the second corner, the existing Punchy right arm moves out to the player's right and behind the camera as if reaching behind the right ear
- The same arm returns holding the pencil, makes three quick deliberate drafting strokes on the open sheet, withdraws the pencil, and the sheet rolls closed
- Punchy owns the arm pose and timing. JEM Building only triggers the inspected Punchy animation seam and changes the held model state
- The action must never render an extra modeled hand or create a third or fourth arm
- Target rhythm: reach 0.18 s, return 0.18 s, three strokes over 0.52 s, close and settle 0.30 s

## Geometry and texture budget

- Java 1.20.1 cuboid item models authored in Blockbench
- One 64x64 Aseprite-authored atlas with a limited blue, pale-paper, tan-cord, graphite, and wood palette
- Closed model: one stepped cylindrical roll, one connected cord wrap, one readable knot/bow contact
- Open model: one broad sheet with one curled edge and no duplicated paper layers posing as extra sheets
- Pencil: one small prop revealed only during the Punchy sketch action
- Texture carries blueprint linework and paper wear. Geometry remains silhouette-led

## Reference reading

- The user's photograph supplies the single-roll silhouette, blue material, pale edge, and central cord
- Project positive references supply stepped Minecraft cuboid construction, clean material separation, and believable attachment contact
- External Blockbench scroll examples were inspected only for category legibility. Their geometry and textures are not copied
