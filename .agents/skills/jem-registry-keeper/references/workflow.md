# Registry Keeper review guide

The executable data contract is always `Catalog/changes/schema.json`; this guide is
only the review sequence.

## Selector choice

- Exact different behavior: `id`.
- One mod namespace with shared behavior: `namespace`.
- Native tag ownership: `tag`.
- Reviewed Atlas classification: `category`.
- Existing canonical JSON with per-ID values: `source_set`.
- One global rule: `registry` with value `*` and explicit exceptions/conditions.

Do not expand a shared selector into copied ID records. Do not use a broad selector
when affected IDs genuinely have different values.

## Precedence and conflicts

Priorities apply from low to high; selector specificity applies from broad to exact.
Record intentional replacement in `precedence.overrides`. If equal-order rules touch
the same property, keep both and resolve the reported conflict explicitly instead
of depending on file order.

Use `inherits` only for a real reusable parent rule. Missing parents and cycles are
invalid. Prefer ordinary overlapping selectors when both rules independently apply;
inheritance means the child semantically requires the parent even if the parent's
own selector does not match that object.

## History

Each source-changing task adds one compact history entry to the shared rule. Use
`SELF` for the commit containing that entry. If pre-Atlas origin evidence is absent,
use `null` and say it is unresolved; do not backfill a guessed SHA or date.

## Impact review

Check recipes both consuming and producing an item, tag/category membership,
equipment and combat, bosses/events, economy/settlements, loot/acquisition, AI,
biomes/worldgen, progression gates, networking and client/server ownership. Put
machine-known recipe edges in generated output and keep causal explanation once in
the shared record.
