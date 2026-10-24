# Native model packaging

`package_native_models.py` consumes the final Blockbench Java Block/Item codec exports. It does not author textures or house geometry. Keep the editable `.bbmodel`, native codec JSON and external PNG together in the owning art directory

```powershell
python Mods/MobHome/tools/package_native_models.py --small Docs/art/mobhome-20261024/small-stone-optimized.json --large Docs/art/mobhome-20261024/large-log-optimized.json
```

Run from the repository root. `--resources` optionally selects another complete resource output directory. Copy the matching native PNG exports separately to `assets/mobhome/textures/block/small_materials.png` and `large_materials.png`

The input must preserve explicit face UVs, element names and native Java rotations. All faces use the house's single material atlas. Names prefixed `nocollision:` identify decorative foliage and the gable render planes. The paired exterior/interior gable planes additionally define triangular wall collision. All remaining cuboids are physical solids

Unrotated cuboids are split at the occupied-cell boundaries with their original UV mapping interpolated to each piece. Rotated cuboids are split only along the rotation axis, so the native rotation and roof slope remain intact. Internal partition caps are omitted. Every piece is assigned exactly once to a legal occupied-cell model. Cell coordinates use `x + size*z + size*size*y`, with front at local positive Z and SOUTH at zero blockstate rotation. Render geometry uses native static baked cutout models without an animated renderer

The same export produces `data/mobhome/shelter_geometry.json`. Unrotated collision is exact. Rotated roof collision uses conservative cuboid stairs at two-model-unit intervals. This deliberately approximates the slope and may extend to the outer edge of each two-unit strip. It never replaces the roof with one enclosing box. Collision is clipped to occupied cells and rotated by the existing server-owned multiblock shape cache. Door and window openings therefore follow the actual solid elements rather than an enclosing house cuboid

Whole-house item models are uniformly normalized around their native center, including rotation pivots, within Java's element-coordinate limits. UVs and world models are unchanged. Items use separate display transforms. The per-cell block export never repeats the entire house in every cell
