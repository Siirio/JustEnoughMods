---
name: jem-registry-keeper
description: Maintain the permanent JEM Registry Atlas before and after any change to modded gameplay, items, blocks, entities, recipes, gear, biomes, loot, economy, progression, Java, Mixins, KubeJS, datapacks, configs, installed mod JARs or dependencies. Use it to query original facts, analyze direct and indirect impact, update canonical change history and effective behavior, and prevent uncovered gameplay changes.
---

# JEM Registry Keeper

Use the existing `Catalog/` only. Never create a parallel registry, spreadsheet,
report database or per-object copy of one shared rule.

For third-party mechanics, cross-mod assumptions and quest ideas, additionally
consult `Catalog/research/README.md` and run
`python Catalog/generator/query_web_mechanics.py find <mod_id>` / `coverage`.
This evidence-only supplement never overrides installed source or canonical
registry, changes and effective behavior. Follow its version and detection limits.

## Bounded packaging-only path

Use this path when an installed JAR changes only to repair its toolchain,
reobfuscation, manifest, signing, compression or delivery packaging, and direct
comparison proves that registry IDs, resources, recipes, configuration and gameplay
semantics are unchanged. This path overrides repeated full-Atlas work below.

1. Perform one production build and one focused artifact comparison
2. Record the new artifact hash and packaging cause in the owning change record
3. Use the smallest project Atlas refresh that records the new artifact evidence,
   then run one final `scan` and `validate`
4. If the current project tools require a full Atlas rebuild for the new JAR hash,
   run the full `rebuild`, `audit`, `scan`, `validate` sequence once only
5. Do not repeat Atlas generation or validation unless a task-owned input changed
   after the successful result
6. Never invoke the shared Atlas publisher, an independent reproducibility store or
   `jem-atlas-engine` for an ordinary JEM repair unless the user explicitly requests
   it or the owning project documentation requires it
7. Do not scan every repository archive to diagnose an optional external Atlas tool
   after the canonical project Atlas has passed
8. Run one pack preview, one publication and application, and one consolidated final
   state check

After the root cause is confirmed, stop optional diagnostics. If a mandatory command
runs longer than five minutes, report the active command and continue only the
required owner workflow. Do not add broader audits to use the waiting time.

## Required workflow

1. From the repository root, read `Catalog/README.md`,
   `Catalog/changes/schema.json`, the owning project docs and owning source.
2. Query every known affected ID with
   `python Catalog/generator/query_atlas.py behavior <namespace:path>`.
3. Run `python Catalog/generator/maintain_atlas.py scan` before changing source.
   Treat listed uncovered paths as the impact-analysis worklist, not as permission
   to invent semantics.
4. Identify the smallest selector that exactly expresses the change: one exact ID
   for unique behavior; a tag, namespace, category, source-backed set or registry
   selector for shared behavior. Add explicit exceptions and runtime conditions.
5. Implement in the canonical owner. Preserve base registry facts under
   `Catalog/registries/`; record the applied rule under `Catalog/changes/records/`.
6. In the same task, record before/after behavior, operation, owner/source, purpose,
   date, `SELF` commit, history, precedence and direct/indirect systems. Never guess
   a missing original value or commit; mark it unresolved.
7. Run:

   ```powershell
   python Catalog/generator/maintain_atlas.py rebuild
   python Catalog/generator/maintain_atlas.py audit
   python Catalog/generator/maintain_atlas.py scan
   python Catalog/generator/validate_atlas.py
   ```

8. Query representative exact IDs again and inspect conflicts and recipe impact.
   Regenerate the full Atlas only when installed registrations/JARs changed or the
   original registry snapshot is otherwise stale. Apply the bounded packaging-only
   path when the JAR change is proven not to change registrations or gameplay.

Follow `references/workflow.md` for selector, precedence, history and impact review
details. Project prohibitions on builds, tests, profiles and deployment still apply.
