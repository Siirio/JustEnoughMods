# JEM external mechanics evidence — first researched tranche

This is a **non-canonical, source-backed research layer** under the existing JEM Registry Atlas. It is **not** a parallel item registry, automatic gameplay modification, runtime capture, verified entity-AI catalog or executable FTB Quests file. The authoritative sources remain:

`Catalog/registries` (original statically observed identities) → `Catalog/changes` (JEM modifications) → `Catalog/effective` (derived JEM behavior).

Why this exists: the existing static catalog can tell the AI that an item exists but often cannot explain how a player actually uses it. External mod guides add **reviewable gameplay observations** without promoting them into verified installed-version behavior.

## Data contract

- One JSON file per researched **mod ID**, under `web-mechanics/`.
- Each file records the exact version observed in `Catalog/manifest.json`, the pack snapshot SHA, source URL, publisher, source scope, date and distinct mechanic keys.
- Every observation links to one or more declared sources, records a concrete player action and outcome, optional static registry references, local prerequisite links and a **quest idea**, not an FTB task.
- `EXTERNAL_SOURCE_ONLY` and `runtime_verified: false` are mandatory here. Author docs may describe newer or older releases than the installed JAR. A source `installed_release` only proves release-page provenance, **not live gameplay behavior**.
- A `quest_seed.detection` of `custom_criterion` means standard item-inventory detection would be misleading or impossible; obtain a real Forge/FTB signal and test it before implementation. `inventory` is a **candidate** completion mechanism and still needs ID, variants and conditions checked.
- Never turn community-only data, outdated wiki crafting numbers or project-page marketing statements into a server balancing value. For numbers, prefer the installed 1.20.1 JAR, config and authorised runtime evidence.
- Optional dependencies and compatibility warnings must be checked against the installed manifest before suggesting an actual conflict. A known upstream issue in another release is **not a current JEM bug**.
- **No copied third-party prose**: summaries are short original paraphrases with links, not wiki mirrors or redistributed screenshots.

## Reader (offline)

Install the reader's tooling dependency with
`python -m pip install -r Catalog/research/requirements.txt` if `jsonschema` is
not already available. This dependency is used only by the offline reader.
Every mode validates the research against `schema.json` before returning records;
missing or malformed inputs fail instead of appearing as empty coverage.

```powershell
python Catalog/generator/query_web_mechanics.py find unearthed_journey
python Catalog/generator/query_web_mechanics.py quests exposure
python Catalog/generator/query_web_mechanics.py graph --mod unearthed_journey
python Catalog/generator/query_web_mechanics.py coverage
python Catalog/generator/query_web_mechanics.py audit --check-registry
```

`coverage` enumerates **all currently installed mod IDs** and explicitly lists those not yet researched. It does not pretend 10 researched mod IDs provide encyclopaedic coverage of the pack. `audit` checks source references, feature-link cycles, syntax, version drift and (optionally) statically observed registry refs; it does not launch the game or prove runtime truth.

Enabled outer artifacts and their embedded dependencies contribute mod IDs, with
duplicate IDs counted once. Snapshot and version drift remain explicit warnings;
an unchanged version string does not prove that the pack's effective rules match.
Search and quest results retain the original research snapshot and date. Graph
edges retain source URLs, version and conditions and are typed `requires`.

## Safe workflow for Codex / FTB Quests / conflict research

1. Read `Catalog/README.md` and the exact static registry record, JEM changes and effective behavior.
2. Query this external evidence by `mod ID` or mechanic. Follow the original URL and check the exact installed version.
3. Inspect the owning JAR/config/source for version-dependent conditions, tags, registry IDs and working gameplay detection events.
4. Model prerequisites as typed graph edges. Detect unavailable dependencies, cycles, overlapping roles and impossible quest conditions before implementation.
5. Propose FTB objectives that require **player actions**; avoid books made entirely of generic checkmarks and text. For custom actions, create/use a real criterion, otherwise label the quest as not yet implementable.
6. Store any actual JEM rule change in `Catalog/changes`, not here. Future generators can materialise reviewed mechanic relationships as a derived graph; never silently replace `canonical/relationships.jsonl` with web guesses.

## Known snapshot cautions

- Native static registry detection can mistake localization keys such as `exposure:album.tooltip.photos_count` and `spawn:angler_fish.description` for real registered objects. These should be treated as **potential false positives** until the Forge registry is captured; never delete them automatically from the canonical generator output.
- Unearthed Journey's author describes **three** Cenozoic dimensions, unlike the earlier informal two-dimension description.
- Installed Skarrier Mobs is **1.1.1**; a later hotfix addresses a Frost crash. Don't claim the hotfix is installed.
- Installed Exposure is **1.9.21**, though the upstream site advertises newer versions. Its compatibility warnings are not by themselves evidence of a current pack issue.
- `Catalog/generated/atlas/canonical/relationships.jsonl` remains empty in the current export; `graph` here only shows researched, not runtime-confirmed, prerequisite hypotheses.

Further details: `Docs/catalog/WEB_MECHANICS_FIRST_PASS.md`.
