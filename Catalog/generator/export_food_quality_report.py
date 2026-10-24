"""Project the Atlas food authority and exact installed recipe evidence into documentation."""
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import zipfile


def result_ids(value):
    if isinstance(value, str):
        return [value] if ':' in value else []
    if isinstance(value, list):
        return [item for entry in value for item in result_ids(entry)]
    if isinstance(value, dict):
        return [value[k] for k in ('item', 'id') if isinstance(value.get(k), str) and ':' in value[k]]
    return []


def main():
    root = Path(__file__).resolve().parents[2]
    authority = root / 'Catalog/changes/food-quality-assignments.json'
    data = json.loads(authority.read_text(encoding='utf-8'))
    recipes = defaultdict(list)
    inputs = {}
    for jar in sorted((root / 'Client/ManagedPack/mods').glob('*.jar')):
        with zipfile.ZipFile(jar) as archive:
            matching = [p for p in archive.namelist() if '/recipes/' in p and p.endswith('.json')]
            if not matching:
                continue
            inputs[jar.name] = hashlib.sha256(jar.read_bytes()).hexdigest()
            for name in matching:
                try:
                    recipe = json.loads(archive.read(name))
                except (ValueError, UnicodeDecodeError):
                    continue
                if not isinstance(recipe, dict):
                    continue
                outputs = []
                for key in ('result', 'results', 'output', 'outputItem'):
                    outputs += result_ids(recipe.get(key))
                for item in set(outputs):
                    recipes[item].append((jar.name, name))
    rows = sorted(data['assignments'], key=lambda row: row['id'])
    counts = Counter(row['quality_tier'] for row in rows)
    selected = [row for row in rows if row['quality_tier'] == 3]
    selected += [row for row in rows if row['quality_tier'] == 2][:50]
    lines = ['# Food quality v3 coverage', '',
        'Generated from the single Atlas authority. Categories and traits are user-supplied choices, not rankings inferred from recipes.', '',
        f'{len(rows)} exact IDs, tier counts {dict(sorted(counts.items()))}. '
        f'{sum(r["bonus_policy"] == "food_blessing" for r in rows)} blessing meals, '
        f'{sum(r["bonus_policy"] == "native_only" for r in rows)} native-only meals, '
        f'{sum(r["variety_eligible"] for r in rows)} variety-eligible IDs.', '',
        '## Installed acquisition evidence', '',
        'The 49 Masterwork and 50 Exquisite entries below link to actual installed recipe JSON. '
        'A recipe proves a declared result, not ingredient availability, enabled conditions, food properties or runtime consumption. '
        'No recipe match means acquisition remains unresolved, including loot, trading and Java-created recipes. '
        'These limits do not change the supplied category or quota.', '',
        '| Exact ID | Tier | Installed recipe evidence |', '| --- | ---: | --- |']
    for row in selected:
        evidence = '<br>'.join(f'`{jar}` / `{path}`' for jar, path in recipes.get(row['id'], [])) or 'Unresolved, no static recipe result match'
        lines.append(f'| `{row["id"]}` | {row["quality_tier"]} | {evidence} |')
    lines += ['', '## Complete assignment projection', '',
              '| Exact ID | Registry | Native route | Tier | Bonus policy | Traits | Variety |',
              '| --- | --- | --- | ---: | --- | --- | --- |']
    for row in rows:
        lines.append(f'| `{row["id"]}` | {row["registry"]} | {row["consumption_route"]} | {row["quality_tier"]} | '
                     f'{row["bonus_policy"]} | {", ".join(row["blessing_traits"])} | {str(row["variety_eligible"]).lower()} |')
    lines += ['', '## Installed recipe source hashes', '', '| JAR | SHA-256 |', '| --- | --- |']
    used = {jar for row in selected for jar, _ in recipes.get(row['id'], [])}
    lines += [f'| `{jar}` | `{inputs[jar]}` |' for jar in sorted(used)]
    lines += ['', '## Native bite admission', '',
              'JEMFood wraps the actual logical-server BlockState.use call. A scoped receipt requires the same player\'s '
              'native FoodData.eat or positive native food-level insertion, a consuming return and an actual portion/state transition. '
              'Failed use, menu opening, knife slicing, prediction, creative and spectator calls do not award food bonuses. '
              'The seam needs no optional classes. The 22 declared block-bite IDs remain data entries, not a claim that every '
              'block supports direct eating. Native Cookery portions use the same receipt when their exact block ID is assigned. '
              'Runtime bite acceptance remains unverified without a player run.', '']
    (root / 'Docs/food/COVERAGE.md').write_text('\n'.join(lines), encoding='utf-8')
    print(json.dumps({'assignments': len(rows), 'tiers': dict(counts), 'review_sample': len(selected),
                      'sample_with_recipe': sum(bool(recipes.get(r['id'])) for r in selected),
                      'authority_sha256': hashlib.sha256(authority.read_bytes()).hexdigest()}))


if __name__ == '__main__':
    main()
