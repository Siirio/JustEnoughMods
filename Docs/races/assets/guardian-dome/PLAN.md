# Connected Magical Vanishing Shield Dome

Latest user direction: replace independent flat/overlapping tiles with **one
cohesive curved model**, correct UVs/normals and player-relative Minecraft scale.
This explicitly supersedes the preceding requirement for separately stacked
cuboid plates. Pixel-art material, gold/amber/ivory and no floor rune remain.

1. Remove the separate-plate generator/export from the active runtime. Generate
   one Blockbench mesh:16 azimuth sectors, six latitude rings, one shared crown
   vertex per surface. Outer/inner radius2.20/2.16 blocks, baseY=.10; one joined
   rim closes the thickness. Total194 shared positions/208 faces; no isolated panels.
2. Share ring vertices, including sector15→0. Reverse the inner face winding;
   outward analytic normals on the exterior, inward on the interior, hard downward
   normals at the bottom rim. The surface shader interpolates corner normals;
   normals at the UV seam are identical despite different U coordinates.
3. Wrap one **256x64** purpose-painted gold-wave atlas around the complete dome.
   Approximate base texel density18.5px/block; nearest filtering, no mipmaps/blur.
   Seam columns match. Polar texels are deliberately uniform to avoid pinched
   rune strips. UVs belong to face corners; positions remain shared.
4. Export the actual Blockbench project mesh as bounded indexed JSON, keeping
   vertices, winding, per-corner UVs and normals. Java reads that exact resource;
   it no longer reconstructs spheres or places per-plate icons in radial loops.
5. Use additive NEW_ENTITY triangles, normal-aware entity shader, backface culling,
   depth testing and color-only writes. Choose outer versus inner surface from
   camera position so both layers do not superimpose their wave patterns. No depth
   bias, coplanar decals or polygon overlap is used to hide bad seams.
6. Preserve30-tick authoritative guardian event. Uniform .92→1 growth and1→1.06
   release are applied to the entire connected shell; global lifetime alpha fades
   over the final8 ticks. Gameplay, healing, Resistance and the pearl are unchanged.

```text
author.py                  geometry/material source owner
gold-wave.ora/.png          external editable layers/exact atlas
guardian-dome.bbmodel       one native Blockbench mesh
export.mjs                 actual native-project export, UV/normal preservation
guardian_dome/model.json    runtime indexed geometry
GuardianDomeRenderer.java   bounded load, material, smooth normals, one shell draw
```

An ordinary player is1.8 blocks tall; the dome is2.2 above its .1-block base and
4.4 across. Editor preview captures use a neutral material to inspect geometry/UV
without Blockbench additive overexposure. They are not runtime shader evidence.
