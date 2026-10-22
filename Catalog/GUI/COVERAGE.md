# Installed GUI reference coverage

User request: catalog the GUI components available to `test jem`, arranged as
`GUI/mod-name (or vanilla) -> link + PNG`, for selective future JEM resource-pack
work. This export prepares references; it does not author or install that pack.

## Receipt

- Native installation: `fn9aq74`, generation 63, content token
  `ea5c8e10320356cce80c6bda1af5c992198ce954`, status `ACTIVE`.
- Read-only inputs: profile vanilla 1.20.1 client archive, Forge mapped client
  classes, 285 active managed mod JARs plus the profile bootstrap, declared nested
  JARs, KubeJS assets, FancyMenu assets, selected file resource packs and Paxi packs.
- Sources: 598; image resource candidates considered: 45,320.
- Export: 10,837 original PNG variants, 10,688 distinct resource paths, 361
  owner/namespace groups (145 groups with PNGs), 74 PNG metadata companions.
- Categories: 2,735 interface assets, 7,915 map/entity icons, 28 font sheets and
  159 other texture references from UI classes (unproved render dependencies).
- Other evidence: 10,047 UI class candidates and 2,344 non-PNG GUI resource entries.
- Full per-source fingerprints, byte hashes, dimensions, containing modules and
  original override paths are in [manifest.json](manifest.json).

The active native installation is the input, not the clean source pack or another
profile. The top-level `mods` folder alone would have missed the 285 managed JARs.
Original resource namespaces remain intact even when a current JEM domain owns
them, for example Races resources under `jem_roles`. Resource namespace and module
ownership are separate fields.

## Boundaries

The shared Atlas CLI returned `UNSUPPORTED` for `audit --domain gui`. Accordingly
this is a non-canonical reference attachment section alongside the existing
Atlas, not a replacement factual graph, GUI registry or runtime adapter. The
existing server Atlas input lock also has pre-existing drift; this request does
not regenerate or promote unrelated gameplay evidence.

The PNG selection uses GUI/HUD/widget/icon/book/font and similar resource path
segments plus PNG constants in classes with GUI names, GUI superclasses or
references to `net/minecraft/client/gui`. These provide static candidates, not
proof of reachable screens, live controls or complete renderer semantics. The
class list intentionally retains helpers, mixins, integrations and disabled
features. A zero-PNG group is not evidence that its mod has no interface.

Whole source sheets remain whole. No UV rectangles, state boundaries, nine-slice
rules or procedural control images are invented. A native button may use part of
a shared vanilla sheet; a rendered item/entity, dynamic map, font-composed label
or code-drawn panel may have no standalone PNG. These remain unresolved rather
than being represented by fabricated art. Exact per-control decomposition and
runtime screen coverage are not complete.

Selected file-pack variants, including CoffeeGUI, remain beside native variants.
Known file selection order yields only `selected_stack_candidate`; ambiguous mod,
builtin, generated, Paxi and server-resource-pack order is not asserted as a
runtime winner. Unselected file packs are not included. Packaged Xaero icons for
other modpacks remain reference candidates, with target namespace/observed mod
metadata fields; their presence never registers an entity or proves it installed.

One packaged image is empty and cannot supply a PNG:
`assets/xaerominimap/entity/icon/sprite/dawnera/worm/worm.png` in the selected
`Icon Xaero's 1.22.zip` support pack (the manifest records its exact locator/hash).
The original entry is zero bytes. It is recorded as `invalid_png`, not silently
repaired or counted as an exported image.

## Visual evidence and provenance review

Applied the native-resource-pack Taste Engine pair and visual-auditor to this
reference-only scope. Inspected the actual vanilla and CoffeeGUI
`minecraft:textures/gui/container/inventory.png` originals: native slots and
button regions are visible, and the transparent unused regions remain intact.
The references establish existing assets, not new personal-taste classifications
or approval for a new design. The exporter copies source PNG bytes unchanged;
hashes record identity. No pixel painting, resampling, crops or custom GUI art is
produced. New-art geometry/palette/editor-source gates are therefore N/A.

Inspected the rendered local gallery and its vanilla filter. Source and CoffeeGUI
variants display beside one another with PNG links and readable source labels.
The gallery renders 60 matching entries per page instead of mounting every image
at once; the full manifest and group Markdown pages retain all entries. This was
document presentation review, not gameplay or control-state acceptance.

No game was launched. Runtime composition, control hitboxes, enabled screens and
resource-manager precedence remain UNOBSERVED. No gameplay acceptance, automated
test, Gradle build, static code analysis or resource-pack installation is claimed.

PNG files, animation metadata companions and the HTML gallery are local reference
attachments excluded from Git. The tracked exporter, manifests and group indexes
recreate them without redistributing third-party artwork. The gallery's selection
download lists chosen references/override targets for the later authoring task;
it does not install resources. Original licenses remain attached to source
records; the export confers no additional redistribution rights.
