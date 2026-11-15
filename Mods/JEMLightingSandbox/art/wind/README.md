# Wind gust atlas

The reference is the white stepped curl in the Windy icon supplied on
2026-11-14. Use its original source pixels for the calm gust. The strong
gusts use the matching native Windy animation. The two strong variants
have different orientations, speeds and world-space dimensions.

The direct Aseprite export is a 512 × 96 RGBA atlas with eight 64 × 24
cells per row and nearest sampling. Cells 0–11 reveal and erase the
unchanged light silhouette of the original icon. Cells 12–30 preserve the
strong-wind frames. Cell 31 is empty. The game applies biome color to
the strong gusts at draw time. It does not recolor the texture on disk.

Open `wind_gusts.aseprite` to edit the two native layers. To recreate
the assembly, run `assemble-wind-atlas.lua` with Aseprite and its
`source` parameter set to `src/main/resources/assets/windy` from
the pinned upstream archive and `output` set to an existing folder.
See the packaged `licenses/windy/NOTICE.txt` for source and license.

The atlas and its contour were checked in Aseprite output. Visual motion,
terrain clearance and apparent length need in-game review.
