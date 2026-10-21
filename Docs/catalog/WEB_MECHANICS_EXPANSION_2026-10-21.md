# Installed-pack mechanics research expansion

Started 2026-10-21; continued after midnight on 2026-10-22 (+05:00).
The research is bound to `Catalog/manifest.json` snapshot
`efd23bcffc6553f6f423cd4b5576e2732ad7e938`. The concurrent architecture revamp
is a separate change; its staged renames, deletions and gameplay edits are not
inputs to this research delivery.

## Requested scope and ownership

The user requests repository-wide Atlas enrichment using real web research and
available YouTube guide transcripts, preservation of concurrent work, and no
remaining documentary omissions hidden behind an inaccessible first source.
The later clarification explicitly excludes launching the game. Standalone
Rubidium is prohibited because the user reports crashes with it.

The existing ownership chain is preserved:

`Catalog/registries` -> `Catalog/changes` -> `Catalog/effective`.

External research supplies source-cited actions, prerequisites and detection
guidance. It never overwrites registry identities, effective numerical balance,
canonical JEM rules, gameplay source, installed artifacts or a release baseline.
Internal JEM rules remain at their existing source/Docs owners rather than being
retold as a second external specification.

## Coverage interpretation

Every enabled manifest entry has an explicit research document or a reviewed
owner/artifact route. The count includes embedded API modules; each is explained
as infrastructure rather than assigned an invented survival objective.

`canonical-routing.json` accounts for 14 JEM IDs, the vanilla base and one
filename-derived Kotlin archive entry. The latter has no outer mod descriptor;
its nested `kotlinforforge` module is researched independently. This routing
does not silently delete anything from the canonical manifest.

The installed `rubidium` ID is a stub declared by Embeddium, not a standalone
Rubidium artifact. No Rubidium JAR is installed, removed or recommended by this
task. The prohibition is retained in the research profile and README.

Coverage of manifest entries is separate from proof of every object field or
runtime interaction. An empty unassessed-ID list does not erase missing runtime
registry captures, optional consumer behavior, unknown effective configuration
or an unimplemented quest signal. Neither a project overview nor a successful
schema check proves exhaustive semantic knowledge.

## Evidence and depth

Primary sources include author project descriptions, versioned source/wiki
pages, and exact installed dictionaries, guidebooks, tooltips, advancements,
recipes and other data resources. Source references retain release caveats,
resource paths and artifact hashes where available. Search-result extracts are
distinguished from opened pages; descriptive metadata is not a watched video.

The deeper pass covers the native Alex's Mobs dictionary and Alex's Caves
Compendium, major boss routes, machine/heat/fluid/assembly/contraption/train and
Create 6 logistics families, Exposure's actual capture/develop/print/display
workflow, food workstations, transport, storage, fishing, combat controls,
FTB criteria/task families and native Xaero privacy/ownership boundaries.

Actual automatic-caption transcripts were retrieved for:

- [Aarods' performance guide](https://www.youtube.com/watch?v=xOxQlVHakbM):
  Spanish; six timestamped corroborating chapters, explicitly qualified for its
  NeoForge/multi-version context.
- [Liopyu's KubeJS introduction](https://www.youtube.com/watch?v=xhJJbNJjics):
  English; the script-directory lifecycle explanation, qualified for the older
  tutorial API generation.
- [Aarods' Exposure guide](https://www.youtube.com/watch?v=jjjI6jD_RO4):
  Spanish; timestamped photographic workflow corroboration with release caveats.

Profiles contain short original summaries, not redistributed full transcripts.
Each transcript source records language, automatic/manual status, media offsets
and a digest of the retrieved caption sequence. The source schema distinguishes
`youtube_transcript` from `youtube_description`; descriptions cannot satisfy a
transcript claim. Existing version-1 fields retain their previous meaning.

## Reading and validation

```powershell
python Catalog/generator/query_web_mechanics.py coverage
python Catalog/generator/query_web_mechanics.py find alexsmobs
python Catalog/generator/query_web_mechanics.py quests exposure
python Catalog/generator/query_web_mechanics.py graph --mod create
python Catalog/generator/query_web_mechanics.py audit --check-registry --require-coverage --check-artifacts
python Catalog/generator/validate_atlas.py
```

The strict coverage flag rejects an enabled manifest ID with neither a research
record nor an explicit owner/artifact route. The original `unresearched_mod_ids`
field still means no external research JSON; the separately named canonical
routes explain why those IDs are not omitted external-mod research.

Old research snapshots and exact version placeholders remain visible. A newer
manifest generates a drift warning instead of silently relabeling historical
evidence as fresh. Multiple packaged dependency versions are candidates for
loader resolution; they are not proof that each candidate ran simultaneously.

No Gradle build, automated tests, gameplay/server launch, runtime verification,
managed client/server synchronization, AutoModpack publication, stable-profile
mutation, release promotion or MineRENT deployment belongs to this task.

## Final measured result

The PR is based on committed `main` at
`435e8b06f6d3e52eb6fd09f9b313007986d69cdc`; its inherited manifest has 349 IDs.

| Reviewed data | Count |
| --- | ---: |
| External research profiles | 333 |
| Existing JEM owner routes | 14 |
| Vanilla owner route | 1 |
| Evidenced archive wrapper | 1 |
| Unassessed manifest IDs | 0 |
| Source-backed observations | 1441 |
| Source declarations | 776 |
| Installed-static source declarations with artifact/resource proof | 289 |
| Researched prerequisite edges | 148 |
| Actual caption guides / chapter citations | 3 / 8 |

The strict research audit, including static registry references and archive,
nested archive, SHA256, resource and wrapper evidence, passes with zero errors.
Its one warning preserves the older imported research snapshot rather than
misrepresenting it as newly captured. Canonical `validate_atlas.py` passes on the
committed source scope with its referenced installed-artifact prerequisites.
Actual reader queries return 95 Alex's Mobs observations, 26 Exposure quest
ideas and 35 Create prerequisite edges.

A pre-existing derived baseline audit digest was stale relative to
`Catalog/changes`: `maintain_atlas.py audit` on committed main refreshed only
`baseline-coverage.json`'s digest. Its historical source-ownership totals remain
3681 watched, 801 covered and 2880 unresolved; these are pre-existing governance
records, not an external-mod mechanics coverage percentage. No source ownership
or canonical gameplay value was invented to clear that separate historical list.

During research, the concurrent architecture session regenerated the working
manifest to 346 IDs, retiring three JEM identities. A separate audit of that
working manifest also passes with zero errors and zero unassessed IDs. Its five
warnings expose snapshot drift and the three retired owner routes. That session's
new manifest, staged architecture instructions, gameplay sources and changelog
entries are preserved in the shared checkout/index and excluded from this PR.
Research routes retain their reviewed source snapshot; resolve current canonical
owners when consuming them after the architecture update.

The reader/provenance changes received an independent read-only review with no
actionable findings. No automated tests or fixtures were created or executed.
The task changes no runtime artifact and requires no local Server installation
or native AutoModpack publication; concurrent publication state belongs to the
architecture delivery.
