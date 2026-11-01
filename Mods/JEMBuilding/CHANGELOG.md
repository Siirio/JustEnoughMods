## 1.0.0 - 2026-11-01

- Made Shift+right-click with the hammer explicitly cancel the complete linked temporary region
- Replaced sphere rings with bounded discrete block-cell preview geometry shared with the saved hollow sphere
- Replaced pitch-times-limit column targeting with a cursor-ray intersection against the column axis
- Built and installed the corrected sandbox artifact with SHA-256 `59f053f54ef45576eaa69212acc9ca1e0d68f01a1710093878cf25002c20c013`
- Replaced face-driven plane selection with a left-click radial menu for wall, floor, 1x1 column, and hollow sphere modes
- Added block and air right-click confirmation with an inclusive 256-block per-axis bound
- Made wall orientation follow the dominant horizontal look direction while retaining leftward and rightward expansion
- Built and installed the sandbox artifact with SHA-256 `6764c2284166b06566d7afea1a528cf8a86414e6918d3bb0d0de8bc31dfecda7`
- Bound construction orientation to the clicked block face so all three orthogonal planes are directly selectable and diagonal planes cannot occur
- Made construction placeholders native-placement replaceable, added immediate Creative whole-region conversion, and continued Survival conversion across matching inventory stacks with exact native consumption
- Rebuilt and installed the corrected sandbox artifact with SHA-256 `a86d9dbf5b806e426da5e87830501175c7fa1c8f981c36b9dc8be429d509bd18`
- Removed a duplicated persisted-region accessor block that prevented Java compilation
- Built the reobfuscated standalone artifact and installed its exact bytes in the local `jem sandbox` profile for manual acceptance
- Added one standalone JEM Builder Hammer with the requested copper, copper-block, stick and cobblestone-wall recipe
- Added live axis-aligned air targeting for XY, ZY and XZ construction planes
- Added persistent hollow non-colliding placeholder blocks and bounded server-owned placement/removal work
- Added survival material filling through native block-item placement, preserving inventory cost and placement behavior
- Reused the established custom 3D hammer model and texture as self-contained JEM Building assets
