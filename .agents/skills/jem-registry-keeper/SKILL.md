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
   original registry snapshot is otherwise stale.

Follow `references/workflow.md` for selector, precedence, history and impact review
details. Project prohibitions on builds, tests, profiles and deployment still apply.
