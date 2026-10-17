"""Package canonical Puffish data with explicit sandbox-only semantic icon substitutions."""
import json
from pathlib import Path
import sys

MODULE = Path(__file__).resolve().parent
SOURCE = MODULE.parents[1] / 'Client/ManagedPack/config/paxi/datapacks/jem-profession-skills/data'
ICONS = MODULE / 'src/sandbox/resources/assets/jem_professions/sandbox-icons.json'
AVAILABILITY = MODULE / 'src/sandbox/resources/assets/jem_professions/sandbox-availability.json'


def adapt_icons(value, mapping):
    if isinstance(value, dict):
        if value.get('type') == 'item' and isinstance(value.get('data'), dict):
            original = value['data'].get('item')
            if original in mapping:
                value['data']['item'] = mapping[original]
        for child in value.values():
            adapt_icons(child, mapping)
    elif isinstance(value, list):
        for child in value:
            adapt_icons(child, mapping)


def main():
    output = Path(sys.argv[1]).resolve()
    mapping = json.loads(ICONS.read_text(encoding='utf-8'))
    supported = json.loads(AVAILABILITY.read_text(encoding='utf-8'))
    for source in SOURCE.rglob('*.json'):
        target = output / 'data' / source.relative_to(SOURCE)
        target.parent.mkdir(parents=True, exist_ok=True)
        value = json.loads(source.read_text(encoding='utf-8'))
        adapt_icons(value, mapping)
        if source.name == 'definitions.json':
            for node_id, definition in value.items():
                if node_id not in supported:
                    # Native Puffish requests obey this server-loaded requirement too.
                    # Costs, IDs, parent connections and XP remain canonical.
                    definition['required_points'] = 2147483647
                    definition['extra_description'] = 'Эффект недоступен в этой песочнице. Effect unavailable in this sandbox.'
                elif supported[node_id]:
                    definition['required_mods'] = [supported[node_id]]
        target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
