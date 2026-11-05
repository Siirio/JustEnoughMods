# Blueprint Roll visual brief

## Source and ownership

- User reference: `reference/user-blueprint-roll.png`
- Source: image supplied directly by the user in Codex chat on 2026-11-04
- SHA-256: `998C5577A7CAA2CEC087CEA7B66693D83214CD37E1EF0E031B468A337D603149`
- Use: visual direction only. No pixels or geometry are copied into production assets
- User icon reference: `reference/user-blue-house-icon-style.png`
- Source: cropped inventory screenshot supplied directly by the user in Codex chat on 2026-11-05
- SHA-256: `50F771FEC0E6308983504E9C81CFB9F0E2D5336623FF6F097BCAF69B027FCEA7`
- Use: blue tile, bright inset and compact white-symbol direction only. Screenshot pixels are not copied into production assets

## G0 contract

- The active tool is one blue blueprint sheet, never a bundle of two or three rolls
- The closed silhouette is one long narrow roll with a literal 2x2 body cross-section and a temporary white cord around its center
- The open silhouette is the same single sheet partially unrolled into a readable drawing surface
- The finished Structure Blueprint uses a compact blue square inventory plaque with a centered white house symbol, while retaining the closed Blockbench roll model in hand
- Finished Structure Blueprints have no opening or sketch animation
- No arm geometry is authored in Blockbench. The only props are the blueprint sheet, cord, and pencil

## First-person action

- First corner selection opens the roll
- On the second corner, the left hand rises to pin the upper edge while the right hand drops to the lower edge and pulls the sheet vertically open. Both grips remain visibly connected to the paper during this tension beat
- The right wrist then turns palm-up and the existing Punchy right arm moves out to the player's right and behind the camera as if reaching behind the right ear
- The same arm returns holding the pencil, makes three quick deliberate drafting strokes on the open sheet, returns the pencil behind the ear, and comes back to compress the sheet with the left hand
- The left hand releases only after the closed roll has transferred decisively into the right hand
- Punchy owns the arm pose and timing. JEM Building only triggers the inspected Punchy animation seam and changes the held model state
- The action must never render an extra modeled hand or create a third or fourth arm
- Target rhythm: two-hand tension 0.30 s, wrist turn and ear reach 0.28 s, return 0.18 s, three strokes over 0.46 s, pencil return 0.24 s, close and transfer 0.32 s

### Art-direction prompt

A practiced builder handles one compact blueprint as a physical object. The left hand rises and pins the upper edge. The right hand reaches down, grips the lower edge, and pulls it downward while the left hand yields slightly upward so the single sheet reads as vertically tensioned rather than floating. The right wrist rolls palm-up, exits past the right edge toward the unseen pencil behind the ear, returns with the pencil, and draws three short purposeful strokes. It returns the pencil behind the ear, comes back empty, helps compress the sheet, and leaves the closed roll seated large and clearly in the right hand. Keep the camera restrained, preserve continuous hand-to-prop contact, reuse only Punchy's two existing arms, and stage each ownership transfer as clearly as the positive first-person medkit reference. The user's runtime screenshot is the negative reference for scale and contact: never leave a tiny prop detached near the center-bottom of the frame

## Geometry and texture budget

- Java 1.20.1 cuboid item models authored in Blockbench
- One 64x64 Aseprite-authored atlas using blueprint blue, a slightly deeper blue, cool white linework and graphite only
- Closed model: one 12x2x2 blue body, one minimal darker-blue depth strip and one simple white cord wrap
- Closed model has no bow, segmented rings, stepped cylinder or brown, tan, beige and wood colors
- Open model: one broad sheet with one curled edge and no duplicated paper layers posing as extra sheets
- Open sheet linework: one compact room-plan motif with a doorway gap, surrounded by broad quiet blue areas. Random decorative lines and dense wear are prohibited
- Pencil: one small prop revealed only during the Punchy sketch action
- Texture carries only the minimal blueprint plan and controlled material shading. Geometry remains silhouette-led

## Finished-blueprint house icon

- Consumer: `jem_building:structure_blueprint` GUI and inventory icon only
- Native source and export: layered 32x32 Aseprite RGBA, displayed by Minecraft at 16x16 inventory scale with nearest-neighbour pixel structure
- Projection: frontal blueprint sheet with readable scroll depth, not a flat square plaque or a perspective object
- Dominant mass: one connected deep-blue sheet occupying most of the canvas with a lighter blue inset, restrained top-left highlight, a top roll shifted backward and a bottom roll shifted forward
- Focal symbol: one connected cool-white house silhouette centered on the tile, built from two-pixel clusters so its roof, wall, doorway and single window survive the 16x16 display
- Value roles: navy contour and lower-right depth, saturated blueprint-blue face, cyan highlight, cool-white house, pale-blue symbol shadow
- Avoid: copying either screenshot glyph, tiny isolated pixels, a flat square tile, a generic white document page, a detailed cottage illustration, glow spill, text, gradients and antialiasing
- Authoring owner: Codex paints the native pixels directly in Aseprite under the user's explicit 2026-11-05 instruction not to use Gemini for this task

## Reference reading

- The user's photograph supplies the single-roll silhouette, elongated proportion, blue material, pale edge and central cord
- Project positive references supply clean Minecraft cuboid construction, strong material separation and readable attachment contact
- `ANI05_Medkit` supplies the positive first-person staging principle: large readable prop, two hands with distinct jobs, visible contact, and deliberate ownership changes as each hand enters and leaves frame
- External Blockbench scroll examples were inspected only for category legibility. Their geometry and textures are not copied
