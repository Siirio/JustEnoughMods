# Blueprint Roll review log

## C0 preflight

- Primary reference is present locally and checksum-recorded
- Exact model, texture, and animation Taste Engine pairs were loaded
- Production route is Blockbench for cuboid model and UV, Aseprite for native pixels, Punchy for the existing first-person arm
- Hard rejection conditions: more than one sheet, disconnected cord, extra arm geometry, pencil moving without the Punchy hand, or finished blueprints inheriting the tool animation

## Candidate review

- Closed C0: rejected because the 26-cuboid stepped barrel and brown bow made the roll chunky and overbuilt
- Closed C1: one elongated 12x2x2 blue body, one narrow darker-blue depth strip and one simple white center wrap. The complete model uses three cuboids and has no bow, segmented rings or brown palette
- Open C0: rejected for an overly thick flat sheet that read closer to a tray than paper
- Open C1: reduced the sheet to half-unit thickness while retaining one connected curled edge. The silhouette now reads as one unrolled sheet
- Open C2: user rejected the remaining dense random linework. Repainted the open-sheet UV planes as broad quiet blue fields with one compact cyan room-plan motif and doorway gap, while retaining the accepted single-sheet geometry, curled edge, pencil, and Punchy animation
- Pencil C0: one compact square-section pencil with graphite tip, wood body, metal band and eraser. It is a prop only and includes no hand geometry
- Inventory icon C0: user rejected the segmented blue strip because it did not read as the requested tied rectangular roll
- Inventory icon C1: rejected during internal review because the disconnected white pixels did not form a readable bow and the pale marks continued to segment the body
- Inventory icon C2: one uninterrupted eight-pixel-wide diagonal blue rectangle with continuous highlight, body and shadow planes. A narrow white center cord, two connected loops, central knot and two short tails read as a bow at native inventory size
- Inventory icon C3: user retained the C2 body, palette and bow but identified its horizontal terminal cuts. The two blue source layers were rebuilt within parallel `x-y = -18` and `x-y = 18` end planes, producing diagonal stair-step caps without center spikes. The linework and bow layers match C2 byte-for-byte when exported independently
- Animation source: the Blockbench project contains only `right_arm`, `left_arm` and `camera` animation bones with no cubes. Punchy renders the player's existing arms
- Geometry validation: corrected closed C1 passed outliner, unlisted-node, degenerate and Java block-limit gates with exactly three cuboids. Its blue body is 12x2x2 and only the temporary cord extends the total cross-section to 2.4
- Texture validation: layered Aseprite sources remain populated and editable. Their exported runtime textures contain no brown, tan, beige, rope-brown or wood-brown pixels
- Open-sheet validation: the updated embedded Blockbench texture matches the native Aseprite export. The final three-quarter render contains one readable plan, no random line scatter and no transparent holes
- Animation validation: both clips target existing bones and have valid keyframe timing. Geometry-dependent gates are intentionally inapplicable because the project contains no arm geometry
- Runtime C0: user rejected the installed 1.1.1 first-person presentation because the roll was tiny, detached near the center-bottom of the frame and not seated in the visible right hand. Source inspection traced this to 0.68/0.72 first-person display scales plus a Punchy override that hid the real right item and substituted a NBT-empty closed roll into the left hand
- First-person C1: first-person closed/open scales are now 1.08/1.02, the GUI roll scale is 1.06, and the generated finished-blueprint icon is enlarged to 1.25 without repainting its approved pixels. A hidden `blueprint_open_prop` supplies the actual open model during Punchy overrides instead of relying on NBT that the override cannot carry
- Animation C1: the editable Blockbench source and runtime clip now stage left upper grip, right lower pull, vertical tension, palm-up ear reach, pencil return, three drafting strokes, pencil replacement, two-hand compression and a final closed-roll override in the right hand. The source still contains exactly the existing two arm bones and camera, with no arm cubes
- Animation C1 numeric review: headless Blockbench inspection recognizes both clips at 0.8 s and 1.9 s, every animated bone exists, the key beats sample without skipped channels, and both arms return to neutral at the end. Geometry-dependent contact rendering remains inapplicable to this arm-only source
- House icon C0 cold read: the 32x32 candidate immediately reads as one white house inside one blue inventory tile, with a single focal symbol, connected clusters and no copied screenshot glyphs. The first light-background render produced contradictory compositing evidence, so it was not accepted from that preview alone
- House icon C1: explicitly normalized all four Aseprite layers, exported the actual transparent runtime PNG, then inspected that export at native 32x32, simulated 16x16 game display, checker, light and dark backgrounds. The stepped navy contour remains closed, the cyan top-left and deep-blue lower-right establish tile depth, and the roof, wall and doorway survive at 16x16. The tiny window intentionally drops out before the house silhouette does
- House icon C2: user identified that C1 lost the original scroll character and read as a flat tile. Added one editable `Paper curls` layer with a top roll offset backward to the right and a broader bottom roll offset forward to the left. The opposing offsets make both paper curls visible without shrinking or obscuring the centered house
- House icon provenance: `structure_blueprint_icon.aseprite` remains the layered production source. The previous approved diagonal roll source is preserved unchanged as `structure_blueprint_roll_icon.aseprite`, and the user's 100x82 screenshot remains reference-only under `reference/user-blue-house-icon-style.png`
- Icon validation: native 32x32 Aseprite source retains separate silhouette, shading, linework and white-cord layers. The six-color export contains only dark blue, blue, lighter blue, cool white and transparent pixels, with no brown or beige. Both terminal planes are parallel at 45 degrees and preserve the eight-pixel body width
- Delivery verdict: house icon C2, open-sheet C2 and first-person C1 pass source review. JEM Building 1.1.3 was rebuilt and installed only in the closed `jem sandbox` profile. Live in-game projection movement, icon composition in the inventory and player acceptance remain unobserved until the sandbox is launched manually
